package de.keksuccino.fancymenu.platform;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.platform.services.IPlatformHelper;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;
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
        return !FMLLoader.isProduction();
    }

    @Override
    public boolean isOnClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        return keyMapping.getKey();
    }

}