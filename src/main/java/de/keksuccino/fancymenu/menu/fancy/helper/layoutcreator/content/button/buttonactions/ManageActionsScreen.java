//TODO übernehmenn
//package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.buttonactions;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
//import de.keksuccino.fancymenu.menu.fancy.helper.ui.ScrollableScreen;
//import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
//import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMYesNoPopup;
//import de.keksuccino.konkrete.gui.content.AdvancedButton;
//import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
//import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
//import de.keksuccino.konkrete.input.MouseInput;
//import de.keksuccino.konkrete.input.StringUtils;
//import de.keksuccino.konkrete.localization.Locals;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.Font;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.network.chat.Component;
//
//import java.awt.*;
//import java.util.List;
//
//public class ManageActionsScreen extends ScrollableScreen {
//
//    protected static final Color ENTRY_BACK_1 = new Color(0, 0, 0, 50);
//    protected static final Color ENTRY_BACK_2 = new Color(0, 0, 0, 90);
//    protected static final Color SELECTED_ENTRY_BACK_COLOR = new Color(222, 174, 87); //237, 154, 59
//
//    protected int entryBackTick = 0;
//    protected AdvancedButton doneButton;
//    protected AdvancedButton removeButton;
//    protected AdvancedButton editButton;
//    protected AdvancedButton moveUpButton;
//    protected AdvancedButton moveDownButton;
//    protected AdvancedButton addButton;
//
//    protected ActionScrollEntry selected;
//    protected List<ButtonScriptEngine.ActionContainer> actions;
//
//    public ManageActionsScreen(Screen parent, List<ButtonScriptEngine.ActionContainer> actions) {
//
//        super(parent, Locals.localize("fancymenu.customization.items.ticker.manage_actions"));
//
//        this.actions = actions;
//
//        this.doneButton = new AdvancedButton(0, 0, 60, 20, Locals.localize("fancymenu.guicomponents.done"), true, (press) -> {
//            this.onClose();
//        });
//        this.doneButton.ignoreLeftMouseDownClickBlock = true;
//        UIBase.colorizeButton(this.doneButton);
//
//        this.removeButton = new AdvancedButton(0, 0, 60, 20, Locals.localize("fancymenu.customization.items.ticker.manage_actions.remove"), true, (press) -> {
//            FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call) -> {
//                if (call) {
//                    if (this.selected != null) {
//                        this.actions.remove(this.selected.action);
//                        Minecraft.getInstance().setScreen(new ManageActionsScreen(this.parent, this.actions));
//                    }
//                }
//            }, StringUtils.splitLines(Locals.localize("fancymenu.customization.items.ticker.manage_actions.remove.confirm"), "%n%"));
//            PopupHandler.displayPopup(p);
//        });
//        this.removeButton.ignoreLeftMouseDownClickBlock = true;
//        UIBase.colorizeButton(this.removeButton);
//
//        this.editButton = new AdvancedButton(0, 0, 60, 20, Locals.localize("fancymenu.customization.items.ticker.manage_actions.edit"), true, (press) -> {
//            if (this.selected != null) {
//                ButtonActionScreen s = new ButtonActionScreen(this, (call) -> {
//                    if (call != null) {
//                        this.selected.action.action = call.get(0);
//                        this.selected.action.value = call.get(1);
//                    }
//                });
//                s.setButtonAction(this.selected.action.action);
//                if (this.selected.action.value != null) {
//                    s.setValueString(this.selected.action.value);
//                }
//                Minecraft.getInstance().setScreen(s);
//            }
//        });
//        this.editButton.ignoreLeftMouseDownClickBlock = true;
//        UIBase.colorizeButton(this.editButton);
//
//        this.moveUpButton = new AdvancedButton(0, 0, 60, 20, Locals.localize("fancymenu.editor.actions.manage.move_up"), true, (press) -> {
//            if (this.selected != null) {
//                int index = this.actions.indexOf(this.selected.action);
//                if (index > 0) {
//                    this.actions.remove(this.selected.action);
//                    this.actions.add(index-1, this.selected.action);
//                    Minecraft.getInstance().setScreen(new ManageActionsScreen(this.parent, this.actions));
//                }
//            }
//        });
//        this.moveUpButton.ignoreLeftMouseDownClickBlock = true;
//        UIBase.colorizeButton(this.moveUpButton);
//
//        this.moveDownButton = new AdvancedButton(0, 0, 60, 20, Locals.localize("fancymenu.editor.actions.manage.move_down"), true, (press) -> {
//            if (this.selected != null) {
//                int index = this.actions.indexOf(this.selected.action);
//                if ((index >= 0) && (index <= this.actions.size()-2)) {
//                    this.actions.remove(this.selected.action);
//                    this.actions.add(index+1, this.selected.action);
//                    Minecraft.getInstance().setScreen(new ManageActionsScreen(this.parent, this.actions));
//                }
//            }
//        });
//        this.moveDownButton.ignoreLeftMouseDownClickBlock = true;
//        UIBase.colorizeButton(this.moveDownButton);
//
//        this.addButton = new AdvancedButton(0, 0, 60, 20, Locals.localize("fancymenu.editor.actions.manage.add"), true, (press) -> {
//            Minecraft.getInstance().setScreen(new ButtonActionScreen(this, (call) -> {
//                if (call != null) {
//                    this.actions.add(new ButtonScriptEngine.ActionContainer(call.get(0), call.get(1)));
//                    Minecraft.getInstance().setScreen(new ManageActionsScreen(this.parent, this.actions));
//                }
//            }));
//        });
//        this.addButton.ignoreLeftMouseDownClickBlock = true;
//        UIBase.colorizeButton(this.addButton);
//
//        //Add all actions to the list
//        for (ButtonScriptEngine.ActionContainer c : this.actions) {
//            if (this.entryBackTick == 0) {
//                this.scrollArea.addEntry(new ActionScrollEntry(this.scrollArea, this, c, ENTRY_BACK_1));
//                this.entryBackTick = 1;
//            } else {
//                this.scrollArea.addEntry(new ActionScrollEntry(this.scrollArea, this, c, ENTRY_BACK_2));
//                this.entryBackTick = 0;
//            }
//        }
//
//    }
//
//    @Override
//    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
//
//        super.render(matrix, mouseX, mouseY, partialTicks);
//
//        int xCenter = this.width / 2;
//
//        // ADD | MOVE UP | MOVE DOWN | EDIT | REMOVE | DONE
//
//        this.moveDownButton.setX(xCenter - this.moveDownButton.getWidth() - 3);
//        this.moveDownButton.setY(this.height - 35);
//        this.moveDownButton.active = this.selected != null;
//        this.moveDownButton.render(matrix, mouseX, mouseY, partialTicks);
//
//        this.moveUpButton.setX(this.moveDownButton.x - this.moveUpButton.getWidth() - 5);
//        this.moveUpButton.setY(this.height - 35);
//        this.moveUpButton.active = this.selected != null;
//        this.moveUpButton.render(matrix, mouseX, mouseY, partialTicks);
//
//        this.addButton.setX(this.moveUpButton.x - this.addButton.getWidth() - 5);
//        this.addButton.setY(this.height - 35);
//        this.addButton.render(matrix, mouseX, mouseY, partialTicks);
//
//        this.editButton.setX(xCenter + 2);
//        this.editButton.setY(this.height - 35);
//        this.editButton.active = this.selected != null;
//        this.editButton.render(matrix, mouseX, mouseY, partialTicks);
//
//        this.removeButton.setX(this.editButton.x + this.editButton.getWidth() + 5);
//        this.removeButton.setY(this.height - 35);
//        this.removeButton.active = this.selected != null;
//        this.removeButton.render(matrix, mouseX, mouseY, partialTicks);
//
//        this.doneButton.setX(this.removeButton.x + this.removeButton.getWidth() + 5);
//        this.doneButton.setY(this.height - 35);
//        this.doneButton.render(matrix, mouseX, mouseY, partialTicks);
//
//        if (PopupHandler.isPopupActive()) {
//            this.doneButton.active = false;
//            this.editButton.active = false;
//            this.removeButton.active = false;
//            this.moveUpButton.active = false;
//            this.moveDownButton.active = false;
//            this.addButton.active = false;
//        } else {
//            this.doneButton.active = true;
//            this.addButton.active = true;
//        }
//
//    }
//
//    @Override
//    public boolean isOverlayButtonHovered() {
//        return (this.doneButton.isHoveredOrFocused() || this.removeButton.isHoveredOrFocused() || this.editButton.isHoveredOrFocused() || this.moveUpButton.isHoveredOrFocused() || this.moveDownButton.isHoveredOrFocused());
//    }
//
//    public static class ActionScrollEntry extends ScrollAreaEntryBase {
//
//        public ButtonScriptEngine.ActionContainer action;
//
//        protected ManageActionsScreen parentScreen;
//        public Color backgroundColor;
//        protected boolean leftMouseDown = false;
//
//        public ActionScrollEntry(ScrollArea parentScrollArea, ManageActionsScreen parentScreen, ButtonScriptEngine.ActionContainer action, Color backgroundColor) {
//            super(parentScrollArea, (call) -> {});
//            this.action = action;
//            this.backgroundColor = backgroundColor;
//            this.parentScreen = parentScreen;
//            this.setHeight(30);
//        }
//
//        @Override
//        public void renderEntry(PoseStack matrix) {
//
//            super.renderEntry(matrix);
//
//            int footerHeight = 50;
//            if (!this.leftMouseDown && (MouseInput.getMouseY() < (this.parentScreen.height - footerHeight))) {
//                if (this.isHoveredOrFocused() && MouseInput.isLeftMouseDown() && !this.parentScreen.isOverlayButtonHovered() && !PopupHandler.isPopupActive()) {
//                    this.parentScreen.selected = this;
//                }
//            }
//            this.leftMouseDown = MouseInput.isLeftMouseDown();
//
//            if ((this.parentScreen.selected != null) && (this.parentScreen.selected == this)) {
//                fill(matrix, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), SELECTED_ENTRY_BACK_COLOR.getRGB());
//            } else {
//                fill(matrix, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), this.backgroundColor.getRGB());
//            }
//
//            int centerX = this.x + (this.getWidth() / 2);
//            int centerY = this.y + (this.getHeight() / 2);
//            Font font = Minecraft.getInstance().font;
//            Component actionComp = Component.literal(Locals.localize("fancymenu.customization.items.ticker.manage_actions.action", this.action.action));
//            String valueString = this.action.value;
//            if (valueString == null) {
//                valueString = "";
//            }
//            if (font.width(valueString) >= 200) {
//                valueString = font.plainSubstrByWidth(valueString, 200) + "...";
//            }
//            Component valueComp = Component.literal(Locals.localize("fancymenu.customization.items.ticker.manage_actions.value", valueString));
//            drawCenteredString(matrix, font, actionComp, centerX, centerY - font.lineHeight - 1, -1);
//            drawCenteredString(matrix, font, valueComp, centerX, centerY + 1, -1);
//
//        }
//
//    }
//
//}
