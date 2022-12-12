using JetBrains.ReSharper.Feature.Services.Generate.Actions;
using JetBrains.ReSharper.Feature.Services.Generate.Workflows;
using JetBrains.ReSharper.Psi.Resources;

namespace UtBot;

public class GenerateUnitTestWorkflow : GenerateCodeWorkflowBase
{
    public const string Kind = "UnitTest";

    public GenerateUnitTestWorkflow() : base(
        Kind,
        PsiSymbolsThemedIcons.SymbolUnitTest.Id,
        "Tests with UnitTestBot",
        GenerateActionGroup.CLR_LANGUAGE,
        "Generate tests with UnitTestBot",
        "Select methods for generation",
        "Generate.UnitTest")
    {
    }

    public override double Order => 10;
}