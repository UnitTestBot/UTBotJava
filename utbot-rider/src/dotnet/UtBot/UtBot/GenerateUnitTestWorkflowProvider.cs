using System.Collections.Generic;
using JetBrains.Application.DataContext;
using JetBrains.ReSharper.Feature.Services.Generate.Actions;

namespace UtBot;

[GenerateProvider]
public class GenerateUnitTestWorkflowProvider : IGenerateWorkflowProvider
{
    public IEnumerable<IGenerateActionWorkflow> CreateWorkflow(IDataContext dataContext)
    {
        return new[] { new GenerateUnitTestWorkflow() };
    }
}
