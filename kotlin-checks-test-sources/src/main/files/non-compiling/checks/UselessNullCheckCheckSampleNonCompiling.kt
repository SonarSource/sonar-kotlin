package checks

class UselessNullCheckCheckSampleNonCompiling : UnresolvedBaseClass {
    fun example() {
        if (unresolvedBaseClassProperty == null) {
        }
    }
}
