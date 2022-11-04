package host.skyone.configure

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class Configure<T>(
    val name: String,
    private val file: String,
    private val serializer: ConfigureSerializer<T>,
    override val coroutineContext: CoroutineContext = EmptyCoroutineContext
) : CoroutineScope {
    var cache: T = updateSync()
        private set
    val current
        get() = async { update() }

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

    suspend fun update(new: T? = null): T = withContext(Dispatchers.IO) { updateSync(new) }

    fun checkSync(checker: (T?) -> T?) {
        val current = runCatching {
            return@runCatching updateSync()
        }.getOrNull()
        val data = checker(current) ?: return
        updateSync(data)
    }

    suspend fun check(checker: (T?) -> T?) = withContext(Dispatchers.IO) { checkSync(checker) }
}