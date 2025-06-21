package ca.bkaw.torque.util;

import ca.bkaw.torque.platform.Identifier;

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
}
