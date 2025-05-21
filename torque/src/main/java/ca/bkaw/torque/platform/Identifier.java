package ca.bkaw.torque.platform;

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
        if (!namespace.matches("[a-z0-9._]+")) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }
        if (!key.matches("[a-z0-9._/]+")) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
    }
}
