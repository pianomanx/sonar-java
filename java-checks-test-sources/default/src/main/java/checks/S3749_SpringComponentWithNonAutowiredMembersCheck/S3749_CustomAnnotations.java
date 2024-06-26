package checks.S3749_SpringComponentWithNonAutowiredMembersCheck;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class S3749_CustomAnnotations {

  @interface MyInjectionAnnotation {}

  @interface MyUnrelatedAnnotation {}

  String field1 = null; // Noncompliant

  @MyInjectionAnnotation
  String field2 = null;

  @MyUnrelatedAnnotation
  String field3 = null; // Noncompliant

  @Autowired
  String field4 = null;
}

@Controller
class ConstructorInjection {
  private String env;  // Compliant
  private String yyyAdaptor; // Compliant
  private String jaxbContext; // Noncompliant

  @S3749_CustomAnnotations.MyInjectionAnnotation
  public ConstructorInjection(String env, String yyyAdaptor,
                              @Qualifier("YYYYReq") String jaxbContext) {

    this.env = env;
    this.yyyAdaptor = yyyAdaptor;
  }

  @S3749_CustomAnnotations.MyInjectionAnnotation
  public ConstructorInjection(String env, String yyyAdaptor) {
    this.env = env;
    this.yyyAdaptor = yyyAdaptor;
  }
}

