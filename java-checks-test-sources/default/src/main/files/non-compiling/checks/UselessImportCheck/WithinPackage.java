package checks.UselessImportCheck;

import a.b.c.Foo;
import a.b.c.Bar;
import a.b.c.Baz;
import a.b.c.Qux;
import a.b.c.ReferencedFromJavadoc;
import a.b.c.NonCompliant; // Noncompliant
//^[sc=1;ec=27]
import NonCompliant2; // Noncompliant
import static a.b.c.Foo.d;
import a.b.c.*;
import static a.b.c.Foo.*;
import a.b.c.MyException;
import a.b.c.MyException2;
import a.b.c.MyAnnotation1;
import a.b.c.MyAnnotation2;
import a.b.c.MyAnnotation3;
import java.lang.String; // Noncompliant {{Remove this unnecessary import: java.lang classes are always implicitly imported.}}
import java.lang.*; // Noncompliant {{Remove this unnecessary import: java.lang classes are always implicitly imported.}}
import a.b.c.Foo; // Noncompliant {{Remove this duplicated import.}}

import checks.UselessImportCheck.*; // Noncompliant {{Remove this unnecessary import: same package classes are always implicitly imported.}}
import checks.UselessImportCheckClose.*;
import static checks.UselessImportCheck.Foo.*;
import checks.UselessImportCheck.foo.*;
import checks.UselessImportCheck.foo.Foo;
import pkg.NonCompliant1; // Noncompliant
import pkg.CompliantClass1;
import pkg.CompliantClass2;
import pkg.CompliantClass3;
import pkg.CompliantClass4;
import java.lang.reflect.Array;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault; // Noncompliant
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import javax.annotation.Nonnull;
import checks.UselessImportCheck.WithPackageAux; // Noncompliant {{Remove this unnecessary import: same package classes are always implicitly imported.}}

import java.io.File; // Noncompliant

import static checks.UselessImportCheck.SomeEntity.FIND_BY_NAME;
import static checks.UselessImportCheck.SomeEntity.FIND_BY_AGE; // Noncompliant
import static checks.UselessImportCheck.Foo2.A.BAR; // Noncompliant
import static checks.UselessImportCheck.Foo2.A.FLUP; // compliant, used outside of owning class
import static checks.UselessImportCheck.Foo2.A.qix; // compliant : Method symbols are ignored.

public class Foo2 extends Foo {
  Bar a = new Baz<String>();
  Map<@Nonnull String, @Nonnull String> modulesMap = new HashMap<>();
  @Qux
  void test() throws MyException, MyException2 {
  }
  // ReferencedFromJavadoc
  @a.b.c.NonCompliant
  a.b.c.NonCompliant foo(a.b.c.NonCompliant bar) {
    List<CompliantClass1> ok = ImmutableList.<CompliantClass4>of();
    Class ok2 = CompliantClass2.class;
    CompliantClass3.staticMethod("OK");
    pkg.NonCompliant1 ok3;
    Array ok4;
    tottttt a;
    System.out.println(something.t);
    foo(ArrayList::new);
    return new a.b.c.NonCompliant();
  }
  void foo(@Nullable int x){
    System.out.println(FLUP);;
  }
  static class A {
    public static final String BAR = "value";
    public static final String FLUP = "value";
    public static void qix() {}
    byte @MyAnnotation2 [] table = null;
    org.foo.@MyAnnotation1 B myB;


    void foo(java.util.List<String> list) {
      for (@MyAnnotation3 Object o : list) {
        o.toString();
      }
    }

    void foo() {
      System.out.println(BAR);
      qix();
    }
  }

  /**
   * FileUtils#getFile() .... <--- should not invalid import of j-a-v-a-.-i-o-.-F-i-l-e (avoid recognition here)
   */
  void bar() {
    // ...
  }
}

@MyAnnotation(name = FIND_BY_NAME)
class SomeEntity {
  public static final String FIND_BY_NAME = "SomeEntity.findByName";
  public static final String FIND_BY_AGE = "SomeEntity.findByAge";
  private String name;

  @MyAnnotation(name = FIND_BY_AGE)
  public String getEntityName() {
    return name;
  }
}

@interface MyAnnotation {
  String name();
}
