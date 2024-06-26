package checks;

import java.util.Collection;
import java.util.List;
import java.util.Set;

class ToArrayCheckSample<T> {
  <E extends T> Object[] foo(List<String> listOfString, List<String> listOfNumber, Set rawSet, Collection<E> col) {
    String[] a1 = (String[]) listOfString.toArray(); // Noncompliant {{Pass "new String[0]" as argument to "toArray".}} [[quickfixes=qf1]]
//                           ^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf1 {{Pass "new String[0]" as argument}}
    // edit@qf1 [[sc=51;ec=51]] {{new String[0]}}
    // edit@qf1 [[sc=19;ec=30]] {{}}
    Number[] a21 = (Number[])       listOfNumber.toArray(); // Noncompliant {{Pass "new Number[0]" as argument to "toArray".}} [[quickfixes=qf2]]
//                                  ^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf2 {{Pass "new Number[0]" as argument}}
    // edit@qf2 [[sc=58;ec=58]] {{new Number[0]}}
    // edit@qf2 [[sc=20;ec=37]] {{}}
    Number[] a22 = (Number[]) rawSet.toArray(); // Noncompliant {{Pass "new Number[0]" as argument to "toArray".}} [[quickfixes=qf3]]
//                            ^^^^^^^^^^^^^^^^
    // fix@qf3 {{Pass "new Number[0]" as argument}}
    // edit@qf3 [[sc=46;ec=46]] {{new Number[0]}}
    // Only one edit, can not remove cast without compilation error
    Number[] a23 = (Number[]) col.toArray(); // Noncompliant {{Pass "new Number[0]" as argument to "toArray".}} [[quickfixes=qf4]]
//                            ^^^^^^^^^^^^^
    // fix@qf4 {{Pass "new Number[0]" as argument}}
    // edit@qf4 [[sc=43;ec=43]] {{new Number[0]}}
    // edit@qf4 [[sc=20;ec=31]] {{}}

    Object[] a3 = listOfString.toArray(); // Compliant
    String[] a4 = listOfString.toArray(new String[0]); // Compliant
    Object[] a5 = (Object[]) listOfString.toArray(); // Compliant
    E[] a6 = (E[]) col.toArray(); // Compliant
    Object o = (Object) listOfString.toArray(); // Compliant

    Number[] a7 = (Number[]) rawSet.toArray(new Number[0]);
    Number[] a8 = col.toArray(new Number[0]);

    return listOfString.toArray(); // Compliant
  }

  abstract class MySet<P> implements Set<P> {
    void callToArray() {
      String[] a1 = (String[]) toArray(); // Compliant
      String[] a2 = toArray(new String[0]); // Compliant
    }
  }
}
