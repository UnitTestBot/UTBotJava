using JetBrains.Application.UI.ActionsRevised.Menu;
using JetBrains.ReSharper.Feature.Services.Generate.Actions;
using JetBrains.ReSharper.Resources.Resources.Icons;
using JetBrains.UI.RichText;

namespace UtBot;

[Action("Generate.UnitTest", "Generate Unit Test", Icon = typeof(PsiFeaturesUnsortedThemedIcons.FuncZoneGenerate))]
internal class GenerateUnitTestAction : GenerateActionBase<GenerateUnitTestWorkflowProvider>
{
    protected override RichText Caption => "Generate Unit Test";
}