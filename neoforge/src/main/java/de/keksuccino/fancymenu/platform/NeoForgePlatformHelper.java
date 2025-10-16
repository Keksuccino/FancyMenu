package de.keksuccino.fancymenu.platform;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.platform.services.IPlatformHelper;
import de.keksuccino.fancymenu.util.mod.UniversalModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "neoforge";
    }

    @Override
    public String getPlatformDisplayName() {
        return "NeoForge";
    }

    @Override
    public String getLoaderVersion() {
        return this.getModVersion("neoforge");
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public String getModVersion(String modId) {
        try {
            Optional<? extends ModContainer> o = ModList.get().getModContainerById(modId);
            if (o.isPresent()) {
                ModContainer c = o.get();
                return c.getModInfo().getVersion().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.0.0";
    }

    @Override
    public List<String> getLoadedModIds() {
        List<String> l = new ArrayList<>();
        for (IModInfo info : ModList.get().getMods()) {
            l.add(info.getModId());
        }
        return l;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public boolean isOnClient() {
        return FMLEnvironment.getDist() == Dist.CLIENT;
    }

    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        return keyMapping.getKey();
    }

    @Override
    public @Nullable ResourceLocation getItemKey(@NotNull Item item) {
        try {
            return BuiltInRegistries.ITEM.getKey(item);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ResourceLocation getEffectKey(@NotNull MobEffect effect) {
        try {
            return BuiltInRegistries.MOB_EFFECT.getKey(effect);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ResourceLocation getEntityKey(@NotNull EntityType<?> type) {
        try {
            return BuiltInRegistries.ENTITY_TYPE.getKey(type);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @NotNull List<UniversalModContainer> getLoadedMods() {
        List<UniversalModContainer> mods = new ArrayList<>();
        ModList.get().getMods().forEach(mod -> {
            mods.add(new UniversalModContainer(mod.getModId(), mod.getDisplayName(), mod.getDescription(), mod.getOwningFile().getLicense(), null));
        });
        return mods;
    }

}