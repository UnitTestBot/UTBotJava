using System;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.Loader;
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
    public static void Main(string[] args)
    {
        // TextWriter);
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
                vSharpModel.Generate.Set((_, arguments) =>
                {
                    var (assemblyPath, projectCsprojPath, solutionFilePath,
                        moduleFqnName, methodToken, generationTimeout) = arguments;
                    var assemblyLoadContext = new AssemblyLoadContext(VSharpProcessName);
                    var fs = File.OpenRead(assemblyPath);
                    var ass = assemblyLoadContext.LoadFromStream(fs);
                    fs.Close();
                    fs.Dispose();
                    var methodBase = ass.GetModules().Single(module => module.FullyQualifiedName == moduleFqnName).ResolveMethod(methodToken);
                    Debug.Assert(methodBase != null);
                    var stat = TestGenerator.Cover(methodBase, generationTimeout);
                    var targetProject = new FileInfo(projectCsprojPath);
                    var solution = new FileInfo(solutionFilePath);
                    var declaringType = methodBase.DeclaringType;
                    Debug.Assert(declaringType != null);
                    var (generatedProject, renderedFiles) = Renderer.Render(stat.Results(), targetProject,
                        declaringType,
                        assemblyLoadContext, solution);
                    var result = new GenerateResults(generatedProject.FullName, renderedFiles.ToArray());
                    blockingQueue.Add("End");
                    return result;
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
        ldef.Terminate();
    }
}