package checks;

import org.junit.jupiter.params.provider.MethodSource;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
@interface Observes {}

class UnusedPrivateMethodCheck {

  @MethodSource
  private void unusedMethodWithWrongAnnotation(){} // Compilant - FN

  @MethodSource()
  void testMessages(String message) {
    // ...
  }

  private static Stream<String> testMessages() { // Compliant, used by @MethodSource
    return Stream.of("Hello world!", "Carpe diem!");
  }

  private void init(@Observes Object object, String test) {} // Noncompliant
  private void init(@javax.enterprise.event.Observes Object object) {} //Compliant, javax.enterprise.event.Observes is an exception to the rule
  private void jakartaInit(@jakarta.enterprise.event.Observes Object object) {} //Compliant, jakarta.enterprise.event.Observes is an exception to the rule
  private void initNc(@AnotherAnnotation Object object) {} // Noncompliant

  private UnusedPrivateMethodCheck() {}
  private UnusedPrivateMethodCheck(int a) {} // Noncompliant [[quickfixes=qf_constructor]]
//        ^^^^^^^^^^^^^^^^^^^^^^^^
  // fix@qf_constructor {{Remove the unused constructor}}
  // edit@qf_constructor [[sl=-1;sc=40;el=+0;ec=45]] {{}}

  public UnusedPrivateMethodCheck(String s) {
    init();
  }

  private void init() {
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    // this method should not be considered as dead code, see Serializable contract
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    // this method should not be considered as dead code, see Serializable contract
  }

  private Object writeReplace() throws java.io.ObjectStreamException {
    // this method should not be considered as dead code, see Serializable contract
    return null;
  }

  private Object readResolve() throws java.io.ObjectStreamException {
    // this method should not be considered as dead code, see Serializable contract
    return null;
  }

  private void readObjectNoData() throws java.io.ObjectStreamException {
    // this method should not be considered as dead code, see Serializable contract
  }

  @SuppressWarnings("unused")
  private int unusedPrivateMethod() {
    return 1;
  }
  private int unusedPrivateMethod(int a, String s) { // Noncompliant {{Remove this unused private "unusedPrivateMethod" method.}}
    return 1;
  }

  private void varargs(String first, String second, Object... objects) {
  }

  private void varargs(String... strings) {
  }

  public void usage() {
    varargs("", "", new Object());
    varargs("", "", ""); // should resolve to 'String...' and not 'String, Object...'
  }

  public enum Attribute {
    ID("plop", "foo", true);

    Attribute(String prettyName, String type, boolean hidden) { }

    private Attribute(String name) { } // Noncompliant

    Attribute(String prettyName, String[][] martrix, int i) { // Noncompliant {{Remove this unused private "Attribute" constructor.}}
    }

  }

  private class A {
    A(int a) {}
    private A(){}
    private <T> T foo(T t) {
      return null;
    }

    public void bar() {
      foo("");
    }
  }
  // This comment will be deleted by the quick fix.

  private void testQuickFix1() { // Noncompliant [[quickfixes=qf1]]
//             ^^^^^^^^^^^^^
    // fix@qf1 {{Remove the unused method}}
    // edit@qf1 [[sl=-3;sc=4;el=+5;ec=4]] {{}}
    int i = 12;
  }

  private void testQuickFix2() { // Noncompliant [[quickfixes=qf2]]
//             ^^^^^^^^^^^^^
    // fix@qf2 {{Remove the unused method}}
    // edit@qf2 [[sl=-2;sc=4;el=+6;ec=4]] {{}}
    int i = 12;
    int j = 12;
  }

}

class OuterClass {

  private static <T> void genericMethod(T argument) {
    new Object() {
      private void unused() { // Noncompliant {{Remove this unused private "unused" method.}}
      }
    };
  }

  private static <T extends java.util.List<String>> void complexGenericMethod(T argument) {
  }

  class NestedGenericClass<T> {
    private NestedGenericClass(T argument) { // Compliant
    }

    private void genericMethod(T argument) { // Compliant
    }
  }

  class ComplexNestedGenericClass<T extends java.util.Collection<Object>> {
    private ComplexNestedGenericClass(T argument) {
    }

    private void genericMethod(T argument) {
    }
  }

  public void test() {
    genericMethod("string");
    complexGenericMethod(new java.util.ArrayList<String>());
    new NestedGenericClass<java.util.List<Object>>(new java.util.ArrayList<Object>()).genericMethod(new java.util.LinkedList<Object>());
    new ComplexNestedGenericClass<java.util.List<Object>>(new java.util.ArrayList<Object>()).genericMethod(new java.util.LinkedList<Object>());
  }

}

