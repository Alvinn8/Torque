package ca.bkaw.torque.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parser for tag strings that can handle both simple tags and tags with values.
 * <p>
 * Examples:
 * - Simple tags: "#seat #driver" -> {seat: null, driver: null}
 * - Tags with values: "#mass=50kg #density=3x" -> {mass: "50kg", density: "3x"}
 * - Mixed: "#wheel #driven #mass=100kg" -> {wheel: null, driven: null, mass: "100kg"}
 */
public class TagString {

    // Pattern to match tags: #tagname or #tagname=value
    // Tag names can contain letters, numbers, and underscores
    // Values can contain letters, numbers, underscores, and various symbols but not spaces
    private static final Pattern TAG_PATTERN = Pattern.compile("#(?<name>[a-zA-Z0-9_]+)(?:=(?<value>[^\\s#]+))?");

    private final Map<String, String> tags;

    /**
     * Create an empty TagString.
     *
     * @return An empty TagString instance.
     */
    public static @NotNull TagString empty() {
        return new TagString(Map.of());
    }

    /**
     * Parse a string containing tags and return the result.
     * 
     * @param text The text to parse (e.g., "element name #seat #driver #mass=50kg")
     * @return A ParseResult containing simple tags and tags with values
     */
    @NotNull
    public static TagString parse(@Nullable String text) {
        if (text == null) {
            return empty();
        }

        Map<String, String> tags = new HashMap<>();

        Matcher matcher = TAG_PATTERN.matcher(text);
        while (matcher.find()) {
            String tagName = matcher.group("name");
            String tagValue = matcher.group("value");
            tags.put(tagName, tagValue); // tagValue can be null
        }

        return new TagString(tags);
    }

    public TagString(@NotNull Map<String, String> tags) {
        this.tags = tags;
    }

    /**
     * Check if the string has the tag.
     *
     * @param tagName The tag name to check.
     * @return True if the tag exists.
     */
    public boolean hasTag(@NotNull String tagName) {
        return this.tags.containsKey(tagName);
    }

    /**
     * Get the value of a tag with a value.
     *
     * @param tagName The tag name
     * @return The tag value, or null if the tag doesn't exist or has no value
     */
    @Nullable
    public String getTagValue(@NotNull String tagName) {
        return this.tags.get(tagName);
    }
}
