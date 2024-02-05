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
package org.openrewrite.quarkus;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.AddProperty;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.yaml.MergeYaml;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A recipe to uniformly add a property to Quarkus configuration file. This recipe supports adding properties to
 * "application.properties" and "application.yaml" files. This recipe will only add the property if it does not already
 * exist within the configuration file.
 * <p>
 * NOTE: Because an application may have a large collection of yaml files (some of which may not even be related to
 * Quarkus configuration), this recipe will only make changes to files that match one of the pathExpressions. If
 * the recipe is configured without pathExpressions, it will query the execution context for reasonable defaults.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddQuarkusProperty extends Recipe {

    @Option(displayName = "Property key",
            description = "The property key to add.",
            example = "quarkus.http.port")
    String property;

    @Option(displayName = "Property value",
            description = "The value of the new property key.",
            example = "9090")
    String value;

    @Option(displayName = "Optional comment to be prepended to the property",
            description = "A comment that will be added to the new property.",
            required = false,
            example = "This is a comment")
    @Nullable
    String comment;

    @Option(displayName = "Profile",
            description = "The profile to add the property to. If not specified, the property will be added to the default profile.",
            required = false,
            example = "dev")
    @Nullable
    String profile;

    @Option(displayName = "Optional list of file path matcher",
            description = "Each value in this list represents a glob expression that is used to match which files will " +
                          "be modified. If this value is not present, this recipe will query the execution context for " +
                          "reasonable defaults. (\"**/application.yml\", \"**/application.yaml\", " +
                          "\"**/application.properties\" and \"**/META-INF/microprofile-config.properties\".",
            required = false,
            example = "[\"**/application.yaml\"]")
    @Nullable
    List<String> pathExpressions;

    @Override
    public String getDisplayName() {
        return "Add a Quarkus configuration property";
    }

    @Override
    public String getDescription() {
        return "Add a Quarkus configuration property to an existing configuration file if it does not already exist in that file.";
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
            public @Nullable Tree visit(@Nullable Tree t, ExecutionContext ctx) {
                if (t instanceof Yaml.Documents) {
                    t = createMergeYamlVisitor().getVisitor().visit(t, ctx);
                } else if (t instanceof Properties.File) {
                    t = new AddProperty(propertyName(property, profile), value, comment, null)
                            .getVisitor().visit(t, ctx);
                }
                return t;
            }
        };
    }

    private MergeYaml createMergeYamlVisitor() {
        String[] propertyParts = yamlPropertyName(property, profile).split("\\.");

        StringBuilder yaml = new StringBuilder();

        String indent = "";
        for (String part : propertyParts) {
            if (yaml.length() > 0) {
                yaml.append("\n");
            }
            if (!StringUtils.isBlank(comment)) {
                //noinspection StringEquality
                if (part == propertyParts[propertyParts.length - 1]) {
                    yaml.append(indent).append("# ").append(comment).append("\n");
                }
            }
            yaml.append(indent).append(part).append(":");
            indent = indent + "  ";
        }
        if (quoteValue(value)) {
            yaml.append(" \"").append(value).append('"');
        } else {
            yaml.append(" ").append(value);
        }
        return new MergeYaml("$", yaml.toString(), true, null);
    }

    private static final Pattern scalarNeedsAQuote = Pattern.compile("[^a-zA-Z\\d\\s]+");

    private boolean quoteValue(String value) {
        return scalarNeedsAQuote.matcher(value).matches();
    }

    private static String propertyName(String name, @Nullable String profile) {
        return profile == null ? name : "%" + profile + "." + name;
    }

    private static String yamlPropertyName(String name, @Nullable String profile) {
        return profile == null ? name : "\"%" + profile + "\"." + name;
    }
}
