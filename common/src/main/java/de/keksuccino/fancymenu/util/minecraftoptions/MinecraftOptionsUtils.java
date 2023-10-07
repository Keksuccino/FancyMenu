package de.keksuccino.fancymenu.util.minecraftoptions;

import de.keksuccino.fancymenu.util.ExtendedMinecraftOptions;
import de.keksuccino.fancymenu.util.minecraftoptions.codec.OptionInstanceCodec;
import de.keksuccino.fancymenu.util.minecraftoptions.codec.OptionInstanceCodecRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MinecraftOptionsUtils {

    @NotNull
    public static Map<String, OptionInstance<?>> getOptionInstances() {
        return ((ExtendedMinecraftOptions)Minecraft.getInstance().options).getOptionInstancesFancyMenu();
    }

    @NotNull
    public static List<OptionInstanceBundle> getOptionInstancesWithCodec() {
        List<OptionInstanceBundle> bundles = new ArrayList<>();
        for (Map.Entry<String, OptionInstance<?>> m : getOptionInstances().entrySet()) {
            OptionInstanceCodec<?> codec = findCodec(m.getValue());
            if (codec != null) {
                bundles.add(new OptionInstanceBundle(m.getKey(), m.getValue(), codec));
            }
        }
        return bundles;
    }

    @Nullable
    public static OptionInstanceBundle getOptionInstanceWithCodec(@NotNull String name) {
        for (OptionInstanceBundle b : getOptionInstancesWithCodec()) {
            if (b.name().equals(name)) return b;
        }
        return null;
    }

    @Nullable
    public static OptionInstanceCodec<?> findCodec(@NotNull OptionInstance<?> instance) {
        for (OptionInstanceCodec<?> codec : OptionInstanceCodecRegistry.getCodecs()) {
            if (codec.isCompatible(instance)) return codec;
        }
        return null;
    }

    public static void save() {
        Minecraft.getInstance().options.save();
    }

    public record OptionInstanceBundle(@NotNull String name, @NotNull OptionInstance<?> instance, @NotNull OptionInstanceCodec<?> codec) {
    }

}
