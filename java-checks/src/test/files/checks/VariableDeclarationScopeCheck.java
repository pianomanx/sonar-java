import java.util.List;
import java.util.ArrayList;

abstract class A {

  void m1();

  void m2(int par) {
    int a; // Noncompliant {{Move the declaration of "a" closer to the code that uses it.}}
//      ^

    if (true) {
    }

    while(true) {
      if (true) {
        par = 1;
        return;
      } else {
        return;
      }
    }

    a = 5;
  }

  void m3(int par) {
    int var; // Noncompliant {{Move the declaration of "var" closer to the code that uses it.}}

    do {
      if (true) {
        throw new IllegalStateException("error");
      } else {
        throw new IllegalStateException("error");
      }
    } while (true);

    var = 5;
  }

  void m5() {
    int b;
    {
      b = 5;
      int c = b + 2;
    }
  }

  void m5() {
    int b = 5;
    int c = 8;
  }

  void unused() {
    if (true) {
      // no issues for unused variables
      int b = 2;
      return;
    }
  }

  void withLambdaUsingVar() {
    List<String> outsideLambda = new ArrayList<>(); // Compliant: this cannot be declared anywhere else
    Collections.singletonList(23).stream.map(x -> {
        outsideLambda.add(x);
        return x;
      })
      .collect(Collectors.toList());
    return outsideLambda;
  }

  void withLambdaNotUsingVar() {
    List<String> outsideLambda = new ArrayList<>(); // Noncompliant
    Collections.singletonList(23).stream.map(x -> {
        return x;
      })
      .collect(Collectors.toList());
    return outsideLambda;
  }
}
