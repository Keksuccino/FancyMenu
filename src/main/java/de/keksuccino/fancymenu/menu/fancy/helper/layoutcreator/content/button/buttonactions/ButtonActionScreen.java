package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.buttonactions;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionRegistry;
import de.keksuccino.fancymenu.menu.fancy.helper.PlaceholderEditBox;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutButton;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.ScrollableScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollAreaEntry;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ButtonActionScreen extends ScrollableScreen {

    protected static final Color ENTRY_BACK_1 = new Color(0, 0, 0, 50);
    protected static final Color ENTRY_BACK_2 = new Color(0, 0, 0, 90);

    public LayoutButton parentButton;

    protected List<ButtonAction> buttonActions = new ArrayList<>();
    protected int entryBackTick = 0;
    protected AdvancedButton doneButton;
    protected PlaceholderEditBox valueTextField;

    public ButtonActionScreen(Screen parent, LayoutButton parentButton) {

        super(parent, Locals.localize("fancymenu.helper.ui.button_action.set"));

        this.parentButton = parentButton;

        this.doneButton = new AdvancedButton(0, 0, 200, 20, Locals.localize("fancymenu.guicomponents.done"), true, (press) -> {
            this.onDone();
            this.onClose();
        });
        this.doneButton.ignoreLeftMouseDownClickBlock = true;
        UIBase.colorizeButton(this.doneButton);

        this.valueTextField = new PlaceholderEditBox(Minecraft.getInstance().font, 0, 0, 150, 20, true, null);
        this.valueTextField.setCanLoseFocus(true);
        this.valueTextField.setFocus(false);
        this.valueTextField.setMaxLength(1000);
        if (this.parentButton.actionContent != null) {
            this.valueTextField.setValue(this.parentButton.actionContent);
        }

        //LEGACY BUTTON ACTIONS
        LegacyButtonActions.getLegacyActions(this).forEach((action) -> {
            this.addButtonAction(action);
        });

        //API BUTTON ACTIONS
        for (ButtonActionContainer c : ButtonActionRegistry.getActions()) {
            this.addButtonAction(new ButtonAction(this, c.getAction(), c.getActionDescription(), c.hasValue(), c.getValueDescription(), c.getValueExample(), null));
        }

        ButtonAction selectedAction = null;
        if (this.parentButton.actionType != null) {
            selectedAction = this.getButtonActionByName(this.parentButton.actionType);
        }
        if (selectedAction == null) {
            selectedAction = this.buttonActions.get(0);
        }
        this.setButtonActionSelected(selectedAction, true);

    }

    @Override
    protected void init() {
        super.init();
        this.scrollArea.height = this.height - 165;
    }

    @Override
    public boolean isOverlayButtonHovered() {
        return this.doneButton.isHoveredOrFocused();
    }

    protected void addButtonAction(ButtonAction action) {
        this.buttonActions.add(action);
        if (this.entryBackTick == 0) {
            this.scrollArea.addEntry(new ButtonActionScrollEntry(this.scrollArea, action, ENTRY_BACK_1));
            this.entryBackTick = 1;
        } else {
            this.scrollArea.addEntry(new ButtonActionScrollEntry(this.scrollArea, action, ENTRY_BACK_2));
            this.entryBackTick = 0;
        }
        this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea, 1, new Color(255,255,255,100)));
    }

    protected void setButtonActionSelected(ButtonAction action, boolean selected) {
        action.selected = selected;
        this.buttonActions.forEach((ba) -> {
            if (ba != action) {
                ba.selected = false;
            }
        });
    }

    @Nullable
    protected ButtonAction getSelectedButtonAction() {
        for (ButtonAction a : this.buttonActions) {
            if (a.selected) {
                return a;
            }
        }
        return null;
    }

    @Nullable
    protected ButtonAction getButtonActionByName(String name) {
        for (ButtonAction a : this.buttonActions) {
            if (a.name.equals(name)) {
                return a;
            }
        }
        return null;
    }

    protected void onDone() {
        ButtonAction selected = this.getSelectedButtonAction();
        if (selected == null) {
            selected = this.buttonActions.get(0);
        }
        String value = null;
        if (selected.hasValue) {
            value = this.valueTextField.getValue();
        }
        if (!this.parentButton.actionType.equals(selected.name) || !this.parentButton.actionContent.equals(value)) {
            this.parentButton.handler.history.saveSnapshot(this.parentButton.handler.history.createSnapshot());
        }
        this.parentButton.actionType = selected.name;
        this.parentButton.actionContent = value;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {

        super.render(matrix, mouseX, mouseY, partialTicks);

        ButtonAction selected = getSelectedButtonAction();
        if (selected == null) {
            selected = buttonActions.get(0);
        }
        int xCenter = this.width / 2;

        //Draw taller footer
        fill(matrix, 0, this.height - 115, this.width, this.height, HEADER_FOOTER_COLOR.getRGB());

        this.doneButton.setX(xCenter - (this.doneButton.getWidth() / 2));
        this.doneButton.setY(this.height - 35);
        this.doneButton.render(matrix, mouseX, mouseY, partialTicks);

        if (selected.hasValue) {

            //Action Value
            drawCenteredString(matrix, font, Component.literal(Locals.localize("helper.creator.custombutton.config.actionvalue", selected.valueDesc)), xCenter, this.height - 100, -1);

            //Action Value Text Field
            this.valueTextField.setX(xCenter - (this.valueTextField.getWidth() / 2));
            this.valueTextField.setY(this.height - 85);
            this.valueTextField.setEditable(selected.hasValue);
            this.valueTextField.active = selected.hasValue;
            this.valueTextField.render(matrix, mouseX, mouseY, partialTicks);

            //Action Value Example
            drawCenteredString(matrix, font, Component.literal(Locals.localize("helper.creator.custombutton.config.actionvalue.example", selected.valueExample)), xCenter, this.height - 60, -1);

        }

        //Render desc of hovered entry
        int footerHeight = 115;
        if (mouseY < (this.height - footerHeight)) {
            for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
                if (e instanceof ButtonActionScrollEntry) {
                    if (e.isHoveredOrFocused()) {
                        String desc = ((ButtonActionScrollEntry)e).action.desc;
                        if (desc != null) {
                            renderDescription(matrix, Arrays.asList(StringUtils.splitLines(desc, "%n%")), mouseX, mouseY);
                        }
                    }
                }
            }
        }

    }

    public static class ButtonActionScrollEntry extends ScrollAreaEntryBase {

        public ButtonAction action;
        public Color backgroundColor;

        protected boolean leftMouseDown = false;

        public ButtonActionScrollEntry(ScrollArea parent, ButtonAction action, Color backgroundColor) {
            super(parent, (call) -> {});
            this.action = action;
            this.backgroundColor = backgroundColor;
            this.setHeight(30);
        }

        @Override
        public void renderEntry(PoseStack matrix) {

            super.renderEntry(matrix);

            int footerHeight = 115;
            if (!this.leftMouseDown && (MouseInput.getMouseY() < (this.action.parent.height - footerHeight))) {
                if (this.isHoveredOrFocused() && MouseInput.isLeftMouseDown()) {
                    action.setSelected(true);
                }
            }
            this.leftMouseDown = MouseInput.isLeftMouseDown();

            fill(matrix, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), this.backgroundColor.getRGB());

            this.action.render(matrix, MouseInput.getMouseX(), MouseInput.getMouseY(), this);

        }

    }

    public static class ButtonAction extends GuiComponent {

        protected ButtonActionScreen parent;
        protected String name;
        protected String desc;
        protected String valueDesc;
        protected String valueExample;
        protected CharacterFilter valueFilter = null;
        protected boolean hasValue;

        protected boolean selected = false;

        public ButtonAction(ButtonActionScreen parent, String name, String desc, boolean hasValue, @Nullable String valueDesc, @Nullable String valueExample, @Nullable CharacterFilter valueFilter) {
            this.parent = parent;
            this.name = name;
            this.desc = desc;
            this.valueDesc = valueDesc;
            this.valueFilter = valueFilter;
            this.hasValue = hasValue;
            this.valueExample = valueExample;
        }

        public void render(PoseStack matrix, int mouseX, int mouseY, ButtonActionScrollEntry entry) {

            int centerX = entry.x + (entry.getWidth() / 2);
            int centerY = entry.y + (entry.getHeight() / 2);
            Font font = Minecraft.getInstance().font;

            Color nameBackColor = new Color(255,255,255,30);
            String renderName = "§l" + this.name;
            if (this.selected) {
                renderName = "§a§l" + this.name;
            }
            int nameWidth = font.width(renderName);
            int nameX = centerX - (nameWidth / 2);
            int nameY = centerY - 5;
            fill(matrix, nameX - 5, nameY - 5, nameX + nameWidth + 5, nameY + font.lineHeight + 5, nameBackColor.getRGB());
            drawCenteredString(matrix, font, renderName, centerX, nameY, -1);

        }

        protected void setSelected(boolean selected) {
            this.parent.setButtonActionSelected(this, selected);
        }

    }

}
