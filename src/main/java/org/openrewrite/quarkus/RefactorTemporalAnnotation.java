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

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotation;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Annotation;
import org.openrewrite.java.tree.J.FieldAccess;
import org.openrewrite.java.tree.J.Identifier;

@Value
@EqualsAndHashCode(callSuper = false)
public class RefactorTemporalAnnotation extends Recipe {

    private static final String TEMPORAL_ANNOTATION = "jakarta.persistence.Temporal";
    private static final String ENTITY_ANNOTATION = "jakarta.persistence.Entity";
    private static final String DATE_TYPE = "java.util.Date";
    private static final String JAVA_TIME_LOCAL_DATE = "java.time.LocalDate";
    private static final String JAVA_TIME_LOCAL_TIME = "java.time.LocalTime";
    private static final String JAVA_TIME_OFFSETDATETIME = "java.time.OffsetDateTime";
    private static final String JAVA_TIME_LOCAL_DATE_TIME = "java.time.LocalDateTime";

    private static final TypeMatcher DATE_MATCHER = new TypeMatcher(DATE_TYPE);

    @Override
    public String getDisplayName() {
        return "Refactor @Temporal annotation java.util.Date fields to java.time API";
    }

    @Override
    public String getDescription() {
        return "Replace java.util.Date fields annotated with @Temporal " +
                "with java.time.LocalDate, java.time.LocalTime, java.time.LocalDateTime or java.time.OffsetDateTime.";
    }

    @Option(displayName = "Use offsetDateTime",
            description = "If `true` the recipe will use OffsetDateTime instead of LocalDateTime. Default `false`.",
            required = false)
    @Nullable
    Boolean useOffsetDateTime;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesType<>(ENTITY_ANNOTATION, true),
                        new UsesType<>(DATE_TYPE, true),
                        new UsesType<>(TEMPORAL_ANNOTATION, true)
                ),
                new TemporalRefactorVisitor(Boolean.TRUE.equals(useOffsetDateTime))
        );
    }

    @RequiredArgsConstructor
    private static class TemporalRefactorVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final boolean useOffsetDT;

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            J.VariableDeclarations decls = super.visitVariableDeclarations(multiVariable, ctx);

            if (!DATE_MATCHER.matches(decls.getTypeExpression())) {
                return decls;
            }

            String newTypeToUse = FindAnnotations.find(decls, TEMPORAL_ANNOTATION)
                    .stream()
                    .findFirst()
                    .map(Annotation::getArguments)
                    .map(args -> args.get(0))
                    .map(arg -> {
                        if (arg instanceof FieldAccess) {
                            return getNewType(((FieldAccess) arg).getSimpleName());
                        }
                        if (arg instanceof Identifier) {
                            return getNewType(((Identifier) arg).getSimpleName());
                        }
                        return null;
                    })
                    .orElse(null);
            if (newTypeToUse == null) {
                return decls;
            }

            doAfterVisit(new RemoveAnnotation(TEMPORAL_ANNOTATION).getVisitor());

            maybeRemoveImport(DATE_TYPE);
            maybeAddImport(newTypeToUse);
            return (J.VariableDeclarations) new ChangeType(DATE_TYPE, newTypeToUse, null).getVisitor().visitNonNull(decls, ctx);
        }

        private @Nullable String getNewType(String temporalConstant) {
            switch (temporalConstant) {
                case "DATE":
                    return JAVA_TIME_LOCAL_DATE;
                case "TIME":
                    return JAVA_TIME_LOCAL_TIME;
                case "TIMESTAMP":
                    if (useOffsetDT) {
                        return JAVA_TIME_OFFSETDATETIME;
                    } else {
                        return JAVA_TIME_LOCAL_DATE_TIME;
                    }
                default:
                    return null;
            }
        }
    }
}
