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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.openrewrite.internal.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class QuarkusProfileUtils {

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

    static String formatKey(String property, String value, String profile, Set<String> dottedSegments) {
        StringBuilder yaml = new StringBuilder();
        formatKey(yaml, property, value, StringUtils.isNotEmpty(profile) ? singletonList(profile) : emptyList(), dottedSegments);
        return yaml.toString();
    }

    static void formatKey(StringBuilder yaml, String property, String value, List<String> profiles) {
        formatKey(yaml, property, value, profiles, emptySet());
        if (yaml.length() > 0) {
            yaml.append(System.lineSeparator());
        }
    }

    /**
     * @param dottedSegments keys containing dots that must stay a single segment, like a quoted persistence unit name
     */
    private static void formatKey(StringBuilder yaml, String property, String value, List<String> profiles, Set<String> dottedSegments) {
        List<String> propertyParts = splitKey(property, dottedSegments);

        String indent = "";
        if (!profiles.isEmpty()) {
            yaml.append("'%")
                    .append(java.lang.String.join(",", profiles))
                    .append("':")
                    .append(System.lineSeparator());
            indent = indent + "  ";
        }
        for (int i = 0; i < propertyParts.size(); i++) {
            String part = propertyParts.get(i);
            if (i > 0 && yaml.length() > 0) {
                yaml.append(System.lineSeparator());
            }
            yaml.append(indent).append(part).append(":");
            indent = indent + "  ";
        }
        yaml.append(" ").append(value);
    }

    private static List<String> splitKey(String property, Set<String> dottedSegments) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        nextPart:
        while (start < property.length()) {
            for (String dotted : dottedSegments) {
                int end = start + dotted.length();
                if (property.startsWith(dotted, start) && (end == property.length() || property.charAt(end) == '.')) {
                    parts.add('"' + dotted + '"');
                    start = end + 1;
                    continue nextPart;
                }
            }
            int dot = property.indexOf('.', start);
            int end = dot == -1 ? property.length() : dot;
            parts.add(property.substring(start, end));
            start = end + 1;
        }
        return parts;
    }
}
