package de.keksuccino.fancymenu.customization.requirement.ui;

import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.GuiGraphics;
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
    protected RequirementContainer parent;
    protected RequirementGroup group;
    protected boolean isEdit;
    protected Consumer<RequirementGroup> callback;

    protected ScrollArea requirementsScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton groupModeButton;
    protected ExtendedButton addRequirementButton;
    protected ExtendedButton removeRequirementButton;
    protected ExtendedButton editRequirementButton;
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;
    protected AdvancedTextField groupIdentifierTextField;

    public BuildRequirementGroupScreen(@Nullable Screen parentScreen, @NotNull RequirementContainer parent, @Nullable RequirementGroup groupToEdit, @NotNull Consumer<RequirementGroup> callback) {

        super((groupToEdit != null) ? Component.literal(I18n.get("fancymenu.requirements.screens.edit_group")) : Component.literal(I18n.get("fancymenu.requirements.screens.add_group")));

        this.parentScreen = parentScreen;
        this.parent = parent;
        this.group = (groupToEdit != null) ? groupToEdit : new RequirementGroup("group_" + System.currentTimeMillis(), RequirementGroup.GroupMode.AND, parent);
        this.callback = callback;
        this.isEdit = groupToEdit != null;
        this.updateRequirementsScrollArea();

        this.groupIdentifierTextField = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 150, 20, true, CharacterFilter.getBasicFilenameCharacterFilter()) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                super.render(graphics, mouseX, mouseY, partial);
                BuildRequirementGroupScreen.this.group.identifier = this.getValue();
            }
        };
        if (this.group.identifier != null) {
            this.groupIdentifierTextField.setValue(this.group.identifier);
        }

    }

    @Override
    protected void init() {

        //Reset the GUI scale in case the layout editor changed it
        Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
		this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        this.groupModeButton = new ExtendedButton(0, 0, 150, 20, "", (button) -> {
            if (this.group.mode == RequirementGroup.GroupMode.AND) {
                this.group.mode = RequirementGroup.GroupMode.OR;
            } else {
                this.group.mode = RequirementGroup.GroupMode.AND;
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.group.mode == RequirementGroup.GroupMode.AND) {
                    this.setLabel(I18n.get("fancymenu.requirements.screens.build_group_screen.mode.and"));
                } else {
                    this.setLabel(I18n.get("fancymenu.requirements.screens.build_group_screen.mode.or"));
                }
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.groupModeButton);
        this.groupModeButton.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.build_group_screen.mode.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.groupModeButton);

        this.addRequirementButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.requirements.screens.add_requirement"), (button) -> {
            BuildRequirementScreen s = new BuildRequirementScreen(this, this.parent, null, (call) -> {
                if (call != null) {
                    this.group.addInstance(call);
                    this.updateRequirementsScrollArea();
                }
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.addRequirementButton);
        this.addRequirementButton.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.build_group_screen.add_requirement.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.addRequirementButton);

        this.editRequirementButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.requirements.screens.edit_requirement"), (button) -> {
            RequirementInstance i = this.getSelectedInstance();
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
                    this.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.build_group_screen.no_requirement_selected")));
                    this.active = false;
                } else {
                    this.setUITooltip((UITooltip) null);
                    this.active = true;
                }
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.editRequirementButton);
        this.editRequirementButton.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.build_group_screen.edit_requirement.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.editRequirementButton);

        this.removeRequirementButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.requirements.screens.remove_requirement"), (button) -> {
            RequirementInstance i = this.getSelectedInstance();
            if (i != null) {
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.requirements.screens.remove_requirement.confirm"), MessageDialogStyle.WARNING, call -> {
                    if (call) {
                        this.group.removeInstance(i);
                        this.updateRequirementsScrollArea();
                    }
                });
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.getSelectedInstance() == null) {
                    this.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.build_group_screen.no_requirement_selected")));
                    this.active = false;
                } else {
                    this.setUITooltip((UITooltip) null);
                    this.active = true;
                }
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.removeRequirementButton);
        this.removeRequirementButton.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.build_group_screen.remove_requirement.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.removeRequirementButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.common_components.done"), (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.group);
        }) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                BuildRequirementGroupScreen s = BuildRequirementGroupScreen.this;
                if (s.group.getInstances().isEmpty()) {
                    this.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.build_group_screen.finish.no_requirements_added")));
                    this.active = false;
                } else if ((s.parent.getGroup(s.group.identifier) != null) && (s.parent.getGroup(s.group.identifier) != s.group)) {
                    this.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.build_group_screen.finish.identifier_already_used")));
                    this.active = false;
                } else if ((s.group.identifier == null) || (s.group.identifier.replace(" ", "").length() == 0)) {
                    this.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.build_group_screen.finish.identifier_too_short")));
                    this.active = false;
                } else {
                    this.setUITooltip((UITooltip) null);
                    this.active = true;
                }
                super.renderWidget(graphics, mouseX, mouseY, partialTicks);
            }
        };
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.common_components.cancel"), (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            if (this.isEdit) {
                this.callback.accept(this.group);
            } else {
                this.callback.accept(null);
            }
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.addRenderableWidget(this.requirementsScrollArea);

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

        graphics.fill(0, 0, this.width, this.height, UIBase.getUITheme().interface_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUITheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, I18n.get("fancymenu.requirements.screens.build_group_screen.group_requirements"), 20, 50, UIBase.getUITheme().generic_text_base_color.getColorInt(), false);

        this.requirementsScrollArea.setWidth(this.width - 20 - 150 - 20 - 20, true);
        this.requirementsScrollArea.setHeight(this.height - 85, true);
        this.requirementsScrollArea.setX(20, true);
        this.requirementsScrollArea.setY(50 + 15, true);

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

        String idLabel = I18n.get("fancymenu.requirements.screens.build_group_screen.group_identifier");
        int idLabelWidth = this.font.width(idLabel);
        graphics.drawString(this.font, idLabel, this.width - 20 - idLabelWidth, this.groupIdentifierTextField.getY() - 15, UIBase.getUITheme().generic_text_base_color.getColorInt(), false);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics $$0, int $$1, int $$2, float $$3) {
    }

    @Nullable
    protected RequirementInstance getSelectedInstance() {
        ScrollAreaEntry e = this.requirementsScrollArea.getFocusedEntry();
        if (e instanceof ManageRequirementsScreen.RequirementInstanceEntry) {
            return ((ManageRequirementsScreen.RequirementInstanceEntry) e).instance;
        }
        return null;
    }

    protected void updateRequirementsScrollArea() {

        this.requirementsScrollArea.clearEntries();

        for (RequirementInstance i : this.group.getInstances()) {
            ManageRequirementsScreen.RequirementInstanceEntry e = new ManageRequirementsScreen.RequirementInstanceEntry(this.requirementsScrollArea, i, 14);
            this.requirementsScrollArea.addEntry(e);
        }

    }

}
