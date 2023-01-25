using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
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
using JetBrains.Rider.Model;
using JetBrains.Util;
using JetBrains.Util.Dotnet.TargetFrameworkIds;
using UtBot.Rd.Generated;
using UtBot.VSharp;

namespace UtBot;

[GeneratorBuilder(GenerateUnitTestWorkflow.Kind, typeof(CSharpLanguage))]
internal sealed class UnitTestBuilder : GeneratorBuilderBase<CSharpGeneratorContext>
{
    private readonly IBackgroundProgressIndicatorManager _backgroundProgressIndicatorManager;
    private readonly Lifetime _lifetime;
    private readonly ILogger _logger;
    private readonly IShellLocks _shellLocks;
    private readonly ISolutionBuilder _solutionBuilder;
    private const int GenerationTimeout = 10;

    public UnitTestBuilder(
        Lifetime lifetime,
        IShellLocks shellLocks,
        IBackgroundProgressIndicatorManager backgroundProgressIndicatorManager,
        ISolutionBuilder solutionBuilder,
        ILogger logger)
    {
        _lifetime = lifetime;
        _shellLocks = shellLocks;
        _backgroundProgressIndicatorManager = backgroundProgressIndicatorManager;
        _solutionBuilder = solutionBuilder;
        _logger = logger;
    }

    protected override void Process(CSharpGeneratorContext context, IProgressIndicator progress)
    {
        if (context.PsiModule.ContainingProjectModule is not IProject project) return;
        var typeElement = context.ClassDeclaration.DeclaredElement;
        if (typeElement == null) return;
        if (typeElement is not IClass && typeElement is not IStruct) return;
        var tfm = context.PsiModule.TargetFrameworkId;
        var assembly = project.GetOutputFilePath(tfm);
        var descriptors = new List<MethodDescriptor>();
        foreach (var inputElement in context.InputElements.WithProgress(progress, "Generating Unit tests")
                     .OfType<GeneratorDeclaredElement<IMethod>>())
            descriptors.Add(new MethodDescriptor(inputElement.DeclaredElement.ShortName, typeElement.GetClrName().FullName));

        var progressLifetimeDef = _lifetime.CreateNested();
        var indicator =
            _backgroundProgressIndicatorManager.CreateIndicator(progressLifetimeDef.Lifetime, true, true,
                "Generating Unit Tests");
        _shellLocks.Tasks.StartNew(_lifetime, Scheduling.FreeThreaded, () =>
        {
            try
            {
                Generate(indicator, project, assembly, descriptors, tfm);
            }
            finally
            {
                progressLifetimeDef.Terminate();
            }
        });
    }

    private void Generate(IBackgroundProgressIndicator progressIndicator, IProject project,
        VirtualFileSystemPath assemblyPath, List<MethodDescriptor> descriptors, TargetFrameworkId tfm)
    {
        var solution = project.GetSolution();
        var solutionMark = solution.GetSolutionMark();
        if (solutionMark == null) return;

        var solutionFilePath = solutionMark.Location.FullPath;
        _logger.Verbose($"Solution path: {solutionFilePath}");

        project.Locks.AssertNonMainThread();

        SolutionBuilderRequest buildRequest;
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

        var pluginPath = FileSystemPath.Parse(Assembly.GetExecutingAssembly().Location).Parent;
        var vsharpRunner = pluginPath.Combine("UtBot.VSharp.dll");

        foreach (var descriptor in descriptors)
        {
            var currentGeneratedItem = $"{descriptor.TypeName}.{descriptor.MethodName}";
            _logger.Verbose($"Start Generation for {currentGeneratedItem}");
            progressIndicator.Lifetime.ThrowIfNotAlive();
            progressIndicator.Header.SetValue(currentGeneratedItem);

            _logger.Catch(() =>
            {
                var port = NetworkUtil.GetFreePort();
                var proc = new ProcessWithRdServer(VSharpMain.VSharpProcessName, project.ProjectFileLocation.Parent.FullPath, port, vsharpRunner.FullPath,
                    project.Locks,
                    _lifetime);
                var projectCsprojPath = project.ProjectFileLocation.FullPath;
                var vsharpProjectTarget = calculateTestProjectTarget(tfm);
                var args = new GenerateArguments(assemblyPath.FullPath, projectCsprojPath, solutionFilePath, descriptor,
                    GenerationTimeout, vsharpProjectTarget);
                var result = proc.VSharpModel.Generate.Sync(args, RpcTimeouts.Maximal);
                _logger.Info("Result acquired");
                if (result.IsGenerated)
                {
                    _shellLocks.ExecuteOrQueue(_lifetime, "UnitTestBuilder::Generate", () =>
                    {
                        if (solution.IsValid())
                        {
                            solution.GetProtocolSolution().GetFileSystemModel().RefreshPaths
                                .Start(_lifetime,
                                    new RdFsRefreshRequest(new List<string> { result.GeneratedProjectPath }, true));
                        }
                    });
                }
                else
                {
                    _logger.Info(
                        $"Could not generate tests for ${currentGeneratedItem}, exception - ${result.ExceptionMessage}");
                }
            });

            _logger.Verbose($"Generation finished for {currentGeneratedItem}");
        }

        _shellLocks.ExecuteOrQueue(_lifetime, "UnitTestBuilder::Generate", () =>
        {
            if (project.IsValid())
                solution.GetProtocolSolution().GetFileSystemModel().RefreshPaths
                    .Start(_lifetime,
                        new RdFsRefreshRequest(new List<string> { solutionMark.Location.FullPath }, true));
        });
    }

    private static HashSet<String> possibleTargets = new()
    {
        "net7.0",
        "net6.0",
        "netcoreapp1.0",
        "netcoreapp1.1",
        "netcoreapp2.0",
        "netcoreapp2.1",
        "netcoreapp2.2",
        "netcoreapp3.0",
        "netcoreapp3.1",
        "net35",
        "net40",
        "net451",
        "net452",
        "net46",
        "net461",
        "net462",
        "net47",
        "net471",
        "net472",
        "net48"
    };

    private static string calculateTestProjectTarget(TargetFrameworkId tfm)
    {
        var id = tfm.TryGetShortIdentifier();

        if (possibleTargets.Contains(id))
            return id;

        return "net6.0";
    }
}