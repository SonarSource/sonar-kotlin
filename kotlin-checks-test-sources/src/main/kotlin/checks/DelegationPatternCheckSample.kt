package checks
class DelegationPatternCheckSample<P>: Z<P>, U<P>, Y() {


    val z: W<P> = TODO()

    fun buf(p: P) { // NO HIT
        z.bee(p)
    }


    override fun bee(p: P) { // HIT
        z.bee(p)
    }

    override fun bumble(p: P):P = z.bumble(p) // HIT

    override fun bumble2(p: P):P { // HIT
        return z.bumble2(p)
    }

    companion object {

    }

}

class Sample2: I1 {

    val i: I2 = TODO()

    override fun foo() { // HIT
        i.foo()
    }
}




interface I1 {
    fun foo()
}

interface I2: I1 {
    fun bar()
}


object X {

}

open class Y0

open class Y: Y0()

interface Z<P> {
    fun bee(p: P)

    fun bumble(p: P): P


}

interface U<Q>: Z<Q> {
    fun bumble2(p: Q): Q
}

interface W<P>: U<P> {
    fun bumble3(p: P): P
}