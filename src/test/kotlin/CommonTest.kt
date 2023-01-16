import host.skyone.configure.ConfigureLoader
import host.skyone.configure.ConfigureSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.io.File
import java.io.InputStream
import kotlin.test.assertEquals

@Serializable
data class TestData(val id: Int, val name: String)

object TestDataSerializer : ConfigureSerializer<List<TestData>> {
    override val type = "json"

    override val default: Lazy<List<TestData>> = lazy {
        listOf(
            TestData(0, "aaa"),
            TestData(1, "bbb")
        )
    }

    override fun encode(data: List<TestData>) = Json.encodeToString(data).encodeToByteArray().inputStream()

    override fun decode(inputStream: InputStream) =
        Json.decodeFromString<List<TestData>>(inputStream.readAllBytes().decodeToString())
}

class CommonTest {
    @Test
    fun loadNewFile(): Unit = runBlocking {
        val tempDir = "CommonTest-loadNewFile"
        try {
            val testDataLoader = ConfigureLoader(tempDir) {
                loadSync("data", TestDataSerializer)
            }
            val dataProvider = testDataLoader.load("data", TestDataSerializer).await()
            assertEquals(
                dataProvider.current.await(),
                listOf(
                    TestData(0, "aaa"),
                    TestData(1, "bbb")
                )
            )
        } finally {
            File(tempDir).run {
                listFiles()?.forEach { it.delete() }
                delete()
            }
        }
    }

    @Test
    fun updateFromFile(): Unit = runBlocking {
        val tempDir = "CommonTest-updateFromFile"
        try {
            val testDataLoader = ConfigureLoader(tempDir) {
                loadSync("data", TestDataSerializer)
            }
            val dataProvider = testDataLoader.get<List<TestData>>("data")
            File("$tempDir/data.json").writeText(
                """
                    [
                       {
                          "id": 0,
                          "name": "test"
                       }
                    ]
                """.trimIndent()
            )
            assertEquals(
                dataProvider.current.await(),
                listOf(
                    TestData(0, "test")
                )
            )
        } finally {
            File(tempDir).run {
                listFiles()?.forEach { it.delete() }
                delete()
            }
        }
    }
}