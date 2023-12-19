package de.keksuccino.fancymenu.customization.layout.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class ChooseAnimationScreen extends Screen {

    protected Consumer<String> callback;
    protected String selectedAnimationName = null;
    protected IAnimationRenderer selectedAnimation = null;

    protected ScrollArea animationListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;

    public ChooseAnimationScreen(@Nullable String preSelectedAnimation, @NotNull Consumer<String> callback) {

        super(Component.translatable("fancymenu.animation.choose"));

        this.callback = callback;
        this.updateAnimationScrollAreaContent();

        if (preSelectedAnimation != null) {
            for (ScrollAreaEntry e : this.animationListScrollArea.getEntries()) {
                if ((e instanceof AnimationScrollEntry a) && a.animation.equals(preSelectedAnimation)) {
                    a.setSelected(true);
                    this.setSelectedAnimation(a);
                    break;
                }
            }
        }

    }

    @Override
    protected void init() {

        super.init();

        this.doneButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.done"), (button) -> {
            this.callback.accept(this.selectedAnimationName);
        }) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                if (ChooseAnimationScreen.this.selectedAnimationName == null) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.animation.choose.no_animation_selected")).setDefaultStyle(), false, true);
                    this.active = false;
                } else {
                    this.active = true;
                }
                super.renderWidget(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, Component.translatable("fancymenu.animation.choose.available_animations"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.animationListScrollArea.setWidth((this.width / 2) - 40, true);
        this.animationListScrollArea.setHeight(this.height - 85, true);
        this.animationListScrollArea.setX(20, true);
        this.animationListScrollArea.setY(50 + 15, true);
        this.animationListScrollArea.render(graphics, mouseX, mouseY, partial);

        Component previewLabel = Component.translatable("fancymenu.animation.choose.preview");
        int previewLabelWidth = this.font.width(previewLabel);
        graphics.drawString(this.font, previewLabel, this.width - 20 - previewLabelWidth, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        if (this.selectedAnimation != null) {
            int aniW = (this.width / 2) - 40;
            int aniH = this.height / 2;
            AspectRatio ratio = new AspectRatio(this.selectedAnimation.getWidth(), this.selectedAnimation.getHeight());
            int[] size = ratio.getAspectRatioSizeByMaximumSize(aniW, aniH);
            aniW = size[0];
            aniH = size[1];
            int aniX = this.width - 20 - aniW;
            int aniY = 50 + 15;
            boolean cachedLooped = this.selectedAnimation.isGettingLooped();
            graphics.fill(aniX, aniY, aniX + aniW, aniY + aniH, UIBase.getUIColorTheme().area_background_color.getColorInt());
            this.selectedAnimation.setLooped(false);
            this.selectedAnimation.setPosX(aniX);
            this.selectedAnimation.setPosY(aniY);
            this.selectedAnimation.setWidth(aniW);
            this.selectedAnimation.setHeight(aniH);
            this.selectedAnimation.render(graphics);
            this.selectedAnimation.setLooped(cachedLooped);
            UIBase.renderBorder(graphics, aniX, aniY, aniX + aniW, aniY + aniH, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.getUIColorTheme().element_border_color_normal.getColor(), true, true, true, true);
        }

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(graphics, mouseX, mouseY, partial);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.doneButton.getY() - 5 - 20);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    protected void setSelectedAnimation(@Nullable AnimationScrollEntry entry) {
        if (this.selectedAnimation != null) {
            this.selectedAnimation.resetAnimation();
            if (this.selectedAnimation instanceof AdvancedAnimation) {
                ((AdvancedAnimation)this.selectedAnimation).stopAudio();
                ((AdvancedAnimation)this.selectedAnimation).resetAudio();
            }
        }
        if (entry == null) {
            this.selectedAnimation = null;
            this.selectedAnimationName = null;
            return;
        }
        this.selectedAnimationName = entry.animation;
        this.selectedAnimation = AnimationHandler.getAnimation(entry.animation);
        if (this.selectedAnimation != null) {
            this.selectedAnimation.resetAnimation();
            if (this.selectedAnimation instanceof AdvancedAnimation) {
                ((AdvancedAnimation)this.selectedAnimation).stopAudio();
                ((AdvancedAnimation)this.selectedAnimation).resetAudio();
            }
        }
    }

    protected void updateAnimationScrollAreaContent() {
        this.animationListScrollArea.clearEntries();
        for (String s : AnimationHandler.getExternalAnimationNames()) {
            AnimationScrollEntry e = new AnimationScrollEntry(this.animationListScrollArea, s, (entry) -> {
                this.setSelectedAnimation((AnimationScrollEntry)entry);
            });
            this.animationListScrollArea.addEntry(e);
        }
        if (this.animationListScrollArea.getEntries().isEmpty()) {
            this.animationListScrollArea.addEntry(new TextScrollAreaEntry(this.animationListScrollArea, Component.translatable("fancymenu.animation.choose.no_animations"), (entry) -> {}));
        }
        int totalWidth = this.animationListScrollArea.getTotalEntryWidth();
        for (ScrollAreaEntry e : this.animationListScrollArea.getEntries()) {
            e.setWidth(totalWidth);
        }
    }

    @Override
    public boolean keyPressed(int button, int $$1, int $$2) {

        if (button == InputConstants.KEY_ENTER) {
            if (this.selectedAnimationName != null) {
                this.callback.accept(this.selectedAnimationName);
                return true;
            }
        }

        return super.keyPressed(button, $$1, $$2);

    }

    public static class AnimationScrollEntry extends TextListScrollAreaEntry {

        public String animation;

        public AnimationScrollEntry(ScrollArea parent, @NotNull String animation, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, Component.literal(animation).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), UIBase.getUIColorTheme().listing_dot_color_1.getColor(), onClick);
            this.animation = animation;
        }

    }

}
