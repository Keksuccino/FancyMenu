//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.loadingrequirement;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.ConfirmationScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.*;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.component.Component;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.widget.AdvancedButton;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.internal.LoadingRequirementGroup;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.internal.LoadingRequirementInstance;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;

public class ManageRequirementsScreen extends Screen {

    protected GuiScreen parentScreen;
    protected LoadingRequirementContainer container;
    protected Consumer<LoadingRequirementContainer> callback;

    protected ScrollArea requirementsScrollArea = new ScrollArea(0, 0, 0, 0);
    protected AdvancedButton addRequirementButton;
    protected AdvancedButton addGroupButton;
    protected AdvancedButton editButton;
    protected AdvancedButton removeButton;
    protected AdvancedButton doneButton;

    public ManageRequirementsScreen(GuiScreen parentScreen, LoadingRequirementContainer container, Consumer<LoadingRequirementContainer> callback) {

        super(Component.literal(Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.manage")));

        this.parentScreen = parentScreen;
        this.container = container;
        this.callback = callback;
        this.updateRequirementsScrollArea();

        this.addRequirementButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.loading_requirement.screens.add_requirement"), true, (button) -> {
            BuildRequirementScreen s = new BuildRequirementScreen(this, this.container, null, (call) -> {
                if (call != null) {
                    this.container.addInstance(call);
                    this.updateRequirementsScrollArea();
                }
            });
            Minecraft.getMinecraft().displayGuiScreen(s);
        });
        this.addRequirementButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.add_requirement.desc"), "%n%"));
        UIBase.applyDefaultButtonSkinTo(this.addRequirementButton);

        this.addGroupButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.loading_requirement.screens.add_group"), true, (button) -> {
            BuildRequirementGroupScreen s = new BuildRequirementGroupScreen(this, this.container, null, (call) -> {
                if (call != null) {
                    this.container.addGroup(call);
                    this.updateRequirementsScrollArea();
                }
            });
            Minecraft.getMinecraft().displayGuiScreen(s);
        });
        this.addGroupButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.add_group.desc"), "%n%"));
        UIBase.applyDefaultButtonSkinTo(this.addGroupButton);

        this.editButton = new AdvancedButton(0, 0, 150, 20, "", true, (button) -> {
            Screen s = null;
            if (this.isInstanceSelected()) {
                s = new BuildRequirementScreen(this, this.container, this.getSelectedInstance(), (call) -> {
                    if (call != null) {
                        this.updateRequirementsScrollArea();
                    }
                });
            } else if (this.isGroupSelected()) {
                s = new BuildRequirementGroupScreen(this, this.container, this.getSelectedGroup(), (call) -> {
                    if (call != null) {
                        this.updateRequirementsScrollArea();
                    }
                });
            }
            if (s != null) {
                Minecraft.getMinecraft().displayGuiScreen(s);
            }
        }) {
            @Override
            public void render(int p_93658_, int p_93659_, float p_93660_) {
                ManageRequirementsScreen s = ManageRequirementsScreen.this;
                if (!s.isInstanceSelected() && !s.isGroupSelected()) {
                    this.setMessage(Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.edit.generic"));
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.no_entry_selected"), "%n%"));
                    this.enabled = false;
                } else {
                    if (s.isInstanceSelected()) {
                        this.setMessage(Locals.localize("fancymenu.editor.loading_requirement.screens.edit_requirement"));
                    } else {
                        this.setMessage(Locals.localize("fancymenu.editor.loading_requirement.screens.edit_group"));
                    }
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.edit.desc"), "%n%"));
                    this.enabled = true;
                }
                super.render(p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.editButton);

        this.removeButton = new AdvancedButton(0, 0, 150, 20, "", true, (button) -> {
            Screen s = null;
            if (this.isInstanceSelected()) {
                LoadingRequirementInstance i = this.getSelectedInstance();
                s = new ConfirmationScreen(this, (call) -> {
                    if (call) {
                        this.container.removeInstance(i);
                        this.updateRequirementsScrollArea();
                    }
                }, StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.remove_requirement.confirm"), "%n%"));
            } else if (this.isGroupSelected()) {
                LoadingRequirementGroup g = this.getSelectedGroup();
                s = new ConfirmationScreen(this, (call) -> {
                    if (call) {
                        this.container.removeGroup(g);
                        this.updateRequirementsScrollArea();
                    }
                }, StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.remove_group.confirm"), "%n%"));
            }
            if (s != null) {
                Minecraft.getMinecraft().displayGuiScreen(s);
            }
        }) {
            @Override
            public void render(int p_93658_, int p_93659_, float p_93660_) {
                ManageRequirementsScreen s = ManageRequirementsScreen.this;
                if (!s.isInstanceSelected() && !s.isGroupSelected()) {
                    this.setMessage(Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.remove.generic"));
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.no_entry_selected"), "%n%"));
                    this.enabled = false;
                } else {
                    if (s.isInstanceSelected()) {
                        this.setMessage(Locals.localize("fancymenu.editor.loading_requirement.screens.remove_requirement"));
                    } else {
                        this.setMessage(Locals.localize("fancymenu.editor.loading_requirement.screens.remove_group"));
                    }
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.remove.desc"), "%n%"));
                    this.enabled = true;
                }
                super.render(p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.removeButton);

        this.doneButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.guicomponents.done"), true, (button) -> {
            Minecraft.getMinecraft().displayGuiScreen(this.parentScreen);
            this.callback.accept(this.container);
        });
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

    }

    @Override
    protected void init() {

        //Reset GUI scale in case it was changed by the layout editor
        if ((this.parentScreen != null) && (this.parentScreen instanceof LayoutEditorScreen)) {
            if (((LayoutEditorScreen)this.parentScreen).oriscale != -1) {
                Minecraft.getMinecraft().gameSettings.guiScale = ((LayoutEditorScreen)this.parentScreen).oriscale;
                ((LayoutEditorScreen)this.parentScreen).oriscale = -1;
                ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
                this.width = res.getScaledWidth();
                this.height = res.getScaledHeight();
            }
        }

        super.init();

    }

    @Override
    public void onClose() {
        Minecraft.getMinecraft().displayGuiScreen(this.parentScreen);
        this.callback.accept(this.container);
    }

    @Override
    public void render(int mouseX, int mouseY, float partial) {

        fill(0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        this.title.getStyle().setBold(true);
        AbstractGui.drawFormattedString(this.font, this.title, 20, 20, -1);

        this.font.drawString(Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.requirements_and_groups"), 20, 50, -1);

        this.requirementsScrollArea.setWidth(this.width - 20 - 150 - 20 - 20, true);
        this.requirementsScrollArea.setHeight(this.height - 85, true);
        this.requirementsScrollArea.setX(20, true);
        this.requirementsScrollArea.setY(50 + 15, true);
        this.requirementsScrollArea.render(mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(mouseX, mouseY, partial);

        this.removeButton.setX(this.width - 20 - this.removeButton.getWidth());
        this.removeButton.setY(this.doneButton.getY() - 15 - 20);
        this.removeButton.render(mouseX, mouseY, partial);

        this.editButton.setX(this.width - 20 - this.editButton.getWidth());
        this.editButton.setY(this.removeButton.getY() - 5 - 20);
        this.editButton.render(mouseX, mouseY, partial);

        this.addGroupButton.setX(this.width - 20 - this.addGroupButton.getWidth());
        this.addGroupButton.setY(this.editButton.getY() - 5 - 20);
        this.addGroupButton.render(mouseX, mouseY, partial);

        this.addRequirementButton.setX(this.width - 20 - this.addRequirementButton.getWidth());
        this.addRequirementButton.setY(this.addGroupButton.getY() - 5 - 20);
        this.addRequirementButton.render(mouseX, mouseY, partial);

        super.render(mouseX, mouseY, partial);

    }

    
    protected LoadingRequirementInstance getSelectedInstance() {
        ScrollAreaEntry e = this.requirementsScrollArea.getFocusedEntry();
        if (e instanceof RequirementInstanceEntry) {
            return ((RequirementInstanceEntry)e).instance;
        }
        return null;
    }

    protected boolean isInstanceSelected() {
        return this.getSelectedInstance() != null;
    }

    
    protected LoadingRequirementGroup getSelectedGroup() {
        ScrollAreaEntry e = this.requirementsScrollArea.getFocusedEntry();
        if (e instanceof RequirementGroupEntry) {
            return ((RequirementGroupEntry)e).group;
        }
        return null;
    }

    protected boolean isGroupSelected() {
        return this.getSelectedGroup() != null;
    }

    protected void updateRequirementsScrollArea() {

        this.requirementsScrollArea.clearEntries();

        for (LoadingRequirementGroup g : this.container.getGroups()) {
            RequirementGroupEntry e = new RequirementGroupEntry(this.requirementsScrollArea, g);
            this.requirementsScrollArea.addEntry(e);
        }

        for (LoadingRequirementInstance i : this.container.getInstances()) {
            RequirementInstanceEntry e = new RequirementInstanceEntry(this.requirementsScrollArea, i, 14);
            this.requirementsScrollArea.addEntry(e);
        }

    }

    public static class RequirementGroupEntry extends TextListScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;

        public LoadingRequirementGroup group;

        public RequirementGroupEntry(ScrollArea parent, LoadingRequirementGroup group) {
            super(parent, Component.literal(group.identifier).setStyle(new TextStyle().setColorRGB(TEXT_COLOR_GRAY_1.getRGB())).append(Component.literal(" (" + Locals.localize("fancymenu.editor.loading_requirement.screens.manage_screen.group.info", "" + group.getInstances().size()) + ")").setStyle(new TextStyle().setColorRGB(TEXT_COLOR_GREY_4.getRGB()))), LISTING_DOT_ORANGE, (entry) -> {});
            this.group = group;
            this.setHeight(this.getHeight() + (HEADER_FOOTER_HEIGHT * 2));
        }

    }

    public static class RequirementInstanceEntry extends ScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;

        public LoadingRequirementInstance instance;
        public final int lineHeight;
        public FontRenderer font = Minecraft.getMinecraft().fontRenderer;

        private ITextComponent displayNameComponent;
        private ITextComponent modeComponent;
        private ITextComponent valueComponent;

        public RequirementInstanceEntry(ScrollArea parent, LoadingRequirementInstance instance, int lineHeight) {

            super(parent, 100, 30);
            this.instance = instance;
            this.lineHeight = lineHeight;

            this.displayNameComponent = Component.literal(this.instance.requirement.getDisplayName()).setStyle(new TextStyle().setColorRGB(TEXT_COLOR_GRAY_1.getRGB()));
            String modeString = (this.instance.mode == LoadingRequirementInstance.RequirementMode.IF) ? Locals.localize("fancymenu.editor.loading_requirement.screens.requirement.info.mode.normal") : Locals.localize("fancymenu.editor.loading_requirement.screens.requirement.info.mode.opposite");
            this.modeComponent = Component.literal(Locals.localize("fancymenu.editor.loading_requirement.screens.requirement.info.mode") + " ").setStyle(new TextStyle().setColorRGB(TEXT_COLOR_GRAY_1.getRGB())).append(Component.literal(modeString).setStyle(new TextStyle().setColorRGB(TEXT_COLOR_GREY_4.getRGB())));
            String valueString = (this.instance.value != null) ? this.instance.value : Locals.localize("fancymenu.editor.loading_requirement.screens.requirement.info.value.none");
            this.valueComponent = Component.literal(Locals.localize("fancymenu.editor.loading_requirement.screens.requirement.info.value") + " ").setStyle(new TextStyle().setColorRGB(TEXT_COLOR_GRAY_1.getRGB())).append(Component.literal(valueString).setStyle(new TextStyle().setColorRGB(TEXT_COLOR_GREY_4.getRGB())));

            this.setWidth(this.calculateWidth());
            this.setHeight((lineHeight * 3) + (HEADER_FOOTER_HEIGHT * 2));

        }

        @Override
        public void render(int mouseX, int mouseY, float partial) {

            super.render(mouseX, mouseY, partial);

            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            int centerYLine2 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);
            int centerYLine3 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 5);

            RenderSystem.enableBlend();

            renderListingDot(this.getX() + 5, centerYLine1 - 2, LISTING_DOT_RED);
            AbstractGui.drawFormattedString(this.font, this.displayNameComponent, (this.getX() + 5 + 4 + 3), (centerYLine1 - (this.font.FONT_HEIGHT / 2)), -1);

            renderListingDot(this.getX() + 5 + 4 + 3, centerYLine2 - 2, LISTING_DOT_BLUE);
            AbstractGui.drawFormattedString(this.font, this.modeComponent, (this.getX() + 5 + 4 + 3 + 4 + 3), (centerYLine2 - (this.font.FONT_HEIGHT / 2)), -1);

            renderListingDot(this.getX() + 5 + 4 + 3, centerYLine3 - 2, LISTING_DOT_BLUE);
            AbstractGui.drawFormattedString(this.font, this.valueComponent, (this.getX() + 5 + 4 + 3 + 4 + 3), (centerYLine3 - (this.font.FONT_HEIGHT / 2)), -1);

        }

        private int calculateWidth() {
            int w = 5 + 4 + 3 + this.font.getStringWidth(this.displayNameComponent.getFormattedText()) + 5;
            int w2 = 5 + 4 + 3 + 4 + 3 + this.font.getStringWidth(this.modeComponent.getFormattedText()) + 5;
            int w3 = 5 + 4 + 3 + 4 + 3 + this.font.getStringWidth(this.valueComponent.getFormattedText()) + 5;
            if (w2 > w) {
                w = w2;
            }
            if (w3 > w) {
                w = w3;
            }
            return w;
        }

        @Override
        public void onClick(ScrollAreaEntry entry) {}

    }

}
