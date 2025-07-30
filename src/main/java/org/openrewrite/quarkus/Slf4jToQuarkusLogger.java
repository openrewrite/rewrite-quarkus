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
package org.openrewrite.quarkus;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

import static java.util.Collections.nCopies;

public class Slf4jToQuarkusLogger extends Recipe {

    private static final String ORG_SLF_4_J_LOGGER = "org.slf4j.Logger";
    private static final MethodMatcher LOGGER_TRACE = new MethodMatcher(ORG_SLF_4_J_LOGGER + " trace(String, ..)");
    private static final MethodMatcher LOGGER_DEBUG = new MethodMatcher(ORG_SLF_4_J_LOGGER + " debug(String, ..)");
    private static final MethodMatcher LOGGER_INFO = new MethodMatcher(ORG_SLF_4_J_LOGGER + " info(String, ..)");
    private static final MethodMatcher LOGGER_WARN = new MethodMatcher(ORG_SLF_4_J_LOGGER + " warn(String, ..)");
    private static final MethodMatcher LOGGER_ERROR = new MethodMatcher(ORG_SLF_4_J_LOGGER + " error(String, ..)");

    @Override
    public String getDisplayName() {
        return "Migrate SLF4J Logger injection and usage to Quarkus static `Log`";
    }

    @Override
    public String getDescription() {
        return "Removes usage of SLF4J Logger fields, adjusts imports, and replaces logger method calls with static " +
                "Quarkus Log calls, including message formatting and method renaming for parameterized logging.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>(ORG_SLF_4_J_LOGGER, true),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                        if (cd != classDecl) {
                            doAfterVisit(new RemoveAnnotationVisitor(new AnnotationMatcher("@lombok.extern.slf4j.Slf4j")));
                        }
                        return cd;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation methodInvocation, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(methodInvocation, ctx);
                        if (!LOGGER_TRACE.matches(mi) &&
                                !LOGGER_DEBUG.matches(mi) &&
                                !LOGGER_INFO.matches(mi) &&
                                !LOGGER_WARN.matches(mi) &&
                                !LOGGER_ERROR.matches(mi)) {
                            return mi;
                        }

                        maybeRemoveImport(ORG_SLF_4_J_LOGGER);
                        maybeAddImport("io.quarkus.logging.Log");

                        List<Expression> args = ListUtils.mapFirst(mi.getArguments(), arg ->
                                arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String ? ((J.Literal) arg)
                                        .withValue(((String) ((J.Literal) arg).getValue()).replace("{}", "%s"))
                                        .withValueSource((((J.Literal) arg).getValueSource()).replace("{}", "%s")) : arg);
                        String placeholders = String.join(", ", nCopies(args.size(), "#{any()}"));
                        String template = String.format("Log.%s%s(%s)", mi.getSimpleName(), 1 < args.size() ? "f" : "", placeholders);
                        return JavaTemplate.builder(template)
                                .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "quarkus-core"))
                                .imports("io.quarkus.logging.Log")
                                .build()
                                .apply(updateCursor(mi), mi.getCoordinates().replace(), args.toArray());
                    }

                    @Override
                    public J.@Nullable VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable, ctx);
                        if (TypeUtils.isOfClassType(variableDeclarations.getType(), ORG_SLF_4_J_LOGGER)) {
                            maybeRemoveImport(ORG_SLF_4_J_LOGGER);
                            maybeRemoveImport("org.slf4j.LoggerFactory");
                            maybeRemoveImport("javax.inject.Inject");
                            maybeRemoveImport("jakarta.inject.Inject");
                            return null;
                        }
                        return variableDeclarations;
                    }
                }
        );
    }
}
