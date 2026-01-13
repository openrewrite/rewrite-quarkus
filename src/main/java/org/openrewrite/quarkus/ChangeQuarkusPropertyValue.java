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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.quarkus.search.FindQuarkusProperties;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class ChangeQuarkusPropertyValue extends Recipe {

    @Option(displayName = "Property key",
            description = "The name of the property key whose value is to be changed. Supports regex.",
            example = "quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy")
    String propertyKey;

    @Option(displayName = "New value",
            description = "The new value to be used for key specified by `propertyKey`.",
            example = "read-sync")
    String newValue;

    @Option(displayName = "Old value",
            required = false,
            description = "Only change the property value if it matches the configured `oldValue`.",
            example = "read-sync")
    @Nullable
    String oldValue;

    @Option(displayName = "Profile",
            description = "The profile where the property is defined. If not specified, the property will be changed on all profiles by default.",
            required = false,
            example = "dev")
    @Nullable
    String profile;

    @Option(displayName = "Change for all profiles",
            description = "If set to true, the property value will be changed on all available profiles. Defaults to `true` if a profile is not defined.",
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
                .and(Validated.notBlank("propertyKey", propertyKey));

        if (StringUtils.isNotEmpty(profile)) {
            validated = validated.and(Validated
                    .test("changeAllProfiles", "cannot be used together with profile", changeAllProfiles, x -> x == null || !x)
            );
        }

        return validated;
    }

    String displayName = "Change Quarkus configuration property value";

    String description = "Change the value of a property in Quarkus configuration files.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindQuarkusProperties(propertyKey, profile, changeAllProfiles, pathExpressions).getVisitor(),
                new ChangeQuarkusPropertyValueVisitor(propertyKey, newValue, oldValue, profile, changeAllProfiles, pathExpressions)
        );
    }
}
