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
package org.openrewrite.java.quarkus;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

public class ConfigPropertiesToConfigMapping extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use `@ConfigMapping`";
    }

    @Override
    public String getDescription() {
        return "Migrate Quarkus configuration classes annotated with `@ConfigProperties` to the equivalent Smallrye `@ConfigMapping`.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new UsesType<>("io.quarkus.arc.config.ConfigProperties");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final AnnotationMatcher CONFIG_PROPERTIES_ANNOTATION_MATCHER = new AnnotationMatcher("@io.quarkus.arc.config.ConfigProperties");

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                if (cd.getLeadingAnnotations().stream().anyMatch(CONFIG_PROPERTIES_ANNOTATION_MATCHER::matches)
                        && cd.getKind().equals(J.ClassDeclaration.Kind.Type.Interface)) {
                    doAfterVisit(new ChangeType("io.quarkus.arc.config.ConfigProperties", "io.smallrye.config.ConfigMapping"));
                }
                return cd;
            }
        };
    }
}
