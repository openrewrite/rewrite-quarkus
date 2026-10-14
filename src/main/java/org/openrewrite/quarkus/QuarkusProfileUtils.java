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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;

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

    static String formatKey(String property, String value, List<String> profiles, Set<String> dottedSegments) {
        StringBuilder yaml = new StringBuilder();
        appendKey(yaml, property, value, profiles, dottedSegments);
        return yaml.toString();
    }

    static void formatKey(StringBuilder yaml, String property, String value, List<String> profiles) {
        appendKey(yaml, property, value, profiles, emptySet());
        yaml.append(System.lineSeparator());
    }

    /**
     * @param dottedSegments keys containing dots that must stay a single segment, like a quoted persistence unit name
     */
    private static void appendKey(StringBuilder yaml, String property, String value, List<String> profiles, Set<String> dottedSegments) {
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
        while (start < property.length()) {
            String dotted = dottedSegmentAt(property, start, dottedSegments);
            int dot = property.indexOf('.', start);
            int end = dotted != null ? start + dotted.length() : dot == -1 ? property.length() : dot;
            parts.add(dotted != null ? '"' + dotted + '"' : property.substring(start, end));
            start = end + 1;
        }
        return parts;
    }

    private static @Nullable String dottedSegmentAt(String property, int start, Set<String> dottedSegments) {
        String longest = null;
        for (String dotted : dottedSegments) {
            int end = start + dotted.length();
            if (property.startsWith(dotted, start) && (end == property.length() || property.charAt(end) == '.') &&
                (longest == null || dotted.length() > longest.length())) {
                longest = dotted;
            }
        }
        return longest;
    }
}
