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
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import static java.util.Collections.singletonList;

public class UseIdentifierOnDefaultKafkaBroker extends Recipe {

    @Getter
    final String displayName = "Use `@Identifier(\"default-kafka-broker\")`";

    @Getter
    final String description = "Use `@io.smallrye.common.annotation.Identifier` on default kafka broker configuration.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("javax.inject.Named", null), new UseIdentifierOnDefaultKafkaBrokerVisitor());
    }

    private static class UseIdentifierOnDefaultKafkaBrokerVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final AnnotationMatcher MATCHER = new AnnotationMatcher("@javax.inject.Named(\"default-kafka-broker\")");

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            J.Annotation a = super.visitAnnotation(annotation, ctx);
            if (MATCHER.matches(a)) {
                maybeRemoveImport("javax.inject.Named");
                maybeAddImport("io.smallrye.common.annotation.Identifier");
                a = JavaTemplate.builder("@Identifier(\"default-kafka-broker\")")
                        .javaParser(JavaParser.fromJavaVersion()
                                .dependsOn(singletonList(
                                                Parser.Input.fromString(
                                                        "package io.smallrye.common.annotation;\n" +
                                                        "public interface Identifier {\n" +
                                                        "    String value();\n" +
                                                        "}"
                                                )
                                        )
                                ))
                        .imports("io.smallrye.common.annotation.Identifier")
                        .build().apply(getCursor(), a.getCoordinates().replace());
            }
            return a;
        }

    }

}
