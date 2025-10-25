package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.rendering.ui.screen.WidgetifiedScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("deprecation")
@WidgetifiedScreen
@Mixin(DeathScreen.class)
public abstract class MixinDeathScreen extends Screen {

    @Shadow @Final @Nullable private Component causeOfDeath;
    @Shadow private Component deathScore;

    @Unique private TextWidget titleTextFancyMenu;
    @Unique @Nullable private TextWidget causeOfDeathTextFancyMenu;
    @Unique private TextWidget deathScoreTextFancyMenu;

    // dummy constructor
    @SuppressWarnings("all")
    private MixinDeathScreen() {
        super(Component.empty());
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void after_init_FancyMenu(CallbackInfo info) {
        if (this.isCustomizableFancyMenu()) {
            this.titleTextFancyMenu = this.addRenderableWidget(new TextWidget(0, 60, this.width, this.font.lineHeight, this.font, this.getTitle()))
                    .setTextAlignment(TextWidget.TextAlignment.CENTER)
                    .setScale(2.0F)
                    .setWidgetIdentifierFancyMenu("death_screen_title");

            if (this.causeOfDeath != null) {
                this.causeOfDeathTextFancyMenu = this.addRenderableWidget(new TextWidget(0, 85, this.width, this.font.lineHeight, this.font, this.causeOfDeath))
                        .setTextAlignment(TextWidget.TextAlignment.CENTER)
                        .setWidgetIdentifierFancyMenu("death_screen_cause_of_death");
            } else {
                this.causeOfDeathTextFancyMenu = null;
            }

            this.deathScoreTextFancyMenu = this.addRenderableWidget(new TextWidget(0, 100, this.width, this.font.lineHeight, this.font, this.deathScore))
                    .setTextAlignment(TextWidget.TextAlignment.CENTER)
                    .setWidgetIdentifierFancyMenu("death_screen_score");
        } else {
            this.titleTextFancyMenu = null;
            this.causeOfDeathTextFancyMenu = null;
            this.deathScoreTextFancyMenu = null;
        }
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private boolean cancel_renderCenteredText_FancyMenu(GuiGraphics instance, Font font, Component text, int x, int y, int color) {
        if (!this.isCustomizableFancyMenu()) {
            return true;
        }
        if (this.isTitleComponentFancyMenu(text)) {
            return false;
        }
        if (this.isCauseOfDeathComponentFancyMenu(text)) {
            return false;
        }
        if (this.isDeathScoreComponentFancyMenu(text)) {
            return false;
        }
        return true;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentHoverEffect(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Style;II)V"))
    private boolean cancel_renderHover_FancyMenu(GuiGraphics instance, Font font, Style style, int mouseX, int mouseY) {
        return !this.isCustomizableFancyMenu();
    }

    @WrapWithCondition(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/DeathScreen;handleComponentClicked(Lnet/minecraft/network/chat/Style;)Z"))
    private boolean cancel_handleComponentClicked_FancyMenu(DeathScreen instance, Style style) {
        return !this.isCustomizableFancyMenu();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void after_render_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
        if (!this.isCustomizableFancyMenu()) {
            return;
        }
        if (this.causeOfDeathTextFancyMenu != null) {
            this.renderCauseOfDeathTooltipFancyMenu(graphics, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void before_mouseClicked_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!this.isCustomizableFancyMenu() || this.causeOfDeathTextFancyMenu == null || this.causeOfDeath == null) {
            return;
        }
        if (!this.causeOfDeathTextFancyMenu.isTextHovered(mouseX, mouseY)) {
            return;
        }
        if (((CustomizableWidget)this.causeOfDeathTextFancyMenu).isHiddenFancyMenu()) {
            return;
        }
        Style style = this.getClickedComponentStyleAtFancyMenu((int)mouseX);
        if (style != null && style.getClickEvent() != null && style.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL) {
            this.handleComponentClicked(style);
            cir.setReturnValue(false);
        }
    }

    @Unique
    private void renderCauseOfDeathTooltipFancyMenu(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.causeOfDeathTextFancyMenu == null || this.causeOfDeath == null) {
            return;
        }
        if (!this.causeOfDeathTextFancyMenu.isTextHovered(mouseX, mouseY)) {
            return;
        }
        if (((CustomizableWidget)this.causeOfDeathTextFancyMenu).isHiddenFancyMenu()) {
            return;
        }
        Style style = this.getClickedComponentStyleAtFancyMenu(mouseX);
        if (style != null) {
            graphics.renderComponentHoverEffect(this.font, style, mouseX, mouseY);
        }
    }

    @Unique
    private @Nullable Style getClickedComponentStyleAtFancyMenu(int mouseX) {
        if (this.causeOfDeath == null || this.causeOfDeathTextFancyMenu == null) {
            return null;
        }
        if (((CustomizableWidget)this.causeOfDeathTextFancyMenu).isHiddenFancyMenu()) {
            return null;
        }
        double left = this.causeOfDeathTextFancyMenu.getRenderX();
        double right = left + this.causeOfDeathTextFancyMenu.getScaledTextWidth();
        if (mouseX < left || mouseX > right) {
            return null;
        }
        float scale = this.causeOfDeathTextFancyMenu.getScale();
        double relative = (mouseX - left) / Math.max(0.0001F, scale);
        return this.font.getSplitter().componentStyleAtWidth(this.causeOfDeath, Mth.floor(relative));
    }

    @Unique
    private boolean isTitleComponentFancyMenu(@NotNull Component component) {
        Component title = this.getTitle();
        return component == title || component.equals(title);
    }

    @Unique
    private boolean isCauseOfDeathComponentFancyMenu(@NotNull Component component) {
        if (this.causeOfDeath == null) {
            return false;
        }
        return component == this.causeOfDeath || component.equals(this.causeOfDeath);
    }

    @Unique
    private boolean isDeathScoreComponentFancyMenu(@NotNull Component component) {
        return component == this.deathScore || component.equals(this.deathScore);
    }

    @Unique
    private boolean isCustomizableFancyMenu() {
        return ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}
