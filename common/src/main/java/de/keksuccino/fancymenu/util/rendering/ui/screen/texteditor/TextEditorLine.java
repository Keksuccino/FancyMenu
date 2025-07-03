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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        if (this.isInEditorArea()) {
            super.render(graphics, mouseX, mouseY, partial);
        }
        this.lastTickValue = this.getValue();
    }

    /**
     * Creates a formatted component from a plain string, applying formatting rules.
     *
     * @param text The input string.
     * @param characterStartIndex The starting character index in the full line for context-sensitive formatting.
     * @return A formatted MutableComponent.
     */
    protected MutableComponent getFormattedText(String text, int characterStartIndex) {
        MutableComponent comp = Component.literal("");
        int currentIndex = characterStartIndex;
        for (char c : text.toCharArray()) {
            Style style = Style.EMPTY;
            for (TextEditorFormattingRule r : this.parent.formattingRules) {
                Style rs = r.getStyle(c, currentIndex, this.getCursorPosition(), this, this.parent.currentRenderCharacterIndexTotal + currentIndex, this.parent);
                if ((rs != null) && (rs != Style.EMPTY)) {
                    style = rs.applyTo(style);
                }
            }
            comp.append(Component.literal(String.valueOf(c)).withStyle(style));
            currentIndex++;
        }
        return comp;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (!this.isVisible() || !this.isInEditorArea()) {
            return;
        }

        // Reset total character index for the parent editor screen before rendering lines
        if (this.lineIndex == 0) {
            this.parent.currentRenderCharacterIndexTotal = 0;
        }

        this.setTextColor(this.parent.textColor.getRGB());
        this.setTextColorUneditable(this.parent.textColor.getRGB());

        // Render focused line background
        if (this.isFocused()) {
            graphics.fill(0, this.getY(), this.parent.width, this.getY() + this.height, this.parent.focusedLineColor.getRGB());
        }

        int textColorInt = this.isEditable() ? this.getAsAccessor().getTextColorFancyMenu() : this.getAsAccessor().getTextColorUneditableFancyMenu();
        int displayPos = this.getAsAccessor().getDisplayPosFancyMenu();
        int cursorPos = this.getCursorPosition() - displayPos;
        int highlightPos = this.getAsAccessor().getHighlightPosFancyMenu() - displayPos;

        // The visible part of the text, trimmed by the display position (scrolling)
        String visibleText = this.getValue().substring(displayPos);

        boolean isCursorInVisibleArea = cursorPos >= 0 && cursorPos <= visibleText.length();
        boolean renderCursor = this.isFocused() && (Util.getMillis() - this.getAsAccessor().getFocusedTimeFancyMenu()) / 300L % 2L == 0L && isCursorInVisibleArea;

        int textX = this.getX() + 1;
        int textY = this.getY() + (this.getHeight() - this.font2.lineHeight) / 2;

        // --- NEW AND IMPROVED RENDERING LOGIC ---

        // 1. Prepare formatted components for text before and after cursor
        String textBeforeCursorStr = isCursorInVisibleArea ? visibleText.substring(0, cursorPos) : visibleText;
        String textAfterCursorStr = isCursorInVisibleArea ? visibleText.substring(cursorPos) : "";

        MutableComponent textBeforeCursorComp = getFormattedText(textBeforeCursorStr, displayPos);
        MutableComponent textAfterCursorComp = getFormattedText(textAfterCursorStr, displayPos + textBeforeCursorStr.length());

        // 2. Calculate positions BEFORE rendering
        int textAfterCursorX = textX + this.font2.width(textBeforeCursorComp);
        int cursorRenderX = textAfterCursorX;

        // 3. Render the text parts
        graphics.drawString(this.font2, textBeforeCursorComp, textX, textY, textColorInt, false);
        graphics.drawString(this.font2, textAfterCursorComp, textAfterCursorX, textY, textColorInt, false);

        // Update the total character count for the parent editor
        this.parent.currentRenderCharacterIndexTotal += visibleText.length();

        // 4. Render Highlight
        if (highlightPos != cursorPos) {
            int selectionStart = Math.min(cursorPos, highlightPos);
            int selectionEnd = Math.max(cursorPos, highlightPos);

            String textToHighlightStart = visibleText.substring(0, selectionStart);
            String textToHighlightEnd = visibleText.substring(0, selectionEnd);

            // Calculate screen coordinates for highlight start and end based on formatted text width
            int highlightStartX = textX + this.font2.width(getFormattedText(textToHighlightStart, displayPos));
            int highlightEndX = textX + this.font2.width(getFormattedText(textToHighlightEnd, displayPos));

            // Cache positions for hover checks
            this.currentHighlightPosXStart = highlightStartX;
            this.currentHighlightPosXEnd = highlightEndX;

            graphics.textHighlight(highlightStartX, textY - 1, highlightEndX, textY + 1 + this.font2.lineHeight);
        } else {
            this.currentHighlightPosXStart = 0;
            this.currentHighlightPosXEnd = 0;
        }

        // 5. Render Cursor
        if (renderCursor) {
            boolean isCursorAtEnd = this.getCursorPosition() >= this.getValue().length();
            if (isCursorAtEnd) {
                // Render underscore cursor at the end
                graphics.drawString(this.font2, "_", cursorRenderX, textY, textColorInt, false);
            } else {
                // Render vertical bar cursor
                graphics.fill(cursorRenderX, textY - 1, cursorRenderX + 1, textY + this.font2.lineHeight, textColorInt);
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
        if (Screen.isCopy(keycode) || Screen.isPaste(keycode) || Screen.isSelectAll(keycode) || Screen.isCut(keycode)) {
            return false;
        }
        if (keycode == InputConstants.KEY_BACKSPACE) {
            return false;
        }
        if (((keycode == InputConstants.KEY_RIGHT) || (keycode == InputConstants.KEY_LEFT)) && this.parent.isInMouseHighlightingMode()) {
            return false;
        }
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