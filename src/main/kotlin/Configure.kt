package host.skyone.configure

import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 配置文件对象. 一个 [Configure] 代表一个配置文件.
 *
 * 包含配置文件的 名称, 位置, [ConfigureSerializer]. 由 [ConfigureLoader] 构造.
 *
 * 首次加载时使用 [ConfigureLoader.load] 获得. 例如
 *
 * ```kotlin
 * val configureLoader = ConfigureLoader("config")
 * val config = configureLoader.load("data", DataSerializer)
 * ```
 *
 * 已经加载过的配置也可以通过 [ConfigureLoader.get] 获得. 例如
 *
 * ```kotlin
 * val configureLoader = ConfigureLoader("config")
 * val config = configureLoader.get<Data>("data")
 * ```
 *
 * @param [name] 配置名称
 * @param [file] 文件的绝对位置
 * @param [serializer] 此配置对应的序列化类
 *
 * @property [cache] 缓存的配置
 * @property [current] 立即从文件获取配置
 *
 * @see [ConfigureLoader]
 */
class Configure<T>(
    val name: String,
    private val file: String,
    private val serializer: ConfigureSerializer<T>,
    override val coroutineContext: CoroutineContext = EmptyCoroutineContext
) : CoroutineScope {
    /**
     * 缓存的配置, 由 [ConfigureLoader] 定时刷新
     *
     * @see [ConfigureLoader.UpdateConfigTask]
     */
    var cache: T = updateSync()
        private set

    /**
     * 立即刷新当前配置文件并返回其值
     *
     * @see [Configure.update]
     */
    val current
        get() = async { updateSync() }

    /**
     * 同步的将新配置写入文件
     *
     * [update] 的同步版
     *
     * @param [new] 新的配置值, 为 `null` 则仅从文件读取配置
     *
     * @return [T] 更新后的配置值
     *
     * @see [update]
     */
    fun updateSync(new: T? = null): T {
        val file = File(file)
        return if (new == null) {
            ConfigureManager.logger.debug("Load data from `$file`")
            val input = file.inputStream()
            serializer.decode(input).also {
                cache = it
                input.close()
            }
        } else {
            ConfigureManager.logger.debug("Save data to `$file`")
            file.delete()
            val output = file.outputStream()
            serializer.encode(new).also { it.transferTo(output) }
            output.close()
            cache = new
            new
        }
    }

    /**
     * 异步的将新配置写入文件
     *
     * [updateSync] 的异步版
     *
     * @param [new] 新的配置值, 为 `null` 则仅从文件读取配置
     *
     * @return [T] 更新后的配置值
     *
     * @see [updateSync]
     */
    suspend fun update(new: T? = null): Deferred<T> =
        withContext(Dispatchers.IO) { async { updateSync(new) } }

    /**
     * 同步的检查配置文件是否正确
     *
     * [check] 的同步版
     *
     * @param [checker] 检查配置的值并返回正确的值, 如果配置正确, 则返回 `null`
     *
     * @see [check]
     */
    fun checkSync(checker: (T?) -> T?) {
        val current = runCatching {
            return@runCatching updateSync()
        }.getOrNull()
        val data = checker(current) ?: return
        updateSync(data)
    }

    /**
     * 异步的检查配置文件是否正确
     *
     * [checkSync] 的异步版
     *
     * @param [checker] 检查配置的值并返回正确的值, 如果配置正确, 则返回 `null`
     *
     * @return [Job]
     *
     * @see [checkSync]
     */
    suspend fun check(checker: (T?) -> T?): Job =
        withContext(Dispatchers.IO) { launch { checkSync(checker) } }
}