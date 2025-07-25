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
package org.sonar.plugins.java;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.api.internal.apachecommons.io.FileUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.GeneratedCheckList;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonarsource.analyzer.commons.collections.SetUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

class GeneratedCheckListTest {

  private static final String ARTIFICIAL_DESCRIPTION = "-1";

  private final Gson gson = new Gson();

  private static final Set<String> BLACK_LIST = SetUtils.immutableSetOf(
    "AbstractXPathBasedCheck.java",
    "AbstractWebXmlXPathBasedCheck.java",
    "AbstractRegexCheck.java");

  /**
   * Enforces that each check declared in list.
   */
  @Test
  void count() {
    int count = 0;
    List<File> files = getCheckFiles();
    for (File file : files) {
      if (file.getName().endsWith("Check.java") && !BLACK_LIST.contains(file.getName())) {
        count++;
      }
    }
    assertThat(GeneratedCheckList.getChecks()).hasSize(count);
  }

  private static List<File> getCheckFiles() {
    List<File> files = (List<File>) FileUtils.listFiles(new File("../java-checks/src/main/java/org/sonar/java/checks/"), new String[] {"java"}, true);
    files.addAll(FileUtils.listFiles(new File("../java-checks-aws/src/main/java/org/sonar/java/checks/"), new String[] {"java"}, true));
    return files;
  }

  @Test
  void min_check_count() {
    assertThat(GeneratedCheckList.getJavaChecks()).hasSizeGreaterThan(500);
    assertThat(GeneratedCheckList.getJavaTestChecks()).hasSizeGreaterThan(40);
    assertThat(GeneratedCheckList.getJavaChecksNotWorkingForAutoScan()).hasSizeGreaterThan(40);
    assertThat(GeneratedCheckList.getChecks()).hasSizeGreaterThan(600);
  }

  private static class CustomRulesDefinition implements RulesDefinition {

    @Override
    public void define(Context context) {
      String language = "java";
      NewRepository repository = context
        .createRepository(GeneratedCheckList.REPOSITORY_KEY, language)
        .setName("SonarQube");

      List<Class<?>> checks = GeneratedCheckList.getChecks();
      new RulesDefinitionAnnotationLoader().load(repository, checks.toArray(new Class[checks.size()]));

      for (NewRule rule : repository.rules()) {
        try {
          rule.setName("Artificial Name (set via JSON files, no need to test it)");
          rule.setMarkdownDescription(ARTIFICIAL_DESCRIPTION);
        } catch (IllegalStateException e) {
          // it means that the html description was already set in Rule annotation
          fail("Description of " + rule.key() + " should be in separate file");
        }
      }
      repository.done();
    }
  }

  /**
   * Enforces that each check has test, name and description.
   */
  @Test
  void test() {
    Map<String, String> keyMap = new HashMap<>();
    for (Class<?> cls : GeneratedCheckList.getChecks()) {
      String testName = '/' + cls.getName().replace('.', '/') + "Test.java";
      List<String> checkModules = List.of("java-checks", "java-checks-aws");

      String simpleName = cls.getSimpleName();
      // Handle legacy keys.
      Rule ruleAnnotation = AnnotationUtils.getAnnotation(cls, Rule.class);
      keyMap.put(ruleAnnotation.key(), ruleAnnotation.key());
      assertThat(checkModules.stream()
        .anyMatch(module -> Files.exists(Path.of("../", module, "src/test/java", testName))))
        .overridingErrorMessage("No test for " + simpleName)
        .isTrue();
    }

    Set<String> keys = new HashSet<>();
    CustomRulesDefinition definition = new CustomRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    List<RulesDefinition.Rule> rules = context.repository(GeneratedCheckList.REPOSITORY_KEY).rules();

    for (RulesDefinition.Rule rule : rules) {
      assertThat(keys).as("Duplicate key " + rule.key()).doesNotContain(rule.key());
      keys.add(rule.key());
      assertThat(getClass().getResource("/org/sonar/l10n/java/rules/" + GeneratedCheckList.REPOSITORY_KEY + "/" + keyMap.get(rule.key()) + ".html"))
        .overridingErrorMessage("No description for " + rule.key() + " " + keyMap.get(rule.key()))
        .isNotNull();
      assertThat(getClass().getResource("/org/sonar/l10n/java/rules/" + GeneratedCheckList.REPOSITORY_KEY + "/" + keyMap.get(rule.key()) + ".json"))
        .overridingErrorMessage("No json metadata file for " + rule.key() + " " + keyMap.get(rule.key()))
        .isNotNull();

      assertThat(rule.htmlDescription()).isNull();
      assertThat(rule.markdownDescription()).isEqualTo(ARTIFICIAL_DESCRIPTION);

      for (RulesDefinition.Param param : rule.params()) {
        assertThat(param.description()).overridingErrorMessage(rule.key() + " missing description for param " + param.key()).isNotEmpty();
      }
    }
  }

  @Test
  void enforce_CheckList_registration() {
    List<File> files = getCheckFiles();
    List<Class<?>> checks = GeneratedCheckList.getChecks();
    files.stream()
      .filter(file -> file.getName().endsWith("Check.java"))
      .filter(file -> !file.getName().startsWith("Abstract"))
      .map(File::getAbsolutePath)
      .map(f -> f.replace(File.separatorChar, '.'))
      .map(f -> f.substring(f.indexOf("org.sonar.java.checks"), f.length() - 5))
      .forEach(className -> {
        try {
          Class<?> aClass = Class.forName(className);
          assertThat(checks).as(className + " is not declared in CheckList").contains(aClass);
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException(e);
        }
      });
  }

  @Test
  void rules_targeting_tests_should_have_tests_tag() throws Exception {
    Set<Class<? extends JavaCheck>> testChecks = new HashSet<>(GeneratedCheckList.getJavaTestChecks());
    Set<Class<? extends JavaCheck>> mainChecks = new HashSet<>(GeneratedCheckList.getJavaChecks());

    for (Class<?> cls : GeneratedCheckList.getChecks()) {
      String key = AnnotationUtils.getAnnotation(cls, Rule.class).key();
      URL metadataURL = getClass().getResource("/org/sonar/l10n/java/rules/" + GeneratedCheckList.REPOSITORY_KEY + "/" + key + ".json");
      File metadataFile = new File(metadataURL.toURI());
      assertThat(metadataFile).exists();
      try (FileReader jsonReader = new FileReader(metadataFile)) {
        DummyMetatada metadata = gson.fromJson(jsonReader, DummyMetatada.class);

        if (!"deprecated".equals(metadata.status)) {
          // deprecated rules usually have no tags
          if ((testChecks.contains(cls) && !mainChecks.contains(cls)) || "S3414".equals(key)) {
            assertThat(metadata.tags)
              .as("Rule " + key + " is targeting tests sources and should contain the 'tests' tag.")
              .contains("tests");
          } else {
            assertThat(metadata.tags)
              .as("Rule " + key + " is targeting main sources and should not contain the 'tests' tag.")
              .doesNotContain("tests");
          }
        }
      }
    }
  }

  private static class DummyMetatada {
    // ignore all the other fields
    String[] tags;
    String status;
  }

}
