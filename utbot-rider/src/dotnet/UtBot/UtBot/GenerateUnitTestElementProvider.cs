using JetBrains.Annotations;
using JetBrains.ReSharper.Feature.Services.CSharp.Generate;
using JetBrains.ReSharper.Feature.Services.Generate;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.Resolve;
using JetBrains.ReSharper.Psi.Tree;

namespace UtBot;

[GeneratorElementProvider(GenerateUnitTestWorkflow.Kind, typeof(CSharpLanguage))]
public class GenerateUnitTestElementProvider : GeneratorProviderBase<CSharpGeneratorContext>
{
    public override void Populate(CSharpGeneratorContext context)
    {
        var memberSource = context.ExternalElementsSource?.GetTypeElement() ?? context.ClassDeclaration.DeclaredElement;
        if (memberSource == null) return;

        var substitution = context.ExternalElementsSource?.GetSubstitution() ?? memberSource.IdSubstitution;

        var usageContext = (ITreeNode)context.ClassDeclaration.Body ?? context.ClassDeclaration;

        foreach (var method in memberSource.Methods)
            if (MethodFilter(method, substitution, usageContext))
            {
                var element = new GeneratorDeclaredElement<IMethod>(method, substitution);
                context.ProvidedElements.Add(element);
                context.InputElements.Add(element);
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