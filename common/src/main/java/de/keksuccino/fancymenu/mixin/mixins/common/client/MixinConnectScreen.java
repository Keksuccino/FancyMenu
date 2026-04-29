package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.WidgetifiedScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@WidgetifiedScreen
@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen extends Screen {

    @Shadow private Component status;

    @Unique private TextWidget statusTextFancyMenu;

    protected MixinConnectScreen(Component $$0) {
        super($$0);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void afterInitFancyMenu(CallbackInfo info) {

        this.statusTextFancyMenu = this.addRenderableWidget(TextWidget.of(this.status, 0, (this.height / 2) - 50, 200))
                .centerWidget(this)
                .setTextAlignment(TextWidget.TextAlignment.CENTER)
                .setWidgetIdentifierFancyMenu("status");

    }

    @Inject(at = @At("HEAD"), method = "startConnecting")
    private static void onStartConnectingFancyMenu(Screen screen, Minecraft mc, ServerAddress address, ServerData data, CallbackInfo ci) {
        if (address != null) {
            LastWorldHandler.setLastWorld(address.getHost() + ":" + address.getPort(), true);
        }
        if (data != null && data.ip != null) {
            SeamlessWorldLoadingHandler.beginServerLoad(data.ip);
        } else if (address != null) {
            SeamlessWorldLoadingHandler.beginServerLoad(address.getHost() + ":" + address.getPort());
        }
    }

    @Inject(at = @At("HEAD"), method = "connect", cancellable = true)
    private void onConnectFancyMenu(Minecraft minecraft, ServerAddress address, CallbackInfo info) {
        if (address.getHost().equals("%fancymenu_dummy_address%")) {
            info.cancel();
        }
    }

    @Inject(method = "updateStatus", at = @At("RETURN"))
    private void afterUpdateStatusFancyMenu(Component component, CallbackInfo info) {
        if (this.statusTextFancyMenu != null) {
            this.statusTextFancyMenu.setMessage((component != null) ? component : Component.empty());
        }
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ConnectScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private boolean wrapDrawCenteredStringInRenderFancyMenu(PoseStack poseStack, Font $$0, Component $$1, int $$2, int $$3, int $$4) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}
