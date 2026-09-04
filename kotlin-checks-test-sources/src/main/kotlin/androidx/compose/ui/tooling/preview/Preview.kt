package androidx.compose.ui.tooling.preview

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Repeatable
annotation class Preview(val name: String = "")
