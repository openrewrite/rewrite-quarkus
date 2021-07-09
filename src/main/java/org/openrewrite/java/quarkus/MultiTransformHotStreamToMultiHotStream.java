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
package org.openrewrite.java.quarkus;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class MultiTransformHotStreamToMultiHotStream extends Recipe {
    private static final MethodMatcher HOT_STREAM_METHOD_MATCHER = new MethodMatcher("io.smallrye.mutiny.groups.MultiTransform toHotStream()");

    @Override
    public String getDisplayName() {
        return "Use Mutiny `multi.toHotStream()`";
    }

    @Override
    public String getDescription() {
        return "Replace Mutiny API usages of `multi.transform().toHotStream()` with `multi.toHotStream()`.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(HOT_STREAM_METHOD_MATCHER);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MultiTransformHotStreamToMultiHotStreamVisitor();
    }

    private static class MultiTransformHotStreamToMultiHotStreamVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher TRANSFORM_METHOD_MATCHER = new MethodMatcher("io.smallrye.mutiny.Multi transform()");
        private static final MethodMatcher SELECT_METHOD_MATCHER = new MethodMatcher("io.smallrye.mutiny.Multi select()");

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            if (HOT_STREAM_METHOD_MATCHER.matches(mi)) {
                if (mi.getSelect() != null && mi.getSelect() instanceof J.MethodInvocation) {
                    J.MethodInvocation mSelect = (J.MethodInvocation) mi.getSelect();
                    if (TRANSFORM_METHOD_MATCHER.matches(mSelect) || SELECT_METHOD_MATCHER.matches(mSelect)) {
                        return mi.withSelect(mSelect.getSelect());
                    }
                }
            }
            return mi;
        }
    }
}
