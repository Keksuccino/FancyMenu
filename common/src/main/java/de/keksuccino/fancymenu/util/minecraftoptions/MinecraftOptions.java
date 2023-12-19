package de.keksuccino.fancymenu.util.minecraftoptions;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinOptions;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class MinecraftOptions {

    private static final Map<String, MinecraftOption> CACHED_OPTIONS = new LinkedHashMap<>();

    @NotNull
    public static Map<String, MinecraftOption> getOptions() {
        cacheOptions();
        return CACHED_OPTIONS;
    }

    @Nullable
    public static MinecraftOption getOption(@NotNull String name) {
        return getOptions().get(name);
    }

    public static void save() {
        Minecraft.getInstance().options.save();
    }

    @SuppressWarnings("all")
    private static void cacheOptions() {

        if (CACHED_OPTIONS.isEmpty()) {

            try {

                ((IMixinOptions)Minecraft.getInstance().options).invokeProcessOptionsFancyMenu(new Options.FieldAccess() {
                    @Override
                    public <T> void process(String name, OptionInstance<T> instance) {
                        CACHED_OPTIONS.put(name, MinecraftOption.of(name, (OptionInstance<Object>) instance));
                    }
                    @Override
                    public int process(String s, int i) {
                        return i;
                    }
                    @Override
                    public boolean process(String s, boolean b) {
                        return b;
                    }
                    @Override
                    public String process(String s, String s1) {
                        return s1;
                    }
                    @Override
                    public float process(String s, float v) {
                        return v;
                    }
                    @Override
                    public <T> T process(String s, T t, Function<String, T> function, Function<T, String> function1) {
                        return t;
                    }
                });

                for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
                    MinecraftOption option = MinecraftOption.of(keyMapping);
                    CACHED_OPTIONS.put(option.name, option);
                }

                for (PlayerModelPart modelPart : PlayerModelPart.values()) {
                    MinecraftOption option = MinecraftOption.of(modelPart);
                    CACHED_OPTIONS.put(option.name, option);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

    }

}
