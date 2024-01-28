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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.properties.PropertiesIsoVisitor;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.quarkus.QuarkusExecutionContextView;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;
import org.openrewrite.yaml.tree.YamlKey;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindQuarkusProperties extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find Quarkus property";
    }

    @Override
    public String getDescription() {
        return "Finds occurrences of a Quarkus property key.";
    }

    @Option(displayName = "Property key",
            description = "The property key to look for.",
            example = "quarkus.http.port")
    String propertyKey;

    @Option(displayName = "Profile",
            description = "The profile where the property is defined. If not specified, the property will be changed on the default profile.",
            required = false,
            example = "dev")
    @Nullable
    String profile;

    @Option(displayName = "Search on all Profiles",
            description = "If set, the property will be searched on all available profiles.",
            required = false,
            example = "false")
    @Nullable
    Boolean searchAllProfiles;

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate()
                .and(Validated.notBlank("propertyKey", propertyKey));

        if (StringUtils.isNotEmpty(profile)) {
            validated = validated.and(Validated
                    .test("searchAllProfiles", "cannot be used together with profile", searchAllProfiles, x -> x == null || !x)
            );
        }

        return validated;
    }

    /**
     * Find a set of matching {@link Properties}.
     *
     * @param p                 The set of properties to search over.
     * @param propertyKey       The name of property key to look for.
     * @param profile           The profile where the property is defined. If not specified, the property will be changed on the default profile.
     * @param searchAllProfiles If set, the property will be searched on all available profiles.
     * @return The set of found properties matching the propertyKey.
     */
    public static Set<Properties.Entry> find(Properties p, String propertyKey, @Nullable String profile, @Nullable Boolean searchAllProfiles) {
        final Pattern pattern = Pattern.compile(getSearchRegex(propertyKey, profile, searchAllProfiles));
        PropertiesIsoVisitor<Set<Properties.Entry>> findVisitor = new PropertiesIsoVisitor<Set<Properties.Entry>>() {
            @Override
            public Properties.Entry visitEntry(Properties.Entry entry, Set<Properties.Entry> ps) {
                if (pattern.matcher(entry.getKey()).find()) {
                    ps.add(entry);
                }
                return super.visitEntry(entry, ps);
            }
        };

        Set<Properties.Entry> entries = new TreeSet<>(Comparator.comparing(Properties.Entry::getKey));
        findVisitor.visit(p, entries);
        return entries;
    }

    /**
     * Find a set of matching {@link Properties}.
     *
     * @param y                 The set of properties to search over.
     * @param propertyKey       The name of property key to look for.
     * @param profile           The profile where the property is defined. If not specified, the property will be changed on the default profile.
     * @param searchAllProfiles If set, the property will be searched on all available profiles.
     * @return The set of found properties matching the propertyKey.
     */
    public static Set<Yaml.Mapping.Entry> find(Yaml.Documents y, String propertyKey, @Nullable String profile, @Nullable Boolean searchAllProfiles) {
        final Pattern pattern = Pattern.compile(getSearchRegex(propertyKey, profile, searchAllProfiles));
        YamlIsoVisitor<Set<Yaml.Mapping.Entry>> findVisitor = new YamlIsoVisitor<Set<Yaml.Mapping.Entry>>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Set<Yaml.Mapping.Entry> entries) {
                entry = super.visitMappingEntry(entry, entries);
                String prop = getProperty(getCursor());
                if (pattern.matcher(prop).find()) {
                    YamlKey newKey = entry.getKey().copyPaste();
                    if (newKey instanceof Yaml.Scalar) {
                        newKey = ((Yaml.Scalar) newKey).withValue(prop);
                    }
                    entries.add(entry.copyPaste().withKey(newKey));
                }
                return entry;
            }
        };

        Set<Yaml.Mapping.Entry> entries = new TreeSet<>((o1, o2) -> {
            if (o1.getKey() instanceof Yaml.Scalar && o2.getKey() instanceof Yaml.Scalar) {
                return o1.getKey().getValue().compareTo(o2.getKey().getValue());
            }
            return 0;
        });
        findVisitor.visit(y, entries);
        return entries;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final Pattern pattern = Pattern.compile(getSearchRegex(propertyKey, profile, searchAllProfiles));

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                QuarkusExecutionContextView quarkusCtx = QuarkusExecutionContextView.view(ctx);
                return quarkusCtx.isQuarkusConfigFile(sourceFile, null);
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree t, ExecutionContext ctx) {
                if (t instanceof Yaml.Documents) {
                    t = new YamlIsoVisitor<ExecutionContext>() {
                        @Override
                        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                            entry = super.visitMappingEntry(entry, ctx);
                            String prop = getProperty(getCursor());
                            if (pattern.matcher(prop).find()) {
                                entry = SearchResult.found(entry);
                            }
                            return entry;
                        }
                    }.visit(t, ctx);
                } else if (t instanceof Properties.File) {
                    t = new PropertiesVisitor<ExecutionContext>() {
                        @Override
                        public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                            if (pattern.matcher(entry.getKey()).find()) {
                                entry = SearchResult.found(entry);
                            }
                            return super.visitEntry(entry, ctx);
                        }
                    }.visit(t, ctx);
                }
                return t;
            }
        };
    }

    private static String getSearchRegex(String propertyKey, @Nullable String profile, @Nullable Boolean searchAllProfiles) {
        if (Boolean.TRUE.equals(searchAllProfiles)) {
            return "^(?:%[\\w\\-_,]+\\.)?" + propertyKey + "$";
        } else if (StringUtils.isNotEmpty(profile)) {
            return "^%[\\w\\-_,]*(?:" + profile + ")[\\w\\-_,]*\\." + propertyKey + "$";
        }
        return "^" + propertyKey + "$";
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
