package checks

import org.springframework.web.bind.annotation.RequestMapping

class TooManyParametersKotlinCheck {

    fun oneParam(s: String) { } // Compliant
    fun manyParam(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Noncompliant

    @org.springframework.web.bind.annotation.RequestMapping
    fun annotated1(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant
    @RequestMapping
    fun annotated1(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant
    @GetMapping
    fun annotated2(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant
    @PostMapping
    fun annotated3(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant
    @PutMapping
    fun annotated4(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant
    @DeleteMapping
    fun annotated5(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant
    @PatchMapping
    fun annotated6(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant
    @JsonCreator
    fun annotated7(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant

    @JsonCreator
    @SomethingElse
    fun annotated8(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant

    @SomethingElse
    @JsonCreator
    fun annotated9(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant

    @my.own.pack.JsonCreator
    fun annotated10(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Compliant, FN, unlikely

    @JSONCREATOR
    fun annotated11(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Noncompliant

    @SomethingElse
    fun annotated12(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String) { } // Noncompliant

}
