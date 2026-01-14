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
package org.openrewrite.quarkus.quarkus2;

import lombok.Getter;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class UseReactivePanacheMongoEntityBaseUniT extends Recipe {
    private static final MethodMatcher PERSIST_MATCHER = new MethodMatcher("io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase persist()");
    private static final MethodMatcher UPDATE_MATCHER = new MethodMatcher("io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase update()");
    private static final MethodMatcher PERSIST_OR_UPDATE_MATCHER = new MethodMatcher("io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase persistOrUpdate()");

    @Getter
    final String displayName = "Use `Uni<T extends ReactivePanacheMongoEntityBase>`";

    @Getter
    final String description = "The `persist()`, `update()`, and `persistOrUpdate()` methods now return a `Uni<T extends ReactivePanacheMongoEntityBase>` instead of a `Uni<Void>` to allow chaining the methods.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                new UsesMethod<>(PERSIST_MATCHER),
                new UsesMethod<>(UPDATE_MATCHER),
                new UsesMethod<>(PERSIST_OR_UPDATE_MATCHER)
        ), new UseReactivePanacheMongoEntityBaseUniTVisitor());
    }

    private static class UseReactivePanacheMongoEntityBaseUniTVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final JavaParser.Builder<?, ?> parser =
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
                        ).collect(toList())
                );

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
                    mi = JavaTemplate.builder("#{any(io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase)}.persist().replaceWithVoid()")
                            .javaParser(parser)
                            .build().apply(new Cursor(getCursor().getParent(), mi),
                                    mi.getCoordinates().replace(),
                                    mi.getSelect());
                }
            } else if (UPDATE_MATCHER.matches(mi)) {
                if (hasVoidParameterization(mi)) {
                    mi = JavaTemplate.builder("#{any(io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase)}.update().replaceWithVoid()")
                            .javaParser(parser)
                            .build().apply(new Cursor(getCursor().getParent(), mi),
                                    mi.getCoordinates().replace(),
                                    mi.getSelect());
                }
            } else if (PERSIST_OR_UPDATE_MATCHER.matches(mi)) {
                if (hasVoidParameterization(mi)) {
                    mi = JavaTemplate.builder("#{any(io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase)}.persistOrUpdate().replaceWithVoid()")
                            .javaParser(parser)
                            .build().apply(new Cursor(getCursor().getParent(), mi),
                                    mi.getCoordinates().replace(),
                                    mi.getSelect());
                }
            }

            return mi;
        }

    }

}
