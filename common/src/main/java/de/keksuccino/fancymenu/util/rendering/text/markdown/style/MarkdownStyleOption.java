package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * One value of a {@link MarkdownStyle}, like a color, a size or a mode.<br>
 * Every option has a key that names it, a group it gets shown in, and the value each {@link MarkdownLook} gives it.
 * The typed subclasses read and write the value without boxing it, and {@link #getValue()} and {@link #setValue(Object)}
 * handle the options of all types alike.
 */
public abstract class MarkdownStyleOption<T> {

    private final MarkdownStyle style;
    private final String key;
    private final Group group;
    private final Impact impact;

    protected MarkdownStyleOption(@NotNull MarkdownStyle style, @NotNull String key, @NotNull Group group, @NotNull Impact impact) {
        this.style = style;
        this.key = key;
        this.group = group;
        this.impact = impact;
        style.register(this);
    }

    /**
     * Returns the name of the option, which is unique in its style and never changes, so it can identify the option in saved data.
     */
    @NotNull
    public String getKey() {
        return this.key;
    }

    @NotNull
    public Group getGroup() {
        return this.group;
    }

    /**
     * Returns what a renderer has to do again after the value of the option changed.
     */
    @NotNull
    public Impact getImpact() {
        return this.impact;
    }

    @NotNull
    public abstract Type getType();

    public abstract T getValue();

    public abstract void setValue(T value);

    /**
     * Returns the value the given look gives the option. For a color that follows the UI theme, this is the color the theme has right now.
     */
    public abstract T getLookValue(@NotNull MarkdownLook look);

    /**
     * Gives the option the value of the given look.
     */
    public void applyLook(@NotNull MarkdownLook look) {
        this.setValue(this.getLookValue(look));
    }

    protected void changed() {
        this.style.onOptionChanged(this);
    }

    public enum Type {

        BOOLEAN,
        FLOAT,
        COLOR,
        ENUM

    }

    /**
     * The part of the text an option belongs to.
     */
    public enum Group {

        TEXT,
        HEADLINES,
        LISTS,
        LINKS,
        CODE,
        QUOTES,
        CALLOUTS,
        TABLES,
        IMAGES,
        SEPARATORS

    }

    /**
     * What a changed option makes outdated, from the cheapest to the most expensive thing to do again.
     */
    public enum Impact {

        /** Nothing. The value gets read while drawing. */
        DRAW,
        /** The cached render components of fragments, which hold the colors and formats of the text itself. */
        COMPONENTS,
        /** The lines, because the value decides where lines wrap or how big and where lines and fragments are. */
        LAYOUT,
        /** The fragments, because the value changes how wide their text gets measured. */
        PARSE

    }

    public static final class BooleanOption extends MarkdownStyleOption<Boolean> {

        private final boolean classicValue;
        private final boolean pixelValue;
        private final boolean neutralValue;
        private boolean value;

        BooleanOption(@NotNull MarkdownStyle style, @NotNull String key, @NotNull Group group, @NotNull Impact impact, boolean classicValue, boolean pixelValue, boolean neutralValue) {
            super(style, key, group, impact);
            this.classicValue = classicValue;
            this.pixelValue = pixelValue;
            this.neutralValue = neutralValue;
            this.value = classicValue;
        }

        public boolean get() {
            return this.value;
        }

        public void set(boolean value) {
            if (this.value == value) return;
            this.value = value;
            this.changed();
        }

        public boolean getLook(@NotNull MarkdownLook look) {
            return (look == MarkdownLook.PIXEL) ? this.pixelValue : (look == MarkdownLook.NEUTRAL) ? this.neutralValue : this.classicValue;
        }

        @Override
        public @NotNull Type getType() {
            return Type.BOOLEAN;
        }

        @Override
        public Boolean getValue() {
            return this.value;
        }

        @Override
        public void setValue(Boolean value) {
            this.set(value);
        }

        @Override
        public Boolean getLookValue(@NotNull MarkdownLook look) {
            return this.getLook(look);
        }

    }

    public static final class FloatOption extends MarkdownStyleOption<Float> {

        private final float classicValue;
        private final float pixelValue;
        private final float neutralValue;
        private final float minimum;
        private float value;

        FloatOption(@NotNull MarkdownStyle style, @NotNull String key, @NotNull Group group, @NotNull Impact impact, float minimum, float classicValue, float pixelValue, float neutralValue) {
            super(style, key, group, impact);
            this.minimum = minimum;
            this.classicValue = classicValue;
            this.pixelValue = pixelValue;
            this.neutralValue = neutralValue;
            this.value = classicValue;
        }

        public float get() {
            return this.value;
        }

        /**
         * Sets the value. A value below the {@linkplain #getMinimum() minimum} of the option becomes the minimum.
         */
        public void set(float value) {
            value = Math.max(this.minimum, value);
            if (this.value == value) return;
            this.value = value;
            this.changed();
        }

        /**
         * Returns the smallest value the option takes, or negative infinity if it takes every value.
         */
        public float getMinimum() {
            return this.minimum;
        }

        public float getLook(@NotNull MarkdownLook look) {
            return (look == MarkdownLook.PIXEL) ? this.pixelValue : (look == MarkdownLook.NEUTRAL) ? this.neutralValue : this.classicValue;
        }

        @Override
        public @NotNull Type getType() {
            return Type.FLOAT;
        }

        @Override
        public Float getValue() {
            return this.value;
        }

        @Override
        public void setValue(Float value) {
            this.set(value);
        }

        @Override
        public Float getLookValue(@NotNull MarkdownLook look) {
            return this.getLook(look);
        }

    }

    public static final class EnumOption<E extends Enum<E>> extends MarkdownStyleOption<E> {

        private final E classicValue;
        private final E pixelValue;
        private final E neutralValue;
        @NotNull
        private E value;

        EnumOption(@NotNull MarkdownStyle style, @NotNull String key, @NotNull Group group, @NotNull Impact impact, @NotNull E classicValue, @NotNull E pixelValue, @NotNull E neutralValue) {
            super(style, key, group, impact);
            this.classicValue = classicValue;
            this.pixelValue = pixelValue;
            this.neutralValue = neutralValue;
            this.value = classicValue;
        }

        @NotNull
        public E get() {
            return this.value;
        }

        public void set(@NotNull E value) {
            if (this.value == Objects.requireNonNull(value)) return;
            this.value = value;
            this.changed();
        }

        /**
         * Returns all values the option can have.
         */
        @NotNull
        public E[] getValues() {
            return this.classicValue.getDeclaringClass().getEnumConstants();
        }

        @Override
        public @NotNull Type getType() {
            return Type.ENUM;
        }

        @Override
        public E getValue() {
            return this.value;
        }

        @Override
        public void setValue(E value) {
            this.set(value);
        }

        @Override
        public E getLookValue(@NotNull MarkdownLook look) {
            return (look == MarkdownLook.PIXEL) ? this.pixelValue : (look == MarkdownLook.NEUTRAL) ? this.neutralValue : this.classicValue;
        }

    }

    /**
     * A color. An optional color can be unset, which means that whatever it overrides keeps its own color.<br>
     * A color either is fixed or follows a source, like a color of the UI theme. The option keeps the last color of its source,
     * and {@link MarkdownStyle#updateFollowedColors()} reads all sources again, so reading the color while drawing costs nothing.
     */
    public static final class ColorOption extends MarkdownStyleOption<DrawableColor> {

        private final LookColor classicValue;
        private final LookColor pixelValue;
        private final LookColor neutralValue;
        private final boolean optional;
        /** The source the color follows. NULL if the color is fixed or unset. */
        @Nullable
        private IntSupplier source;
        private boolean set;
        private int argb;
        /** The color as an object, built when it gets asked for. */
        @Nullable
        private DrawableColor color;

        ColorOption(@NotNull MarkdownStyle style, @NotNull String key, @NotNull Group group, @NotNull Impact impact, boolean optional, @NotNull LookColor classicValue, @NotNull LookColor pixelValue, @NotNull LookColor neutralValue) {
            super(style, key, group, impact);
            this.optional = optional;
            this.classicValue = classicValue;
            this.pixelValue = pixelValue;
            this.neutralValue = neutralValue;
            if (!optional && (!classicValue.set || !pixelValue.set || !neutralValue.set)) throw new IllegalArgumentException("Only optional colors can be unset: " + key);
            this.source = classicValue.source;
            this.set = classicValue.set;
            this.argb = (classicValue.source != null) ? classicValue.source.getAsInt() : classicValue.argb;
        }

        /**
         * Returns if the option has a color. Only {@linkplain #isOptional() optional} colors can have none.
         */
        public boolean isSet() {
            return this.set;
        }

        /**
         * Returns if the option can be unset.
         */
        public boolean isOptional() {
            return this.optional;
        }

        /**
         * Returns if the color follows a source that can change, like the UI theme.
         */
        public boolean isFollowing() {
            return this.source != null;
        }

        /**
         * Returns the color as ARGB value, or 0 if the option is unset.
         */
        public int argb() {
            return this.argb;
        }

        /**
         * Returns the color, or NULL if the option is unset.
         */
        @Nullable
        public DrawableColor get() {
            if (!this.set) return null;
            if (this.color == null) this.color = DrawableColor.of((this.argb >> 16) & 0xFF, (this.argb >> 8) & 0xFF, this.argb & 0xFF, this.argb >>> 24);
            return this.color;
        }

        /**
         * Gives the option a fixed color. NULL unsets an optional color.
         */
        public void set(@Nullable DrawableColor color) {
            if ((color == null) && !this.optional) throw new NullPointerException("The color is not optional: " + this.getKey());
            this.source = null;
            this.update(color != null, (color != null) ? color.getColorInt() : 0);
            // The given object stays the one the option returns
            this.color = color;
        }

        /**
         * Makes the color follow the given source of ARGB values from now on.
         */
        public void follow(@NotNull IntSupplier source) {
            this.source = Objects.requireNonNull(source);
            this.update(true, source.getAsInt());
        }

        /**
         * Reads the source of the color again.
         *
         * @return If that changed the color.
         */
        boolean updateFollowedColor() {
            if (this.source == null) return false;
            int argb = this.source.getAsInt();
            if (this.argb == argb) return false;
            this.argb = argb;
            this.color = null;
            return true;
        }

        private void update(boolean set, int argb) {
            boolean changed = (this.set != set) || (this.argb != argb);
            this.set = set;
            this.argb = argb;
            this.color = null;
            if (changed) this.changed();
        }

        private void apply(@NotNull LookColor look) {
            if (look.source != null) {
                this.follow(look.source);
            } else {
                this.set(look.set ? look.resolve() : null);
            }
        }

        @NotNull
        private LookColor lookColor(@NotNull MarkdownLook look) {
            return (look == MarkdownLook.PIXEL) ? this.pixelValue : (look == MarkdownLook.NEUTRAL) ? this.neutralValue : this.classicValue;
        }

        @Override
        public void applyLook(@NotNull MarkdownLook look) {
            this.apply(this.lookColor(look));
        }

        @Override
        public @NotNull Type getType() {
            return Type.COLOR;
        }

        @Override
        @Nullable
        public DrawableColor getValue() {
            return this.get();
        }

        @Override
        public void setValue(@Nullable DrawableColor value) {
            this.set(value);
        }

        @Override
        @Nullable
        public DrawableColor getLookValue(@NotNull MarkdownLook look) {
            LookColor lookColor = this.lookColor(look);
            return lookColor.set ? lookColor.resolve() : null;
        }

    }

    /**
     * The color a look gives a {@link ColorOption}: no color, a fixed color, or a source the color follows.
     */
    static final class LookColor {

        static final LookColor NONE = new LookColor(false, 0, null);

        private final boolean set;
        private final int argb;
        @Nullable
        private final IntSupplier source;

        private LookColor(boolean set, int argb, @Nullable IntSupplier source) {
            this.set = set;
            this.argb = argb;
            this.source = source;
        }

        @NotNull
        static LookColor of(int argb) {
            return new LookColor(true, argb, null);
        }

        @NotNull
        static LookColor following(@NotNull IntSupplier source) {
            return new LookColor(true, 0, source);
        }

        @NotNull
        private DrawableColor resolve() {
            int argb = (this.source != null) ? this.source.getAsInt() : this.argb;
            return DrawableColor.of((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, argb >>> 24);
        }

    }

}
