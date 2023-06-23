package de.keksuccino.fancymenu.mixin.mixins.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelLoadingScreen.class)
public class MixinLevelLoadingScreen {

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;renderChunks(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/server/level/progress/StoringChunkProgressListener;IIII)V"))
    private boolean wrapRenderChunksFancyMenu(PoseStack poseStack, StoringChunkProgressListener storingChunkProgressListener, int i, int j, int k, int l) {
        return FancyMenu.getOptions().showLevelLoadingScreenChunkAnimation.getValue();
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private boolean wrapRenderPercentStringFancyMenu(PoseStack poseStack, Font font, String s, int i1, int i2, int i3) {
        return FancyMenu.getOptions().showLevelLoadingScreenPercent.getValue();
    }

}