class UnusedPrivateMethodCheckLambdas {
  void method(){
    IntStream.range(1, 5)
      .map((x)-> x*x )
      .map(x -> x * x)
      .map((int x) -> x * x)
      .map((x)-> x*x )
    ;
  }
}

class UnusedPrivateMethodCheckReturnTypeInference {
  private void foo(java.util.List<String> l) {}
  void test() {
    java.util.List<String> l = new ArrayList<>();
    foo(l.stream().sorted().collect(Collectors.toList()));
  }
}
class UnusedPrivateMethodCheckBar {
  public void print() {
    java.util.List<String> list = java.util.Arrays.asList("x", "y", "z");
    java.util.List<Foo> foos = list.stream().map(Foo::new).collect(Collectors.toList());
    System.out.println(foos.get(0).foo);
  }

  public class Foo {
    private String foo;
    private Foo(String foo) {
      this.foo = foo;
    }
  }
}

class UnusedPrivateMethodCheckNestedTypeInference1 {
  public <D, L extends List<D>> void foo(Callback2<L> cb2) {
    qix(rs -> bar(cb2.doStuff(rs)));
  }

  private void qix(Callback1 cb1) {}

  private <D, L extends List<D>> void bar(L data) {}

  @FunctionalInterface
  private interface Callback1 {
    void doStuff(String rs);
  }

  @FunctionalInterface
  private interface Callback2<T> {
    T doStuff(String rs);
  }
}

class UnusedPrivateMethodCheckNestedTypeInference2 {
  public Supplier<Map<Boolean, BigDecimal>> branch() {
    return turnover(calculateTurnover(this::extractSourceBranches));
  }

  private Supplier<Map<Boolean, BigDecimal>> turnover(Function<Double, BigDecimal> param1) {
    return Collections::emptyMap;
  }

  private <A, S> Function<A, BigDecimal> calculateTurnover(Function<A, Set<S>> param1) {
    return x -> BigDecimal.ZERO;
  }

  private Set<Integer> extractSourceBranches(Double param) {
    return emptySet();
  }
}

class UnusedPrivateMethodCheckMyClass<A extends Serializable> {

  private UnusedPrivateMethodCheckMyClass(MyClassBuilder<A> builder) { // Compliant: used in MyClassBuilder
    builder.build();
  }

  public static final class MyClassBuilder<B extends Serializable> {
    public UnusedPrivateMethodCheckMyClass<B> build() {
      return new UnusedPrivateMethodCheckMyClass<>(this); // constructor is correctly resolved when using diamond
    }
  }
}

class CheckAnnotations {
  @interface ProxyMethod {
    public String value();
  }

  @interface MethodProvided {
    public String value();
  }

  @interface Getter {
    public String getterMethod();
  }

  @interface Setter {
    public String method();
  }

  @interface ArgumentIsNotAString {
    public int method();
  }

  abstract static class MethodReferencedInAnnotation1 {

    private void foo1() {} // Compliant

    @ProxyMethod("foo1")
    abstract void bar1();
  }

  @MethodProvided(value = "foo2")
  abstract static class MethodReferencedInAnnotation2 {

    private void foo2() {} // Compliant

    @ProxyMethod("foo2")
    abstract void bar2();

    private void baz2() {} // Noncompliant
  }

  static class MethodReferencedInAnnotation3 {

    @Getter(getterMethod = "foo3")
    int bar3;

    private int foo3() { // Compliant
      return 42;
    }
  }

  static class MethodReferencedInAnnotation4 {

    @Setter(method = "foo4")
    int bar4;

    private void foo4(int value) {} // Compliant

    private void bar4(int value) {} // Noncompliant
  }

  static class MethodReferencedInAnnotation5 {

    @Getter(getterMethod = "foo52")
    @Setter(method = "foo54")
    int bar5;

    private void foo51(int value) {} // Noncompliant

    private int foo52() { // Compliant
      return 42;
    }

    private void foo53(int value) {} // Noncompliant

    private void foo54(int value) {} // Compliant

    private void foo55(int value) {} // Noncompliant
  }

  abstract static class Coverage1 {

    private void foo6() {} // Noncompliant

    @ProxyMethod("""
      foo6""")
    abstract void notStringLitNorAssignExpr();

    @Getter(getterMethod = """
      foo6""")
    int assignExprWithNoStringLit;
  }

  @ArgumentIsNotAString(method = 42)
  static class Coverage2 {
    private void foo7() {} // Noncompliant
  }

  @MethodProvided(value = "foo8")
  static class TN {
    private void foo8() {} // Compliant, method is referenced in annotation

    private void foo8(int param) {} // Compliant, method is referenced in annotation
  }

  static class FN {
    private void foo8() {} // Noncompliant
  }
}
