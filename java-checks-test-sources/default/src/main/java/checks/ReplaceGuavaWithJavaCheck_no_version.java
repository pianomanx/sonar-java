package checks;

class ReplaceGuavaWithJavaCheck_no_version {
  ReplaceGuavaWithJavaCheck_no_version(com.google.common.base.Predicate p) {} // Noncompliant {{Use "java.util.function.Predicate" instead. (sonar.java.source not set. Assuming 8 or greater.)}}
//                                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
}
