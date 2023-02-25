//TODO remove debug
package de.keksuccino.fancymenu.menu.fancy.helper.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.mixin.client.IMixinEditBox;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TextEditorScreenOld extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();

    protected Screen parentScreen;
    protected CharacterFilter characterFilter;
    protected List<TextEditorInputBox> textFieldLines = new ArrayList<>();
    protected ScrollBar verticalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, 10, 40, 0, 0, 0, 0, UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor());
    protected ScrollBar horizontalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, 40, 10, 0, 0, 0, 0, UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor());
    protected int lastCursorPosSetByUser = 0;
    protected boolean justSwitchedLineByWordDeletion = false;
    protected int offsetX = 0;
    protected int offsetY = 0;
    public int headerHeight = 50;
    public int footerHeight = 50;
    public int lineHeight = 20;

    public TextEditorScreenOld(Component name, @Nullable Screen parent, @Nullable CharacterFilter characterFilter) {
        super(name);
        this.parentScreen = parent;
        this.characterFilter = characterFilter;
        this.addLine();
        this.getLine(0).setFocus(true);
        KeyboardHandler.addKeyPressedListener(this::onKeyPress);
    }

    @Override
    protected void init() {
        super.init();
        this.verticalScrollBar.scrollAreaEndX = this.width;
        this.verticalScrollBar.scrollAreaEndY = this.height-10;
        this.horizontalScrollBar.scrollAreaEndX = this.width-10;
        this.horizontalScrollBar.scrollAreaEndY = this.height;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {

        this.justSwitchedLineByWordDeletion = false;

        this.renderBackground(matrix);

        //Update positions and size of lines and render them
        int longestTextWidth = 0;
        int index = 0;
        for (TextEditorInputBox f : this.textFieldLines) {
            int textWidth = Minecraft.getInstance().font.width(f.getValue());
            if (textWidth > longestTextWidth) {
                longestTextWidth = textWidth;
            }
            f.y = this.headerHeight + (20 * index) + this.offsetY;
            f.x = 20 + this.offsetX;
            f.setHeight(this.lineHeight);
            ((IMixinEditBox)f).setDisplayPosFancyMenu(0);
            f.render(matrix, mouseX, mouseY, partial);
            index++;
        }
        //Make all lines have the same length (length of the longest line)
        for (TextEditorInputBox f : this.textFieldLines) {
            f.setWidth(longestTextWidth + 30);
        }

        this.verticalScrollBar.render(matrix);
        this.horizontalScrollBar.render(matrix);

        super.render(matrix, mouseX, mouseY, partial);

    }

    protected TextEditorInputBox addLineAtIndex(int index) {
        TextEditorInputBox f = new TextEditorInputBox(Minecraft.getInstance().font, 0, 0, 50, this.lineHeight, true, this.characterFilter, this);
        f.setMaxLength(Integer.MAX_VALUE);
        this.textFieldLines.add(index, f);
        return f;
    }

    protected TextEditorInputBox addLine() {
        return this.addLineAtIndex(this.getLineCount());
    }

    protected void removeLineAtIndex(int index) {
        if (index < 1) {
            return;
        }
        if (index <= this.getLineCount()-1) {
            this.textFieldLines.remove(index);
        }
    }

    protected void removeLastLine() {
        this.removeLineAtIndex(this.getLineCount()-1);
    }

    public int getLineCount() {
        return this.textFieldLines.size();
    }

    @Nullable
    public TextEditorInputBox getLine(int index) {
        return this.textFieldLines.get(index);
    }

    protected void goUpLine() {
        if (this.isLineFocused()) {
            int current = Math.max(0, this.getFocusedLineIndex());
            if (current > 0) {
                TextEditorInputBox currentLine = this.getLine(current);
                this.setFocusedLine(current - 1);
                if (currentLine != null) {
                    this.getFocusedLine().moveCursorTo(this.lastCursorPosSetByUser);
                }
            }
        }
    }

    protected void goDownLine(boolean isNewLine) {
        if (this.isLineFocused()) {
            int current = Math.max(0, this.getFocusedLineIndex());
            if (isNewLine) {
                this.addLineAtIndex(current+1);
            }
            TextEditorInputBox currentLine = this.getLine(current);
            this.setFocusedLine(current+1);
            if (currentLine != null) {
                TextEditorInputBox nextLine = this.getFocusedLine();
                if (isNewLine) {
                    //Split content of currentLine at cursor pos and move text after cursor to next line if ENTER was pressed
                    String textBeforeCursor = currentLine.getValue().substring(0, currentLine.getCursorPosition());
                    String textAfterCursor = currentLine.getValue().substring(currentLine.getCursorPosition());
                    currentLine.setValue(textBeforeCursor);
                    nextLine.setValue(textAfterCursor);
                    nextLine.moveCursorTo(0);
                } else {
                    nextLine.moveCursorTo(this.lastCursorPosSetByUser);
                }
            }
        }
    }

    protected void setFocusedLine(int index) {
        if (index <= this.getLineCount()-1) {
            for (TextEditorInputBox f : this.textFieldLines) {
                f.setFocus(false);
            }
            this.getLine(index).setFocus(true);
        }
    }

    /**
     * Returns the index of the focused line or -1 if no line is focused.
     **/
    protected int getFocusedLineIndex() {
        int index = 0;
        for (TextEditorInputBox f : this.textFieldLines) {
            if (f.isFocused()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    @Nullable
    protected TextEditorInputBox getFocusedLine() {
        if (this.isLineFocused()) {
            return this.getLine(this.getFocusedLineIndex());
        }
        return null;
    }

    protected boolean isLineFocused() {
        return (this.getFocusedLineIndex() > -1);
    }

    protected void onKeyPress(KeyboardData d) {

        if ((Minecraft.getInstance().screen != null) && (Minecraft.getInstance().screen == this)) {

            //ENTER
            if (d.keycode == 257) {
                if (this.isLineFocused()) {
                    this.goDownLine(true);
                }
            }
            //ARROW UP
            if (d.keycode == InputConstants.KEY_UP) {
                this.goUpLine();
            }
            //ARROW DOWN
            if (d.keycode == InputConstants.KEY_DOWN) {
                this.goDownLine(false);
            }

        }

    }

    @Override
    public void onClose() {
        if (this.parentScreen != null) {
            Minecraft.getInstance().setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }

    protected int getEditBoxCursorX(EditBox editBox) {
        try {
            IMixinEditBox b = (IMixinEditBox) editBox;
            String s = this.font.plainSubstrByWidth(editBox.getValue().substring(b.getDisplayPosFancyMenu()), editBox.getInnerWidth());
            int j = editBox.getCursorPosition() - b.getDisplayPosFancyMenu();
            boolean flag = j >= 0 && j <= s.length();
            boolean flag2 = editBox.getCursorPosition() < editBox.getValue().length() || editBox.getValue().length() >= b.getMaxLengthFancyMenu();
            int l = b.getBorderedFancyMenu() ? editBox.getX() + 4 : editBox.getX();
            int j1 = l;
            if (!s.isEmpty()) {
                String s1 = flag ? s.substring(0, j) : s;
                j1 += this.font.width(b.getFormatterFancyMenu().apply(s1, b.getDisplayPosFancyMenu()));
            }
            int k1 = j1;
            if (!flag) {
                k1 = j > 0 ? l + editBox.getWidth() : l;
            } else if (flag2) {
                k1 = j1 - 1;
                --j1;
            }
            return k1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static class TextEditorInputBox extends AdvancedTextField {

        public TextEditorScreenOld parent;

        public TextEditorInputBox(Font font, int x, int y, int width, int height, boolean handleSelf, @Nullable CharacterFilter characterFilter, TextEditorScreenOld parent) {
            super(font, x, y, width, height, handleSelf, characterFilter);
            this.parent = parent;
        }

        @Override
        public void setCursorPosition(int newPos) {

            super.setCursorPosition(newPos);

            if ((newPos != this.parent.lastCursorPosSetByUser) && this.isFocused()) {
                this.parent.lastCursorPosSetByUser = this.getCursorPosition();
            }

            if (this.parent.isLineFocused() && (this.parent.getFocusedLine() == this)) {

                //Make the lines scroll horizontally with the cursor position if the cursor is too far to the left or right
                int cursorX = this.parent.getEditBoxCursorX(this);
                int maxToRight = this.parent.width - 20;
                int maxToLeft = 20;
                if (cursorX > maxToRight) {
                    this.parent.offsetX -= cursorX - maxToRight;
                } else if (cursorX < maxToLeft) {
                    this.parent.offsetX += maxToLeft - (cursorX);
                    if (this.parent.offsetX > 0) {
                        this.parent.offsetX = 0;
                    }
                }
                if (this.getCursorPosition() == 0) {
                    this.parent.offsetX = 0;
                }

                //Make the lines scroll vertically with the cursor position if the cursor is too far up or down
                int lineY = this.y;
                if (lineY > (this.parent.height - this.parent.footerHeight - this.parent.lineHeight)) {
                    this.parent.offsetY -= this.parent.lineHeight;
                } else if (lineY < this.parent.headerHeight) {
                    this.parent.offsetY += this.parent.lineHeight;
                    if (this.parent.offsetY > 0) {
                        this.parent.offsetY = 0;
                    }
                }
                if (this.parent.getFocusedLineIndex() == 0) {
                    this.parent.offsetY = 0;
                }

            }

        }

        @Override
        public boolean keyPressed(int p_94132_, int p_94133_, int p_94134_) {
            return super.keyPressed(p_94132_, p_94133_, p_94134_);
        }

        @Override
        public void deleteChars(int i) {
            //If the user presses BACKSPACE and the cursor pos is at 0, it will jump one line up,
            //adds the text behind the cursor at the end of the new line and deletes the old line
            if (!this.parent.justSwitchedLineByWordDeletion) {
                if (this.getCursorPosition() == 0) {
                    int lastLineIndex = this.parent.getFocusedLineIndex();
                    this.parent.justSwitchedLineByWordDeletion = true;
                    this.parent.goUpLine();
                    this.parent.getFocusedLine().moveCursorToEnd();
                    this.parent.getFocusedLine().insertText(this.getValue());
                    this.parent.getFocusedLine().setCursorPosition(this.parent.getFocusedLine().getCursorPosition()-this.getValue().length());
                    this.parent.getFocusedLine().setHighlightPos(this.parent.getFocusedLine().getCursorPosition());
                    if (lastLineIndex > 0) {
                        this.parent.removeLineAtIndex(this.parent.getFocusedLineIndex()+1);
                    }
                } else {
                    super.deleteChars(i);
                }
            }
        }

    }

}
