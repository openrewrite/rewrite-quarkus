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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class GrpcServiceAnnotationToGrpcClient extends Recipe {
    private static final String GRPC_SERVICE_ANNOTATION_FQN = "io.quarkus.grpc.runtime.annotations.GrpcService";
    private static final String GRPC_CLIENT_ANNOTATION_FQN = "io.quarkus.grpc.GrpcClient";

    @Getter
    final String displayName = "Use `@GrpcClient`";

    @Getter
    final String description = "The `@GrpcService` annotation is replaced with `@GrpcClient` in Quarkus 2.x. Removes the optional `@GrpcClient.value()` unless the service name is different from the name of annotated element.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(GRPC_SERVICE_ANNOTATION_FQN, null), new GrpcServiceToGrpcClientAnnotationVisitor());
    }

    private static class GrpcServiceToGrpcClientAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static boolean shouldRemoveArgument(J.VariableDeclarations.NamedVariable namedVariable, J.Literal assignValue) {
            Object value = assignValue.getValue();
            assert value != null;
            return namedVariable.getSimpleName().equals(value);
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            doAfterVisit(new ChangeType(GRPC_SERVICE_ANNOTATION_FQN, GRPC_CLIENT_ANNOTATION_FQN, true).getVisitor());
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            J.Annotation a = super.visitAnnotation(annotation, ctx);
            if (TypeUtils.isOfClassType(annotation.getType(), GRPC_SERVICE_ANNOTATION_FQN) &&
                    a.getArguments() != null &&
                    getCursor().getParentOrThrow().getValue() instanceof J.VariableDeclarations) {

                a = a.withArguments(ListUtils.map(a.getArguments(), arg -> {
                    Cursor varDecsCursor = getCursor().getParentOrThrow();
                    J.VariableDeclarations.NamedVariable namedVariable = varDecsCursor.<J.VariableDeclarations>getValue().getVariables().get(0);
                    if (arg instanceof J.Assignment) {
                        J.Assignment assignment = (J.Assignment) arg;
                        if (assignment.getVariable() instanceof J.Identifier && assignment.getAssignment() instanceof J.Literal) {
                            J.Identifier assignName = (J.Identifier) assignment.getVariable();
                            if ("value".equals(assignName.getSimpleName())) {
                                if (shouldRemoveArgument(namedVariable, (J.Literal) assignment.getAssignment())) {
                                    return null;
                                }
                            }
                        }
                    } else if (arg instanceof J.Literal) {
                        if (shouldRemoveArgument(namedVariable, (J.Literal) arg)) {
                            return null;
                        }
                    }

                    return arg;
                }));
            }

            return a;
        }

    }

}
