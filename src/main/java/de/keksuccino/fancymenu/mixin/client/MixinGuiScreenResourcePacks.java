package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiScreenResourcePacks.class)
public class MixinGuiScreenResourcePacks {

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;refreshResources()V"), method = "actionPerformed")
    protected void onActionPerformed(Minecraft minecraft) {

        minecraft.refreshResources();

        System.out.println("[FANCYMENU] Updating animation sizes..");
        AnimationHandler.setupAnimationSizes();

    }

}
