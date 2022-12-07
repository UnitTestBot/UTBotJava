using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.UnitTestFramework;
using JetBrains.Rider.Model;

namespace UtBot;

[ZoneDefinition(ZoneFlags.AutoEnable)]
public interface IUtBotPluginZone :
    IZone,
    IRequire<IProjectModelZone>,
    IRequire<IRiderModelZone>,
    IRequire<ILanguageCSharpZone>,
    IRequire<IUnitTestingZone>
{
}

[ZoneMarker]
public class ZoneMarker : IRequire<IUtBotPluginZone>
{
}