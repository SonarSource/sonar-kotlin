package checks

// Noncompliant@+3
/**
 * KDoc with
 * TODO something */
// ^^^^
class TodoCommentCheckSample {

    // Noncompliant@+1
    // TODO something
//     ^^^^

    // notatodo comment

    // not2todo comment

    // a todolist

}
