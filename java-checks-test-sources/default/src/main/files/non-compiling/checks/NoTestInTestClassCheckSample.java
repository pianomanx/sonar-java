package checks;

import java.lang.Deprecated;
import org.junit.experimental.runners.Enclosed;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;
import org.junit.runner.Suite;
import org.junit.runners.JUnit4;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import com.googlecode.zohhak.api.TestWith;
import com.googlecode.zohhak.api.runners.ZohhakRunner;

class A extends junit.framework.TestCase {
  void testFoo() {
  }
}

public class JUnit3Test extends junit.framework.TestCase {
  public void testNothing() {
    assertTrue(true);
  }
}
class B extends junit.framework.TestCase { // Noncompliant {{Add some tests to this class.}}
//    ^
  void foo() {
  }
}

@RunWith(JUnit4.class)
public class TestJUnit3With5 extends junit.framework.TestCase { // Compliant
  @org.junit.Test
  public void notJUnit3() { }
}

@RunWith(JUnit4.class)
public class TestJUnit3With4_JUnit3_Style extends junit.framework.TestCase { // Compliant
  public void testNothing() {
    assertTrue(true);
  }
}

class TestJUnit4WithJUnit3 { // Compliant, even if surefire-plugin includes this class as a test, it's too noisy
                             // if this rule do the same.
  public void test() {
  }
}

class JUnit4WithJUnit3Test { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^
  public void test() { // Simply naming test is not enough for JUnit 4
  }
}

class JUnit4WithJUnit3Tests { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^
  public void test() {
  }
}

class JUnit4WithJUnit3TestCase { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^^^
  public void test() {
  }
}

class ATest { // Noncompliant {{Add some tests to this class.}}
//    ^^^^^
  @Deprecated
  void foo() {
    new AnonymousClass() {
      void testfoo(){
      }
    };
  }
}
class BTest {
  @org.junit.Test
  void foo()  {
    class MyInnerTest { }
  }
}
enum MyTest {}
class AnonymousClass extends junit.framework.TestCase{
  void testfoo(){}
}

public abstract class AbstractIntegrationTest { //designed for extension should not raise issue.

}

class TestNGTest {
  @org.testng.annotations.Test
  void foo() {
  }
}

@org.testng.annotations.Test
public class FooTest {
  public void test1() {
  }

  public void test2() {
  }
}

@org.testng.annotations.Test
public class TestNGClassTest { // Noncompliant
//           ^^^^^^^^^^^^^^^
  public int field;
  private void test1() { }
  public static void foo() {}
}

@org.testng.annotations.Test
class TestNGClassTestWithMethodAnnotated { // compliant, with testng when the class is annotated with @Test all public methods are considered as tests
                                          // non public methods can also be added to tests with the @Test annotation
  @org.testng.annotations.Test
  void myMethod(){

  }
}

@org.testng.annotations.Test
class TestNGClassWithUnkownAnnotation {
  @Unkown
  void myMethod(){

  }
}

@org.testng.annotations.Test
class TestNGClassWithWrongAnnotation { // Noncompliant
  @Override
  void myMethod(){

  }
}


@org.testng.annotations.Test(groups ="integration")
public abstract class AbstractIntegrationTest2{
}

class BaseTest {
  @Test
  public void test() {
  }
}

class InterTest extends BaseTest {
}

class ImplTest extends BaseTest {
}

class OtherTest extends BaseTest {
  @Test
  public void test2() {
  }
}

@RunWith(cucumber.api.junit.Cucumber.class)
public class MyCucumberTest { // should not raise an issue
}
@RunWith(Cucumber.class)
public class MyCucumber2Test { // no issue
}

@RunWith(MyRunner.class)
public class MyCucumber3Test { // Noncompliant
//           ^^^^^^^^^^^^^^^
}
@RunWith(getRunner())
public class MyCucumber4Test { // Noncompliant
//           ^^^^^^^^^^^^^^^
  public Class<? extends Runner> getRunner() {
    return null;
  }
}
@RunWith(value1= MyRunner.class, value2= YourRunner.class)
public class MyCucumber5Test { // Noncompliant
//           ^^^^^^^^^^^^^^^
}

public class CTest {
  @Test // no issue, junit5 annotation
  public void testFoo() {
    assertThat(new A().foo(null)).isEqualTo(0);
  }
}
public class DTest { // Noncompliant {{Add some tests to this class.}}
//           ^^^^^
  public void testFoo() {
    assertThat(new A().foo(null)).isEqualTo(0);
  }
}

@RunWith(Suite.class)
@Suite.SuiteClasses(value = { S2187Test.Test1.class, S2187Test.Test2.class })
public class S2187Test {

  public static class Test1 {

    @Test
    public void test() {
      Assert.assertTrue(true);
    }
  }

  public static class Test2 {

    @Test
    public void test() {
      Assert.assertTrue(true);
    }
  }

}

@RunWith(Theories.class)
public class MyTheorieClassTest {
  @Theory
  public void test_method() {

  }
}

public class Junit5MetaAnnotationTest {

  @Test
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Bar {

  }

  @Bar
  public void test() {

  }

}

public class JUnit5InheritedDefaultMethodsTest implements TestA { // Compliant

}

interface TestA {

  @Test
  default void method1() {}
}

public class CrazyHierarchyTest extends AbstractCrazyHierarchyTest { } // Compliant, contains test from TestA interface

abstract class AbstractCrazyHierarchyTest implements TestB { }

interface TestB extends TestA { }

class MyUnitTest { // Compliant
  @ParameterizedTest
  void foo() {
    assertThat(plop);
  }
}

class CustomAnnotationTest {
  @CustomAnnotation
  void foo() {}
}

@org.junit.platform.commons.annotation.Testable
@interface CustomAnnotation {}

class NestedTest { // Compliant
  @Nested
  class NestedClass {
    @Test
    public void foo() {
      Assert.assertTrue(true);
    }
  }
}

class NoTestsInNestedTest { // Noncompliant {{Add some tests to this class.}}
//    ^^^^^^^^^^^^^^^^^^^
  @Nested
  class NestedClass {
    public void foo() {
      Assert.assertTrue(true);
    }
  }
}

class SomeTest implements SomeInterface { } // Noncompliant {{Add some tests to this class.}}
//    ^^^^^^^^

interface SomeInterface {
  class Foo implements SomeInterface { }
}


@RunWith(ZohhakRunner.class)
public class MyZohhakTest { // Noncompliant
//           ^^^^^^^^^^^^
}

@RunWith(ZohhakRunner.class)
public class MyZohhak2Test { // Compliant, Zohhak uses @TestWith
  @TestWith({
    "1, 2",
    "3, 4"
  })
  public void testFoo1(int p1, int p2) {
  }
}

@RunWith(ZohhakRunner.class)
public class MyZohhak3Test { // Compliant, Zohhak uses @TestWith
  @TestWith(value=" 7 = 7 > 5 => true", separator="=>")
  public void testFoo3(String string, boolean bool) {
  }
}

@RunWith(ZohhakRunner.class)
public class MyZohhak4Test { // Compliant
  @Test
  public void testFoo4() {
  }
}

class SubclassTest extends UndefinedParent { // Compliant, we cannot know what is in ParentTestClass so we don't raise issues
}

class Subclass2Test extends myPackage.UndefinedParent {
}

class Subclass3Test implements UndefinedInterface {

}

abstract class ResolvedParent {}
class Subclass4Test extends ResolvedParent {} // Noncompliant
