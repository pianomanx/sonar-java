class A {
  void foo(int i, long l, String[] s) {
  }
  void bar(){}

  void plop() {
   foo(1,2,new String[]{"1"}); // Noncompliant {{Remove this forbidden call}}
// ^^^
   bar();
  }
}
