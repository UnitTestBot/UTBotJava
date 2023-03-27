using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Properties;
using JetBrains.Threading;
using JetBrains.Util;

namespace UtBot.Utils;

[SolutionComponent]
public class ProjectPublisher
{
    private readonly LifetimeDefinition _lifetimeDef;

    public ProjectPublisher(Lifetime lifetime)
    {
        _lifetimeDef = lifetime.CreateNested();
    }

    public void PublishSync(IProject project, IProjectConfiguration config, VirtualFileSystemPath outputDir)
    {
        var publishLifetimeDef = _lifetimeDef.Lifetime.CreateNested();

        try
        {
            var name = $"Publish::{project.Name}";
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
                RedirectStandardError = true,
                RedirectStandardOutput = true
            };

            var process = new Process();
            process.StartInfo = processInfo;
            process.Exited += (_, a) =>
            {
                if (process.ExitCode != 0)
                {
                    throw new Exception(
                        $"Publish process exited with code {process.ExitCode}: {process.StandardOutput.ReadToEnd()}");
                }

                publishLifetimeDef.Terminate();
            };

            void Schedule(SingleThreadScheduler scheduler)
            {
                process.Start();
                process.WaitForExit();
            }

            SingleThreadScheduler.RunOnSeparateThread(publishLifetimeDef.Lifetime, name, Schedule);
            SpinWaitEx.SpinUntil(publishLifetimeDef.Lifetime, () => process.HasExited);
        }
        catch (Exception e)
        {
            throw;
        }
        finally
        {
            publishLifetimeDef.Terminate();
        }
    }

    private string GetArchitecture()
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
