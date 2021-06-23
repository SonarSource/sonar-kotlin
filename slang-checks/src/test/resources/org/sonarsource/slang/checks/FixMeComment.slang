// comment 1

// Noncompliant@+1
// FIXME

// Noncompliant@+1
// FIXME just do it
// ^^^^^

// Noncompliant@+1
// Fixme just do it

// Noncompliant@+1
// This is a FIXME just do it

// This is not aFIXME comment

/*
  Multiline comment
*/

// Noncompliant@+2
/*
  FiXmE Multiline comment */
//^^^^^

// Noncompliant@+2
/*
fixme Multiline comment */

// Noncompliant@+1
//fixme comment
//^^^^^

// notafixme comment

// not2fixme comment

// a fixmelist

// Noncompliant@+1
// fixme: things to do

// Noncompliant@+1
// :fixme: things to do

// Noncompliant@+1
// valid end of line fixme

// Noncompliant@+1
// valid end of file fixme