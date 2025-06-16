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
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.properties.PropertiesIsoVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.quarkus.QuarkusExecutionContextView;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This recipe is used to find Quarkus profiles.
 */
@EqualsAndHashCode(callSuper = false)
@Value
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
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                QuarkusExecutionContextView quarkusCtx = QuarkusExecutionContextView.view(ctx);
                return quarkusCtx.isQuarkusConfigFile(sourceFile, null);
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                tree = super.visit(tree, ctx);
                if (tree instanceof Properties.File) {
                    doAfterVisit(new PropertiesIsoVisitor<ExecutionContext>() {
                        @Override
                        public Properties.Entry visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                            entry = super.visitEntry(entry, ctx);
                            entry = entry.withValue(entry.getValue().withMarkers(entry.getValue().getMarkers().computeByType(new SearchResult(Tree.randomId(), null), (s1, s2) -> s1 == null ? s2 : s1)));
                            return super.visitEntry(entry, ctx);
                        }
                    });
                } else if (tree instanceof Yaml.Documents) {
                    doAfterVisit(new YamlIsoVisitor<ExecutionContext>() {
                        @Override
                        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                            entry = super.visitMappingEntry(entry, ctx);
                            entry = entry.withValue(entry.getValue().withMarkers(entry.getValue().getMarkers().computeByType(new SearchResult(Tree.randomId(), null), (s1, s2) -> s1 == null ? s2 : s1)));
                            return super.visitMappingEntry(entry, ctx);
                        }
                    });
                }
                return tree;
            }
        };
    }

    /**
     * Find Quarkus profiles in the given tree.
     *
     * @param tree The {@link Properties} or {@link Yaml.Documents} tree to search for Quarkus profiles.
     * @return The Quarkus profiles in use on the tree.
     */
    public static Set<String> find(Tree tree) {
        Set<String> profiles = new HashSet<>();
        new TreeVisitor<Tree, Set<String>>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, Set<String> ctx) {
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
                    new YamlIsoVisitor<Set<String>>() {
                        @Override
                        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Set<String> ctx) {
                            entry = super.visitMappingEntry(entry, ctx);
                            String prop = FindQuarkusProperties.getProperty(getCursor());
                            addProfile(prop, ctx);
                            return entry;
                        }
                    }.reduce(tree, profiles);
                }
                return tree;
            }
        }.visit(tree, profiles);
        return profiles;
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
}
