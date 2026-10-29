package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import de.keksuccino.fancymenu.mixin.support.client.WidgetifiedTextReplacementController;
import de.keksuccino.fancymenu.util.rendering.ui.screen.WidgetifiedScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
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
    @Unique private final WidgetifiedTextReplacementController<TextWidget> textReplacementController_FancyMenu = new WidgetifiedTextReplacementController<>();

    protected MixinConnectScreen(Component $$0) {
        super($$0);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void afterInitFancyMenu(CallbackInfo info) {
        this.statusTextFancyMenu = this.textReplacementController_FancyMenu.initialize(ScreenCustomization.isCustomizationEnabledForScreen(this), this::createStatusTextWidget_FancyMenu);
    }

    @Inject(at = @At("HEAD"), method = "startConnecting")
    private static void onStartConnectingFancyMenu(Screen screen, Minecraft mc, ServerAddress address, ServerData data, boolean isQuickPlay, CallbackInfo ci) {
        if (address != null) {
            LastWorldHandler.setLastWorld(address.getHost() + ":" + address.getPort(), true);
        }
        String serverIp = getServerIp_FancyMenu(address, data);
        if (serverIp != null) {
            SeamlessWorldLoadingHandler.beginServerLoad(serverIp);
        }
    }

    /**
     * Fires after vanilla started the connection attempt instead of at HEAD, so it skips vanilla's "already connecting" early return and runs after startConnecting
     * cleared the level of the previous server, which keeps it after "On Server Left" of that server.
     * The ConnectScreen is already open at this point, so join actions of the listener get refused by vanilla and the Join Server action instead of starting nested connections.
     */
    @Inject(method = "startConnecting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ConnectScreen;connect(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;)V", shift = At.Shift.AFTER))
    private static void afterConnectInStartConnectingFancyMenu(Screen screen, Minecraft mc, ServerAddress address, ServerData data, boolean isQuickPlay, CallbackInfo ci) {
        if (Listeners.ON_START_JOINING_SERVER.hasInstancesListening()) Listeners.ON_START_JOINING_SERVER.onStartJoiningServer(getServerIp_FancyMenu(address, data));
    }

    @Inject(at = @At("HEAD"), method = "connect", cancellable = true)
    private void onConnectFancyMenu(Minecraft minecraft, ServerAddress address, ServerData serverData, CallbackInfo info) {
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

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private boolean wrapDrawCenteredStringInRenderFancyMenu(GuiGraphics instance, Font $$0, Component $$1, int $$2, int $$3, int $$4) {
        return this.textReplacementController_FancyMenu.shouldRenderVanillaText();
    }

    @Unique
    @Nullable
    private static String getServerIp_FancyMenu(@Nullable ServerAddress address, @Nullable ServerData data) {
        if (data != null && data.ip != null) return data.ip;
        if (address != null) return address.getHost() + ":" + address.getPort();
        return null;
    }

    @Unique
    private TextWidget createStatusTextWidget_FancyMenu() {
        TextWidget widget = TextWidget.of(this.status, 0, (this.height / 2) - 50, 200);
        widget.centerWidget(this);
        widget.setTextAlignment(TextWidget.TextAlignment.CENTER);
        widget.setWidgetIdentifierFancyMenu("status");
        return this.addRenderableWidget(widget);
    }

}
