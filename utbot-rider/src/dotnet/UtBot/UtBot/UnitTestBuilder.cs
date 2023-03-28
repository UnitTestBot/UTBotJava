using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text.Encodings.Web;
using System.Text.Json;
using System.Timers;
using JetBrains.Application.Notifications;
using JetBrains.Application.Progress;
using JetBrains.Application.Threading;
using JetBrains.Application.Threading.Tasks;
using JetBrains.Application.UI.Controls;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.ProjectsHost;
using JetBrains.Rd.Tasks;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Feature.Services.CSharp.Generate;
using JetBrains.ReSharper.Feature.Services.Generate;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.Util;
using JetBrains.Rider.Model;
using JetBrains.Util;
using JetBrains.Util.Threading;
using UtBot.Rd;
using UtBot.Rd.Generated;
using UtBot.Utils;
using UtBot.VSharp;

namespace UtBot;

[GeneratorBuilder(GenerateUnitTestWorkflow.Kind, typeof(CSharpLanguage))]
internal sealed class UnitTestBuilder : GeneratorBuilderBase<CSharpGeneratorContext>
{
    private const string PublishDirName = "utbot-publish";

    private readonly IBackgroundProgressIndicatorManager _backgroundProgressIndicatorManager;
    private readonly Lifetime _lifetime;
    private readonly ILogger _logger;
    private readonly IShellLocks _shellLocks;
    private readonly UtBotRiderModel _riderModel;
    private readonly Notifications _notifications;

    public UnitTestBuilder(
        Lifetime lifetime,
        ISolution solution,
        IShellLocks shellLocks,
        IBackgroundProgressIndicatorManager backgroundProgressIndicatorManager,
        ILogger logger,
        Notifications notifications)
    {
        _lifetime = lifetime;
        _shellLocks = shellLocks;
        _backgroundProgressIndicatorManager = backgroundProgressIndicatorManager;
        _logger = logger;
        _notifications = notifications;
        _riderModel = solution.GetProtocolSolution().GetUtBotRiderModel();
    }

    protected override void Process(CSharpGeneratorContext context, IProgressIndicator progress)
    {
        _notifications.Refresh();

        var timeoutString = context.GetOption(TimeoutGeneratorOption.Id);

        if (!int.TryParse(timeoutString, out var timeout) || timeout <= 0)
        {
            _notifications.ShowError("Invalid timeout value. Timeout should be an integer number greater than zero");
            return;
        }

        if (context.PsiModule.ContainingProjectModule is not IProject project) return;

        if (!DotNetVersionUtils.CanRunVSharp(project.GetSolution()))
        {
            _notifications.ShowError($"At least .NET {DotNetVersionUtils.MinCompatibleSdkMajor} SDK is required for UnitTestBot.NET");
            return;
        }

        var typeElement = context.ClassDeclaration.DeclaredElement;
        if (typeElement == null) return;
        if (typeElement is not IClass && typeElement is not IStruct) return;
        var testProjectTfm = DotNetVersionUtils.GetTestProjectFramework(project);

        var jsonOptions = new JsonSerializerOptions
        {
            Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
        };

        var descriptors = new List<MethodDescriptor>();
        foreach (var inputElement in context.InputElements.WithProgress(progress, "Generating unit tests")
                     .OfType<GeneratorDeclaredElement<IMethod>>())
        {
            var methodName = inputElement.DeclaredElement.ShortName;
            var hasNoOverLoads = typeElement.GetAllClassMembers().Count(m => m.Member.ShortName == methodName) == 1;

            var parameterDescriptors = new List<string>();

            if (!hasNoOverLoads)
            {
                foreach (var parameter in inputElement.DeclaredElement.Parameters)
                {
                    var typeDescriptor = ToTypeDescriptor(parameter.Type, inputElement.DeclaredElement, typeElement);
                    parameterDescriptors.Add(JsonSerializer.Serialize(typeDescriptor, jsonOptions));
                }
            }

            descriptors.Add(new MethodDescriptor(methodName, typeElement.GetClrName().FullName, hasNoOverLoads, parameterDescriptors));
        }

        var progressLifetimeDef = _lifetime.CreateNested();
        var indicator =
            _backgroundProgressIndicatorManager.CreateIndicator(progressLifetimeDef.Lifetime, true, true,
                "Generating unit tests");

        _shellLocks.Tasks.StartNew(_lifetime, Scheduling.FreeThreaded, () =>
        {
            try
            {
                Generate(indicator, project, descriptors, testProjectTfm, timeout);
            }
            finally
            {
                progressLifetimeDef.Terminate();
            }
        });
    }

