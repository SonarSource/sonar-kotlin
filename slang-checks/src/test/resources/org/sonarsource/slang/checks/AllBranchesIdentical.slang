
  if (x) { foo; };
  if (x) { foo; } else { bar; };
  if (x) { foo; } else { foo; }; // Noncompliant
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  if (x) { foo; } else if (y) { foo; };

  if (x) { foo; } else if (y) { foo; } else { bar };

  if (x) { foo; } else if (y) { foo; } else { foo }; // Noncompliant
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  if (x) if (y) return foo else return foo else return bar; // Noncompliant
//       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  match (x) { };
  match (x) { 1 -> a; };
  match (x) { 1 -> a; else -> b; };
  match (x) { 1 -> a; else -> a; }; // Noncompliant
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  match (x) { 1 -> a; 2 -> a; else -> b; };
  match (x) { 1 -> a; 2 -> a; else -> a; }; // Noncompliant
