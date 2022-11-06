package host.skyone.configure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class ConfigureLoader(val baseDir: String, frequency: Long = 60_000L, init: ConfigureLoader.() -> Unit = {}) {
    private val configDir = File(baseDir)
    private val configs = HashMap<String, Configure<*>>()
    private val timer = Timer()

    init {
        if (!configDir.exists()) {
            ConfigureManager.logger.warn("`${baseDir}` does not exist, creating")
            configDir.mkdir()
        }
        if (configDir.isFile) {
            ConfigureManager.logger.error("`${baseDir}` is a file, expects a directory")
            exitProcess(-1)
        }
        if (!(configDir.canRead() && configDir.canWrite())) {
            ConfigureManager.logger.error("No read and write permissions for the `${baseDir}` directory")
            exitProcess(-1)
        }
        if (frequency > 0) {
            timer.schedule(UpdateConfigTask(this), frequency, frequency)
        }

        this.apply(init)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> load(
        name: String,
        serializer: ConfigureSerializer<T>
    ): Configure<T> {
        if (configs[name] != null) {
            return configs[name] as Configure<T>
        }
        val configFile = File(configDir, "$name.${serializer.type}")
        if (!configFile.exists()) {
            ConfigureManager.logger.warn("`${baseDir}/$name` does not exist, creating")
            withContext(Dispatchers.IO) {
                configFile.createNewFile()
                configFile.outputStream().also {
                    serializer.encode(serializer.default.value).transferTo(it)
                }.close()
            }
        }
        if (configFile.isDirectory) {
            ConfigureManager.logger.error("`${baseDir}/$name` is a directory, expects a file")
            exitProcess(-1)
        }
        if (!(configDir.canRead() && configDir.canWrite())) {
            ConfigureManager.logger.error("No read and write permissions for the `${baseDir}/$name` directory")
            exitProcess(-1)
        }
        return Configure(name, configFile.absolutePath, serializer).also {
            configs[name] = it
        }
    }

    fun <T>loadSync(
        name: String,
        serializer: ConfigureSerializer<T>
    ) = runBlocking { load(name, serializer) }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(name: String): Configure<T> {
        val config = configs[name] ?: throw ConfigureNotLoadedException("$configDir/$name not loaded")
        return config as Configure<T>
    }

    private class UpdateConfigTask(val target: ConfigureLoader) : TimerTask() {
        override fun run() = target.configs.forEach { it.value.updateSync() }
    }
    class ConfigureNotLoadedException(override val message: String) : Exception(message)
}