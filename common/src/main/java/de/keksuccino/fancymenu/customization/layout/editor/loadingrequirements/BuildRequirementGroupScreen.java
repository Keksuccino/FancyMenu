package de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements;

import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementGroup;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class BuildRequirementGroupScreen extends Screen {

    protected Screen parentScreen;
    protected LoadingRequirementContainer parent;
    protected LoadingRequirementGroup group;
    protected boolean isEdit;
    protected Consumer<LoadingRequirementGroup> callback;

    protected ScrollArea requirementsScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton groupModeButton;
    protected ExtendedButton addRequirementButton;
    protected ExtendedButton removeRequirementButton;
    protected ExtendedButton editRequirementButton;
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;
    protected ExtendedEditBox groupIdentifierTextField;

    public BuildRequirementGroupScreen(@Nullable Screen parentScreen, @NotNull LoadingRequirementContainer parent, @Nullable LoadingRequirementGroup groupToEdit, @NotNull Consumer<LoadingRequirementGroup> callback) {

        super((groupToEdit != null) ? Component.literal(I18n.get("fancymenu.editor.loading_requirement.screens.edit_group")) : Component.literal(I18n.get("fancymenu.editor.loading_requirement.screens.add_group")));

        this.parentScreen = parentScreen;
        this.parent = parent;
        this.group = (groupToEdit != null) ? groupToEdit : new LoadingRequirementGroup("group_" + System.currentTimeMillis(), LoadingRequirementGroup.GroupMode.AND, parent);
        this.callback = callback;
        this.isEdit = groupToEdit != null;
        this.updateRequirementsScrollArea();

    }

    @Override
    protected void init() {

        this.groupIdentifierTextField = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 150, 20, Component.empty()) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                super.render(graphics, mouseX, mouseY, partial);
                BuildRequirementGroupScreen.this.group.identifier = this.getValue();
            }
        };
        if (this.group.identifier != null) {
            this.groupIdentifierTextField.setValue(this.group.identifier);
        }
        this.groupIdentifierTextField.setCharacterFilter(CharacterFilter.buildOnlyLowercaseFileNameFilter());
        this.addWidget(this.groupIdentifierTextField);

        //Reset the GUI scale in case the layout editor changed it
        Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
		this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        this.groupModeButton = new ExtendedButton(0, 0, 150, 20, "", (button) -> {
            if (this.group.mode == LoadingRequirementGroup.GroupMode.AND) {
                this.group.mode = LoadingRequirementGroup.GroupMode.OR;
            } else {
                this.group.mode = LoadingRequirementGroup.GroupMode.AND;
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.group.mode == LoadingRequirementGroup.GroupMode.AND) {
                    this.setLabel(I18n.get("fancymenu.editor.loading_requirement.screens.build_group_screen.mode.and"));
                } else {
                    this.setLabel(I18n.get("fancymenu.editor.loading_requirement.screens.build_group_screen.mode.or"));
                }
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.groupModeButton);
        this.groupModeButton.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.mode.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.groupModeButton);

        this.addRequirementButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.loading_requirement.screens.add_requirement"), (button) -> {
            BuildRequirementScreen s = new BuildRequirementScreen(this, this.parent, null, (call) -> {
                if (call != null) {
                    this.group.addInstance(call);
                    this.updateRequirementsScrollArea();
                }
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.addRequirementButton);
        this.addRequirementButton.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.add_requirement.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.addRequirementButton);

        this.editRequirementButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.loading_requirement.screens.edit_requirement"), (button) -> {
            LoadingRequirementInstance i = this.getSelectedInstance();
            if (i != null) {
                BuildRequirementScreen s = new BuildRequirementScreen(this, this.parent, i, (call) -> {
                    if (call != null) {
                        this.updateRequirementsScrollArea();
                    }
                });
                Minecraft.getInstance().setScreen(s);
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.getSelectedInstance() == null) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.no_requirement_selected")));
                    this.active = false;
                } else {
                    this.setTooltip((Tooltip) null);
                    this.active = true;
                }
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.editRequirementButton);
        this.editRequirementButton.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.edit_requirement.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.editRequirementButton);

        this.removeRequirementButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.loading_requirement.screens.remove_requirement"), (button) -> {
            LoadingRequirementInstance i = this.getSelectedInstance();
            if (i != null) {
                Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call) -> {
                    if (call) {
                        this.group.removeInstance(i);
                        this.updateRequirementsScrollArea();
                    }
                    Minecraft.getInstance().setScreen(this);
                }, LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.remove_requirement.confirm")));
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.getSelectedInstance() == null) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.no_requirement_selected")));
                    this.active = false;
                } else {
                    this.setTooltip((Tooltip) null);
                    this.active = true;
                }
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.removeRequirementButton);
        this.removeRequirementButton.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.remove_requirement.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.removeRequirementButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.guicomponents.done"), (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.group);
        }) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                BuildRequirementGroupScreen s = BuildRequirementGroupScreen.this;
                if (s.group.getInstances().isEmpty()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.finish.no_requirements_added")));
                    this.active = false;
                } else if ((s.parent.getGroup(s.group.identifier) != null) && (s.parent.getGroup(s.group.identifier) != s.group)) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.finish.identifier_already_used")));
                    this.active = false;
                } else if ((s.group.identifier == null) || (s.group.identifier.replace(" ", "").length() == 0)) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.finish.identifier_too_short")));
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
                this.callback.accept(this.group);
            } else {
                this.callback.accept(null);
            }
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parentScreen);
        if (this.isEdit) {
            this.callback.accept(this.group);
        } else {
            this.callback.accept(null);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, I18n.get("fancymenu.editor.loading_requirement.screens.build_group_screen.group_requirements"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.requirementsScrollArea.setWidth(this.width - 20 - 150 - 20 - 20, true);
        this.requirementsScrollArea.setHeight(this.height - 85, true);
        this.requirementsScrollArea.setX(20, true);
        this.requirementsScrollArea.setY(50 + 15, true);
        this.requirementsScrollArea.render(graphics, mouseX, mouseY, partial);

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

        this.removeRequirementButton.setX(this.width - 20 - this.removeRequirementButton.getWidth());
        this.removeRequirementButton.setY(((this.isEdit) ? this.doneButton.getY() : this.cancelButton.getY()) - 15 - 20);
        this.removeRequirementButton.render(graphics, mouseX, mouseY, partial);

        this.editRequirementButton.setX(this.width - 20 - this.editRequirementButton.getWidth());
        this.editRequirementButton.setY(this.removeRequirementButton.getY() - 5 - 20);
        this.editRequirementButton.render(graphics, mouseX, mouseY, partial);

        this.addRequirementButton.setX(this.width - 20 - this.addRequirementButton.getWidth());
        this.addRequirementButton.setY(this.editRequirementButton.getY() - 5 - 20);
        this.addRequirementButton.render(graphics, mouseX, mouseY, partial);

        this.groupModeButton.setX(this.width - 20 - this.groupModeButton.getWidth());
        this.groupModeButton.setY(this.addRequirementButton.getY() - 5 - 20);
        this.groupModeButton.render(graphics, mouseX, mouseY, partial);

        this.groupIdentifierTextField.setX(this.width - 20 - this.groupIdentifierTextField.getWidth());
        this.groupIdentifierTextField.setY(this.groupModeButton.getY() - 15 - 20);
        this.groupIdentifierTextField.render(graphics, mouseX, mouseY, partial);

        String idLabel = I18n.get("fancymenu.editor.loading_requirement.screens.build_group_screen.group_identifier");
        int idLabelWidth = this.font.width(idLabel);
        graphics.drawString(this.font, idLabel, this.width - 20 - idLabelWidth, this.groupIdentifierTextField.getY() - 15, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics $$0, int $$1, int $$2, float $$3) {
    }

    @Nullable
    protected LoadingRequirementInstance getSelectedInstance() {
        ScrollAreaEntry e = this.requirementsScrollArea.getFocusedEntry();
        if (e instanceof ManageRequirementsScreen.RequirementInstanceEntry) {
            return ((ManageRequirementsScreen.RequirementInstanceEntry) e).instance;
        }
        return null;
    }

    protected void updateRequirementsScrollArea() {

        this.requirementsScrollArea.clearEntries();

        for (LoadingRequirementInstance i : this.group.getInstances()) {
            ManageRequirementsScreen.RequirementInstanceEntry e = new ManageRequirementsScreen.RequirementInstanceEntry(this.requirementsScrollArea, i, 14);
            this.requirementsScrollArea.addEntry(e);
        }

    }

}
