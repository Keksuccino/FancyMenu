//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.mixin.client.IMixinEditBox;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.gui.content.handling.AdvancedWidgetsHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TextEditorScreen extends Screen {

    //TODO Zeilennummer an linkem Rand rendern (kleiner als normaler Text)

    private static final Logger LOGGER = LogManager.getLogger();

    protected Screen parentScreen;
    protected CharacterFilter characterFilter;
    protected List<TextEditorInputBox> textFieldLines = new ArrayList<>();
    protected ScrollBar verticalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, 10, 40, 0, 0, 0, 0, UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor());
    protected ScrollBar horizontalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, 40, 10, 0, 0, 0, 0, UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor());
    protected int lastCursorPosSetByUser = 0;
    protected boolean justSwitchedLineByWordDeletion = false;
    protected boolean triggeredFocusedLineWasTooHighInCursorPosMethod = false;
    public int headerHeight = 50;
    public int footerHeight = 50;
    public int borderLeft = 20;
    public int borderRight = 20;
    public int lineHeight = 14;
    public Color screenBackgroundColor = new Color(60, 63, 65);
    public Color editorAreaBorderColor = UIBase.getButtonBorderIdleColor();
    public Color editorAreaBackgroundColor = new Color(43, 43, 43);
    public Color textColor = new Color(158, 170, 184);
    public Color focusedLineColor = new Color(50, 50, 50);
    public Color scrollGrabberIdleColor = new Color(89, 91, 93);
    public Color scrollGrabberHoverColor = new Color(102, 104, 104);
    public Color sideBarColor = new Color(49, 51, 53);
    protected int currentLineWidth;
    protected int lastTickFocusedLineIndex = -1;
    protected TextEditorInputBox startHighlightLine = null;
    protected int startHighlightLineIndex = -1;
    protected int endHighlightLineIndex = -1;
    protected int overriddenTotalScrollHeight = -1;

    public TextEditorScreen(Component name, @Nullable Screen parent, @Nullable CharacterFilter characterFilter) {
        super(name);
        this.parentScreen = parent;
        this.characterFilter = characterFilter;
        this.addLine();
        this.getLine(0).setFocus(true);
        this.verticalScrollBar.setScrollWheelAllowed(true);
    }

    @Override
    protected void init() {

        super.init();

        this.verticalScrollBar.scrollAreaStartX = this.getEditorAreaX() - 1;
        this.verticalScrollBar.scrollAreaStartY = this.getEditorAreaY() - 1;
        this.verticalScrollBar.scrollAreaEndX = this.getEditorAreaX() + this.getEditorAreaWidth() + 10;
        this.verticalScrollBar.scrollAreaEndY = this.getEditorAreaY() + this.getEditorAreaHeight() + 1;

        this.horizontalScrollBar.scrollAreaStartX = this.getEditorAreaX() - 1;
        this.horizontalScrollBar.scrollAreaStartY = this.getEditorAreaY() - 1;
        this.horizontalScrollBar.scrollAreaEndX = this.getEditorAreaX() + this.getEditorAreaWidth() + 1;
        this.horizontalScrollBar.scrollAreaEndY = this.getEditorAreaY() + this.getEditorAreaHeight() + 10;

    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {

        //Update scroll grabber colors
        this.verticalScrollBar.idleBarColor = this.scrollGrabberIdleColor;
        this.verticalScrollBar.hoverBarColor = this.scrollGrabberHoverColor;
        this.horizontalScrollBar.idleBarColor = this.scrollGrabberIdleColor;
        this.horizontalScrollBar.hoverBarColor = this.scrollGrabberHoverColor;

        //Reset scrolls if content fits editor area
        if (this.currentLineWidth <= this.getEditorAreaWidth()) {
            this.horizontalScrollBar.setScroll(0.0F);
        }
        if (this.getTotalLineHeight() <= this.getEditorAreaHeight()) {
            this.verticalScrollBar.setScroll(0.0F);
        }

        this.justSwitchedLineByWordDeletion = false;

        this.updateCurrentLineWidth();

        //Adjust the scroll wheel speed depending on the amount of lines
        this.verticalScrollBar.setWheelScrollSpeed(1.0F / ((float)this.getTotalScrollHeight() / 500.0F));

        this.renderScreenBackground(matrix);

        this.renderEditorAreaBackground(matrix);

        Window win = Minecraft.getInstance().getWindow();
        double scale = win.getGuiScale();
        int sciBottom = this.height - this.footerHeight;
        //Don't render parts of lines outside of editor area
        RenderSystem.enableScissor((int)(this.borderLeft * scale), (int)(win.getHeight() - (sciBottom * scale)), (int)(this.getEditorAreaWidth() * scale), (int)(this.getEditorAreaHeight() * scale));

        //Update positions and size of lines and render them
        this.updateLines((line) -> {
            line.render(matrix, mouseX, mouseY, partial);
        });

        RenderSystem.disableScissor();

        this.verticalScrollBar.render(matrix);
        this.horizontalScrollBar.render(matrix);

//        super.render(matrix, mouseX, mouseY, partial);

        this.lastTickFocusedLineIndex = this.getFocusedLineIndex();
        this.triggeredFocusedLineWasTooHighInCursorPosMethod = false;

        this.renderBorder(matrix);

        this.tickMouseHighlighting();

    }

    protected void renderBorder(PoseStack matrix) {
        //top
        fill(matrix, this.borderLeft - 1, this.headerHeight - 1, this.width - this.borderRight + 1, this.headerHeight, this.editorAreaBorderColor.getRGB());
        //left
        fill(matrix, this.borderLeft - 1, this.headerHeight, this.borderLeft, this.height - this.footerHeight, this.editorAreaBorderColor.getRGB());
        //right
        fill(matrix, this.width - this.borderRight, this.headerHeight, this.width - this.borderRight+1, this.height - this.footerHeight, this.editorAreaBorderColor.getRGB());
        //down
        fill(matrix, this.borderLeft - 1, this.height - this.footerHeight, this.width - this.borderRight + 1, this.height - this.footerHeight + 1, this.editorAreaBorderColor.getRGB());
    }

    protected void renderEditorAreaBackground(PoseStack matrix) {
        fill(matrix, this.getEditorAreaX(), this.getEditorAreaY(), this.getEditorAreaX() + this.getEditorAreaWidth(), this.getEditorAreaY() + this.getEditorAreaHeight(), this.editorAreaBackgroundColor.getRGB());
    }

    protected void renderScreenBackground(PoseStack matrix) {
        fill(matrix, 0, 0, this.width, this.height, this.screenBackgroundColor.getRGB());
    }

    protected void tickMouseHighlighting() {

        if (!MouseInput.isLeftMouseDown()) {
            this.startHighlightLine = null;
            return;
        }

        //Auto-scroll if mouse outside of editor area and in mouse-highlighting mode
        if (this.isInMouseHighlightingMode()) {
            int mX = MouseInput.getMouseX();
            int mY = MouseInput.getMouseY();
            float speedMult = 0.008F;
            if (mX < this.borderLeft) {
                float f = Math.max(0.01F, (float)(this.borderLeft - mX) * speedMult);
                this.horizontalScrollBar.setScroll(this.horizontalScrollBar.getScroll() - f);
            } else if (mX > (this.width - this.borderRight)) {
                float f = Math.max(0.01F, (float)(mX - (this.width - this.borderRight)) * speedMult);
                this.horizontalScrollBar.setScroll(this.horizontalScrollBar.getScroll() + f);
            }
            if (mY < this.headerHeight) {
                float f = Math.max(0.01F, (float)(this.headerHeight - mY) * speedMult);
                LOGGER.info(f);
                this.verticalScrollBar.setScroll(this.verticalScrollBar.getScroll() - f);
            } else if (mY > (this.height - this.footerHeight)) {
                float f = Math.max(0.01F, (float)(mY - (this.height - this.footerHeight)) * speedMult);
                LOGGER.info(f);
                this.verticalScrollBar.setScroll(this.verticalScrollBar.getScroll() + f);
            }
        }

        if (!this.isMouseInsideEditorArea()) {
            return;
        }

        TextEditorInputBox first = this.startHighlightLine;
        TextEditorInputBox hovered = this.getHoveredLine();
        if ((hovered != null) && !hovered.isFocused() && (first != null)) {

            int firstIndex = this.getLineIndex(first);
            int hoveredIndex = this.getLineIndex(hovered);
            boolean firstIsBeforeHovered = hoveredIndex > firstIndex;
            boolean firstIsAfterHovered = hoveredIndex < firstIndex;

            if (first.isInMouseHighlightingMode) {
                if (firstIsAfterHovered) {
                    this.setFocusedLine(this.getLineIndex(hovered));
                    if (!hovered.isInMouseHighlightingMode) {
                        hovered.isInMouseHighlightingMode = true;
                        hovered.getAsAccessor().setShiftPressedFancyMenu(false);
                        hovered.moveCursorTo(hovered.getValue().length());
                    }
                } else if (firstIsBeforeHovered) {
                    this.setFocusedLine(this.getLineIndex(hovered));
                    if (!hovered.isInMouseHighlightingMode) {
                        hovered.isInMouseHighlightingMode = true;
                        hovered.getAsAccessor().setShiftPressedFancyMenu(false);
                        hovered.moveCursorTo(0);
                    }
                } else if (first == hovered) {
                    this.setFocusedLine(this.getLineIndex(first));
                }
            }

            int startIndex = Math.min(hoveredIndex, firstIndex);
            int endIndex = Math.max(hoveredIndex, firstIndex);
            int index = 0;
            for (TextEditorInputBox t : this.textFieldLines) {
                //Highlight all lines between the first and current line and remove highlighting from lines outside of highlight range
                if ((t != hovered) && (t != first)) {
                    if ((index > startIndex) && (index < endIndex)) {
                        if (firstIsAfterHovered) {
                            t.setCursorPosition(0);
                            t.setHighlightPos(t.getValue().length());
                        } else if (firstIsBeforeHovered) {
                            t.setCursorPosition(t.getValue().length());
                            t.setHighlightPos(0);
                        }
                    } else {
                        t.getAsAccessor().setShiftPressedFancyMenu(false);
                        t.moveCursorTo(0);
                        t.isInMouseHighlightingMode = false;
                    }
                }
                index++;
            }
            this.startHighlightLineIndex = startIndex;
            this.endHighlightLineIndex = endIndex;

            if (first != hovered) {
                first.getAsAccessor().setShiftPressedFancyMenu(true);
                if (firstIsAfterHovered) {
                    first.moveCursorTo(0);
                } else if (firstIsBeforeHovered) {
                    first.moveCursorTo(first.getValue().length());
                }
                first.getAsAccessor().setShiftPressedFancyMenu(false);
            }

        }

        TextEditorInputBox focused = this.getFocusedLine();
        if ((focused != null) && focused.isInMouseHighlightingMode) {
            if ((this.startHighlightLineIndex == -1) && (this.endHighlightLineIndex == -1)) {
                this.startHighlightLineIndex = this.getLineIndex(focused);
                this.endHighlightLineIndex = this.startHighlightLineIndex;
            }
            int i = Mth.floor(MouseInput.getMouseX()) - focused.getX();
            if (focused.getAsAccessor().getBorderedFancyMenu()) {
                i -= 4;
            }
            String s = this.font.plainSubstrByWidth(focused.getValue().substring(focused.getAsAccessor().getDisplayPosFancyMenu()), focused.getInnerWidth());
            focused.getAsAccessor().setShiftPressedFancyMenu(true);
            focused.moveCursorTo(this.font.plainSubstrByWidth(s, i).length() + focused.getAsAccessor().getDisplayPosFancyMenu());
            focused.getAsAccessor().setShiftPressedFancyMenu(false);
            if ((focused.getAsAccessor().getHighlightPosFancyMenu() == focused.getCursorPosition()) && (this.startHighlightLineIndex == this.endHighlightLineIndex)) {
                this.resetHighlighting();
            }
        }

    }

    protected void updateLines(@Nullable Consumer<TextEditorInputBox> doAfterEachLineUpdate) {
        try {
            int index = 0;
            for (TextEditorInputBox line : this.textFieldLines) {
                line.y = this.headerHeight + (this.lineHeight * index) + this.getLineRenderOffsetY();
                line.x = this.borderLeft + this.getLineRenderOffsetX();
                line.setWidth(this.currentLineWidth);
                line.setHeight(this.lineHeight);
                line.getAsAccessor().setDisplayPosFancyMenu(0);
                if (doAfterEachLineUpdate != null) {
                    doAfterEachLineUpdate.accept(line);
                }
                index++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void updateCurrentLineWidth() {
        //Find width of the longest line and update current line width
        int longestTextWidth = 0;
        for (TextEditorInputBox f : this.textFieldLines) {
            if (f.textWidth > longestTextWidth) {
                //Calculating the text size for every line every tick kills the CPU, so I'm calculating the size on value change in the text box
                longestTextWidth = f.textWidth;
            }
        }
        this.currentLineWidth = longestTextWidth + 30;
    }

    protected int getLineRenderOffsetX() {
        return -(int)(((float)this.getTotalScrollWidth() / 100.0F) * (this.horizontalScrollBar.getScroll() * 100.0F));
    }

    protected int getLineRenderOffsetY() {
        return -(int)(((float)this.getTotalScrollHeight() / 100.0F) * (this.verticalScrollBar.getScroll() * 100.0F));
    }

    protected int getTotalLineHeight() {
        return this.lineHeight * this.textFieldLines.size();
    }

    protected TextEditorInputBox addLineAtIndex(int index) {
        TextEditorInputBox f = new TextEditorInputBox(Minecraft.getInstance().font, 0, 0, 50, this.lineHeight, true, this.characterFilter, this);
        f.setMaxLength(Integer.MAX_VALUE);
        if (index > 0) {
            TextEditorInputBox before = this.getLine(index-1);
            if (before != null) {
                f.setY(before.getY() + this.lineHeight);
            }
        }
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
        int index = this.getFocusedLineIndex();
        if (index != -1) {
            return this.getLine(index);
        }
        return null;
    }

    protected boolean isLineFocused() {
        return (this.getFocusedLineIndex() > -1);
    }

    @Nullable
    protected TextEditorInputBox getLineAfter(TextEditorInputBox line) {
        int index = this.getLineIndex(line);
        if ((index > -1) && (index < (this.getLineCount()-1))) {
            return this.getLine(index+1);
        }
        return null;
    }

    @Nullable
    protected TextEditorInputBox getLineBefore(TextEditorInputBox line) {
        int index = this.getLineIndex(line);
        if (index > 0) {
            return this.getLine(index-1);
        }
        return null;
    }

    protected boolean isAtLeastOneLineInHighlightMode() {
        for (TextEditorInputBox t : this.textFieldLines) {
            if (t.isInMouseHighlightingMode) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    /** Returns the lines between two indexes, EXCLUDING start AND end indexes! **/
    protected List<TextEditorInputBox> getLinesBetweenIndexes(int startIndex, int endIndex) {
        startIndex = Math.min(Math.max(startIndex, 0), this.textFieldLines.size()-1);
        endIndex = Math.min(Math.max(endIndex, 0), this.textFieldLines.size()-1);
        List<TextEditorInputBox> l = new ArrayList<>();
        l.addAll(this.textFieldLines.subList(startIndex, endIndex));
        if (!l.isEmpty()) {
            l.remove(0);
        }
        return l;
    }

    @Nullable
    public TextEditorInputBox getHoveredLine() {
        for (TextEditorInputBox t : this.textFieldLines) {
            if (t.isHovered()) {
                return t;
            }
        }
        return null;
    }

    public int getLineIndex(TextEditorInputBox inputBox) {
        return this.textFieldLines.indexOf(inputBox);
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
                    //Add amount of spaces of the beginning of the old line to the beginning of the new line
                    if (textBeforeCursor.startsWith(" ")) {
                        int spaces = 0;
                        for (char c : textBeforeCursor.toCharArray()) {
                            if (String.valueOf(c).equals(" ")) {
                                spaces++;
                            } else {
                                break;
                            }
                        }
                        nextLine.setValue(textBeforeCursor.substring(0, spaces) + nextLine.getValue());
                        nextLine.moveCursorTo(spaces);
                    }
                } else {
                    nextLine.moveCursorTo(this.lastCursorPosSetByUser);
                }
            }
        }
    }

    public List<TextEditorInputBox> getCopyOfLines() {
        List<TextEditorInputBox> l = new ArrayList<>();
        for (TextEditorInputBox t : this.textFieldLines) {
            TextEditorInputBox n = new TextEditorInputBox(this.font, 0, 0, 0, 0, true, this.characterFilter, this);
            n.setValue(t.getValue());
            n.setFocus(t.isFocused());
            n.moveCursorTo(t.getCursorPosition());
            l.add(n);
        }
        return l;
    }

    public boolean isTextHighlighted() {
        return (this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1);
    }

    @NotNull
    public String getHighlightedText() {
        try {
            if ((this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1)) {
                List<TextEditorInputBox> lines = new ArrayList<>();
                lines.add(this.getLine(this.startHighlightLineIndex));
                if (this.startHighlightLineIndex != this.endHighlightLineIndex) {
                    lines.addAll(this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex));
                    lines.add(this.getLine(this.endHighlightLineIndex));
                }
                StringBuilder s = new StringBuilder();
                boolean b = false;
                for (TextEditorInputBox t : lines) {
                    if (b) {
                        s.append("\n");
                    }
                    s.append(t.getHighlighted());
                    b = true;
                }
                String ret = s.toString();
                return ret;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @NotNull
    public String cutHighlightedText() {
        String highlighted = this.getHighlightedText();
        this.deleteHighlightedText();
        return highlighted;
    }

    public void deleteHighlightedText() {
        int linesRemoved = 0;
        try {
            if ((this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1)) {
                if (this.startHighlightLineIndex == this.endHighlightLineIndex) {
                    this.getLine(this.startHighlightLineIndex).insertText("");
                } else {
                    TextEditorInputBox start = this.getLine(this.startHighlightLineIndex);
                    start.insertText("");
                    TextEditorInputBox end = this.getLine(this.endHighlightLineIndex);
                    end.insertText("");
                    if ((this.endHighlightLineIndex - this.startHighlightLineIndex) > 1) {
                        for (TextEditorInputBox line : this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex)) {
                            this.removeLineAtIndex(this.getLineIndex(line));
                            linesRemoved++;
                        }
                    }
                    String oldStartValue = start.getValue();
                    start.setCursorPosition(start.getValue().length());
                    start.setHighlightPos(start.getCursorPosition());
                    start.insertText(end.getValue());
                    start.setCursorPosition(oldStartValue.length());
                    start.setHighlightPos(start.getCursorPosition());
                    this.removeLineAtIndex(this.getLineIndex(end));
                    linesRemoved++;
                    this.setFocusedLine(this.startHighlightLineIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.correctYScroll(-linesRemoved);
        this.resetHighlighting();
    }

    public void resetHighlighting() {
        this.startHighlightLineIndex = -1;
        this.endHighlightLineIndex = -1;
        for (TextEditorInputBox t : this.textFieldLines) {
            t.setHighlightPos(t.getCursorPosition());
        }
    }

    public boolean isInMouseHighlightingMode() {
        return MouseInput.isLeftMouseDown() && (this.startHighlightLine != null);
    }

    public void pasteText(String text) {
        try {
            if ((text != null) && !text.equals("")) {
                int addedLinesCount = 0;
                if (this.isTextHighlighted()) {
                    this.deleteHighlightedText();
                }
                if (!this.isLineFocused()) {
                    this.setFocusedLine(this.getLineCount()-1);
                    this.getFocusedLine().moveCursorToEnd();
                }
                TextEditorInputBox focusedLine = this.getFocusedLine();
                //These two strings are for correctly pasting text within a char sequence (if the cursor is not at the end or beginning of the line)
                String textBeforeCursor = "";
                String textAfterCursor = "";
                if (focusedLine.getValue().length() > 0) {
                    textBeforeCursor = focusedLine.getValue().substring(0, focusedLine.getCursorPosition());
                    if (focusedLine.getCursorPosition() < focusedLine.getValue().length()) {
                        textAfterCursor = this.getFocusedLine().getValue().substring(focusedLine.getCursorPosition(), focusedLine.getValue().length());
                    }
                }
                focusedLine.setValue(textBeforeCursor);
                focusedLine.setCursorPosition(textBeforeCursor.length());
                String[] lines = new String[]{text};
                if (text.contains("\n")) {
                    lines = text.split("\n");
                }
                Array.set(lines, lines.length-1, lines[lines.length-1] + textAfterCursor);
                if (lines.length == 1) {
                    this.getFocusedLine().insertText(lines[0]);
                } else if (lines.length > 1) {
                    int index = -1;
                    for (String s : lines) {
                        if (index == -1) {
                            index = this.getFocusedLineIndex();
                        } else {
                            this.addLineAtIndex(index);
                            addedLinesCount++;
                        }
                        this.getLine(index).insertText(s);
                        index++;
                    }
                    this.setFocusedLine(index - 1);
                    this.getFocusedLine().setCursorPosition(Math.max(0, this.getFocusedLine().getValue().length() - textAfterCursor.length()));
                    this.getFocusedLine().setHighlightPos(this.getFocusedLine().getCursorPosition());
                }
                this.correctYScroll(addedLinesCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.resetHighlighting();
    }

    public void setText(String text) {
        TextEditorInputBox t = this.getLine(0);
        this.textFieldLines.clear();
        this.textFieldLines.add(t);
        this.setFocusedLine(0);
        t.setValue("");
        t.moveCursorTo(0);
        this.pasteText(text);
    }

    public String getText() {
        StringBuilder s = new StringBuilder();
        boolean b = false;
        for (TextEditorInputBox t : this.textFieldLines) {
            if (b) {
                s.append("\n");
            }
            s.append(t.getValue());
            b = true;
        }
        return s.toString();
    }

    @Override
    public boolean keyPressed(int keycode, int i1, int i2) {

        //ENTER
        if (keycode == 257) {
            if (!this.isInMouseHighlightingMode()) {
                if (this.isLineFocused()) {
                    this.resetHighlighting();
                    this.goDownLine(true);
                    this.correctYScroll(1);
                }
            }
            return true;
        }
        //ARROW UP
        if (keycode == InputConstants.KEY_UP) {
            if (!this.isInMouseHighlightingMode()) {
                this.resetHighlighting();
                this.goUpLine();
                this.correctYScroll(0);
            }
            return true;
        }
        //ARROW DOWN
        if (keycode == InputConstants.KEY_DOWN) {
            if (!this.isInMouseHighlightingMode()) {
                this.resetHighlighting();
                this.goDownLine(false);
                this.correctYScroll(0);
            }
            return true;
        }

        //BACKSPACE
        if (keycode == InputConstants.KEY_BACKSPACE) {
            if (!this.isInMouseHighlightingMode()) {
                if (this.isTextHighlighted()) {
                    this.deleteHighlightedText();
                } else {
                    if (this.isLineFocused()) {
                        TextEditorInputBox focused = this.getFocusedLine();
                        focused.getAsAccessor().setShiftPressedFancyMenu(false);
                        focused.getAsAccessor().invokeDeleteTextFancyMenu(-1);
                        focused.getAsAccessor().setShiftPressedFancyMenu(Screen.hasShiftDown());
                    }
                }
                this.resetHighlighting();
            }
            return true;
        }
        //CTRL + C
        if (Screen.isCopy(keycode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlightedText());
            return true;
        }
        //CTRL + V
        if (Screen.isPaste(keycode)) {
            this.pasteText(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        //CTRL + A
        if (Screen.isSelectAll(keycode)) {
            for (TextEditorInputBox t : this.textFieldLines) {
                t.setHighlightPos(0);
                t.setCursorPosition(t.getValue().length());
            }
            this.setFocusedLine(this.getLineCount()-1);
            this.startHighlightLineIndex = 0;
            this.endHighlightLineIndex = this.getLineCount()-1;
            return true;
        }
        //CTRL + U
        if (Screen.isCut(keycode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.cutHighlightedText());
            this.resetHighlighting();
            return true;
        }
        //Reset highlighting when pressing left/right arrow keys
        if ((keycode == InputConstants.KEY_RIGHT) || (keycode == InputConstants.KEY_LEFT)) {
            this.resetHighlighting();
            return true;
        }

        return super.keyPressed(keycode, i1, i2);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (this.isMouseInsideEditorArea()) {
            if (button == 0) {
                this.resetHighlighting();
                if (this.getHoveredLine() == null) {
                    TextEditorInputBox focus = this.getLine(this.getLineCount()-1);
                    for (TextEditorInputBox t : this.textFieldLines) {
                        if ((MouseInput.getMouseY() >= t.y) && (MouseInput.getMouseY() <= t.y + t.getHeight())) {
                            focus = t;
                            break;
                        }
                    }
                    this.setFocusedLine(this.getLineIndex(focus));
                    this.getFocusedLine().moveCursorToEnd();
                    this.correctYScroll(0);
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);

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

    public void scrollToLine(int lineIndex, boolean bottom) {
        if (bottom) {
            this.scrollToLine(lineIndex, -Math.max(0, this.getEditorAreaHeight() - this.lineHeight));
        } else {
            this.scrollToLine(lineIndex, 0);
        }
    }

    public void scrollToLine(int lineIndex, int offset) {
        int totalLineHeight = this.getTotalScrollHeight();
        float f = (float)Math.max(0, ((lineIndex + 1) * this.lineHeight) - this.lineHeight) / (float)totalLineHeight;
        if (offset != 0) {
            if (offset > 0) {
                f += ((float)offset / (float)totalLineHeight);
            } else {
                f -= ((float)Math.abs(offset) / (float)totalLineHeight);
            }
        }
        this.verticalScrollBar.setScroll(f);
    }

    protected int getTotalScrollHeight() {
        if (this.overriddenTotalScrollHeight != -1) {
            return this.overriddenTotalScrollHeight;
        }
        return this.getTotalLineHeight();
    }

    protected int getTotalScrollWidth() {
        //return Math.max(0, this.currentLineWidth - this.getEditorAreaWidth())
        return this.currentLineWidth;
    }

    protected void correctYScroll(int lineCountOffsetAfterRemovingAdding) {

        //Don't fix scroll if in mouse-highlighting mode or no line is focused
        if (this.isInMouseHighlightingMode() || !this.isLineFocused()) {
            return;
        }

        int minY = this.getEditorAreaY();
        int maxY = this.getEditorAreaY() + this.getEditorAreaHeight();
        int currentLineY = this.getFocusedLine().getY();

        if (currentLineY < minY) {
            this.scrollToLine(this.getFocusedLineIndex(), false);
        } else if ((currentLineY + this.lineHeight) > maxY) {
            this.scrollToLine(this.getFocusedLineIndex(), true);
        } else if (lineCountOffsetAfterRemovingAdding != 0) {
            this.overriddenTotalScrollHeight = -1;
            int removedAddedLineCount = Math.abs(lineCountOffsetAfterRemovingAdding);
            if (lineCountOffsetAfterRemovingAdding > 0) {
                this.overriddenTotalScrollHeight = this.getTotalScrollHeight() - (this.lineHeight * removedAddedLineCount);
            } else if (lineCountOffsetAfterRemovingAdding < 0) {
                this.overriddenTotalScrollHeight = this.getTotalScrollHeight() + (this.lineHeight * removedAddedLineCount);
            }
            this.updateLines(null);
            this.overriddenTotalScrollHeight = -1;
            int diffToTop = Math.max(0, this.getFocusedLine().getY() - this.getEditorAreaY());
            this.scrollToLine(this.getFocusedLineIndex(), -diffToTop);
            this.correctYScroll(0);
        }

        if (this.getTotalLineHeight() <= this.getEditorAreaHeight()) {
            this.verticalScrollBar.setScroll(0.0F);
        }

    }

    protected void correctXScroll(TextEditorInputBox calledIn) {

        //Don't fix scroll if in mouse-highlighting mode
        if (this.isInMouseHighlightingMode()) {
            return;
        }

        if (this.isLineFocused() && (this.getFocusedLine() == calledIn)) {

            int xStart = calledIn.x;

            this.updateCurrentLineWidth();
            this.updateLines(null);

            //Make the lines scroll horizontally with the cursor position if the cursor is too far to the left or right
            int cursorWidth = 2;
            if (calledIn.getCursorPosition() >= calledIn.getValue().length()) {
                cursorWidth = 6;
            }
            int editorAreaCenterX = this.getEditorAreaX() + (this.getEditorAreaWidth() / 2);
            int cursorX = this.getEditBoxCursorX(calledIn);
            if (cursorX > editorAreaCenterX) {
                cursorX += cursorWidth + 5;
            } else if (cursorX < editorAreaCenterX) {
                cursorX -= cursorWidth + 5;
            }
            int maxToRight = this.width - this.borderRight;
            int maxToLeft = this.borderLeft;
            float currentScrollX = this.horizontalScrollBar.getScroll();
            int currentLineW = this.getTotalScrollWidth();
            boolean textGotDeleted = calledIn.lastTickValue.length() > calledIn.getValue().length();
            if (cursorX > maxToRight) {
                float f = (float)(cursorX - maxToRight) / (float)currentLineW;
                this.horizontalScrollBar.setScroll(currentScrollX + f);
            } else if (cursorX < maxToLeft) {
                //By default, move back the line just a little when moving the cursor to the left side by using the mouse or arrow keys
                float f = (float)(maxToLeft - cursorX) / (float)currentLineW;
                //But move it back a big chunk when deleting chars (by pressing backspace)
                if (textGotDeleted) {
                    f = (float)(maxToRight - maxToLeft) / (float)currentLineW;
                }
                this.horizontalScrollBar.setScroll(currentScrollX - f);
            } else if ((calledIn.x < 0) && textGotDeleted && (xStart < calledIn.x)) {
                float f = (float)(calledIn.x - xStart) / (float)currentLineW;
                this.horizontalScrollBar.setScroll(currentScrollX + f);
            } else if (xStart > calledIn.x) {
                float f = (float)(xStart - calledIn.x) / (float)currentLineW;
                this.horizontalScrollBar.setScroll(currentScrollX - f);
            }
            if (calledIn.getCursorPosition() == 0) {
                this.horizontalScrollBar.setScroll(0.0F);
            }

        }

    }

    public boolean isMouseInsideEditorArea() {
        int xStart = this.borderLeft;
        int yStart = this.headerHeight;
        int xEnd = this.width - this.borderRight;
        int yEnd = this.height - this.footerHeight;
        int mX = MouseInput.getMouseX();
        int mY = MouseInput.getMouseY();
        return (mX >= xStart) && (mX <= xEnd) && (mY >= yStart) && (mY <= yEnd);
    }

    public int getEditorAreaWidth() {
        return (this.width - this.borderRight) - this.borderLeft;
    }

    public int getEditorAreaHeight() {
        return (this.height - this.footerHeight) - this.headerHeight;
    }

    public int getEditorAreaX() {
        return this.borderLeft;
    }

    public int getEditorAreaY() {
        return this.headerHeight;
    }

    public static class TextEditorInputBox extends AdvancedTextField {

        public TextEditorScreen parent;
        protected String lastTickValue = "";
        protected boolean cursorPositionTicked = false;
        public boolean isInMouseHighlightingMode = false;
        protected final Font font2;
        protected final boolean handleSelf2;
        public int textWidth = 0;

        protected static boolean leftRightArrowWasDown = false;

        public TextEditorInputBox(Font font, int x, int y, int width, int height, boolean handleSelf, @Nullable CharacterFilter characterFilter, TextEditorScreen parent) {
            super(font, x, y, width, height, handleSelf, characterFilter);
            this.parent = parent;
            this.font2 = font;
            this.handleSelf2 = handleSelf;
            this.setBordered(false);
        }

        @Override
        public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {

            //Only render line if inside of the editor area (for performance reasons)
            if ((this.getY() + this.getHeight() >= this.parent.getEditorAreaY()) && (this.getY() <= this.parent.getEditorAreaY() + this.parent.getEditorAreaHeight())) {
                super.render(matrix, mouseX, mouseY, partial);
            }

            this.lastTickValue = this.getValue();

        }

        @Override
        public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partial) {

            this.setTextColor(this.parent.textColor.getRGB());
            this.setTextColorUneditable(this.parent.textColor.getRGB());

            if (this.handleSelf2) {
                AdvancedWidgetsHandler.handleWidget(this);
            }

            if (this.isVisible()) {

                if (this.isFocused()) {
                    //Render focused background
                    fill(matrix, 0, this.getY(), this.parent.width, this.getY() + this.height, this.parent.focusedLineColor.getRGB());
                }

                int textColorInt = this.isEditable() ? this.getAsAccessor().getTextColorFancyMenu() : this.getAsAccessor().getTextColorUneditableFancyMenu();
                int cursorPos = this.getCursorPosition() - this.getAsAccessor().getDisplayPosFancyMenu();
                int highlightPos = this.getAsAccessor().getHighlightPosFancyMenu() - this.getAsAccessor().getDisplayPosFancyMenu();
//                String text = this.font2.plainSubstrByWidth(this.getValue().substring(this.getAsAccessor().getDisplayPosFancyMenu()), this.getInnerWidth());
                String text = this.getValue();
                boolean isCursorNotAtStartOrEnd = cursorPos >= 0 && cursorPos <= text.length();
                boolean renderCursor = this.isFocused() && this.getAsAccessor().getFrameFancyMenu() / 6 % 2 == 0 && isCursorNotAtStartOrEnd;
                int textX = this.getAsAccessor().getBorderedFancyMenu() ? this.getX() + 4 : this.getX() + 1;
                int textY = this.getAsAccessor().getBorderedFancyMenu() ? this.getY() + (this.height - 8) / 2 : (this.getY() + Math.max(0, (this.getHeight() / 2)) - (this.font2.lineHeight / 2));
                int textXRender = textX;
                if (highlightPos > text.length()) {
                    highlightPos = text.length();
                }

                if (!text.isEmpty()) {
                    String textBeforeCursor = isCursorNotAtStartOrEnd ? text.substring(0, cursorPos) : text;
                    //Render text before cursor
                    textXRender = this.font2.drawShadow(matrix, this.getAsAccessor().getFormatterFancyMenu().apply(textBeforeCursor, this.getAsAccessor().getDisplayPosFancyMenu()), (float)textX, (float)textY, textColorInt);
                }

                boolean isCursorAtEndOfLine = this.getCursorPosition() < this.getValue().length() || this.getValue().length() >= this.getAsAccessor().getMaxLengthFancyMenu();
                int cursorPosRender = textXRender;
                if (!isCursorNotAtStartOrEnd) {
                    cursorPosRender = cursorPos > 0 ? textX + this.width : textX;
                } else if (isCursorAtEndOfLine) {
                    cursorPosRender = textXRender - 1;
                    --textXRender;
                }

                if (!text.isEmpty() && isCursorNotAtStartOrEnd && cursorPos < text.length()) {
                    //Render text after cursor
                    this.font2.drawShadow(matrix, this.getAsAccessor().getFormatterFancyMenu().apply(text.substring(cursorPos), this.getCursorPosition()), (float)textXRender, (float)textY, textColorInt);
                }

                if (this.getAsAccessor().getHintFancyMenu() != null && text.isEmpty() && !this.isFocused()) {
                    this.font2.drawShadow(matrix, this.getAsAccessor().getHintFancyMenu(), (float)textXRender, (float)textY, textColorInt);
                }

                if (!isCursorAtEndOfLine && this.getAsAccessor().getSuggestionFancyMenu() != null) {
                    this.font2.drawShadow(matrix, this.getAsAccessor().getSuggestionFancyMenu(), (float)(cursorPosRender - 1), (float)textY, -8355712);
                }

                if (renderCursor) {
                    if (isCursorAtEndOfLine) {
                        GuiComponent.fill(matrix, cursorPosRender, textY - 1, cursorPosRender + 1, textY + 1 + 9, -3092272);
                    } else {
                        this.font2.drawShadow(matrix, "_", (float)cursorPosRender, (float)textY, textColorInt);
                    }
                }

                if (highlightPos != cursorPos) {
                    int highlightX = textX + this.font2.width(text.substring(0, highlightPos));
                    this.getAsAccessor().invokeRenderHighlightFancyMenu(cursorPosRender, textY - 1, highlightX - 1, textY + 1 + 9);
                }

            }

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

            super.setCursorPosition(newPos);

            //Caching the last cursor position set by the user, to set it to the new line when changing the line
            if ((newPos != this.parent.lastCursorPosSetByUser) && this.isFocused()) {
                this.parent.lastCursorPosSetByUser = this.getCursorPosition();
            }

            this.parent.correctXScroll(this);
            this.cursorPositionTicked = true;

        }

        @Override
        public void onTick() {

            this.cursorPositionTicked = false;

            if (!MouseInput.isLeftMouseDown() && this.isInMouseHighlightingMode) {
                this.isInMouseHighlightingMode = false;
            }

            super.onTick();

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
                        this.parent.getFocusedLine().moveCursorTo(this.parent.getFocusedLine().getValue().length());
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
                        this.parent.getFocusedLine().moveCursorTo(0);
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
                    int yBeforeRemoving = this.parent.getLine(0).y;
                    this.parent.justSwitchedLineByWordDeletion = true;
                    this.parent.goUpLine();
                    this.parent.getFocusedLine().moveCursorToEnd();
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

            if (!this.parent.isMouseInsideEditorArea()) {
                return false;
            }

            if ((mouseButton == 0) && this.isHovered() && !this.isInMouseHighlightingMode && this.isVisible()) {
                if (!this.parent.isAtLeastOneLineInHighlightMode()) {
                    this.parent.startHighlightLine = this;
                }
                this.isInMouseHighlightingMode = true;
                this.setFocus(true);
                super.mouseClicked(mouseX, mouseY, mouseButton);
                this.getAsAccessor().setShiftPressedFancyMenu(false);
                this.setHighlightPos(this.getCursorPosition());
            } else if ((mouseButton == 0) && !this.isHovered()) {
                //Clear highlighting when left-clicked in another line, etc.
                this.setHighlightPos(this.getCursorPosition());
            }

            if (!this.isInMouseHighlightingMode) {
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

}
