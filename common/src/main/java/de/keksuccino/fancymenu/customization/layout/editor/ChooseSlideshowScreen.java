package de.keksuccino.fancymenu.customization.layout.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class ChooseSlideshowScreen extends PiPWindowBody {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

    protected Consumer<String> callback;
    protected String selectedSlideshowName = null;
    protected ExternalTextureSlideshowRenderer selectedSlideshow = null;

    protected ScrollArea slideshowListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;

    public ChooseSlideshowScreen(@Nullable String preSelectedSlideshow, @NotNull Consumer<String> callback) {

        super(Component.translatable("fancymenu.slideshow.choose"));

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

    }

    @Override
    protected void init() {

        boolean blur = UIBase.shouldBlur();

        this.slideshowListScrollArea.setSetupForBlurInterface(blur);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.done"), (button) -> {
            this.closeWithResult(this.selectedSlideshowName);
        }).setIsActiveSupplier(consumes -> ChooseSlideshowScreen.this.selectedSlideshowName != null)
                .setUITooltipSupplier(consumes -> {
                    if (ChooseSlideshowScreen.this.selectedSlideshowName == null) {
                        return UITooltip.of(Component.translatable("fancymenu.slideshow.choose.no_slideshow_selected"));
                    }
                    return null;
                });
        this.addRenderableWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, blur);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.closeWithResult(null);
        });
        this.addRenderableWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, blur);

        this.addRenderableWidget(this.slideshowListScrollArea);

    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        graphics.drawString(this.font, Component.translatable("fancymenu.slideshow.choose.available_slideshows"), 20, 50, getGenericTextColor(), false);

        this.slideshowListScrollArea.setWidth(((float) this.width / 2) - 40, true);
        this.slideshowListScrollArea.setHeight(this.height - 85, true);
        this.slideshowListScrollArea.setX(20, true);
        this.slideshowListScrollArea.setY(50 + 15, true);

        if (this.selectedSlideshow != null) {
            Component previewLabel = Component.translatable("fancymenu.slideshow.choose.preview");
            int previewLabelWidth = this.font.width(previewLabel);
            graphics.drawString(this.font, previewLabel, this.width - 20 - previewLabelWidth, 50, getGenericTextColor(), false);

            int slideW = (this.width / 2) - 40;
            int slideH = this.height / 2;
            AspectRatio ratio = new AspectRatio(this.selectedSlideshow.getImageWidth(), this.selectedSlideshow.getImageHeight());
            int[] size = ratio.getAspectRatioSizeByMaximumSize(slideW, slideH);
            slideW = size[0];
            slideH = size[1];
            int slideX = this.width - 20 - slideW;
            int slideY = 50 + 15;
            graphics.fill(slideX, slideY, slideX + slideW, slideY + slideH, getAreaBackgroundColor());
            this.selectedSlideshow.x = slideX;
            this.selectedSlideshow.y = slideY;
            this.selectedSlideshow.width = slideW;
            this.selectedSlideshow.height = slideH;
            this.selectedSlideshow.render(graphics);
            UIBase.renderBorder(graphics, slideX, slideY, slideX + slideW, slideY + slideH, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.getUITheme().ui_interface_widget_border_color.getColor(), true, true, true, true);
        }

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.doneButton.getY() - 5 - 20);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics $$0, int $$1, int $$2, float $$3) {
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
            SlideshowScrollEntry e = new SlideshowScrollEntry(this.slideshowListScrollArea, s, getLabelTextColor(), (entry) -> {
                this.setSelectedSlideshow((SlideshowScrollEntry)entry);
            });
            this.slideshowListScrollArea.addEntry(e);
        }
        if (this.slideshowListScrollArea.getEntries().isEmpty()) {
            this.slideshowListScrollArea.addEntry(new TextScrollAreaEntry(this.slideshowListScrollArea, Component.translatable("fancymenu.slideshow.choose.no_slideshows"), (entry) -> {}));
        }
        float totalWidth = this.slideshowListScrollArea.getTotalEntryWidth();
        for (ScrollAreaEntry e : this.slideshowListScrollArea.getEntries()) {
            e.setWidth(totalWidth);
        }
    }

    @Override
    public boolean keyPressed(int button, int $$1, int $$2) {

        if (button == InputConstants.KEY_ENTER) {
            if (this.selectedSlideshowName != null) {
                this.closeWithResult(this.selectedSlideshowName);
                return true;
            }
        }

        return super.keyPressed(button, $$1, $$2);

    }

    protected void closeWithResult(@Nullable String slideshowName) {
        this.callback.accept(slideshowName);
        this.closeWindow();
    }

    private int getGenericTextColor() {
        return UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt()
                : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
    }

    private int getLabelTextColor() {
        return UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
    }

    private int getAreaBackgroundColor() {
        return UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ChooseSlideshowScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFocus(true)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ChooseSlideshowScreen screen) {
        return openInWindow(screen, null);
    }

    public static class SlideshowScrollEntry extends TextListScrollAreaEntry {

        public String slideshow;

        public SlideshowScrollEntry(ScrollArea parent, @NotNull String slideshow, int labelColor, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, Component.literal(slideshow).setStyle(Style.EMPTY.withColor(labelColor)), UIBase.getUITheme().bullet_list_dot_color_1, onClick);
            this.slideshow = slideshow;
        }

    }

}
