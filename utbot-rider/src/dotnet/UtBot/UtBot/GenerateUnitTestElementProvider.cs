using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.ReSharper.Feature.Services.CSharp.Generate;
using JetBrains.ReSharper.Feature.Services.Generate;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.Resolve;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;

namespace UtBot;

internal class TimeoutGeneratorOption : IGeneratorOption
{
    public static readonly string Id = "TimeoutGeneratorOption";
    private const string InitialValue = "10";

    public IReadOnlyList<string> GetPossibleValues() => new string[] { };

    public bool IsValidValue(string value) =>
        value.IsNullOrEmpty() || (int.TryParse(value, out var intValue) && intValue > 0);

    public string ID => Id;

    public string Title => "Timeout (s) for all selected methods";

    public GeneratorOptionKind Kind => GeneratorOptionKind.Text;

    public bool Persist => false;

    public string Value { get; set; } = InitialValue;

    public bool OverridesGlobalOption { get; set; } = false;

    public bool HasDependentOptions => false;
}

[GeneratorElementProvider(GenerateUnitTestWorkflow.Kind, typeof(CSharpLanguage))]
internal class GenerateUnitTestElementProvider : GeneratorProviderBase<CSharpGeneratorContext>
{
    public override void Populate(CSharpGeneratorContext context)
    {
        context.Options.Add(new TimeoutGeneratorOption());

        var memberSource = context.ExternalElementsSource?.GetTypeElement() ?? context.ClassDeclaration.DeclaredElement;
        if (memberSource == null) return;

        var substitution = context.ExternalElementsSource?.GetSubstitution() ?? memberSource.IdSubstitution;

        var usageContext = (ITreeNode)context.ClassDeclaration.Body ?? context.ClassDeclaration;

        foreach (var method in memberSource.Methods)
        {
            if (MethodFilter(method, substitution, usageContext))
            {
                var element = new GeneratorDeclaredElement<IMethod>(method, substitution);
                context.ProvidedElements.Add(element);
                context.InputElements.Add(element);
            }
        }
    }

    protected virtual bool MethodFilter([NotNull] IMethod method, ISubstitution substitution,
        [NotNull] ITreeNode context)
    {
        if (method.IsSynthetic()) return false;
        if (method.GetAccessRights() != AccessRights.PUBLIC) return false;
        return true;
    }
}
