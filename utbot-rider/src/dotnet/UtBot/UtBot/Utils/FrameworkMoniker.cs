using System;
using System.Text.RegularExpressions;
using JetBrains.Util;

namespace UtBot.Utils;

internal static class DotNetRegex
{
    public static readonly Regex ModernNet = new(@"net\d+\.\d+");
    public static readonly Regex NetFramework = new(@"net\d+");
    public static readonly Regex NetCore = new(@"netcoreapp(\d+)\.(\d+)");
    public static readonly Regex NetStandard = new(@"netstandard\d+\.\d+");
    public static readonly Regex Any = new(@"net\d+\.\d+|netcoreapp\d+\.\d+|net\d+");
}

internal enum DotNetKind
{
    NetFramework,
    NetCore,
    Net,
    NetStandard,
    Unknown
}

internal class FrameworkMoniker : IComparable<FrameworkMoniker>
{
    public string Name { get; }
    public DotNetKind Kind { get; }

    public FrameworkMoniker(string name)
    {
        // Checking 'netcoreapp6.0' etc. cases --- look like .NET Core, but in fact .NET
        var matches = DotNetRegex.NetCore.Matches(name);
        if (matches.IsEmpty())
        {
            Name = name;
            Kind = GetKind(name);
            return;
        }

        var major = int.Parse(matches[0].Groups[1].Value);
        if (major >= 5)
        {
            var minor = int.Parse(matches[0].Groups[2].Value);
            Name = $"net{major}.{minor}";
            Kind = DotNetKind.Net;
            return;
        }

        Name = name;
        Kind = DotNetKind.NetCore;
    }

    private const string NetStandard10 = "netstandard1.0";
    private const string NetStandard11 = "netstandard1.1";
    private const string NetStandard12 = "netstandard1.2";
    private const string NetStandard13 = "netstandard1.3";
    private const string NetStandard14 = "netstandard1.4";
    private const string NetStandard20 = "netstandard2.0";
    private const string NetStandard21 = "netstandard2.1";

    private static DotNetKind GetKind(string tfm)
    {
        if (DotNetRegex.ModernNet.IsMatch(tfm))
        {
            return DotNetKind.Net;
        }

        if (DotNetRegex.NetFramework.IsMatch(tfm))
        {
            return DotNetKind.NetFramework;
        }

        if (DotNetRegex.NetStandard.IsMatch(tfm))
        {
            return DotNetKind.NetStandard;
        }

        return DotNetRegex.NetCore.IsMatch(tfm) ? DotNetKind.NetCore : DotNetKind.Unknown;
    }

    public int CompareTo(FrameworkMoniker other) =>
        (Kind, other.Kind) switch
        {
            (DotNetKind.NetStandard, not DotNetKind.NetStandard) or
                (not DotNetKind.NetStandard, DotNetKind.NetStandard) =>
                throw new InvalidOperationException("Cannot compare .NET Standard and specific .NET"),

            (DotNetKind.NetFramework, DotNetKind.NetCore) or
                (DotNetKind.NetFramework, DotNetKind.Net) or
                (DotNetKind.NetCore, DotNetKind.Net) => -1,

            (DotNetKind.NetCore, DotNetKind.NetFramework) or
                (DotNetKind.Net, DotNetKind.NetFramework) or
                (DotNetKind.Net, DotNetKind.NetCore) => 1,

            _ => string.Compare(Name, other.Name, StringComparison.InvariantCulture)
        };

    // https://learn.microsoft.com/en-us/dotnet/standard/net-standard?tabs=net-standard-1-5#select-net-standard-version
    public bool ImplementsStandard(FrameworkMoniker standardFrameworkMoniker)
    {
        if (standardFrameworkMoniker.Kind is not DotNetKind.NetStandard)
        {
            throw new ArgumentException("standardTfm is not a .NET Standard TFM");
        }

        var standardName = standardFrameworkMoniker.Name;

        return Kind switch
        {
            DotNetKind.Net => true,
            DotNetKind.NetFramework =>
                Name switch
                {
                    "net45" => standardName is NetStandard10 or NetStandard11,
                    "net451" or "net452" => standardName is NetStandard10 or NetStandard11 or NetStandard12,
                    "net46" => standardName is NetStandard10 or NetStandard11 or NetStandard12 or NetStandard13,
                    "net461" => standardName is NetStandard10 or NetStandard11 or NetStandard12 or NetStandard13
                        or NetStandard14,
                    _ => standardName is not NetStandard21
                },
            DotNetKind.NetCore =>
                Name switch
                {
                    "netcoreapp1.0" or "netcoreapp1.1" => standardName is not (NetStandard20 or NetStandard21),
                    "netcoreapp2.0" or "netcoreapp2.1" or "netcoreapp2.2" => standardName is not NetStandard21,
                    _ => true
                },
            DotNetKind.NetStandard => CompareTo(standardFrameworkMoniker) >= 0,
            _ => false
        };
    }

    public override bool Equals(object obj)
    {
        if (obj is not FrameworkMoniker another)
        {
            return false;
        }

        return Name.Equals(another.Name);
    }

    public override int GetHashCode() => Name.GetHashCode();

    public override string ToString() => Name;
}
