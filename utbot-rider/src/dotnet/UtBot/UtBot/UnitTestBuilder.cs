using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.Loader;
using System.Threading;
using JetBrains.Application.Progress;
using JetBrains.Application.Threading;
using JetBrains.Application.Threading.Tasks;
using JetBrains.Application.UI.Controls;
using JetBrains.DataFlow;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Features.SolutionBuilders;
using JetBrains.ProjectModel.ProjectsHost;
using JetBrains.Rd.Tasks;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Feature.Services.CSharp.Generate;
using JetBrains.ReSharper.Feature.Services.Generate;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.UnitTestFramework;
using JetBrains.ReSharper.UnitTestFramework.Criteria;
using JetBrains.ReSharper.UnitTestFramework.Execution.Hosting;
using JetBrains.ReSharper.UnitTestFramework.Exploration.Artifacts;
using JetBrains.Rider.Model;
using JetBrains.Util;
using UtBot.Rd.Generated;
using UtBot.VSharp;
using Thread = System.Threading.Thread;

namespace UtBot;

[GeneratorBuilder(GenerateUnitTestWorkflow.Kind, typeof(CSharpLanguage))]
internal sealed class UnitTestBuilder : GeneratorBuilderBase<CSharpGeneratorContext>
{
    private readonly IBackgroundProgressIndicatorManager _backgroundProgressIndicatorManager;
    private readonly Lifetime _lifetime;
    private readonly ILogger _logger;
    private readonly IShellLocks _shellLocks;
    private readonly ISolutionBuilder _solutionBuilder;
    private readonly IUnitTestArtifactExplorationProcess _unitTestArtifactExplorationProcess;
    private readonly IUnitTestingFacade _unitTestingFacade;
    private const int _generationTimeout = 10;

    public UnitTestBuilder(
        Lifetime lifetime,
        IShellLocks shellLocks,
        IBackgroundProgressIndicatorManager backgroundProgressIndicatorManager,
        ISolutionBuilder solutionBuilder,
        IUnitTestArtifactExplorationProcess unitTestArtifactExplorationProcess,
        IUnitTestingFacade unitTestingFacade,
        ILogger logger)
    {
        _lifetime = lifetime;
        _shellLocks = shellLocks;
        _backgroundProgressIndicatorManager = backgroundProgressIndicatorManager;
        _solutionBuilder = solutionBuilder;
        _unitTestArtifactExplorationProcess = unitTestArtifactExplorationProcess;
        _unitTestingFacade = unitTestingFacade;
        _logger = logger;
    }

    protected override void Process(CSharpGeneratorContext context, IProgressIndicator progress)
    {
        if (context.PsiModule.ContainingProjectModule is not IProject project) return;
        var typeElement = context.ClassDeclaration.DeclaredElement;
        if (typeElement == null) return;
        if (!(typeElement is IClass) && !(typeElement is IStruct)) return;
        var assembly = project.GetOutputFilePath(context.PsiModule.TargetFrameworkId);
        var descriptors = new List<UnitTestMethodDescriptor>();
        foreach (var inputElement in context.InputElements.WithProgress(progress, "Generating Unit tests")
                     .OfType<GeneratorDeclaredElement<IMethod>>())
            descriptors.Add(new UnitTestMethodDescriptor
                { MethodName = inputElement.DeclaredElement.ShortName, TypeName = typeElement.ShortName });

        var progressLifetimeDef = _lifetime.CreateNested();
        var indicator =
            _backgroundProgressIndicatorManager.CreateIndicator(progressLifetimeDef.Lifetime, true, true,
                "Generating Unit Tests");
        _shellLocks.Tasks.StartNew(_lifetime, Scheduling.FreeThreaded, () =>
        {
            try
            {
                Generate(indicator, project, assembly, descriptors);
            }
            finally
            {
                progressLifetimeDef.Terminate();
            }
        });
    }

