using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text.RegularExpressions;
using JetBrains.ProjectModel;
using JetBrains.Util;

namespace UtBot.Utils;

internal readonly record struct TestProjectTargetFramework(FrameworkMoniker FrameworkMoniker, bool IsDefault);

[SolutionComponent]
internal class DotNetVersionUtils
{
    public const int MinCompatibleSdkMajor = 6;
    private const string NUnitProjectMinTfm = "net6.0";

    private readonly Lazy<bool> _canRunVSharp;

    public DotNetVersionUtils(ISolution solution)
    {
        _canRunVSharp = new Lazy<bool>(() => GetCanRunVSharp(solution.SolutionDirectory.FullPath));
    }

    public bool CanRunVSharp => _canRunVSharp.Value;

    public TestProjectTargetFramework GetTestProjectFramework(IProject project)
    {
        var path = project.ProjectFileLocation.Directory.FullPath;

        var nUnitNewInfo = RunProcess(new ProcessStartInfo
        {
            FileName = "dotnet",
            Arguments = "new nunit -f",
            WorkingDirectory = path
        }, returnError: true);

        if (nUnitNewInfo == null)
            throw new Exception("Could not get new NUnit info");

        var matches = DotNetRegex.Any.Matches(nUnitNewInfo);

        if (matches.IsEmpty())
            throw new Exception("Could not parse TFMs from new NUnit info");

        // Code in Allocator.cs uses methods which were added in .NET 6
        var testProjectRequiredFramework = new FrameworkMoniker(NUnitProjectMinTfm);

        var availableTfms = matches
            .Select(m => new FrameworkMoniker(m.Value))
            .Distinct()
            .Where(s => s.CompareTo(testProjectRequiredFramework) >= 0)
            .OrderBy()
            .ToList();

        var projectTfms = GetTfms(project).ToList();

        TestProjectTargetFramework framework;

        var exactMatch = availableTfms.FirstOrDefault(t => projectTfms.Contains(t));

        if (exactMatch is not null)
        {
            return new(exactMatch, false);
        }

        var projectStandards = projectTfms.Where(t => t.Kind is DotNetKind.NetStandard);
        var standardMatch =
            availableTfms.FirstOrDefault(t => projectStandards.Any(t.ImplementsStandard));

        if (standardMatch is not null)
        {
            return new(standardMatch, false);
        }

        var defaultTfm = availableTfms.First();
        return new(defaultTfm, true);
    }

    // TODO: is there a case when --list-runtimes is not available, but runtime is installed?
    private static bool GetCanRunVSharp(string workingDir)
    {
        var sdksInfo = RunProcess(new ProcessStartInfo
        {
            FileName = "dotnet",
            Arguments = "--list-runtimes",
            WorkingDirectory = workingDir
        });

        if (sdksInfo is null)
        {
            return false;
        }

        var matches = Regex.Matches(sdksInfo, @"\.(\S+)\.App (\d+)\.(\d+)\.(\d+)");

        if (matches.Count < 1)
        {
            return false;
        }

        for (var i = 0; i < matches.Count; ++i)
        {
            if (matches[i].Groups[1].Value == "AspNetCore")
            {
                continue;
            }

            if (!int.TryParse(matches[i].Groups[2].Value, out var majorVersion))
            {
                continue;
            }

            if (majorVersion >= MinCompatibleSdkMajor)
            {
                return true;
            }
        }

        return false;
    }

    private IEnumerable<FrameworkMoniker> GetTfms(IProject project) =>
        project.TargetFrameworkIds
            .Select(i => new FrameworkMoniker(i.TryGetShortIdentifier()));

    private static string RunProcess(ProcessStartInfo startInfo, bool returnError = false)
    {
        startInfo.RedirectStandardError = true;
        startInfo.RedirectStandardOutput = true;

        var pi = Process.Start(startInfo);
        var s = returnError ? pi?.StandardError.ReadToEnd() : pi?.StandardOutput.ReadToEnd();
        pi?.WaitForExit();
        return s;
    }
}
