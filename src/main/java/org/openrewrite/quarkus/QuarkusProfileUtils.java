/*
 * Copyright 2024 the original author or authors.
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

import org.openrewrite.internal.StringUtils;

import java.util.Collections;
import java.util.List;

class QuarkusProfileUtils {

    private QuarkusProfileUtils() {
    }

    static String getKeyWithoutProfile(String propertyKey) {
        if (propertyKey.isEmpty() || propertyKey.charAt(0) != '%') {
            return propertyKey;
        }
        int index = propertyKey.indexOf('.');
        if (index == -1) {
            return propertyKey;
        }
        return propertyKey.substring(index + 1);
    }

    static String[] getProfilesFromPropertyKey(String propertyKey) {
        if (propertyKey.isEmpty() || propertyKey.charAt(0) != '%') {
            return new String[0];
        }
        int index = propertyKey.indexOf('.');
        if (index == -1) {
            return new String[0];
        }
        return propertyKey.substring(1, index).split(",");
    }

    static String formatKey(String property, String value, String profile) {
        StringBuilder yaml = new StringBuilder();
        formatKey(yaml, property, value, StringUtils.isNotEmpty(profile) ? Collections.singletonList(profile) : Collections.emptyList());
        return yaml.toString();
    }

    static void formatKey(StringBuilder yaml, String property, String value, List<String> profiles) {
        String[] propertyParts = property.split("\\.");

        String indent = "";
        if (!profiles.isEmpty()) {
            yaml.append("'%")
                    .append(java.lang.String.join(",", profiles))
                    .append("':")
                    .append(System.lineSeparator());
            indent = indent + "  ";
        }
        for (int i = 0; i < propertyParts.length; i++) {
            String part = propertyParts[i];
            if (i > 0 && yaml.length() > 0) {
                yaml.append(System.lineSeparator());
            }
            yaml.append(indent).append(part).append(":");
            indent = indent + "  ";
        }
        yaml.append(" ").append(value);
        if (yaml.length() > 0) {
            yaml.append(System.lineSeparator());
        }
    }
}
