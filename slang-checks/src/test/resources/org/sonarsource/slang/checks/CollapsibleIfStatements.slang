if (a) {
  print(a);
  print(b);
};

if (a) {
} else if (b) {
  if (c) {
  }
} else {
};

if (a)
  if (b) {
  } else {
  };

   if (a) { // Noncompliant {{Merge this "if" statement with the nested one.}}
// ^^
     if (b) {}
//   ^^<
    };

if (a) {
} else {
    if (c) { // Noncompliant {{Merge this "if" statement with the nested one.}}
      if (b) {}
    }
};

if (a) // Noncompliant {{Merge this "if" statement with the nested one.}}
 if (b)
    print();


if (a) { // Noncompliant
  if (b) {
  }
};

if (a) { // Noncompliant
  if (b) { // Noncompliant
    if (c) {
    }
  }
};

if (a) { // Noncompliant
  if (b) {
    if (c) {
    } else {
    }
  }
};

if (a) // Noncompliant
  if (b) {
  };

