public int fun test(int a){
  if (a > 0) {
     return 0;
  };

  if (a > 1) {
     if (a > 2) {
       return 2;
     } else {
       return 3;
     }
  };

  if (a > 2) {
     return 2;
  } else if (a > 3) {
     return 3;
  } else {
     return 4;
  };
}