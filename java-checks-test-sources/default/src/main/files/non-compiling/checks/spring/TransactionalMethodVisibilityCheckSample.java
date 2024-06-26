package checks;

import org.springframework.transaction.annotation.Transactional;

class TransactionalMethodVisibilityCheckSample {
  // Cannot compile because a Transactional method should be overridable
  @org.springframework.transaction.annotation.Transactional
  private void privateTransactionalMethod() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation.}}
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^

  // Cannot compile because a Transactional method should be overridable
  @Transactional
  private void privateTransactionalMethodWithImportBasedAnnotation() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation.}}

  @org.xxx.Transactional
  private void privateMethodWithNonSpringAnnotation() {} // Compliant
  
}
