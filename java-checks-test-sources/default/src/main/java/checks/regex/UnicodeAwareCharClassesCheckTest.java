/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package checks.regex;

import java.util.regex.Pattern;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.URL;

public class UnicodeAwareCharClassesCheckTest {

  @Email(regexp = "[a-z]") // Noncompliant {{Replace this character range with a Unicode-aware character class.}}
//                  ^^^
  String email;

  @URL(regexp = "\\p{Lower}", flags = jakarta.validation.constraints.Pattern.Flag.DOTALL) // Noncompliant {{Enable the "(?U)" flag or use a Unicode-aware alternative.}}
// ^^^
//               ^^^^^^^^^^@-1<
  String url1;

  @URL(regexp = "(?U)\\p{Lower}") // Compliant
  String url2;

  @URL(regexp = "\\p{ASCII}") // Compliant
  String url3;

  void NoncompliantCharRanges() {
    Pattern.compile("[a-z]"); // Noncompliant {{Replace this character range with a Unicode-aware character class.}}
//                    ^^^
    Pattern.compile("[A-Z]"); // Noncompliant
    Pattern.compile("[0-9a-z]"); // Noncompliant
    Pattern.compile("[abcA-Zdef]"); // Noncompliant
    Pattern.compile("[\\x{61}-\\x{7A}]"); // Noncompliant
//                    ^^^^^^^^^^^^^^^
    Pattern.compile("[a-zA-Z]"); // Noncompliant {{Replace these character ranges with Unicode-aware character classes.}}
//                   ^^^^^^^^
//                    ^^^@-1<
//                       ^^^@-2<
    String regex = "[a-zA-Z]"; // Noncompliant
    Pattern.compile(regex + regex);
  }

  void NoncompliantPredefinedPosixClasses() {
    Pattern.compile("\\p{Lower}"); // Noncompliant {{Enable the "UNICODE_CHARACTER_CLASS" flag or use a Unicode-aware alternative.}}
//          ^^^^^^^
//                   ^^^^^^^^^^@-1<
    Pattern.compile("\\p{Alnum}"); // Noncompliant
    Pattern.compile("\\p{Space}"); // Noncompliant
    Pattern.compile("\\s"); // Noncompliant
    Pattern.compile("\\S"); // Noncompliant
    Pattern.compile("\\w"); // Noncompliant
    Pattern.compile("\\W"); // Noncompliant
    Pattern.compile("\\s\\w\\p{Lower}"); // Noncompliant
    Pattern.compile("\\S\\p{Upper}\\w"); // Noncompliant
  }

  void compliantCharRanges() {
    Pattern.compile("[0-9]"); // Compliant: we do not consider digits
    Pattern.compile("[a-y]"); // Compliant: It appears a more restrictive range than simply 'all letters'
    Pattern.compile("[D-Z]");
    Pattern.compile("[\\x{1F600}-\\x{1F637}]");
  }

  void compliantPredefinedPosixClasses() {
    Pattern.compile("\\p{ASCII}");
    Pattern.compile("\\p{Cntrl}");
    Pattern.compile("\\p{Lower}", Pattern.UNICODE_CHARACTER_CLASS);
    Pattern.compile("(?U)\\p{Lower}");
    Pattern.compile("\\w", Pattern.UNICODE_CHARACTER_CLASS);
    Pattern.compile("(?U)\\w");
    Pattern.compile("(?U:\\w)");
    Pattern.compile("\\w", Pattern.CANON_EQ | Pattern.COMMENTS | Pattern.UNICODE_CHARACTER_CLASS | Pattern.UNIX_LINES);
    Pattern.compile("\\w((?U)\\w)\\w");
    Pattern.compile("\\w(?U:[a-y])\\w"); // Compliant. We assume the developer knows what they are doing if they are using unicode flags somewhere.
  }

}
