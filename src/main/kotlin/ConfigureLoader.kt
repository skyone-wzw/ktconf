package host.skyone.configure

import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.system.exitProcess

/**
 * 配置文件加载器. 一个 [ConfigureLoader] 代表一个配置文件目录
 *
 * 主要通过 [ConfigureLoader.load] 和 [ConfigureLoader.loadSync] 加载配置文件.
 * 一个 [ConfigureLoader] 可以包含多个任意种类的 [Configure]
 *
 * 例如:
 *
 * ```kotlin
 * val configureLoader = ConfigureLoader("data", 30_000L) {
 *     loadSync("settings", SettingsSerializer)
 * }
 * ```
 *
 * @param [baseDir] 配置文件目录名称, 不存在则会创建
 * @param [frequency] 从文件中刷新配置的频率, 单位毫秒(ms), 默认 60_000
 * @param [init] 请看上方例子
 *
 * @see [Configure]
 */
class ConfigureLoader(
    val baseDir: String,
    frequency: Long = 60_000L,
    override val coroutineContext: CoroutineContext = EmptyCoroutineContext,
    init: ConfigureLoader.() -> Unit = {}
) : CoroutineScope {
    val configDir = File(baseDir)
    val configs = HashMap<String, Configure<*>>()
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

    /**
     * 同步的加载一个配置文件, 如果该文件已被加载, 则直接返回
     *
     * [ConfigureLoader.load] 的同步版
     *
     * @throws [ClassCastException] 不会做类型检查, 需要自行保证以后 `load` 的类型与第一次相同
     *
     * @param [name] 配置文件的名称, 即不包含后缀的文件名
     * @param [serializer] 配置文件的序列化工具类
     *
     * @return [Configure] 配置文件对象
     *
     * @see [ConfigureSerializer]
     * @see [Configure]
     * @see [ConfigureLoader.load]
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> loadSync(name: String, serializer: ConfigureSerializer<T>): Configure<T> {
        configs[name]?.let {
            if (it.cache.javaClass.isInstance(T::class.java)) return it as Configure<T>
        }
        return internalLoad(name, serializer)
    }

    /**
     * DO NOT CALL THIS INTERNAL FUNCTION UNLESS YUO KNOW WHAT YOU ARE DOING!
     *
     * @see [loadSync]
     * @see [load]
     */
    fun <T : Any> internalLoad(name: String, serializer: ConfigureSerializer<T>): Configure<T> {
        val configFile = File(configDir, "$name.${serializer.type}")
        if (!configFile.exists()) {
            ConfigureManager.logger.warn("`${baseDir}/$name.${serializer.type}` does not exist, creating")
            configFile.createNewFile()
            configFile.outputStream().also {
                serializer.encode(serializer.default.value).transferTo(it)
            }.close()
        }
        if (configFile.isDirectory) {
            ConfigureManager.logger.error("`${baseDir}/$name` is a directory, expects a file")
            exitProcess(-1)
        }
        if (!(configDir.canRead() && configDir.canWrite())) {
            ConfigureManager.logger.error("No read and write permissions for the `${baseDir}/$name` directory")
            exitProcess(-1)
        }
        return Configure(name, configFile, serializer).also {
            configs[name] = it
        }
    }

    /**
     * 异步的加载一个配置文件
     *
     * [ConfigureLoader.loadSync] 的异步版
     *
     * @throws [ClassCastException] 不会做类型检查, 需要自行保证以后 `load` 的类型与第一次相同
     *
     * @param [name] 配置文件的名称, 即不包含后缀的文件名
     * @param [serializer] 配置文件的序列化工具类
     *
     * @return [Configure] 配置文件对象
     *
     * @see [ConfigureSerializer]
     * @see [Configure]
     * @see [ConfigureLoader.loadSync]
     */
    suspend inline fun <reified T : Any> load(
        name: String,
        serializer: ConfigureSerializer<T>
    ): Deferred<Configure<T>> = withContext(Dispatchers.IO) { async { loadSync(name, serializer) } }

    /**
     * 获得一个已被加载的 [Configure]
     *
     * @param [name] 文件名称
     *
     * @throws [ClassCastException] [T] 与第一次 [load] 不相同
     * @throws [ConfigureNotLoadedException] 配置文件还未被 [load]
     *
     * @return [Configure] 配置文件对象
     */
    @Suppress("UNCHECKED_CAST")
    inline operator fun <reified T : Any> get(name: String): Configure<T> {
        val config = configs[name] ?: throw ConfigureNotLoadedException("$configDir/$name not loaded")
        if (config.cache.javaClass.isInstance(T::class.java))
            throw ClassCastException("config[\"${name}\"] type mismatch: ${config.cache::class.simpleName} to ${T::class.simpleName}")
        return config as Configure<T>
    }

    private class UpdateConfigTask(val target: ConfigureLoader) : TimerTask() {
        override fun run() = target.configs.forEach { it.value.updateSync() }
    }

    class ConfigureNotLoadedException(override val message: String) : Exception(message)
}