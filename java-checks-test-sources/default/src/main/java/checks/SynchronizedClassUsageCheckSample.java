package checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable; // Compliant
import java.util.List;
import java.util.Map;
import java.util.Stack; // Compliant
import java.util.Vector; // Compliant

public class SynchronizedClassUsageCheckSample {

  public SynchronizedClassUsageCheckSample() {}

  interface IA {
 // Noncompliant@+2 {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
 // Noncompliant@+1 {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
    Vector f3(Vector a);
  }

  class A implements IA {
    List a = new Vector(); // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
//           ^^^^^^^^^^^^

//  Noncompliant@+1
    Vector a1 = new Vector(); // Noncompliant
//  ^^^^^^


//  Noncompliant@+1
    Hashtable a2 = new Hashtable(); // Noncompliant

    Map a3 = new Hashtable(); // Noncompliant {{Replace the synchronized class "Hashtable" by an unsynchronized one such as "HashMap".}}
    Hashtable a4 = foo(); // Noncompliant {{Replace the synchronized class "Hashtable" by an unsynchronized one such as "HashMap".}}
    HashMap a5 = new HashMap();     // Compliant
    ArrayList a6 = new ArrayList(); // Compliant
    Vector<Integer> a7; // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}

// Noncompliant@+1
    StringBuffer a8 = new StringBuffer(); // Noncompliant

// Noncompliant@+1
    java.util.Stack a9 = new java.util.Stack(); // Noncompliant

    List l = null; // Compliant
    List<Object> listeners = getVector(); // Compliant

    A(Vector v) { // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
      a = v;
      new antlr.collections.impl.Vector().capacity();
    }

    private static Hashtable foo() { // Noncompliant
      return null;
    }

    private void f() {
      System.out.println(Vector.class); // Compliant
      a.addAll(new java.util.Vector()); // Compliant
      java.util.Vector<Integer> result = null; // Noncompliant
      List result2 = new java.util.Vector<Integer>(); // Noncompliant
    }

    private Vector getVector() { // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
//          ^^^^^^
      return new Vector();
    }

    public Vector a10; // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}

    public java.util.Stack f2() { // Noncompliant {{Replace the synchronized class "Stack" by an unsynchronized one such as "Deque".}}
      return null;
    }

    public void f(Vector a) { // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
//                ^^^^^^
    }

    @Override
    public Vector f3(Vector a) { // Compliant
      Vector b = new Vector(); // Compliant: since Vector is part of the overriding signature, it is allowed to use inside the method body
      Hashtable ht; // Noncompliant

      class Anonymous implements IA{
        @Override
        public Vector f3(Vector a) {
          Vector b = new Vector();
          Hashtable ht; // Noncompliant
          return null;
        }
      }

      Vector b2 = new Vector();
      return b;
    }
    public void f(Integer i) { // Compliant
    }
  }

  interface AInterface {
 // Noncompliant@+1
    Vector a(Vector a); // Noncompliant
  }

  enum AEnum implements AInterface {
    A,B,C;

    Vector a; // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}

    Vector b() { // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
      return null;
    }

    @Override
    public Vector a(Vector a) { // Compliant
      return null;
    }
  }

  class B {
    class Stack {}
    B() {}
    void foo(Stack stack) { // Compliant
    }
  }

  class MyVector<T> extends Vector<T> { // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
  }

  class InferedTypeFromLambda {
    void foo(Vector<Vector<String>> v) { // Noncompliant
      Collections.sort(v, (s1, s2) -> -1);
      Collections.sort(v, (Vector<String> s1, Vector<String> s2) -> -1);
      Collections.sort(v, (Vector<String> s1, Vector<String> s2) -> {

// Noncompliant@+1
        Vector<String> x = s1; // Noncompliant
        return -1;
      });
    }
  }
  class InvokeStringBufferMethod {
    String fComponents[] = new String[]{"Hello"};

    public String toString() {

// Noncompliant@+1
      StringBuffer buf = new StringBuffer(); // Noncompliant

      for (int i = 0; i < fComponents.length; i++) {
        buf.append(fComponents[i]);
      }

      return buf.toString();
    }
  }
}


