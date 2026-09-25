package pro.mikey.autoclicker.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Generic setting wrapper for the Settings API.
 * Each module declares its configuration as Setting fields, which can be
 * auto-discovered by the UI and serialized/deserialized by ConfigManager.
 *
 * @param <T> The value type (Boolean, Integer, Float, String, Enum)
 */
public abstract class Setting<T> {

    private final String id; // Unique key for JSON, e.g. "anti_afk.enabled"
    private final String displayName; // Human-readable name for UI
    private final T defaultValue;
    private T value;
    private String group; // Logical group for UI sub-headers (null = ungrouped)
    private String description; // Tooltip description (null = no tooltip)
    private final List<Consumer<T>> listeners = new ArrayList<>();

    protected Setting(String id, String displayName, T defaultValue) {
        this.id = id;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    /** Fluent builder: assign this setting to a UI group. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S withGroup(String groupName) {
        this.group = groupName;
        return (S) this;
    }

    /** Returns the UI group name, or null if ungrouped. */
    public String getGroup() {
        return group;
    }

    /** Fluent builder: assign a tooltip description. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S withDescription(String desc) {
        this.description = desc;
        return (S) this;
    }

    /** Returns the tooltip description, or null if none. */
    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public T get() {
        return value;
    }

    public T getDefault() {
        return defaultValue;
    }

    public void set(T newValue) {
        T validated = validate(newValue);
        if (!validated.equals(this.value)) {
            this.value = validated;
            for (Consumer<T> listener : listeners) {
                listener.accept(validated);
            }
        }
    }

    /** Reset to default value. */
    public void reset() {
        set(defaultValue);
    }

    /** Add a change listener. */
    public Setting<T> onChange(Consumer<T> listener) {
        listeners.add(listener);
        return this;
    }

    /** Subclasses override to clamp/validate values. */
    protected T validate(T value) {
        return value;
    }

    /**
     * Returns the type name for serialization ("boolean", "int", "float", "enum",
     * "string").
     */
    public abstract String getTypeName();

    /** Deserialize from a generic Object (from JSON). */
    public abstract void setFromObject(Object obj);

    /** Serialize to a JSON-compatible Object. */
    public Object toSerializable() {
        return value;
    }

    // ═══════════════════════════════════════════════════════════════
    // Concrete subclasses
    // ═══════════════════════════════════════════════════════════════

    /** Boolean setting (toggle). */
    public static class BooleanSetting extends Setting<Boolean> {

        public BooleanSetting(String id, String displayName, boolean defaultValue) {
            super(id, displayName, defaultValue);
        }

        @Override
        public String getTypeName() {
            return "boolean";
        }

        @Override
        public void setFromObject(Object obj) {
            if (obj instanceof Boolean b) {
                set(b);
            } else if (obj instanceof String s) {
                set(Boolean.parseBoolean(s));
            }
        }
    }

    /** Integer setting with min/max bounds. */
    public static class IntSetting extends Setting<Integer> {

        private final int min;
        private final int max;

        public IntSetting(String id, String displayName, int defaultValue, int min, int max) {
            super(id, displayName, defaultValue);
            this.min = min;
            this.max = max;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        @Override
        protected Integer validate(Integer value) {
            return Math.max(min, Math.min(max, value));
        }

        @Override
        public String getTypeName() {
            return "int";
        }

        @Override
        public void setFromObject(Object obj) {
            if (obj instanceof Number n) {
                set(n.intValue());
            } else if (obj instanceof String s) {
                try {
                    set(Integer.parseInt(s));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    /** Float/Double setting with min/max bounds. */
    public static class FloatSetting extends Setting<Double> {

        private final double min;
        private final double max;

        public FloatSetting(String id, String displayName, double defaultValue, double min, double max) {
            super(id, displayName, defaultValue);
            this.min = min;
            this.max = max;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        @Override
        protected Double validate(Double value) {
            return Math.max(min, Math.min(max, value));
        }

        @Override
        public String getTypeName() {
            return "float";
        }

        @Override
        public void setFromObject(Object obj) {
            if (obj instanceof Number n) {
                set(n.doubleValue());
            } else if (obj instanceof String s) {
                try {
                    set(Double.parseDouble(s));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    /** Enum setting — stores as ordinal internally, serialized as string. */
    public static class EnumSetting<E extends Enum<E>> extends Setting<E> {

        private final Class<E> enumClass;

        public EnumSetting(String id, String displayName, E defaultValue, Class<E> enumClass) {
            super(id, displayName, defaultValue);
            this.enumClass = enumClass;
        }

        public Class<E> getEnumClass() {
            return enumClass;
        }

        public E[] getValues() {
            return enumClass.getEnumConstants();
        }

        /** Cycle to next enum value. */
        public void cycle() {
            E[] values = getValues();
            int next = (get().ordinal() + 1) % values.length;
            set(values[next]);
        }

        /** Cycle to previous enum value. */
        public void cycleBack() {
            E[] values = getValues();
            int prev = (get().ordinal() - 1 + values.length) % values.length;
            set(values[prev]);
        }

        @Override
        public String getTypeName() {
            return "enum";
        }

        @Override
        public Object toSerializable() {
            return get().name();
        }

        @Override
        public void setFromObject(Object obj) {
            if (obj instanceof String s) {
                try {
                    set(Enum.valueOf(enumClass, s));
                } catch (IllegalArgumentException ignored) {
                }
            } else if (obj instanceof Boolean b) {
                // Backward compat: old BooleanSetting configs (true → first enum, false →
                // second enum)
                E[] values = getValues();
                if (values.length >= 2) {
                    set(b ? values[0] : values[1]);
                }
            }
        }
    }

    /** String list setting (e.g. mining filter list, trash item list). */
    public static class StringListSetting extends Setting<java.util.List<String>> {

        public StringListSetting(String id, String displayName, java.util.List<String> defaultValue) {
            super(id, displayName, defaultValue != null ? new ArrayList<>(defaultValue) : new ArrayList<>());
        }

        @Override
        public String getTypeName() {
            return "string_list";
        }

        @Override
        public void setFromObject(Object obj) {
            if (obj instanceof java.util.List<?> list) {
                java.util.List<String> result = new ArrayList<>();
                for (Object item : list) {
                    result.add(String.valueOf(item));
                }
                set(result);
            }
        }
    }
}
