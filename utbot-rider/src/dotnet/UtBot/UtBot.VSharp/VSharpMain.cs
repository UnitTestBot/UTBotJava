using System;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Runtime.Loader;
using System.Text;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Util;
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
            descriptor, generationTimeout, targetFramework) = arguments;
        var assemblyLoadContext = new AssemblyLoadContext(VSharpProcessName);
        using var fs = File.OpenRead(assemblyPath);
        var ass = assemblyLoadContext.LoadFromStream(fs);
        var type = ass.GetType(descriptor.TypeName, throwOnError: false);
        if (type?.FullName != descriptor.TypeName)
            throw new InvalidDataException($"cannot find type - {descriptor.TypeName}");
        var methodInfo = type.GetMethod(descriptor.MethodName,
            BindingFlags.Instance
            | BindingFlags.Static
            | BindingFlags.Public);
        if (methodInfo?.Name != descriptor.MethodName)
            throw new InvalidDataException(
                $"cannot find method - ${descriptor.MethodName} for type - ${descriptor.TypeName}");
        var stat = TestGenerator.Cover(methodInfo, generationTimeout, verbosity:Verbosity.Info);
        var targetProject = new FileInfo(projectCsprojPath);
        var solution = new FileInfo(solutionFilePath);
        var declaringType = methodInfo.DeclaringType;
        Debug.Assert(declaringType != null);
        var (generatedProject, renderedFiles) =
            Renderer.Render(stat.Results(), targetProject, declaringType, assemblyLoadContext, solution, targetFramework);
        return new GenerateResults(true, generatedProject.FullName, renderedFiles.ToArray(), null);
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
                Logger.ConfigureWriter(new SignalWriter(vSharpModel.Log));
                vSharpModel.Generate.Set((_, arguments) =>
                {
                    try
                    {
                        return GenerateImpl(arguments);
                    }
                    catch (Exception e)
                    {
                        return new GenerateResults(false, "", EmptyArray<string>.Instance, e.ToString());
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
