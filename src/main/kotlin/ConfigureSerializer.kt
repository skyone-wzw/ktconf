package host.skyone.configure

import java.io.InputStream

interface ConfigureSerializer<T> {
    val type: String
    val default: Lazy<T>
    fun encode(data: T): InputStream
    fun decode(inputStream: InputStream): T
}