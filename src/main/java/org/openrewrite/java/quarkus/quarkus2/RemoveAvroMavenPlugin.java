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
package org.openrewrite.java.quarkus.quarkus2;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.RemovePlugin;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.marker.XmlSearchResult;

public class RemoveAvroMavenPlugin extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove `avro-maven-plugin`";
    }

    @Override
    public String getDescription() {
        return "Removes the `avro-maven-plugin` if the `quarkus-maven-plugin` is found in the project's `pom.xml`. Avro has been integrated with the Quarkus code generation mechanism. This replaces the need to use the Avro plugin.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new MavenVisitor() {
            @Override
            public Maven visitMaven(Maven maven, ExecutionContext ctx) {
                if (!FindPlugin.find(maven, "io.quarkus", "quarkus-maven-plugin").isEmpty()) {
                    maven = maven.withMarkers(maven.getMarkers().addIfAbsent(new XmlSearchResult(RemoveAvroMavenPlugin.this)));
                }
                return super.visitMaven(maven, ctx);
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveAvroMavenPluginVisitor();
    }

    private static class RemoveAvroMavenPluginVisitor extends MavenVisitor {
        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            doAfterVisit(new RemovePlugin("org.apache.avro", "avro-maven-plugin"));
            return super.visitMaven(maven, ctx);
        }
    }

}

