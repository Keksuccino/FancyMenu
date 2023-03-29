//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.loadingrequirement;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.ConfirmationScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.internal.LoadingRequirementGroup;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.internal.LoadingRequirementInstance;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
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
    protected AdvancedButton groupModeButton;
    protected AdvancedButton addRequirementButton;
    protected AdvancedButton removeRequirementButton;
    protected AdvancedButton editRequirementButton;
    protected AdvancedButton doneButton;
    protected AdvancedButton cancelButton;
    protected AdvancedTextField groupIdentifierTextField;

    public BuildRequirementGroupScreen(@Nullable Screen parentScreen, @NotNull LoadingRequirementContainer parent, @Nullable LoadingRequirementGroup groupToEdit, @NotNull Consumer<LoadingRequirementGroup> callback) {

        super((groupToEdit != null) ? Component.literal(Locals.localize("fancymenu.editor.loading_requirement.screens.edit_group")) : Component.literal(Locals.localize("fancymenu.editor.loading_requirement.screens.add_group")));

        this.parentScreen = parentScreen;
        this.parent = parent;
        this.group = (groupToEdit != null) ? groupToEdit : new LoadingRequirementGroup("group_" + System.currentTimeMillis(), LoadingRequirementGroup.GroupMode.AND, parent);
        this.callback = callback;
        this.isEdit = groupToEdit != null;
        this.updateRequirementsScrollArea();

        this.groupIdentifierTextField = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 150, 20, true, CharacterFilter.getBasicFilenameCharacterFilter()) {
            @Override
            public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {
                super.render(matrix, mouseX, mouseY, partial);
                BuildRequirementGroupScreen.this.group.identifier = this.getValue();
            }
        };
        if (this.group.identifier != null) {
            this.groupIdentifierTextField.setValue(this.group.identifier);
        }

        this.groupModeButton = new AdvancedButton(0, 0, 150, 20, "", true, (button) -> {
            if (this.group.mode == LoadingRequirementGroup.GroupMode.AND) {
                this.group.mode = LoadingRequirementGroup.GroupMode.OR;
            } else {
                this.group.mode = LoadingRequirementGroup.GroupMode.AND;
            }
        }) {
            @Override
            public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.group.mode == LoadingRequirementGroup.GroupMode.AND) {
                    this.setMessage(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.mode.and"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.mode.or"));
                }
                super.render(matrix, mouseX, mouseY, partial);
            }
        };
        this.groupModeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.mode.desc"), "%n%"));
        UIBase.applyDefaultButtonSkinTo(this.groupModeButton);

        this.addRequirementButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.loading_requirement.screens.add_requirement"), true, (button) -> {
            BuildRequirementScreen s = new BuildRequirementScreen(this, this.parent, null, (call) -> {
                if (call != null) {
                    this.group.addInstance(call);
                    this.updateRequirementsScrollArea();
                }
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addRequirementButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.add_requirement.desc"), "%n%"));
        UIBase.applyDefaultButtonSkinTo(this.addRequirementButton);

        this.editRequirementButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.loading_requirement.screens.edit_requirement"), true, (button) -> {
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
            public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.getSelectedInstance() == null) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.no_requirement_selected"), "%n%"));
                    this.active = false;
                } else {
                    this.setDescription((String[])null);
                    this.active = true;
                }
                super.render(matrix, mouseX, mouseY, partial);
            }
        };
        this.editRequirementButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.edit_requirement.desc"), "%n%"));
        UIBase.applyDefaultButtonSkinTo(this.editRequirementButton);

        this.removeRequirementButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.loading_requirement.screens.remove_requirement"), true, (button) -> {
            LoadingRequirementInstance i = this.getSelectedInstance();
            if (i != null) {
                ConfirmationScreen s = new ConfirmationScreen(this, (call) -> {
                    if (call) {
                        this.group.removeInstance(i);
                        this.updateRequirementsScrollArea();
                    }
                }, StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.remove_requirement.confirm"), "%n%"));
                Minecraft.getInstance().setScreen(s);
            }
        }) {
            @Override
            public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {
                if (BuildRequirementGroupScreen.this.getSelectedInstance() == null) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.no_requirement_selected"), "%n%"));
                    this.active = false;
                } else {
                    this.setDescription((String[])null);
                    this.active = true;
                }
                super.render(matrix, mouseX, mouseY, partial);
            }
        };
        this.removeRequirementButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.remove_requirement.desc"), "%n%"));
        UIBase.applyDefaultButtonSkinTo(this.removeRequirementButton);

        this.doneButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.guicomponents.done"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.group);
        }) {
            @Override
            public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
                BuildRequirementGroupScreen s = BuildRequirementGroupScreen.this;
                if (s.group.getInstances().isEmpty()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.finish.no_requirements_added"), "%n%"));
                    this.active = false;
                } else if ((s.parent.getGroup(s.group.identifier) != null) && (s.parent.getGroup(s.group.identifier) != s.group)) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.finish.identifier_already_used"), "%n%"));
                    this.active = false;
                } else if ((s.group.identifier == null) || (s.group.identifier.replace(" ", "").length() == 0)) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.finish.identifier_too_short"), "%n%"));
                    this.active = false;
                } else {
                    this.setDescription((String[])null);
                    this.active = true;
                }
                super.renderButton(matrix, mouseX, mouseY, partialTicks);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

        this.cancelButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.guicomponents.cancel"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            if (this.isEdit) {
                this.callback.accept(this.group);
            } else {
                this.callback.accept(null);
            }
        });
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

    }

    @Override
    protected void init() {

        //Reset the GUI scale in case the layout editor changed it
        Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
		this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        super.init();

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

        fill(matrix, 0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(matrix, titleComp, 20, 20, -1);

        this.font.draw(matrix, Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.group_requirements"), 20, 50, -1);

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

        String idLabel = Locals.localize("fancymenu.editor.loading_requirement.screens.build_group_screen.group_identifier");
        int idLabelWidth = this.font.width(idLabel);
        this.font.draw(matrix, idLabel, this.width - 20 - idLabelWidth, this.groupIdentifierTextField.getY() - 15, -1);

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
