/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.quarkus.search;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.PropertiesIsoVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.quarkus.QuarkusExecutionContextView;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This recipe is used to find Quarkus properties.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class FindQuarkusProfiles extends Recipe {

    @Override
    public String getDisplayName() {
        return "Search Quarkus profiles";
    }

    @Override
    public String getDescription() {
        return "Search the properties for existing Quarkus profiles.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new TreeVisitor<Tree, ExecutionContext>() {
                    @Override
                    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext executionContext) {
                        QuarkusExecutionContextView quarkusCtx = QuarkusExecutionContextView.view(executionContext);
                        return sourceFile instanceof Properties.File || sourceFile instanceof Yaml.Documents &&
                                                                        quarkusCtx.isQuarkusConfigFile(sourceFile.getSourcePath(), null);
                    }
                },
                new FindQuarkusProfilesVisitor()
        );
    }

    private static class FindQuarkusProfilesVisitor extends TreeVisitor<Tree, ExecutionContext> {

        @Getter
        private final Set<String> profiles = new HashSet<>();

        @Override
        public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
            tree = super.visit(tree, ctx);
            if (tree instanceof Properties.File) {
                Properties.File propertiesFile = (Properties.File) tree;
                new PropertiesIsoVisitor<Set<String>>() {
                    @Override
                    public Properties.Entry visitEntry(Properties.Entry entry, Set<String> ctx) {
                        entry = super.visitEntry(entry, ctx);
                        addProfile(entry.getKey(), ctx);
                        return entry;
                    }
                }.reduce(propertiesFile, profiles);
            } else if (tree instanceof Yaml.Documents) {
                Yaml.Documents yamlDocuments = (Yaml.Documents) tree;
                new YamlIsoVisitor<Set<String>>() {
                    @Override
                    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Set<String> ctx) {
                        entry = super.visitMappingEntry(entry, ctx);
                        String prop = getProperty(getCursor());
                        addProfile(prop, ctx);
                        return entry;
                    }
                }.reduce(yamlDocuments, profiles);
            }
            return tree;
        }
    }

    public static Set<String> find(Tree tree) {
        FindQuarkusProfilesVisitor visitor = new FindQuarkusProfilesVisitor();
        visitor.visit(tree, new InMemoryExecutionContext());
        return visitor.getProfiles();
    }

    private static void addProfile(String propertyKey, Set<String> profiles) {
        if (propertyKey.isEmpty() || propertyKey.charAt(0) != '%') {
            return;
        }
        int index = propertyKey.indexOf('.');
        if (index == -1) {
            return;
        }
        String temp = propertyKey.substring(1, index);
        profiles.addAll(Arrays.asList(temp.split(",")));
    }

    private static String getProperty(Cursor cursor) {
        StringBuilder asProperty = new StringBuilder();
        Iterator<Object> path = cursor.getPath();
        int i = 0;
        while (path.hasNext()) {
            Object next = path.next();
            if (next instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry entry = (Yaml.Mapping.Entry) next;
                if (i++ > 0) {
                    asProperty.insert(0, '.');
                }
                asProperty.insert(0, entry.getKey().getValue());
            }
        }
        return asProperty.toString();
    }
}
