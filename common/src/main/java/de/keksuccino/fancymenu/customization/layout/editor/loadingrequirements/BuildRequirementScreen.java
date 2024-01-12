package de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements;

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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BuildRequirementScreen extends Screen {

    protected Screen parentScreen;
    protected LoadingRequirementContainer parent;
    protected final LoadingRequirementInstance instance;
    protected boolean isEdit;
    protected Consumer<LoadingRequirementInstance> callback;

    protected ScrollArea requirementsListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea requirementDescriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton requirementModeButton;
    protected ExtendedButton editValueButton;
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;

    public BuildRequirementScreen(@Nullable Screen parentScreen, @NotNull LoadingRequirementContainer parent, @Nullable LoadingRequirementInstance instanceToEdit, @NotNull Consumer<LoadingRequirementInstance> callback) {

        super((instanceToEdit != null) ? Component.literal(I18n.get("fancymenu.editor.loading_requirement.screens.edit_requirement")) : Component.literal(I18n.get("fancymenu.editor.loading_requirement.screens.add_requirement")));

        this.parentScreen = parentScreen;
        this.parent = parent;
        this.instance = (instanceToEdit != null) ? instanceToEdit : new LoadingRequirementInstance(null, null, LoadingRequirementInstance.RequirementMode.IF, parent);
        this.isEdit = instanceToEdit != null;
        this.callback = callback;
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

    @Override
    protected void init() {

        this.editValueButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.loading_requirement.screens.build_screen.edit_value"), (button) -> {
            if (this.instance.requirement != null) {
                this.instance.requirement.editValue(this, this.instance);
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                LoadingRequirement r = BuildRequirementScreen.this.instance.requirement;
                if ((r != null) && !r.hasValue()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_screen.edit_value.desc.no_value")));
                } else {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_screen.edit_value.desc.normal")));
                }
                this.active = (r != null) && r.hasValue();
                super.render(graphics, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.editValueButton);
        UIBase.applyDefaultWidgetSkinTo(this.editValueButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.guicomponents.done"), (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.instance);
        }) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                if (BuildRequirementScreen.this.instance.requirement == null) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_screen.finish.desc.no_requirement_selected")));
                    this.active = false;
                } else if ((BuildRequirementScreen.this.instance.value == null) && BuildRequirementScreen.this.instance.requirement.hasValue()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_screen.finish.desc.no_value_set")));
                    this.active = false;
                } else {
                    this.setTooltip((Tooltip) null);
                    this.active = true;
                }
                super.renderWidget(graphics, mouseX, mouseY, partialTicks);
            }
        };
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.guicomponents.cancel"), (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            if (this.isEdit) {
                this.callback.accept(this.instance);
            } else {
                this.callback.accept(null);
            }
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.requirementModeButton = new ExtendedButton(0, 0, 150, 20, "", (button) -> {
            if (this.instance.mode == LoadingRequirementInstance.RequirementMode.IF) {
                this.instance.mode = LoadingRequirementInstance.RequirementMode.IF_NOT;
            } else {
                this.instance.mode = LoadingRequirementInstance.RequirementMode.IF;
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                if (BuildRequirementScreen.this.instance.mode == LoadingRequirementInstance.RequirementMode.IF) {
                    this.setLabel(I18n.get("fancymenu.editor.loading_requirement.screens.build_screen.requirement_mode.normal"));
                } else {
                    this.setLabel(I18n.get("fancymenu.editor.loading_requirement.screens.build_screen.requirement_mode.opposite"));
                }
                super.render(graphics, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.requirementModeButton);
        this.requirementModeButton.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_screen.requirement_mode.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.requirementModeButton);

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

        graphics.drawString(this.font, I18n.get("fancymenu.editor.loading_requirement.screens.build_screen.available_requirements"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.requirementsListScrollArea.setWidth((this.width / 2) - 40, true);
        this.requirementsListScrollArea.setHeight(this.height - 85, true);
        this.requirementsListScrollArea.setX(20, true);
        this.requirementsListScrollArea.setY(50 + 15, true);
        this.requirementsListScrollArea.render(graphics, mouseX, mouseY, partial);

        String descLabelString = I18n.get("fancymenu.editor.loading_requirement.screens.build_screen.requirement_description");
        int descLabelWidth = this.font.width(descLabelString);
        graphics.drawString(this.font, descLabelString, this.width - 20 - descLabelWidth, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.requirementDescriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.requirementDescriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.requirementDescriptionScrollArea.setX(this.width - 20 - this.requirementDescriptionScrollArea.getWidthWithBorder(), true);
        this.requirementDescriptionScrollArea.setY(50 + 15, true);
        this.requirementDescriptionScrollArea.render(graphics, mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(graphics, mouseX, mouseY, partial);

        if (!this.isEdit) {
            this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
            this.cancelButton.setY(this.doneButton.getY() - 5 - 20);
            this.cancelButton.render(graphics, mouseX, mouseY, partial);
        } else {
            this.cancelButton.active = false;
        }

        this.editValueButton.setX(this.width - 20 - this.editValueButton.getWidth());
        this.editValueButton.setY(((this.isEdit) ? this.doneButton.getY() : this.cancelButton.getY()) - 15 - 20);
        this.editValueButton.render(graphics, mouseX, mouseY, partial);

        this.requirementModeButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.requirementModeButton.setY(this.editValueButton.getY() - 5 - 20);
        this.requirementModeButton.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics $$0, int $$1, int $$2, float $$3) {
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

    protected void setContentOfRequirementsList(@Nullable String category) {

        this.requirementsListScrollArea.clearEntries();

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
            Component backLabel = Component.literal(I18n.get("fancymenu.editor.loading_requirement.screens.lists.back")).withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()));
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

    public static class RequirementScrollEntry extends TextListScrollAreaEntry {

        public LoadingRequirement requirement;

        public RequirementScrollEntry(ScrollArea parent, @NotNull Component text, @NotNull Color listDotColor, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, text, listDotColor, onClick);
        }

    }

}
