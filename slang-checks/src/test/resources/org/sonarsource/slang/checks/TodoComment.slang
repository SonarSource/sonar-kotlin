// comment 1

// Noncompliant@+1
// TODO

// Noncompliant@+1
// TODO just do it
// ^^^^

// Noncompliant@+1
// Todo just do it

// Noncompliant@+1
// This is a TODO just do it

// This is not aTODO comment

/*
  Multiline comment
*/

// Noncompliant@+2
/*
  TODO Multiline comment */
//^^^^

// Noncompliant@+2
/*
TODO Multiline comment */

// Noncompliant@+1
//todo comment
//^^^^

// notatodo comment

// not2todo comment

// a todolist

// Noncompliant@+1
// todo: things to do

// Noncompliant@+1
// :TODO: things to do

// Noncompliant@+1
// valid end of line todo

// Noncompliant@+1
// valid end of file todo