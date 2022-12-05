//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.item.items.ticker;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.buttonactions.ButtonActionScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.ScrollableScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMYesNoPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;

public class ManageTickerActionsScreen extends ScrollableScreen {

    protected static final Color ENTRY_BACK_1 = new Color(0, 0, 0, 50);
    protected static final Color ENTRY_BACK_2 = new Color(0, 0, 0, 90);
    protected static final Color SELECTED_ENTRY_BACK_COLOR = new Color(222, 174, 87); //237, 154, 59

    protected int entryBackTick = 0;
    protected AdvancedButton doneButton;
    protected AdvancedButton removeButton;
    protected AdvancedButton editButton;

    protected TickerActionScrollEntry selected;
    protected TickerLayoutEditorElement tickerElement;

    protected ManageTickerActionsScreen(Screen parent, TickerLayoutEditorElement tickerElement) {

        super(parent, Locals.localize("fancymenu.customization.items.ticker.manage_actions"));

        this.tickerElement = tickerElement;

        this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.guicomponents.done"), true, (press) -> {
            this.onClose();
        });
        this.doneButton.ignoreLeftMouseDownClickBlock = true;
        UIBase.colorizeButton(this.doneButton);

        this.removeButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.customization.items.ticker.manage_actions.remove"), true, (press) -> {
            FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0), 240, (call) -> {
                if (call) {
                    if (this.selected != null) {
                        ((TickerCustomizationItem)this.tickerElement.object).actions.remove(this.selected.action);
                        Minecraft.getInstance().setScreen(new ManageTickerActionsScreen(parent, tickerElement));
                    }
                }
            }, StringUtils.splitLines(Locals.localize("fancymenu.customization.items.ticker.manage_actions.remove.confirm"), "%n%"));
            PopupHandler.displayPopup(p);
        });
        this.removeButton.ignoreLeftMouseDownClickBlock = true;
        UIBase.colorizeButton(this.removeButton);

        this.editButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.customization.items.ticker.manage_actions.edit"), true, (press) -> {
            if (this.selected != null) {
                ButtonActionScreen s = new ButtonActionScreen(this, (call) -> {
                    if (call != null) {
                        this.selected.action.action = call.get(0);
                        this.selected.action.value = call.get(1);
                    }
                });
                s.setButtonAction(this.selected.action.action);
                if (this.selected.action.value != null) {
                    s.setValueString(this.selected.action.value);
                }
                Minecraft.getInstance().setScreen(s);
            }
        });
        this.editButton.ignoreLeftMouseDownClickBlock = true;
        UIBase.colorizeButton(this.editButton);

        //Add all actions to the list
        for (TickerCustomizationItem.ActionContainer c : ((TickerCustomizationItem)tickerElement.object).actions) {
            if (this.entryBackTick == 0) {
                this.scrollArea.addEntry(new TickerActionScrollEntry(this.scrollArea, this, c, ENTRY_BACK_1));
                this.entryBackTick = 1;
            } else {
                this.scrollArea.addEntry(new TickerActionScrollEntry(this.scrollArea, this, c, ENTRY_BACK_2));
                this.entryBackTick = 0;
            }
        }

    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {

        super.render(matrix, mouseX, mouseY, partialTicks);

        int xCenter = this.width / 2;

        this.removeButton.setX(xCenter - (this.removeButton.getWidth() / 2));
        this.removeButton.setY(this.height - 35);
        this.removeButton.render(matrix, mouseX, mouseY, partialTicks);
        if (this.selected != null) {
            this.removeButton.active = true;
        } else {
            this.removeButton.active = false;
        }

        this.editButton.setX(this.removeButton.x - this.editButton.getWidth() - 5);
        this.editButton.setY(this.height - 35);
        this.editButton.render(matrix, mouseX, mouseY, partialTicks);
        if (this.selected != null) {
            this.editButton.active = true;
        } else {
            this.editButton.active = false;
        }

        this.doneButton.setX(this.removeButton.x + this.removeButton.getWidth() + 5);
        this.doneButton.setY(this.height - 35);
        this.doneButton.render(matrix, mouseX, mouseY, partialTicks);

        if (PopupHandler.isPopupActive()) {
            this.doneButton.active = false;
            this.editButton.active = false;
            this.removeButton.active = false;
        } else {
            this.doneButton.active = true;
        }

    }

    @Override
    public boolean isOverlayButtonHovered() {
        return (this.doneButton.isHoveredOrFocused() || this.removeButton.isHoveredOrFocused() || this.editButton.isHoveredOrFocused());
    }

    public static class TickerActionScrollEntry extends ScrollAreaEntryBase {

        public TickerCustomizationItem.ActionContainer action;

        protected ManageTickerActionsScreen parentScreen;
        public Color backgroundColor;
        protected boolean leftMouseDown = false;

        public TickerActionScrollEntry(ScrollArea parentScrollArea, ManageTickerActionsScreen parentScreen, TickerCustomizationItem.ActionContainer action, Color backgroundColor) {
            super(parentScrollArea, (call) -> {});
            this.action = action;
            this.backgroundColor = backgroundColor;
            this.parentScreen = parentScreen;
            this.setHeight(30);
        }

        @Override
        public void renderEntry(PoseStack matrix) {

            super.renderEntry(matrix);

            int footerHeight = 50;
            if (!this.leftMouseDown && (MouseInput.getMouseY() < (this.parentScreen.height - footerHeight))) {
                if (this.isHoveredOrFocused() && MouseInput.isLeftMouseDown() && !this.parentScreen.isOverlayButtonHovered()) {
                    this.parentScreen.selected = this;
                }
            }
            this.leftMouseDown = MouseInput.isLeftMouseDown();

            if ((this.parentScreen.selected != null) && (this.parentScreen.selected == this)) {
                fill(matrix, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), SELECTED_ENTRY_BACK_COLOR.getRGB());
            } else {
                fill(matrix, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), this.backgroundColor.getRGB());
            }

            int centerX = this.x + (this.getWidth() / 2);
            int centerY = this.y + (this.getHeight() / 2);
            Font font = Minecraft.getInstance().font;
            Component actionComp = Component.literal(Locals.localize("fancymenu.customization.items.ticker.manage_actions.action", this.action.action));
            String valueString = this.action.value;
            if (valueString == null) {
                valueString = "";
            }
            if (font.width(valueString) >= 200) {
                valueString = font.plainSubstrByWidth(valueString, 200) + "...";
            }
            Component valueComp = Component.literal(Locals.localize("fancymenu.customization.items.ticker.manage_actions.value", valueString));
            drawCenteredString(matrix, font, actionComp, centerX, centerY - font.lineHeight - 1, -1);
            drawCenteredString(matrix, font, valueComp, centerX, centerY + 1, -1);

        }

    }

}
