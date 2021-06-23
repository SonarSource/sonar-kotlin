int fun (p1, p2) {}

// short comment

// Noncompliant@+1 {{Split this 124 characters long line (which is greater than 120 authorized).}}
int fun fooVeryLongName(fooVeryLongName1, fooVeryLongName2, fooVeryLongName3, fooVeryLongName4, fooVeryLongName5, p6, p7) {}

// Noncompliant@+1 {{Split this 125 characters long line (which is greater than 120 authorized).}}
// this is a very comment that should raise an issue when its length is greater that 120 characters that is the default value