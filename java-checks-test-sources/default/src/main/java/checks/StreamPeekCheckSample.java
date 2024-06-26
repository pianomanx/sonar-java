package checks;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

class StreamPeekCheckSample {
  void foo(){
    Stream.of("one", "two", "three", "four")
      .filter(e -> e.length() > 3)
      .peek(e -> System.out.println("Filtered value: " + e)); // Noncompliant {{Remove this use of "Stream.peek".}}
//     ^^^^
    IntStream.of(1, 2, 3)
      .peek(e -> System.out.println(e)); // Noncompliant
    LongStream.of(1, 2, 3)
      .peek(e -> System.out.println(e)); // Noncompliant
    DoubleStream.of(1., 2., 3.)
      .peek(e -> System.out.println(e)); // Noncompliant
  }
}
