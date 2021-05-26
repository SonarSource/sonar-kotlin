package checks

class SelfAssignmentCheckSample {

    var v = ""
    var w = ""
    fun f() {
        var x:Int = 0
        var w = ""
        x = 1
        x = x + 1;
        x += x;
        x = x; // Noncompliant {{Remove or correct this useless self-assignment.}}
//      ^^^^^


        this.v = this.v // Noncompliant {{Remove or correct this useless self-assignment.}}
        this.v = v // Compliant, 'v' could be local variable
        this.v = this.w
        this.v = w
        this.w = w

    }
}
