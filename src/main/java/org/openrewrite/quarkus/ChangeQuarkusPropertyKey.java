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
import org.openrewrite.quarkus.search.FindQuarkusProperties;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeQuarkusPropertyKey extends Recipe {

    @Option(displayName = "Old property key",
            description = "The property key to rename. Supports regex.",
            example = "quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy")
    String oldPropertyKey;

    @Option(displayName = "New property key",
            description = "The new name for the property key. Supports regex.",
            example = "quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy")
    String newPropertyKey;

    @Option(displayName = "Except",
            description = "Regex. If any of these property keys exist as direct children of `oldPropertyKey`, then they will not be moved to `newPropertyKey`.",
            required = false)
    @Nullable
    List<String> except;

    @Option(displayName = "Profile",
            description = "The profile where the property is defined. If not specified, the property will be changed on all profiles. Defaults to `true`.",
            required = false,
            example = "dev")
    @Nullable
    String profile;

    @Option(displayName = "Change for all Profiles",
            description = "If set, thr property will be changed on all available profiles.",
            required = false,
            example = "false")
    @Nullable
    Boolean changeAllProfiles;

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
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate()
                .and(Validated.notBlank("newPropertyKey", oldPropertyKey))
                .and(Validated.notBlank("newPropertyKey", newPropertyKey));

        if (StringUtils.isNotEmpty(profile)) {
            validated = validated.and(Validated
                    .test("changeAllProfiles", "cannot be used together with profile", changeAllProfiles, x -> x == null || !x)
            );
        }

        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Change Quarkus property key";
    }

    @Override
    public String getDescription() {
        return "Change a Quarkus property key.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindQuarkusProperties(oldPropertyKey, profile, changeAllProfiles).getVisitor(),
                new ChangeQuarkusPropertyKeyVisitor(oldPropertyKey, newPropertyKey, except, profile, changeAllProfiles, pathExpressions)
        );
    }
}