    private TypeDescriptor ToTypeDescriptor(IType typ, IMethod declMethod, ITypeElement declTyp)
    {
        var par = typ.GetTypeParameterType();

        if (declMethod.TypeParameters.Contains(par))
        {
            return new TypeDescriptor
            {
                ArrayRank = null,
                Name = null,
                MethodParameterPosition = declMethod.TypeParameters.IndexOf(par),
                TypeParameterPosition = null,
                Parameters = new()
            };
        }

        if (declTyp.TypeParameters.Contains(par))
        {
            return new TypeDescriptor
            {
                ArrayRank = null,
                Name = null,
                MethodParameterPosition = null,
                TypeParameterPosition = declTyp.TypeParameters.IndexOf(par),
                Parameters = new()
            };
        }

        if (typ is IArrayType arr)
        {
            var elementType = ToTypeDescriptor(typ.GetScalarType(), declMethod, declTyp);

            return new TypeDescriptor
            {
                ArrayRank = arr.Rank,
                Name = null,
                MethodParameterPosition = null,
                TypeParameterPosition = null,
                Parameters = new List<TypeDescriptor> { elementType }
            };
        }

        var subst = typ.GetScalarType().GetSubstitution();
        var pars = typ.GetTypeElement().TypeParameters.Select(p => ToTypeDescriptor(subst[p], declMethod, declTyp));

        return new TypeDescriptor
        {
            ArrayRank = null,
            Name = typ.GetScalarType()?.GetClrName().FullName,
            MethodParameterPosition = null,
            TypeParameterPosition = null,
            Parameters = pars.ToList()
        };
    }

