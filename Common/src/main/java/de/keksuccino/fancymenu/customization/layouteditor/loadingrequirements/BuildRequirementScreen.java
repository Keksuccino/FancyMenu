
package de.keksuccino.fancymenu.customization.layouteditor.loadingrequirements;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.LoadingRequirementRegistry;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.internal.LoadingRequirementInstance;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
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
    protected AdvancedButton requirementModeButton;
    protected AdvancedButton editValueButton;
    protected AdvancedButton doneButton;
    protected AdvancedButton cancelButton;

    public BuildRequirementScreen(@Nullable Screen parentScreen, @NotNull LoadingRequirementContainer parent, @Nullable LoadingRequirementInstance instanceToEdit, @NotNull Consumer<LoadingRequirementInstance> callback) {

        super((instanceToEdit != null) ? Component.literal(Locals.localize("fancymenu.editor.loading_requirement.screens.edit_requirement")) : Component.literal(Locals.localize("fancymenu.editor.loading_requirement.screens.add_requirement")));

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
                    e.setFocused(true);
                    break;
                }
            }
        }

        this.editValueButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.edit_value"), true, (button) -> {
            TextEditorScreen s = new TextEditorScreen(button.getMessage(), this, null, (call) -> {
                if (call != null) {
                    this.instance.value = call;
                }
            });
            if ((this.instance.requirement != null) && (this.instance.requirement.getValueFormattingRules() != null)) {
                s.formattingRules.addAll(this.instance.requirement.getValueFormattingRules());
            }
            s.multilineMode = false;
            if (this.instance.value != null) {
                s.setText(this.instance.value);
            } else if (this.instance.requirement != null) {
                s.setText(this.instance.requirement.getValuePreset());
            }
            Minecraft.getInstance().setScreen(s);
        }) {
            @Override
            public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                LoadingRequirement r = BuildRequirementScreen.this.instance.requirement;
                if ((r != null) && !r.hasValue()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.edit_value.desc.no_value"), "%n%"));
                } else {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.edit_value.desc.normal"), "%n%"));
                }
                if ((r == null) || !r.hasValue()) {
                    this.active = false;
                } else {
                    this.active = true;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.editValueButton);

        this.doneButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.guicomponents.done"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.instance);
        }) {
            @Override
            public void renderWidget(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
                if (BuildRequirementScreen.this.instance.requirement == null) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.finish.desc.no_requirement_selected"), "%n%"));
                    this.active = false;
                } else if ((BuildRequirementScreen.this.instance.value == null) && BuildRequirementScreen.this.instance.requirement.hasValue()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.finish.desc.no_value_set"), "%n%"));
                    this.active = false;
                } else {
                    this.setDescription((String[])null);
                    this.active = true;
                }
                super.renderWidget(matrix, mouseX, mouseY, partialTicks);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

        this.cancelButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.guicomponents.cancel"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            if (this.isEdit) {
                this.callback.accept(this.instance);
            } else {
                this.callback.accept(null);
            }
        });
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

        this.requirementModeButton = new AdvancedButton(0, 0, 150, 20, "", true, (button) -> {
            if (this.instance.mode == LoadingRequirementInstance.RequirementMode.IF) {
                this.instance.mode = LoadingRequirementInstance.RequirementMode.IF_NOT;
            } else {
                this.instance.mode = LoadingRequirementInstance.RequirementMode.IF;
            }
        }) {
            @Override
            public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (BuildRequirementScreen.this.instance.mode == LoadingRequirementInstance.RequirementMode.IF) {
                    this.setMessage(Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.requirement_mode.normal"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.requirement_mode.opposite"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.requirementModeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.requirement_mode.desc"), "%n%"));
        UIBase.applyDefaultButtonSkinTo(this.requirementModeButton);

    }

    @Override
    protected void init() {

        //Reset the GUI scale in case the layout editor changed it
        Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
        this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        super.init();

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
    public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partial) {

        fill(matrix, 0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(matrix, titleComp, 20, 20, -1);

        this.font.draw(matrix, Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.available_requirements"), 20, 50, -1);

        this.requirementsListScrollArea.setWidth((this.width / 2) - 40, true);
        this.requirementsListScrollArea.setHeight(this.height - 85, true);
        this.requirementsListScrollArea.setX(20, true);
        this.requirementsListScrollArea.setY(50 + 15, true);
        this.requirementsListScrollArea.render(matrix, mouseX, mouseY, partial);

        String descLabelString = Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.requirement_description");
        int descLabelWidth = this.font.width(descLabelString);
        this.font.draw(matrix, descLabelString, this.width - 20 - descLabelWidth, 50, -1);

        this.requirementDescriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.requirementDescriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.requirementDescriptionScrollArea.setX(this.width - 20 - this.requirementDescriptionScrollArea.getWidthWithBorder(), true);
        this.requirementDescriptionScrollArea.setY(50 + 15, true);
        this.requirementDescriptionScrollArea.render(matrix, mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(matrix, mouseX, mouseY, partial);

        if (!this.isEdit) {
            this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
            this.cancelButton.setY(this.doneButton.getY() - 5 - 20);
            this.cancelButton.render(matrix, mouseX, mouseY, partial);
        } else {
            this.cancelButton.active = false;
        }

        this.editValueButton.setX(this.width - 20 - this.editValueButton.getWidth());
        this.editValueButton.setY(((this.isEdit) ? this.doneButton.getY() : this.cancelButton.getY()) - 15 - 20);
        this.editValueButton.render(matrix, mouseX, mouseY, partial);

        this.requirementModeButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.requirementModeButton.setY(this.editValueButton.getY() - 5 - 20);
        this.requirementModeButton.render(matrix, mouseX, mouseY, partial);

        super.render(matrix, mouseX, mouseY, partial);

    }

    protected void setDescription(@Nullable LoadingRequirement requirement) {

        this.requirementDescriptionScrollArea.clearEntries();

        if ((requirement != null) && (requirement.getDescription() != null)) {
            for (String s : requirement.getDescription()) {
                TextScrollAreaEntry e = new TextScrollAreaEntry(this.requirementDescriptionScrollArea, Component.literal(s), (entry) -> {});
                e.setFocusable(false);
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
                Component label = Component.literal(m.getKey()).withStyle(Style.EMPTY.withColor(UIBase.TEXT_COLOR_GRAY_1.getRGB()));
                TextListScrollAreaEntry e = new TextListScrollAreaEntry(this.requirementsListScrollArea, label, UIBase.LISTING_DOT_RED, (entry) -> {
                    BuildRequirementScreen.this.setContentOfRequirementsList(m.getKey());
                    BuildRequirementScreen.this.instance.requirement = null;
                    this.setDescription(null);
                });
                e.setFocusable(false);
                this.requirementsListScrollArea.addEntry(e);
            }
            //Add requirement entries without category
            for (LoadingRequirement r : LoadingRequirementRegistry.getRequirementsWithoutCategory()) {
                Component label = Component.literal(r.getDisplayName()).withStyle(Style.EMPTY.withColor(UIBase.TEXT_COLOR_GRAY_1.getRGB()));
                RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.LISTING_DOT_BLUE, (entry) -> {
                    this.instance.requirement = r;
                    this.setDescription(this.instance.requirement);
                });
                e.requirement = r;
                this.requirementsListScrollArea.addEntry(e);
            }

        } else {

            //Add "Back" button
            Component backLabel = Component.literal(Locals.localize("fancymenu.editor.loading_requirement.screens.lists.back")).withStyle(Style.EMPTY.withColor(UIBase.TEXT_COLOR_ORANGE_1.getRGB()));
            TextListScrollAreaEntry backEntry = new TextListScrollAreaEntry(this.requirementsListScrollArea, backLabel, UIBase.LISTING_DOT_RED, (entry) -> {
                BuildRequirementScreen.this.setContentOfRequirementsList(null);
                BuildRequirementScreen.this.instance.requirement = null;
                this.setDescription(null);
            });
            backEntry.setFocusable(false);
            this.requirementsListScrollArea.addEntry(backEntry);

            //Add requirement entries of given category
            List<LoadingRequirement> l = categories.get(category);
            if (l != null) {
                for (LoadingRequirement r : l) {
                    Component label = Component.literal(r.getDisplayName()).withStyle(Style.EMPTY.withColor(UIBase.TEXT_COLOR_GRAY_1.getRGB()));
                    RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.LISTING_DOT_BLUE, (entry) -> {
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
