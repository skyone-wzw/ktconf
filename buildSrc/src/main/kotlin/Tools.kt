import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.attributes

fun Jar.makeManifast() {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}