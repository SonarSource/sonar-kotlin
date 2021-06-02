package checks

// Noncompliant@+3
/**
 * KDoc with
 * FIXME something */
// ^^^^^
class FixMeCommentCheckSample {

    // Noncompliant@+1
    // FIXME
//     ^^^^^

    // notafixme comment

    // not2fixme comment

    // a fixmelist

}
