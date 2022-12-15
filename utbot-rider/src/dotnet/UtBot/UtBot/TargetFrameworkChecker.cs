#nullable enable
using System;
using System.Diagnostics;
using System.Text.RegularExpressions;

namespace UtBot;

public class FrameworkChecker
{
    private readonly int _majorVersion;

    public FrameworkChecker()
    {
        var dotnetInfo = RunDotnet(new ProcessStartInfo
        {
            Arguments = "--info",
        });
        if (dotnetInfo == null)
            throw new Exception("Could not get dotnet info");

        MatchCollection matches = Regex.Matches(dotnetInfo, @".*?Version:\s*?(\d{1})\.\d{1}\.\d{3}");
        if (matches.Count < 1)
            throw new Exception("Could not find dotnet cli");

        var match = matches[0];
        if (!int.TryParse(match.Groups[1].Value, out _majorVersion))
            throw new Exception("Could not parse dotnet version");
    }

    private static string? RunDotnet(ProcessStartInfo startInfo)
    {
        startInfo.FileName = "dotnet";
        startInfo.RedirectStandardError = true;
        startInfo.RedirectStandardOutput = true;

        var pi = Process.Start(startInfo);
        var s = pi?.StandardOutput.ReadToEnd();
        pi?.WaitForExit();
        return s;
    }

    public bool FrameworkSupportsVSharp()
    {
        return _majorVersion >= 6;
    }

    public bool FrameworkSupportsProject(Version tfm)
    {
        return _majorVersion >= tfm.Major;
    }
}
