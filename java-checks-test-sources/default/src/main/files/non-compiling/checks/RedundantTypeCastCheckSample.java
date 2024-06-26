package checks;

import java.util.function.Predicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;
import java.util.Set;

class Outer {
  class A {
  }
  class B extends A { }
  List list;
  List<String> foo() {
    Object obj = null;
    Object o1 = (List<String>) foo(); // Noncompliant {{Remove this unnecessary cast to "List".}}
//              ^^^^^^^^^^^^^^
    Object o2 = (List<? extends String>) foo(); // Noncompliant {{Remove this unnecessary cast to "List".}}
    Object o3 = (List<? super String>) foo(); // Noncompliant {{Remove this unnecessary cast to "List".}}
    String s1 = (String) obj; // Compliant
    String s2 = (String) s1; // Noncompliant {{Remove this unnecessary cast to "String".}}
    A a = (A) new B(); // Noncompliant {{Remove this unnecessary cast to "A".}}
    A[][] as = (A[][]) new B[1][1]; // Noncompliant {{Remove this unnecessary cast to "A[][]".}}
    B b = null;
    fun(b);
    fun((B) b); // Noncompliant
    fun((A) b); // Compliant - exception to distinguish the method to call
    List<B> bees = new java.util.ArrayList<B>();
    List<A> aaas = (List) bees;
    C c = new C((A) null); // Compliant - exception to distinguish the constructor to call
    C c2 = new C((B) b); // Noncompliant
    foo((List<List<A>>) (List<?>) foo2()); // compliant
    obj = (Unknown<String>) unknown;
    String[] stringList = (String[]) list.toArray(new String[0]); // Compliant
    return null;
  }

  List<String> foo2() {

    int a = 1;
    int b = 2;
    double d = (double) a / (double) b;
    int c = (int)a; // Noncompliant {{Remove this unnecessary cast to "int".}}
    int e = (int) d;
    return null;
  }

  private static int charConversion(char c) {
    return (char) ((c | 0x20) - 'a'); // Compliant
  }

  void foo(List<List<A>> a) {}

  void castInArguments(List<String> p) {
    Collection<String> v1 = Collections.emptyList();
    List<String> v2 = Collections.emptyList();
    castInArguments((List<String>) v1); // Compliant - cast needed
    castInArguments((List<String>) v2); // Noncompliant
  }

  List<List<B>> foo3() {
    return null;
  }
  void fun(A a) {
  }

  void fun(B b) {
  }

  void funBParameter(B b) {
  }

  class C {
    C(A a) {}
    C(B a) throws Exception {
      Object o = (Object) fun().newInstance(); // Noncompliant {{Remove this unnecessary cast to "Object".}}
    }
    Class fun() { return null;}
    public <T> T[] toArray(T[] a) {
      Object[] elementData = new Object[0];
      // Make a new array of a's runtime type, but my contents:
      return (T[]) Arrays.copyOf(elementData, 12, a.getClass()); // Compliant - The cast is mandatory!
    }
    String[] fun2(){
      return (String[]) null; // Noncompliant {{Remove this unnecessary cast to "String[]".}}
    }
    void fun3() {
      Object[] a = null;
      Collection<C> c = (Collection<C>) Arrays.asList(a);
    }
  }
}

abstract class MyClass {
  public String field;
  abstract <U extends MyClass> U foo();

  String qix2() {
    return ((MyOtherClass) unknown()).bar(); // Compliant
  }
}

@lombok.AllArgsConstructor
enum MyPrivateEnum {

  A((byte) 1), // constructor can not be resolved by ECJ, as it is generated by lombok
  B((byte) 2);

  @lombok.Getter
  private byte value;


}

class FP_S1905 {
  static class Overloaded {
    static String f() {
      return "";
    }

    static String f(String a) {
      return "";
    }
  }

  void main() {
    foo(Overloaded::f); // Does not compile without cast
    foo(String::new); // Does not compile without cast

    bar(((Supplier<String>) Overloaded::fff));
    foo((Supplier<String>) Overloaded::unknown);
    unknown((Supplier<String>) Overloaded::fff);
  }

  void main2() {
    foo((Supplier<String>) Overloaded::f); // Compliant, cast is mandatory
    foo((Function<String, String>) Overloaded::f); // Compliant, cast is mandatory

    foo((Supplier<String>) Overloaded::fff); // Compliant
    bar((Supplier<String>) Overloaded::fff); // Compliant
    bar((Supplier<String>) Overloaded::f); // Compliant

    bar((Supplier<String>) NotOverloaded::notOverloded); // Compliant
    foo((Supplier<String>) NotOverloaded::notOverloded); // Compliant

    rawBar((Supplier<String>) Overloaded::fff); // Compliant, cast is redundant


    foo((Supplier<String>) String::new); // Compliant
    foo((Function<String, String>) String::new); // Compliant
  }

  void foo(Supplier<String> supplier) {
  }

  void foo(Function<String, String> function) {
  }

  void main222() {
    foo((FFunction<String>) NotOverloaded::xxx); // Compliant
  }

  void foo222(FFunction<String, String> function) {
  }
}
