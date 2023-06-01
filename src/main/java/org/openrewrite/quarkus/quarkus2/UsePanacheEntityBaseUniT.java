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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UsePanacheEntityBaseUniT extends Recipe {
    private static final MethodMatcher PERSIST_MATCHER = new MethodMatcher("io.quarkus.hibernate.reactive.panache.PanacheEntityBase persist()");
    private static final MethodMatcher PERSIST_AND_FLUSH_MATCHER = new MethodMatcher("io.quarkus.hibernate.reactive.panache.PanacheEntityBase persistAndFlush()");
    private static final JavaParser.Builder<?, ?> PARSER =
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
                    ).collect(Collectors.toList()));

    @Override
    public String getDisplayName() {
        return "Use `Uni<T extends PanacheEntityBase>`";
    }

    @Override
    public String getDescription() {
        return "The `persist()` and `persistAndFlush()` methods now return an `Uni<T extends PanacheEntityBase>` instead of an `Uni<Void>` to allow chaining the methods.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                new UsesMethod<>(PERSIST_MATCHER),
                new UsesMethod<>(PERSIST_AND_FLUSH_MATCHER)
        ), new UsePanacheEntityBaseUniTVisitor());
    }

    private static class UsePanacheEntityBaseUniTVisitor extends JavaIsoVisitor<ExecutionContext> {
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
                    mi = JavaTemplate.builder("#{any(io.quarkus.hibernate.reactive.panache.PanacheEntityBase)}.persist().replaceWithVoid()")
                            .javaParser(PARSER)
                            .build().apply(new Cursor(getCursor().getParent(), mi),
                                    mi.getCoordinates().replace(),
                                    mi.getSelect());
                }
            } else if (PERSIST_AND_FLUSH_MATCHER.matches(mi)) {
                if (hasVoidParameterization(mi)) {
                    mi = JavaTemplate.builder("#{any(io.quarkus.hibernate.reactive.panache.PanacheEntityBase)}.persistAndFlush().replaceWithVoid()")
                            .javaParser(PARSER)
                            .build().apply(new Cursor(getCursor().getParent(), mi),
                                    mi.getCoordinates().replace(),
                                    mi.getSelect());
                }
            }

            return mi;
        }

    }

}
