/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jspecify.annotations.Nullable;
package org.openrewrite.quarkus;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.*;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SLF4JToQuarkusLogger extends Recipe {

    private static final String ORG_SLF_4_J_LOGGER = "org.slf4j.Logger";
    private static final MethodMatcher LOGGER_INFO = new MethodMatcher(String.format("%s info(..)", ORG_SLF_4_J_LOGGER));
    private static final MethodMatcher LOGGER_DEBUG = new MethodMatcher(String.format("%s debug(..)", ORG_SLF_4_J_LOGGER));
    private static final MethodMatcher LOGGER_WARN = new MethodMatcher(String.format("%s warn(..)", ORG_SLF_4_J_LOGGER));
    private static final MethodMatcher LOGGER_ERROR = new MethodMatcher(String.format("%s error(..)", ORG_SLF_4_J_LOGGER));

    @Override
    public String getDisplayName() {
        return "Migrate SLF4J Logger injection and usage to Quarkus static Log";
    }

    @Override
    public String getDescription() {
        return "Removes usage of SLF4J Logger fields, adjusts imports, and replaces logger method calls with static Quarkus Log calls, including message formatting and method renaming for parameterized logging.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesType<>(ORG_SLF_4_J_LOGGER, false),
                        new UsesMethod<>(LOGGER_INFO),
                        new UsesMethod<>(LOGGER_DEBUG),
                        new UsesMethod<>(LOGGER_WARN),
                        new UsesMethod<>(LOGGER_ERROR)
                ),
                new SLF4JToQuarkusLoggerVisitor()
        );
    }

    private static class SLF4JToQuarkusLoggerVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final String JAVAX_INJECT_INJECT = "javax.inject.Inject";
        private static final String JAKARTA_INJECT_INJECT = "jakarta.inject.Inject";
        private static final String QUARKUS_LOG = "io.quarkus.logging.Log";
        private static final String SLF4J_ANNOTATION = "lombok.extern.slf4j.Slf4j";

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration classDeclaration = super.visitClassDeclaration(classDecl, executionContext);
            boolean foundSlf4jAnnotation = !FindAnnotations.find(classDeclaration, "@" + SLF4J_ANNOTATION).isEmpty();
            if (foundSlf4jAnnotation) {
                maybeRemoveImport(SLF4J_ANNOTATION);
                doAfterVisit(new RemoveAnnotationVisitor(new AnnotationMatcher("@" + SLF4J_ANNOTATION)));
            }
            return classDeclaration;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

            Optional<String> logMethod = getLogMethod(mi, method.getArguments().size() > 1);
            if (!logMethod.isPresent()) {
                return mi;
            }

            maybeRemoveImport(ORG_SLF_4_J_LOGGER);
            maybeAddImport(QUARKUS_LOG, false);

            List<Expression> args = method.getArguments().stream()
                    .map(arg -> {
                        if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
                            return ((J.Literal) arg).withValueSource((((J.Literal) arg).getValueSource()).replaceAll("\\{\\}", "%s"));
                        } else {
                            return arg;
                        }
                    })
                    .collect(Collectors.toList());
            String placeholders = args.stream().map(a -> "#{any(String)}").collect(Collectors.joining(", "));
            JavaTemplate template = JavaTemplate.builder(logMethod.get() + "(" + placeholders + ")").contextSensitive().build();
            return template.apply(updateCursor(method), method.getCoordinates().replace(), args.toArray());
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable, executionContext);
            if (isInjectedOrFactoryLogger(variableDeclarations)) {
                maybeRemoveImport(ORG_SLF_4_J_LOGGER);
                maybeRemoveImport(JAVAX_INJECT_INJECT);
                maybeRemoveImport(JAKARTA_INJECT_INJECT);
                return null;
            }
            return variableDeclarations;
        }

        private boolean isInjectedOrFactoryLogger(J.VariableDeclarations varDecl) {
            return TypeUtils.isOfClassType(varDecl.getType(), ORG_SLF_4_J_LOGGER);
        }

        private Optional<String> getLogMethod(J.MethodInvocation mi, boolean withArgs) {
            return LOGGER_INFO.matches(mi) ? Optional.of("Log.info" + (withArgs ? "f" : "")) :
                    LOGGER_DEBUG.matches(mi) ? Optional.of("Log.debug" + (withArgs ? "f" : "")) :
                            LOGGER_WARN.matches(mi) ? Optional.of("Log.warn" + (withArgs ? "f" : "")) :
                                    LOGGER_ERROR.matches(mi) ? Optional.of("Log.error" + (withArgs ? "f" : "")) :
                                            Optional.empty();
        }
    }
}
