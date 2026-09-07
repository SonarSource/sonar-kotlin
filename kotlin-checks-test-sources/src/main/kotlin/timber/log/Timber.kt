package timber.log

class Timber private constructor() {

    abstract class Tree {
        open fun v(message: String) = Unit
        open fun d(message: String) = Unit
        open fun i(message: String) = Unit
        open fun w(message: String) = Unit
        open fun e(message: String) = Unit
        open fun wtf(message: String) = Unit
    }

    companion object Forest : Tree() {
        fun tag(tag: String): Tree = this
    }
}
