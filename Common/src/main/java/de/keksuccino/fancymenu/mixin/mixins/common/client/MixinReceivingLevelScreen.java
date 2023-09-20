package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.patches.WidgetifiedScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@WidgetifiedScreen
@Mixin(ReceivingLevelScreen.class)
public class MixinReceivingLevelScreen extends Screen {

    private static final Component DOWNLOADING_TERRAIN_TEXT_FANCYMENU = Component.translatable("multiplayer.downloadingTerrain");

    protected MixinReceivingLevelScreen(Component $$0) {
        super($$0);
    }

    @Override
    protected void init() {

        this.addRenderableWidget(TextWidget.of(DOWNLOADING_TERRAIN_TEXT_FANCYMENU, 0, (this.height / 2) - 50, 200))
                .centerWidget(this)
                .setTextAlignment(TextWidget.TextAlignment.CENTER)
                .setWidgetIdentifierFancyMenu("downloading_terrain");

    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ReceivingLevelScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private boolean wrapDrawCenteredStringInRenderFancyMenu(PoseStack poseStack, Font font, Component component, int i1, int i2, int i3) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}
