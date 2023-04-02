//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.actions;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.ConfirmationScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.*;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.component.Component;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.widget.AdvancedButton;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.ITextComponent;

import java.util.List;
import java.util.function.Consumer;

public class ManageActionsScreen extends Screen {

    protected GuiScreen parentScreen;
    protected List<ActionInstance> instances;

    protected ScrollArea actionsScrollArea = new ScrollArea(0, 0, 0, 0);
    protected Consumer<List<ActionInstance>> callback;
    protected AdvancedButton addActionButton;
    protected AdvancedButton moveUpButton;
    protected AdvancedButton moveDownButton;
    protected AdvancedButton editButton;
    protected AdvancedButton removeButton;
    protected AdvancedButton doneButton;

    public ManageActionsScreen(GuiScreen parentScreen, List<ActionInstance> instances, Consumer<List<ActionInstance>> callback) {

        super(Component.literal(Locals.localize("fancymenu.editor.action.screens.manage_screen.manage")));

        this.parentScreen = parentScreen;
        this.callback = callback;
        this.instances = instances;
        this.updateActionInstanceScrollArea(false);

        this.addActionButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.add_action"), true, (button) -> {
            BuildActionScreen s = new BuildActionScreen(this, null, (call) -> {
                if (call != null) {
                    this.instances.add(call);
                    this.updateActionInstanceScrollArea(false);
                }
            });
            Minecraft.getMinecraft().displayGuiScreen(s);
        });
        this.addActionButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.add_action.desc"), "%n%"));
        UIBase.applyDefaultButtonSkinTo(this.addActionButton);

        this.moveUpButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.move_action_up"), true, (button) -> {
            if (this.isInstanceSelected()) {
                ActionInstance selected = this.getSelectedInstance();
                int index = this.instances.indexOf(selected);
                if (index > 0) {
                    this.instances.remove(selected);
                    this.instances.add(index-1, selected);
                    this.updateActionInstanceScrollArea(true);
                    for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
                        if ((e instanceof ActionInstanceEntry) && (((ActionInstanceEntry)e).instance == selected)) {
                            e.setFocused(true);
                            break;
                        }
                    }
                }
            }
        }) {
            @Override
            public void render(int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.finish.no_action_selected"), "%n%"));
                    this.enabled = false;
                } else {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.move_action_up.desc"), "%n%"));
                    this.enabled = true;
                }
                super.render(p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.moveUpButton);

        this.moveDownButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.move_action_down"), true, (button) -> {
            if (this.isInstanceSelected()) {
                ActionInstance selected = this.getSelectedInstance();
                int index = this.instances.indexOf(selected);
                if ((index >= 0) && (index <= this.instances.size()-2)) {
                    this.instances.remove(selected);
                    this.instances.add(index+1, selected);
                    this.updateActionInstanceScrollArea(true);
                    for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
                        if ((e instanceof ActionInstanceEntry) && (((ActionInstanceEntry)e).instance == selected)) {
                            e.setFocused(true);
                            break;
                        }
                    }
                }
            }
        }) {
            @Override
            public void render(int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.finish.no_action_selected"), "%n%"));
                    this.enabled = false;
                } else {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.move_action_down.desc"), "%n%"));
                    this.enabled = true;
                }
                super.render(p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.moveDownButton);

        this.editButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.edit_action"), true, (button) -> {
            if (this.isInstanceSelected()) {
                BuildActionScreen s = new BuildActionScreen(this, this.getSelectedInstance(), (call) -> {
                    this.updateActionInstanceScrollArea(false);
                });
                Minecraft.getMinecraft().displayGuiScreen(s);
            }
        }) {
            @Override
            public void render(int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.finish.no_action_selected"), "%n%"));
                    this.enabled = false;
                } else {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.edit_action.desc"), "%n%"));
                    this.enabled = true;
                }
                super.render(p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.editButton);

        this.removeButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.remove_action"), true, (button) -> {
            if (this.isInstanceSelected()) {
                ActionInstance i = this.getSelectedInstance();
                Screen s = new ConfirmationScreen(this, (call) -> {
                    if (call) {
                        this.instances.remove(i);
                        this.updateActionInstanceScrollArea(false);
                    }
                }, StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.remove_action.confirm"), "%n%"));
                Minecraft.getMinecraft().displayGuiScreen(s);
            }
        }) {
            @Override
            public void render(int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.finish.no_action_selected"), "%n%"));
                    this.enabled = false;
                } else {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.remove_action.desc"), "%n%"));
                    this.enabled = true;
                }
                super.render(p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.removeButton);

        this.doneButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.guicomponents.done"), true, (button) -> {
            Minecraft.getMinecraft().displayGuiScreen(this.parentScreen);
            this.callback.accept(this.instances);
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
        this.callback.accept(this.instances);
    }

    @Override
    public void render(int mouseX, int mouseY, float partial) {

        fill(0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        this.title.getStyle().setBold(true);
        AbstractGui.drawFormattedString(this.font, this.title, 20, 20, -1);

        this.font.drawString(Locals.localize("fancymenu.editor.action.screens.manage_screen.actions"), 20, 50, -1);

        this.actionsScrollArea.setWidth(this.width - 20 - 150 - 20 - 20, true);
        this.actionsScrollArea.setHeight(this.height - 85, true);
        this.actionsScrollArea.setX(20, true);
        this.actionsScrollArea.setY(50 + 15, true);
        this.actionsScrollArea.render(mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(mouseX, mouseY, partial);

        this.removeButton.setX(this.width - 20 - this.removeButton.getWidth());
        this.removeButton.setY(this.doneButton.getY() - 15 - 20);
        this.removeButton.render(mouseX, mouseY, partial);

        this.editButton.setX(this.width - 20 - this.editButton.getWidth());
        this.editButton.setY(this.removeButton.getY() - 5 - 20);
        this.editButton.render(mouseX, mouseY, partial);

        this.moveDownButton.setX(this.width - 20 - this.moveDownButton.getWidth());
        this.moveDownButton.setY(this.editButton.getY() - 5 - 20);
        this.moveDownButton.render(mouseX, mouseY, partial);

        this.moveUpButton.setX(this.width - 20 - this.moveUpButton.getWidth());
        this.moveUpButton.setY(this.moveDownButton.getY() - 5 - 20);
        this.moveUpButton.render(mouseX, mouseY, partial);

        this.addActionButton.setX(this.width - 20 - this.addActionButton.getWidth());
        this.addActionButton.setY(this.moveUpButton.getY() - 5 - 20);
        this.addActionButton.render(mouseX, mouseY, partial);

        super.render(mouseX, mouseY, partial);

    }

    
    protected ActionInstance getSelectedInstance() {
        ScrollAreaEntry e = this.actionsScrollArea.getFocusedEntry();
        if (e instanceof ActionInstanceEntry) {
            return ((ActionInstanceEntry)e).instance;
        }
        return null;
    }

    protected boolean isInstanceSelected() {
        return this.getSelectedInstance() != null;
    }

    protected void updateActionInstanceScrollArea(boolean keepScroll) {

        float oldScrollVertical = this.actionsScrollArea.verticalScrollBar.getScroll();
        float oldScrollHorizontal = this.actionsScrollArea.horizontalScrollBar.getScroll();

        this.actionsScrollArea.clearEntries();

        for (ActionInstance i : this.instances) {
            ActionInstanceEntry e = new ActionInstanceEntry(this.actionsScrollArea, i, 14);
            this.actionsScrollArea.addEntry(e);
        }

        if (keepScroll) {
            this.actionsScrollArea.verticalScrollBar.setScroll(oldScrollVertical);
            this.actionsScrollArea.horizontalScrollBar.setScroll(oldScrollHorizontal);
        }

    }

    public static class ActionInstanceEntry extends ScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;

        public ActionInstance instance;
        public final int lineHeight;
        public FontRenderer font = Minecraft.getMinecraft().fontRenderer;

        private ITextComponent displayNameComponent;
        private ITextComponent valueComponent;

        public ActionInstanceEntry(ScrollArea parent, ActionInstance instance, int lineHeight) {

            super(parent, 100, 30);
            this.instance = instance;
            this.lineHeight = lineHeight;

            this.displayNameComponent = Component.literal(this.instance.action.getAction()).setStyle(new TextStyle().setColorRGB(TEXT_COLOR_GRAY_1.getRGB()));
            String valueString = ((this.instance.value != null) && this.instance.action.hasValue()) ? this.instance.value : Locals.localize("fancymenu.editor.action.screens.manage_screen.info.value.none");
            this.valueComponent = Component.literal(Locals.localize("fancymenu.editor.action.screens.manage_screen.info.value") + " ").setStyle(new TextStyle().setColorRGB(TEXT_COLOR_GRAY_1.getRGB())).append(Component.literal(valueString).setStyle(new TextStyle().setColorRGB(TEXT_COLOR_GREY_4.getRGB())));

            this.setWidth(this.calculateWidth());
            this.setHeight((lineHeight * 2) + (HEADER_FOOTER_HEIGHT * 2));

        }

        @Override
        public void render(int mouseX, int mouseY, float partial) {

            super.render(mouseX, mouseY, partial);

            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            int centerYLine2 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);

            RenderSystem.enableBlend();

            renderListingDot(this.getX() + 5, centerYLine1 - 2, LISTING_DOT_RED);
            AbstractGui.drawFormattedString(this.font, this.displayNameComponent, (this.getX() + 5 + 4 + 3), (centerYLine1 - (this.font.FONT_HEIGHT / 2)), -1);

            renderListingDot(this.getX() + 5 + 4 + 3, centerYLine2 - 2, LISTING_DOT_BLUE);
            AbstractGui.drawFormattedString(this.font, this.valueComponent, (this.getX() + 5 + 4 + 3 + 4 + 3), (centerYLine2 - (this.font.FONT_HEIGHT / 2)), -1);

        }

        private int calculateWidth() {
            int w = 5 + 4 + 3 + this.font.getStringWidth(this.displayNameComponent.getFormattedText()) + 5;
            int w2 = 5 + 4 + 3 + 4 + 3 + this.font.getStringWidth(this.valueComponent.getFormattedText()) + 5;
            if (w2 > w) {
                w = w2;
            }
            return w;
        }

        @Override
        public void onClick(ScrollAreaEntry entry) {}

    }

    public static class ActionInstance {

        public ButtonActionContainer action;
        public String value;

        public ActionInstance(ButtonActionContainer action,  String value) {
            this.action = action;
            this.value = value;
        }

    }

}
