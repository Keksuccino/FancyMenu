package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.ScreenUtils;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.events.widget.RenderTabNavigationBarHeaderBackgroundEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TabNavigationBar.class)
public class MixinTabNavigationBar {

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void head_extractRenderState_FancyMenu(GuiGraphicsExtractor graphics, int $$1, int $$2, float $$3, CallbackInfo info) {
        TabNavigationBar bar = this.getBar_FancyMenu();
        EventHandler.INSTANCE.postEvent(new RenderTabNavigationBarHeaderBackgroundEvent.Pre(bar, graphics, bar.getWidth(), bar.getHeight()));
    }

    @Inject(method = "extractWidgetRenderState", at = @At("RETURN"))
    private void after_extractRenderState_FancyMenu(GuiGraphicsExtractor graphics, int $$1, int $$2, float $$3, CallbackInfo info) {
        TabNavigationBar bar = this.getBar_FancyMenu();
        EventHandler.INSTANCE.postEvent(new RenderTabNavigationBarHeaderBackgroundEvent.Post(bar, graphics, bar.getWidth(), bar.getHeight()));
    }

    /**
     * @reason Re-init the screen when the tab got changed by clicking one of the Tab buttons at the top
     */
    @Inject(method = "setFocused(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/tabs/TabManager;setCurrentTab(Lnet/minecraft/client/gui/components/tabs/Tab;Z)V", shift = At.Shift.AFTER))
    private void after_setCurrentTab_in_setFocused_FancyMenu(GuiEventListener guiEventListener, CallbackInfo info) {
        this.reInitScreenAfterTabChanged_FancyMenu();
    }

    /**
     * @reason Re-init the screen when the tab got changed by using keys (arrow keys, Tab key, number keys)
     */
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/tabs/TabNavigationBar;selectTab(IZ)V", shift = At.Shift.AFTER))
    private void after_selectTab_in_keyPressed_FancyMenu(KeyEvent event, CallbackInfoReturnable<Boolean> info) {
        this.reInitScreenAfterTabChanged_FancyMenu();
    }

    //Without this FancyMenu wouldn't be able to customize Tab screens (even with this it's still not fully working, but at least it doesn't completely break everything anymore)
    @Unique
    private void reInitScreenAfterTabChanged_FancyMenu() {
        if (ScreenUtils.getScreen() != null) {
            if (ScreenCustomization.isCustomizationEnabledForScreen(ScreenUtils.getScreen()) && this.isBarPartOfCurrentScreen_FancyMenu()) {
                ScreenCustomization.reInitCurrentScreen();
            }
        }
    }

    @Unique
    private boolean isBarPartOfCurrentScreen_FancyMenu() {
        if (ScreenUtils.getScreen() == null) return false;
        return ((IMixinScreen)ScreenUtils.getScreen()).getChildrenFancyMenu().contains(this.getBar_FancyMenu());
    }

    @Unique
    private TabNavigationBar getBar_FancyMenu() {
        return (TabNavigationBar)((Object)this);
    }

}
