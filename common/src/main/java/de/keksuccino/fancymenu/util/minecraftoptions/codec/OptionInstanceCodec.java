package de.keksuccino.fancymenu.util.minecraftoptions.codec;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import net.minecraft.client.OptionInstance;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class OptionInstanceCodec<T> {

    protected final Class<? extends T> type;
    protected final ConsumingSupplier<T, String> getter;
    protected final ConsumingSupplier<String, T> setter;

    public OptionInstanceCodec(@NotNull Class<? extends T> type, @NotNull ConsumingSupplier<T, String> getter, @NotNull ConsumingSupplier<String, T> setter) {
        this.type = type;
        this.getter = getter;
        this.setter = setter;
    }

    @SuppressWarnings("all")
    public void set(@NotNull OptionInstance<?> instance, @NotNull String value) {
        Objects.requireNonNull(value);
        if (this.isCompatible(instance)) {
            OptionInstance<T> casted = (OptionInstance<T>) instance;
            T setTo = this.setter.get(value);
            if (setTo != null) casted.set(setTo);
        }
    }

    @SuppressWarnings("all")
    @Nullable
    public String get(@NotNull OptionInstance<?> instance) {
        if (this.isCompatible(instance)) {
            OptionInstance<T> casted = (OptionInstance<T>) instance;
            //TODO remove debug
            LogManager.getLogger().info("############## GET: " + casted.get().getClass() + " | CODEC TYPE: " + this.type + " | OPTION: " + instance.toString());
            return this.getter.get(casted.get());
        }
        return null;
    }

    @SuppressWarnings("all")
    public boolean isCompatible(@NotNull OptionInstance<?> instance) {
        try {
            Objects.requireNonNull(instance);
            OptionInstance<T> casted = (OptionInstance<T>) instance;
            T value = casted.get();
            return true;
        } catch (Exception ignore) {}
        return false;
    }

}
