package checks;

class MembersDifferOnlyByCapitalizationCheckSample {

abstract class A implements MyInterface {
  private static final long var1 = -9215047833775013803L; // Compliant
  public long var2 = 0L;

  public void equAls(Object obj) {} // Noncompliant {{Rename method "equAls" to prevent any misunderstanding/clash with method "equals" defined in superclass "java.lang.Object".}}

  protected void finaliZe() {} // Noncompliant {{Rename method "finaliZe" to prevent any misunderstanding/clash with method "finalize" defined in superclass "java.lang.Object".}}

  public void myMethod() {}

  public Object stuff;
//              ^^^^^>
  public void stuff() {} // Noncompliant {{Rename method "stuff" to prevent any misunderstanding/clash with field "stuff".}}
  //          ^^^^^
  public void stuff(int i) {} // Noncompliant {{Rename method "stuff" to prevent any misunderstanding/clash with field "stuff".}}

  public void foo(int i) {} // Compliant
  public void foo(boolean i) {} // Compliant

  public void myOtherMethoD() {} // Noncompliant {{Rename method "myOtherMethoD" to prevent any misunderstanding/clash with method "myOtherMethod" defined in interface "checks.MembersDifferOnlyByCapitalizationCheckSample$MyInterface".}}
//            ^^^^^^^^^^^^^
  private static void gUl() {} // Noncompliant {{Rename method "gUl" to prevent any misunderstanding/clash with method "gul".}}
                    //^^^
  public void gul() {} // Compliant
//            ^^^<

  private boolean qix;

  public Object myField; // Noncompliant
  @Override public void myField() {} // Compliant, as it overrides the parent interface method

  public A() {}
  class MyInnerClass {}
  ;

  public void SUPER() {} // Compliant
  public void tHiS() {} // Compliant

}

interface MyInterface {
  public boolean myOtherMethod();
  public void gul();
  public void myField();
}

abstract class B extends A {
  private static final long var1 = -9215047833775013803L; // Compliant
  public void var2() {}; // Noncompliant
//            ^^^^>
  public void vAr2() {}; // Noncompliant
            //^^^^

  public void myMethoD() {} // Noncompliant {{Rename method "myMethoD" to prevent any misunderstanding/clash with method "myMethod" defined in superclass "checks.MembersDifferOnlyByCapitalizationCheckSample$A".}}

  public Object qix;  // Compliant

  public void fOo(int i) {} // Noncompliant
}

class Visibility {
  public int tmp0;
//           ^^^^>
  private void tmp0() {}
  public void tmp0(int i) {} // Noncompliant {{Rename method "tmp0" to prevent any misunderstanding/clash with field "tmp0".}}
  //          ^^^^
  protected void tmp0(long l) {}
  void tmp0(short s) {}

  private int tmp1;
  private void tmp1() {} // Compliant - private members having same name are ignored
  public void tmp1(int i) {}
  protected void tmp1(long l) {}
  void tmp1(short s) {}

  int tmp2;
//    ^^^^>
  private void tmp2() {}
  public void tmp2(int i) {}
  protected void tmp2(long l) {}
  void tmp2(short s) {} // Noncompliant {{Rename method "tmp2" to prevent any misunderstanding/clash with field "tmp2".}}
  //   ^^^^

  protected int tmp3;
//              ^^^^>
  private void tmp3() {}
  public void tmp3(int i) {}
  protected void tmp3(long l) {} // Noncompliant {{Rename method "tmp3" to prevent any misunderstanding/clash with field "tmp3".}}
  //             ^^^^
  void tmp3(short s) {}
}

public enum MyEnum {
  FOO;

  public void foo() {} // Compliant
  public static MyEnum valueof(int i) { return FOO; } // Noncompliant {{Rename method "valueof" to prevent any misunderstanding/clash with method "valueOf".}}
}

class ABuilder {
  String name;
  String wrong;
//       ^^^^^>
  String value;

  ABuilder name(String name) { // Compliant - exception for builder pattern
    this.name = name;
    return this;
  }

  ABuilder WRONG(String wrong) { // Noncompliant
    //     ^^^^^
    this.wrong = value;
    return this;
  }
}


class SameName {
  Object m;
  Object f1, f2, f3;
  Object flambda;
  Object fAnon;

  Object m() {  // Compliant
    return m;
  }

  Object M() { // Noncompliant
    return m;
  }

  Object f1() { // Noncompliant
    return new Object();
  }

  void f2() { // Noncompliant
    return;
  }

  Object flambda() { // Compliant
    call(f -> {
      return new Object();
    });
    return flambda;
  }

  void call(java.util.function.Function<Object, Object> o) {}

  Object fAnon() { // Compliant
    Object o = new Object() {
      public String toString() {
        return "";
      }
    };
    return fAnon;
  }

  Object foo;
  void foo() { // Noncompliant
    SameName sn = new SameName() {
      Object bar() {
        return foo;
      }
    };
  }

  boolean cond;

  Object f3() { // Noncompliant
    if (cond) {
      return new Object();
    }
    return f3;
  }
}

  record MyRecord(int intComponent, boolean bool) {

    public static final boolean BooL = false; // Noncompliant {{Rename field "BooL" to prevent any misunderstanding/clash with field "bool".}}

    @Override public boolean bool() {
      return bool;
    }

    public int intCOMPONENT() { // Noncompliant {{Rename method "intCOMPONENT" to prevent any misunderstanding/clash with method "intComponent".}}
      return intComponent;
    }
  }

  Object subClass; // Compliant
  class subClass { }

  class NoDuplications {
    void Abcde(){ }
//       ^^^^^>
    void aBcde(){ } // Noncompliant {{Rename method "aBcde" to prevent any misunderstanding/clash with method "Abcde".}}
    //   ^^^^^
    void abCde(){ } // Noncompliant {{Rename method "abCde" to prevent any misunderstanding/clash with method "Abcde".}}
    void abcDe(){ } // Noncompliant {{Rename method "abcDe" to prevent any misunderstanding/clash with method "Abcde".}}
    void abcdE(){ } // Noncompliant {{Rename method "abcdE" to prevent any misunderstanding/clash with method "Abcde".}}
  }

}
