package de.keksuccino.fancymenu.util.rendering.ui.texteditor;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.formattingrules.TextEditorFormattingRules;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("all")
public class TextEditorScreen extends Screen {

    //TODO Ganze Zeile markieren, wenn zwischen highlightStart und highlightEnd index

    //TODO Bei highlight start und end Zeilen alles markieren, was innerhalb von markiertem bereich liegt, selbst wenn eigentlicher Text kürzer (also alles NACH cursor bei end und alles VOR cursor bei start)

    //TODO Style.withFont() nutzen, um eventuell in editor mit eigener Font zu arbeiten

    //TODO Auto-scrollen bei maus außerhalb von editor area während markieren verbessern (ist zu schnell bei langen Texten)

    //TODO fixen: bei korrigieren von Y scroll scrollt es ab und zu nach unten (vermutlich Rundungsfehler in Offset Berechnung)

    //TODO zum korrigieren nicht mehr unnötig zeilen updaten, sondern stattdessen checken, wie yOffset mit alter Zeilenanzahl und neuer Zeilenanzahl ist, dann Differenz berechnen

    private static final Logger LOGGER = LogManager.getLogger();

    protected final CharacterFilter characterFilter;
    protected final Consumer<String> callback;
    protected List<TextEditorLine> textFieldLines = new ArrayList<>();
    protected ScrollBar verticalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, UIBase.VERTICAL_SCROLL_BAR_WIDTH, UIBase.VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, (Color) null, null);
    protected ScrollBar horizontalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, UIBase.HORIZONTAL_SCROLL_BAR_WIDTH, UIBase.HORIZONTAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, (Color) null, null);
    protected ScrollBar verticalScrollBarPlaceholderMenu = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, UIBase.VERTICAL_SCROLL_BAR_WIDTH, UIBase.VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, (Color) null, null);
    protected ScrollBar horizontalScrollBarPlaceholderMenu = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, UIBase.HORIZONTAL_SCROLL_BAR_WIDTH, UIBase.HORIZONTAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, (Color) null, null);
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
    protected Color screenBackgroundColor = UIBase.getUIColorTheme().screen_background_color.getColor();
    protected Color editorAreaBorderColor = UIBase.getUIColorTheme().element_border_color_normal.getColor();
    protected Color editorAreaBackgroundColor = UIBase.getUIColorTheme().area_background_color.getColor();
    protected Color textColor = UIBase.getUIColorTheme().text_editor_text_color.getColor();
    protected Color focusedLineColor = UIBase.getUIColorTheme().list_entry_color_selected_hovered.getColor();
    protected Color scrollGrabberIdleColor = UIBase.getUIColorTheme().scroll_grabber_color_normal.getColor();
    protected Color scrollGrabberHoverColor = UIBase.getUIColorTheme().scroll_grabber_color_hover.getColor();
    protected Color sideBarColor = UIBase.getUIColorTheme().text_editor_sidebar_color.getColor();
    protected Color lineNumberTextColorNormal = UIBase.getUIColorTheme().text_editor_line_number_text_color_normal.getColor();
    protected Color lineNumberTextColorFocused = UIBase.getUIColorTheme().text_editor_line_number_text_color_selected.getColor();
    protected Color multilineNotSupportedNotificationColor = UIBase.getUIColorTheme().error_text_color.getColor();
    protected Color placeholderEntryBackgroundColorIdle = UIBase.getUIColorTheme().area_background_color.getColor();
    protected Color placeholderEntryBackgroundColorHover = UIBase.getUIColorTheme().list_entry_color_selected_hovered.getColor();
    protected Color placeholderEntryDotColorPlaceholder = UIBase.getUIColorTheme().listing_dot_color_1.getColor();
    protected Color placeholderEntryDotColorCategory = UIBase.getUIColorTheme().listing_dot_color_2.getColor();
    protected Color placeholderEntryLabelColor = UIBase.getUIColorTheme().description_area_text_color.getColor();
    protected Color placeholderEntryBackToCategoriesLabelColor = UIBase.getUIColorTheme().warning_text_color.getColor();
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
    protected long multilineNotSupportedNotificationDisplayStart = -1L;
    protected boolean boldTitle = true;
    protected ConsumingSupplier<TextEditorScreen, Boolean> textValidator = null;
    protected Tooltip textValidatorFeedbackTooltip = null;
    protected boolean selectedHoveredOnRightClickMenuOpen = false;

    @NotNull
    public static TextEditorScreen build(@Nullable Component title, @Nullable CharacterFilter characterFilter, @NotNull Consumer<String> callback) {
        return new TextEditorScreen(title, characterFilter, callback);
    }

    public TextEditorScreen(@Nullable CharacterFilter characterFilter, @NotNull Consumer<String> callback) {
        this(null, characterFilter, callback);
    }

    public TextEditorScreen(@Nullable Component title, @Nullable CharacterFilter characterFilter, @NotNull Consumer<String> callback) {
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
        this.updatePlaceholderEntries(null, true, true);
        this.updateCurrentLineWidth();
    }

    @Override
    public void init() {

        this.updateRightClickContextMenu();
        this.addWidget(this.rightClickContextMenu);

        this.verticalScrollBar.scrollAreaStartX = this.getEditorAreaX() + 1;
        this.verticalScrollBar.scrollAreaStartY = this.getEditorAreaY() + 1;
        this.verticalScrollBar.scrollAreaEndX = this.getEditorAreaX() + this.getEditorAreaWidth() - 2;
        this.verticalScrollBar.scrollAreaEndY = this.getEditorAreaY() + this.getEditorAreaHeight() - this.horizontalScrollBar.grabberHeight - 2;

        this.horizontalScrollBar.scrollAreaStartX = this.getEditorAreaX() + 1;
        this.horizontalScrollBar.scrollAreaStartY = this.getEditorAreaY() + 1;
        this.horizontalScrollBar.scrollAreaEndX = this.getEditorAreaX() + this.getEditorAreaWidth() - this.verticalScrollBar.grabberWidth - 2;
        this.horizontalScrollBar.scrollAreaEndY = this.getEditorAreaY() + this.getEditorAreaHeight() - 1;

        int placeholderAreaX = this.width - this.borderRight - this.placeholderMenuWidth;
        int placeholderAreaY = this.getEditorAreaY();

        this.verticalScrollBarPlaceholderMenu.scrollAreaStartX = placeholderAreaX + 1;
        this.verticalScrollBarPlaceholderMenu.scrollAreaStartY = placeholderAreaY + 1;
        this.verticalScrollBarPlaceholderMenu.scrollAreaEndX = placeholderAreaX + this.placeholderMenuWidth - 2;
        this.verticalScrollBarPlaceholderMenu.scrollAreaEndY = placeholderAreaY + this.getEditorAreaHeight() - this.horizontalScrollBarPlaceholderMenu.grabberHeight - 2;

        this.horizontalScrollBarPlaceholderMenu.scrollAreaStartX = placeholderAreaX + 1;
        this.horizontalScrollBarPlaceholderMenu.scrollAreaStartY = placeholderAreaY + 1;
        this.horizontalScrollBarPlaceholderMenu.scrollAreaEndX = placeholderAreaX + this.placeholderMenuWidth - this.verticalScrollBarPlaceholderMenu.grabberWidth - 2;
        this.horizontalScrollBarPlaceholderMenu.scrollAreaEndY = placeholderAreaY + this.getEditorAreaHeight() - 1;

        //Set scroll grabber colors
        this.verticalScrollBar.idleBarColor = this.scrollGrabberIdleColor;
        this.verticalScrollBar.hoverBarColor = this.scrollGrabberHoverColor;
        this.horizontalScrollBar.idleBarColor = this.scrollGrabberIdleColor;
        this.horizontalScrollBar.hoverBarColor = this.scrollGrabberHoverColor;

        //Set placeholder menu scroll bar colors
        this.verticalScrollBarPlaceholderMenu.idleBarColor = this.scrollGrabberIdleColor;
        this.verticalScrollBarPlaceholderMenu.hoverBarColor = this.scrollGrabberHoverColor;
        this.horizontalScrollBarPlaceholderMenu.idleBarColor = this.scrollGrabberIdleColor;
        this.horizontalScrollBarPlaceholderMenu.hoverBarColor = this.scrollGrabberHoverColor;

        this.cancelButton = new ExtendedButton(this.width - this.borderRight - 100 - 5 - 100, this.height - 35, 100, 20, I18n.get("fancymenu.guicomponents.cancel"), (button) -> {
            this.onClose();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.doneButton = new ExtendedButton(this.width - this.borderRight - 100, this.height - 35, 100, 20, I18n.get("fancymenu.guicomponents.done"), (button) -> {
            if (this.isTextValid()) this.callback.accept(this.getText());
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        if (this.allowPlaceholders) {
            this.placeholderButton = new ExtendedButton(this.width - this.borderRight - 100, (this.headerHeight / 2) - 10, 100, 20, I18n.get("fancymenu.ui.text_editor.placeholders"), (button) -> {
                if (extendedPlaceholderMenu) {
                    extendedPlaceholderMenu = false;
                } else {
                    extendedPlaceholderMenu = true;
                }
                this.rebuildWidgets();
            }).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.dynamicvariabletextfield.variables.desc"))).setDefaultStyle());
            this.addWidget(this.placeholderButton);
            UIBase.applyDefaultWidgetSkinTo(this.placeholderButton);
            if (extendedPlaceholderMenu) {
                this.placeholderButton.setBackground(ExtendedButton.ColorButtonBackground.create(UIBase.getUIColorTheme().element_background_color_normal, UIBase.getUIColorTheme().element_background_color_hover, DrawableColor.of(this.editorAreaBorderColor), DrawableColor.of(this.editorAreaBorderColor)));
                ((IMixinAbstractWidget)this.placeholderButton).setHeightFancyMenu(this.getEditorAreaY() - ((this.headerHeight / 2) - 10));
            }
        } else {
            this.placeholderButton = null;
            extendedPlaceholderMenu = false;
        }

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
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.copy"));

        this.rightClickContextMenu.addClickableEntry("paste", Component.translatable("fancymenu.ui.text_editor.paste"), (menu, entry) -> {
            this.pasteText(Minecraft.getInstance().keyboardHandler.getClipboard());
            menu.closeMenu();
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.paste"));

        this.rightClickContextMenu.addSeparatorEntry("separator_after_paste");

        this.rightClickContextMenu.addClickableEntry("cut", Component.translatable("fancymenu.ui.text_editor.cut"), (menu, entry) -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.cutHighlightedText());
            menu.closeMenu();
        }).setIsActiveSupplier((menu, entry) -> {
            if (!menu.isOpen()) return false;
            return this.selectedHoveredOnRightClickMenuOpen;
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.cut"));

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
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.select_all"));

    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {

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

        this.renderScreenBackground(pose);

        this.renderEditorAreaBackground(pose);

        //TODO use GuiComponent#enableScissor instead
        Window win = Minecraft.getInstance().getWindow();
        double scale = win.getGuiScale();
        int sciBottom = this.height - this.footerHeight;
        //Don't render parts of lines outside of editor area
        RenderSystem.enableScissor((int)(this.borderLeft * scale), (int)(win.getHeight() - (sciBottom * scale)), (int)(this.getEditorAreaWidth() * scale), (int)(this.getEditorAreaHeight() * scale));

        this.formattingRules.forEach((rule) -> rule.resetRule(this));
        this.currentRenderCharacterIndexTotal = 0;
        this.lineNumberRenderQueue.clear();
        //Update positions and size of lines and render them
        this.updateLines((line) -> {
            if (line.isInEditorArea()) {
                this.lineNumberRenderQueue.add(() -> this.renderLineNumber(pose, line));
            }
            line.render(pose, mouseX, mouseY, partial);
        });

        RenderSystem.disableScissor();

        this.renderLineNumberBackground(pose, this.borderLeft);

        RenderSystem.enableScissor(0, (int)(win.getHeight() - (sciBottom * scale)), (int)(this.borderLeft * scale), (int)(this.getEditorAreaHeight() * scale));
        for (Runnable r : this.lineNumberRenderQueue) {
            r.run();
        }
        RenderSystem.disableScissor();

        this.lastTickFocusedLineIndex = this.getFocusedLineIndex();
        this.triggeredFocusedLineWasTooHighInCursorPosMethod = false;

        UIBase.renderBorder(pose, this.borderLeft-1, this.headerHeight-1, this.getEditorAreaX() + this.getEditorAreaWidth(), this.height - this.footerHeight + 1, 1, this.editorAreaBorderColor, true, true, true, true);

        this.verticalScrollBar.render(pose);
        this.horizontalScrollBar.render(pose);

        this.renderPlaceholderMenu(pose, mouseX, mouseY, partial);

        this.cancelButton.render(pose, mouseX, mouseY, partial);

        this.doneButton.active = this.isTextValid();
        this.doneButton.setTooltip(this.textValidatorFeedbackTooltip);
        this.doneButton.render(pose, mouseX, mouseY, partial);

        this.renderMultilineNotSupportedNotification(pose, mouseX, mouseY, partial);

        this.rightClickContextMenu.render(pose, mouseX, mouseY, partial);

        this.tickMouseHighlighting();

        MutableComponent t = this.title.copy();
        t.setStyle(t.getStyle().withBold(this.boldTitle));
        this.font.draw(pose, t, this.borderLeft, (this.headerHeight / 2) - (this.font.lineHeight / 2), UIBase.getUIColorTheme().generic_text_base_color.getColorInt());

    }

    protected void renderMultilineNotSupportedNotification(PoseStack matrix, int mouseX, int mouseY, float partial) {
        long now = System.currentTimeMillis();
        if (!this.multilineMode && (this.multilineNotSupportedNotificationDisplayStart + 3000L >= now)) {
            int a = 255;
            int diff = (int) (this.multilineNotSupportedNotificationDisplayStart + 3000L - now);
            if (diff <= 1000) {
                float f = (float)diff / 1000F;
                a = Math.max(10, (int)(255F * f));
            }
            Color c = new Color(this.multilineNotSupportedNotificationColor.getRed(), this.multilineNotSupportedNotificationColor.getGreen(), this.multilineNotSupportedNotificationColor.getBlue(), a);
            this.font.draw(matrix, I18n.get("fancymenu.ui.text_editor.error.multiline_support"), this.borderLeft, this.headerHeight - this.font.lineHeight - 5, c.getRGB());
        }
    }

    protected void renderPlaceholderMenu(PoseStack matrix, int mouseX, int mouseY, float partial) {

        if (extendedPlaceholderMenu) {

            if (this.getTotalPlaceholderEntriesWidth() <= this.placeholderMenuWidth) {
                this.horizontalScrollBarPlaceholderMenu.setScroll(0.0F);
            }
            if (this.getTotalPlaceholderEntriesHeight() <= this.getEditorAreaHeight()) {
                this.verticalScrollBarPlaceholderMenu.setScroll(0.0F);
            }

            //Render placeholder menu background
            fill(matrix, this.width - this.borderRight - this.placeholderMenuWidth, this.getEditorAreaY(), this.width - this.borderRight, this.getEditorAreaY() + this.getEditorAreaHeight(), this.editorAreaBackgroundColor.getRGB());

            Window win = Minecraft.getInstance().getWindow();
            double scale = win.getGuiScale();
            int sciBottom = this.height - this.footerHeight;
            //Don't render parts of placeholder entries outside of placeholder menu area
            RenderSystem.enableScissor((int)((this.width - this.borderRight - this.placeholderMenuWidth) * scale), (int)(win.getHeight() - (sciBottom * scale)), (int)(this.placeholderMenuWidth * scale), (int)(this.getEditorAreaHeight() * scale));

            //Render placeholder entries
            List<PlaceholderMenuEntry> entries = new ArrayList<>();
            entries.addAll(this.placeholderMenuEntries);
            int index = 0;
            for (PlaceholderMenuEntry e : entries) {
                e.x = (this.width - this.borderRight - this.placeholderMenuWidth) + this.getPlaceholderEntriesRenderOffsetX();
                e.y = this.getEditorAreaY() + (this.placeholderMenuEntryHeight * index) + this.getPlaceholderEntriesRenderOffsetY();
                e.render(matrix, mouseX, mouseY, partial);
                index++;
            }

            RenderSystem.disableScissor();

            //Render placeholder menu border
            UIBase.renderBorder(matrix, this.width - this.borderRight - this.placeholderMenuWidth - 1, this.headerHeight-1, this.width - this.borderRight, this.height - this.footerHeight + 1, 1, this.editorAreaBorderColor, true, true, true, true);

            //Render placeholder menu scroll bars
            this.verticalScrollBarPlaceholderMenu.render(matrix);
            this.horizontalScrollBarPlaceholderMenu.render(matrix);

        }

        if (this.placeholderButton != null) {
            this.placeholderButton.render(matrix, mouseX, mouseY, partial);
        }

    }

    public int getTotalPlaceholderEntriesHeight() {
        return this.placeholderMenuEntryHeight * this.placeholderMenuEntries.size();
    }

    public int getTotalPlaceholderEntriesWidth() {
        int i = this.placeholderMenuWidth;
        for (PlaceholderMenuEntry e : this.placeholderMenuEntries) {
            if (e.getWidth() > i) {
                i = e.getWidth();
            }
        }
        return i;
    }

    public int getPlaceholderEntriesRenderOffsetX() {
        int totalScrollWidth = Math.max(0, this.getTotalPlaceholderEntriesWidth() - this.placeholderMenuWidth);
        return -(int)(((float)totalScrollWidth / 100.0F) * (this.horizontalScrollBarPlaceholderMenu.getScroll() * 100.0F));
    }

    public int getPlaceholderEntriesRenderOffsetY() {
        int totalScrollHeight = Math.max(0, this.getTotalPlaceholderEntriesHeight() - this.getEditorAreaHeight());
        return -(int)(((float)totalScrollHeight / 100.0F) * (this.verticalScrollBarPlaceholderMenu.getScroll() * 100.0F));
    }

    public void updatePlaceholderEntries(@Nullable String category, boolean clearList, boolean addBackButton) {

        if (clearList) {
            this.placeholderMenuEntries.clear();
        }

        Map<String, List<Placeholder>> categories = this.getPlaceholdersOrderedByCategories();
        if (!categories.isEmpty()) {
            List<Placeholder> otherCategory = categories.get(I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.other"));
            if (otherCategory != null) {

                if (category == null) {

                    //Add category entries
                    for (Map.Entry<String, List<Placeholder>> m : categories.entrySet()) {
                        if (m.getValue() != otherCategory) {
                            PlaceholderMenuEntry entry = new PlaceholderMenuEntry(this, Component.literal(m.getKey()), () -> {
                                this.updatePlaceholderEntries(m.getKey(), true, true);
                            });
                            entry.dotColor = this.placeholderEntryDotColorCategory;
                            entry.entryLabelColor = this.placeholderEntryLabelColor;
                            this.placeholderMenuEntries.add(entry);
                        }
                    }
                    //Add placeholder entries of the "Other" category to the end of the categories list (because other = no category)
                    this.updatePlaceholderEntries(I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.other"), false, false);

                } else {

                    if (addBackButton) {
                        PlaceholderMenuEntry backToCategoriesEntry = new PlaceholderMenuEntry(this, Component.literal(I18n.get("fancymenu.ui.text_editor.placeholders.back_to_categories")), () -> {
                            this.updatePlaceholderEntries(null, true, true);
                        });
                        backToCategoriesEntry.dotColor = this.placeholderEntryDotColorCategory;
                        backToCategoriesEntry.entryLabelColor = this.placeholderEntryBackToCategoriesLabelColor;
                        this.placeholderMenuEntries.add(backToCategoriesEntry);
                    }

                    List<Placeholder> placeholders = categories.get(category);
                    if (placeholders != null) {
                        for (Placeholder p : placeholders) {
                            PlaceholderMenuEntry entry = new PlaceholderMenuEntry(this, Component.literal(p.getDisplayName()), () -> {
                                this.pasteText(p.getDefaultPlaceholderString().toString());
                            });
                            List<String> desc = p.getDescription();
                            if (desc != null) {
                                entry.setDescription(desc.toArray(new String[0]));
                            }
                            entry.dotColor = this.placeholderEntryDotColorPlaceholder;
                            entry.entryLabelColor = this.placeholderEntryLabelColor;
                            this.placeholderMenuEntries.add(entry);
                        }
                    }

                }

                for (PlaceholderMenuEntry e : this.placeholderMenuEntries) {
                    e.backgroundColorIdle = this.placeholderEntryBackgroundColorIdle;
                    e.backgroundColorHover = this.placeholderEntryBackgroundColorHover;
                }

                this.verticalScrollBarPlaceholderMenu.setScroll(0.0F);
                this.horizontalScrollBarPlaceholderMenu.setScroll(0.0F);
            }
        }

    }

    protected Map<String, List<Placeholder>> getPlaceholdersOrderedByCategories() {
        //Build lists of all placeholders ordered by categories
        Map<String, List<Placeholder>> categories = new LinkedHashMap<>();
        for (Placeholder p : PlaceholderRegistry.getPlaceholders()) {
            String cat = p.getCategory();
            if (cat == null) {
                cat = I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.other");
            }
            List<Placeholder> l = categories.get(cat);
            if (l == null) {
                l = new ArrayList<>();
                categories.put(cat, l);
            }
            l.add(p);
        }
        //Move the Other category to the end
        List<Placeholder> otherCategory = categories.get(I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.other"));
        if (otherCategory != null) {
            categories.remove(I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.other"));
            categories.put(I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.other"), otherCategory);
        }
        return categories;
    }

    protected void renderLineNumberBackground(PoseStack matrix, int width) {
        fill(matrix, this.getEditorAreaX(), this.getEditorAreaY() - 1, this.getEditorAreaX() - width - 1, this.getEditorAreaY() + this.getEditorAreaHeight() + 1, this.sideBarColor.getRGB());
    }

    protected void renderLineNumber(PoseStack matrix, TextEditorLine line) {
        String lineNumberString = "" + (line.lineIndex+1);
        int lineNumberWidth = this.font.width(lineNumberString);
        this.font.draw(matrix, lineNumberString, this.getEditorAreaX() - 3 - lineNumberWidth, line.getY() + (line.getHeight() / 2) - (this.font.lineHeight / 2), line.isFocused() ? this.lineNumberTextColorFocused.getRGB() : this.lineNumberTextColorNormal.getRGB());
    }

    protected void renderEditorAreaBackground(PoseStack matrix) {
        fill(matrix, this.getEditorAreaX(), this.getEditorAreaY(), this.getEditorAreaX() + this.getEditorAreaWidth(), this.getEditorAreaY() + this.getEditorAreaHeight(), this.editorAreaBackgroundColor.getRGB());
    }

    protected void renderScreenBackground(PoseStack matrix) {
        fill(matrix, 0, 0, this.width, this.height, this.screenBackgroundColor.getRGB());
    }

    protected void tickMouseHighlighting() {

        if (!MouseInput.isLeftMouseDown()) {
            this.startHighlightLine = null;
            for (TextEditorLine t : this.textFieldLines) {
                t.isInMouseHighlightingMode = false;
            }
            return;
        }

        //Auto-scroll if mouse outside of editor area and in mouse-highlighting mode
        if (this.isInMouseHighlightingMode()) {
            int mX = MouseInput.getMouseX();
            int mY = MouseInput.getMouseY();
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
                        hovered.getAsAccessor().setShiftPressedFancyMenu(false);
                        hovered.moveCursorTo(hovered.getValue().length());
                    }
                } else if (firstIsBeforeHovered) {
                    this.setFocusedLine(this.getLineIndex(hovered));
                    if (!hovered.isInMouseHighlightingMode) {
                        hovered.isInMouseHighlightingMode = true;
                        hovered.getAsAccessor().setShiftPressedFancyMenu(false);
                        hovered.moveCursorTo(0);
                    }
                } else if (first == hovered) {
                    this.setFocusedLine(this.getLineIndex(first));
                }
            }

            int startIndex = Math.min(hoveredIndex, firstIndex);
            int endIndex = Math.max(hoveredIndex, firstIndex);
            int index = 0;
            for (TextEditorLine t : this.textFieldLines) {
                //Highlight all lines between the first and current line and remove highlighting from lines outside of highlight range
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
                        t.getAsAccessor().setShiftPressedFancyMenu(false);
                        t.moveCursorTo(0);
                        t.isInMouseHighlightingMode = false;
                    }
                }
                index++;
            }
            this.startHighlightLineIndex = startIndex;
            this.endHighlightLineIndex = endIndex;

            if (first != hovered) {
                first.getAsAccessor().setShiftPressedFancyMenu(true);
                if (firstIsAfterHovered) {
                    first.moveCursorTo(0);
                } else if (firstIsBeforeHovered) {
                    first.moveCursorTo(first.getValue().length());
                }
                first.getAsAccessor().setShiftPressedFancyMenu(false);
            }

        }

        TextEditorLine focused = this.getFocusedLine();
        if ((focused != null) && focused.isInMouseHighlightingMode) {
            if ((this.startHighlightLineIndex == -1) && (this.endHighlightLineIndex == -1)) {
                this.startHighlightLineIndex = this.getLineIndex(focused);
                this.endHighlightLineIndex = this.startHighlightLineIndex;
            }
            int i = Mth.floor(MouseInput.getMouseX()) - focused.getX();
            if (focused.getAsAccessor().getBorderedFancyMenu()) {
                i -= 4;
            }
            String s = this.font.plainSubstrByWidth(focused.getValue().substring(focused.getAsAccessor().getDisplayPosFancyMenu()), focused.getInnerWidth());
            focused.getAsAccessor().setShiftPressedFancyMenu(true);
            focused.moveCursorTo(this.font.plainSubstrByWidth(s, i).length() + focused.getAsAccessor().getDisplayPosFancyMenu());
            focused.getAsAccessor().setShiftPressedFancyMenu(false);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateCurrentLineWidth() {
        //Find width of the longest line and update current line width
        int longestTextWidth = 0;
        for (TextEditorLine f : this.textFieldLines) {
            if (f.textWidth > longestTextWidth) {
                //Calculating the text size for every line every tick kills the CPU, so I'm calculating the size on value change in the text box
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
        if (!this.multilineMode && (this.getLineCount() > 0)) {
            this.multilineNotSupportedNotificationDisplayStart = System.currentTimeMillis();
            return null;
        }
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
     * Returns the index of the focused line or -1 if no line is focused.
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

    @Nullable
    /** Returns the lines between two indexes, EXCLUDING start AND end indexes! **/
    public List<TextEditorLine> getLinesBetweenIndexes(int startIndex, int endIndex) {
        startIndex = Math.min(Math.max(startIndex, 0), this.textFieldLines.size()-1);
        endIndex = Math.min(Math.max(endIndex, 0), this.textFieldLines.size()-1);
        List<TextEditorLine> l = new ArrayList<>();
        l.addAll(this.textFieldLines.subList(startIndex, endIndex));
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
                    this.getFocusedLine().moveCursorTo(this.lastCursorPosSetByUser);
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
                if (isNewLine) {
                    //Split content of currentLine at cursor pos and move text after cursor to next line if ENTER was pressed
                    String textBeforeCursor = currentLine.getValue().substring(0, currentLine.getCursorPosition());
                    String textAfterCursor = currentLine.getValue().substring(currentLine.getCursorPosition());
                    currentLine.setValue(textBeforeCursor);
                    nextLine.setValue(textAfterCursor);
                    nextLine.moveCursorTo(0);
                    //Add amount of spaces of the beginning of the old line to the beginning of the new line
                    if (textBeforeCursor.startsWith(" ")) {
                        int spaces = 0;
                        for (char c : textBeforeCursor.toCharArray()) {
                            if (String.valueOf(c).equals(" ")) {
                                spaces++;
                            } else {
                                break;
                            }
                        }
                        nextLine.setValue(textBeforeCursor.substring(0, spaces) + nextLine.getValue());
                        nextLine.moveCursorTo(spaces);
                    }
                } else {
                    nextLine.moveCursorTo(this.lastCursorPosSetByUser);
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
            n.moveCursorTo(t.getCursorPosition());
            l.add(n);
        }
        return l;
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
                    lines.addAll(this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex));
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
                String ret = s.toString();
                return ret;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                    this.getLine(this.startHighlightLineIndex).insertText("");
                } else {
                    TextEditorLine start = this.getLine(this.startHighlightLineIndex);
                    start.insertText("");
                    TextEditorLine end = this.getLine(this.endHighlightLineIndex);
                    end.insertText("");
                    if ((this.endHighlightLineIndex - this.startHighlightLineIndex) > 1) {
                        for (TextEditorLine line : this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex)) {
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
        } catch (Exception e) {
            e.printStackTrace();
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
            if ((text != null) && !text.equals("")) {
                int addedLinesCount = 0;
                if (this.isTextHighlighted()) {
                    this.deleteHighlightedText();
                }
                if (!this.isLineFocused()) {
                    this.setFocusedLine(this.getLineCount()-1);
                    this.getFocusedLine().moveCursorToEnd();
                }
                TextEditorLine focusedLine = this.getFocusedLine();
                //These two strings are for correctly pasting text within a char sequence (if the cursor is not at the end or beginning of the line)
                String textBeforeCursor = "";
                String textAfterCursor = "";
                if (focusedLine.getValue().length() > 0) {
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
                if (!this.multilineMode && (lines.length > 1)) {
                    lines = new String[]{lines[0]};
                    this.multilineNotSupportedNotificationDisplayStart = System.currentTimeMillis();
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
                        this.getLine(index).insertText(s);
                        index++;
                    }
                    this.setFocusedLine(index - 1);
                    this.getFocusedLine().setCursorPosition(Math.max(0, this.getFocusedLine().getValue().length() - textAfterCursor.length()));
                    this.getFocusedLine().setHighlightPos(this.getFocusedLine().getCursorPosition());
                }
                this.correctYScroll(addedLinesCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.resetHighlighting();
    }

    public TextEditorScreen setText(@Nullable String text) {
        if (text == null) text = "";
        TextEditorLine t = this.getLine(0);
        this.textFieldLines.clear();
        this.textFieldLines.add(t);
        this.setFocusedLine(0);
        t.setValue("");
        t.moveCursorTo(0);
        this.pasteText(text);
        this.setFocusedLine(0);
        t.moveCursorTo(0);
        this.verticalScrollBar.setScroll(0.0F);
        return this;
    }

    @NotNull
    public String getText() {
        StringBuilder s = new StringBuilder();
        boolean b = false;
        for (TextEditorLine t : this.textFieldLines) {
            if (b) {
                s.append("\n");
            }
            s.append(t.getValue());
            b = true;
        }
        return s.toString();
    }

    protected boolean isTextValid() {
        if (this.textValidator != null) return this.textValidator.get(this);
        return true;
    }

    public TextEditorScreen setTextValidator(@Nullable ConsumingSupplier<TextEditorScreen, Boolean> textValidator) {
        this.textValidator = textValidator;
        return this;
    }

    public TextEditorScreen setTextValidatorUserFeedback(@Nullable Tooltip feedback) {
        this.textValidatorFeedbackTooltip = feedback;
        return this;
    }

    public boolean placeholdersAllowed() {
        return this.allowPlaceholders;
    }

    public TextEditorScreen setPlaceholdersAllowed(boolean allowed) {
        this.allowPlaceholders = allowed;
        this.init();
        return this;
    }

    public boolean isMultilineMode() {
        return this.multilineMode;
    }

    public TextEditorScreen setMultilineMode(boolean multilineMode) {
        this.multilineMode = multilineMode;
        return this;
    }

    public boolean isBoldTitle() {
        return this.boldTitle;
    }

    public TextEditorScreen setBoldTitle(boolean boldTitle) {
        this.boldTitle = boldTitle;
        return this;
    }

    /**
     * @return The text BEFORE the cursor or NULL if no line is focused.
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
                s.append(t.getValue().substring(0, t.getCursorPosition()));
            }
            b = true;
        }
        return s.toString();
    }

    /**
     * @return The text AFTER the cursor or NULL if no line is focused.
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
                s.append(t.getValue().substring(t.getCursorPosition(), t.getValue().length()));
            }
            b = true;
        }
        return s.toString();
    }

//    if (value != null) {
//        value = StringUtils.convertFormatCodes(value, "§", "&");
//        if (ScreenCustomization.isExistingGameDirectoryPath(value)) {
//            return ScreenCustomization.getAbsoluteGameDirectoryPath(value);
//        }
//    }

    @Override
    public boolean charTyped(char character, int modifiers) {

        for (TextEditorLine l : this.textFieldLines) {
            l.charTyped(character, modifiers);
        }

        return super.charTyped(character, modifiers);

    }


    @Override
    public boolean keyPressed(int keycode, int i1, int i2) {

        for (TextEditorLine l : new ArrayList<>(this.textFieldLines)) {
            l.keyPressed(keycode, i1, i2);
        }

        //ENTER
        if (keycode == 257) {
            if (!this.isInMouseHighlightingMode() && this.multilineMode) {
                if (this.isLineFocused()) {
                    this.resetHighlighting();
                    this.goDownLine(true);
                    this.correctYScroll(1);
                }
            }
            if (!this.multilineMode) {
                this.multilineNotSupportedNotificationDisplayStart = System.currentTimeMillis();
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
                    this.deleteHighlightedText();
                } else {
                    if (this.isLineFocused()) {
                        TextEditorLine focused = this.getFocusedLine();
                        focused.getAsAccessor().setShiftPressedFancyMenu(false);
                        focused.getAsAccessor().invokeDeleteTextFancyMenu(-1);
                        focused.getAsAccessor().setShiftPressedFancyMenu(Screen.hasShiftDown());
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
            Minecraft.getInstance().keyboardHandler.setClipboard(this.cutHighlightedText());
            this.resetHighlighting();
            return true;
        }
        //Reset highlighting when pressing left/right arrow keys
        if ((keycode == InputConstants.KEY_RIGHT) || (keycode == InputConstants.KEY_LEFT)) {
            this.resetHighlighting();
            return true;
        }

        return super.keyPressed(keycode, i1, i2);

    }

    @Override
    public boolean keyReleased(int i1, int i2, int i3) {

        for (TextEditorLine l : this.textFieldLines) {
            l.keyReleased(i1, i2, i3);
        }

        return super.keyReleased(i1, i2, i3);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        this.setFocused(null);

        if (super.mouseClicked(mouseX, mouseY, button)) return true;

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
                                if ((MouseInput.getMouseY() >= t.y) && (MouseInput.getMouseY() <= t.y + t.getHeight())) {
                                    focus = t;
                                    break;
                                }
                            }
                            this.setFocusedLine(this.getLineIndex(focus));
                            this.getFocusedLine().moveCursorToEnd();
                            this.correctYScroll(0);
                        } else if ((button == 1) && !isHighlightedHovered) {
                            //Focus line in case it is right-clicked
                            this.setFocusedLine(this.getLineIndex(hoveredLine));
                            //Set cursor in case line is right-clicked
                            String s = this.font.plainSubstrByWidth(hoveredLine.getValue().substring(hoveredLine.getAsAccessor().getDisplayPosFancyMenu()), hoveredLine.getInnerWidth());
                            hoveredLine.moveCursorTo(this.font.plainSubstrByWidth(s, MouseInput.getMouseX() - hoveredLine.getX()).length() + hoveredLine.getAsAccessor().getDisplayPosFancyMenu());
                        }
                    }
                    if (button == 1) {
                        this.selectedHoveredOnRightClickMenuOpen = this.isHighlightedTextHovered();
                        this.rightClickContextMenu.openMenuAtMouse();
                    } else if (this.rightClickContextMenu.isOpen() && !this.rightClickContextMenu.isHovered()) {
                        this.rightClickContextMenu.closeMenu();
                        //Call mouseClicked of lines after closing the menu, so the focused line and cursor pos gets updated
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
    public void onClose() {
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
        } catch (Exception e) {
            e.printStackTrace();
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

        //Don't fix scroll if in mouse-highlighting mode or no line is focused
        if (this.isInMouseHighlightingMode() || !this.isLineFocused()) {
            return;
        }

        int minY = this.getEditorAreaY();
        int maxY = this.getEditorAreaY() + this.getEditorAreaHeight();
        int currentLineY = this.getFocusedLine().getY();

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

            int oldX = line.x;

            this.updateCurrentLineWidth();
            this.updateLines(null);

            int newX = line.x;
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
                //By default, move back the line just a little when moving the cursor to the left side by using the mouse or arrow keys
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
        int mX = MouseInput.getMouseX();
        int mY = MouseInput.getMouseY();
        return (mX >= xStart) && (mX <= xEnd) && (mY >= yStart) && (mY <= yEnd);
    }

    public int getEditorAreaWidth() {
        int i = (this.width - this.borderRight) - this.borderLeft;
        if (extendedPlaceholderMenu) {
            i = i - this.placeholderMenuWidth - 15;
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

    public class PlaceholderMenuEntry extends UIBase {

        public TextEditorScreen parent;
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

        public PlaceholderMenuEntry(@NotNull TextEditorScreen parent, @NotNull Component label, @NotNull Runnable clickAction) {
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
                public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                    if (PlaceholderMenuEntry.this.parent.isMouseInteractingWithPlaceholderGrabbers()) {
                        this.isHovered = false;
                    }
                    super.render(p_93657_, p_93658_, p_93659_, p_93660_);
                }
            };
        }

        public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {
            //Update the button colors
            this.buttonBase.setBackground(ExtendedButton.ColorButtonBackground.create(DrawableColor.of(this.backgroundColorIdle), DrawableColor.of(this.backgroundColorHover), DrawableColor.of(this.backgroundColorIdle), DrawableColor.of(this.backgroundColorHover)));
            //Update the button pos
            this.buttonBase.x = this.x;
            this.buttonBase.y = this.y;
            int yCenter = this.y + (this.getHeight() / 2);
            //Render the button
            this.buttonBase.render(matrix, mouseX, mouseY, partial);
            //Render dot
            renderListingDot(matrix, this.x + 5, yCenter - 2, this.dotColor);
            //Render label
            this.font.draw(matrix, this.label, this.x + 5 + 4 + 3, yCenter - (this.font.lineHeight / 2), this.entryLabelColor.getRGB());
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
            this.buttonBase.setTooltip(Tooltip.of(desc).setDefaultStyle());
        }

    }

}