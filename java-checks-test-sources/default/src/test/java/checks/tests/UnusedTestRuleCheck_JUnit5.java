package checks.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TestInfo Demo")
class UnusedTestRuleCheck_Junit5 {

  @TempDir
  static Path sharedTempDir;
  @TempDir
  static Path unusedSharedTempDir; // Noncompliant {{Remove this unused "TempDir".}}
//            ^^^^^^^^^^^^^^^^^^^
  @TempDir
  File tempDir;
  @TempDir
  File unusedTempDir; // Noncompliant {{Remove this unused "TempDir".}}
//     ^^^^^^^^^^^^^

  UnusedTestRuleCheck_Junit5(TestInfo testInfo) {
    assertEquals("TestInfo Demo", testInfo.getDisplayName());
  }

  @BeforeEach
  void init(TestInfo testInfo) {
    String displayName = testInfo.getDisplayName();
    assertTrue(displayName.equals("TEST 1") || displayName.equals("test2()"));
  }

  @Test
  @DisplayName("TEST 1")
  @Tag("my-tag")
  void test1(TestInfo testInfo) {
    assertEquals("TEST 1", testInfo.getDisplayName());
    assertTrue(testInfo.getTags().contains("my-tag"));
  }

  @Test
  void test2() {
  }

  @Test
  void testUsingTempDir() throws IOException {
    Path file1 = sharedTempDir.resolve("test.txt");
    Path file2 = tempDir.toPath().resolve("test.txt");
    assertTrue(true);
  }

  @Test
  void testUsingTempDirParam(@TempDir Path tempDirParam) throws IOException {
    Path file = tempDirParam.resolve("test.txt");
    assertTrue(true);
  }

  @Test
  void testNotUsingTempDirParam(@TempDir Path unusedTempDirParam) throws IOException { // Noncompliant {{Remove this unused "TempDir".}}
//                                            ^^^^^^^^^^^^^^^^^^
    assertTrue(true);
  }

  void helperMethodWithParam(Double doubleValue) {
  }
}

@DisplayName("TestInfo Demo")
class Test2 {

  interface ForCoverage {
  }

  Test2(TestInfo testInfo) { // Noncompliant {{Remove this unused "TestInfo".}}
//               ^^^^^^^^
  }

  @BeforeEach
  void init(TestInfo testInfo) { // Noncompliant {{Remove this unused "TestInfo".}}
//                   ^^^^^^^^
    String displayName = "abc";
    assertTrue(displayName.equals("TEST 1") || displayName.equals("test2()"));
  }

  @Test
  @DisplayName("TEST 1")
  @Tag("my-tag")
  void test1(TestInfo testInfo) { // Noncompliant
    assertEquals("TEST 1", "another");
  }

}
