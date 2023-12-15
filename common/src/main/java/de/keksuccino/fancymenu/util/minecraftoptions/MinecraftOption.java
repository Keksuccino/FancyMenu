package de.keksuccino.fancymenu.util.minecraftoptions;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinOptions;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.MathUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class MinecraftOption {

    protected String name;
    protected OptionInstance<Object> optionInstance;
    protected KeyMapping keyMapping;
    protected PlayerModelPart modelPart;

    @NotNull
    public static MinecraftOption of(@NotNull String name, @NotNull OptionInstance<?> optionInstance) {
        MinecraftOption option = new MinecraftOption();
        option.optionInstance = (OptionInstance<Object>) optionInstance;
        option.name = name;
        return option;
    }

    @NotNull
    public static MinecraftOption of(@NotNull KeyMapping keyMapping) {
        MinecraftOption option = new MinecraftOption();
        option.name = "key_" + keyMapping.getName();
        option.keyMapping = keyMapping;
        return option;
    }

    @NotNull
    public static MinecraftOption of(@NotNull PlayerModelPart modelPart) {
        MinecraftOption option = new MinecraftOption();
        option.name = "modelPart_" + modelPart.getId();
        option.modelPart = modelPart;
        return option;
    }

    protected MinecraftOption() {
    }

    @Nullable
    public String get() {
        try {
            if (this.optionInstance != null) {
                return this.optionInstance.get();
            } else if (this.keyMapping != null) {
                return this.keyMapping.saveString();
            } else if (this.modelPart != null) {
                return "" + ((IMixinOptions)Minecraft.getInstance().options).getModelPartsFancyMenu().contains(this.modelPart);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void set(@NotNull String value) {
        try {
            if (this.optionInstance != null) {
                this.optionInstance.set(value);
            } else if (this.keyMapping != null) {
                this.keyMapping.setKey(InputConstants.getKey(value));
            } else if (this.modelPart != null) {
                Set<PlayerModelPart> parts = ((IMixinOptions)Minecraft.getInstance().options).getModelPartsFancyMenu();
                if (value.equalsIgnoreCase("true") && !parts.contains(this.modelPart)) {
                    parts.add(this.modelPart);
                } else if (value.equalsIgnoreCase("false")) {
                    parts.remove(this.modelPart);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Minecraft.getInstance().options.save();
    }

    @Nullable
    public OptionInstance<Object> getOptionInstance() {
        return this.optionInstance;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public static class OptionInstance<T> {

        protected final String name;
        protected final Consumer<T> setter;
        protected final Supplier<T> getter;
        protected final OptionCodec<T> codec;

        public OptionInstance(@NotNull String name, @NotNull Supplier<T> getter, @NotNull Consumer<T> setter, @NotNull OptionCodec<T> codec) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.codec = codec;
        }

        @NotNull
        public String getName() {
            return this.name;
        }

        public void set(@NotNull String value) {
            T t = this.codec.write.get(Objects.requireNonNull(value));
            if (t != null) this.setter.accept(t);
        }

        @Nullable
        public String get() {
            T t = this.getter.get();
            if (t != null) return this.codec.read.get(t);
            return null;
        }

    }

    public record OptionCodec<T>(@NotNull ConsumingSupplier<T, String> read, @NotNull ConsumingSupplier<String, T> write) {

        public static final OptionCodec<String> STRING_CODEC = new OptionCodec<>(consumes -> consumes, consumes -> consumes);
        public static final OptionCodec<Integer> INTEGER_CODEC = new OptionCodec<>(Object::toString, consumes -> MathUtils.isInteger(consumes) ? Integer.parseInt(consumes) : null);
        public static final OptionCodec<Long> LONG_CODEC = new OptionCodec<>(Object::toString, consumes -> MathUtils.isLong(consumes) ? Long.parseLong(consumes) : null);
        public static final OptionCodec<Double> DOUBLE_CODEC = new OptionCodec<>(Object::toString, consumes -> MathUtils.isDouble(consumes) ? Double.parseDouble(consumes) : null);
        public static final OptionCodec<Float> FLOAT_CODEC = new OptionCodec<>(Object::toString, consumes -> MathUtils.isFloat(consumes) ? Float.parseFloat(consumes) : null);
        public static final OptionCodec<Boolean> BOOLEAN_CODEC = new OptionCodec<>(consumes -> consumes ? "true" : "false", "true"::equalsIgnoreCase);

    }

}
