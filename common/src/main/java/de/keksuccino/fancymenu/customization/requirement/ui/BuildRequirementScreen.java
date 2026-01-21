package de.keksuccino.fancymenu.customization.requirement.ui;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class BuildRequirementScreen extends PiPWindowBody implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

    protected RequirementContainer parent;
    protected final RequirementInstance instance;
    protected boolean isEdit;
    protected Consumer<RequirementInstance> callback;

    protected ScrollArea requirementsListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea descriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedEditBox searchBar;

    private static final Comparator<Requirement> REQUIREMENT_DISPLAY_NAME_COMPARATOR = Comparator
            .comparing((Requirement requirement) -> requirement.getDisplayName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Requirement::getDisplayName)
            .thenComparing(Requirement::getIdentifier);

    public BuildRequirementScreen(@NotNull RequirementContainer parent, @Nullable RequirementInstance instanceToEdit, @NotNull Consumer<RequirementInstance> callback) {

        super((instanceToEdit != null) ? Component.translatable("fancymenu.requirements.screens.edit_requirement") : Component.translatable("fancymenu.requirements.screens.add_requirement"));

        this.parent = parent;
        this.instance = (instanceToEdit != null) ? instanceToEdit : new RequirementInstance(null, null, RequirementInstance.RequirementMode.IF, parent);
        this.isEdit = instanceToEdit != null;
        this.callback = callback;

    }

    @SuppressWarnings("all")
    @Override
    protected void init() {
        boolean blur = UIBase.shouldBlur();

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 20 + 1, 50 + 15 + 1, (this.width / 2) - 40 - 2, 20 - 2, Component.empty());
        this.searchBar.setHintFancyMenu(consumes -> Component.translatable("fancymenu.requirements.build_requirement.screen.search_requirement"));
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updateRequirementsList());
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar, blur);
        this.setupInitialFocusWidget(this, this.searchBar);

        // Set positions for scroll areas
        this.requirementsListScrollArea.setSetupForBlurInterface(blur);
        this.requirementsListScrollArea.setWidth((this.width / 2) - 40, true);
        this.requirementsListScrollArea.setHeight(this.height - 85 - 25, true);
        this.requirementsListScrollArea.setX(20, true);
        this.requirementsListScrollArea.setY(50 + 15 + 25, true);
        this.addRenderableWidget(this.requirementsListScrollArea);

        this.descriptionScrollArea.setSetupForBlurInterface(blur);
        this.descriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.descriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.descriptionScrollArea.setX(this.width - 20 - this.descriptionScrollArea.getWidthWithBorder(), true);
        this.descriptionScrollArea.setY(50 + 15, true);
        this.descriptionScrollArea.horizontalScrollBar.active = false;
        this.addRenderableWidget(this.descriptionScrollArea);

        // Calculate button positions
        int doneButtonX = this.width - 20 - 150; // 150 is button width
        int doneButtonY = this.height - 20 - 20;
        int cancelButtonX = doneButtonX;
        int cancelButtonY = doneButtonY - 5 - 20;
        int editValueButtonX = doneButtonX;
        int editValueButtonY = (this.isEdit ? doneButtonY : cancelButtonY) - 15 - 20;
        int requirementModeButtonX = doneButtonX;
        int requirementModeButtonY = editValueButtonY - 5 - 20;

        // Create buttons with positions in constructors
        ExtendedButton editValueButton = new ExtendedButton(editValueButtonX, editValueButtonY, 150, 20, Component.translatable("fancymenu.requirements.screens.build_screen.edit_value"), (button) -> {
            if (this.instance.requirement != null) {
                this.instance.requirement.editValue(this, this.instance);
            }
        }).setUITooltipSupplier(consumes -> {
            if ((this.instance.requirement != null) && !this.instance.requirement.hasValue()) {
                return UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.requirements.screens.build_screen.edit_value.desc.no_value"));
            }
            return UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.requirements.screens.build_screen.edit_value.desc.normal"));
        }).setIsActiveSupplier(consumes -> (this.instance.requirement != null) && this.instance.requirement.hasValue());
        this.addRenderableWidget(editValueButton);
        UIBase.applyDefaultWidgetSkinTo(editValueButton, blur);

        ExtendedButton doneButton = new ExtendedButton(doneButtonX, doneButtonY, 150, 20, Component.translatable("fancymenu.common_components.done"), (button) -> {
            this.callback.accept(this.instance);
            this.closeWindow();
        }).setUITooltipSupplier(consumes -> {
            if (this.instance.requirement == null) {
                return UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.requirements.screens.build_screen.finish.desc.no_requirement_selected"));
            } else if ((this.instance.value == null) && this.instance.requirement.hasValue()) {
                return UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.requirements.screens.build_screen.finish.desc.no_value_set"));
            }
            return null;
        }).setIsActiveSupplier(consumes -> {
            if (this.instance.requirement == null) {
                return false;
            } else if ((this.instance.value == null) && this.instance.requirement.hasValue()) {
                return false;
            }
            return true;
        });
        this.addRenderableWidget(doneButton);
        UIBase.applyDefaultWidgetSkinTo(doneButton, blur);

        ExtendedButton cancelButton = new ExtendedButton(cancelButtonX, cancelButtonY, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            if (this.isEdit) {
                this.callback.accept(this.instance);
            } else {
                this.callback.accept(null);
            }
            this.closeWindow();
        }).setIsActiveSupplier(consumes -> !this.isEdit);
        cancelButton.visible = !this.isEdit;
        this.addRenderableWidget(cancelButton);
        UIBase.applyDefaultWidgetSkinTo(cancelButton, blur);

        ExtendedButton requirementModeButton = new ExtendedButton(requirementModeButtonX, requirementModeButtonY, 150, 20, Component.empty(), (button) -> {
            if (this.instance.mode == RequirementInstance.RequirementMode.IF) {
                this.instance.mode = RequirementInstance.RequirementMode.IF_NOT;
            } else {
                this.instance.mode = RequirementInstance.RequirementMode.IF;
            }
        }).setUITooltipSupplier(consumes -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.requirements.screens.build_screen.requirement_mode.desc")))
                .setLabelSupplier(consumes -> {
                    if (this.instance.mode == RequirementInstance.RequirementMode.IF) {
                        return Component.translatable("fancymenu.requirements.screens.build_screen.requirement_mode.normal");
                    }
                    return Component.translatable("fancymenu.requirements.screens.build_screen.requirement_mode.opposite");
                });
        this.addRenderableWidget(requirementModeButton);
        UIBase.applyDefaultWidgetSkinTo(requirementModeButton, blur);

        this.updateRequirementsList();
        this.setDescription(this.instance.requirement);

    }

    @Override
    public void onWindowClosedExternally() {
        if (this.isEdit) {
            this.callback.accept(this.instance);
        } else {
            this.callback.accept(null);
        }
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int textColor = this.getGenericTextColor();
        graphics.drawString(this.font, Component.translatable("fancymenu.requirements.screens.build_screen.available_requirements"), 20, 50, textColor, false);

        Component descLabel = Component.translatable("fancymenu.requirements.screens.build_screen.requirement_description");
        int descLabelWidth = this.font.width(descLabel);
        graphics.drawString(this.font, descLabel, this.width - 20 - descLabelWidth, 50, textColor, false);

        this.performInitialWidgetFocusActionInRender();

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    protected void setDescription(@Nullable Requirement requirement) {

        this.descriptionScrollArea.clearEntries();

        this.descriptionScrollArea.addEntry(new CellScreen.SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

        if ((requirement != null) && (requirement.getDescription() != null)) {
            for (String s : requirement.getDescription()) {
                this.addDescriptionLine(Component.literal(s));
            }
        }

        this.descriptionScrollArea.addEntry(new CellScreen.SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

    }

    protected void addDescriptionLine(@NotNull Component line) {
        List<Component> lines = new ArrayList<>();
        int maxWidth = (int)(this.descriptionScrollArea.getInnerWidth() - 15F);
        if (this.font.width(line) > maxWidth) {
            this.font.getSplitter().splitLines(line, maxWidth, Style.EMPTY).forEach(formatted -> {
                lines.add(TextFormattingUtils.convertFormattedTextToComponent(formatted));
            });
        } else {
            lines.add(line);
        }
        lines.forEach(component -> {
            TextScrollAreaEntry e = new TextScrollAreaEntry(this.descriptionScrollArea, component, (entry) -> {});
            e.setSelectable(false);
            e.setBackgroundColorHover(e.getBackgroundColorNormal());
            e.setPlayClickSound(false);
            e.setTextBaseColor(this.getLabelTextColor());
            this.descriptionScrollArea.addEntry(e);
        });
    }

    protected boolean requirementFitsSearchValue(@NotNull Requirement requirement, @Nullable String s) {
        if ((s == null) || s.isBlank()) return true;
        s = s.toLowerCase();
        if (requirement.getDisplayName().toLowerCase().contains(s)) return true;
        return this.requirementDescriptionContains(requirement, s);
    }

    protected boolean requirementDescriptionContains(@NotNull Requirement requirement, @NotNull String s) {
        List<String> desc = Objects.requireNonNullElse(requirement.getDescription(), new ArrayList<>());
        for (String line : desc) {
            if (line.toLowerCase().contains(s)) return true;
        }
        return false;
    }

    protected void setContentOfRequirementsList(@Nullable String category) {
        int labelColor = this.getLabelTextColor();

        String searchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        if (searchValue.isBlank()) searchValue = null;

        this.requirementsListScrollArea.clearEntries();

        if (searchValue != null) {
            List<Requirement> requirements = RequirementRegistry.getRequirements();
            requirements.sort(REQUIREMENT_DISPLAY_NAME_COMPARATOR);
            for (Requirement r : requirements) {
                if ((LayoutEditorScreen.getCurrentInstance() != null) && !r.shouldShowUpInEditorRequirementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
                if (!this.requirementFitsSearchValue(r, searchValue)) continue;
                Component label = Component.literal(r.getDisplayName());
                RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.getUITheme().bullet_list_dot_color_1, (entry) -> {
                    this.instance.requirement = r;
                    this.setDescription(this.instance.requirement);
                });
                e.setTextBaseColor(labelColor);
                e.requirement = r;
                this.requirementsListScrollArea.addEntry(e);
            }
            return;
        }

        LinkedHashMap<String, List<Requirement>> categories = RequirementRegistry.getRequirementsOrderedByCategories();
        categories.values().forEach(list -> list.sort(REQUIREMENT_DISPLAY_NAME_COMPARATOR));
        List<Map.Entry<String, List<Requirement>>> sortedCategories = new ArrayList<>(categories.entrySet());
        sortedCategories.sort(Comparator
                .comparing((Map.Entry<String, List<Requirement>> entry) -> entry.getKey(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Map.Entry::getKey));

        if (category == null) {

            //Add category entries
            for (Map.Entry<String, List<Requirement>> m : sortedCategories) {
                Component label = Component.literal(m.getKey());
                TextListScrollAreaEntry e = new TextListScrollAreaEntry(this.requirementsListScrollArea, label, UIBase.getUITheme().bullet_list_dot_color_2, (entry) -> {
                    BuildRequirementScreen.this.setContentOfRequirementsList(m.getKey());
                    BuildRequirementScreen.this.instance.requirement = null;
                    this.setDescription(null);
                });
                e.setSelectable(false);
                e.setTextBaseColor(labelColor);
                this.requirementsListScrollArea.addEntry(e);
            }

            //Add requirement entries without category
            List<Requirement> uncategorized = RequirementRegistry.getRequirementsWithoutCategory();
            uncategorized.sort(REQUIREMENT_DISPLAY_NAME_COMPARATOR);
            for (Requirement r : uncategorized) {
                if ((LayoutEditorScreen.getCurrentInstance() != null) && !r.shouldShowUpInEditorRequirementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
                Component label = Component.literal(r.getDisplayName());
                RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.getUITheme().bullet_list_dot_color_1, (entry) -> {
                    this.instance.requirement = r;
                    this.setDescription(this.instance.requirement);
                });
                e.setTextBaseColor(labelColor);
                e.requirement = r;
                this.requirementsListScrollArea.addEntry(e);
            }

        } else {

            //Add "Back" button
            Component backLabel = Component.translatable("fancymenu.requirements.screens.lists.back");
            TextListScrollAreaEntry backEntry = new TextListScrollAreaEntry(this.requirementsListScrollArea, backLabel, UIBase.getUITheme().bullet_list_dot_color_2, (entry) -> {
                BuildRequirementScreen.this.setContentOfRequirementsList(null);
                BuildRequirementScreen.this.instance.requirement = null;
                this.setDescription(null);
            });
            backEntry.setSelectable(false);
            backEntry.setTextBaseColor(UIBase.getUITheme().warning_text_color.getColorInt());
            this.requirementsListScrollArea.addEntry(backEntry);

            //Add requirement entries of given category
            List<Requirement> l = categories.get(category);
            if (l != null) {
                for (Requirement r : l) {
                    if ((LayoutEditorScreen.getCurrentInstance() != null) && !r.shouldShowUpInEditorRequirementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
                    Component label = Component.literal(r.getDisplayName());
                    RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.getUITheme().bullet_list_dot_color_1, (entry) -> {
                        this.instance.requirement = r;
                        this.setDescription(this.instance.requirement);
                    });
                    e.setTextBaseColor(labelColor);
                    e.requirement = r;
                    this.requirementsListScrollArea.addEntry(e);
                }
            }

        }

    }

    protected void updateRequirementsList() {

        this.setContentOfRequirementsList(null);

        //Select correct entry if instance has requirement
        if (this.instance.requirement != null) {
            this.setContentOfRequirementsList(this.instance.requirement.getCategory());
            for (ScrollAreaEntry e : this.requirementsListScrollArea.getEntries()) {
                if ((e instanceof RequirementScrollEntry) && (((RequirementScrollEntry) e).requirement == this.instance.requirement)) {
                    e.setSelected(true);
                    break;
                }
            }
        }

    }

    public class RequirementScrollEntry extends TextListScrollAreaEntry {

        public Requirement requirement;
        protected long lastClickTime = 0;
        protected static final long DOUBLE_CLICK_TIME = 500; // milliseconds

        public RequirementScrollEntry(ScrollArea parent, @NotNull Component text, @NotNull DrawableColor listDotColor, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, text, listDotColor, onClick);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            long currentTime = System.currentTimeMillis();
            
            // Check if this is a double-click
            if (currentTime - this.lastClickTime < DOUBLE_CLICK_TIME) {
                // Double-click detected
                if ((this.requirement != null) && (BuildRequirementScreen.this.instance.requirement == this.requirement)) {
                    // Check if requirement has value or doesn't need value -> act as "Done"
                    if (this.requirement.hasValue()) {
                        BuildRequirementScreen.this.instance.requirement.editValue(BuildRequirementScreen.this, BuildRequirementScreen.this.instance);
                    } else {
                        BuildRequirementScreen.this.callback.accept(BuildRequirementScreen.this.instance);
                        BuildRequirementScreen.this.closeWindow();
                    }
                    this.lastClickTime = 0; // Reset to prevent triple clicks
                    return;
                }
            }
            
            this.lastClickTime = currentTime;
            
            // Normal single click behavior
            super.onClick(entry, mouseX, mouseY, button);
        }

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

    public static @NotNull PiPWindow openInWindow(@NotNull BuildRequirementScreen screen, @Nullable PiPWindow parentWindow) {
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

    public static @NotNull PiPWindow openInWindow(@NotNull BuildRequirementScreen screen) {
        return openInWindow(screen, null);
    }

}
