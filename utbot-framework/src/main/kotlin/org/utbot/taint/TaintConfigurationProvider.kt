package org.utbot.taint

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.utbot.common.PathUtil.toPath
import org.utbot.common.utBotTempDirectory
import org.utbot.taint.model.TaintConfiguration
import org.utbot.taint.parser.TaintYamlParser
import java.io.File

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
 * Reads and parses [TaintConfiguration] from the [configPath].
 *
 * @param configPath relative path to the .yaml file in resources
 */
class TaintConfigurationProviderResources(private val configPath: String) : TaintConfigurationProvider {

    override fun lastModified(): Long {
        val selfJarFile = File(javaClass.protectionDomain.codeSource.location.toURI().path)
        return selfJarFile.lastModified()
    }

    override fun getConfiguration(): TaintConfiguration {
        val configUrl = javaClass.classLoader.getResource(configPath)
            ?: return TaintConfiguration()
        val yamlInput = configUrl.readText()
        val dtoConfig = TaintYamlParser.parse(yamlInput)
        return TaintConfigurationAdapter.convert(dtoConfig)
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
        return TaintConfigurationAdapter.convert(dtoConfig)
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

    private val cachedConfigName = "taint-config-cached-$nameSuffix"

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

fun constructDefaultProvider(
    taintUserConfigPath: String?,
    taintResourcesConfigPath: String = "taint/config.yaml"
): TaintConfigurationProvider {
    val resourcesConfig = TaintConfigurationProviderResources(taintResourcesConfigPath)
    val cachedResourcesConfig = TaintConfigurationProviderCached("resources", resourcesConfig)

    val cachedUserConfigOrNull = taintUserConfigPath?.let {
        val userConfig = TaintConfigurationProviderUserRules(taintUserConfigPath)
        TaintConfigurationProviderCached("user", userConfig)
    }

    return TaintConfigurationProviderCombiner(listOfNotNull(cachedUserConfigOrNull, cachedResourcesConfig))
}
