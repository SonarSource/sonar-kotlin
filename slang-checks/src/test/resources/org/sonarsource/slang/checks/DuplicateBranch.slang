  if (x) {
    foo;
    foo;
  };

  if (x) {
    foo;
    foo;
  } else {
    bar;
    bar;
  };

  if (x) { // handled by S3923
    foo;
    foo;
  } else {
    foo;
    foo;
  };

  if (x) {
    foo;
    foo;
  } else if (y) { // Noncompliant {{This branch's code block is the same as the block for the branch on line 22.}}
//              ^[el=+4;ec=3]
    foo;
    foo;
  } else {
    bar;
    bar;
  };

  if (x) {
    foo;
    foo;
  } else if (y) {
    bar;
    bar;
  } else { // Noncompliant
    bar;
    bar;
  };

  if (x) {

  } else if (y) {

  } else {
    bar; bar;
  };

  if (x) {
    foo; foo;
  } else if (y) {
    foo; foo;
  } else {
    bar; bar;
  };

  if (x)
    foo
  else if (y)
    foo
  else
    bar
  ;

  if (x)
    foo
    + bar
  else if (y)
    foo   // Noncompliant
    + bar
  else
    bar
  ;

  match(x) {
    1 ->
      foo
      + bar;
    2 ->
      foo
      + baz;
    3 ->
      foo // Noncompliant
      + bar;
  };

  match(x) {
    1 -> ;
    2 ->
      foo
      + baz;
    3 -> ;
  };
