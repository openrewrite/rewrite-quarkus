package org.openrewrite.quarkus;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.quarkus.search.FindQuarkusProfiles;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
class ChangeQuarkusPropertyKeyVisitor extends TreeVisitor<Tree, ExecutionContext> {

    final String oldPropertyKey;

    final String newPropertyKey;

    @Nullable
    final List<String> except;

    @Nullable
    final String profile;

    @Nullable
    final Boolean changeAllProfiles;

    @Nullable
    final List<String> pathExpressions;

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
        if (tree instanceof Properties.File) {
            return visitPropertiesFile(tree, ctx);
        } else if (tree instanceof Yaml.Documents) {
            return visitYamlDocuments(tree, ctx);
        }
        return tree;
    }

    private @Nullable Tree visitPropertiesFile(Tree tree, ExecutionContext ctx) {
        boolean changeAllProfiles = Boolean.TRUE.equals(this.changeAllProfiles);
        if ((this.profile == null && this.changeAllProfiles == null) || changeAllProfiles) {
            String oldPropertyKey = changeAllProfiles ? "^(.*?)" + this.oldPropertyKey : this.oldPropertyKey;
            String newPropertyKey = changeAllProfiles ? "$1" + fixRegexReferences(this.newPropertyKey) : this.newPropertyKey;
            tree = new org.openrewrite.properties.ChangePropertyKey(oldPropertyKey, newPropertyKey, false, true)
                    .getVisitor()
                    .visit(tree, ctx);
        } else if (profile != null) {
            // Make changes to a property on a single profile
            Set<String> profiles = FindQuarkusProfiles.find(tree);
            if (profiles.size() == 1 && profiles.contains(profile)) {
                tree = new org.openrewrite.properties.ChangePropertyKey(oldPropertyKey, newPropertyKey, false, true)
                        .getVisitor()
                        .visit(tree, ctx);
            } else {
                Set<Properties.Entry> entries = new TreeSet<>(Comparator.comparing(Properties.Entry::getKey));
                Pattern pattern = Pattern.compile(oldPropertyKey);
                new PropertiesVisitor<Set<Properties.Entry>>() {
                    @Override
                    public Properties visitEntry(Properties.Entry entry, Set<Properties.Entry> ps) {
                        if (pattern.matcher(entry.getKey()).find()) {
                            ps.add(entry);
                        }
                        return super.visitEntry(entry, ps);
                    }
                }.visit(tree, entries);

                for (Properties.Entry entry : entries) {
                    String oldKey = entry.getKey();
                    if (oldKey.charAt(0) != '%' || !oldKey.contains(profile)) {
                        continue;
                    }

                    List<String> remainingProfiles = new ArrayList<>(entries.size());
                    for (String profile : oldKey.substring(1, oldKey.indexOf('.')).split(",")) {
                        if (!profile.equals(this.profile)) {
                            remainingProfiles.add(profile);
                        }
                    }

                    String propertySuffix = getPropertySuffix(oldKey);

                    // Remove the old property
                    tree = new org.openrewrite.properties.DeleteProperty(oldKey, false)
                            .getVisitor()
                            .visit(tree, ctx);

                    // Add the new property for the named profile
                    String newKey = "%" + profile + "." + transformString(oldPropertyKey, newPropertyKey, propertySuffix);
                    newKey = fixRegexReferences(newKey);
                    tree = new org.openrewrite.properties.AddProperty(
                            newKey,
                            entry.getValue().getText(),
                            null,
                            entry.getDelimiter().getCharacter().toString()
                    ).getVisitor().visit(tree, ctx);

                    if (!remainingProfiles.isEmpty()) {
                        // Add the new property for the named profile
                        tree = new org.openrewrite.properties.AddProperty(
                                "%" + String.join(",", remainingProfiles) + "." + propertySuffix,
                                entry.getValue().getText(),
                                null,
                                entry.getDelimiter().getCharacter().toString()
                        ).getVisitor().visit(tree, ctx);
                    }
                }
            }
        }
        return tree;
    }

    private @Nullable Tree visitYamlDocuments(Tree tree, ExecutionContext ctx) {
        String oldPropertyKey = this.oldPropertyKey;
        String newPropertyKey = this.newPropertyKey;
        boolean changeAllProfiles = Boolean.TRUE.equals(this.changeAllProfiles);
        if ((this.profile == null && this.changeAllProfiles == null) || changeAllProfiles) {
            if (changeAllProfiles) {
                oldPropertyKey = "^(.*)" + oldPropertyKey;
                newPropertyKey = fixRegexReferences(this.newPropertyKey);
            }
            tree = new org.openrewrite.yaml.ChangePropertyKey(oldPropertyKey, newPropertyKey, true, except)
                    .getVisitor()
                    .visit(tree, ctx);
        } else if (profile != null) {
            // Make changes to a property on a single profile
            // test if key exists in a single profile;
            // if not remove the profile from the key and add a new entry containing the other profiles with the current key value
            throw new UnsupportedOperationException("TODO");
        }
        return tree;
    }

    private static String getPropertySuffix(String propertyKey) {
        if (propertyKey.isEmpty() || propertyKey.charAt(0) != '%') {
            return propertyKey;
        }
        int index = propertyKey.indexOf('.');
        if (index == -1) {
            return propertyKey;
        }
        return propertyKey.substring(index + 1);
    }

    private static String fixRegexReferences(String regex) {
        String temp = regex;
        Pattern pattern = Pattern.compile(".*?[^\\\\]\\$(\\d+).*");
        Matcher matcher = pattern.matcher(temp);
        int start = 0, end = regex.length() - 1;

        while (matcher.region(start, end).matches()) {
            int value = Integer.parseInt(matcher.group(1));
            if (value < 1) {
                throw new IllegalArgumentException("Invalid regex: " + regex);
            }
            temp = new StringBuilder(temp).replace(
                    matcher.start(1), matcher.end(1), String.valueOf(++value)
            ).toString();
            start = matcher.end(1);
            matcher.reset();
        }
        return temp;
    }

    private static String transformString(String oldRegex, String newRegex, String input) {
        Matcher matcher = Pattern.compile(oldRegex).matcher(input);
        if (matcher.find()) {
            StringBuffer result = new StringBuffer();
            do {
                matcher.appendReplacement(result, newRegex);
            } while (matcher.find());
            matcher.appendTail(result);
            return result.toString();
        }
        return input;
    }
}