    private void Generate(IBackgroundProgressIndicator progressIndicator, IProject project,
        VirtualFileSystemPath assemblyPath, List<UnitTestMethodDescriptor> descriptors)
    {
        SolutionBuilderRequest buildRequest;
        var contextUnloaded = false;
        using (_shellLocks.UsingReadLock())
        {
            if (!project.IsValid()) return;
            buildRequest = _solutionBuilder.CreateBuildRequest(BuildSessionTarget.Build,
                new[] { project },
                SolutionBuilderRequestSilentMode.Silent,
                new SolutionBuilderRequestAdvancedSettings(null,
                    false, verbosityLevel: LoggerVerbosityLevel.Normal, isRestoreRequest: true));

            _solutionBuilder.ExecuteBuildRequest(buildRequest);
        }

        buildRequest.State.WaitForValue(_lifetime, state => state.HasFlag(BuildRunState.Completed));

        var assemblyLoadContext = new AssemblyLoadContext("UnitTestGeneration");
        //var renderer = new NunitTestRenderer(assemblyLoadContext);
        try
        {
            var fs = File.OpenRead(assemblyPath.FullPath);
            var ass = assemblyLoadContext.LoadFromStream(fs);
            fs.Close();
            fs.Dispose();
            var solution = project.GetSolution();
            var solutionMark = solution.GetSolutionMark();
            if (solutionMark == null) return;
            string unitTestProjectLocation = null;
            foreach (var descriptor in descriptors)
            foreach (var type in ass.GetTypes())
            {
                if (type.Name != descriptor.TypeName) continue;
                foreach (var methodInfo in type.GetMethods())
                {
                    if (methodInfo.Name != descriptor.MethodName) continue;
                    progressIndicator.Header.SetValue($"{descriptor.TypeName}.{descriptor.MethodName}");
                    var solutionFilePath = solutionMark.Location.FullPath;
                    _logger.Verbose($"Solution path: {solutionFilePath}");
                    _logger.Verbose("Start Generation");
                    _logger.Catch(() =>
                    {
                        project.Locks.AssertNonMainThread(); 
                        var pluginPath = FileSystemPath.Parse(Assembly.GetExecutingAssembly().Location)
                            .Parent;
                        var vsharpRunner = pluginPath.Combine("UtBot.VSharp.dll");
                        var port = NetworkUtil.GetFreePort();
                        var proc = new ProcessWithRdServer(VSharpMain.VSharpProcessName, port, vsharpRunner.FullPath, project.Locks, _lifetime);
                        var projectCsprojPath = project.ProjectFileLocation.FullPath;
                        var moduleFqnName = methodInfo.Module.FullyQualifiedName;
                        var methodToken = methodInfo.MetadataToken;
                        var args = new GenerateArguments(assemblyPath.FullPath, projectCsprojPath, solutionFilePath,
                            moduleFqnName, methodToken, _generationTimeout);
                        var result = proc.VSharpModel.Generate.Sync(args, RpcTimeouts.Maximal);
                        unitTestProjectLocation = result?.GeneratedProjectPath ?? ""; 
                        _shellLocks.ExecuteOrQueue(_lifetime, "UnitTestBuilder::Generate", () =>
                        {
                            if (solution.IsValid())
                            {
                                solution.GetProtocolSolution().GetFileSystemModel().RefreshPaths
                                    .Start(_lifetime, new RdFsRefreshRequest(new List<string> { unitTestProjectLocation }, true));
                            }
                        });
                    });
                    _logger.Verbose("Generation finished");
                }
            }

            //assemblyLoadContext.Unload();
            contextUnloaded = true;
            _shellLocks.ExecuteOrQueue(_lifetime, "UnitTestBuilder::Generate", () =>
            {
                if (project.IsValid())
                    solution.GetProtocolSolution().GetFileSystemModel().RefreshPaths
                        .Start(_lifetime,
                            new RdFsRefreshRequest(new List<string> { solutionMark.Location.FullPath }, true));
            });
            if (unitTestProjectLocation == null) return;
            IProject unitTestProject = null;
            var manualResetEvent = new ManualResetEvent(false);
            var unitTestProjectPath = VirtualFileSystemPath.Parse(unitTestProjectLocation, InteractionContext.Local);

            while (unitTestProject == null && _lifetime.IsAlive && project.IsValid())
            {
                _shellLocks.ExecuteOrQueueReadLock(_lifetime, "UnitTestBuilder::WaitingForProject",
                    () =>
                    {
                        _logger.Warn("Try to get project");
                        unitTestProject = TryToFindProject(solution, unitTestProjectPath);
                        manualResetEvent.Set();
                    });
                if (!manualResetEvent.WaitOne(TimeSpan.FromSeconds(10)))
                {
                    _logger.Warn("Exit by timeout");
                    return;
                }

                manualResetEvent.Reset();
                Thread.Sleep(1000);
            }

            if (unitTestProject != null && unitTestProject.IsValid())
            {
                using (_shellLocks.UsingReadLock())
                {
                    if (!unitTestProject.IsValid()) return;
                    buildRequest = _solutionBuilder.CreateBuildRequest(BuildSessionTarget.Build,
                        new[] { unitTestProject },
                        SolutionBuilderRequestSilentMode.Silent,
                        new SolutionBuilderRequestAdvancedSettings(null,
                            false, verbosityLevel: LoggerVerbosityLevel.Normal, isRestoreRequest: true));

                    _solutionBuilder.ExecuteBuildRequest(buildRequest);
                }

                buildRequest.State.WaitForValue(_lifetime, state => state.HasFlag(BuildRunState.Completed));

                _unitTestArtifactExplorationProcess.ExploreProject(project).ContinueWith(_ =>
                {
                    _unitTestingFacade
                        .Run(new ProjectCriterion(unitTestProject))
                        .Using(UnitTestHost.Instance.GetProvider("Cover"))
                        .In.CurrentOrNewSession();
                }, _lifetime.ToCancellationToken());
            }
        }
        finally
        {
            if (!contextUnloaded)
            {
                //assemblyLoadContext.Unload();
            }
        }
    }

    private IProject TryToFindProject(ISolution solution, VirtualFileSystemPath unitTestProjectPath)
    {
        return solution.FindProjectItemsByLocation(unitTestProjectPath).SingleItem() as IProject;
    }
}