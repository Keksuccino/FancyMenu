package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.rendering.ui.screen.WidgetifiedScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
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

@WidgetifiedScreen
@Mixin(DeathScreen.class)
public abstract class MixinDeathScreen extends Screen {

    @Unique private TextWidget titleTextFancyMenu;
    @Unique @Nullable private TextWidget causeOfDeathTextFancyMenu;
    @Unique private TextWidget deathScoreTextFancyMenu;

    @Shadow @Final @Nullable private Component causeOfDeath;
    @Shadow @Final private Component deathScore;

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

    @Inject(method = "visitText", at = @At("HEAD"), cancellable = true)
    private void before_visitText_FancyMenu(ActiveTextCollector collector, CallbackInfo info) {
        if (this.isCustomizableFancyMenu()) {
            info.cancel();
        }
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
    private void before_mouseClicked_FancyMenu(MouseButtonEvent event, boolean isDoubleClick, CallbackInfoReturnable<Boolean> info) {
        if (!this.isCustomizableFancyMenu() || this.causeOfDeathTextFancyMenu == null || this.causeOfDeath == null) {
            return;
        }
        if (!this.causeOfDeathTextFancyMenu.isTextHovered(event.x(), event.y())) {
            return;
        }
        if (((CustomizableWidget)this.causeOfDeathTextFancyMenu).isHiddenFancyMenu()) {
            return;
        }
        Style style = this.causeOfDeathTextFancyMenu.getStyleAtMouseX(event.x());
        if (style != null && style.getClickEvent() instanceof ClickEvent.OpenUrl openUrl) {
            info.setReturnValue(clickUrlAction(this.minecraft, this, openUrl.uri()));
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
        Style style = this.causeOfDeathTextFancyMenu.getStyleAtMouseX(mouseX);
        if (style != null) {
            graphics.renderComponentHoverEffect(this.font, style, mouseX, mouseY);
        }
    }

    @Unique
    private boolean isCustomizableFancyMenu() {
        return ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}
