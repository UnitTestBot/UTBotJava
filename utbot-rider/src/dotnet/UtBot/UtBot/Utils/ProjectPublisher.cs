using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using JetBrains.Application.Threading.Tasks;
using JetBrains.Application.UI.Controls;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Properties;
using JetBrains.Rider.Model;
using JetBrains.Threading;
using JetBrains.Util;
using UtBot.Rd.Generated;

namespace UtBot.Utils;

public static class ProjectPublisher
{
    public static bool PublishSync(ILogger logger, IBackgroundProgressIndicator indicator, IProject project, IProjectConfiguration config, VirtualFileSystemPath outputDir, UtBotRiderModel model)
    {
        var publishLifetimeDef = indicator.Lifetime.CreateNested();
        indicator.Cancel.Advise(Lifetime.Eternal, canceled => {if (canceled) publishLifetimeDef.Terminate();});

        try
        {
            Directory.CreateDirectory(outputDir.FullPath);
            var projectName = project.ProjectFileLocation.Name;
            var architecture = GetArchitecture();
            var tfm = config.TargetFrameworkId.TryGetShortIdentifier();
            var command = config.TargetFrameworkId.IsNetFramework ? "build" : "publish";

            if (tfm is null)
            {
                throw new ArgumentException("Cannot get framework moniker from project config");
            }

            var processInfo = new ProcessStartInfo
            {
                FileName = "dotnet",
                Arguments =
                    $"{command} \"{projectName}\" --sc -c {config.Name} -a {architecture} -o {outputDir.FullPath} -f {tfm}",
                WorkingDirectory = project.ProjectFileLocation.Directory.FullPath,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };

            using var process = new Process();
            process.StartInfo = processInfo;
            process.OutputDataReceived += (_, args) =>
            {
                model.LogPublishOutput.Fire(args.Data ?? "\n");
            };
            process.ErrorDataReceived += (_, args) =>
            {
                model.LogPublishError.Fire(args.Data ?? "\n");
            };
            process.Exited += (_, a) =>
            {
                publishLifetimeDef.Terminate();
            };
            model.StartPublish.Fire(new StartPublishArgs(processInfo.FileName, processInfo.Arguments, processInfo.WorkingDirectory));
            if (process.Start())
            {
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();
                SpinWaitEx.SpinUntil(publishLifetimeDef.Lifetime, () => process.HasExited);
                if (process.ExitCode != 0)
                {
                    logger.Warn($"Publish process exited with code {process.ExitCode}: {process.StandardOutput.ReadToEnd()}");
                }
                model.StopPublish.Fire(process.ExitCode);
            }
            else
            {
                model.StopPublish.Fire(-1);
            }
            return process.HasExited && process.ExitCode == 0;
        }
        catch (Exception e)
        {
            logger.Warn(e, comment: "Could not publish project for VSharp, exception occured");
            return false;
        }
        finally
        {
            publishLifetimeDef.Terminate();
        }
    }

    private static string GetArchitecture()
    {
        var arch = RuntimeInformation.OSArchitecture;
        return arch switch
        {
            Architecture.X86 => "x86",
            Architecture.X64 => "x64",
            Architecture.Arm => "arm",
            Architecture.Arm64 => "arm64",
            Architecture.Wasm or Architecture.S390x =>
                throw new InvalidOperationException($"Unsupported architecture: {arch}"),
            _ => throw new ArgumentOutOfRangeException()
        };
    }
}
