/*
 * Copyright 2021 the original author or authors.
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

public class UseReactivePanacheMongoEntityBaseUniT extends Recipe {
    private static final MethodMatcher PERSIST_MATCHER = new MethodMatcher("io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase persist()");
    private static final MethodMatcher UPDATE_MATCHER = new MethodMatcher("io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase update()");
    private static final MethodMatcher PERSIST_OR_UPDATE_MATCHER = new MethodMatcher("io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase persistOrUpdate()");
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
                                    "package io.quarkus.mongodb.panache.reactive;" +
                                    "import io.smallrye.mutiny.Uni;" +
                                    "public abstract class ReactivePanacheMongoEntityBase {" +
                                    "    public <T extends ReactivePanacheMongoEntityBase> Uni<T> persist() {};" +
                                    "    public <T extends ReactivePanacheMongoEntityBase> Uni<T> update() {};" +
                                    "    public <T extends ReactivePanacheMongoEntityBase> Uni<T> persistOrUpdate() {};" +
                                    "}"
                            )
                    ).collect(Collectors.toList())
            ).build());

    @Override
    public String getDisplayName() {
        return "Use `Uni<T extends ReactivePanacheMongoEntityBase>`";
    }

    @Override
    public String getDescription() {
        return "The `persist()`, `update()`, and `persistOrUpdate()` methods now return a `Uni<T extends ReactivePanacheMongoEntityBase>` instead of a `Uni<Void>` to allow chaining the methods.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                doAfterVisit(new UsesMethod<>(PERSIST_MATCHER));
                doAfterVisit(new UsesMethod<>(UPDATE_MATCHER));
                doAfterVisit(new UsesMethod<>(PERSIST_OR_UPDATE_MATCHER));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UseReactivePanacheMongoEntityBaseUniTVisitor();
    }

    private static class UseReactivePanacheMongoEntityBaseUniTVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static boolean hasVoidParameterization(J.MethodInvocation method) {
            JavaType.Parameterized returnType = TypeUtils.asParameterized(method.getType());
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
                            JavaTemplate.builder(this::getCursor, "#{any(io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase)}.persist().replaceWithVoid()")
                                    .javaParser(PARSER::get)
                                    .build(),
                            mi.getCoordinates().replace(),
                            mi.getSelect()
                    );
                }
            } else if (UPDATE_MATCHER.matches(mi)) {
                if (hasVoidParameterization(mi)) {
                    mi = mi.withTemplate(
                            JavaTemplate.builder(this::getCursor, "#{any(io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase)}.update().replaceWithVoid()")
                                    .javaParser(PARSER::get)
                                    .build(),
                            mi.getCoordinates().replace(),
                            mi.getSelect()
                    );
                }
            } else if (PERSIST_OR_UPDATE_MATCHER.matches(mi)) {
                if (hasVoidParameterization(mi)) {
                    mi = mi.withTemplate(
                            JavaTemplate.builder(this::getCursor, "#{any(io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase)}.persistOrUpdate().replaceWithVoid()")
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
