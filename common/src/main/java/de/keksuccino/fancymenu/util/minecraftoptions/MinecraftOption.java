package de.keksuccino.fancymenu.util.minecraftoptions;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinOptions;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.StringReader;
import java.util.Set;

public class MinecraftOption {

    private static final Gson GSON = new Gson();

    protected String name;
    protected OptionInstance<Object> optionInstance;
    protected KeyMapping keyMapping;
    protected PlayerModelPart modelPart;

    @NotNull
    public static MinecraftOption of(@NotNull String name, @NotNull OptionInstance<Object> optionInstance) {
        MinecraftOption option = new MinecraftOption();
        option.optionInstance = optionInstance;
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
                DataResult<JsonElement> result = this.optionInstance.codec().encodeStart(JsonOps.INSTANCE, this.optionInstance.get());
                if (result.error().isPresent()) return null;
                if (result.result().isPresent()) {
                    JsonElement json = result.result().get();
                    return GSON.toJson(json);
                }
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
                JsonReader reader = new JsonReader(new StringReader(value.isEmpty() ? "\"\"" : value));
                JsonElement json = JsonParser.parseReader(reader);
                DataResult<Object> result = this.optionInstance.codec().parse(JsonOps.INSTANCE, json);
                if (result.error().isPresent()) return;
                result.result().ifPresent(this.optionInstance::set);
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
    }

    @NotNull
    public OptionInstance<?> getOptionInstance() {
        return this.optionInstance;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

}
