class A {
  public Object clone(){ // Noncompliant {{Remove this "clone" implementation; use a copy constructor or copy factory instead.}}
//              ^^^^^
    return super.clone();
  }
  public Object clonerMethod() {  // Compliant
  }
  public Object clone(int a) {  // Compliant
  }
}
class B {
  @Override
  protected Object clone() throws CloneNotSupportedException { // Compliant, common practice of overriding to throw an exception
    throw new CloneNotSupportedException("Clone not supported for a Singleton");
  }
}
class C {
  @Override
  protected Object clone() throws CloneNotSupportedException { // Noncompliant
    System.out.println("");
    throw new CloneNotSupportedException("Clone not supported for a Singleton");
  }
}
class D {
  protected abstract Object clone() throws CloneNotSupportedException; // Noncompliant
}
class E {
  @Override
  protected Object clone() throws CloneNotSupportedException { // Noncompliant
    throw new UnsupportedOperationException("Clone not supported for a Singleton");
  }
}
