package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlayMenuBar;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinCreativeModeInventoryScreen extends Screen {

    @Unique private boolean resized_FancyMenu = false;

    // Dummy constructor
    private MixinCreativeModeInventoryScreen() {
        super(Component.empty());
    }

    /**
     * @reason Fixes FancyMenu's menu not counting as top-level widget, so creative mode tabs get clicked below it
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void head_mouseClicked_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        CustomizationOverlayMenuBar bar = CustomizationOverlay.getCurrentMenuBarInstance();
        if (bar != null) {
            if (bar.isExpanded() && bar.isUserNavigatingInMenuBar()) {
                info.setReturnValue(bar.mouseClicked(mouseX, mouseY, button));
            }
        }
    }

    /**
     * @reason Fix FM's menu bar not working without resizing the screen once.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(CallbackInfo info) {
        if (!this.resized_FancyMenu) {
            this.resized_FancyMenu = true;
            ScreenCustomization.reInitCurrentScreen();
        }
    }

}
