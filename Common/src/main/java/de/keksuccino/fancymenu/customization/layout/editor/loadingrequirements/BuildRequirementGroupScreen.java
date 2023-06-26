
package de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementGroup;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.fancymenu.util.LocalizationUtils;
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
    protected AdvancedTextField groupIdentifierTextField;

    public BuildRequirementGroupScreen(@Nullable Screen parentScreen, @NotNull LoadingRequirementContainer parent, @Nullable LoadingRequirementGroup groupToEdit, @NotNull Consumer<LoadingRequirementGroup> callback) {

        super((groupToEdit != null) ? Component.literal(I18n.get("fancymenu.editor.loading_requirement.screens.edit_group")) : Component.literal(I18n.get("fancymenu.editor.loading_requirement.screens.add_group")));

        this.parentScreen = parentScreen;
        this.parent = parent;
        this.group = (groupToEdit != null) ? groupToEdit : new LoadingRequirementGroup("group_" + System.currentTimeMillis(), LoadingRequirementGroup.GroupMode.AND, parent);
        this.callback = callback;
        this.isEdit = groupToEdit != null;
        this.updateRequirementsScrollArea();

        this.groupIdentifierTextField = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 150, 20, true, CharacterFilter.getBasicFilenameCharacterFilter()) {
            @Override
            public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partial) {
                super.render(matrix, mouseX, mouseY, partial);
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

        super.init();

        this.groupModeButton = new ExtendedButton(0, 0, 150, 20, "", (button) -> {
            if (this.group.mode == LoadingRequirementGroup.GroupMode.AND) {
                this.group.mode = LoadingRequirementGroup.GroupMode.OR;
            } else {
                this.group.mode = LoadingRequirementGroup.GroupMode.AND;
            }
        }) {
            @Override
            public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.group.mode == LoadingRequirementGroup.GroupMode.AND) {
                    this.setLabel(I18n.get("fancymenu.editor.loading_requirement.screens.build_group_screen.mode.and"));
                } else {
                    this.setLabel(I18n.get("fancymenu.editor.loading_requirement.screens.build_group_screen.mode.or"));
                }
                super.render(matrix, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.groupModeButton);
        this.groupModeButton.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.mode.desc")));
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
        this.addRequirementButton.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.add_requirement.desc")));
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
            public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.getSelectedInstance() == null) {
                    this.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.no_requirement_selected")));
                    this.active = false;
                } else {
                    this.setTooltip((Tooltip) null);
                    this.active = true;
                }
                super.render(matrix, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.editRequirementButton);
        this.editRequirementButton.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.edit_requirement.desc")));
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
            public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.getSelectedInstance() == null) {
                    this.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.no_requirement_selected")));
                    this.active = false;
                } else {
                    this.setTooltip((Tooltip) null);
                    this.active = true;
                }
                super.render(matrix, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.removeRequirementButton);
        this.removeRequirementButton.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.remove_requirement.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.removeRequirementButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.guicomponents.done"), (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.group);
        }) {
            @Override
            public void renderWidget(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
                BuildRequirementGroupScreen s = BuildRequirementGroupScreen.this;
                if (s.group.getInstances().isEmpty()) {
                    this.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.finish.no_requirements_added")));
                    this.active = false;
                } else if ((s.parent.getGroup(s.group.identifier) != null) && (s.parent.getGroup(s.group.identifier) != s.group)) {
                    this.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.finish.identifier_already_used")));
                    this.active = false;
                } else if ((s.group.identifier == null) || (s.group.identifier.replace(" ", "").length() == 0)) {
                    this.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.loading_requirement.screens.build_group_screen.finish.identifier_too_short")));
                    this.active = false;
                } else {
                    this.setTooltip((Tooltip) null);
                    this.active = true;
                }
                super.renderWidget(matrix, mouseX, mouseY, partialTicks);
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
    public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partial) {

        fill(matrix, 0, 0, this.width, this.height, UIBase.getUIColorScheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(matrix, titleComp, 20, 20, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        this.font.draw(matrix, I18n.get("fancymenu.editor.loading_requirement.screens.build_group_screen.group_requirements"), 20, 50, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        this.requirementsScrollArea.setWidth(this.width - 20 - 150 - 20 - 20, true);
        this.requirementsScrollArea.setHeight(this.height - 85, true);
        this.requirementsScrollArea.setX(20, true);
        this.requirementsScrollArea.setY(50 + 15, true);
        this.requirementsScrollArea.render(matrix, mouseX, mouseY, partial);

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

        this.removeRequirementButton.setX(this.width - 20 - this.removeRequirementButton.getWidth());
        this.removeRequirementButton.setY(((this.isEdit) ? this.doneButton.getY() : this.cancelButton.getY()) - 15 - 20);
        this.removeRequirementButton.render(matrix, mouseX, mouseY, partial);

        this.editRequirementButton.setX(this.width - 20 - this.editRequirementButton.getWidth());
        this.editRequirementButton.setY(this.removeRequirementButton.getY() - 5 - 20);
        this.editRequirementButton.render(matrix, mouseX, mouseY, partial);

        this.addRequirementButton.setX(this.width - 20 - this.addRequirementButton.getWidth());
        this.addRequirementButton.setY(this.editRequirementButton.getY() - 5 - 20);
        this.addRequirementButton.render(matrix, mouseX, mouseY, partial);

        this.groupModeButton.setX(this.width - 20 - this.groupModeButton.getWidth());
        this.groupModeButton.setY(this.addRequirementButton.getY() - 5 - 20);
        this.groupModeButton.render(matrix, mouseX, mouseY, partial);

        this.groupIdentifierTextField.setX(this.width - 20 - this.groupIdentifierTextField.getWidth());
        this.groupIdentifierTextField.setY(this.groupModeButton.getY() - 15 - 20);
        this.groupIdentifierTextField.render(matrix, mouseX, mouseY, partial);

        String idLabel = I18n.get("fancymenu.editor.loading_requirement.screens.build_group_screen.group_identifier");
        int idLabelWidth = this.font.width(idLabel);
        this.font.draw(matrix, idLabel, this.width - 20 - idLabelWidth, this.groupIdentifierTextField.getY() - 15, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        super.render(matrix, mouseX, mouseY, partial);

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
