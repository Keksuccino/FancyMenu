package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.DataPackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import javax.annotation.Nullable;

@Mixin(CreateWorldScreen.class)
public interface IMixinCreateWorldScreen {

    @Invoker("<init>") static CreateWorldScreen invokeConstructFancyMenu(@Nullable Screen $$0, DataPackConfig $$1, WorldGenSettingsComponent $$2) {
        return null;
    }

    @Invoker("createDefaultLoadConfig") static WorldLoader.InitConfig invokeCreateDefaultLoadConfigFancyMenu(PackRepository $$0, DataPackConfig $$1) {
        return null;
    }

}
