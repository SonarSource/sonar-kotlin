package checks

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.jvm.Throws

class UnusedPrivateMethodKotlinCheckTest {
    
    @Suppress
    private fun withAnnotation1() { // Noncompliant 
        
    }

    @OptIn
    private fun withAnnotation2() { // Noncompliant 
        
    }
    
    @DelicateCoroutinesApi
    private fun withAnnotation3() { // Noncompliant 
        
    }
    
    @Throws
    private fun withAnnotation4() { // Noncompliant 
        
    }
    
    @MyCustom
    private fun withAnnotation5() { // Compliant 
        
    }
    
    @MyCustom
    @Throws
    private fun withAnnotation6() { // Compliant 
        
    }
    
    private fun unused() { // Noncompliant {{Remove this unused private "unused" method.}}
//              ^^^^^^
    }

    private fun used() {
        ::used
    }

    private operator fun plus(p: Int) {
        val x = fun () {} // Anonymous
    }


    private infix fun usedInfix(p: Int) = p // Compliant, used as infix function
    init {
        this usedInfix 1
    }

    fun publicUnusedFun() {
    }

    private fun String.unusedExtension() { // Noncompliant {{Remove this unused private "unusedExtension" method.}}
    }

    // Serializable method should not raise any issue in Kotlin.
    private fun writeObject() { } // Compliant
    private fun readObject() { } // Compliant
    private fun writeReplace() { } // Compliant
    private fun readResolve() { } // Compliant
    private fun readObjectNoData() { } // Compliant

    class Inner {
        private fun unused() { // Noncompliant
        }
    }

}

annotation class MyCustom {
    
}
