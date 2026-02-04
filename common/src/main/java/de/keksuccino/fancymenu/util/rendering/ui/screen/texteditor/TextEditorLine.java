package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinEditBox;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TextEditorLine extends AdvancedTextField {

    public TextEditorWindowBody parent;
    protected String lastTickValue = "";
    public boolean isInMouseHighlightingMode = false;
    protected final boolean handleSelf2;
    public int textWidth = 0;
    public int lineIndex = 0;
    protected int currentHighlightPosXStart = 0;
    protected int currentHighlightPosXEnd = 0;
    protected int currentCharacterRenderIndex = 0;

    protected static boolean leftRightArrowWasDown = false;

    public TextEditorLine(Font font, int x, int y, int width, int height, boolean handleSelf, @Nullable CharacterFilter characterFilter, TextEditorWindowBody parent) {
        super(font, x, y, width, height, handleSelf, characterFilter);
        this.parent = parent;
        this.handleSelf2 = handleSelf;
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

        this.setTextColor(this.parent.textColor.get().getColorInt());
        this.setTextColorUneditable(this.parent.textColor.get().getColorInt());

        if (this.isVisible()) {

            if (this.isFocused()) {
                //Render focused background
                int lineX = this.parent.getEditorAreaX();
                int lineY = this.getY();
                int lineWidth = this.parent.getEditorAreaWidth();
                int lineHeight = this.height;
                int lineColor = this.parent.focusedLineColor.get().getColorInt();
                int areaY = this.parent.getEditorAreaY();
                int areaBottom = areaY + this.parent.getEditorAreaHeight();
                int visibleTop = Math.max(lineY, areaY);
                int visibleBottom = Math.min(lineY + lineHeight, areaBottom);
                int visibleHeight = visibleBottom - visibleTop;
                if (visibleHeight > 0) {
                    boolean roundTop = lineY <= areaY + 5;
                    boolean roundBottom = (lineY + lineHeight) >= areaBottom - 5;
                    if (roundTop || roundBottom) {
                        float radius = UIBase.getInterfaceCornerRoundingRadius();
                        if (roundTop && roundBottom) {
                            SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, lineX, visibleTop, lineWidth, visibleHeight, radius, radius, radius, radius, lineColor, partial);
                        } else if (roundTop) {
                            SmoothRectangleRenderer.renderSmoothRectRoundTopCornersScaled(graphics, lineX, visibleTop, lineWidth, visibleHeight, radius, lineColor, partial);
                        } else {
                            SmoothRectangleRenderer.renderSmoothRectRoundBottomCornersScaled(graphics, lineX, visibleTop, lineWidth, visibleHeight, radius, lineColor, partial);
                        }
                    } else {
                        graphics.fill(lineX, visibleTop, lineX + lineWidth, visibleTop + visibleHeight, lineColor);
                    }
                }
            }

            int textColorInt = this.isEditable() ? this.getAsAccessor().getTextColorFancyMenu() : this.getAsAccessor().getTextColorUneditableFancyMenu();
            int cursorPos = this.getCursorPosition() - this.getAsAccessor().getDisplayPosFancyMenu();
            int highlightPos = this.getAsAccessor().getHighlightPosFancyMenu() - this.getAsAccessor().getDisplayPosFancyMenu();
            String text = this.getValue();
            boolean isCursorInsideVisibleText = cursorPos >= 0 && cursorPos <= text.length();
            boolean renderCursor = this.isFocused() && (Util.getMillis() - this.getAsAccessor().getFocusedTimeFancyMenu()) / 300L % 2L == 0L && isCursorInsideVisibleText;
            float textHeight = UIBase.getUITextHeightNormal();
            float textX = this.getAsAccessor().getBorderedFancyMenu() ? this.getX() + 4.0F : this.getX() + 1.0F;
            float textY = this.getAsAccessor().getBorderedFancyMenu()
                    ? this.getY() + (this.height - textHeight) / 2F
                    : this.getY() + Math.max(0.0F, (this.getHeight() / 2F)) - (textHeight / 2F);
            float textXAfterCursor = textX;
            if (highlightPos > text.length()) {
                highlightPos = text.length();
            }

            MutableComponent beforeCursorComp = null;
            MutableComponent afterCursorComp = null;
            boolean renderAfterCursor = false;

            if (!text.isEmpty()) {
                String textBeforeCursor = isCursorInsideVisibleText ? text.substring(0, cursorPos) : text;
                beforeCursorComp = this.getFormattedText(textBeforeCursor);
                textXAfterCursor = textX + UIBase.getUITextWidthNormal(beforeCursorComp);

                if (isCursorInsideVisibleText && cursorPos < text.length()) {
                    afterCursorComp = this.getFormattedText(text.substring(cursorPos));
                    renderAfterCursor = true;
                }
            }

            boolean isCursorNotAtEndOfLine = this.getCursorPosition() < this.getValue().length() || this.getValue().length() >= this.getAsAccessor().getMaxLengthFancyMenu();
            float cursorPosRender = textXAfterCursor;
            if (!isCursorInsideVisibleText) {
                cursorPosRender = cursorPos > 0 ? textX + this.width : textX;
            } else if (isCursorNotAtEndOfLine) {
                cursorPosRender = textXAfterCursor - 1;
            }

            if (!text.isEmpty() && beforeCursorComp != null) {
                UIBase.renderText(graphics, beforeCursorComp, textX, textY, textColorInt);
                if (renderAfterCursor && afterCursorComp != null) {
                    UIBase.renderText(graphics, afterCursorComp, textXAfterCursor, textY, textColorInt);
                }
            }

            if (this.getAsAccessor().getHintFancyMenu() != null && text.isEmpty() && !this.isFocused()) {
                UIBase.renderText(graphics, this.getAsAccessor().getHintFancyMenu(), textXAfterCursor, textY, textColorInt);
            }

            if (!isCursorNotAtEndOfLine && this.getAsAccessor().getSuggestionFancyMenu() != null) {
                UIBase.renderText(graphics, this.getAsAccessor().getSuggestionFancyMenu(), cursorPosRender - 1, textY, -8355712);
            }

            if (renderCursor) {
                if (isCursorNotAtEndOfLine) {
                    graphics.fill((int) cursorPosRender, (int) (textY - 1), (int) cursorPosRender + 1, (int) (textY + 1 + textHeight), textColorInt);
                } else {
                    graphics.fill((int) cursorPosRender, (int) (textY + textHeight - 2), (int) cursorPosRender + 5, (int) (textY + textHeight - 1), textColorInt);
                }
            }

            if (highlightPos != cursorPos) {
                float highlightWidth = UIBase.getUITextWidth(text.substring(0, highlightPos));
                this.currentHighlightPosXStart = (int) cursorPosRender;
                this.currentHighlightPosXEnd = (int) (textX + highlightWidth) - 1;
                this.getAsAccessor().invokeRenderHighlightFancyMenu(graphics, this.currentHighlightPosXStart, (int) (textY - 1), this.currentHighlightPosXEnd, (int) (textY + 1 + textHeight));
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
            int mouseX = this.parent.getRenderMouseX();
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

        this.textWidth = Math.round(UIBase.getUITextWidth(this.getValue()));

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
        this.textWidth = Math.round(UIBase.getUITextWidth(this.getValue()));
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
        this.textWidth = Math.round(UIBase.getUITextWidth(this.getValue()));
    }

    @Override
    public void insertText(String textToWrite) {
        super.insertText(textToWrite);
        this.textWidth = Math.round(UIBase.getUITextWidth(this.getValue()));
    }

    @Override
    public void setMaxLength(int p_94200_) {
        super.setMaxLength(p_94200_);
        this.textWidth = Math.round(UIBase.getUITextWidth(this.getValue()));
    }

}
