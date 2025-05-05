package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinEditBox;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TextEditorLine extends ExtendedEditBox {

    private static final Logger LOGGER = LogManager.getLogger();

    public TextEditorScreen parent;
    protected String lastTickValue = "";
    public boolean isInMouseHighlightingMode = false;
    protected final Font font2;
    public int textWidth = 0;
    public int lineIndex = 0;
    protected int currentHighlightPosXStart = 0;
    protected int currentHighlightPosXEnd = 0;
    protected int currentCharacterRenderIndex = 0;

    protected static boolean leftRightArrowWasDown = false;

    public TextEditorLine(Font font, int x, int y, int width, int height, @Nullable CharacterFilter characterFilter, TextEditorScreen parent) {
        super(font, x, y, width, height, Component.empty());
        this.setCharacterFilter(characterFilter);
        this.parent = parent;
        this.font2 = font;
        this.setBordered(false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        //Only render line if inside the editor area (for performance reasons)
        if (this.isInEditorArea()) {
            super.render(graphics, mouseX, mouseY, partial);
        }

        this.lastTickValue = this.getValue();

    }

    protected MutableComponent getFormattedText(String text) {
        List<Component> chars = new ArrayList<>();
        for (char c : text.toCharArray()) {
            Style style = Style.EMPTY;
            for (TextEditorFormattingRule r : this.parent.formattingRules) {
                Style rs = r.getStyle(c, this.currentCharacterRenderIndex, this.getCursorPosition(), this, this.parent.currentRenderCharacterIndexTotal, this.parent);
                if ((rs != null) && (rs != Style.EMPTY)) {
                    style = rs.applyTo(style);
                }
            }
            chars.add(Component.literal(String.valueOf(c)).withStyle(style));
            this.currentCharacterRenderIndex++;
            this.parent.currentRenderCharacterIndexTotal++;
        }
        MutableComponent comp = Component.literal("");
        for (Component c : chars) {
            comp.append(c);
        }
        return comp;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.currentCharacterRenderIndex = 0;

        this.setTextColor(this.parent.textColor.getRGB());
        this.setTextColorUneditable(this.parent.textColor.getRGB());

        if (this.isVisible()) {

            if (this.isFocused()) {
                //Render focused background
                graphics.fill(RenderType.gui(), 0, this.getY(), this.parent.width, this.getY() + this.height, this.parent.focusedLineColor.getRGB());
            }

            int textColorInt = this.isEditable() ? this.getAsAccessor().getTextColorFancyMenu() : this.getAsAccessor().getTextColorUneditableFancyMenu();
            int cursorPos = this.getCursorPosition() - this.getAsAccessor().getDisplayPosFancyMenu();
            int highlightPos = this.getAsAccessor().getHighlightPosFancyMenu() - this.getAsAccessor().getDisplayPosFancyMenu();
            String text = this.getValue();
            boolean isCursorNotAtStartOrEnd = cursorPos >= 0 && cursorPos <= text.length();
            boolean renderCursor = this.isFocused() && (Util.getMillis() - this.getAsAccessor().getFocusedTimeFancyMenu()) / 300L % 2L == 0L && isCursorNotAtStartOrEnd;
            int textX = this.getAsAccessor().getBorderedFancyMenu() ? this.getX() + 4 : this.getX() + 1;
            int textY = this.getAsAccessor().getBorderedFancyMenu() ? this.getY() + (this.height - 8) / 2 : (this.getY() + Math.max(0, (this.getHeight() / 2)) - (this.font2.lineHeight / 2));
            int textXRender = textX;
            if (highlightPos > text.length()) {
                highlightPos = text.length();
            }

            if (!text.isEmpty()) {
                String textBeforeCursor = isCursorNotAtStartOrEnd ? text.substring(0, cursorPos) : text;
                //Render text before cursor
                textXRender = graphics.drawString(this.font2, this.getFormattedText(textBeforeCursor), textX, textY, textColorInt, false);
            }

            boolean isCursorNotAtEndOfLine = this.getCursorPosition() < this.getValue().length() || this.getValue().length() >= this.getAsAccessor().getMaxLengthFancyMenu();
            int cursorPosRender = textXRender;
            if (!isCursorNotAtStartOrEnd) {
                cursorPosRender = cursorPos > 0 ? textX + this.width : textX;
            } else if (isCursorNotAtEndOfLine) {
                cursorPosRender = textXRender - 1;
            }

            if (!text.isEmpty() && isCursorNotAtStartOrEnd && cursorPos < text.length()) {
                //Render text after cursor
                graphics.drawString(this.font2, this.getFormattedText(text.substring(cursorPos)), textXRender, textY, textColorInt, false);
            }

            if (this.getAsAccessor().getHintFancyMenu() != null && text.isEmpty() && !this.isFocused()) {
                graphics.drawString(this.font2, this.getAsAccessor().getHintFancyMenu(), textXRender, textY, textColorInt, false);
            }

            if (!isCursorNotAtEndOfLine && this.getAsAccessor().getSuggestionFancyMenu() != null) {
                graphics.drawString(this.font2, this.getAsAccessor().getSuggestionFancyMenu(), (cursorPosRender - 1), textY, -8355712, false);
            }

            if (renderCursor) {
                if (isCursorNotAtEndOfLine) {
                    graphics.fill(RenderType.guiOverlay(), cursorPosRender, textY - 1, cursorPosRender + 1, textY + 1 + 9, textColorInt);
                } else {
                    graphics.drawString(this.font2, "_", cursorPosRender, textY, textColorInt, false);
                }
            }

            if (highlightPos != cursorPos) {
                this.currentHighlightPosXStart = cursorPosRender;
                this.currentHighlightPosXEnd = textX + this.font2.width(text.substring(0, highlightPos)) - 1;
                this.getAsAccessor().invokeRenderHighlightFancyMenu(graphics, this.currentHighlightPosXStart, textY - 1, this.currentHighlightPosXEnd, textY + 1 + 9);
            } else {
                this.currentHighlightPosXStart = 0;
                this.currentHighlightPosXEnd = 0;
            }

        }

    }

    public boolean isInEditorArea() {
        return ((this.getY() + this.getHeight() >= this.parent.getEditorAreaY()) && (this.getY() <= this.parent.getEditorAreaY() + this.parent.getEditorAreaHeight()));
    }

    public boolean isHighlightedHovered() {
        if (this.isInEditorArea() && (this.currentHighlightPosXStart != this.currentHighlightPosXEnd) && this.isHovered()) {
            int mouseX = MouseInput.getMouseX();
            return ((mouseX >= Math.min(this.currentHighlightPosXStart, this.currentHighlightPosXEnd)) && (mouseX <= Math.max(this.currentHighlightPosXStart, this.currentHighlightPosXEnd)));
        }
        return false;
    }

    public int getActualHeight() {
        int h = this.height;
        if (this.getAsAccessor().getBorderedFancyMenu()) {
            h += 2;
        }
        return h;
    }

    public IMixinEditBox getAsAccessor() {
        return (IMixinEditBox) this;
    }

    @Override
    public void setCursorPosition(int newPos) {

        this.textWidth = this.font2.width(this.getValue());

        super.setCursorPosition(newPos);

        //Caching the last cursor position set by the user, to set it to the new line when changing the line
        if ((newPos != this.parent.lastCursorPosSetByUser) && this.isFocused()) {
            this.parent.lastCursorPosSetByUser = this.getCursorPosition();
        }

        this.parent.correctXScroll(this);

    }

    public void tick() {

        if (!MouseInput.isLeftMouseDown() && this.isInMouseHighlightingMode) {
            this.isInMouseHighlightingMode = false;
        }

        leftRightArrowWasDown = false;

    }

    @Override
    public boolean keyPressed(int keycode, int i1, int i2) {
        //Handled by the editor
        if (Screen.isCopy(keycode) || Screen.isPaste(keycode) || Screen.isSelectAll(keycode) || Screen.isCut(keycode)) {
            return false;
        }
        //Text deletion is handled by the editor
        if (keycode == InputConstants.KEY_BACKSPACE) {
            return false;
        }
        //Don't move cursor when in mouse-highlighting mode
        if (((keycode == InputConstants.KEY_RIGHT) || (keycode == InputConstants.KEY_LEFT)) && this.parent.isInMouseHighlightingMode()) {
            return false;
        }
        //Jump to line above when pressing ARROW LEFT while at start of line
        if (keycode == InputConstants.KEY_LEFT) {
            if (!leftRightArrowWasDown) {
                if (this.parent.isLineFocused() && (this.parent.getFocusedLine() == this) && (this.getCursorPosition() <= 0) && (this.parent.getLineIndex(this) > 0)) {
                    leftRightArrowWasDown = true;
                    this.parent.goUpLine();
                    this.parent.getFocusedLine().moveCursorTo(this.parent.getFocusedLine().getValue().length(), false);
                    this.parent.correctYScroll(0);
                    return true;
                }
            } else {
                return true;
            }
        }
        //Jump to line below when pressing ARROW RIGHT while at end of line
        if (keycode == InputConstants.KEY_RIGHT) {
            if (!leftRightArrowWasDown) {
                if (this.parent.isLineFocused() && (this.parent.getFocusedLine() == this) && (this.getCursorPosition() >= this.getValue().length()) && (this.parent.getLineIndex(this) < this.parent.getLineCount() - 1)) {
                    leftRightArrowWasDown = true;
                    this.parent.goDownLine(false);
                    this.parent.getFocusedLine().moveCursorTo(0, false);
                    this.parent.correctYScroll(0);
                    return true;
                }
            } else {
                return true;
            }
        }
        return super.keyPressed(keycode, i1, i2);
    }

    @Override
    public void deleteChars(int i) {
        //If the user presses BACKSPACE and the cursor pos is at 0, it will jump one line up, adds
        //the text behind the cursor at the end of the new line and deletes the old line
        if (!this.parent.justSwitchedLineByWordDeletion) {
            if ((this.getCursorPosition() == 0) && (this.parent.getFocusedLineIndex() > 0)) {
                int lastLineIndex = this.parent.getFocusedLineIndex();
                this.parent.justSwitchedLineByWordDeletion = true;
                this.parent.goUpLine();
                this.parent.getFocusedLine().moveCursorToEnd(false);
                this.parent.getFocusedLine().insertText(this.getValue());
                this.parent.getFocusedLine().setCursorPosition(this.parent.getFocusedLine().getCursorPosition()-this.getValue().length());
                this.parent.getFocusedLine().setHighlightPos(this.parent.getFocusedLine().getCursorPosition());
                if (lastLineIndex > 0) {
                    this.parent.removeLineAtIndex(this.parent.getFocusedLineIndex()+1);
                    this.parent.correctYScroll(-1);
                }
            } else {
                super.deleteChars(i);
            }
        }
        this.textWidth = this.font2.width(this.getValue());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

        if (!this.parent.isMouseInsideEditorArea() || this.parent.rightClickContextMenu.isOpen()) {
            return false;
        }

        if ((mouseButton == 0) && this.isHovered() && !this.isInMouseHighlightingMode && this.isVisible()) {
            if (!this.parent.isAtLeastOneLineInHighlightMode()) {
                this.parent.startHighlightLine = this;
            }
            this.isInMouseHighlightingMode = true;
            this.parent.setFocusedLine(Math.max(0, this.parent.getLineIndex(this)));
            super.mouseClicked(mouseX, mouseY, mouseButton);
            this.setHighlightPos(this.getCursorPosition());
        } else if ((mouseButton == 0) && !this.isHovered()) {
            //Clear highlighting when left-clicked in another line, etc.
            this.setHighlightPos(this.getCursorPosition());
        }

        if (!this.isInMouseHighlightingMode && (mouseButton == 0)) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return true;

    }

    @Override
    public void setValue(String p_94145_) {
        super.setValue(p_94145_);
        this.textWidth = this.font2.width(this.getValue());
    }

    @Override
    public void insertText(String textToWrite) {
        super.insertText(textToWrite);
        this.textWidth = this.font2.width(this.getValue());
    }

    @Override
    public void setMaxLength(int p_94200_) {
        super.setMaxLength(p_94200_);
        this.textWidth = this.font2.width(this.getValue());
    }

}