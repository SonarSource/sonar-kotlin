  if (true) {}
//^^>
  else if (true) {}
  else if (true) {
    if (true) { // Compliant
//  ^^>
      if (true) {// Noncompliant {{Refactor this code to not nest more than 2 control flow statements.}}
//    ^^
        if (true) {
        };
      };
    };
  };