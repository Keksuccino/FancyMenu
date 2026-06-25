package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.WelcomeWindowBody;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.events.screen.CloseScreenEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.events.screen.OpenScreenEvent;
import de.keksuccino.fancymenu.events.screen.OpenScreenPostInitEvent;
import de.keksuccino.fancymenu.events.screen.ScreenTickEvent;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PipableScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinGui {

    @Unique private boolean lateClientInitDoneFancyMenu = false;
    @Unique @Nullable private Screen lastScreen_FancyMenu = null;

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Nullable private Screen screen;

    @Inject(method = "setOverlay", at = @At("HEAD"))
    private void before_setOverlay_FancyMenu(Overlay overlay, CallbackInfo info) {
        if (!this.lateClientInitDoneFancyMenu) {
            this.lateClientInitDoneFancyMenu = true;
            FancyMenu.lateClientInit();
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;tick()V"))
    private void before_screenTick_FancyMenu(CallbackInfo info) {
        if (this.screen == null) return;
        EventHandler.INSTANCE.postEvent(new ScreenTickEvent.Pre(this.screen));
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;tick()V", shift = At.Shift.AFTER))
    private void after_screenTick_FancyMenu(CallbackInfo info) {
        if (this.screen == null) return;
        EventHandler.INSTANCE.postEvent(new ScreenTickEvent.Post(this.screen));
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void before_setScreen_FancyMenu(Screen screen, CallbackInfo info) {
        if (ScreenUtils.areSetScreenCallsBlocked()) {
            info.cancel();
            return;
        }

//        // This routes setScreen() calls inside PipWindows through the actual window instead of normal MC
//        PiPWindow pip = PiPWindowHandler.INSTANCE.getLastClickedWindowThisTick();
//        if (pip != null) {
//            pip.setScreen(screen);
//            info.cancel();
//            return;
//        }

        if (screen instanceof PipableScreen) {
            throw new RuntimeException("[FANCYMENU] PipableScreens can't be set as normal screens! They are meant to be used only for PiPWindows! Failed to open as normal screen: " + screen);
        }

        // This is just for giving FM the correct screen identifiers for all possible scenarios
        if ((screen == null) && (this.minecraft.level == null)) {
            screen = new TitleScreen();
        } else if ((screen == null) && ((this.minecraft.player != null) && this.minecraft.player.isDeadOrDying())) {
            if (this.minecraft.player.shouldShowDeathScreen()) {
                screen = new DeathScreen(null, this.minecraft.level.getLevelData().isHardcore(), this.minecraft.player);
            }
        }
        final Screen finalScreen = screen;

        if ((ScreenUtils.getScreen() instanceof LayoutEditorScreen e) && !(screen instanceof LayoutEditorScreen)) {
            e.layout.menuBackgrounds.forEach(menuBackground -> {
                menuBackground.onCloseScreen(e, finalScreen);
                menuBackground.onDisableOrRemove();
            });
            e.getAllElements().forEach(element -> {
                element.element.onCloseScreen(e, finalScreen);
                element.element.onDestroyElement();
            });
        }

        if (screen instanceof LayoutEditorScreen e) {
            e.justOpened = true;
        }

        this.lastScreen_FancyMenu = this.screen;

        // Reset GUI scale in case some layout changed it
        RenderingUtils.resetGuiScale();

        // Open Welcome window
        if (FancyMenu.getOptions().showWelcomeScreen.getValue() && !FancyMenu.getOptions().modpackMode.getValue() && (screen instanceof TitleScreen)) {
            FancyMenu.getOptions().showWelcomeScreen.setValue(false);
            WelcomeWindowBody.openInWindow();
        }

        // Handle Overrides
        Screen overrideWith = CustomGuiHandler.beforeSetScreen(screen);
        if (overrideWith != null) {
            info.cancel();
            ScreenUtils.setScreen(overrideWith);
            return;
        }

        if ((screen != null) && (screen != this.screen)) {
            Screen cachedCurrent = this.screen;
            Listeners.ON_OPEN_SCREEN.onScreenOpened(screen);
            if (cachedCurrent != this.screen) {
                info.cancel();
            }
        }
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void after_setScreen_FancyMenu(Screen screen, CallbackInfo info) {
        boolean newScreenType = false;
        if ((this.lastScreen_FancyMenu == null) && (this.screen != null)) {
            newScreenType = true;
        } else if ((this.lastScreen_FancyMenu != null) && (this.screen == null)) {
            newScreenType = true;
        } else if ((this.lastScreen_FancyMenu != null) && (this.screen != null)) {
            String lastId = ScreenIdentifierHandler.getIdentifierOfScreen(this.lastScreen_FancyMenu);
            String newId = ScreenIdentifierHandler.getIdentifierOfScreen(this.screen);
            if (!lastId.equals(newId)) {
                newScreenType = true;
            }
        }

        if (newScreenType) ScreenCustomization.onSwitchingToNewScreenType(this.screen, this.lastScreen_FancyMenu);
    }

    /** @reason Init.Pre listeners may change the GUI scale, so init needs the refreshed scaled dimensions. */
    @WrapOperation(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(II)V"))
    private void wrap_init_FancyMenu(Screen instance, int width, int height, Operation<Void> original) {
        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(instance, InitOrResizeScreenEvent.InitializationPhase.INIT));
        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(instance, InitOrResizeScreenEvent.InitializationPhase.INIT));
        Window window = Minecraft.getInstance().getWindow();
        original.call(instance, window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateTitle()V"))
    private void after_initCurrentScreen_FancyMenu(Screen screen, CallbackInfo info) {
        if (screen != null) {
            ScrollScreenNormalizer.normalizeScrollableScreen(screen);
            EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(screen, InitOrResizeScreenEvent.InitializationPhase.INIT));
            EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(screen, InitOrResizeScreenEvent.InitializationPhase.INIT));
            EventHandler.INSTANCE.postEvent(new OpenScreenPostInitEvent(screen));
        }
    }

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
    private void before_screenRemoved_FancyMenu(Screen screen, CallbackInfo info) {
        if (this.screen == null) return;
        EventHandler.INSTANCE.postEvent(new CloseScreenEvent(this.screen, screen));
    }

    /** @reason Fire FancyMenu close screen listeners after the screen was removed. */
    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V", shift = At.Shift.AFTER))
    private void after_screenRemoved_FancyMenu(Screen screen, CallbackInfo info) {
        if (this.lastScreen_FancyMenu != null) {
            Listeners.ON_CLOSE_SCREEN.onScreenClosed(this.lastScreen_FancyMenu);
        }
    }

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
    private void before_screenAdded_FancyMenu(Screen screen, CallbackInfo info) {
        if (this.screen == null) return;
        EventHandler.INSTANCE.postEvent(new OpenScreenEvent(this.screen));
    }

}
