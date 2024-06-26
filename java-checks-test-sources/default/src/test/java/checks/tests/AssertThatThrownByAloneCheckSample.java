package checks.tests;

import java.io.IOException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AssertThatThrownByAloneCheckSample {

  @Test
  void noncompliant() {
    assertThatThrownBy(() -> shouldThrow()); // Noncompliant {{Test further the exception raised by this assertThatThrownBy call.}}
//  ^^^^^^^^^^^^^^^^^^
    assertThatThrownBy(() -> shouldThrow(), "Don't do that"); // Noncompliant {{Test further the exception raised by this assertThatThrownBy call.}}
//  ^^^^^^^^^^^^^^^^^^
  }

  @Test
  void compliant() {
    AbstractThrowableAssert<?, ? extends Throwable> assertion = assertThatThrownBy(() -> shouldThrow());
    assertion.hasMessage("my exception");
    assertThatThrownBy(() -> shouldThrow()).isInstanceOf(IOException.class);
    assertThatThrownBy(() -> shouldThrow()).hasMessage("My exception");
  }

  void shouldThrow() {
    throw new IllegalStateException();
  }

}
