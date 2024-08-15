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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodTargetToStatic;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class UsePanacheEntityBaseStaticMethods extends Recipe {
    private static final MethodMatcher GET_ENTITY_MANAGER = new MethodMatcher("io.quarkus.hibernate.orm.panache.PanacheEntityBase getEntityManager()");
    private static final MethodMatcher FLUSH = new MethodMatcher("io.quarkus.hibernate.orm.panache.PanacheEntityBase flush()");

    @Override
    public String getDisplayName() {
        return "Use `PanacheEntityBase` static methods";
    }

    @Override
    public String getDescription() {
        return "The `getEntityManager()` and the `flush()` methods of `PanacheEntityBase` are now static methods.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                new UsesMethod<>(GET_ENTITY_MANAGER),
                new UsesMethod<>(FLUSH)
        ), new UsePanacheEntityBaseStaticMethodsVisitor());
    }

    private static class UsePanacheEntityBaseStaticMethodsVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static @Nullable String getSelectTypeFQ(J.MethodInvocation method) {
            if (method.getSelect() != null) {
                JavaType.FullyQualified selectType = TypeUtils.asFullyQualified(method.getSelect().getType());
                if (selectType != null) {
                    return selectType.getFullyQualifiedName();
                }
            }
            return null;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (GET_ENTITY_MANAGER.matches(method)) {
                String selectType = getSelectTypeFQ(method);
                if (selectType != null) {
                    doAfterVisit(new ChangeMethodTargetToStatic("io.quarkus.hibernate.orm.panache.PanacheEntityBase getEntityManager()", selectType, null, null).getVisitor());
                }
            } else if (FLUSH.matches(method)) {
                String selectType = getSelectTypeFQ(method);
                if (selectType != null) {
                    doAfterVisit(new ChangeMethodTargetToStatic("io.quarkus.hibernate.orm.panache.PanacheEntityBase flush()", selectType, null, null).getVisitor());
                }
            }

            return super.visitMethodInvocation(method, ctx);
        }

    }

}
