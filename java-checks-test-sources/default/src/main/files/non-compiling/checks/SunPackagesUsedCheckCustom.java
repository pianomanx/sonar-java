import com.sun.imageio.plugins; // Compliant
import com.sun.jersey.api.client.ClientHandlerException; // Compliant
import java.util.ArrayList;

class SunPackagesUsedCheckCustom {
  private void f() {
    com.sun.imageio.plugins.bmp d =  // Compliant
      new com.sun.imageio.plugins.bmp(); // Compliant
    java.util.List a;
    sun.Foo b; // Noncompliant {{Use classes from the Java API instead of Sun classes.}}
//  ^^^^^^^
    db.setErrorHandler(new com.sun.org.apache.xml.internal.security.utils
        .IgnoreAllErrorHandler());
    sun       // secondary
//  ^[el=+3;ec=12]<
        .Foo.toto
        .asd c;

    sun.excluded.Foo foo = null; // Compliant, excluded

  }
}
