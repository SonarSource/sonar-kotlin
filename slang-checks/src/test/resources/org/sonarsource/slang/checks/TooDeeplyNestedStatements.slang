if (false) { // Compliant
};

  if (true) {}
//^^>
  else if (true) {}
  else if (true) {
    if (true) { // Compliant
//  ^^>
      if (true) {
//    ^^>
        if (true) { // Noncompliant {{Refactor this code to not nest more than 3 control flow statements.}}
//      ^^
        };
      };
    };
  };

if (false) { // Compliant
  if (true) { // Compliant
  } else {
    if (false) { // Compliant
      if (true) { // Noncompliant {{Refactor this code to not nest more than 3 control flow statements.}}
        if (false) { // Compliant
        };
      } else if (true) { // Compliant
      } else {
        if (false) { // Compliant
        };
      };
    };
  };
};

if (false) {        // Compliant
} else if (false) { // Compliant
} else if (false) { // Compliant
} else if (false) { // Compliant
} else if (false) { // Compliant
};

if (false) // Compliant
  if (false) // Compliant
    if (false) // Compliant
      if (true) // Noncompliant
        println();

  for (int var x = list) {      // Compliant
//^^^>
    for (int var o = objects) { // Compliant
//  ^^^>
      while (false) {           // Compliant
//    ^^^^^>
        for (int var p = list) { // Noncompliant
        };

        while (false){ // Noncompliant
        };

        do { } while (false); // Noncompliant

        if (false) { // Noncompliant
        };

        match (p) { // Noncompliant
//      ^^^^^
        };

        try {  // Noncompliant

        } catch () { };

        x = if (false) 1 else 2;
        x = if (false) { // Noncompliant
          foo();
          1
        } else 2;
    };

    x = if (condition) if (x == 0) 1 else 2 else 3;
    x = if (condition) (if (x == 0) 1 else 2) else 3;
    if (if (x == 0) a() else b()) {
    };

    if (condition) if (x == 0) a() else b(); // Noncompliant

    for (int var o = objects) if (x == 0) 1 else 2; // Noncompliant

    for (int var x = if (x == 0) 1 else 2)  {
    };

    while (if (x == 0) false else true)  {
    };

    match (if (x == 0) 1 else 2) {
        1    -> if (x == 0) 1 else 2; // Noncompliant
        else -> if (x == 0) 1 else 2; // Noncompliant
    };

  };
};


while (true) {
 if (true) {
 } else if (true) {
 } else {
   if (true) {
     if (true) { // Noncompliant
     };
   };
 };
};
