package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinCreativeModeInventoryScreen extends Screen {

    @Unique private boolean resized_FancyMenu = false;

    private MixinCreativeModeInventoryScreen() {
        super(null);
    }

    /**
     * @reason Fix FM's menu bar not working without resizing the screen once.
     */
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void before_extractRenderState_FancyMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
        if (!this.resized_FancyMenu) {
            this.resized_FancyMenu = true;
            ScreenCustomization.reInitCurrentScreen();
        }
    }

}
