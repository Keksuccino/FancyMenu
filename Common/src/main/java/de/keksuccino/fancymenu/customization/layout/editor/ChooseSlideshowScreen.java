
package de.keksuccino.fancymenu.customization.layout.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.misc.InputConstants;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.rendering.ui.widget.Button;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ChooseSlideshowScreen extends Screen {

    protected Screen parentScreen;
    protected Consumer<String> callback;
    protected String selectedSlideshowName = null;
    protected ExternalTextureSlideshowRenderer selectedSlideshow = null;

    protected ScrollArea slideshowListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected AdvancedButton doneButton;
    protected AdvancedButton cancelButton;

    public ChooseSlideshowScreen(@Nullable Screen parentScreen, @Nullable String preSelectedSlideshow, @NotNull Consumer<String> callback) {

        super(Component.translatable("fancymenu.slideshow.choose"));

        this.parentScreen = parentScreen;
        this.callback = callback;
        this.updateSlideshowScrollAreaContent();

        if (preSelectedSlideshow != null) {
            for (ScrollAreaEntry e : this.slideshowListScrollArea.getEntries()) {
                if ((e instanceof SlideshowScrollEntry a) && a.slideshow.equals(preSelectedSlideshow)) {
                    a.setSelected(true);
                    this.setSelectedSlideshow(a);
                    break;
                }
            }
        }

        this.doneButton = new Button(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.done"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.selectedSlideshowName);
        }) {
            @Override
            public void renderWidget(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
                if (ChooseSlideshowScreen.this.selectedSlideshowName == null) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.slideshow.choose.no_slideshow_selected")).setDefaultBackgroundColor(), false, true);
                    this.active = false;
                } else {
                    this.active = true;
                }
                super.renderWidget(matrix, mouseX, mouseY, partialTicks);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

        this.cancelButton = new Button(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.cancel"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(null);
        });
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parentScreen);
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        fill(pose, 0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(pose, titleComp, 20, 20, -1);

        this.font.draw(pose, Component.translatable("fancymenu.slideshow.choose.available_slideshows"), 20, 50, -1);

        this.slideshowListScrollArea.setWidth((this.width / 2) - 40, true);
        this.slideshowListScrollArea.setHeight(this.height - 85, true);
        this.slideshowListScrollArea.setX(20, true);
        this.slideshowListScrollArea.setY(50 + 15, true);
        this.slideshowListScrollArea.render(pose, mouseX, mouseY, partial);

        Component previewLabel = Component.translatable("fancymenu.slideshow.choose.preview");
        int previewLabelWidth = this.font.width(previewLabel);
        this.font.draw(pose, previewLabel, this.width - 20 - previewLabelWidth, 50, -1);

        if (this.selectedSlideshow != null) {
            int slideW = (this.width / 2) - 40;
            int slideH = this.height / 2;
            AspectRatio ratio = new AspectRatio(this.selectedSlideshow.getImageWidth(), this.selectedSlideshow.getImageHeight());
            int[] size = ratio.getAspectRatioSizeByMinimumSize(slideW, slideH);
            slideW = size[0];
            slideH = size[1];
            int slideX = this.width - 20 - slideW;
            int slideY = 50 + 15;
            fill(pose, slideX, slideY, slideX + slideW, slideY + slideH, UIBase.AREA_BACKGROUND_COLOR.getRGB());
            this.selectedSlideshow.x = slideX;
            this.selectedSlideshow.y = slideY;
            this.selectedSlideshow.width = slideW;
            this.selectedSlideshow.height = slideH;
            this.selectedSlideshow.render(pose);
            UIBase.renderBorder(pose, slideX, slideY, slideX + slideW, slideY + slideH, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.ELEMENT_BORDER_COLOR_IDLE, true, true, true, true);
        }

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(pose, mouseX, mouseY, partial);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.doneButton.getY() - 5 - 20);
        this.cancelButton.render(pose, mouseX, mouseY, partial);

        super.render(pose, mouseX, mouseY, partial);

    }

    protected void setSelectedSlideshow(@Nullable ChooseSlideshowScreen.SlideshowScrollEntry entry) {
        if (entry == null) {
            this.selectedSlideshow = null;
            this.selectedSlideshowName = null;
            return;
        }
        this.selectedSlideshowName = entry.slideshow;
        this.selectedSlideshow = SlideshowHandler.getSlideshow(entry.slideshow);
        if (this.selectedSlideshow != null) {
            if (!this.selectedSlideshow.isReady()) {
                this.selectedSlideshow.prepareSlideshow();
            }
        }
    }

    protected void updateSlideshowScrollAreaContent() {
        this.slideshowListScrollArea.clearEntries();
        for (String s : SlideshowHandler.getSlideshowNames()) {
            SlideshowScrollEntry e = new SlideshowScrollEntry(this.slideshowListScrollArea, s, (entry) -> {
                this.setSelectedSlideshow((SlideshowScrollEntry)entry);
            });
            this.slideshowListScrollArea.addEntry(e);
        }
        if (this.slideshowListScrollArea.getEntries().isEmpty()) {
            this.slideshowListScrollArea.addEntry(new TextScrollAreaEntry(this.slideshowListScrollArea, Component.translatable("fancymenu.slideshow.choose.no_slideshows"), (entry) -> {}));
        }
    }

    @Override
    public boolean keyPressed(int button, int $$1, int $$2) {

        if (button == InputConstants.KEY_ENTER) {
            if (this.selectedSlideshowName != null) {
                Minecraft.getInstance().setScreen(this.parentScreen);
                this.callback.accept(this.selectedSlideshowName);
                return true;
            }
        }

        return super.keyPressed(button, $$1, $$2);

    }

    public static class SlideshowScrollEntry extends TextListScrollAreaEntry {

        public String slideshow;

        public SlideshowScrollEntry(ScrollArea parent, @NotNull String slideshow, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, Component.literal(slideshow).setStyle(Style.EMPTY.withColor(TEXT_COLOR_GRAY_1.getRGB())), LISTING_DOT_BLUE, onClick);
            this.slideshow = slideshow;
        }

    }

}
