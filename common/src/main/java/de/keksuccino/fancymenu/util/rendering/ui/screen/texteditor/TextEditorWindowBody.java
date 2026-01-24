package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.formattingrules.TextEditorFormattingRules;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class TextEditorWindowBody extends PiPWindowBody {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final HashMap<String, String> COMPILED_SINGLE_LINE_STRINGS = new HashMap<>();

    public static final String NEWLINE_CODE = "%!n!%";
    public static final String SPACE_CODE = "%!s!%";
    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

    protected final CharacterFilter characterFilter;
    protected final Consumer<String> callback;
    protected List<TextEditorLine> textFieldLines = new ArrayList<>();
    protected ScrollBar verticalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, UIBase.VERTICAL_SCROLL_BAR_WIDTH, UIBase.VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
    protected ScrollBar horizontalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, UIBase.HORIZONTAL_SCROLL_BAR_WIDTH, UIBase.HORIZONTAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
    protected ScrollBar verticalScrollBarPlaceholderMenu = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, UIBase.VERTICAL_SCROLL_BAR_WIDTH, UIBase.VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
    protected ScrollBar horizontalScrollBarPlaceholderMenu = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, UIBase.HORIZONTAL_SCROLL_BAR_WIDTH, UIBase.HORIZONTAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
    protected ContextMenu rightClickContextMenu;
    protected ExtendedButton cancelButton;
    protected ExtendedButton doneButton;
    protected ExtendedButton placeholderButton;
    protected int lastCursorPosSetByUser = 0;
    protected boolean justSwitchedLineByWordDeletion = false;
    protected boolean triggeredFocusedLineWasTooHighInCursorPosMethod = false;
    protected int headerHeight = 50;
    protected int footerHeight = 50;
    protected int borderLeft = 40;
    protected int borderRight = 20;
    protected int lineHeight = 14;
    protected int lineNumberSidebarGapLeft = 4;
    protected int lineNumberSidebarGapRight = 4;
    protected Supplier<DrawableColor> areaBackgroundColor = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_area_background_color_type_1;
        return UIBase.getUITheme().ui_interface_area_background_color_type_1;
    };
    protected Supplier<DrawableColor> areaBorderColor = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_area_border_color;
        return UIBase.getUITheme().ui_interface_widget_border_color;
    };
    protected Supplier<DrawableColor> textColor = () -> UIBase.getUITheme().text_editor_text_color;
    protected Supplier<DrawableColor> focusedLineColor = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_area_entry_selected_color;
        return UIBase.getUITheme().ui_interface_area_entry_selected_color;
    };
    protected Supplier<DrawableColor> scrollGrabberIdleColor = () -> UIBase.getUITheme().scroll_grabber_color_normal;
    protected Supplier<DrawableColor> scrollGrabberHoverColor = () -> UIBase.getUITheme().scroll_grabber_color_hover;
    protected Supplier<DrawableColor> lineNumberSideBarColor = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_area_background_color_type_2;
        return UIBase.getUITheme().ui_blur_interface_area_background_color_type_2;
    };
    protected Supplier<DrawableColor> lineNumberTextColorNormal = () -> UIBase.getUITheme().text_editor_line_number_text_color_normal;
    protected Supplier<DrawableColor> lineNumberTextColorFocused = () -> UIBase.getUITheme().text_editor_line_number_text_color_selected;
    protected Supplier<DrawableColor> placeholderEntryBackgroundColorIdle = () -> {
        if (UIBase.shouldBlur()) return DrawableColor.FULLY_TRANSPARENT;
        return UIBase.getUITheme().ui_interface_area_background_color_type_1;
    };
    protected Supplier<DrawableColor> placeholderEntryBackgroundColorHover = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_area_entry_selected_color;
        return UIBase.getUITheme().ui_interface_area_entry_selected_color;
    };
    protected Supplier<DrawableColor> placeholderEntryDotColorPlaceholder = () -> UIBase.getUITheme().bullet_list_dot_color_1;
    protected Supplier<DrawableColor> placeholderEntryDotColorCategory = () -> UIBase.getUITheme().bullet_list_dot_color_2;
    protected Supplier<DrawableColor> placeholderEntryLabelColor = () -> {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_widget_label_color_normal;
        return UIBase.getUITheme().ui_interface_widget_label_color_normal;
    };
    protected Supplier<DrawableColor> placeholderEntryBackToCategoriesLabelColor = () -> UIBase.getUITheme().warning_text_color;
    protected int currentLineWidth;
    protected int lastTickFocusedLineIndex = -1;
    protected TextEditorLine startHighlightLine = null;
    protected int startHighlightLineIndex = -1;
    protected int endHighlightLineIndex = -1;
    protected int overriddenTotalScrollHeight = -1;
    protected List<Runnable> lineNumberRenderQueue = new ArrayList<>();
    public List<TextEditorFormattingRule> formattingRules = new ArrayList<>();
    protected int currentRenderCharacterIndexTotal = 0;
    /** This is to make different instances of the editor remember the state of the placeholder menu **/
    protected static boolean extendedPlaceholderMenu = false;
    protected int placeholderMenuWidth = 120;
    protected int placeholderMenuEntryHeight = 16;
    protected List<PlaceholderMenuEntry> placeholderMenuEntries = new ArrayList<>();
    protected boolean multilineMode = true;
    protected boolean allowPlaceholders = true;
    protected boolean boldTitle = true;
    protected ConsumingSupplier<TextEditorWindowBody, Boolean> textValidator = null;
    protected UITooltip textValidatorFeedbackUITooltip = null;
    protected boolean selectedHoveredOnRightClickMenuOpen = false;
    protected final TextEditorHistory history = new TextEditorHistory(this);
    protected ExtendedEditBox searchBar;
    protected ExtendedEditBox goToLineField;
    protected boolean isGoToLineOpen = false;

    private static final Comparator<Placeholder> PLACEHOLDER_DISPLAY_NAME_COMPARATOR = Comparator
            .comparing(Placeholder::getDisplayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Placeholder::getDisplayName)
            .thenComparing(Placeholder::getIdentifier);
    private static final Comparator<String> PLACEHOLDER_CATEGORY_COMPARATOR = Comparator
            .comparing((String category) -> category, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(category -> category);

    protected IndentationGuideRenderer indentGuideRenderer;
    protected boolean showIndentationGuides = true;

    @NotNull
    public static TextEditorWindowBody build(@Nullable Component title, @Nullable CharacterFilter characterFilter, @NotNull Consumer<String> callback) {
        return new TextEditorWindowBody(title, characterFilter, callback);
    }

    public TextEditorWindowBody(@Nullable CharacterFilter characterFilter, @NotNull Consumer<String> callback) {
        this(null, characterFilter, callback);
    }

    public TextEditorWindowBody(@Nullable Component title, @Nullable CharacterFilter characterFilter, @NotNull Consumer<String> callback) {
        super((title != null) ? title : Component.literal(""));
        this.minecraft = Minecraft.getInstance();
        this.font = Minecraft.getInstance().font;
        this.characterFilter = characterFilter;
        this.callback = callback;
        this.addLine();
        this.getLine(0).setFocused(true);
        this.verticalScrollBar.setScrollWheelAllowed(true);
        this.verticalScrollBarPlaceholderMenu.setScrollWheelAllowed(true);
        this.formattingRules.addAll(TextEditorFormattingRules.getRules());
        this.indentGuideRenderer = new IndentationGuideRenderer(this);
        this.updateCurrentLineWidth();
    }

    @Override
    public void init() {

        this.placeholderMenuWidth = Math.min(300, Math.max(120, (int)((double)this.width / 3.5D)));

        this.updateRightClickContextMenu();

        this.verticalScrollBar.scrollAreaStartX = this.getEditorAreaX() + 1;
        this.verticalScrollBar.scrollAreaStartY = this.getEditorAreaY() + 1;
        this.verticalScrollBar.scrollAreaEndX = this.getEditorAreaX() + this.getEditorAreaWidth() - 2;
        this.verticalScrollBar.scrollAreaEndY = this.getEditorAreaY() + this.getEditorAreaHeight() - this.horizontalScrollBar.grabberHeight - 2;

        this.horizontalScrollBar.scrollAreaStartX = this.getEditorAreaX() + 1;
        this.horizontalScrollBar.scrollAreaStartY = this.getEditorAreaY() + 1;
        this.horizontalScrollBar.scrollAreaEndX = this.getEditorAreaX() + this.getEditorAreaWidth() - this.verticalScrollBar.grabberWidth - 2;
        this.horizontalScrollBar.scrollAreaEndY = this.getEditorAreaY() + this.getEditorAreaHeight() - 1;

        int placeholderSearchBarY = this.getPlaceholderAreaY() - 25;

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, this.getPlaceholderAreaX(), placeholderSearchBarY, this.getPlaceholderAreaWidth(), 20 - 2, Component.empty());
        this.searchBar.setHintFancyMenu(consumes -> Component.translatable("fancymenu.placeholders.text_editor.search_placeholder"));
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updatePlaceholdersList());
        this.searchBar.setIsVisibleSupplier(consumes -> extendedPlaceholderMenu && this.allowPlaceholders);
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar, UIBase.shouldBlur());

        this.goToLineField = new ExtendedEditBox(Minecraft.getInstance().font, this.getEditorAreaX() + this.getEditorAreaWidth() - 150 - 20, this.getEditorAreaY() + 5, 150, 20, Component.literal(""));
        this.goToLineField.setHintFancyMenu(consumes -> Component.translatable("fancymenu.editor.shortcuts.go_to_line"));
        this.goToLineField.setIsVisibleSupplier(consumes -> this.isGoToLineOpen);
        this.goToLineField.setCharacterFilter(de.keksuccino.fancymenu.util.input.CharacterFilter.buildIntegerFilter());
        this.addRenderableWidget(this.goToLineField);
        UIBase.applyDefaultWidgetSkinTo(this.goToLineField, UIBase.shouldBlur());

        this.verticalScrollBarPlaceholderMenu.scrollAreaStartX = this.getPlaceholderAreaX() + 1;
        this.verticalScrollBarPlaceholderMenu.scrollAreaStartY = this.getPlaceholderAreaY() + 1;
        this.verticalScrollBarPlaceholderMenu.scrollAreaEndX = this.getPlaceholderAreaX() + this.getPlaceholderAreaWidth() - 2;
        this.verticalScrollBarPlaceholderMenu.scrollAreaEndY = this.getPlaceholderAreaY() + this.getPlaceholderAreaHeight() - this.horizontalScrollBarPlaceholderMenu.grabberHeight - 2;

        this.horizontalScrollBarPlaceholderMenu.scrollAreaStartX = this.getPlaceholderAreaX() + 1;
        this.horizontalScrollBarPlaceholderMenu.scrollAreaStartY = this.getPlaceholderAreaY() + 1;
        this.horizontalScrollBarPlaceholderMenu.scrollAreaEndX = this.getPlaceholderAreaX() + this.getPlaceholderAreaWidth() - this.verticalScrollBarPlaceholderMenu.grabberWidth - 2;
        this.horizontalScrollBarPlaceholderMenu.scrollAreaEndY = this.getPlaceholderAreaY() + this.getPlaceholderAreaHeight() - 1;

        //Set scroll grabber colors
        this.verticalScrollBar.idleBarColor = this.scrollGrabberIdleColor;
        this.verticalScrollBar.hoverBarColor = this.scrollGrabberHoverColor;
        this.horizontalScrollBar.idleBarColor = this.scrollGrabberIdleColor;
        this.horizontalScrollBar.hoverBarColor = this.scrollGrabberHoverColor;
        this.verticalScrollBar.setRoundedGrabberEnabled(true);
        this.horizontalScrollBar.setRoundedGrabberEnabled(true);

        //Set placeholder menu scroll bar colors
        this.verticalScrollBarPlaceholderMenu.idleBarColor = this.scrollGrabberIdleColor;
        this.verticalScrollBarPlaceholderMenu.hoverBarColor = this.scrollGrabberHoverColor;
        this.horizontalScrollBarPlaceholderMenu.idleBarColor = this.scrollGrabberIdleColor;
        this.horizontalScrollBarPlaceholderMenu.hoverBarColor = this.scrollGrabberHoverColor;
        this.verticalScrollBarPlaceholderMenu.setRoundedGrabberEnabled(true);
        this.horizontalScrollBarPlaceholderMenu.setRoundedGrabberEnabled(true);

        this.addWidget(this.verticalScrollBar);
        this.addWidget(this.horizontalScrollBar);
        this.addWidget(this.verticalScrollBarPlaceholderMenu);
        this.addWidget(this.horizontalScrollBarPlaceholderMenu);

        this.cancelButton = new ExtendedButton(this.width - this.borderRight - 100 - 5 - 100, this.height - 35, 100, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.callback.accept(null);
            this.closeWindow();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());

        this.doneButton = new ExtendedButton(this.width - this.borderRight - 100, this.height - 35, 100, 20, Component.translatable("fancymenu.common_components.done"), (button) -> {
            if (this.isTextValid()) {
                this.callback.accept(this.getText());
                this.closeWindow();
            }
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, UIBase.shouldBlur());

        if (this.allowPlaceholders) {
            MutableComponent placeholderButtonLabel = Component.translatable("fancymenu.ui.text_editor.placeholders");
            if (extendedPlaceholderMenu) {
                placeholderButtonLabel = placeholderButtonLabel.withStyle(Style.EMPTY.withUnderlined(true));
            }
            this.placeholderButton = new ExtendedButton(this.width - this.borderRight - 100, (this.headerHeight / 2) - 10, 100, 20, placeholderButtonLabel, (button) -> {
                extendedPlaceholderMenu = !extendedPlaceholderMenu;
                this.rebuildWidgets();
            }).setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.placeholders.desc")));
            this.addWidget(this.placeholderButton);
            UIBase.applyDefaultWidgetSkinTo(this.placeholderButton, UIBase.shouldBlur());
        } else {
            this.placeholderButton = null;
            extendedPlaceholderMenu = false;
        }

        this.updatePlaceholdersList();

    }

    public void updateRightClickContextMenu() {

        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
        }
        this.rightClickContextMenu = new ContextMenu();

        this.rightClickContextMenu.addClickableEntry("copy", Component.translatable("fancymenu.ui.text_editor.copy"), (menu, entry) -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlightedText());
                    menu.closeMenu();
                }).setIsActiveSupplier((menu, entry) -> {
                    if (!menu.isOpen()) return false;
                    return this.selectedHoveredOnRightClickMenuOpen;
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.copy"))
                .setIcon(MaterialIcons.CONTENT_COPY);

        this.rightClickContextMenu.addClickableEntry("paste", Component.translatable("fancymenu.ui.text_editor.paste"), (menu, entry) -> {
                    this.pasteText(Minecraft.getInstance().keyboardHandler.getClipboard());
                    menu.closeMenu();
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.paste"))
                .setIcon(MaterialIcons.CONTENT_PASTE);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_paste");

        this.rightClickContextMenu.addClickableEntry("cut", Component.translatable("fancymenu.ui.text_editor.cut"), (menu, entry) -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.cutHighlightedText());
                    menu.closeMenu();
                }).setIsActiveSupplier((menu, entry) -> {
                    if (!menu.isOpen()) return false;
                    return this.selectedHoveredOnRightClickMenuOpen;
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.cut"))
                .setIcon(MaterialIcons.CONTENT_CUT);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_cut");

        this.rightClickContextMenu.addClickableEntry("select_all", Component.translatable("fancymenu.ui.text_editor.select_all"), (menu, entry) -> {
                    for (TextEditorLine t : this.textFieldLines) {
                        t.setHighlightPos(0);
                        t.setCursorPosition(t.getValue().length());
                    }
                    this.setFocusedLine(this.getLineCount()-1);
                    this.startHighlightLineIndex = 0;
                    this.endHighlightLineIndex = this.getLineCount()-1;
                    menu.closeMenu();
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.select_all"))
                .setIcon(MaterialIcons.SELECT_ALL);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_select_all");

        this.rightClickContextMenu.addClickableEntry("undo", Component.translatable("fancymenu.editor.edit.undo"), (menu, entry) -> {
                    this.history.stepBack();
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.undo"))
                .setIcon(MaterialIcons.UNDO);

        this.rightClickContextMenu.addClickableEntry("redo", Component.translatable("fancymenu.editor.edit.redo"), (menu, entry) -> {
                    this.history.stepForward();
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.redo"))
                .setIcon(MaterialIcons.REDO);

    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        //Reset scrolls if content fits editor area
        if (this.currentLineWidth <= this.getEditorAreaWidth()) {
            this.horizontalScrollBar.setScroll(0.0F);
        }
        if (this.getTotalLineHeight() <= this.getEditorAreaHeight()) {
            this.verticalScrollBar.setScroll(0.0F);
        }

        this.justSwitchedLineByWordDeletion = false;

        this.updateCurrentLineWidth();

        //Adjust the scroll wheel speed depending on the amount of lines
        this.verticalScrollBar.setWheelScrollSpeed(1.0F / ((float)this.getTotalScrollHeight() / 500.0F));

        this.renderLineNumberBackground(graphics, partial);

        this.renderEditorAreaBackground(graphics, partial);

        // Render indentation guides if enabled
        if (this.showIndentationGuides) {
            this.indentGuideRenderer.render(graphics);
        }

        //Don't render parts of lines outside editor area
        graphics.enableScissor(this.getEditorAreaX(), this.getEditorAreaY(), this.getEditorAreaX() + this.getEditorAreaWidth(), this.getEditorAreaY() + this.getEditorAreaHeight());

        this.formattingRules.forEach((rule) -> rule.resetRule(this));
        this.currentRenderCharacterIndexTotal = 0;
        this.lineNumberRenderQueue.clear();
        //Update positions and size of lines and render them
        this.updateLines((line) -> {
            if (line.isInEditorArea()) {
                this.lineNumberRenderQueue.add(() -> this.renderLineNumber(graphics, line));
            }
            line.render(graphics, mouseX, mouseY, partial);
        });

        graphics.disableScissor();

        //Don't render line numbers outside the line number area
        int lineNumberMinX = this.getLineNumberSidebarX();
        int lineNumberMaxX = this.getLineNumberSidebarRight();
        graphics.enableScissor(lineNumberMinX, this.getEditorAreaY() + 2, lineNumberMaxX, this.getEditorAreaY() + this.getEditorAreaHeight() - 2);

        for (Runnable r : this.lineNumberRenderQueue) {
            r.run();
        }

        graphics.disableScissor();

        this.lastTickFocusedLineIndex = this.getFocusedLineIndex();
        this.triggeredFocusedLineWasTooHighInCursorPosMethod = false;

        this.renderEditorAreaBorder(graphics, partial);

        this.verticalScrollBar.render(graphics, mouseX, mouseY, partial);
        this.horizontalScrollBar.render(graphics, mouseX, mouseY, partial);

        this.renderPlaceholderMenu(graphics, mouseX, mouseY, partial);

        this.cancelButton.render(graphics, mouseX, mouseY, partial);

        this.doneButton.active = this.isTextValid();
        this.doneButton.setUITooltip(this.textValidatorFeedbackUITooltip);
        this.doneButton.render(graphics, mouseX, mouseY, partial);

        this.renderMultilineNotSupportedNotification(graphics, mouseX, mouseY, partial);

        this.tickMouseHighlighting();

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    protected void renderMultilineNotSupportedNotification(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (!this.multilineMode) {
            MutableComponent indicator = Component.translatable("fancymenu.editor.text_editor.single_line_warning.indicator").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt())).append(Component.literal(" [?]").withStyle(Style.EMPTY.withBold(true).withColor(UIBase.getUITheme().warning_text_color.getColorInt())));
            int indicatorX = this.getEditorAreaX();
            int indicatorY = this.getEditorAreaY() - this.font.lineHeight - 5;
            int indicatorWidth = this.font.width(indicator);
            graphics.drawString(this.font, indicator, indicatorX, indicatorY, -1, false);
            if (UIBase.isXYInArea(mouseX, mouseY, indicatorX, indicatorY, indicatorWidth, this.font.lineHeight)) {
                TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(Component.translatable("fancymenu.editor.text_editor.single_line_warning").withColor(UIBase.getUITheme().error_text_color.getColorInt())), () -> true);
            }
        }
    }

    protected void renderPlaceholderMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (extendedPlaceholderMenu) {

            if (this.getTotalPlaceholderEntriesWidth() <= this.getPlaceholderAreaWidth()) {
                this.horizontalScrollBarPlaceholderMenu.setScroll(0.0F);
            }
            if (this.getTotalPlaceholderEntriesHeight() <= this.getPlaceholderAreaHeight()) {
                this.verticalScrollBarPlaceholderMenu.setScroll(0.0F);
            }

            //Render placeholder menu background
            float placeholderAreaRadius = UIBase.getInterfaceCornerRoundingRadius();
            SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, this.width - this.borderRight - this.getPlaceholderAreaWidth(), this.getPlaceholderAreaY(), this.getPlaceholderAreaWidth(), this.getPlaceholderAreaHeight(), placeholderAreaRadius, placeholderAreaRadius, placeholderAreaRadius, placeholderAreaRadius, this.areaBackgroundColor.get().getColorInt(), partial);

            //Don't render parts of placeholder entries outside of placeholder menu area
            graphics.enableScissor(this.width - this.borderRight - this.getPlaceholderAreaWidth(), this.getPlaceholderAreaY() + 2, this.width - this.borderRight, this.getPlaceholderAreaY() + this.getPlaceholderAreaHeight() - 2);

            //Render placeholder entries
            List<PlaceholderMenuEntry> entries = new ArrayList<>(this.placeholderMenuEntries);
            int index = 0;
            for (PlaceholderMenuEntry e : entries) {
                e.x = (this.width - this.borderRight - this.getPlaceholderAreaWidth()) + this.getPlaceholderEntriesRenderOffsetX();
                e.y = (this.getPlaceholderAreaY()) + (this.placeholderMenuEntryHeight * index) + this.getPlaceholderEntriesRenderOffsetY();
                e.render(graphics, mouseX, mouseY, partial);
                index++;
            }

            graphics.disableScissor();

            //Render placeholder menu border
            SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(graphics, this.width - this.borderRight - this.getPlaceholderAreaWidth() - 1, this.getPlaceholderAreaY() - 1, this.getPlaceholderAreaWidth() + 2, this.getPlaceholderAreaHeight() + 2, 1.0F, placeholderAreaRadius, placeholderAreaRadius, placeholderAreaRadius, placeholderAreaRadius, this.areaBorderColor.get().getColorInt(), partial);

            //Render placeholder menu scroll bars
            this.verticalScrollBarPlaceholderMenu.render(graphics, mouseX, mouseY, partial);
            this.horizontalScrollBarPlaceholderMenu.render(graphics, mouseX, mouseY, partial);

        }

        if (this.placeholderButton != null) {
            this.placeholderButton.render(graphics, mouseX, mouseY, partial);
        }

    }

    public int getPlaceholderAreaX() {
        return this.width - this.borderRight - this.getPlaceholderAreaWidth();
    }

    public int getPlaceholderAreaY() {
        return this.getEditorAreaY() + 25;
    }

    public int getPlaceholderAreaHeight() {
        return this.getEditorAreaHeight() - 25;
    }

    public int getPlaceholderAreaWidth() {
        return this.placeholderMenuWidth;
    }

    public int getTotalPlaceholderEntriesHeight() {
        return this.placeholderMenuEntryHeight * this.placeholderMenuEntries.size();
    }

    public int getTotalPlaceholderEntriesWidth() {
        int i = this.getPlaceholderAreaWidth();
        for (PlaceholderMenuEntry e : this.placeholderMenuEntries) {
            if (e.getWidth() > i) {
                i = e.getWidth();
            }
        }
        return i;
    }

    public int getPlaceholderEntriesRenderOffsetX() {
        int totalScrollWidth = Math.max(0, this.getTotalPlaceholderEntriesWidth() - this.getPlaceholderAreaWidth());
        return -(int)(((float)totalScrollWidth / 100.0F) * (this.horizontalScrollBarPlaceholderMenu.getScroll() * 100.0F));
    }

    public int getPlaceholderEntriesRenderOffsetY() {
        int totalScrollHeight = Math.max(0, this.getTotalPlaceholderEntriesHeight() - (this.getPlaceholderAreaHeight()));
        return -(int)(((float)totalScrollHeight / 100.0F) * (this.verticalScrollBarPlaceholderMenu.getScroll() * 100.0F));
    }

    protected boolean placeholderFitsSearchValue(@NotNull Placeholder placeholder, @Nullable String s) {
        if ((s == null) || s.isBlank()) return true;
        s = s.toLowerCase();
        if (placeholder.getDisplayName().toLowerCase().contains(s)) return true;
        if (placeholder.getIdentifier().toLowerCase().contains(s)) return true;
        return this.placeholderDescriptionContains(placeholder, s);
    }

    protected boolean placeholderDescriptionContains(@NotNull Placeholder placeholder, @NotNull String s) {
        List<String> desc = Objects.requireNonNullElse(placeholder.getDescription(), new ArrayList<>());
        for (String line : desc) {
            if (line.toLowerCase().contains(s)) return true;
        }
        return false;
    }

    public void updatePlaceholderEntries(@Nullable String category, boolean clearList, boolean addBackButton) {

        String searchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        if (searchValue.isBlank()) searchValue = null;

        if (clearList || (searchValue != null)) {
            this.placeholderMenuEntries.clear();
        }

        if (searchValue != null) {
            List<Placeholder> placeholders = PlaceholderRegistry.getPlaceholders();
            placeholders.sort(PLACEHOLDER_DISPLAY_NAME_COMPARATOR);
            for (Placeholder p : placeholders) {
                if (!this.placeholderFitsSearchValue(p, searchValue)) continue;
                PlaceholderMenuEntry entry = new PlaceholderMenuEntry(this, Component.literal(p.getDisplayName()), () -> {
                    this.history.saveSnapshot();
                    this.pasteText(p.getDefaultPlaceholderString().toString());
                });
                List<String> desc = p.getDescription();
                if (desc != null) {
                    entry.setDescription(desc.toArray(new String[0]));
                }
                entry.dotColor = this.placeholderEntryDotColorPlaceholder.get().getColor();
                entry.entryLabelColor = this.placeholderEntryLabelColor.get().getColor();
                this.placeholderMenuEntries.add(entry);
            }
            for (PlaceholderMenuEntry e : this.placeholderMenuEntries) {
                e.backgroundColorIdle = this.placeholderEntryBackgroundColorIdle.get().getColor();
                e.backgroundColorHover = this.placeholderEntryBackgroundColorHover.get().getColor();
            }
            this.verticalScrollBarPlaceholderMenu.setScroll(0.0F);
            this.horizontalScrollBarPlaceholderMenu.setScroll(0.0F);
            return;
        }

        Map<String, List<Placeholder>> categories = this.getPlaceholdersOrderedByCategories();
        if (!categories.isEmpty()) {
            List<Placeholder> otherCategory = categories.get(I18n.get("fancymenu.requirements.categories.other"));
            if (otherCategory != null) {

                if (category == null) {

                    //Add category entries
                    for (Map.Entry<String, List<Placeholder>> m : categories.entrySet()) {
                        if (m.getValue() != otherCategory) {
                            PlaceholderMenuEntry entry = new PlaceholderMenuEntry(this, Component.literal(m.getKey()), () -> {
                                this.updatePlaceholderEntries(m.getKey(), true, true);
                            });
                            entry.dotColor = this.placeholderEntryDotColorCategory.get().getColor();
                            entry.entryLabelColor = this.placeholderEntryLabelColor.get().getColor();
                            this.placeholderMenuEntries.add(entry);
                        }
                    }
                    //Add placeholder entries of the "Other" category to the end of the categories list (because other = no category)
                    this.updatePlaceholderEntries(I18n.get("fancymenu.requirements.categories.other"), false, false);

                } else {

                    if (addBackButton) {
                        PlaceholderMenuEntry backToCategoriesEntry = new PlaceholderMenuEntry(this, Component.literal(I18n.get("fancymenu.ui.text_editor.placeholders.back_to_categories")), () -> {
                            this.updatePlaceholderEntries(null, true, true);
                        });
                        backToCategoriesEntry.dotColor = this.placeholderEntryDotColorCategory.get().getColor();
                        backToCategoriesEntry.entryLabelColor = this.placeholderEntryBackToCategoriesLabelColor.get().getColor();
                        this.placeholderMenuEntries.add(backToCategoriesEntry);
                    }

                    List<Placeholder> placeholders = categories.get(category);
                    if (placeholders != null) {
                        for (Placeholder p : placeholders) {
                            PlaceholderMenuEntry entry = new PlaceholderMenuEntry(this, Component.literal(p.getDisplayName()), () -> {
                                this.history.saveSnapshot();
                                this.pasteText(p.getDefaultPlaceholderString().toString());
                            });
                            List<String> desc = p.getDescription();
                            if (desc != null) {
                                entry.setDescription(desc.toArray(new String[0]));
                            }
                            entry.dotColor = this.placeholderEntryDotColorPlaceholder.get().getColor();
                            entry.entryLabelColor = this.placeholderEntryLabelColor.get().getColor();
                            this.placeholderMenuEntries.add(entry);
                        }
                    }

                }

                for (PlaceholderMenuEntry e : this.placeholderMenuEntries) {
                    e.backgroundColorIdle = this.placeholderEntryBackgroundColorIdle.get().getColor();
                    e.backgroundColorHover = this.placeholderEntryBackgroundColorHover.get().getColor();
                }

                this.verticalScrollBarPlaceholderMenu.setScroll(0.0F);
                this.horizontalScrollBarPlaceholderMenu.setScroll(0.0F);
            }
        }

    }

    protected void updatePlaceholdersList() {
        this.updatePlaceholderEntries(null, true, false);
    }

    protected Map<String, List<Placeholder>> getPlaceholdersOrderedByCategories() {
        //Build lists of all placeholders ordered by categories
        Map<String, List<Placeholder>> categories = new LinkedHashMap<>();
        for (Placeholder p : PlaceholderRegistry.getPlaceholders()) {
            if (!p.shouldShowUpInPlaceholderMenu(LayoutEditorScreen.getCurrentInstance())) continue;
            String cat = p.getCategory();
            if (cat == null) {
                cat = I18n.get("fancymenu.requirements.categories.other");
            }
            List<Placeholder> l = categories.computeIfAbsent(cat, k -> new ArrayList<>());
            l.add(p);
        }
        categories.values().forEach(list -> list.sort(PLACEHOLDER_DISPLAY_NAME_COMPARATOR));
        String otherKey = I18n.get("fancymenu.requirements.categories.other");
        List<String> sortedKeys = new ArrayList<>(categories.keySet());
        boolean hasOther = sortedKeys.remove(otherKey);
        sortedKeys.sort(PLACEHOLDER_CATEGORY_COMPARATOR);
        Map<String, List<Placeholder>> sortedCategories = new LinkedHashMap<>();
        for (String key : sortedKeys) {
            sortedCategories.put(key, categories.get(key));
        }
        if (hasOther) {
            sortedCategories.put(otherKey, categories.get(otherKey));
        }
        return sortedCategories;
    }

    protected void renderLineNumberBackground(GuiGraphics graphics, float partial) {
        int width = this.getLineNumberSidebarWidth();
        if (width <= 0) {
            return;
        }
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        float x = this.getLineNumberSidebarX();
        float y = this.getEditorAreaY();
        int height = this.getEditorAreaHeight();
        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, x, y, width, height, radius, radius, radius, radius, this.lineNumberSideBarColor.get().getColorInt(), partial);
        SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(graphics, x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 1.0F, radius, radius, radius, radius, this.areaBorderColor.get().getColorInt(), partial);
    }

    protected void renderLineNumber(GuiGraphics graphics, TextEditorLine line) {
        String lineNumberString = "" + (line.lineIndex+1);
        int lineNumberWidth = this.font.width(lineNumberString);
        int lineNumberX = this.getLineNumberSidebarRight() - 3 - lineNumberWidth;
        graphics.drawString(this.font, lineNumberString, lineNumberX, line.getY() + (line.getHeight() / 2) - (this.font.lineHeight / 2), line.isFocused() ? this.lineNumberTextColorFocused.get().getColorInt() : this.lineNumberTextColorNormal.get().getColorInt(), false);
    }

    protected int getLineNumberSidebarX() {
        return Math.max(0, this.lineNumberSidebarGapLeft);
    }

    protected int getLineNumberSidebarWidth() {
        return Math.max(0, this.borderLeft - this.lineNumberSidebarGapLeft - this.lineNumberSidebarGapRight);
    }

    protected int getLineNumberSidebarRight() {
        return this.getLineNumberSidebarX() + this.getLineNumberSidebarWidth();
    }

    protected void renderEditorAreaBackground(GuiGraphics graphics, float partial) {
        float editorAreaRadius = UIBase.getInterfaceCornerRoundingRadius();
        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, this.getEditorAreaX(), this.getEditorAreaY(), this.getEditorAreaWidth(), this.getEditorAreaHeight(), editorAreaRadius, editorAreaRadius, editorAreaRadius, editorAreaRadius, this.areaBackgroundColor.get().getColorInt(), partial);
    }

    protected void renderEditorAreaBorder(GuiGraphics graphics, float partial) {
        float editorAreaRadius = UIBase.getInterfaceCornerRoundingRadius();
        SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(graphics, this.getEditorAreaX() - 1, this.getEditorAreaY() - 1, this.getEditorAreaWidth() + 2, this.getEditorAreaHeight() + 2, 1.0F, editorAreaRadius, editorAreaRadius, editorAreaRadius, editorAreaRadius, this.areaBorderColor.get().getColorInt(), partial);
    }

    protected void tickMouseHighlighting() {

        if (!MouseInput.isLeftMouseDown()) {
            this.startHighlightLine = null;
            for (TextEditorLine t : this.textFieldLines) {
                t.isInMouseHighlightingMode = false;
            }
            return;
        }

        //Auto-scroll if mouse outside editor area and in mouse-highlighting mode
        if (this.isInMouseHighlightingMode()) {
            int mX = this.getRenderMouseX();
            int mY = this.getRenderMouseY();
            float speedMult = 0.008F;
            if (mX < this.borderLeft) {
                float f = Math.max(0.01F, (float)(this.borderLeft - mX) * speedMult);
                this.horizontalScrollBar.setScroll(this.horizontalScrollBar.getScroll() - f);
            } else if (mX > (this.getEditorAreaX() + this.getEditorAreaWidth())) {
                float f = Math.max(0.01F, (float)(mX - (this.getEditorAreaX() + this.getEditorAreaWidth())) * speedMult);
                this.horizontalScrollBar.setScroll(this.horizontalScrollBar.getScroll() + f);
            }
            if (mY < this.headerHeight) {
                float f = Math.max(0.01F, (float)(this.headerHeight - mY) * speedMult);
                this.verticalScrollBar.setScroll(this.verticalScrollBar.getScroll() - f);
            } else if (mY > (this.height - this.footerHeight)) {
                float f = Math.max(0.01F, (float)(mY - (this.height - this.footerHeight)) * speedMult);
                this.verticalScrollBar.setScroll(this.verticalScrollBar.getScroll() + f);
            }
        }

        if (!this.isMouseInsideEditorArea()) {
            return;
        }

        TextEditorLine first = this.startHighlightLine;
        TextEditorLine hovered = this.getHoveredLine();
        if ((hovered != null) && !hovered.isFocused() && (first != null)) {

            int firstIndex = this.getLineIndex(first);
            int hoveredIndex = this.getLineIndex(hovered);
            boolean firstIsBeforeHovered = hoveredIndex > firstIndex;
            boolean firstIsAfterHovered = hoveredIndex < firstIndex;

            if (first.isInMouseHighlightingMode) {
                if (firstIsAfterHovered) {
                    this.setFocusedLine(this.getLineIndex(hovered));
                    if (!hovered.isInMouseHighlightingMode) {
                        hovered.isInMouseHighlightingMode = true;
                        hovered.moveCursorTo(hovered.getValue().length(), false);
                    }
                } else if (firstIsBeforeHovered) {
                    this.setFocusedLine(this.getLineIndex(hovered));
                    if (!hovered.isInMouseHighlightingMode) {
                        hovered.isInMouseHighlightingMode = true;
                        hovered.moveCursorTo(0, false);
                    }
                } else if (first == hovered) {
                    this.setFocusedLine(this.getLineIndex(first));
                }
            }

            int startIndex = Math.min(hoveredIndex, firstIndex);
            int endIndex = Math.max(hoveredIndex, firstIndex);
            int index = 0;
            for (TextEditorLine t : this.textFieldLines) {
                //Highlight all lines between the first and current focusedLineIndex and remove highlighting from lines outside of highlight range
                if ((t != hovered) && (t != first)) {
                    if ((index > startIndex) && (index < endIndex)) {
                        if (firstIsAfterHovered) {
                            t.setCursorPosition(0);
                            t.setHighlightPos(t.getValue().length());
                        } else if (firstIsBeforeHovered) {
                            t.setCursorPosition(t.getValue().length());
                            t.setHighlightPos(0);
                        }
                    } else {
                        t.moveCursorTo(0, false);
                        t.isInMouseHighlightingMode = false;
                    }
                }
                index++;
            }
            this.startHighlightLineIndex = startIndex;
            this.endHighlightLineIndex = endIndex;

            if (first != hovered) {
                if (firstIsAfterHovered) {
                    first.moveCursorTo(0, true);
                } else if (firstIsBeforeHovered) {
                    first.moveCursorTo(first.getValue().length(), true);
                }
            }

        }

        TextEditorLine focused = this.getFocusedLine();
        if ((focused != null) && focused.isInMouseHighlightingMode) {
            if ((this.startHighlightLineIndex == -1) && (this.endHighlightLineIndex == -1)) {
                this.startHighlightLineIndex = this.getLineIndex(focused);
                this.endHighlightLineIndex = this.startHighlightLineIndex;
            }
            int i = Mth.floor(this.getRenderMouseX()) - focused.getX();
            if (focused.getAsAccessor().getBorderedFancyMenu()) {
                i -= 4;
            }
            String s = this.font.plainSubstrByWidth(focused.getValue().substring(focused.getAsAccessor().getDisplayPosFancyMenu()), focused.getInnerWidth());
            focused.moveCursorTo(this.font.plainSubstrByWidth(s, i).length() + focused.getAsAccessor().getDisplayPosFancyMenu(), true);
            if ((focused.getAsAccessor().getHighlightPosFancyMenu() == focused.getCursorPosition()) && (this.startHighlightLineIndex == this.endHighlightLineIndex)) {
                this.resetHighlighting();
            }
        }

    }

    public void updateLines(@Nullable Consumer<TextEditorLine> doAfterEachLineUpdate) {
        try {
            int index = 0;
            for (TextEditorLine line : this.textFieldLines) {
                line.lineIndex = index;
                line.setY(this.headerHeight + (this.lineHeight * index) + this.getLineRenderOffsetY());
                line.setX(this.borderLeft + this.getLineRenderOffsetX());
                line.setWidth(this.currentLineWidth);
                ((IMixinAbstractWidget)line).setHeightFancyMenu(this.lineHeight);
                line.getAsAccessor().setDisplayPosFancyMenu(0);
                if (doAfterEachLineUpdate != null) {
                    doAfterEachLineUpdate.accept(line);
                }
                index++;
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCMYENU] Failed to update lines!", ex);
        }
    }

    public void updateCurrentLineWidth() {
        //Find width of the longest focusedLineIndex and update current focusedLineIndex width
        int longestTextWidth = 0;
        for (TextEditorLine f : this.textFieldLines) {
            if (f.textWidth > longestTextWidth) {
                //Calculating the text size for every focusedLineIndex every tick kills the CPU, so I'm calculating the size on value change in the text box
                longestTextWidth = f.textWidth;
            }
        }
        this.currentLineWidth = longestTextWidth + 30;
    }

    public int getLineRenderOffsetX() {
        return -(int)(((float)this.getTotalScrollWidth() / 100.0F) * (this.horizontalScrollBar.getScroll() * 100.0F));
    }

    public int getLineRenderOffsetY() {
        return -(int)(((float)this.getTotalScrollHeight() / 100.0F) * (this.verticalScrollBar.getScroll() * 100.0F));
    }

    public int getTotalLineHeight() {
        return this.lineHeight * this.textFieldLines.size();
    }

    @Nullable
    public TextEditorLine addLineAtIndex(int index) {
        TextEditorLine f = new TextEditorLine(Minecraft.getInstance().font, 0, 0, 50, this.lineHeight, false, this.characterFilter, this);
        f.setMaxLength(Integer.MAX_VALUE);
        f.lineIndex = index;
        if (index > 0) {
            TextEditorLine before = this.getLine(index-1);
            if (before != null) {
                f.setY(before.getY() + this.lineHeight);
            }
        }
        this.textFieldLines.add(index, f);
        return f;
    }

    @Nullable
    public TextEditorLine addLine() {
        return this.addLineAtIndex(this.getLineCount());
    }

    public void removeLineAtIndex(int index) {
        if (index < 1) {
            return;
        }
        if (index <= this.getLineCount()-1) {
            this.textFieldLines.remove(index);
        }
    }

    public void removeLastLine() {
        this.removeLineAtIndex(this.getLineCount()-1);
    }

    public int getLineCount() {
        return this.textFieldLines.size();
    }

    @Nullable
    public TextEditorLine getLine(int index) {
        return this.textFieldLines.get(index);
    }

    public void setFocusedLine(int index) {
        if (index <= this.getLineCount()-1) {
            for (TextEditorLine f : this.textFieldLines) {
                f.setFocused(false);
            }
            this.getLine(index).setFocused(true);
        }
    }

    /**
     * Returns the cursorPos of the focused focusedLineIndex or -1 if no focusedLineIndex is focused.
     **/
    public int getFocusedLineIndex() {
        int index = 0;
        for (TextEditorLine f : this.textFieldLines) {
            if (f.isFocused()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    @Nullable
    public TextEditorLine getFocusedLine() {
        int index = this.getFocusedLineIndex();
        if (index != -1) {
            return this.getLine(index);
        }
        return null;
    }

    public boolean isLineFocused() {
        return (this.getFocusedLineIndex() > -1);
    }

    @Nullable
    public TextEditorLine getLineAfter(TextEditorLine line) {
        int index = this.getLineIndex(line);
        if ((index > -1) && (index < (this.getLineCount()-1))) {
            return this.getLine(index+1);
        }
        return null;
    }

    @Nullable
    public TextEditorLine getLineBefore(TextEditorLine line) {
        int index = this.getLineIndex(line);
        if (index > 0) {
            return this.getLine(index-1);
        }
        return null;
    }

    public boolean isAtLeastOneLineInHighlightMode() {
        for (TextEditorLine t : this.textFieldLines) {
            if (t.isInMouseHighlightingMode) {
                return true;
            }
        }
        return false;
    }

    /** Returns the lines between two indexes, EXCLUDING start AND end indexes! **/
    @Nullable
    public List<TextEditorLine> getLinesBetweenIndexes(int startIndex, int endIndex) {
        startIndex = Math.min(Math.max(startIndex, 0), this.textFieldLines.size()-1);
        endIndex = Math.min(Math.max(endIndex, 0), this.textFieldLines.size()-1);
        List<TextEditorLine> l = new ArrayList<>(this.textFieldLines.subList(startIndex, endIndex));
        if (!l.isEmpty()) {
            l.remove(0);
        }
        return l;
    }

    @Nullable
    public TextEditorLine getHoveredLine() {
        for (TextEditorLine t : this.textFieldLines) {
            if (t.isHovered()) {
                return t;
            }
        }
        return null;
    }

    public int getLineIndex(TextEditorLine inputBox) {
        return this.textFieldLines.indexOf(inputBox);
    }

    public void goUpLine() {
        if (this.isLineFocused()) {
            int current = Math.max(0, this.getFocusedLineIndex());
            if (current > 0) {
                TextEditorLine currentLine = this.getLine(current);
                this.setFocusedLine(current - 1);
                if (currentLine != null) {
                    Objects.requireNonNull(this.getFocusedLine()).moveCursorTo(this.lastCursorPosSetByUser, false);
                }
            }
        }
    }

    public void goDownLine(boolean isNewLine) {
        if (this.isLineFocused()) {
            int current = Math.max(0, this.getFocusedLineIndex());
            if (isNewLine) {
                this.addLineAtIndex(current+1);
            }
            TextEditorLine currentLine = this.getLine(current);
            this.setFocusedLine(current+1);
            if (currentLine != null) {
                TextEditorLine nextLine = this.getFocusedLine();
                if (nextLine == null) return;
                if (isNewLine) {
                    //Split content of currentLine at cursor pos and move text after cursor to next focusedLineIndex if ENTER was pressed
                    String textBeforeCursor = currentLine.getValue().substring(0, currentLine.getCursorPosition());
                    String textAfterCursor = currentLine.getValue().substring(currentLine.getCursorPosition());
                    currentLine.setValue(textBeforeCursor);
                    nextLine.setValue(textAfterCursor);
                    nextLine.moveCursorTo(0, false);
                    
                    //Add indentation of the old line to the new line
                    Matcher matcher = Pattern.compile("^(\\s+)").matcher(textBeforeCursor);
                    if (matcher.find()) {
                        String whitespace = matcher.group(1);
                        nextLine.setValue(whitespace + nextLine.getValue());
                        nextLine.moveCursorTo(whitespace.length(), false);
                    }
                } else {
                    nextLine.moveCursorTo(this.lastCursorPosSetByUser, false);
                }
            }
        }
    }

    public List<TextEditorLine> getCopyOfLines() {
        List<TextEditorLine> l = new ArrayList<>();
        for (TextEditorLine t : this.textFieldLines) {
            TextEditorLine n = new TextEditorLine(this.font, 0, 0, 0, 0, false, this.characterFilter, this);
            n.setValue(t.getValue());
            n.setFocused(t.isFocused());
            n.moveCursorTo(t.getCursorPosition(), false);
            l.add(n);
        }
        return l;
    }

    /**
     * Don't use this if you don't know what you do!<br>
     * For a safe way to get all lines, use {@link TextEditorWindowBody#getCopyOfLines()} instead.
     */
    public List<TextEditorLine> getLines() {
        return this.textFieldLines;
    }

    public boolean isTextHighlighted() {
        return (this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1);
    }

    public boolean isHighlightedTextHovered() {
        if (this.isTextHighlighted()) {
            List<TextEditorLine> highlightedLines = new ArrayList<>();
            if (this.endHighlightLineIndex <= this.getLineCount()-1) {
                highlightedLines.addAll(this.textFieldLines.subList(this.startHighlightLineIndex, this.endHighlightLineIndex+1));
            }
            for (TextEditorLine t : highlightedLines) {
                if (t.isHighlightedHovered()) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public String getHighlightedText() {
        try {
            if ((this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1)) {
                List<TextEditorLine> lines = new ArrayList<>();
                lines.add(this.getLine(this.startHighlightLineIndex));
                if (this.startHighlightLineIndex != this.endHighlightLineIndex) {
                    lines.addAll(Objects.requireNonNull(this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex)));
                    lines.add(this.getLine(this.endHighlightLineIndex));
                }
                StringBuilder s = new StringBuilder();
                boolean b = false;
                for (TextEditorLine t : lines) {
                    if (b) {
                        s.append("\n");
                    }
                    s.append(t.getHighlighted());
                    b = true;
                }
                return s.toString();
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to highlight text!", ex);
        }
        return "";
    }

    @NotNull
    public String cutHighlightedText() {
        String highlighted = this.getHighlightedText();
        this.deleteHighlightedText();
        return highlighted;
    }

    public void deleteHighlightedText() {
        int linesRemoved = 0;
        try {
            if ((this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1)) {
                if (this.startHighlightLineIndex == this.endHighlightLineIndex) {
                    Objects.requireNonNull(this.getLine(this.startHighlightLineIndex)).insertText("");
                } else {
                    TextEditorLine start = this.getLine(this.startHighlightLineIndex);
                    if (start == null) return;
                    start.insertText("");
                    TextEditorLine end = this.getLine(this.endHighlightLineIndex);
                    if (end == null) return;
                    end.insertText("");
                    if ((this.endHighlightLineIndex - this.startHighlightLineIndex) > 1) {
                        for (TextEditorLine line : Objects.requireNonNull(this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex))) {
                            this.removeLineAtIndex(this.getLineIndex(line));
                            linesRemoved++;
                        }
                    }
                    String oldStartValue = start.getValue();
                    start.setCursorPosition(start.getValue().length());
                    start.setHighlightPos(start.getCursorPosition());
                    start.insertText(end.getValue());
                    start.setCursorPosition(oldStartValue.length());
                    start.setHighlightPos(start.getCursorPosition());
                    this.removeLineAtIndex(this.getLineIndex(end));
                    linesRemoved++;
                    this.setFocusedLine(this.startHighlightLineIndex);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to delete highlighted text!", ex);
        }
        this.correctYScroll(-linesRemoved);
        this.resetHighlighting();
    }

    public void resetHighlighting() {
        this.startHighlightLineIndex = -1;
        this.endHighlightLineIndex = -1;
        for (TextEditorLine t : this.textFieldLines) {
            t.setHighlightPos(t.getCursorPosition());
        }
    }

    public boolean isInMouseHighlightingMode() {
        return MouseInput.isLeftMouseDown() && (this.startHighlightLine != null);
    }

    public void pasteText(String text) {
        try {
            if ((text != null) && !text.isEmpty()) {
                int addedLinesCount = 0;
                if (this.isTextHighlighted()) {
                    this.deleteHighlightedText();
                }
                if (!this.isLineFocused()) {
                    this.setFocusedLine(this.getLineCount()-1);
                    Objects.requireNonNull(this.getFocusedLine()).moveCursorToEnd(false);
                }
                TextEditorLine focusedLine = this.getFocusedLine();
                //These two strings are for correctly pasting text within a char sequence (if the cursor is not at the end or beginning of the focusedLineIndex)
                String textBeforeCursor = "";
                String textAfterCursor = "";
                if (!focusedLine.getValue().isEmpty()) {
                    textBeforeCursor = focusedLine.getValue().substring(0, focusedLine.getCursorPosition());
                    if (focusedLine.getCursorPosition() < focusedLine.getValue().length()) {
                        textAfterCursor = this.getFocusedLine().getValue().substring(focusedLine.getCursorPosition(), focusedLine.getValue().length());
                    }
                }
                focusedLine.setValue(textBeforeCursor);
                focusedLine.setCursorPosition(textBeforeCursor.length());
                String[] lines = new String[]{text};
                if (text.contains("\n")) {
                    lines = text.split("\n", -1);
                }
                Array.set(lines, lines.length-1, lines[lines.length-1] + textAfterCursor);
                if (lines.length == 1) {
                    this.getFocusedLine().insertText(lines[0]);
                } else if (lines.length > 1) {
                    int index = -1;
                    for (String s : lines) {
                        if (index == -1) {
                            index = this.getFocusedLineIndex();
                        } else {
                            this.addLineAtIndex(index);
                            addedLinesCount++;
                        }
                        Objects.requireNonNull(this.getLine(index)).insertText(s);
                        index++;
                    }
                    this.setFocusedLine(index - 1);
                    this.getFocusedLine().setCursorPosition(Math.max(0, this.getFocusedLine().getValue().length() - textAfterCursor.length()));
                    this.getFocusedLine().setHighlightPos(this.getFocusedLine().getCursorPosition());
                }
                this.correctYScroll(addedLinesCount);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to paste text!", ex);
        }
        if (this.indentGuideRenderer != null) {
            this.indentGuideRenderer.markDirty();
        }
        this.resetHighlighting();
    }

    public TextEditorWindowBody setText(@Nullable String text) {
        if (text == null) text = "";
        text = text.replace(NEWLINE_CODE, "\n").replace(SPACE_CODE, " ");
        TextEditorLine t = Objects.requireNonNull(this.getLine(0));
        this.textFieldLines.clear();
        this.textFieldLines.add(t);
        this.setFocusedLine(0);
        t.setValue("");
        t.moveCursorTo(0, false);
        this.pasteText(text);
        this.setFocusedLine(0);
        t.moveCursorTo(0, false);
        this.verticalScrollBar.setScroll(0.0F);
        if (this.indentGuideRenderer != null) {
            this.indentGuideRenderer.markDirty();
        }
        return this;
    }

    @NotNull
    public String getText() {
        StringBuilder s = new StringBuilder();
        boolean notFirstLine = false;
        for (TextEditorLine t : this.textFieldLines) {
            String value = t.getValue();
            if (notFirstLine) {
                s.append("\n");
                if (!this.multilineMode) {
                    // Replace all leading spaces with SPACE_CODE
                    Pattern pattern = Pattern.compile("^( +)");
                    Matcher matcher = pattern.matcher(value);
                    if (matcher.find()) {
                        String replacement = matcher.group().replace(" ", SPACE_CODE);
                        value = matcher.replaceFirst(replacement);
                    }
                }
            }
            s.append(value);
            notFirstLine = true;
        }
        String text = s.toString();
        return !this.multilineMode ? text.replace("\n", NEWLINE_CODE) : text;
    }

    protected boolean isTextValid() {
        if (this.textValidator != null) return this.textValidator.get(this);
        return true;
    }

    public TextEditorWindowBody setTextValidator(@Nullable ConsumingSupplier<TextEditorWindowBody, Boolean> textValidator) {
        this.textValidator = textValidator;
        return this;
    }

    public TextEditorWindowBody setTextValidatorUserFeedback(@Nullable UITooltip feedback) {
        this.textValidatorFeedbackUITooltip = feedback;
        return this;
    }

    public boolean placeholdersAllowed() {
        return this.allowPlaceholders;
    }

    public TextEditorWindowBody setPlaceholdersAllowed(boolean allowed) {
        this.allowPlaceholders = allowed;
        this.init();
        return this;
    }

    public boolean isMultilineMode() {
        return this.multilineMode;
    }

    public TextEditorWindowBody setMultilineMode(boolean multilineMode) {
        this.multilineMode = multilineMode;
        return this;
    }

    public boolean isBoldTitle() {
        return this.boldTitle;
    }

    public TextEditorWindowBody setBoldTitle(boolean boldTitle) {
        this.boldTitle = boldTitle;
        return this;
    }

    /**
     * @return The text BEFORE the cursor or NULL if no focusedLineIndex is focused.
     */
    @Nullable
    public String getTextBeforeCursor() {
        if (!this.isLineFocused()) {
            return null;
        }
        int focusedLineIndex = this.getFocusedLineIndex();
        List<TextEditorLine> lines = new ArrayList<>();
        if (focusedLineIndex == 0) {
            lines.add(this.getLine(0));
        } else if (focusedLineIndex > 0) {
            lines.addAll(this.textFieldLines.subList(0, focusedLineIndex+1));
        }
        TextEditorLine lastLine = lines.get(lines.size()-1);
        StringBuilder s = new StringBuilder();
        boolean b = false;
        for (TextEditorLine t : lines) {
            if (b) {
                s.append("\n");
            }
            if (t != lastLine) {
                s.append(t.getValue());
            } else {
                s.append(t.getValue(), 0, t.getCursorPosition());
            }
            b = true;
        }
        return s.toString();
    }

    /**
     * @return The text AFTER the cursor or NULL if no focusedLineIndex is focused.
     */
    @Nullable
    public String getTextAfterCursor() {
        if (!this.isLineFocused()) {
            return null;
        }
        int focusedLineIndex = this.getFocusedLineIndex();
        List<TextEditorLine> lines = new ArrayList<>();
        if (focusedLineIndex == this.getLineCount()-1) {
            lines.add(this.getLine(this.getLineCount()-1));
        } else if (focusedLineIndex < this.getLineCount()-1) {
            lines.addAll(this.textFieldLines.subList(focusedLineIndex, this.getLineCount()));
        }
        TextEditorLine firstLine = lines.get(0);
        StringBuilder s = new StringBuilder();
        boolean b = false;
        for (TextEditorLine t : lines) {
            if (b) {
                s.append("\n");
            }
            if (t != firstLine) {
                s.append(t.getValue());
            } else {
                s.append(t.getValue(), t.getCursorPosition(), t.getValue().length());
            }
            b = true;
        }
        return s.toString();
    }

    @Override
    public boolean charTyped(char character, int modifiers) {

        if (this.indentGuideRenderer != null) {
            this.indentGuideRenderer.markDirty();
        }

        if (this.isGoToLineOpen && (this.goToLineField != null) && this.goToLineField.isFocused()) {
            return this.goToLineField.charTyped(character, modifiers);
        }

        if (this.placeholdersAllowed() && extendedPlaceholderMenu && (this.searchBar != null) && this.searchBar.isFocused()) {
            return this.searchBar.charTyped(character, modifiers);
        }

        if (this.isLineFocused()) {
            this.history.saveSnapshot();
        }

        for (TextEditorLine l : this.textFieldLines) {
            l.charTyped(character, modifiers);
        }

        return super.charTyped(character, modifiers);

    }


    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {

        if (this.indentGuideRenderer != null) {
            this.indentGuideRenderer.markDirty();
        }

        if (this.isGoToLineOpen && (this.goToLineField != null)) {
            if (keycode == InputConstants.KEY_ESCAPE) {
                this.isGoToLineOpen = false;
                this.goToLineField.setFocused(false);
                return true;
            }
            if (keycode == InputConstants.KEY_ENTER) {
                try {
                    String val = this.goToLineField.getValue();
                    if (!val.isEmpty()) {
                        int line = Integer.parseInt(val);
                        line = Math.max(1, Math.min(line, this.getLineCount()));
                        this.setFocusedLine(line - 1);
                        this.correctYScroll(0);
                        TextEditorLine l = this.getFocusedLine();
                        if (l != null) l.moveCursorTo(0, false);
                    }
                } catch (Exception ignored) {}
                this.isGoToLineOpen = false;
                this.goToLineField.setFocused(false);
                return true;
            }
            if (this.goToLineField.keyPressed(keycode, scancode, modifiers)) return true;
        }

        //CTRL + G | GO TO LINE
        if (Screen.hasControlDown() && (keycode == GLFW.GLFW_KEY_G)) {
            this.isGoToLineOpen = !this.isGoToLineOpen;
            if (this.isGoToLineOpen) {
                this.goToLineField.setValue("");
                this.goToLineField.setFocused(true);
                this.setFocused(this.goToLineField);
            }
            return true;
        }

        if (this.placeholdersAllowed() && extendedPlaceholderMenu && (this.searchBar != null) && this.searchBar.isFocused()) {
            return this.searchBar.keyPressed(keycode, scancode, modifiers);
        }

        for (TextEditorLine l : new ArrayList<>(this.textFieldLines)) {
            l.keyPressed(keycode, scancode, modifiers);
        }

        String key = GLFW.glfwGetKeyName(keycode, scancode);
        if (key == null) key = "";

        //CTRL + Z | STEP BACK
        if (Screen.hasControlDown() && (key.equals("z"))) {
            this.history.stepBack();
            return true;
        }
        //CTRL + Y | STEP FORWARD
        if (Screen.hasControlDown() && (key.equals("y"))) {
            this.history.stepForward();
            return true;
        }
        //ALT + UP | MOVE LINE UP
        boolean altDown = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
        if (altDown && ((keycode == InputConstants.KEY_UP) || (keycode == GLFW.GLFW_KEY_PAGE_UP))) {
            if (this.isLineFocused()) {
                int index = this.getFocusedLineIndex();
                if (index > 0) {
                    this.history.saveSnapshot();
                    TextEditorLine current = this.getLine(index);
                    TextEditorLine above = this.getLine(index - 1);
                    if ((current != null) && (above != null)) {
                        String currentVal = current.getValue();
                        String aboveVal = above.getValue();
                        current.setValue(aboveVal);
                        above.setValue(currentVal);
                        this.setFocusedLine(index - 1);
                        above.moveCursorTo(current.getCursorPosition(), false);
                        this.resetHighlighting();
                    }
                }
            }
            return true;
        }

        //ALT + DOWN | MOVE LINE DOWN
        if (altDown && ((keycode == InputConstants.KEY_DOWN) || (keycode == GLFW.GLFW_KEY_PAGE_DOWN))) {
            if (this.isLineFocused()) {
                int index = this.getFocusedLineIndex();
                if (index < this.getLineCount() - 1) {
                    this.history.saveSnapshot();
                    TextEditorLine current = this.getLine(index);
                    TextEditorLine below = this.getLine(index + 1);
                    if ((current != null) && (below != null)) {
                        String currentVal = current.getValue();
                        String belowVal = below.getValue();
                        current.setValue(belowVal);
                        below.setValue(currentVal);
                        this.setFocusedLine(index + 1);
                        below.moveCursorTo(current.getCursorPosition(), false);
                        this.resetHighlighting();
                    }
                }
            }
            return true;
        }

        //ENTER
        if (keycode == InputConstants.KEY_ENTER) {
            if (!this.isInMouseHighlightingMode()) {
                if (this.isLineFocused()) {
                    this.history.saveSnapshot();
                    this.resetHighlighting();
                    this.goDownLine(true);
                    this.correctYScroll(1);
                }
            }
            return true;
        }
        //ARROW UP
        if (keycode == InputConstants.KEY_UP) {
            if (!this.isInMouseHighlightingMode()) {
                this.resetHighlighting();
                this.goUpLine();
                this.correctYScroll(0);
            }
            return true;
        }
        //ARROW DOWN
        if (keycode == InputConstants.KEY_DOWN) {
            if (!this.isInMouseHighlightingMode()) {
                this.resetHighlighting();
                this.goDownLine(false);
                this.correctYScroll(0);
            }
            return true;
        }

        //BACKSPACE
        if (keycode == InputConstants.KEY_BACKSPACE) {
            if (!this.isInMouseHighlightingMode()) {
                if (this.isTextHighlighted()) {
                    this.history.saveSnapshot();
                    this.deleteHighlightedText();
                } else {
                    if (this.isLineFocused()) {
                        if (!this.getText().isEmpty()) this.history.saveSnapshot();
                        TextEditorLine focused = Objects.requireNonNull(this.getFocusedLine());
                        focused.getAsAccessor().invokeDeleteTextFancyMenu(-1);
                    }
                }
                this.resetHighlighting();
            }
            return true;
        }
        //CTRL + C
        if (Screen.isCopy(keycode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlightedText());
            return true;
        }
        //CTRL + V
        if (Screen.isPaste(keycode)) {
            this.history.saveSnapshot();
            this.pasteText(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        //CTRL + A
        if (Screen.isSelectAll(keycode)) {
            for (TextEditorLine t : new ArrayList<>(this.textFieldLines)) {
                t.setHighlightPos(0);
                t.setCursorPosition(t.getValue().length());
            }
            this.setFocusedLine(this.getLineCount()-1);
            this.startHighlightLineIndex = 0;
            this.endHighlightLineIndex = this.getLineCount()-1;
            return true;
        }
        //CTRL + U
        if (Screen.isCut(keycode)) {
            this.history.saveSnapshot();
            Minecraft.getInstance().keyboardHandler.setClipboard(this.cutHighlightedText());
            this.resetHighlighting();
            return true;
        }
        //Reset highlighting when pressing left/right arrow keys
        if ((keycode == InputConstants.KEY_RIGHT) || (keycode == InputConstants.KEY_LEFT)) {
            this.resetHighlighting();
            return true;
        }

        //CTRL + D | DUPLICATE LINE
        if (Screen.hasControlDown() && (keycode == GLFW.GLFW_KEY_D)) {
            if (this.isLineFocused()) {
                this.history.saveSnapshot();
                int index = this.getFocusedLineIndex();
                TextEditorLine current = this.getLine(index);
                if (current != null) {
                    TextEditorLine newLine = this.addLineAtIndex(index + 1);
                    if (newLine != null) {
                        newLine.setValue(current.getValue());
                        this.setFocusedLine(index + 1);
                        newLine.moveCursorTo(current.getCursorPosition(), false);
                        this.correctYScroll(1);
                    }
                }
            }
            return true;
        }



        //CTRL + HOME | GO TO START
        if (Screen.hasControlDown() && (keycode == GLFW.GLFW_KEY_HOME)) {
            this.resetHighlighting();
            if (this.getLineCount() > 0) {
                this.setFocusedLine(0);
                TextEditorLine line = this.getLine(0);
                if (line != null) line.moveCursorTo(0, false);
                this.correctYScroll(-this.getTotalLineHeight()); 
            }
            return true;
        }

        //CTRL + END | GO TO END
        if (Screen.hasControlDown() && (keycode == GLFW.GLFW_KEY_END)) {
            this.resetHighlighting();
            if (this.getLineCount() > 0) {
                int lastIndex = this.getLineCount() - 1;
                this.setFocusedLine(lastIndex);
                TextEditorLine line = this.getLine(lastIndex);
                if (line != null) line.moveCursorToEnd(false);
                this.correctYScroll(this.getTotalLineHeight());
            }
            return true;
        }

        return super.keyPressed(keycode, scancode, modifiers);

    }

    @Override
    public boolean keyReleased(int i1, int i2, int i3) {

        if (this.isGoToLineOpen && (this.goToLineField != null) && this.goToLineField.isFocused()) {
            return this.goToLineField.keyReleased(i1, i2, i3);
        }

        if (this.placeholdersAllowed() && extendedPlaceholderMenu && (this.searchBar != null) && this.searchBar.isFocused()) {
            return this.searchBar.keyReleased(i1, i2, i3);
        }

        for (TextEditorLine l : this.textFieldLines) {
            l.keyReleased(i1, i2, i3);
        }

        return super.keyReleased(i1, i2, i3);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        this.setFocused(null);

        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (this.isGoToLineOpen && (this.goToLineField != null)) {
            if (!this.goToLineField.isMouseOver(mouseX, mouseY)) {
                this.isGoToLineOpen = false;
                this.goToLineField.setFocused(false);
            }
        }

        this.selectedHoveredOnRightClickMenuOpen = false;

        if (!this.isMouseInteractingWithGrabbers()) {

            for (TextEditorLine l : this.textFieldLines) {
                l.mouseClicked(mouseX, mouseY, button);
            }

            if (this.isMouseInsideEditorArea()) {
                if (button == 1) {
                    this.rightClickContextMenu.closeMenu();
                }
                if ((button == 0) || (button == 1)) {
                    boolean isHighlightedHovered = this.isHighlightedTextHovered();
                    TextEditorLine hoveredLine = this.getHoveredLine();
                    if (!this.rightClickContextMenu.isOpen()) {
                        if ((button == 0) || !isHighlightedHovered) {
                            this.resetHighlighting();
                        }
                        if (hoveredLine == null) {
                            TextEditorLine focus = this.getLine(this.getLineCount()-1);
                            for (TextEditorLine t : this.textFieldLines) {
                                if ((mouseY >= t.getY()) && (mouseY <= t.getY() + t.getHeight())) {
                                    focus = t;
                                    break;
                                }
                            }
                            this.setFocusedLine(this.getLineIndex(focus));
                            Objects.requireNonNull(this.getFocusedLine()).moveCursorToEnd(false);
                            this.correctYScroll(0);
                        } else if ((button == 1) && !isHighlightedHovered) {
                            //Focus focusedLineIndex in case it is right-clicked
                            this.setFocusedLine(this.getLineIndex(hoveredLine));
                            //Set cursor in case focusedLineIndex is right-clicked
                            String s = this.font.plainSubstrByWidth(hoveredLine.getValue().substring(hoveredLine.getAsAccessor().getDisplayPosFancyMenu()), hoveredLine.getInnerWidth());
                            hoveredLine.moveCursorTo(this.font.plainSubstrByWidth(s, (int)mouseX - hoveredLine.getX()).length() + hoveredLine.getAsAccessor().getDisplayPosFancyMenu(), false);
                        }
                    }
                    if (button == 1) {
                        this.selectedHoveredOnRightClickMenuOpen = this.isHighlightedTextHovered();
                        ContextMenuHandler.INSTANCE.setAndOpenAtMouse(this.rightClickContextMenu);
                    } else if (this.rightClickContextMenu.isOpen() && !this.rightClickContextMenu.isHovered()) {
                        this.rightClickContextMenu.closeMenu();
                        //Call mouseClicked of lines after closing the menu, so the focused focusedLineIndex and cursor pos gets updated
                        this.textFieldLines.forEach((line) -> {
                            line.mouseClicked(mouseX, mouseY, button);
                        });
                        //Call mouseClicked of editor again to do everything that would happen when clicked without the context menu opened
                        this.mouseClicked(mouseX, mouseY, button);
                    }
                }
            }

        }

        List<PlaceholderMenuEntry> entries = new ArrayList<>(this.placeholderMenuEntries);
        for (PlaceholderMenuEntry e : entries) {
            e.buttonBase.mouseClicked(mouseX, mouseY, button);
        }

        return false;

    }

    @Override
    public void tick() {

        for (TextEditorLine l : this.textFieldLines) {
            l.tick();
        }

        super.tick();

    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    public boolean isMouseInteractingWithGrabbers() {
        return this.verticalScrollBar.isGrabberGrabbed() || this.verticalScrollBar.isGrabberHovered() || this.horizontalScrollBar.isGrabberGrabbed() || this.horizontalScrollBar.isGrabberHovered();
    }

    public boolean isMouseInteractingWithPlaceholderGrabbers() {
        return this.verticalScrollBarPlaceholderMenu.isGrabberGrabbed() || this.verticalScrollBarPlaceholderMenu.isGrabberHovered() || this.horizontalScrollBarPlaceholderMenu.isGrabberGrabbed() || this.horizontalScrollBarPlaceholderMenu.isGrabberHovered();
    }

    public int getEditBoxCursorX(EditBox editBox) {
        try {
            IMixinEditBox b = (IMixinEditBox) editBox;
            String s = this.font.plainSubstrByWidth(editBox.getValue().substring(b.getDisplayPosFancyMenu()), editBox.getInnerWidth());
            int j = editBox.getCursorPosition() - b.getDisplayPosFancyMenu();
            boolean flag = j >= 0 && j <= s.length();
            boolean flag2 = editBox.getCursorPosition() < editBox.getValue().length() || editBox.getValue().length() >= b.getMaxLengthFancyMenu();
            int l = b.getBorderedFancyMenu() ? editBox.getX() + 4 : editBox.getX();
            int j1 = l;
            if (!s.isEmpty()) {
                String s1 = flag ? s.substring(0, j) : s;
                j1 += this.font.width(b.getFormatterFancyMenu().apply(s1, b.getDisplayPosFancyMenu()));
            }
            int k1 = j1;
            if (!flag) {
                k1 = j > 0 ? l + editBox.getWidth() : l;
            } else if (flag2) {
                k1 = j1 - 1;
                --j1;
            }
            return k1;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get cursor X position!", ex);
        }
        return 0;
    }

    public void scrollToLine(int lineIndex, boolean bottom) {
        if (bottom) {
            this.scrollToLine(lineIndex, -Math.max(0, this.getEditorAreaHeight() - this.lineHeight));
        } else {
            this.scrollToLine(lineIndex, 0);
        }
    }

    public void scrollToLine(int lineIndex, int offset) {
        int totalLineHeight = this.getTotalScrollHeight();
        float f = (float)Math.max(0, ((lineIndex + 1) * this.lineHeight) - this.lineHeight) / (float)totalLineHeight;
        if (offset != 0) {
            if (offset > 0) {
                f += ((float)offset / (float)totalLineHeight);
            } else {
                f -= ((float)Math.abs(offset) / (float)totalLineHeight);
            }
        }
        this.verticalScrollBar.setScroll(f);
    }

    public int getTotalScrollHeight() {
        if (this.overriddenTotalScrollHeight != -1) {
            return this.overriddenTotalScrollHeight;
        }
        return this.getTotalLineHeight();
    }

    public int getTotalScrollWidth() {
        //return Math.max(0, this.currentLineWidth - this.getEditorAreaWidth())
        return this.currentLineWidth;
    }

    public void correctYScroll(int lineCountOffsetAfterRemovingAdding) {

        //Don't fix scroll if in mouse-highlighting mode or no focusedLineIndex is focused
        if (this.isInMouseHighlightingMode() || !this.isLineFocused()) {
            return;
        }

        int minY = this.getEditorAreaY();
        int maxY = this.getEditorAreaY() + this.getEditorAreaHeight();
        int currentLineY = Objects.requireNonNull(this.getFocusedLine()).getY();

        if (currentLineY < minY) {
            this.scrollToLine(this.getFocusedLineIndex(), false);
        } else if ((currentLineY + this.lineHeight) > maxY) {
            this.scrollToLine(this.getFocusedLineIndex(), true);
        } else if (lineCountOffsetAfterRemovingAdding != 0) {
            this.overriddenTotalScrollHeight = -1;
            int removedAddedLineCount = Math.abs(lineCountOffsetAfterRemovingAdding);
            if (lineCountOffsetAfterRemovingAdding > 0) {
                this.overriddenTotalScrollHeight = this.getTotalScrollHeight() - (this.lineHeight * removedAddedLineCount);
            } else if (lineCountOffsetAfterRemovingAdding < 0) {
                this.overriddenTotalScrollHeight = this.getTotalScrollHeight() + (this.lineHeight * removedAddedLineCount);
            }
            this.updateLines(null);
            this.overriddenTotalScrollHeight = -1;
            int diffToTop = Math.max(0, this.getFocusedLine().getY() - this.getEditorAreaY());
            this.scrollToLine(this.getFocusedLineIndex(), -diffToTop);
            this.correctYScroll(0);
        }

        if (this.getTotalLineHeight() <= this.getEditorAreaHeight()) {
            this.verticalScrollBar.setScroll(0.0F);
        }

    }

    public void correctXScroll(TextEditorLine line) {

        //Don't fix scroll if in mouse-highlighting mode
        if (this.isInMouseHighlightingMode()) {
            return;
        }

        if (this.isLineFocused() && (this.getFocusedLine() == line)) {

            int oldX = line.getX();

            this.updateCurrentLineWidth();
            this.updateLines(null);

            int newX = line.getX();
            String oldValue = line.lastTickValue;
            String newValue = line.getValue();

            //Make the lines scroll horizontally with the cursor position if the cursor is too far to the left or right
            int cursorWidth = 2;
            if (line.getCursorPosition() >= newValue.length()) {
                cursorWidth = 6;
            }
            int editorAreaCenterX = this.getEditorAreaX() + (this.getEditorAreaWidth() / 2);
            int cursorX = this.getEditBoxCursorX(line);
            if (cursorX > editorAreaCenterX) {
                cursorX += cursorWidth + 5;
            } else if (cursorX < editorAreaCenterX) {
                cursorX -= cursorWidth + 5;
            }
            int maxToRight = this.getEditorAreaX() + this.getEditorAreaWidth();
            int maxToLeft = this.getEditorAreaX();
            float currentScrollX = this.horizontalScrollBar.getScroll();
            int currentLineW = this.getTotalScrollWidth();
            boolean textGotDeleted = oldValue.length() > newValue.length();
            boolean textGotAdded = oldValue.length() < newValue.length();
            if (cursorX > maxToRight) {
                float f = (float)(cursorX - maxToRight) / (float)currentLineW;
                this.horizontalScrollBar.setScroll(currentScrollX + f);
            } else if (cursorX < maxToLeft) {
                //By default, move back the focusedLineIndex just a little when moving the cursor to the left side by using the mouse or arrow keys
                float f = (float)(maxToLeft - cursorX) / (float)currentLineW;
                //But move it back a big chunk when deleting chars (by pressing backspace)
                if (textGotDeleted) {
                    f = (float)(maxToRight - maxToLeft) / (float)currentLineW;
                }
                this.horizontalScrollBar.setScroll(currentScrollX - f);
            } else if (textGotDeleted && (oldX < newX)) {
                float f = (float)(newX - oldX) / (float)currentLineW;
                this.horizontalScrollBar.setScroll(currentScrollX + f);
            } else if (textGotAdded && (oldX > newX)) {
                float f = (float)(oldX - newX) / (float)currentLineW;
                this.horizontalScrollBar.setScroll(currentScrollX - f);
            }
            if (line.getCursorPosition() == 0) {
                this.horizontalScrollBar.setScroll(0.0F);
            }

        }

    }

    public boolean isMouseInsideEditorArea() {
        int xStart = this.borderLeft;
        int yStart = this.headerHeight;
        int xEnd = this.getEditorAreaX() + this.getEditorAreaWidth();
        int yEnd = this.height - this.footerHeight;
        int mX = this.getRenderMouseX();
        int mY = this.getRenderMouseY();
        return (mX >= xStart) && (mX <= xEnd) && (mY >= yStart) && (mY <= yEnd);
    }

    public int getEditorAreaWidth() {
        int i = (this.width - this.borderRight) - this.borderLeft;
        if (extendedPlaceholderMenu) {
            i = i - this.getPlaceholderAreaWidth() - 15;
        }
        return i;
    }

    public int getEditorAreaHeight() {
        return (this.height - this.footerHeight) - this.headerHeight;
    }

    public int getEditorAreaX() {
        return this.borderLeft;
    }

    public int getEditorAreaY() {
        return this.headerHeight;
    }

    public void toggleIndentationGuides() {
        this.showIndentationGuides = !this.showIndentationGuides;
    }

    public boolean areIndentationGuidesVisible() {
        return this.showIndentationGuides;
    }

    /**
     * @return The compiled version of the input string or NULL if the input was NULL.
     */
    public static String compileSingleLineString(@Nullable String s) {
        if (s == null) return null;
        String compiled = COMPILED_SINGLE_LINE_STRINGS.computeIfAbsent(s, s1 -> s1.replace(NEWLINE_CODE, "").replace(SPACE_CODE, ""));
        return compiled;
    }

    public static void clearCompiledSingleLineCache() {
        LOGGER.info("[FANCYMENU] Clearing text editor's compiled single line string cache..");
        COMPILED_SINGLE_LINE_STRINGS.clear();
    }

    public static class PlaceholderMenuEntry extends UIBase {

        public TextEditorWindowBody parent;
        public final Component label;
        public Runnable clickAction;
        public int x;
        public int y;
        public final int labelWidth;
        protected Color backgroundColorIdle = Color.GRAY;
        protected Color backgroundColorHover = Color.LIGHT_GRAY;
        protected Color dotColor = Color.BLUE;
        protected Color entryLabelColor = Color.WHITE;
        public ExtendedButton buttonBase;
        public Font font = Minecraft.getInstance().font;

        public PlaceholderMenuEntry(@NotNull TextEditorWindowBody parent, @NotNull Component label, @NotNull Runnable clickAction) {
            this.parent = parent;
            this.label = label;
            this.clickAction = clickAction;
            this.labelWidth = this.font.width(this.label);
            this.buttonBase = new ExtendedButton(0, 0, this.getWidth(), this.getHeight(), "", (button) -> {
                this.clickAction.run();
            }) {
                @Override
                public boolean isHoveredOrFocused() {
                    if (PlaceholderMenuEntry.this.parent.isMouseInteractingWithPlaceholderGrabbers()) {
                        return false;
                    }
                    return super.isHoveredOrFocused();
                }
                @Override
                public void onClick(double p_93371_, double p_93372_) {
                    if (PlaceholderMenuEntry.this.parent.isMouseInteractingWithPlaceholderGrabbers()) {
                        return;
                    }
                    super.onClick(p_93371_, p_93372_);
                }
                @Override
                public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                    if (PlaceholderMenuEntry.this.parent.isMouseInteractingWithPlaceholderGrabbers()) {
                        this.isHovered = false;
                    }
                    super.render(graphics, p_93658_, p_93659_, p_93660_);
                }
            };
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            //Update the button colors
            this.buttonBase.setBackgroundColor(
                    DrawableColor.FULLY_TRANSPARENT,
                    DrawableColor.FULLY_TRANSPARENT,
                    DrawableColor.FULLY_TRANSPARENT,
                    DrawableColor.FULLY_TRANSPARENT,
                    DrawableColor.FULLY_TRANSPARENT,
                    DrawableColor.FULLY_TRANSPARENT
            );
            //Update the button pos
            this.buttonBase.setX(this.x);
            this.buttonBase.setY(this.y);
            int yCenter = this.y + (this.getHeight() / 2);
            //Render hover effect
            if (!this.parent.isMouseInteractingWithPlaceholderGrabbers() && this.buttonBase.isMouseOver(mouseX, mouseY)) {
                int areaY = this.parent.getPlaceholderAreaY() + 2;
                int areaBottom = this.parent.getPlaceholderAreaY() + this.parent.getPlaceholderAreaHeight() - 2;
                int entryY = this.y;
                int entryHeight = this.getHeight();
                int visibleTop = Math.max(entryY, areaY);
                int visibleBottom = Math.min(entryY + entryHeight, areaBottom);
                int visibleHeight = visibleBottom - visibleTop;
                if (visibleHeight > 0) {
                    boolean roundTop = entryY <= areaY + 5;
                    boolean roundBottom = (entryY + entryHeight) >= areaBottom - 5;
                    int hoverColor = this.backgroundColorHover.getRGB();
                    if (roundTop || roundBottom) {
                        float radius = UIBase.getInterfaceCornerRoundingRadius();
                        if (roundTop && roundBottom) {
                            SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, this.x, visibleTop, this.getWidth(), visibleHeight, radius, radius, radius, radius, hoverColor, partial);
                        } else if (roundTop) {
                            SmoothRectangleRenderer.renderSmoothRectRoundTopCornersScaled(graphics, this.x, visibleTop, this.getWidth(), visibleHeight, radius, hoverColor, partial);
                        } else {
                            SmoothRectangleRenderer.renderSmoothRectRoundBottomCornersScaled(graphics, this.x, visibleTop, this.getWidth(), visibleHeight, radius, hoverColor, partial);
                        }
                    } else {
                        graphics.fill(this.x, visibleTop, this.x + this.getWidth(), visibleTop + visibleHeight, hoverColor);
                    }
                }
            }
            //Render the button
            this.buttonBase.render(graphics, mouseX, mouseY, partial);
            //Render dot
            renderListingDot(graphics, this.x + 5, yCenter - 2, this.dotColor);
            //Render label
            graphics.drawString(this.font, this.label, this.x + 5 + 4 + 3, yCenter - (this.font.lineHeight / 2), this.entryLabelColor.getRGB(), false);
        }

        public int getWidth() {
            return Math.max(this.parent.placeholderMenuWidth, 5 + 4 + 3 + this.labelWidth + 5);
        }

        public int getHeight() {
            return this.parent.placeholderMenuEntryHeight;
        }

        public boolean isHovered() {
            return this.buttonBase.isHoveredOrFocused();
        }

        public void setDescription(String... desc) {
            this.buttonBase.setUITooltip(UITooltip.of(desc));
        }

    }

}
