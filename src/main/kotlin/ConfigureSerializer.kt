package host.skyone.configure

import java.io.InputStream

/**
 * 配置文件序列化工具类
 *
 * 一般被 `object` 单例类实现
 *
 * ```kotlin
 * @Serializable
 * data class TestData(val id: Int, val name: String)
 *
 * object TestDataSerializer : ConfigureSerializer<List<TestData>> {
 *     override val type = "json"
 *     override val default: Lazy<List<TestData>> = lazy {
 *         listOf(
 *             TestData(0, "aaa"),
 *             TestData(1, "bbb")
 *         )
 *     }
 *     override fun encode(data: List<TestData>) = Json.encodeToString(data).encodeToByteArray().inputStream()
 *     override fun decode(inputStream: InputStream) =
 *         Json.decodeFromString<List<TestData>>(inputStream.readAllBytes().decodeToString())
 * }
 * ```
 *
 * @property [type] 配置文件后缀, 例如 `json`, `yml`
 * @property [default] 默认值, 一般在首次创建配置文件时使用
 */
interface ConfigureSerializer<T> {
    val type: String
    val default: Lazy<T>
    fun encode(data: T): InputStream
    fun decode(inputStream: InputStream): T
}