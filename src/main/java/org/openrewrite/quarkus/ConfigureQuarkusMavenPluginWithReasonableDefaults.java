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
package org.openrewrite.quarkus;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@EqualsAndHashCode(callSuper = false)
@Value
public class ConfigureQuarkusMavenPluginWithReasonableDefaults extends Recipe {
    @Override
    public String getDisplayName() {
        return "Configure `quarkus-maven-plugin` with reasonable defaults";
    }

    @Override
    public String getDescription() {
        return "Configures the `quarkus-maven-plugin` with reasonable defaults, such as default activated `goals` and `<extensions>` configuration.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ConfigureQuarkusMavenPluginWithReasonableDefaultsVisitor();
    }

    private static class ConfigureQuarkusMavenPluginWithReasonableDefaultsVisitor extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
            doAfterVisit(new AddQuarkusMavenPluginGoalVisitor("build"));
            doAfterVisit(new AddQuarkusMavenPluginGoalVisitor("generate-code"));
            doAfterVisit(new AddQuarkusMavenPluginGoalVisitor("generate-code-tests"));

            FindPlugin.find(document, "io.quarkus", "quarkus-maven-plugin").forEach(plugin -> {
                Optional<Xml.Tag> maybeExtensions = plugin.getChild("extensions");
                if (!maybeExtensions.isPresent()) {
                    Xml.Tag extensionsTag = Xml.Tag.build("<extensions>true</extensions>");
                    doAfterVisit(new AddToTagVisitor<>(plugin, extensionsTag));
                } else {
                    // note, might want to instead interpret `<extensions>false</extensions>` as a specific decision fixme
                    doAfterVisit(new ChangeTagValueVisitor<>(maybeExtensions.get(), "true"));
                }
            });
            return document;
        }
    }
}
