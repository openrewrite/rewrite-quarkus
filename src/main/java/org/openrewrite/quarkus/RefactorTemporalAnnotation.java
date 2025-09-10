package org.openrewrite.quarkus;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
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

import java.util.Objects;

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

    @Override
    public String getDisplayName() {
        return "Refactor @Temporal annotation java.util.Date fields to java.time API";
    }

    @Override
    public String getDescription() {
        return "Replace java.util.Date fields annotated with @Temporal "
                + " with java.time.LocalDate, java.time.LocalTime, java.time.LocalDateTime or java.time.OffsetDateTime.";
    }

    @Option(displayName = "Use offsetDateTime",
            description = "If `true` the recipe will use OffsetDateTime instead of LocalDateTime. Default `false`.",
            required = false)
    @Nullable
    Boolean useOffsetDateTime;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>(ENTITY_ANNOTATION, true),
                new TemporalRefactorVisitor(useOffsetDateTime)
        );
    }

    private static class TemporalRefactorVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final TypeMatcher DATE_MATCHER = new TypeMatcher(DATE_TYPE);

        private final Boolean useOffsetDT;

        public TemporalRefactorVisitor(Boolean useOffsetDateTime) {
            useOffsetDT = useOffsetDateTime;
        }

        @Override
        public J.@NotNull VariableDeclarations visitVariableDeclarations(J.@NotNull VariableDeclarations multiVariable, @NotNull ExecutionContext ctx) {
            J.VariableDeclarations decls = super.visitVariableDeclarations(multiVariable, ctx);

            if (!DATE_MATCHER.matches(decls.getTypeExpression())) {
                return decls;
            }

            Annotation temporalAnnotation = FindAnnotations.find(decls, TEMPORAL_ANNOTATION).stream()
                    .findFirst()
                    .orElse(null);

            if (temporalAnnotation == null) {
                return decls;
            }

            // Extract the enum constant (DATE or TIMESTAMP)
            String newTypeToUse = Objects.requireNonNull(temporalAnnotation.getArguments()).stream()
                    .findFirst()
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
            decls = (J.VariableDeclarations) new ChangeType(DATE_TYPE, newTypeToUse, null).getVisitor().visitNonNull(decls, ctx);

            maybeRemoveImport(DATE_TYPE);
            maybeAddImport(newTypeToUse);
            return decls;
        }

        private String getNewType(String temporalConstant) {
            switch (temporalConstant) {
                case "DATE":
                    return JAVA_TIME_LOCAL_DATE;
                case "TIME":
                    return JAVA_TIME_LOCAL_TIME;
                case "TIMESTAMP":
                    if (Boolean.TRUE.equals(useOffsetDT)) {
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