    private void Generate(IBackgroundProgressIndicator progressIndicator, IProject project,
        List<MethodDescriptor> descriptors, TestProjectTargetFramework testProjectFramework, int timeout)
    {
        var solution = project.GetSolution();
        var solutionMark = solution.GetSolutionMark();
        if (solutionMark == null) return;

        var solutionFilePath = solutionMark.Location.FullPath;
        _logger.Verbose($"Solution path: {solutionFilePath}");

        project.Locks.AssertNonMainThread();

        var config = project.ProjectProperties.ActiveConfigurations.Configurations.First();
        var outputDir = project.GetOutputDirectory(config.TargetFrameworkId).Combine(PublishDirName);
        progressIndicator.Header.SetValue($"Publishing dependencies to {PublishDirName}...");

        if (!ProjectPublisher.PublishSync(_logger, progressIndicator, project, config, outputDir, _riderModel)) {
            var title = $"Cannot publish project {project.Name}";
            var openExceptionMessageCommand = new UserNotificationCommand(
                "Show error info",
                () => MessageBox.ShowError(title, title));
            _notifications.ShowError(title, command: openExceptionMessageCommand);
            return;
        }

        var assemblyFileName = project.GetOutputFilePath(config.TargetFrameworkId).Name;
        var assemblyPath =  Directory.GetFiles(outputDir.FullPath, assemblyFileName, SearchOption.AllDirectories).FirstOrDefault();
        if (assemblyPath is null)
        {
            _notifications.ShowError($"Cannot build project {project.Name}");
            return;
        }

        var typeName = descriptors.Select(m => m.TypeName).Distinct().SingleOrDefault();

        _logger.Verbose($"Start Generation for {typeName}");
        progressIndicator.Lifetime.ThrowIfNotAlive();
        progressIndicator.Header.SetValue(typeName);

        var pluginPath = FileSystemPath.Parse(Assembly.GetExecutingAssembly().Location).Parent;
        var vsharpRunner = pluginPath.Combine("UtBot.VSharp.dll");

        var methodNames = descriptors.Select(m => $"{m.TypeName}.{m.MethodName}").ToArray();
        var intervalS = (double)timeout / methodNames.Length;
        var intervalMs = intervalS > 0 ? intervalS * 1000 : 500;
        using var methodProgressTimer = new Timer(intervalMs);
        var i = 0;
        void ChangeMethodName()
        {
            progressIndicator.Header.SetValue(methodNames[i]);
            i = (i + 1) % methodNames.Length;
        }
        methodProgressTimer.Elapsed += (_, _) => ChangeMethodName();
        _logger.Catch(() =>
        {
            var name = VSharpMain.VSharpProcessName;
            var workingDir = project.ProjectFileLocation.Directory.FullPath;
            var port = NetworkUtil.GetFreePort();
            var runnerPath = vsharpRunner.FullPath;
            var proc = new ProcessWithRdServer(name, workingDir, port, runnerPath, project.Locks, _riderModel, _lifetime, _logger);
            var projectCsprojPath = project.ProjectFileLocation.FullPath;
            List<MapEntry> allAssemblies;
            using (_shellLocks.UsingReadLock())
            {
                allAssemblies = solution.GetAllAssemblies()
                    .Where(it => it.Location.AssemblyPhysicalPath is not null)
                    .Select(it => new MapEntry(it.FullAssemblyName, it.Location.AssemblyPhysicalPath!.FullPath))
                    .DistinctBy(it => it.Key)
                    .ToList();
            }
            var args = new GenerateArguments(assemblyPath, projectCsprojPath, solutionFilePath, descriptors,
                timeout, testProjectFramework.FrameworkMoniker.Name, allAssemblies);
            var vSharpTimeout = TimeSpan.FromSeconds(timeout);
            var rpcTimeout = new RpcTimeouts(vSharpTimeout + TimeSpan.FromSeconds(1), vSharpTimeout + TimeSpan.FromSeconds(30));
            ChangeMethodName();
            methodProgressTimer.Start();
            var result = proc.VSharpModel?.Generate.Sync(args, rpcTimeout);
            methodProgressTimer.Stop();
            proc.Proc.WaitForExit();
            _riderModel.StopVSharp.Fire(proc.Proc.ExitCode);
            _logger.Info("Result acquired");
            if (result is { GeneratedProjectPath: not null })
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

                _notifications.ShowInfo(
                    $"Generated {result.TestsCount} tests and found {result.ErrorsCount} errors for {typeName}");

                if (testProjectFramework.IsDefault)
                {
                    _notifications.ShowWarning(
                        $"Generated test project targets {testProjectFramework.FrameworkMoniker}, which is not directly targeted by {project.Name}. " +
                        "Test project may fail to compile due to reference errors");
                }
            }
            else
            {
                var ex = result == null ? "Could not start V#" : result.ExceptionMessage;
                _logger.Info($"Could not generate tests for ${typeName}, exception - {ex}");

                var title = $"Could not generate tests for {typeName}";
                var openExceptionMessageCommand = new UserNotificationCommand(
                    "Show error info",
                    () => MessageBox.ShowError(ex ?? "Cannot get error info", title));
                _notifications.ShowError(title, command: openExceptionMessageCommand);
            }
        });

        methodProgressTimer.Stop();
        _logger.Verbose($"Generation finished for {typeName}");

        _shellLocks.ExecuteOrQueue(_lifetime, "UnitTestBuilder::Generate", () =>
        {
            if (project.IsValid())
                solution.GetProtocolSolution().GetFileSystemModel().RefreshPaths
                    .Start(_lifetime,
                        new RdFsRefreshRequest(new List<string> { solutionMark.Location.FullPath }, true));
        });
    }
}
