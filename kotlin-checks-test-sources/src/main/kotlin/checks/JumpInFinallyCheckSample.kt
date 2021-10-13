package checks

class JumpInFinallyCheckSample {

    class NonCompliant {

        fun returnInFinally() {
            try {
                throw RuntimeException()
            } finally {
                return  // Noncompliant {{Remove this return statement from this finally block.}}
            //  ^^^^^^
            }
        }

        fun noDoubleReporting() {
            try {
                throw RuntimeException()
            } finally {
                try {
                    println("Inside nested try-finally")
                } finally {
                    return  // Noncompliant {{Remove this return statement from this finally block.}}
                //  ^^^^^^
                }
            }
        }

        fun returnToOuter() {
            (0..100).forEach outer@{
                try {
                    throw RuntimeException()
                } finally {
                    (0..it).forEach inner@{
                        return@outer // Noncompliant {{Remove this return statement from this finally block.}}
                    //  ^^^^^^
                    }
                }
            }
        }

        fun returnInLambdaInFinally() {
            while (true) {
                try {
                    throw RuntimeException()
                } finally {
                    listOf(1, 2, 3).forEach {
                        if (it == 0) return // Noncompliant {{Remove this return statement from this finally block.}}
                    //               ^^^^^^
                    }
                }
            }
        }

        fun throwInFinally() {
            try {
                throw RuntimeException()
            } finally {
                throw RuntimeException() // Noncompliant {{Remove this throw statement from this finally block.}}
            //  ^^^^^
            }
        }

        fun breakInFinally() {
            while (true) {
                try {
                    throw RuntimeException()
                } finally {
                    break // Noncompliant {{Remove this break statement from this finally block.}}
                //  ^^^^^
                }
            }

            do {
                try {
                    throw RuntimeException()
                } finally {
                    break // Noncompliant {{Remove this break statement from this finally block.}}
                //  ^^^^^
                }
            } while (true)

            for (ignored in listOf(1, 2, 3)) {
                try {
                    throw RuntimeException()
                } finally {
                    break // Noncompliant {{Remove this break statement from this finally block.}}
                //  ^^^^^
                }
            }

            outer@ while (true) {
                try {
                    throw RuntimeException()
                } finally {
                    inner@ do {
                        break@outer // Noncompliant {{Remove this break statement from this finally block.}}
                    //  ^^^^^
                    } while (true)
                }
            }
        }

        fun continueInFinallyInWhile() {
            while (true) {
                try {
                    throw RuntimeException()
                } finally {
                    continue // Noncompliant {{Remove this continue statement from this finally block.}}
                //  ^^^^^^^^
                }
            }
        }

        fun continueInFinallyInDoWhile() {
            do {
                try {
                    throw RuntimeException()
                } finally {
                    continue // Noncompliant {{Remove this continue statement from this finally block.}}
                //  ^^^^^^^^
                }
            } while (true)
        }

        fun continueInFinallyInFor() {
            for (ignored in listOf(1, 2, 3)) {
                try {
                    throw RuntimeException()
                } finally {
                    continue // Noncompliant {{Remove this continue statement from this finally block.}}
                //  ^^^^^^^^
                }
            }
        }

        fun continueToOuterLabelInFinally() {
            outer@ while (true) {
                try {
                    throw RuntimeException()
                } finally {
                    inner@ do {
                        continue@outer // Noncompliant {{Remove this continue statement from this finally block.}}
                    //  ^^^^^^^^
                    } while (true)
                }
            }
        }
    }

    class Compliant {
        fun breakInForInFinally() {
            try {
                throw RuntimeException()
            } finally {
                for (ignored in listOf(1, 2, 3)) {
                    break // Compliant
                }
            }
        }

        fun breakInDoWhileInFinally() {
            try {
                throw RuntimeException()
            } finally {
                do {
                    break // Compliant
                } while (true)
            }
        }

        fun breakInWhileInFinally() {
            try {
                throw RuntimeException()
            } finally {
                while (true) {
                    break // Compliant
                }
            }
        }

        fun breakToLabelInFinally() {
            while (true) {
                try {
                    throw RuntimeException()
                } finally {
                    inner@ do {
                        break@inner // Compliant
                    } while (true)
                }
            }
        }

        fun continueInForInFinally() {
            try {
                throw RuntimeException()
            } finally {
                for (ignored in listOf(1, 2, 3)) {
                    continue // Compliant
                }
            }
        }

        fun continueInDoWhileInFinally() {
            try {
                throw RuntimeException()
            } finally {
                do {
                    continue // Compliant
                } while (true)
            }
        }

        fun continueInWhileInFinally() {
            try {
                throw RuntimeException()
            } finally {
                while (true) {
                    continue // Compliant
                }
            }
        }

        fun continueToInnerLabelInFinally() {
            while (true) {
                try {
                    throw RuntimeException()
                } finally {
                    inner@ do {
                        continue@inner // Compliant
                    } while (true)
                }
            }
        }

        fun returnInLambdaInFinally() {
            while (true) {
                try {
                    throw RuntimeException()
                } finally {
                    listOf(1, 2, 3).forEach {
                        return@forEach // Compliant
                    }

                    listOf(1, 2, 3).map {
                        (0..it).filter { return@map }  // Compliant
                    }

                    listOf(1, 2, 3).map {
                        (0..it).filter { return@filter true }  // Compliant
                    }
                }
            }
        }

        fun returnToInner() {
            (0..100).forEach outer@{
                try {
                    throw RuntimeException()
                } finally {
                    (0..it).forEach inner@{
                        return@inner  // Compliant
                    }
                }
            }
        }

        fun returnInNamedFunction() {
            try {
                throw RuntimeException()
            } finally {
                fun sayHelloAndReturn() {
                    print("Hello, World!")
                    return // Compliant
                }
            }
        }

        fun throwInNamedFunction() {
            try {
                throw RuntimeException()
            } finally {
                fun sayHelloAndThrow() {
                    print("Hello, World!")
                    throw IllegalArgumentException() // Compliant
                }
            }
        }
    }
}
