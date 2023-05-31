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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.RemovePlugin;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.xml.tree.Xml;

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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                if (!FindPlugin.find(document, "io.quarkus", "quarkus-maven-plugin").isEmpty()) {
                    document = SearchResult.found(document);
                }
                return super.visitDocument(document, ctx);
            }
        }, new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                doAfterVisit(new RemovePlugin("org.apache.avro", "avro-maven-plugin").getVisitor());
                return document;
            }
        });
    }
}
