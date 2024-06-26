package checks;

import java.lang.reflect.Method;

class ReflectionOnNonRuntimeAnnotationCheckSample {
  void foo(Method m, Class<?> c) {
    m.isAnnotationPresent(UnknownAnnotation.class); // Compliant, unable to retrieve annotation
    c.isAnnotationPresent(UnknownAnnotation.class); // Compliant, unable to retrieve annotation
  }
}
