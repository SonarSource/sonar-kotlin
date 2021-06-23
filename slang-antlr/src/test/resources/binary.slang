// Example method in slang 
a = 2;
b = K + 1;
c = a - a;
d = "hello world";
e = 'c' + 'h';
true && false;
f = e;

native ["DEFER"] {
  [ a = 1; ]
  [ b = 2;
    c = b + 1;
  ]
};

public int fun test()
{
  e = a / c;
}
