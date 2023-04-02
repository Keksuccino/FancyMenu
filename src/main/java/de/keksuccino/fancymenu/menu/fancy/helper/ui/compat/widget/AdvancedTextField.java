package de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.widget;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.InputConstants;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.Screen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.SharedConstants;
import de.keksuccino.fancymenu.mixin.client.IMixinGuiTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

public class AdvancedTextField extends de.keksuccino.konkrete.gui.content.AdvancedTextField {

    public boolean shiftPressed = false;
    protected final FontRenderer font;
    protected final CharacterFilter filter;

    public AdvancedTextField(FontRenderer fontRenderer, int x, int y, int width, int height, boolean handleSelf, @Nullable CharacterFilter filter) {
        super(fontRenderer, x, y, width, height, handleSelf, filter);
        this.font = fontRenderer;
        this.filter = filter;
    }

    public String getValue() {
        return super.getText();
    }

    /** Use getValue() instead **/
    @Deprecated
    @Override
    public final String getText() {
        return this.getValue();
    }

    public void setValue(String value) {
        super.setText(value);
    }

    /** Use setValue() instead **/
    @Deprecated
    @Override
    public final void setText(String textIn) {
        this.setValue(textIn);
    }

    public void render(int mouseX, int mouseY, float partial) {
        this.renderButton(mouseX, mouseY, partial);
    }

    public void renderButton(int mouseX, int mouseY, float partial) {
        super.drawTextBox();
    }

    /** Use renderButton() instead **/
    @Deprecated
    @Override
    public final void drawTextBox() {
        this.render(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());
    }

    /** You need to manually call this every screen tick in {@link Screen#tick()}. **/
    public void tick() {
        super.updateCursorCounter();
    }

    /** Use tick() instead **/
    @Deprecated
    @Override
    public final void updateCursorCounter() {
        this.tick();
    }

    public void setTextColorUneditable(int color) {
        super.setDisabledTextColour(color);
    }

    /** Use setTextColorUneditable() instead **/
    @Deprecated
    @Override
    public final void setDisabledTextColour(int color) {
        this.setTextColorUneditable(color);
    }

    public boolean isVisible() {
        return super.getVisible();
    }

    /** Use isVisible() instead **/
    @Deprecated
    @Override
    public final boolean getVisible() {
        return this.isVisible();
    }

    public void setMaxLength(int i) {
        super.setMaxStringLength(i);
    }

    /** Use setMaxLength() instead **/
    @Deprecated
    @Override
    public final void setMaxStringLength(int length) {
        this.setMaxLength(length);
    }

    public int getMaxLength() {
        return super.getMaxStringLength();
    }

