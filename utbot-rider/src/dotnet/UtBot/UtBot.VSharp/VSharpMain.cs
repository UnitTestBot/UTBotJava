using System;
using System.Collections.Concurrent;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.Loader;
using System.Text;
using System.Text.Json;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using UtBot.Rd;
using UtBot.Rd.Generated;
using VSharp;
using VSharp.TestRenderer;

namespace UtBot.VSharp;

public static class VSharpMain
{
    public static readonly string VSharpProcessName = "VSharp";

    private class SignalWriter : TextWriter
    {
        private readonly ISignal<string> _signal;
        public SignalWriter(ISignal<string> signal)
        {
            _signal = signal;
        }

        public override void Write(string value)
        {
            _signal.Fire(value);
        }

        public override void WriteLine(string value)
        {
            Write(value);
        }

        public override Encoding Encoding => Encoding.Default;
    }

    private static GenerateResults GenerateImpl(GenerateArguments arguments)
    {
        var (assemblyPath, projectCsprojPath, solutionFilePath,
            methodDescriptors, generationTimeout, targetFramework) = arguments;

        var assemblyLoadContext = new AssemblyLoadContext(VSharpProcessName);
        assemblyLoadContext.Resolving += (context, name) =>
        {
            var found = Directory.GetFiles(new FileInfo(assemblyPath).Directory?.FullName, $"{name.Name}.dll")
                .FirstOrDefault();
            if (found is null)
            {
                return null;
            }

            return context.LoadFromAssemblyPath(found);
        };
        var assembly = assemblyLoadContext.LoadFromAssemblyPath(assemblyPath);
        var methods = methodDescriptors.Select(d => d.ToMethodInfo(assembly)).ToList();
        var declaringType = methods.Select(m => m.DeclaringType).Distinct().SingleOrDefault();

        var stat = TestGenerator.Cover(methods, generationTimeout, verbosity:Verbosity.Info);

        var testedProject = new FileInfo(projectCsprojPath);
        var solution = new FileInfo(solutionFilePath);

        var (generatedProject, renderedFiles) =
            Renderer.Render(stat.Results(), testedProject, declaringType, solution, targetFramework);

        return new GenerateResults(
            generatedProject.FullName,
            renderedFiles,
            null,
            (int)stat.TestsCount,
            (int)stat.ErrorsCount);
    }

    private static bool MatchesType(TypeDescriptor typeDescriptor, Type typ)
    {
        if (typ.IsGenericMethodParameter)
        {
            return typ.GenericParameterPosition == typeDescriptor.MethodParameterPosition;
        }

        if (typ.IsGenericTypeParameter)
        {
            return typ.GenericParameterPosition == typeDescriptor.TypeParameterPosition;
        }

        if (typ.IsArray)
        {
            return typ.GetArrayRank() == typeDescriptor.ArrayRank &&
                   MatchesType(typeDescriptor.Parameters[0], typ.GetElementType());
        }

        var name = typ.IsGenericType ? typ.GetGenericTypeDefinition().FullName : typ.FullName;

        if (name != typeDescriptor.Name)
        {
            Logger.printLogString(Logger.Error, $"{typ.FullName} != {typeDescriptor.Name}");
            return false;
        }

        var genericArguments = typ.GetGenericArguments();

        if (genericArguments.Length != typeDescriptor.Parameters.Count)
        {
            return false;
        }

        for (var i = 0; i < genericArguments.Length; ++i)
        {
            if (!MatchesType(typeDescriptor.Parameters[i], genericArguments[i]))
            {
                return false;
            }
        }

        return true;
    }

    private static bool MatchesMethod(MethodDescriptor descriptor, MethodInfo methodInfo)
    {
        var targetParameters = descriptor.Parameters.Select(p => JsonSerializer.Deserialize<TypeDescriptor>(p)).ToArray();

        if (methodInfo.Name != descriptor.MethodName)
        {
            return false;
        }

        var parameters = methodInfo.GetParameters();

        if (parameters.Length != targetParameters.Length)
        {
            return false;
        }

        for (var i = 0; i < parameters.Length; ++i)
        {
            if (!MatchesType(targetParameters[i], parameters[i].ParameterType))
            {
                return false;
            }
        }

        return true;
    }

    private static MethodInfo ToMethodInfo(this MethodDescriptor descriptor, Assembly assembly)
    {
        var type = assembly.GetType(descriptor.TypeName, throwOnError: false);

        if (type?.FullName != descriptor.TypeName)
            throw new InvalidDataException($"Cannot find type {descriptor.TypeName}, found: {type?.Name}");

        MethodInfo methodInfo;
        var bindingFlags = BindingFlags.Instance | BindingFlags.Static | BindingFlags.Public;

        if (descriptor.HasNoOverloads)
        {
            methodInfo = type.GetMethod(descriptor.MethodName, bindingFlags);
        }
        else
        {
            methodInfo = type.GetMethods()
                .FirstOrDefault(m => MatchesMethod(descriptor, m));
        }

        if (methodInfo?.Name != descriptor.MethodName)
            throw new InvalidDataException(
                $"Cannot find method ${descriptor.MethodName} for type ${descriptor.TypeName}");

        return methodInfo;
    }

    public static void Main(string[] args)
    {
        using var blockingQueue = new BlockingCollection<string>(1);
        var port = int.Parse(args[0]);
        var ldef = new LifetimeDefinition();
        SingleThreadScheduler.RunOnSeparateThread(ldef.Lifetime, VSharpProcessName, scheduler =>
        {
            var wire = new SocketWire.Client(ldef.Lifetime, scheduler, port);
            var serializers = new Serializers();
            var identities = new Identities(IdKind.Client);
            var protocol = new Protocol(VSharpProcessName, serializers, identities, scheduler, wire, ldef.Lifetime);
            scheduler.Queue(() =>
            {
                var vSharpModel = new VSharpModel(ldef.Lifetime, protocol);
                // Configuring V# logger: messages will be send via RD to UTBot plugin process
                Logger.configureWriter(new SignalWriter(vSharpModel.Log));
                vSharpModel.Generate.Set((_, arguments) =>
                {
                    try
                    {
                        return GenerateImpl(arguments);
                    }
                    catch (Exception e)
                    {
                        return new GenerateResults(null, new(), e.ToString(), 0, 0);
                    }
                    finally
                    {
                        scheduler.Queue(() => { blockingQueue.Add("End"); });
                    }
                });
                vSharpModel.Ping.Advise(ldef.Lifetime, s =>
                {
                    if (s == RdUtil.MainProcessName)
                    {
                        vSharpModel.Ping.Fire(VSharpProcessName);
                    }
                });
            });
        });
        blockingQueue.Take();
        // todo check if queueing take as next action is enough
        // Thread.Sleep(1000);
        ldef.Terminate();
    }
}
