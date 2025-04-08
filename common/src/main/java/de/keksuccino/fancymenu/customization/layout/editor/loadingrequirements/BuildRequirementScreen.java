package de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirementRegistry;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class BuildRequirementScreen extends Screen {

    protected Screen parentScreen;
    protected LoadingRequirementContainer parent;
    protected final LoadingRequirementInstance instance;
    protected boolean isEdit;
    protected Consumer<LoadingRequirementInstance> callback;

    protected ScrollArea requirementsListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea requirementDescriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedEditBox searchBar;

    public BuildRequirementScreen(@Nullable Screen parentScreen, @NotNull LoadingRequirementContainer parent, @Nullable LoadingRequirementInstance instanceToEdit, @NotNull Consumer<LoadingRequirementInstance> callback) {

        super((instanceToEdit != null) ? Component.translatable("fancymenu.editor.loading_requirement.screens.edit_requirement") : Component.translatable("fancymenu.editor.loading_requirement.screens.add_requirement"));

        this.parentScreen = parentScreen;
        this.parent = parent;
        this.instance = (instanceToEdit != null) ? instanceToEdit : new LoadingRequirementInstance(null, null, LoadingRequirementInstance.RequirementMode.IF, parent);
        this.isEdit = instanceToEdit != null;
        this.callback = callback;

    }

    @SuppressWarnings("all")
    @Override
    protected void init() {

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 20 + 1, 50 + 15 + 1, (this.width / 2) - 40 - 2, 20 - 2, Component.empty()) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                super.renderWidget(graphics, mouseX, mouseY, partial);
                if (this.getValue().isBlank() && !this.isFocused()) {
                    graphics.drawString(this.font, Component.translatable("fancymenu.requirements.build_requirement.screen.search_requirement"), this.getX() + 4, this.getY() + (this.getHeight() / 2) - (this.font.lineHeight / 2), UIBase.getUIColorTheme().edit_box_text_color_uneditable.getColorInt(), false);
                }
            }
        };
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updateRequirementsList());
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar);

        // Set positions for scroll areas
        this.requirementsListScrollArea.setWidth((this.width / 2) - 40, true);
        this.requirementsListScrollArea.setHeight(this.height - 85 - 25, true);
        this.requirementsListScrollArea.setX(20, true);
        this.requirementsListScrollArea.setY(50 + 15 + 25, true);

        this.requirementDescriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.requirementDescriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.requirementDescriptionScrollArea.setX(this.width - 20 - this.requirementDescriptionScrollArea.getWidthWithBorder(), true);
        this.requirementDescriptionScrollArea.setY(50 + 15, true);

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
        ExtendedButton editValueButton = new ExtendedButton(editValueButtonX, editValueButtonY, 150, 20, Component.translatable("fancymenu.editor.loading_requirement.screens.build_screen.edit_value"), (button) -> {
            if (this.instance.requirement != null) {
                this.instance.requirement.editValue(this, this.instance);
            }
        }).setTooltipSupplier(consumes -> {
            if ((this.instance.requirement != null) && !this.instance.requirement.hasValue()) {
                return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.screens.build_screen.edit_value.desc.no_value"));
            }
            return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.screens.build_screen.edit_value.desc.normal"));
        }).setIsActiveSupplier(consumes -> (this.instance.requirement != null) && this.instance.requirement.hasValue());
        this.addRenderableWidget(editValueButton);
        UIBase.applyDefaultWidgetSkinTo(editValueButton);

        ExtendedButton doneButton = new ExtendedButton(doneButtonX, doneButtonY, 150, 20, Component.translatable("fancymenu.guicomponents.done"), (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.instance);
        }).setTooltipSupplier(consumes -> {
            if (this.instance.requirement == null) {
                return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.screens.build_screen.finish.desc.no_requirement_selected"));
            } else if ((this.instance.value == null) && this.instance.requirement.hasValue()) {
                return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.screens.build_screen.finish.desc.no_value_set"));
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
        UIBase.applyDefaultWidgetSkinTo(doneButton);

        ExtendedButton cancelButton = new ExtendedButton(cancelButtonX, cancelButtonY, 150, 20, Component.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            if (this.isEdit) {
                this.callback.accept(this.instance);
            } else {
                this.callback.accept(null);
            }
        }).setIsActiveSupplier(consumes -> !this.isEdit);
        cancelButton.visible = !this.isEdit;
        this.addRenderableWidget(cancelButton);
        UIBase.applyDefaultWidgetSkinTo(cancelButton);

        ExtendedButton requirementModeButton = new ExtendedButton(requirementModeButtonX, requirementModeButtonY, 150, 20, Component.empty(), (button) -> {
            if (this.instance.mode == LoadingRequirementInstance.RequirementMode.IF) {
                this.instance.mode = LoadingRequirementInstance.RequirementMode.IF_NOT;
            } else {
                this.instance.mode = LoadingRequirementInstance.RequirementMode.IF;
            }
        }).setTooltipSupplier(consumes -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.screens.build_screen.requirement_mode.desc")))
                .setLabelSupplier(consumes -> {
                    if (this.instance.mode == LoadingRequirementInstance.RequirementMode.IF) {
                        return Component.translatable("fancymenu.editor.loading_requirement.screens.build_screen.requirement_mode.normal");
                    }
                    return Component.translatable("fancymenu.editor.loading_requirement.screens.build_screen.requirement_mode.opposite");
                });
        this.addRenderableWidget(requirementModeButton);
        UIBase.applyDefaultWidgetSkinTo(requirementModeButton);

        this.updateRequirementsList();

        this.setDescription(this.instance.requirement);

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parentScreen);
        if (this.isEdit) {
            this.callback.accept(this.instance);
        } else {
            this.callback.accept(null);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, Component.translatable("fancymenu.editor.loading_requirement.screens.build_screen.available_requirements"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.requirementsListScrollArea.render(graphics, mouseX, mouseY, partial);

        Component descLabel = Component.translatable("fancymenu.editor.loading_requirement.screens.build_screen.requirement_description");
        int descLabelWidth = this.font.width(descLabel);
        graphics.drawString(this.font, descLabel, this.width - 20 - descLabelWidth, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.requirementDescriptionScrollArea.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    protected void setDescription(@Nullable LoadingRequirement requirement) {
        this.requirementDescriptionScrollArea.clearEntries();
        if ((requirement != null) && (requirement.getDescription() != null)) {
            for (String s : requirement.getDescription()) {
                TextScrollAreaEntry e = new TextScrollAreaEntry(this.requirementDescriptionScrollArea, Component.literal(s).withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), (entry) -> {});
                e.setSelectable(false);
                e.setBackgroundColorHover(e.getBackgroundColorIdle());
                e.setPlayClickSound(false);
                this.requirementDescriptionScrollArea.addEntry(e);
            }
        }
    }

    protected boolean requirementFitsSearchValue(@NotNull LoadingRequirement requirement, @Nullable String s) {
        if ((s == null) || s.isBlank()) return true;
        s = s.toLowerCase();
        if (requirement.getDisplayName().toLowerCase().contains(s)) return true;
        return this.requirementDescriptionContains(requirement, s);
    }

    protected boolean requirementDescriptionContains(@NotNull LoadingRequirement requirement, @NotNull String s) {
        List<String> desc = Objects.requireNonNullElse(requirement.getDescription(), new ArrayList<>());
        for (String line : desc) {
            if (line.toLowerCase().contains(s)) return true;
        }
        return false;
    }

    protected void setContentOfRequirementsList(@Nullable String category) {

        String searchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        if (searchValue.isBlank()) searchValue = null;

        this.requirementsListScrollArea.clearEntries();

        if (searchValue != null) {
            for (LoadingRequirement r : LoadingRequirementRegistry.getRequirements()) {
                if ((LayoutEditorScreen.getCurrentInstance() != null) && !r.shouldShowUpInEditorRequirementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
                if (!this.requirementFitsSearchValue(r, searchValue)) continue;
                Component label = Component.literal(r.getDisplayName()).withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
                RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.getUIColorTheme().listing_dot_color_1.getColor(), (entry) -> {
                    this.instance.requirement = r;
                    this.setDescription(this.instance.requirement);
                });
                e.requirement = r;
                this.requirementsListScrollArea.addEntry(e);
            }
            return;
        }

        LinkedHashMap<String, List<LoadingRequirement>> categories = LoadingRequirementRegistry.getRequirementsOrderedByCategories();

        if (category == null) {

            //Add category entries
            for (Map.Entry<String, List<LoadingRequirement>> m : categories.entrySet()) {
                Component label = Component.literal(m.getKey()).withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
                TextListScrollAreaEntry e = new TextListScrollAreaEntry(this.requirementsListScrollArea, label, UIBase.getUIColorTheme().listing_dot_color_2.getColor(), (entry) -> {
                    BuildRequirementScreen.this.setContentOfRequirementsList(m.getKey());
                    BuildRequirementScreen.this.instance.requirement = null;
                    this.setDescription(null);
                });
                e.setSelectable(false);
                this.requirementsListScrollArea.addEntry(e);
            }

            //Add requirement entries without category
            for (LoadingRequirement r : LoadingRequirementRegistry.getRequirementsWithoutCategory()) {
                if ((LayoutEditorScreen.getCurrentInstance() != null) && !r.shouldShowUpInEditorRequirementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
                Component label = Component.literal(r.getDisplayName()).withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
                RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.getUIColorTheme().listing_dot_color_1.getColor(), (entry) -> {
                    this.instance.requirement = r;
                    this.setDescription(this.instance.requirement);
                });
                e.requirement = r;
                this.requirementsListScrollArea.addEntry(e);
            }

        } else {

            //Add "Back" button
            Component backLabel = Component.translatable("fancymenu.editor.loading_requirement.screens.lists.back").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()));
            TextListScrollAreaEntry backEntry = new TextListScrollAreaEntry(this.requirementsListScrollArea, backLabel, UIBase.getUIColorTheme().listing_dot_color_2.getColor(), (entry) -> {
                BuildRequirementScreen.this.setContentOfRequirementsList(null);
                BuildRequirementScreen.this.instance.requirement = null;
                this.setDescription(null);
            });
            backEntry.setSelectable(false);
            this.requirementsListScrollArea.addEntry(backEntry);

            //Add requirement entries of given category
            List<LoadingRequirement> l = categories.get(category);
            if (l != null) {
                for (LoadingRequirement r : l) {
                    if ((LayoutEditorScreen.getCurrentInstance() != null) && !r.shouldShowUpInEditorRequirementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
                    Component label = Component.literal(r.getDisplayName()).withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
                    RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.getUIColorTheme().listing_dot_color_1.getColor(), (entry) -> {
                        this.instance.requirement = r;
                        this.setDescription(this.instance.requirement);
                    });
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

    public static class RequirementScrollEntry extends TextListScrollAreaEntry {

        public LoadingRequirement requirement;

        public RequirementScrollEntry(ScrollArea parent, @NotNull Component text, @NotNull Color listDotColor, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, text, listDotColor, onClick);
        }

    }

}