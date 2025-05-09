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
package org.sonar.java.se.checks;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.se.checks.XxeProcessingCheck.XmlSetXIncludeAware;
import org.sonar.java.se.checks.XxeProperty.FeatureXInclude;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.plugins.java.api.semantic.MethodMatchers;

import static org.sonar.java.se.checks.XxeProcessingCheck.PARSING_METHODS;

/**
 * This check uses the symbolic value and constraints set by XxeProcessingCheck.
 * It must therefore always be executed afterwards.
 *
 * @see org.sonar.java.se.checks.XxeProcessingCheck
 */
@Rule(key = "S6373")
public class AllowXMLInclusionCheck extends AbstractXMLProcessing {

  private static final List<Class<? extends Constraint>> DOMAINS = Arrays.asList(FeatureXInclude.class, XmlSetXIncludeAware.class);

  @Override
  protected MethodMatchers getParsingMethods() {
    return PARSING_METHODS;
  }

  protected boolean isUnSecuredByProperty(@Nullable ConstraintsByDomain constraintsByDomain) {
    if (constraintsByDomain == null) {
      // Not vulnerable unless some properties are explicitly set.
      return false;
    }
    return (constraintsByDomain.hasConstraint(FeatureXInclude.ENABLE)
      || constraintsByDomain.hasConstraint(XmlSetXIncludeAware.ENABLE))
      && !constraintsByDomain.hasConstraint(XxeProcessingCheck.XxeEntityResolver.CUSTOM_ENTITY_RESOLVER);
  }

  @Override
  protected String getMessage() {
    return "Disable the inclusion of files in XML processing.";
  }

  @Override
  protected boolean shouldTrackConstraint(Constraint constraint) {
    return constraint == FeatureXInclude.ENABLE || constraint == XmlSetXIncludeAware.ENABLE;
  }

  @Override
  protected List<Class<? extends Constraint>> getDomains() {
    return DOMAINS;
  }

}
