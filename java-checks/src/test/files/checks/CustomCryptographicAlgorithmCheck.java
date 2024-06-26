import java.security.MessageDigest;
import java.util.Observable;

class A { // Compliant
  void foo() {
    MessageDigest md = new MessageDigest() { // Noncompliant {{Make sure using a non-standard cryptographic algorithm is safe here.}}
//                         ^^^^^^^^^^^^^

    };
  }
}

abstract class B extends MessageDigest { // Noncompliant {{Make sure using a non-standard cryptographic algorithm is safe here.}}
//             ^
  protected B(String algorithm) {
    super(algorithm);
  }
}

abstract class C extends java.security.MessageDigest { // Noncompliant
  protected C(String algorithm) {
    super(algorithm);
  }
}

abstract class D extends B { // Noncompliant
  protected D(String algorithm) {
    super(algorithm);
  }
}

class E extends Observable { // Compliant
}
