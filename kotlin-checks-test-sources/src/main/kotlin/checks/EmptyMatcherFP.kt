package checks


class EmptyMatcherFP private constructor(
    private val privateName: String,
    severity: Int = 0
) {

    companion object {
        val SMARTCAST = EmptyMatcherFP("SMARTCAST", 1)
        val IMPLICIT_RECEIVER_SMARTCAST =
            EmptyMatcherFP("IMPLICIT_RECEIVER_SMARTCAST", 1)
        val CONSTANT = EmptyMatcherFP("CONSTANT", 1)
        val LEAKING_THIS = EmptyMatcherFP("LEAKING_THIS", 1)
        val IMPLICIT_EXHAUSTIVE =
            EmptyMatcherFP("IMPLICIT_EXHAUSTIVE", 1)
        val ELEMENT_WITH_ERROR_TYPE = EmptyMatcherFP("ELEMENT_WITH_ERROR_TYPE")
        val UNRESOLVED_WITH_TARGET = EmptyMatcherFP("UNRESOLVED_WITH_TARGET")
        val MISSING_UNRESOLVED = EmptyMatcherFP("MISSING_UNRESOLVED")
        val DYNAMIC = EmptyMatcherFP("DYNAMIC", 1)
    }

}