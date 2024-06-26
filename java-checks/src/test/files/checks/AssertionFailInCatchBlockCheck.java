import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;

public class MyTest {

  static {
    try {
      File.createTempFile(null, null);
    } catch (IOException e) {
      Assert.fail(e.getMessage()); // Noncompliant {{Remove this failure assertion and simply add the exception type to the method signature.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
    Assert.fail(); // Compliant, not in catch
  }

  @Test
  public void testMethodKoWithMessage() {

    try {
      // Some code
    } catch (Exception e) {
      if (true) {
        Assert.fail(e.getMessage()); // Noncompliant {{Remove this failure assertion and simply add the exception type to the method signature.}}
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^
      }
    }

    try {
      // Some code
    } catch (Exception e) {
      org.junit.Assert.fail(e.getMessage()); // Noncompliant {{Remove this failure assertion and simply add the exception type to the method signature.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    try {
      // Some code
    } catch (Exception e) {
      org.junit.jupiter.api.Assertions.fail(e.getMessage()); // Noncompliant {{Remove this failure assertion and simply add the exception type to the method signature.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    try {
      // Some code
    } catch (Exception e) {
      junit.framework.Assert.fail(e.getMessage()); // Noncompliant {{Remove this failure assertion and simply add the exception type to the method signature.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    try {
      // Some code
    } catch (Exception e) {
      org.fest.assertions.Fail.fail(); // Noncompliant {{Remove this failure assertion and simply add the exception type to the method signature.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    try {
      // Some code
    } catch (Exception e) {
      org.assertj.core.api.Assertions.fail(""); // Noncompliant
    }

    try {
      // Some code
    } catch (Exception e) {
      org.assertj.core.api.Assertions.fail("", e); // Noncompliant
    }

  }

  @Test
  public void testMethodKoWithNoMessage() {
    try {
      // Some code
    } catch (Exception e) {
      Assert.fail(); // Noncompliant {{Remove this failure assertion and simply add the exception type to the method signature.}}
//    ^^^^^^^^^^^^^
    }

    try {
      // Some code
    } catch (Exception e) {
      org.junit.Assert.fail(); // Noncompliant {{Remove this failure assertion and simply add the exception type to the method signature.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^
    }

    try {
      // Some code
    } catch (Exception e) {
      junit.framework.Assert.fail(); // Noncompliant {{Remove this failure assertion and simply add the exception type to the method signature.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

  }

  @Test
  public void testMethodOk() throws Exception {
    // Some code
    Assert.fail("Compliant : out of try-catch");
    try {
      Assert.fail("Compliant : in try");
      org.junit.jupiter.api.Assertions.fail("Compliant : in try");
    } catch (Exception e) {

    }
    org.junit.jupiter.api.Assertions.fail("Compliant : out of try-catch");
  }

}
