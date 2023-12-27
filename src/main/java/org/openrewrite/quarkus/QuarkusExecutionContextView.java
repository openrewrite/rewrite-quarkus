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

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QuarkusExecutionContextView extends DelegatingExecutionContext {

    private static final String DEFAULT_APPLICATION_CONFIGURATION_PATHS = "org.openrewrite.java.quarkus.defaultApplicationConfigurationPaths";

    public QuarkusExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static QuarkusExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof QuarkusExecutionContextView) {
            return (QuarkusExecutionContextView) ctx;
        }
        return new QuarkusExecutionContextView(ctx);
    }

    /**
     * The path expressions used to find a Quarkus application's default configuration file(s). The default masks used to
     * find the application's root configuration are "**&#47;application.properties", "**&#47;application.yml",
     * "**&#47;application.yaml" and "**&#47;META-INF&#47;microprofile-config.properties"
     *
     * @param pathExpressions A list of expressions that will be used as masks to find an application's default configuration file(s)
     * @return this
     */
    public QuarkusExecutionContextView setDefaultApplicationConfigurationPaths(List<String> pathExpressions) {
        putMessage(DEFAULT_APPLICATION_CONFIGURATION_PATHS, pathExpressions);
        return this;
    }

    /**
     * The path expressions used to find a Quarkus application's default configuration file. The default masks used to
     * find the application's root configuration are "**&#47;application.properties", "**&#47;application.yml",
     * "**&#47;application.yaml" and "**&#47;META-INF&#47;microprofile-config.properties"
     *
     * @return A list of file paths expression that will be used to find a Quarkus application's default configuration file(s)
     */
    public List<String> getDefaultApplicationConfigurationPaths() {
        return getMessage(DEFAULT_APPLICATION_CONFIGURATION_PATHS,
                Arrays.asList("**/application.{properties,yaml,yml}", "**/META-INF/microprofile-config.properties")
        );
    }

    public boolean isQuarkusConfigFile(Tree tree, @Nullable List<String> pathExpressions) {
        if (!(tree instanceof Properties.File || tree instanceof Yaml.Documents)) {
            return false;
        }
        List<String> expressions = pathExpressions != null ? pathExpressions : Collections.emptyList();
        if (expressions.isEmpty()) {
            // If not defined, get reasonable defaults from the execution context.
            expressions = getDefaultApplicationConfigurationPaths();
        }
        if (expressions.isEmpty()) {
            return true;
        }
        for (String filePattern : expressions) {
            if (FileSystems.getDefault().getPathMatcher("glob:" + filePattern).matches(((SourceFile)tree).getSourcePath())) {
                return true;
            }
        }

        return false;
    }
}
