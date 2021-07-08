/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.quarkus.quarkus2;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UsePanacheEntityBaseUniT extends Recipe {
    private static final MethodMatcher PERSIST_MATCHER = new MethodMatcher("io.quarkus.hibernate.reactive.panache.PanacheEntityBase persist()");
    private static final MethodMatcher PERSIST_AND_FLUSH_MATCHER = new MethodMatcher("io.quarkus.hibernate.reactive.panache.PanacheEntityBase persistAndFlush()");
    private static final ThreadLocal<JavaParser> PARSER = ThreadLocal.withInitial(() ->
            JavaParser.fromJavaVersion().dependsOn(
                    Stream.of(
                            Parser.Input.fromString("" +
                                    "package io.smallrye.mutiny;" +
                                    "public interface Uni<T> {" +
                                    "    Uni<Void> replaceWithVoid() {};" +
                                    "}"
                            ),
                            Parser.Input.fromString("" +
                                    "package io.quarkus.hibernate.reactive.panache;" +
                                    "import io.smallrye.mutiny.Uni;" +
                                    "public abstract class PanacheEntityBase {" +
                                    "    public <T extends PanacheEntityBase> Uni<T> persist() {};" +
                                    "    public <T extends PanacheEntityBase> Uni<T> persistAndFlush() {};" +
                                    "}"
                            )
                    ).collect(Collectors.toList())
            ).build());

    @Override
    public String getDisplayName() {
        return "Return `Uni<T extends PanacheEntityBase>` on `PanacheEntityBase#persist()`";
    }

    @Override
    public String getDescription() {
        return "The `persist()` and `persistAndFlush()` methods now return an `Uni<T extends PanacheEntityBase>` instead of an `Uni<Void>` to allow chaining the methods.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                doAfterVisit(new UsesMethod<>(PERSIST_MATCHER));
                doAfterVisit(new UsesMethod<>(PERSIST_AND_FLUSH_MATCHER));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UsePanacheEntityBaseUniTVisitor();
    }

    private static class UsePanacheEntityBaseUniTVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static boolean hasVoidParameterization(J.MethodInvocation method) {
            JavaType.Parameterized returnType = TypeUtils.asParameterized(method.getReturnType());
            if (returnType != null) {
                List<JavaType> parameterized = returnType.getTypeParameters();
                if (!parameterized.isEmpty()) {
                    return TypeUtils.isOfClassType(parameterized.get(0), "java.lang.Void");
                }
            }
            return false;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            if (PERSIST_MATCHER.matches(mi)) {
                if (hasVoidParameterization(mi)) {
                    mi = mi.withTemplate(
                            JavaTemplate.builder(this::getCursor, "#{any(io.quarkus.hibernate.reactive.panache.PanacheEntityBase)}.persist().replaceWithVoid()")
                                    .javaParser(PARSER::get)
                                    .build(),
                            mi.getCoordinates().replace(),
                            mi.getSelect()
                    );
                }
            } else if (PERSIST_AND_FLUSH_MATCHER.matches(mi)) {
                if (hasVoidParameterization(mi)) {
                    mi = mi.withTemplate(
                            JavaTemplate.builder(this::getCursor, "#{any(io.quarkus.hibernate.reactive.panache.PanacheEntityBase)}.persistAndFlush().replaceWithVoid()")
                                    .javaParser(PARSER::get)
                                    .build(),
                            mi.getCoordinates().replace(),
                            mi.getSelect()
                    );
                }
            }

            return mi;
        }

    }

}
