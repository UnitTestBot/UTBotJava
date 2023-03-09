package org.utbot.taint

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.utbot.common.PathUtil.toPath
import org.utbot.common.utBotTempDirectory
import org.utbot.taint.model.TaintConfiguration
import org.utbot.taint.parser.yaml.TaintYamlParser
import java.io.File

const val TAINT_CONFIGURATION_RESOURCES_PATH = "taint/config.yaml"
const val TAINT_CONFIGURATION_CACHED_DEFAULT_NAME = "taint-config-cached"

/**
 * Provide the [TaintConfiguration].
 */
interface TaintConfigurationProvider {

    /**
     * Returns the time that the configuration was last modified.
     *
     * Uses the same format as the [java.io.File.lastModified].
     */
    fun lastModified(): Long

    /**
     * Provides [TaintConfiguration].
     */
    fun getConfiguration(): TaintConfiguration
}

/**
 * Provides an empty [TaintConfiguration].
 */
class TaintConfigurationProviderEmpty : TaintConfigurationProvider {
    override fun lastModified() = 0L
    override fun getConfiguration() = TaintConfiguration() // no rules
}

/**
 * Reads and parses [TaintConfiguration] from the [configPath].
 *
 * @param configPath relative path to the .yaml file in resources
 */
class TaintConfigurationProviderResources(
    private val configPath: String = TAINT_CONFIGURATION_RESOURCES_PATH
) : TaintConfigurationProvider {

    override fun lastModified(): Long {
        val selfJarFile = File(javaClass.protectionDomain.codeSource.location.toURI().path)
        return selfJarFile.lastModified()
    }

    override fun getConfiguration(): TaintConfiguration {
        val configUrls = javaClass.classLoader.getResources(configPath).toList()
        val configUrl = when (configUrls.size) {
            0 -> return TaintConfiguration()
            1 -> configUrls.single()
            else -> error("Cannot choose between several taint configurations: $configUrls")
        }
        val yamlInput = configUrl.readText()
        val dtoConfig = TaintYamlParser.parse(yamlInput)
        return YamlTaintConfigurationAdapter.convert(dtoConfig)
    }
}

/**
 * Reads and parses [TaintConfiguration] from the [configPath].
 *
 * @param configPath relative path to the .yaml file in the user's project.
 */
class TaintConfigurationProviderUserRules(private val configPath: String) : TaintConfigurationProvider {

    override fun lastModified(): Long {
        val configFile = configPath.toPath().toFile()
        if (!configFile.exists()) {
            return 0
        }
        return configFile.lastModified()
    }

    override fun getConfiguration(): TaintConfiguration {
        val configFile = configPath.toPath().toFile()
        if (!configFile.exists()) {
            return TaintConfiguration()
        }
        val yamlInput = configFile.readText()
        val dtoConfig = TaintYamlParser.parse(yamlInput)
        return YamlTaintConfigurationAdapter.convert(dtoConfig)
    }
}

/**
 * Combines taint configurations from several providers.
 */
class TaintConfigurationProviderCombiner(private val inners: List<TaintConfigurationProvider>) : TaintConfigurationProvider {

    override fun lastModified(): Long {
        return inners.maxOf { it.lastModified() }
    }

    override fun getConfiguration(): TaintConfiguration =
        inners.fold(TaintConfiguration()) { resultConfig, configProvider ->
            resultConfig + configProvider.getConfiguration()
        }
}

/**
 * Stores binary configuration file to the utbot temp directory to reduce parsing time in the future.
 *
 * @param nameSuffix the cached file will have "-$nameSuffix" suffix
 * @param inner provider to cache
 */
class TaintConfigurationProviderCached(
    nameSuffix: String,
    private val inner: TaintConfigurationProvider,
) : TaintConfigurationProvider {

    private val cachedConfigName = "$TAINT_CONFIGURATION_CACHED_DEFAULT_NAME-$nameSuffix"

    private val cachedConfigFile = utBotTempDirectory.resolve(cachedConfigName).toFile()

    override fun lastModified(): Long {
        return cachedConfigFile.lastModified()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun getConfiguration(): TaintConfiguration =
        if (!cachedConfigFile.exists() || cachedConfigFile.lastModified() < inner.lastModified()) {
            val config = inner.getConfiguration()
            val bytes = Cbor.encodeToByteArray(config)
            cachedConfigFile.writeBytes(bytes)
            config
        } else {
            val bytes = cachedConfigFile.readBytes()
            Cbor.decodeFromByteArray(bytes)
        }
}
