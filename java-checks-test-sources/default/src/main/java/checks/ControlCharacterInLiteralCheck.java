package checks;

public class ControlCharacterInLiteralCheck {

  String[] data = {
    // Compliant
    "NoControlCharacter", // Compliant
    "Escaped\u200BCharacter", // Compliant
    "Spaces Spaces Spaces Spaces", // Compliant

    // ASCII control character, C0 control characters
    "U+0000 ' '", // Noncompliant {{Remove the non-escaped \u0000 character from this literal.}}
//  ^^^^^^^^^^^^
    "U+0001 ''", // Noncompliant {{Remove the non-escaped \u0001 character from this literal.}}
    "U+0002 ''", // Noncompliant
    "U+0003 ''", // Noncompliant
    "U+0004 ''", // Noncompliant
    "U+0005 ''", // Noncompliant
    "U+0006 ''", // Noncompliant
    "U+0007 ''", // Noncompliant
    "U+0008 ''", // Noncompliant
    "U+0009 '	'", // Noncompliant
    "U+000B ''", // Noncompliant
    "U+000C ''", // Noncompliant
    "U+000E ''", // Noncompliant
    "U+000F ''", // Noncompliant
    "U+0010 ''", // Noncompliant
    "U+0011 ''", // Noncompliant
    "U+0012 ''", // Noncompliant
    "U+0013 ''", // Noncompliant
    "U+0014 ''", // Noncompliant
    "U+0015 ''", // Noncompliant
    "U+0016 ''", // Noncompliant
    "U+0017 ''", // Noncompliant
    "U+0018 ''", // Noncompliant
    "U+0019 ''", // Noncompliant
    "U+001A ''", // Noncompliant
    "U+001B ''", // Noncompliant
    "U+001C ''", // Noncompliant
    "U+001D ''", // Noncompliant
    "U+001E ''", // Noncompliant
    "U+001F ''", // Noncompliant
    "U+007F ''", // Noncompliant

    // Unicode characters with White_Space property
    "U+0085 '' next line", // Noncompliant
    "U+00A0 ' ' no-break space", // Noncompliant
    "U+1680 ' ' ogham space mark", // Noncompliant
    "U+2000 ' ' en quad", // Noncompliant
    "U+2001 ' ' em quad", // Noncompliant
    "U+2002 ' ' en space", // Noncompliant
    "U+2003 ' ' em space", // Noncompliant
    "U+2004 ' ' three-per-em space", // Noncompliant
    "U+2005 ' ' four-per-em space", // Noncompliant
    "U+2006 ' ' six-per-em space", // Noncompliant
    "U+2007 ' ' figure space", // Noncompliant
    "U+2008 ' ' punctuation space", // Noncompliant
    "U+2009 ' ' thin space", // Noncompliant
    "U+200A ' ' hair space", // Noncompliant
    "U+2028 ' ' line separator", // Noncompliant
    "U+2029 ' ' paragraph separator", // Noncompliant
    "U+202F ' ' narrow no-break space", // Noncompliant
    "U+205F ' ' medium mathematical space", // Noncompliant
    "U+3000 '　' ideographic space", // Noncompliant

    // Related Unicode characters without White_Space property
    "U+180E '᠎' mongolian vowel separator", // Noncompliant
    "U+200B '​' zero width space", // Noncompliant {{Remove the non-escaped \u200B character from this literal.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    "U+200C '‌' zero width non-joiner", // Noncompliant
    "U+200D '‍' zero width joiner", // Noncompliant
    "U+2060 '⁠' word joiner", // Noncompliant
    "U+FEFF '﻿' zero width non-breaking space", // Noncompliant
  };

  char[] dataChar = {
    '', // Noncompliant {{Remove the non-escaped \u000B character from this literal.}}
//  ^^^
    ' ', // Noncompliant
    '	', // Noncompliant
    '\t',
    '\u000B',
  };

}

