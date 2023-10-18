package de.keksuccino.drippyloadingscreen.mixin.mixins.client;

import de.keksuccino.drippyloadingscreen.customization.items.bars.AbstractProgressBarCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CustomizationItemBase.class)
public class MixinCustomizationItemBase {

    @Shadow public CustomizationItemBase orientationElement;

    @Inject(method = "getOrientationElementPosX", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetOrientationElementPosX(Screen menu, CallbackInfoReturnable<Integer> info) {
        if (this.orientationElement != null) {
            if (this.orientationElement instanceof AbstractProgressBarCustomizationItem) {
                AbstractProgressBarCustomizationItem i = (AbstractProgressBarCustomizationItem) this.orientationElement;
                if (i.useProgressForElementOrientation) {
                    if (i.direction == AbstractProgressBarCustomizationItem.BarDirection.RIGHT) {
                        info.setReturnValue(i.getProgressX() + i.getProgressWidth());
                    }
                    if (i.direction == AbstractProgressBarCustomizationItem.BarDirection.LEFT) {
                        info.setReturnValue(i.getProgressX());
                    }
                }
            }
        }
    }

    @Inject(method = "getOrientationElementPosY", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetOrientationElementPosY(Screen menu, CallbackInfoReturnable<Integer> info) {
        if (this.orientationElement != null) {
            if (this.orientationElement instanceof AbstractProgressBarCustomizationItem) {
                AbstractProgressBarCustomizationItem i = (AbstractProgressBarCustomizationItem) this.orientationElement;
                if (i.useProgressForElementOrientation) {
                    if (i.direction == AbstractProgressBarCustomizationItem.BarDirection.DOWN) {
                        info.setReturnValue(i.getProgressY() + i.getProgressHeight());
                    }
                    if (i.direction == AbstractProgressBarCustomizationItem.BarDirection.UP) {
                        info.setReturnValue(i.getProgressY());
                    }
                }
            }
        }
    }

}
