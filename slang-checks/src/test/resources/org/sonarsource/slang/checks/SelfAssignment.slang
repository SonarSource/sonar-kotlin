  x = 1;
  x = x + 1;
  x += x;
  x = x; // Noncompliant {{Remove or correct this useless self-assignment.}}
//^^^^^
  native[] { [x] } = x; // Ex: this.x = x

  // Ex: this.x = this.x
  // Noncompliant@+1
  native[] { [x] } = native[] {
    [
      x;
    ]
  };