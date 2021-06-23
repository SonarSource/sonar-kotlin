x = x + 1;
x = (x + 1);
x = ((x + 1)); // Noncompliant
//   ^     ^<
x = (((x + 1))); // Noncompliant 2
x = (x + 1) * (x + 2);
x = x + (1 * x) + 2;
x = x + ((1 * x)) + 2; // Noncompliant
x = (1);
x = ((1)); // Noncompliant
//   ^ ^<

if (x && (x + 1 > 0)) {};
if (x && ((x + 1 > 0))) {}; // Noncompliant
if (x && ((x + 1) > 0)) {};
