package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The dialogs that convert a variable to the other type: a separator field and other options at the top, a live preview of the result
 * below them, and Cancel and Done at the bottom.
 *
 * @param <T> The type of the result, which the callback gets on Done. It gets {@code null} when the user cancels.
 */
public abstract class ConvertVariableWindowBody<T> extends PiPWindowBody implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 480;
    public static final int PIP_WINDOW_HEIGHT = 340;
    protected static final int ROW_HEIGHT = 20;
    protected static final int ROW_GAP = 5;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_GAP = 10;
    private static final int PREVIEW_TOP_SPACER_HEIGHT = 3;

    @NotNull
    private final Consumer<T> callback;
    private boolean resultHandled = false;
    @NotNull
    protected String separator;
    protected ExtendedEditBox separatorBox;
    protected final ScrollArea previewScrollArea = new ScrollArea(0, 0, 0, 0);
    private final List<List<AbstractWidget>> optionRows = new ArrayList<>();
    private ExtendedButton cancelButton;
    private ExtendedButton doneButton;
    private float previewTop = 0.0F;
    private float previewBuiltForWidth = -1.0F;

    protected ConvertVariableWindowBody(@NotNull Component title, @NotNull String defaultSeparator, @NotNull Consumer<T> callback) {
        super(title);
        this.separator = Objects.requireNonNull(defaultSeparator);
        this.callback = Objects.requireNonNull(callback);
        this.previewScrollArea.makeEntriesWidthOfArea = true;
        this.previewScrollArea.makeAllEntriesWidthOfWidestEntry = false;
        this.previewScrollArea.horizontalScrollBar.active = false;
    }

    /**
     * Opens the dialog on top of everything else, like all FancyMenu dialogs.
     */
    public static void openInWindow(@NotNull ConvertVariableWindowBody<?> dialog) {
        Dialogs.openGeneric(dialog, dialog.getTitle(), null, PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT).getSecond().setIcon(MaterialIcons.SWAP_HORIZ);
    }

    /**
     * Adds the option rows below the separator field, see {@link #addOptionRow(AbstractWidget...)}.
     */
    protected abstract void initOptions();

    @NotNull
    protected abstract Component getSeparatorTooltip();

    /**
     * Fills the preview with rows that show the result.
     *
     * @param width The inner width of the preview area.
     */
    protected abstract void buildPreview(@NotNull ScrollArea area, float width);

    /**
     * @return Short info about the result, shown right-aligned above the preview, or {@code null} for none.
     */
    @Nullable
    protected abstract String getPreviewInfo();

    /**
     * @return The hint the empty preview shows.
     */
    @NotNull
    protected abstract Component getEmptyPreviewHint();

    @NotNull
    protected abstract T buildResult();

    /**
     * Recalculates the result and shows it in the preview. Gets called whenever an option changes.
     */
    protected void onOptionsChanged() {
        this.previewBuiltForWidth = -1.0F;
    }

    @Override
    protected void init() {

        boolean blur = UIBase.shouldBlur();
        this.optionRows.clear();
        this.previewScrollArea.setSetupForBlurInterface(blur);

        this.separatorBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 100, ROW_HEIGHT, Component.empty());
        this.separatorBox.setMaxLength(1000);
        this.separatorBox.setValue(this.separator);
        this.separatorBox.setResponder(value -> {
            this.separator = value;
            this.onOptionsChanged();
        });
        this.separatorBox.setUITooltip(() -> UITooltip.of(this.getSeparatorTooltip()));
        this.addOptionRow(this.separatorBox);
        this.setupInitialFocusWidget(this, this.separatorBox);

        this.initOptions();

        this.cancelButton = new ExtendedButton(0, 0, BUTTON_WIDTH, ROW_HEIGHT, Component.translatable("fancymenu.common_components.cancel"), button -> this.closeWithResult(null));
        this.doneButton = new ExtendedButton(0, 0, BUTTON_WIDTH, ROW_HEIGHT, Component.translatable("fancymenu.common_components.done"), button -> this.closeWithResult(this.buildResult()));
        for (ExtendedButton button : List.of(this.cancelButton, this.doneButton)) {
            button.setNavigatable(false);
            this.addRenderableWidget(button);
            UIBase.applyDefaultWidgetSkinTo(button, blur);
        }

        this.addRenderableWidget(this.previewScrollArea);
        this.updateLayout();
        this.previewBuiltForWidth = -1.0F;

    }

    /**
     * Adds a row of option widgets below the ones added before, which share the width of the dialog.
     */
    protected void addOptionRow(@NotNull AbstractWidget... widgets) {
        boolean blur = UIBase.shouldBlur();
        for (AbstractWidget widget : widgets) {
            UIBase.applyDefaultWidgetSkinTo(widget, blur);
            this.addRenderableWidget(widget);
        }
        this.optionRows.add(List.of(widgets));
    }

    private void updateLayout() {
        int contentWidth = this.width - (CONTENT_MARGIN * 2);
        int y = getLabeledContentTop();
        for (List<AbstractWidget> row : this.optionRows) {
            int widgetWidth = (contentWidth - ((row.size() - 1) * ROW_GAP)) / row.size();
            int x = CONTENT_MARGIN;
            for (AbstractWidget widget : row) {
                // Edit boxes draw their border inside their bounds, so they get the same inset as the search bars of the variable windows
                boolean editBox = widget instanceof ExtendedEditBox;
                widget.x = editBox ? (x + 1) : x;
                widget.y = editBox ? (y + 1) : y;
                widget.setWidth(editBox ? (widgetWidth - 2) : widgetWidth);
                if (editBox) ((IMixinAbstractWidget) widget).setHeightFancyMenu(ROW_HEIGHT - 2);
                x += widgetWidth + ROW_GAP;
            }
            y += ROW_HEIGHT + ROW_GAP;
        }
        float labelHeight = UIBase.getUITextHeightNormal() + UIBase.getAreaLabelVerticalPadding();
        this.previewTop = y - ROW_GAP + UIBase.getAreaLabelVerticalPadding() + labelHeight;
        int buttonY = this.height - CONTENT_MARGIN - ROW_HEIGHT;
        int buttonsX = (this.width - ((BUTTON_WIDTH * 2) + BUTTON_GAP)) / 2;
        this.cancelButton.setX(buttonsX);
        this.cancelButton.setY(buttonY);
        this.doneButton.setX(buttonsX + BUTTON_WIDTH + BUTTON_GAP);
        this.doneButton.setY(buttonY);
        this.previewScrollArea.setX(CONTENT_MARGIN, true);
        this.previewScrollArea.setY(this.previewTop, true);
        this.previewScrollArea.setWidth(contentWidth, true);
        this.previewScrollArea.setHeight(Math.max(30.0F, buttonY - BUTTON_GAP - this.previewTop), true);
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.performInitialWidgetFocusActionInRender();
        this.updateLayout();

        float width = this.previewScrollArea.getInnerWidth();
        if (width != this.previewBuiltForWidth) {
            this.previewBuiltForWidth = width;
            this.rebuildPreview(width);
        }

        int textColor = UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt() : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
        float labelPadding = UIBase.getUITextHeightNormal() + UIBase.getAreaLabelVerticalPadding();
        UIBase.renderText(graphics, Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert.separator"), CONTENT_MARGIN, getLabeledContentTop() - labelPadding, textColor);
        float previewLabelY = this.previewTop - labelPadding;
        UIBase.renderText(graphics, Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert.preview"), CONTENT_MARGIN, previewLabelY, textColor);
        String info = this.getPreviewInfo();
        if (info != null) {
            UIBase.renderText(graphics, info, this.width - CONTENT_MARGIN - UIBase.getUITextWidth(info), previewLabelY, VariableRowEntry.TextStyle.MUTED.getColor());
        }

    }

    @Override
    public void renderLateBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        for (ScrollAreaEntry entry : this.previewScrollArea.getEntries()) {
            if (!(entry instanceof CellScreen.SpacerScrollAreaEntry)) return;
        }
        ScrollArea area = this.previewScrollArea;
        Component hint = this.getEmptyPreviewHint();
        float hintX = area.getInnerX() + (area.getInnerWidth() / 2.0F) - (UIBase.getUITextWidthNormal(hint) / 2.0F);
        float hintY = area.getInnerY() + (area.getInnerHeight() / 2.0F) - (UIBase.getUITextHeightNormal() / 2.0F);
        UIBase.renderText(graphics, hint, hintX, hintY, VariableRowEntry.TextStyle.MUTED.getColor());
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    private void rebuildPreview(float width) {
        this.previewScrollArea.clearEntries();
        CellScreen.SpacerScrollAreaEntry spacer = new CellScreen.SpacerScrollAreaEntry(this.previewScrollArea, PREVIEW_TOP_SPACER_HEIGHT);
        spacer.setSelectable(false);
        spacer.setClickable(false);
        spacer.setBackgroundColorNormal(() -> DrawableColor.FULLY_TRANSPARENT);
        spacer.setBackgroundColorHover(() -> DrawableColor.FULLY_TRANSPARENT);
        this.previewScrollArea.addEntry(spacer);
        this.buildPreview(this.previewScrollArea, width);
    }

    /**
     * Makes a preview row look like a static line: no hover color, no selection and no clicks.
     */
    @NotNull
    protected static <E extends ScrollAreaEntry> E makeStatic(@NotNull E entry) {
        entry.setSelectable(false);
        entry.setClickable(false);
        entry.setPlayClickSound(false);
        entry.setBackgroundColorHover(entry.getBackgroundColorNormal());
        return entry;
    }

    /**
     * @return The height of preview rows, which is the height of the rows in the list editor and the variable list.
     */
    protected int getPreviewRowHeight() {
        return (int) (UIBase.getUITextHeightNormal() + 8);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == InputConstants.KEY_ENTER) || (keyCode == InputConstants.KEY_NUMPADENTER)) {
            this.closeWithResult(this.buildResult());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onWindowClosedExternally() {
        if (this.resultHandled) return;
        this.resultHandled = true;
        this.callback.accept(null);
    }

    private void closeWithResult(@Nullable T result) {
        if (this.resultHandled) return;
        this.resultHandled = true;
        this.callback.accept(result);
        this.closeWindow();
    }

}
