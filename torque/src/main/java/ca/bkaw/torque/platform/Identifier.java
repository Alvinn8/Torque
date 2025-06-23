package ca.bkaw.torque.platform;

import org.jetbrains.annotations.NotNull;

/**
 * An identifier for a resource concisting of a namespace and a key.
 * <p>
 * Also known as a namespaced key, namespaced identifier, resource location.
 *
 * @param namespace The namespace. Allows characters: [a-z0-9._.]
 * @param key The key of the resource. Allows characters: [a-z0-9._./]
 */
public record Identifier(String namespace, String key) {
    public Identifier {
        if (!validNamespace(namespace)) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }
        if (!validKey(key)) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
    }

    /**
     * Check the string is a valid namespace.
     *
     * @param namespace The namespace to check.
     * @return True if the namespace is valid, false otherwise.
     */
    public static boolean validNamespace(String namespace) {
        return namespace.matches("[a-z0-9._]+");
    }

    /**
     * Check the string is a valid key.
     *
     * @param key The key to check.
     * @return True if the key is valid, false otherwise.
     */
    public static boolean validKey(String key) {
        return key.matches("[a-z0-9._/]+");
    }

    /**
     * Check if the string is a valid identifier.
     *
     * @param identifier The string.
     * @return True if the string is a valid identifier, false otherwise.
     */
    public static boolean validIdentifier(String identifier) {
        String[] parts = identifier.split(":");
        return parts.length == 2 && validNamespace(parts[0]) && validKey(parts[1]);
    }

    /**
     * Create an identifier from a string.
     *
     * @param identifier The identifier string in the format "namespace:key".
     * @return The Identifier.
     */
    public static Identifier fromString(String identifier) {
        String[] parts = identifier.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid identifier: " + identifier);
        }
        return new Identifier(parts[0], parts[1]);
    }

    @Override
    @NotNull
    public String toString() {
        return this.namespace + ":" + this.key;
    }
}
