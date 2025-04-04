package checks;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Head;
import io.micronaut.http.annotation.Options;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.Trace;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;

public class TooManyParametersCheckSample {
  TooManyParametersCheckSample(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // Noncompliant {{Constructor has 8 parameters, which is greater than 7 authorized.}}
  }

  void method(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // Noncompliant {{Method has 8 parameters, which is greater than 7 authorized.}}
//     ^^^^^^
  }

  void otherMethod(int p1) {}

  static void staticMethod(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // Noncompliant

  @CustomAnnotation
  void customAnnotatedMethod(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // Noncompliant
}

class TooManyParametersExtended extends TooManyParametersCheckSample {
  TooManyParametersExtended(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {
    super(p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  void method(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {}

  static void staticMethod(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // Noncompliant
}

class MethodsUsingJsonCreator {
  @JsonCreator
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingAnnotations {
  @javax.ws.rs.GET
  public void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @javax.ws.rs.POST
  public void foo1(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @javax.ws.rs.PUT
  public void foo2(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @javax.ws.rs.PATCH
  public void foo3(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @javax.inject.Inject
  public void foo5(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MicronautHttpAnnotations{
  
  @Get
  public void get(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Post
  public void post(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Put
  public void put(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Delete
  public void delete(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Options
  public void options(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Patch
  public void patch(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Head
  public void head(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Trace
  public void trace(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  
}

/* 
 * Exceptions to the rule : RECORD, ANNOTATION_TYPE (annotations cannot have method params nor constructors)
 */
record Record1(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant 

class JakartaMethodsUsingAnnotations {
  @jakarta.ws.rs.GET
  public void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @jakarta.ws.rs.POST
  public void foo1(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @jakarta.ws.rs.PUT
  public void foo2(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @jakarta.ws.rs.PATCH
  public void foo3(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @jakarta.inject.Inject
  public void foo5(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class AllowLombokBuilder {
  @Builder
  AllowLombokBuilder(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {} // Compliant
}

class AllowSpringAutowired {
  @Autowired
  AllowSpringAutowired(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {} // Compliant
}

@Target(ElementType.METHOD)
@interface CustomAnnotation { }
