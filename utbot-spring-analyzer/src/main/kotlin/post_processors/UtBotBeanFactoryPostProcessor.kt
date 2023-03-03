package post_processors

import org.springframework.beans.BeansException
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.core.PriorityOrdered

import java.io.File
import java.io.FileWriter
import java.util.Arrays

class UtBotBeanFactoryPostProcessor : BeanFactoryPostProcessor, PriorityOrdered {

    @Throws(BeansException::class)
    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {

        println("Started post-processing bean factory in UtBot")

        val beanClassNames = ArrayList<String>()
        for (beanDefinitionName in beanFactory.beanDefinitionNames) {
            val beanDefinition = beanFactory.getBeanDefinition(beanDefinitionName)

            if (beanDefinition is AnnotatedBeanDefinition) {
                val factoryMethodMetadata = beanDefinition.factoryMethodMetadata
                if (factoryMethodMetadata != null) {
                    beanClassNames.add(factoryMethodMetadata.returnTypeName)
                }
            } else {
                var className = beanDefinition.beanClassName
                if (className == null) {
                    className = beanFactory.getBean(beanDefinitionName).javaClass.name
                }
                className?.let { beanClassNames.add(it) }
            }
        }
        for (beanDefinitionName in beanFactory.beanDefinitionNames) {
            val beanRegistry = beanFactory as BeanDefinitionRegistry
            beanRegistry.removeBeanDefinition(beanDefinitionName)
        }

        writeToFile(beanClassNames)
    }

    private fun writeToFile(beanClassNames: ArrayList<String>) {
        try {
            val springBeansFile = File("SpringBeans.txt")
            val fileWriter = FileWriter(springBeansFile)

            val distinctClassNames = beanClassNames.stream()
                .distinct()
                .toArray()
            Arrays.sort(distinctClassNames)

            for (beanClassName in distinctClassNames) {
                fileWriter.append(beanClassName.toString())
                fileWriter.append("\n")
            }

            fileWriter.flush()
            fileWriter.close()
        } catch (e: Throwable) {
            println("Storing bean information failed")
        } finally {
            println("Finished post-processing bean factory in UtBot")
        }
    }

    override fun getOrder(): Int {
        return PriorityOrdered.HIGHEST_PRECEDENCE
    }
}