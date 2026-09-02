package checks

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

@ComposePreview
fun directPreview(
    first: String = "preview default argument!",
    second: String = "preview default argument!",
    third: String = "preview default argument!",
) {
    println("direct preview body!")
    println("direct preview body!")
    println("direct preview body!")

    val firstFixture = "local preview fixture!"
    val secondFixture = "local preview fixture!"
    val thirdFixture = "local preview fixture!"

    fun localPreviewHelper() {
        println("local preview helper!")
        println("local preview helper!")
        println("local preview helper!")
    }
}

@Composable
fun ordinaryComposable() {
    // Noncompliant@+1
    println("ordinary composable remains in scope!")
    println("ordinary composable remains in scope!")
    println("ordinary composable remains in scope!")
}

@Preview
fun unrelatedPreviewAnnotation() {
    // Noncompliant@+1
    println("unrelated Preview remains in scope!")
    println("unrelated Preview remains in scope!")
    println("unrelated Preview remains in scope!")
}

fun separatelyDeclaredPreviewHelper() {
    // Noncompliant@+1
    println("separate preview helper remains in scope!")
    println("separate preview helper remains in scope!")
    println("separate preview helper remains in scope!")
}

@ComposePreview
fun previewUsingSeparateHelper() {
    separatelyDeclaredPreviewHelper()
}

private val firstPreviewFixture = "separate preview fixture remains in scope!" // Noncompliant
private val secondPreviewFixture = "separate preview fixture remains in scope!"
private val thirdPreviewFixture = "separate preview fixture remains in scope!"
