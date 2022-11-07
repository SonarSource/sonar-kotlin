package checks

class EqualsOverridenWithHashCodeCheckSample {



    override fun equals(other: Any?): Boolean { // Noncompliant {{This class overrides "equals()" and should therefore also override "hashCode()".}}
        return super.equals(other)
    }

    fun test(){
        println("test")
    }

    class Compliant{
        override fun equals(other: Any?): Boolean {
            return super.equals(other)
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }


    class NonCompliant{
        override fun hashCode(): Int { // Noncompliant
            return super.hashCode()
        }
    }

    data class DC(val x: String){
        override fun hashCode(): Int { // Noncompliant
            return super.hashCode()
        }
    }

    data class DC2(val x: String){
        override fun equals(other: Any?): Boolean { // Noncompliant
            return super.equals(other)
        }
    }

    data class DCCompliant(val x: String){
        override fun equals(other: Any?): Boolean {
            return super.equals(other)
        }
        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

}


