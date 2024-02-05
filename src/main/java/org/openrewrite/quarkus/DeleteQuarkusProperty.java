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
@EqualsAndHashCode(callSuper = false)
public class DeleteQuarkusProperty extends Recipe {

    @Option(displayName = "Property key",
            description = "The name of the property key whose value is to be changed.",
            example = "quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy")
    String propertyKey;

    @Option(displayName = "Old value",
            required = false,
            description = "Only delete the property value if it matches the configured `oldValue`.",
            example = "read-sync")
    @Nullable
    String oldValue;

    @Option(displayName = "Profile",
            description = "The profile where the property is defined. If not specified, the property will be changed on the default profile.",
            required = false,
            example = "dev")
    @Nullable
    String profile;

    @Option(displayName = "Delete from all Profiles",
            description = "If set to true, the property will be removed from all available profiles. Defaults to `true`.",
            required = false,
            example = "false")
    @Nullable
    Boolean deleteFromAllProfiles;

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
                .and(Validated.notBlank("propertyKey", propertyKey));

        if (StringUtils.isNotEmpty(profile)) {
            validated = validated.and(Validated
                    .test("deleteOnAllProfiles", "cannot be used together with profile", deleteFromAllProfiles, x -> x == null || !x)
            );
        }

        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Delete Quarkus Property";
    }

    @Override
    public String getDescription() {
        return "Delete a property from a Quarkus configuration file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindQuarkusProperties(propertyKey, profile, deleteFromAllProfiles).getVisitor(),
                new DeleteQuarkusPropertyVisitor(propertyKey, oldValue, profile, deleteFromAllProfiles, pathExpressions)
        );
    }
}
