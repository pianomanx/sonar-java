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
package org.sonar.java.checks.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3305")
public class SpringConfigurationWithAutowiredFieldsCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE_FORMAT = "Inject this field value directly into \"%s\", the only method that uses it.";

  private static final List<String> AUTOWIRED_ANNOTATIONS = Arrays.asList(
    SpringUtils.AUTOWIRED_ANNOTATION,
    "javax.inject.Inject");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.symbol().metadata().isAnnotatedWith(SpringUtils.CONFIGURATION_ANNOTATION)) {
      Map<Symbol, VariableTree> autowiredFields = new HashMap<>();
      classTree.members().forEach(m -> collectAutowiredFields(m, autowiredFields));

      Map<Symbol, List<MethodTree>> methodsThatUseAutowiredFields = new HashMap<>();
      autowiredFields.keySet().forEach(f -> methodsThatUseAutowiredFields.put(f, new ArrayList<>()));
      classTree.members().forEach(m -> collectMethodsThatUseAutowiredFields(m, methodsThatUseAutowiredFields));

      // report autowired fields that are used by a single method, if that method is @Bean
      methodsThatUseAutowiredFields.entrySet().stream()
        .filter(methodsForField -> methodsForField.getValue().size() == 1 &&
          methodsForField.getValue().get(0).symbol().metadata().isAnnotatedWith(SpringUtils.BEAN_ANNOTATION))
        .forEach(methodsForField -> reportIssue(
          autowiredFields.get(methodsForField.getKey()).simpleName(),
          String.format(MESSAGE_FORMAT, methodsForField.getValue().get(0).simpleName().name())));
    }
  }

  private static void collectAutowiredFields(Tree tree, Map<Symbol, VariableTree> autowiredFields) {
    if (!tree.is(Tree.Kind.VARIABLE)) {
      return;
    }
    VariableTree variable = (VariableTree) tree;
    Symbol variableSymbol = variable.symbol();
    SymbolMetadata metadata = variableSymbol.metadata();

    for(String annotation: AUTOWIRED_ANNOTATIONS) {
      List<SymbolMetadata.AnnotationValue> annotationValues = metadata.valuesForAnnotation(annotation);
      if (annotationValues != null) {
        if (annotationValues.stream().anyMatch(SpringConfigurationWithAutowiredFieldsCheck::isRequiredFalse)
          && variable.initializer() != null) {
          // Common pattern used to define a default value.
          continue;
        }
        autowiredFields.put(variableSymbol, variable);
      }
    }
  }

  private static boolean isRequiredFalse(SymbolMetadata.AnnotationValue annotationValue) {
    Object value = annotationValue.value();
    return "required".equals(annotationValue.name()) && Boolean.FALSE.equals(value);
  }

  private static void collectMethodsThatUseAutowiredFields(Tree tree, Map<Symbol, List<MethodTree>> methodsThatUseAutowiredFields) {
    if (!tree.is(Tree.Kind.METHOD)) {
      return;
    }
    IdentifiersVisitor identifiersVisitor = new IdentifiersVisitor(methodsThatUseAutowiredFields.keySet());
    tree.accept(identifiersVisitor);
    // for each autowired field that is referenced in this method, add the current method name to the list
    identifiersVisitor.isFieldReferenced.entrySet().stream()
      .filter(Map.Entry::getValue)
      .map(Map.Entry::getKey)
      .forEach(field -> methodsThatUseAutowiredFields.get(field).add((MethodTree) tree));
  }

  private static class IdentifiersVisitor extends BaseTreeVisitor {
    private final Map<Symbol, Boolean> isFieldReferenced = new HashMap<>();

    IdentifiersVisitor(Set<Symbol> autowiredFields) {
      autowiredFields.forEach(f -> isFieldReferenced.put(f, false));
    }

    @Override
    public void visitIdentifier(IdentifierTree identifierTree) {
      isFieldReferenced.computeIfPresent(identifierTree.symbol(), (fieldSym, isPresent) -> true);
    }
  }
}
