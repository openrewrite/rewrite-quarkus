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

    private static final MethodMatcher hotStreamMethodMatcher = new MethodMatcher("io.smallrye.mutiny.groups.MultiTransform toHotStream()");

    @Override
    public String getDisplayName() {
        return "Mutiny `multi.transform().hotStream()` to `multi.hotStream()`.";
    }

    @Override
    public String getDescription() {
        return "Replace deprecated Mutiny `multi.transform().toHotStream()` with `multi.toHotStream()`.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(hotStreamMethodMatcher);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final MethodMatcher transformMethodMatcher = new MethodMatcher("io.smallrye.mutiny.Multi transform()");
            final MethodMatcher selectMethodMatcher = new MethodMatcher("io.smallrye.mutiny.Multi select()");

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
                if (hotStreamMethodMatcher.matches(mi)) {
                    if (mi.getSelect() != null && mi.getSelect() instanceof J.MethodInvocation) {
                        J.MethodInvocation mSelect = (J.MethodInvocation)mi.getSelect();
                        if (transformMethodMatcher.matches(mSelect) || selectMethodMatcher.matches(mSelect)) {
                            return mi.withSelect(mSelect.getSelect());
                        }
                    }
                }
                return mi;
            }
        };
    }
}
