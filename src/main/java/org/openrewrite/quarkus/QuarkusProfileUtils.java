package org.openrewrite.quarkus;

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
