package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public class MixinLevelLoadingScreen extends Screen {

    protected MixinLevelLoadingScreen(Component component) {
        super(component);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelRenderingFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {
        if (MenuCustomization.isMenuCustomizable(this)) {
            info.cancel();
            this.renderBackground(pose);
        }
    }

}
