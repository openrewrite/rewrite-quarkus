/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class AddQuarkusMavenPluginGoalVisitor extends MavenVisitor {
    private static final Predicate<? super Xml.Tag> TAG_KEY_NAME_MATCHES = tag -> tag.getName().equals("goal");
    private static final Predicate<? super Xml.Tag> TAG_HAS_CONTENT = tag -> tag.getContent() != null && tag.getContent().size() == 1;
    private static final Predicate<? super Xml.Tag> TAG_CONTENT_IS_CHAR_DATA = tag -> tag.getContent().get(0) instanceof Xml.CharData;
    private static final BiPredicate<? super Xml.Tag, String> TAG_CONTENT_MATCHES = (tag, str) -> ((Xml.CharData) tag.getContent().get(0)).getText().equals(str);

    private final String goalName;

    public AddQuarkusMavenPluginGoalVisitor(String goalName) {
        this.goalName = goalName;
    }

    @Override
    public Maven visitMaven(Maven maven, ExecutionContext ctx) {
        /**
         * There must be room for making this better. It feels like this shouldn't need so much ceremony around building up a path. Or what would
         * really be helpful is having this walk a configured (fully-defined) path, and try to add any new nodes if none exist.
         * fixme
         */
        FindPlugin.find(maven, "io.quarkus", "quarkus-maven-plugin").forEach(plugin -> {
            Optional<Xml.Tag> maybeExecutions = plugin.getChild("executions");
            if (!maybeExecutions.isPresent()) {
                Xml.Tag executionsTag = Xml.Tag.build("<executions/>");
                doAfterVisit(new AddToTagVisitor<>(plugin, executionsTag));
                doAfterVisit(new AddQuarkusMavenPluginGoalVisitor(goalName));
            } else {
                Xml.Tag executions = maybeExecutions.get();
                Optional<Xml.Tag> maybeExecution = executions.getChildren().stream().filter(exe -> exe.getName().equals("execution")).findFirst();
                if (!maybeExecution.isPresent()) {
                    Xml.Tag executionTag = Xml.Tag.build("<execution/>");
                    doAfterVisit(new AddToTagVisitor<>(executions, executionTag));
                    doAfterVisit(new AddQuarkusMavenPluginGoalVisitor(goalName));
                } else {
                    Xml.Tag execution = maybeExecution.get();
                    Optional<Xml.Tag> maybeGoals = execution.getChild("goals");
                    if (!maybeGoals.isPresent()) {
                        Xml.Tag goalsTag = Xml.Tag.build("<goals/>");
                        doAfterVisit(new AddToTagVisitor<>(execution, goalsTag));
                        doAfterVisit(new AddQuarkusMavenPluginGoalVisitor(goalName));
                    } else {
                        Xml.Tag goals = maybeGoals.get();
                        Optional<Xml.Tag> foundGoals = goals.getChildren().stream()
                                .filter(TAG_KEY_NAME_MATCHES)
                                .filter(TAG_HAS_CONTENT)
                                .filter(TAG_CONTENT_IS_CHAR_DATA)
                                .filter(t -> TAG_CONTENT_MATCHES.test(t, goalName))
                                .findAny();
                        if (!foundGoals.isPresent()) {
                            Xml.Tag goalsTag = Xml.Tag.build("<goal>" + goalName + "</goal>");
                            doAfterVisit(new AddToTagVisitor<>(goals, goalsTag));
                        }
                    }
                }
            }
        });

        return super.visitMaven(maven, ctx);
    }
}
