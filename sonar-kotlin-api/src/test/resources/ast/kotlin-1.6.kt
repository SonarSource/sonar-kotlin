package ast


@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParamAnnotation
class Box<@TypeParamAnnotation T>


enum class EnumClass { E1, E2 }
sealed class SealedParent {
    class SealedChild1 : SealedParent()
    class SealedChild2 : SealedParent()
}

fun whenTest(boolean: Boolean, enum: EnumClass, sealedParent: SealedParent) {
    when (boolean) {
        true -> "foo"
        false -> "test"
        else -> ""
    }

    when (enum) {
        EnumClass.E1 -> "foo"
        EnumClass.E2 -> "bar"
        else -> ""
    }

    when (sealedParent) {
        is SealedParent.SealedChild1 -> "foo"
        is SealedParent.SealedChild2 -> "bar"
        else -> ""
    }
}

class MyClickAction : suspend () -> Unit {
    override suspend fun invoke() { TODO() }
}

fun getSuspending(suspending: suspend () -> Unit) {}
fun test(regular: () -> Unit, suspending: suspend () -> Unit) {
    getSuspending { }           // OK
    getSuspending(suspending) // OK
    getSuspending(regular)      // OK
}

val containerB = PostgreSQLContainer(DockerImageName.parse("postgres:13-alpine"))
    .withDatabaseName("db")
    .withUsername("user")
    .withPassword("password")
    .withInitScript("sql/schema.sql")
