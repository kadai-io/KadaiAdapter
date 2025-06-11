/*
 * Copyright [2024] [envite consulting GmbH]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.kadai;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.OnlyIncludeTests;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.springframework.scheduling.annotation.Scheduled;

class ArchitectureTest {

  public static final String[] PACKAGE_NAMES = {"io.kadai", "acceptance"};
  private static final JavaClasses IMPORTED_CLASSES =
      new ClassFileImporter().importPackages(PACKAGE_NAMES);

  private static final JavaClasses IMPORTED_TEST_CLASSES =
      new ClassFileImporter(List.of(new OnlyIncludeTests())).importPackages(PACKAGE_NAMES);

  @Test
  void testMethodNamesShouldMatchAccordingToOurGuidelines() {
    methods()
        .that(
            are(
                annotatedWith(Test.class)
                    .or(annotatedWith(TestFactory.class))
                    .or(annotatedWith(TestTemplate.class))))
        .and()
        .areNotDeclaredIn(ArchitectureTest.class)
        .should()
        .haveNameMatching("^should_[A-Z][^_]+(_(For|When)_[A-Z][^_]+)?$")
        .check(IMPORTED_TEST_CLASSES);
  }

  @Test
  void testClassesAndTestMethodsShouldBePackagePrivate() {
    classes()
        .that()
        .haveSimpleNameStartingWith("Test")
        .or()
        .haveSimpleNameEndingWith("Test")
        .should()
        .bePackagePrivate()
        .check(IMPORTED_TEST_CLASSES);
    methods()
        .that()
        .areDeclaredInClassesThat()
        .haveSimpleNameStartingWith("Test")
        .or()
        .areDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Test")
        .should()
        .bePackagePrivate()
        .orShould()
        .bePrivate()
        .check(IMPORTED_TEST_CLASSES);
  }

  @Test
  void noMethodsShouldUseThreadSleep() {
    noClasses()
        .should()
        .callMethod(Thread.class, "sleep", long.class)
        .orShould()
        .callMethod(Thread.class, "sleep", long.class, int.class)
        .check(IMPORTED_CLASSES);
  }

  @Test
  void noMethodsShouldUsePrintln() {
    noClasses().should().callMethodWhere(target(nameMatching("println"))).check(IMPORTED_CLASSES);
  }

  @Test
  void noImportsForOldJunitClasses() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.junit.Test")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.junit.Assert")
        .check(IMPORTED_TEST_CLASSES);
  }

  @Test
  void noImportsForJunitAssertionsWeWantAssertJ() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.junit.jupiter.api.Assertions")
        .check(IMPORTED_TEST_CLASSES);
  }

  @Test
  void methodsAnnotatedWithScheduledMustCallTouchMethod() {
    methods()
        .that()
        .areAnnotatedWith(Scheduled.class)
        .and()
        .areNotDeclaredIn(ArchitectureTest.class)
        .should(
            new ArchCondition<JavaMethod>("call touch() method from LastSchedulerRun") {
              @Override
              public void check(JavaMethod method, ConditionEvents events) {
                List<JavaMethodCall> touchCalls =
                    method.getMethodCallsFromSelf().stream()
                        .filter(call -> call.getTarget().getName().equals("touch"))
                        .collect(Collectors.toList());

                if (touchCalls.isEmpty()) {
                  events.add(
                      SimpleConditionEvent.violated(
                          method,
                          "Method "
                              + method.getFullName()
                              + " does not call touch() from LastSchedulerRun"));
                }
              }
            })
        .check(IMPORTED_CLASSES);
  }
}
