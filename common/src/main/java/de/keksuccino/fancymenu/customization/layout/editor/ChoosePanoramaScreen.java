package de.keksuccino.fancymenu.customization.layout.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class ChoosePanoramaScreen extends PiPWindowBody {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;
    private static final int LIST_ENTRY_TOP_DOWN_BORDER = 1;
    private static final int LIST_ENTRY_OUTER_PADDING = 3;
    private static final int LIST_TOP_SPACER_HEIGHT = 5;

    protected Consumer<String> callback;
    protected String selectedPanoramaName = null;
    protected LocalTexturePanoramaRenderer selectedPanorama = null;

    protected ScrollArea panoramaListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;

    public ChoosePanoramaScreen(@Nullable String preSelectedPanorama, @NotNull Consumer<String> callback) {

        super(Component.translatable("fancymenu.panorama.choose"));

        this.callback = callback;
        this.updatePanoramaScrollAreaContent();

        if (preSelectedPanorama != null) {
            for (ScrollAreaEntry e : this.panoramaListScrollArea.getEntries()) {
                if ((e instanceof PanoramaScrollEntry a) && a.panorama.equals(preSelectedPanorama)) {
                    a.setSelected(true);
                    this.setSelectedPanorama(a);
                    break;
                }
            }
        }

    }

    @Override
    protected void init() {

        boolean blur = UIBase.shouldBlur();

        this.panoramaListScrollArea.setSetupForBlurInterface(blur);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.done"), (button) -> {
            this.closeWithResult(this.selectedPanoramaName);
        }).setIsActiveSupplier(consumes -> ChoosePanoramaScreen.this.selectedPanoramaName != null)
                .setUITooltipSupplier(consumes -> {
                    if (ChoosePanoramaScreen.this.selectedPanoramaName == null) {
                        return UITooltip.of(Component.translatable("fancymenu.panorama.choose.no_panorama_selected"));
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

        this.addRenderableWidget(this.panoramaListScrollArea);

    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        float listAreaX = 20.0F;
        float listLabelY = 50.0F;
        float labelHeight = UIBase.getUITextHeightNormal();
        float labelPadding = UIBase.getAreaLabelVerticalPadding();
        float listAreaY = listLabelY + labelHeight + labelPadding;
        UIBase.renderText(graphics, Component.translatable("fancymenu.panorama.choose.available_panoramas"), listAreaX, listLabelY, getGenericTextColor());

        this.panoramaListScrollArea.setWidth(((float) this.width / 2) - 40, true);
        this.panoramaListScrollArea.setHeight(this.height - 85, true);
        this.panoramaListScrollArea.setX(listAreaX, true);
        this.panoramaListScrollArea.setY(listAreaY, true);

        if (this.selectedPanorama != null) {
            Component previewLabel = Component.translatable("fancymenu.panorama.choose.preview");
            float previewLabelWidth = UIBase.getUITextWidthNormal(previewLabel);
            float previewLabelX = this.width - 20 - previewLabelWidth;
            float previewLabelY = listAreaY - labelHeight - labelPadding;
            UIBase.renderText(graphics, previewLabel, previewLabelX, previewLabelY, getGenericTextColor());

            int previewW = (this.width / 2) - 40;
            int previewH = this.height / 2;
            int previewX = this.width - 20 - previewW;
            int previewY = (int) listAreaY;
            graphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, getAreaBackgroundColor());
            this.selectedPanorama.renderInArea(graphics, previewX, previewY, previewW, previewH, partial);
            UIBase.renderBorder(graphics, previewX, previewY, previewX + previewW, previewY + previewH, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.getUITheme().ui_interface_widget_border_color.getColor(), true, true, true, true);
        }

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.doneButton.getY() - 5 - 20);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics $$0, int $$1, int $$2, float $$3) {
    }

    protected void setSelectedPanorama(@Nullable ChoosePanoramaScreen.PanoramaScrollEntry entry) {
        if (entry == null) {
            this.selectedPanoramaName = null;
            this.selectedPanorama = null;
            return;
        }
        this.selectedPanoramaName = entry.panorama;
        this.selectedPanorama = PanoramaHandler.getPanorama(entry.panorama);
    }

    protected void updatePanoramaScrollAreaContent() {
        this.panoramaListScrollArea.clearEntries();
        CellScreen.SpacerScrollAreaEntry spacer = new CellScreen.SpacerScrollAreaEntry(this.panoramaListScrollArea, LIST_TOP_SPACER_HEIGHT);
        spacer.setSelectable(false);
        spacer.selectOnClick = false;
        spacer.setPlayClickSound(false);
        spacer.setBackgroundColorNormal(() -> DrawableColor.FULLY_TRANSPARENT);
        spacer.setBackgroundColorHover(() -> DrawableColor.FULLY_TRANSPARENT);
        this.panoramaListScrollArea.addEntry(spacer);
        boolean addedAny = false;
        for (String s : PanoramaHandler.getPanoramaNames()) {
            PanoramaScrollEntry e = new PanoramaScrollEntry(this.panoramaListScrollArea, s, getLabelTextColor(), (entry) -> {
                this.setSelectedPanorama((PanoramaScrollEntry)entry);
            });
            e.setHeight(this.getListEntryHeight());
            this.panoramaListScrollArea.addEntry(e);
            addedAny = true;
        }
        if (!addedAny) {
            TextScrollAreaEntry emptyEntry = new TextScrollAreaEntry(this.panoramaListScrollArea, Component.translatable("fancymenu.panorama.choose.no_panoramas"), (entry) -> {});
            emptyEntry.setHeight(this.getListEntryHeight());
            this.panoramaListScrollArea.addEntry(emptyEntry);
        }
        float totalWidth = this.panoramaListScrollArea.getTotalEntryWidth();
        for (ScrollAreaEntry e : this.panoramaListScrollArea.getEntries()) {
            e.setWidth(totalWidth);
        }
    }

    @Override
    public boolean keyPressed(int button, int $$1, int $$2) {

        if (button == InputConstants.KEY_ENTER) {
            if (this.selectedPanoramaName != null) {
                this.closeWithResult(this.selectedPanoramaName);
                return true;
            }
        }

        return super.keyPressed(button, $$1, $$2);

    }

    protected void closeWithResult(@Nullable String panoramaName) {
        this.callback.accept(panoramaName);
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

    private int getListEntryHeight() {
        return (int)(UIBase.getUITextHeightNormal()
                + (LIST_ENTRY_TOP_DOWN_BORDER * 2)
                + (LIST_ENTRY_OUTER_PADDING * 2));
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ChoosePanoramaScreen screen, @Nullable PiPWindow parentWindow) {
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

    public static @NotNull PiPWindow openInWindow(@NotNull ChoosePanoramaScreen screen) {
        return openInWindow(screen, null);
    }

    public static class PanoramaScrollEntry extends TextScrollAreaEntry {

        public String panorama;

        public PanoramaScrollEntry(ScrollArea parent, @NotNull String panorama, int labelColor, @NotNull Consumer<TextScrollAreaEntry> onClick) {
            super(parent, Component.literal(panorama), onClick);
            this.setTextBaseColor(labelColor);
            this.panorama = panorama;
        }

    }

}