    /** Use getMaxLength() instead **/
    @Deprecated
    @Override
    public final int getMaxStringLength() {
        return this.getMaxLength();
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getX() {
        return this.x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getY() {
        return this.y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return this.width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return this.height;
    }

    public int getInnerWidth() {
        return this.isBordered() ? this.width - 8 : this.width;
    }

    public boolean isBordered() {
        return super.getEnableBackgroundDrawing();
    }

    /** Use isBordered() instead **/
    @Deprecated
    @Override
    public final boolean getEnableBackgroundDrawing() {
        return this.isBordered();
    }

    public void setBordered(boolean bordered) {
        super.setEnableBackgroundDrawing(bordered);
    }

    public boolean canConsumeInput() {
        return this.isVisible() && this.isFocused() && this.isEditable();
    }

    /** Use setBordered() instead **/
    @Deprecated
    @Override
    public final void setEnableBackgroundDrawing(boolean enableBackgroundDrawingIn) {
        this.setBordered(enableBackgroundDrawingIn);
    }

    public String getHighlighted() {
        return super.getSelectedText();
    }

    /** Use getHighlighted() instead **/
    @Deprecated
    @Override
    public final String getSelectedText() {
        return this.getHighlighted();
    }

    public void insertText(String text) {

        if (this.filter != null) {
            text = this.filter.filterForAllowedChars(text);
        }

        int i = (this.getCursorPosition() < this.getAccessor().getHighlightPosFancyMenu()) ? this.getCursorPosition() : this.getAccessor().getHighlightPosFancyMenu();
        int j = (this.getCursorPosition() < this.getAccessor().getHighlightPosFancyMenu()) ? this.getAccessor().getHighlightPosFancyMenu() : this.getCursorPosition();
        int k = this.getMaxLength() - this.getValue().length() - (i - j);
        String s = SharedConstants.filterText(text);
        int l = s.length();
        if (k < l) {
            s = s.substring(0, k);
            l = k;
        }

        String s1 = (new StringBuilder(this.getValue())).replace(i, j, s).toString();
        if (this.getAccessor().getValidatorFancyMenu().apply(s1)) {
            this.getAccessor().setValueFieldFancyMenu(s1);
            this.setCursorPosition(i + l);
            this.setHighlightPos(this.getCursorPosition());
        }

    }

    /** Use insertText() instead **/
    @Deprecated
    @Override
    public final void writeText(String textToWrite) {
        this.insertText(textToWrite);
    }

    public void moveCursorToEnd() {
        super.setCursorPositionEnd();
    }

    /** Use moveCursorToEnd() instead **/
    @Deprecated
    @Override
    public final void setCursorPositionEnd() {
        this.moveCursorToEnd();
    }

    public void moveCursorToStart() {
        super.setCursorPositionZero();
    }

    /** Use moveCursorToStart() instead **/
    @Deprecated
    @Override
    public final void setCursorPositionZero() {
        this.moveCursorToStart();
    }

    public void setHighlightPos(int pos) {
        int i = this.getValue().length();
        this.getAccessor().setHighlightPosFancyMenu(MathHelper.clamp(pos, 0, i));
        if (this.font != null) {
            if (this.getAccessor().getDisplayPosFancyMenu() > i) {
                this.getAccessor().setDisplayPosFancyMenu(i);
            }

            int j = this.getInnerWidth();
            String s = this.font.trimStringToWidth(this.getValue().substring(this.getAccessor().getDisplayPosFancyMenu()), j);
            int k = s.length() + this.getAccessor().getDisplayPosFancyMenu();
            if (this.getAccessor().getHighlightPosFancyMenu() == this.getAccessor().getDisplayPosFancyMenu()) {
                this.getAccessor().setDisplayPosFancyMenu(this.getAccessor().getDisplayPosFancyMenu() - this.font.trimStringToWidth(this.getValue(), j, true).length());
            }

            if (this.getAccessor().getHighlightPosFancyMenu() > k) {
                this.getAccessor().setDisplayPosFancyMenu(this.getAccessor().getDisplayPosFancyMenu() + this.getAccessor().getHighlightPosFancyMenu() - k);
            } else if (this.getAccessor().getHighlightPosFancyMenu() <= this.getAccessor().getDisplayPosFancyMenu()) {
                this.getAccessor().setDisplayPosFancyMenu(this.getAccessor().getDisplayPosFancyMenu() - (this.getAccessor().getDisplayPosFancyMenu() - this.getAccessor().getHighlightPosFancyMenu()));
            }

            this.getAccessor().setDisplayPosFancyMenu(MathHelper.clamp(this.getAccessor().getDisplayPosFancyMenu(), 0, i));
        }
    }

    /** Use setHighlightPos() instead **/
    @Deprecated
    @Override
    public final void setSelectionPos(int position) {
        this.setHighlightPos(position);
    }

    public int getWordPosition(int i) {
        return super.getNthWordFromCursor(i);
    }

    /** Use getWordPosition() instead **/
    @Deprecated
    @Override
    public final int getNthWordFromCursor(int numWords) {
        return this.getWordPosition(numWords);
    }

    public void deleteChars(int i) {
        super.deleteFromCursor(i);
    }

    /** Use deleteChars() instead **/
    @Deprecated
    @Override
    public final void deleteFromCursor(int num) {
        this.deleteChars(num);
    }

    public void deleteText(int i) {
        if (Screen.hasControlDown()) {
            this.deleteWords(i);
        } else {
            this.deleteChars(i);
        }
    }

    public void moveCursorTo(int i) {
        this.setCursorPosition(i);
        if (!this.shiftPressed) {
            this.setHighlightPos(this.getCursorPosition());
        }
    }

    @Override
    public void setCursorPosition(int pos) {
        this.getAccessor().setCursorPositionFieldFancyMenu(MathHelper.clamp(pos, 0, this.getValue().length()));
    }

    public void moveCursor(int by) {
        super.moveCursorBy(by);
    }

    /** Use moveCursor() instead **/
    @Deprecated
    @Override
    public final void moveCursorBy(int num) {
        this.moveCursor(num);
    }

    public boolean keyPressed(int keycode, int i1, int i2) {
        if (!this.canConsumeInput()) {
            return false;
        } else {
            this.shiftPressed = Screen.hasShiftDown();
            if (Screen.isSelectAll(keycode)) {
                this.moveCursorToEnd();
                this.setHighlightPos(0);
                return true;
            } else if (Screen.isCopy(keycode)) {
                Screen.setClipboardString(this.getHighlighted());
                return true;
            } else if (Screen.isPaste(keycode)) {
                if (this.isEditable()) {
                    this.insertText(Screen.getClipboardString());
                }
                return true;
            } else if (Screen.isCut(keycode)) {
                Screen.setClipboardString(this.getHighlighted());
                if (this.isEditable()) {
                    this.insertText("");
                }
                return true;
            } else {
                switch(keycode) {
                    case InputConstants.BACKSPACE:
                        if (this.isEditable()) {
                            this.shiftPressed = false;
                            this.deleteText(-1);
                            this.shiftPressed = Screen.hasShiftDown();
                        }
                        return true;
                    case InputConstants.INSERT:
                    case InputConstants.ARROW_DOWN:
                    case InputConstants.ARROW_UP:
                    case InputConstants.PAGE_UP:
                    case InputConstants.PAGE_DOWN:
                    default:
                        return false;
                    case InputConstants.DELETE:
                        if (this.isEditable()) {
                            this.shiftPressed = false;
                            this.deleteText(1);
                            this.shiftPressed = Screen.hasShiftDown();
                        }
                        return true;
                    case InputConstants.ARROW_RIGHT:
                        if (Screen.hasControlDown()) {
                            this.moveCursorTo(this.getWordPosition(1));
                        } else {
                            this.moveCursor(1);
                        }
                        return true;
                    case InputConstants.ARROW_LEFT:
                        if (Screen.hasControlDown()) {
                            this.moveCursorTo(this.getWordPosition(-1));
                        } else {
                            this.moveCursor(-1);
                        }
                        return true;
                    case InputConstants.HOME:
                        this.moveCursorToStart();
                        return true;
                    case InputConstants.END:
                        this.moveCursorToEnd();
                        return true;
                }
            }
        }
    }

    /**
     * <b>IMPORTANT:</b> This is not called by vanilla MC!<br>
     * You need to manually call it in {@link Screen#keyReleased(int, int, int)}.
     */
    public boolean keyReleased(int button, int i2, int i3) {
        return false;
    }

    public boolean charTyped(char character, int i1) {
        if (!this.canConsumeInput()) {
            return false;
        } else if (SharedConstants.isAllowedChatCharacter(character)) {
            if (this.isEditable()) {
                this.insertText(Character.toString(character));
            }
            return true;
        } else {
            return false;
        }
    }

    /** Use keyPressed(), keyReleased() and charTyped() instead **/
    @Deprecated
    @Override
    public final boolean textboxKeyTyped(char typedChar, int keyCode) {
        this.keyPressed(keyCode, 0, 0);
        this.charTyped(typedChar, keyCode);
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return super.mouseClicked((int)mouseX, (int)mouseY, mouseButton);
    }

    /** Use mouseClicked(double,double,int) instead **/
    @Deprecated
    @Override
    public final boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return this.mouseClicked((double)mouseX, (double)mouseY, mouseButton);
    }

    public IMixinGuiTextField getAccessor() {
        return (IMixinGuiTextField) this;
    }

    public static void blit(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void fill(int left, int top, int right, int bottom, int color) {
        drawRect(left, top, right, bottom, color);
    }

}
