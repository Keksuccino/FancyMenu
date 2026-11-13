package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownMarkerShape;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.EnumOption;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.LazyContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Adds the sub menu of the {@link MarkdownSettings} of an element to the menu of its editor element:
 * the switch for reading Markdown, the look, and one sub menu for every group of style options.<br>
 * {@link #LAYOUT} says which entry stands where. The entries themselves are the ones the properties of the settings build,
 * so they work for several selected elements at once and can get undone like every other change in the editor.
 */
public final class MarkdownSettingsMenu<H extends PropertyHolder> {

    /** The identifier of the entry of the sub menu in the menu of the editor element. */
    public static final String MENU_IDENTIFIER = "markdown";
    private static final String LOCALIZATION_KEY_BASE = "fancymenu.elements.text.markdown.";
    private static final String MENU_LOCALIZATION_KEY_BASE = LOCALIZATION_KEY_BASE + "style.menu.";
    /** Options with more values than this get a list to pick from, because clicking through all of them takes too long. */
    static final int MOST_VALUES_TO_CYCLE_THROUGH = 4;

    /**
     * The entries of the settings in the order they show up in, sub menus included. Every option of the style stands in it exactly once.<br>
     * The options of the headline levels, the list levels and the callout types have a sub menu per level or type,
     * and what switches a kind of Markdown syntax on stands in front of the options of that syntax.
     */
    static final List<Item> LAYOUT = List.of(
            toggle(settings -> settings.parseMarkdown, MaterialIcons.CODE),
            Separator.INSTANCE,
            Look.INSTANCE,
            Separator.INSTANCE,
            menu("text", MaterialIcons.TEXT_FIELDS,
                    option(style -> style.textColor),
                    option(style -> style.boldTextColor),
                    option(style -> style.textShadow),
                    Separator.INSTANCE,
                    option(style -> style.padding),
                    option(style -> style.lineSpacing),
                    option(style -> style.paragraphSpaceAbove),
                    option(style -> style.paragraphSpaceBelow),
                    Separator.INSTANCE,
                    option(style -> style.blankLineMode),
                    option(style -> style.collapseBlankLines),
                    option(style -> style.blockGapMode),
                    Separator.INSTANCE,
                    menu("text_fine_tuning", MaterialIcons.TUNE,
                            option(style -> style.scaleSizesWithText),
                            option(style -> style.smoothCorners),
                            option(style -> style.blockEdgeInset),
                            Separator.INSTANCE,
                            option(style -> style.snapTextScale),
                            option(style -> style.snapPositions),
                            option(style -> style.snapStrikethrough),
                            option(style -> style.trimGlyphSpacing),
                            option(style -> style.fullLineHeight))),
            menu("headlines", MaterialIcons.TITLE,
                    headlineLevel(1),
                    headlineLevel(2),
                    headlineLevel(3),
                    Separator.INSTANCE,
                    option(style -> style.headlineRuleFadeLength),
                    option(style -> style.headlineRuleShadowColor),
                    option(style -> style.headlineLeaderGap)),
            menu("lists", MaterialIcons.FORMAT_LIST_BULLETED,
                    listLevel(1),
                    listLevel(2),
                    listLevel(3),
                    Separator.INSTANCE,
                    option(style -> style.listIndent),
                    option(style -> style.listTextOffset),
                    option(style -> style.listMarkerGap),
                    option(style -> style.listMarkerSize),
                    option(style -> style.listMarkerAlignment),
                    Separator.INSTANCE,
                    option(style -> style.listItemSpacing),
                    option(style -> style.listSpaceAbove),
                    option(style -> style.listSpaceBelow),
                    Separator.INSTANCE,
                    menu("numbered_lists", MaterialIcons.FORMAT_LIST_NUMBERED,
                            toggle(settings -> settings.numberedLists, MaterialIcons.FORMAT_LIST_NUMBERED),
                            Separator.INSTANCE,
                            option(style -> style.listNumberColor),
                            option(style -> style.listNumberAlignment),
                            option(style -> style.listNumberIndent),
                            option(style -> style.listNumberGap)),
                    menu("extended_list_markers", MaterialIcons.INTERESTS,
                            toggle(settings -> settings.extendedListMarkers, MaterialIcons.INTERESTS),
                            Separator.INSTANCE,
                            option(style -> style.listPlusMarkerShape),
                            option(style -> style.listPlusMarkerColor),
                            Separator.INSTANCE,
                            option(style -> style.listMinusMarkerShape),
                            option(style -> style.listMinusMarkerColor),
                            option(style -> style.listMinusMarkerMode),
                            Separator.INSTANCE,
                            option(style -> style.listTildeMarkerShape),
                            option(style -> style.listTildeMarkerColor),
                            Separator.INSTANCE,
                            option(style -> style.listAsteriskMarkerShape),
                            option(style -> style.listAsteriskMarkerColor))),
            menu("links", MaterialIcons.LINK,
                    option(style -> style.hyperlinkColor),
                    option(style -> style.clickEventColor),
                    option(style -> style.hoverEventColor),
                    Separator.INSTANCE,
                    option(style -> style.linkUnderline),
                    option(style -> style.linkUnderlineColor),
                    Separator.INSTANCE,
                    option(style -> style.linkHoverColor),
                    option(style -> style.linkHoverUnderlineColor)),
            menu("code", MaterialIcons.CODE_BLOCKS,
                    menu("inline_code", MaterialIcons.DATA_OBJECT,
                            option(style -> style.inlineCodeBackgroundColor),
                            option(style -> style.inlineCodeTextColor),
                            Separator.INSTANCE,
                            option(style -> style.inlineCodePaddingLeft),
                            option(style -> style.inlineCodePaddingRight),
                            option(style -> style.inlineCodePaddingTop),
                            option(style -> style.inlineCodePaddingBottom)),
                    menu("code_blocks", MaterialIcons.TERMINAL,
                            option(style -> style.codeBlockBackgroundColor),
                            option(style -> style.codeBlockTextColor),
                            Separator.INSTANCE,
                            option(style -> style.codeBlockFrameTopColor),
                            option(style -> style.codeBlockFrameBottomColor),
                            option(style -> style.codeBlockInnerShadowColor),
                            Separator.INSTANCE,
                            option(style -> style.codeBlockPaddingHorizontal),
                            option(style -> style.codeBlockPaddingVertical),
                            Separator.INSTANCE,
                            option(style -> style.codeBlockFixedWidth),
                            option(style -> style.codeBlockCellWidth),
                            Separator.INSTANCE,
                            option(style -> style.codeBlockSpaceAbove),
                            option(style -> style.codeBlockSpaceBelow)),
                    Separator.INSTANCE,
                    option(style -> style.codeFont),
                    option(style -> style.codeCornerRadius)),
            menu("quotes", MaterialIcons.FORMAT_QUOTE,
                    option(style -> style.quoteTextColor),
                    option(style -> style.quoteItalic),
                    option(style -> style.quoteIndent),
                    Separator.INSTANCE,
                    menu("quote_bar", MaterialIcons.BORDER_LEFT,
                            option(style -> style.quoteBarColor),
                            option(style -> style.quoteBarWidth),
                            option(style -> style.quoteBarPosition),
                            option(style -> style.quoteBarOverhangTop),
                            option(style -> style.quoteBarOverhangBottom)),
                    menu("quote_background", MaterialIcons.TEXTURE,
                            option(style -> style.quoteBackgroundColor),
                            option(style -> style.quoteCornerRadius),
                            Separator.INSTANCE,
                            option(style -> style.quotePaddingTop),
                            option(style -> style.quotePaddingBottom),
                            option(style -> style.quotePaddingSide)),
                    Separator.INSTANCE,
                    option(style -> style.quoteSpaceAbove),
                    option(style -> style.quoteSpaceBelow)),
            menu("callouts", MaterialIcons.CAMPAIGN,
                    toggle(settings -> settings.callouts, MaterialIcons.CAMPAIGN),
                    Separator.INSTANCE,
                    calloutType(0, "note", MaterialIcons.INFO),
                    calloutType(1, "tip", MaterialIcons.LIGHTBULB),
                    calloutType(2, "important", MaterialIcons.PRIORITY_HIGH),
                    calloutType(3, "warning", MaterialIcons.WARNING),
                    calloutType(4, "caution", MaterialIcons.REPORT),
                    Separator.INSTANCE,
                    option(style -> style.calloutTextColor),
                    option(style -> style.calloutLabelBold),
                    option(style -> style.calloutHeaderGap),
                    Separator.INSTANCE,
                    option(style -> style.calloutIcons),
                    option(style -> style.calloutIconDetailColor),
                    option(style -> style.calloutIconGap)),
            menu("tables", MaterialIcons.TABLE,
                    menu("table_header", MaterialIcons.TABLE_VIEW,
                            option(style -> style.tableShowHeader),
                            option(style -> style.tableHeaderBackgroundColor),
                            option(style -> style.tableHeaderTextColor),
                            option(style -> style.tableHeaderBold),
                            Separator.INSTANCE,
                            option(style -> style.tableHeaderSeparatorColor),
                            option(style -> style.tableHeaderSeparatorThickness)),
                    menu("table_rows", MaterialIcons.TABLE_ROWS,
                            option(style -> style.tableRowBackgroundColor),
                            option(style -> style.tableAlternateRows),
                            option(style -> style.tableAlternateRowBackgroundColor),
                            option(style -> style.tableBackgroundColor)),
                    menu("table_lines", MaterialIcons.GRID_4X4,
                            option(style -> style.tableGrid),
                            option(style -> style.tableLineColor),
                            option(style -> style.tableLineThickness),
                            option(style -> style.tableCornerRadius)),
                    menu("table_cell_padding", MaterialIcons.PADDING_ICON,
                            option(style -> style.tableCellPaddingLeft),
                            option(style -> style.tableCellPaddingRight),
                            option(style -> style.tableCellPaddingTop),
                            option(style -> style.tableCellPaddingBottom)),
                    Separator.INSTANCE,
                    option(style -> style.tableSpaceAbove),
                    option(style -> style.tableSpaceBelow)),
            menu("images", MaterialIcons.IMAGE,
                    option(style -> style.imageScaling),
                    option(style -> style.imageMaxScale),
                    option(style -> style.imageAlignment),
                    Separator.INSTANCE,
                    option(style -> style.imageFrameWidth),
                    option(style -> style.imageFrameOuterColor),
                    option(style -> style.imageFrameInnerColor),
                    Separator.INSTANCE,
                    option(style -> style.imageCaption),
                    option(style -> style.imageCaptionColor),
                    option(style -> style.imageCaptionGap),
                    Separator.INSTANCE,
                    option(style -> style.imageSpaceAbove),
                    option(style -> style.imageSpaceBelow)),
            menu("separators", MaterialIcons.HORIZONTAL_RULE,
                    option(style -> style.separatorStyle),
                    option(style -> style.separatorColor),
                    option(style -> style.separatorThickness),
                    Separator.INSTANCE,
                    option(style -> style.separatorOrnamentShape),
                    option(style -> style.separatorOrnamentColor),
                    option(style -> style.separatorOrnamentSize),
                    option(style -> style.separatorOrnamentGap),
                    option(style -> style.separatorReach),
                    option(style -> style.separatorFadeLength),
                    Separator.INSTANCE,
                    option(style -> style.separatorSpaceAbove),
                    option(style -> style.separatorSpaceBelow))
    );

    private final ContextMenuBuilder<H> builder;
    private final Function<H, MarkdownSettings> settingsGetter;
    /** The settings of the element the menu belongs to. Entries that change something ask every selected element for its own settings. */
    private final MarkdownSettings settings;
    private int separatorCount = 0;

    private MarkdownSettingsMenu(@NotNull ContextMenuBuilder<H> builder, @NotNull Function<H, MarkdownSettings> settingsGetter) {
        this.builder = builder;
        this.settingsGetter = settingsGetter;
        this.settings = settingsGetter.apply(builder.self());
    }

    /**
     * Adds the sub menu with all entries of the Markdown settings to the given menu.<br>
     * The sub menu builds its entries when they are needed for the first time. They are far more than the rest of an editor element has,
     * and the editor builds the menus of all its elements again whenever it sets itself up, while most of them never get opened.
     *
     * @param menu The menu of the editor element.
     * @param builder The editor element the menu belongs to.
     * @param settingsGetter Returns the Markdown settings of an editor element of the same kind.
     */
    public static <H extends PropertyHolder> void addTo(@NotNull ContextMenu menu, @NotNull ContextMenuBuilder<H> builder, @NotNull Function<H, MarkdownSettings> settingsGetter) {
        ContextMenu settingsMenu = new LazyContextMenu(lazyMenu -> {
            MarkdownSettingsMenu<H> entries = new MarkdownSettingsMenu<>(builder, settingsGetter);
            // Entries that reset a style property give it the value of the look of the element
            entries.settings.updateResetValues();
            entries.addItems(lazyMenu, LAYOUT);
        });
        menu.addSubMenuEntry(MENU_IDENTIFIER, Component.translatable("fancymenu.elements.text.markdown"), settingsMenu)
                .setStackable(true)
                .setIcon(MaterialIcons.MENU_BOOK);
    }

    private void addItems(@NotNull ContextMenu menu, @NotNull List<Item> items) {
        for (Item item : items) {
            if (item instanceof OptionItem optionItem) {
                this.addOptionEntry(menu, optionItem.option().apply(this.settings.getStyle()));
            } else if (item instanceof ToggleItem toggleItem) {
                Property.BooleanProperty property = toggleItem.property().apply(this.settings);
                describe(property.buildContextMenuEntryAndAddTo(menu, this.builder).setStackable(true).setIcon(toggleItem.icon()), property.getContextMenuEntryLocalizationKeyBase());
            } else if (item instanceof MenuItem menuItem) {
                ContextMenu subMenu = new ContextMenu();
                String localizationKey = MENU_LOCALIZATION_KEY_BASE + menuItem.name();
                describe(menu.addSubMenuEntry("markdown_" + menuItem.name(), Component.translatable(localizationKey), subMenu).setStackable(true).setIcon(menuItem.icon()), localizationKey);
                this.addItems(subMenu, menuItem.items());
            } else if (item == Look.INSTANCE) {
                this.addLookEntry(menu);
            } else {
                menu.addSeparatorEntry("markdown_separator_" + (this.separatorCount++)).setStackable(true);
            }
        }
    }

    private void addOptionEntry(@NotNull ContextMenu menu, @NotNull MarkdownStyleOption<?> option) {
        Property<?> property = this.settings.getProperty(option);
        ContextMenu.ClickableContextMenuEntry<?> entry;
        if ((option instanceof EnumOption<?> enumOption) && (enumOption.getValues().length > MOST_VALUES_TO_CYCLE_THROUGH)) {
            entry = this.addChoiceEntry(menu, enumOption);
        } else if (option.getType() == MarkdownStyleOption.Type.FONT) {
            entry = this.addFontEntry(menu, property);
        } else {
            entry = property.buildContextMenuEntryAndAddTo(menu, this.builder);
        }
        describe(entry.setStackable(true).setIcon(getIcon(option)), property.getContextMenuEntryLocalizationKeyBase());
    }

    /**
     * The entry that shows the look that got applied last, with one entry per look that applies it to all selected elements.
     */
    private void addLookEntry(@NotNull ContextMenu menu) {
        ContextMenu lookMenu = new ContextMenu();
        String localizationKey = this.settings.look.getContextMenuEntryLocalizationKeyBase();
        describe(menu.addSubMenuEntry("markdown_look", Component.empty(), lookMenu)
                .setLabelSupplier(new ValueLabel(localizationKey, this.settings::getLook))
                .setStackable(true)
                .setIcon(MaterialIcons.STYLE), localizationKey);
        for (MarkdownLook look : MarkdownLook.values()) {
            String lookKey = MarkdownSettings.getValueLocalizationKey(look);
            describe(lookMenu.addClickableEntry("look_" + look.name().toLowerCase(Locale.ROOT), Component.translatable(lookKey), (contextMenu, entry) -> this.applyToStack(entry, MarkdownSettings.LOOK_KEY))
                    .setStackable(true)
                    .setStackApplier((stackEntry, value) -> this.settingsGetter.apply(this.builder.self()).applyLook(look))
                    .setIcon(getIcon(look)), lookKey);
        }
    }

    /**
     * An entry with the current value of an option in its label, and a sub menu with one entry for every value the option can have.
     */
    @NotNull
    private <E extends Enum<E>> ContextMenu.ClickableContextMenuEntry<?> addChoiceEntry(@NotNull ContextMenu menu, @NotNull EnumOption<E> option) {
        Property<?> property = this.settings.getProperty(option);
        String key = property.getKey();
        ContextMenu choiceMenu = new ContextMenu();
        for (E value : option.getValues()) {
            ContextMenu.ClickableContextMenuEntry<?> choice = choiceMenu.addClickableEntry("value_" + value.name().toLowerCase(Locale.ROOT), Component.translatable(MarkdownSettings.getValueLocalizationKey(value)), (contextMenu, entry) -> this.applyToStack(entry, key))
                    .setStackable(true)
                    .setStackApplier((stackEntry, ignored) -> {
                        Property<?> resolved = this.builder.self().getProperty(key);
                        if (resolved != null) resolved.forceSet(value);
                    });
            if (value instanceof MarkdownMarkerShape shape) choice.setIcon(getIcon(shape));
        }
        return menu.addSubMenuEntry("menu_entry_" + key, Component.empty(), choiceMenu)
                .setLabelSupplier(new ValueLabel(property.getContextMenuEntryLocalizationKeyBase(), () -> (Enum<?>) property.get()));
    }

    /**
     * An entry that asks for the identifier of a font. No text at all stands for no font.
     */
    @NotNull
    private ContextMenu.ClickableContextMenuEntry<?> addFontEntry(@NotNull ContextMenu menu, @NotNull Property<?> property) {
        String key = property.getKey();
        return this.builder.addGenericStringInputContextMenuEntryTo(menu, "menu_entry_" + key,
                holder -> holder.getProperty(key) != null,
                holder -> {
                    Property<?> resolved = holder.getProperty(key);
                    // No value makes the entry show that there is none, which an empty text would not
                    return ((resolved != null) && (resolved.get() instanceof String font) && !font.isEmpty()) ? font : null;
                },
                (holder, font) -> {
                    Property<?> resolved = holder.getProperty(key);
                    // The option to reset the entry sends no text, because the font a property resets to depends on the look of its element
                    if (resolved != null) resolved.forceSet((font != null) ? font.trim() : resolved.getDefault());
                },
                null, false, false, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, null,
                font -> font.isBlank() || (Identifier.tryParse(font.trim()) != null), null);
    }

    /**
     * Runs the stack appliers of the given entry for all selected elements that have the given property, as one step that can get undone.
     */
    private void applyToStack(@NotNull ContextMenu.ContextMenuEntry<?> entry, @NotNull String propertyKey) {
        ContextMenuBuilder.StackContext<H> stack = this.builder.stack(entry, holder -> holder.getProperty(propertyKey) != null);
        if (!stack.isPrimary() || stack.isEmpty()) return;
        this.builder.saveSnapshot();
        this.builder.applyStackAppliers(entry, null);
    }

    /**
     * Gives the entry the description of the given localization key as tooltip, if it has one.
     */
    private static void describe(@NotNull ContextMenu.ContextMenuEntry<?> entry, @NotNull String localizationKey) {
        String descriptionKey = localizationKey + ".desc";
        if (LocalizationUtils.isLocalizationKey(descriptionKey)) entry.setTooltipSupplier((menu, entryRef) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(descriptionKey)));
    }

    @NotNull
    private static MaterialIcon getIcon(@NotNull MarkdownStyleOption<?> option) {
        String key = option.getKey();
        switch (option.getType()) {
            case COLOR:
                return MaterialIcons.PALETTE;
            case FONT:
                return MaterialIcons.FONT_DOWNLOAD;
            case ENUM:
                return (option.getLookValue(MarkdownLook.CLASSIC) instanceof MarkdownMarkerShape) ? MaterialIcons.CATEGORY : MaterialIcons.REPEAT;
            case BOOLEAN:
                if (key.endsWith("bold")) return MaterialIcons.FORMAT_BOLD;
                if (key.endsWith("italic")) return MaterialIcons.FORMAT_ITALIC;
                if (key.endsWith("shadow")) return MaterialIcons.SHADOW;
                return MaterialIcons.TOGGLE_ON;
            default:
                if (key.endsWith("_space_above")) return MaterialIcons.VERTICAL_ALIGN_TOP;
                if (key.endsWith("_space_below")) return MaterialIcons.VERTICAL_ALIGN_BOTTOM;
                if (key.endsWith("_spacing")) return MaterialIcons.FORMAT_LINE_SPACING;
                if (key.contains("padding") || key.endsWith("_inset")) return MaterialIcons.PADDING_ICON;
                if (key.endsWith("_gap") || key.endsWith("decoration_offset")) return MaterialIcons.SPACE_BAR;
                if (key.endsWith("indent") || key.endsWith("_offset")) return MaterialIcons.FORMAT_INDENT_INCREASE;
                if (key.endsWith("_max_scale")) return MaterialIcons.ZOOM_IN;
                if (key.endsWith("_scale") || key.endsWith("_size")) return MaterialIcons.FORMAT_SIZE;
                if (key.endsWith("_cell_width") || key.endsWith("_reach")) return MaterialIcons.ARROW_RANGE;
                if (key.endsWith("_thickness") || key.endsWith("_width")) return MaterialIcons.LINE_WEIGHT;
                if (key.endsWith("_radius")) return MaterialIcons.ROUNDED_CORNER;
                if (key.endsWith("_fade_length")) return MaterialIcons.GRADIENT;
                return MaterialIcons.HEIGHT;
        }
    }

    @NotNull
    private static MaterialIcon getIcon(@NotNull MarkdownLook look) {
        return (look == MarkdownLook.PIXEL) ? MaterialIcons.GRID_ON : (look == MarkdownLook.NEUTRAL) ? MaterialIcons.CONTRAST : MaterialIcons.HISTORY;
    }

    /**
     * An icon that comes close to what the shape looks like, so the list of shapes is quick to scan.
     */
    @NotNull
    private static MaterialIcon getIcon(@NotNull MarkdownMarkerShape shape) {
        switch (shape) {
            case SQUARE:
                return MaterialIcons.SQUARE;
            case CURSOR:
                return MaterialIcons.PLAY_ARROW;
            case CURSOR_SMALL:
                return MaterialIcons.ARROW_RIGHT;
            case DOT:
                return MaterialIcons.FIBER_MANUAL_RECORD;
            case PLUS:
                return MaterialIcons.ADD;
            case MINUS:
                return MaterialIcons.REMOVE;
            case WAVE:
                return MaterialIcons.WAVES;
            case STAR:
                return MaterialIcons.STAR;
            case DIAMOND:
                return MaterialIcons.STAT_0;
            case DISC:
                return MaterialIcons.CIRCLE;
            case RING:
                return MaterialIcons.RADIO_BUTTON_UNCHECKED;
            case ICON_ADD:
                return MaterialIcons.ADD_CIRCLE;
            case ICON_REMOVE:
                return MaterialIcons.DO_NOT_DISTURB_ON;
            case ICON_EDIT:
                return MaterialIcons.EDIT;
            case ICON_STAR:
                return MaterialIcons.STARS;
            default:
                return MaterialIcons.BLOCK;
        }
    }

    @NotNull
    private static Item option(@NotNull Function<MarkdownStyle, MarkdownStyleOption<?>> option) {
        return new OptionItem(option);
    }

    @NotNull
    private static Item toggle(@NotNull Function<MarkdownSettings, Property.BooleanProperty> property, @NotNull MaterialIcon icon) {
        return new ToggleItem(property, icon);
    }

    @NotNull
    private static Item menu(@NotNull String name, @NotNull MaterialIcon icon, @NotNull Item... items) {
        return new MenuItem(name, icon, List.of(items));
    }

    @NotNull
    private static Item headlineLevel(int level) {
        return menu("headline_" + level, (level == 1) ? MaterialIcons.LOOKS_ONE : (level == 2) ? MaterialIcons.LOOKS_TWO : MaterialIcons.LOOKS_3,
                option(style -> style.headline(level).scale),
                option(style -> style.headline(level).color),
                option(style -> style.headline(level).bold),
                Separator.INSTANCE,
                option(style -> style.headline(level).decoration),
                option(style -> style.headline(level).decorationColor),
                option(style -> style.headline(level).decorationThickness),
                option(style -> style.headline(level).decorationOffset),
                option(style -> style.headline(level).linePadding),
                Separator.INSTANCE,
                option(style -> style.headline(level).spaceAbove),
                option(style -> style.headline(level).spaceBelow));
    }

    @NotNull
    private static Item listLevel(int level) {
        return menu("list_level_" + level, (level == 1) ? MaterialIcons.LOOKS_ONE : (level == 2) ? MaterialIcons.LOOKS_TWO : MaterialIcons.LOOKS_3,
                option(style -> style.listLevel(level).markerShape),
                option(style -> style.listLevel(level).markerColor));
    }

    @NotNull
    private static Item calloutType(int type, @NotNull String name, @NotNull MaterialIcon icon) {
        return menu("callout_" + name, icon,
                option(style -> style.callout(type).color),
                option(style -> style.callout(type).backgroundColor));
    }

    /**
     * One line of a menu.
     */
    interface Item {
    }

    /**
     * The entry of a style option.
     */
    record OptionItem(@NotNull Function<MarkdownStyle, MarkdownStyleOption<?>> option) implements Item {
    }

    /**
     * The entry of one of the switches of the settings that are no part of the style.
     */
    record ToggleItem(@NotNull Function<MarkdownSettings, Property.BooleanProperty> property, @NotNull MaterialIcon icon) implements Item {
    }

    /**
     * A sub menu.
     *
     * @param name What the identifier of its entry and the localization key of its name end with.
     */
    record MenuItem(@NotNull String name, @NotNull MaterialIcon icon, @NotNull List<Item> items) implements Item {
    }

    enum Separator implements Item {

        INSTANCE

    }

    /**
     * The entry of the look.
     */
    enum Look implements Item {

        INSTANCE

    }

    /**
     * A label that shows the name of an entry and the name of its current value, like "Marker: Star".<br>
     * Context menus ask for the label of an entry several times per frame, and the UI font caches text measurements per component instance,
     * so the label stays the same component until its value or the color of the UI theme changes.
     */
    private static final class ValueLabel implements ContextMenu.Supplier<Component> {

        private final String localizationKey;
        private final Supplier<Enum<?>> value;
        @Nullable
        private Component label;
        @Nullable
        private Enum<?> labelValue;
        private int labelColor;

        private ValueLabel(@NotNull String localizationKey, @NotNull Supplier<Enum<?>> value) {
            this.localizationKey = localizationKey;
            this.value = value;
        }

        @Override
        public Component get(ContextMenu menu, ContextMenu.ContextMenuEntry<?> entry) {
            Enum<?> value = this.value.get();
            int color = UIBase.getUITheme().warning_color.getColorInt();
            if ((this.label == null) || (value != this.labelValue) || (color != this.labelColor)) {
                Component valueName = (value != null) ? Component.translatable(MarkdownSettings.getValueLocalizationKey(value)).setStyle(Style.EMPTY.withColor(color)) : Component.literal("---");
                this.label = Component.translatable(this.localizationKey, valueName);
                this.labelValue = value;
                this.labelColor = color;
            }
            return this.label;
        }

    }

}
