package org.utbot.framework.codegen.model

import org.utbot.framework.codegen.CodeGeneratorService
import org.utbot.framework.codegen.TestCodeGenerator

class ModelBasedCodeGeneratorService : CodeGeneratorService {
    override val displayName: String = "Model based code generator"
    override val serviceProvider: TestCodeGenerator = ModelBasedTestCodeGenerator()
}