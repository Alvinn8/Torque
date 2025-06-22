package ca.bkaw.torque.util;

import ca.bkaw.torque.platform.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Registry<T> {
    private final Map<Identifier, T> map = new HashMap<>();
    private final Function<T, Identifier> identifierGetter;

    public Registry(Function<T, Identifier> identifierGetter) {
        this.identifierGetter = identifierGetter;
    }

    public void register(T value) {
        Identifier identifier = this.identifierGetter.apply(value);
        if (this.map.containsKey(identifier)) {
            throw new IllegalArgumentException("Value already registered: " + identifier);
        }
        this.map.put(identifier, value);
    }

    @SafeVarargs
    public final void register(T... values) {
        for (T value : values) {
            this.register(value);
        }
    }

    /**
     * Empty the registry, removing all registered values.
     */
    public void clear() {
        this.map.clear();
    }

    @Contract("null -> null")
    @Nullable
    public T get(@Nullable Identifier identifier) {
        if (identifier == null) {
            return null;
        }
        return this.map.get(identifier);
    }
}
