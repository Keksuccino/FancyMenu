//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.mixin.client.IMixinEditBox;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TextEditorScreen extends Screen {

    //TODO scroll bars auf größe von editor area anpassen

    //TODO wenn Leerzeichen an Anfang von Zeile, neu geaddete Zeile gleiche Anzahl Leerzeichen adden

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
    public int lineHeight = 20;
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

        int editorAreaWidth = (this.width - this.borderRight) - this.borderLeft;
        int editorAreaHeight = (this.height - this.footerHeight) - this.headerHeight;

        //Reset scrolls if content fits editor area
        //TODO nach correctScrollX methode verschieben
        if (this.currentLineWidth <= editorAreaWidth) {
            this.horizontalScrollBar.setScroll(0.0F);
        }

        this.justSwitchedLineByWordDeletion = false;

        this.renderBackground(matrix);

        this.updateCurrentLineWidth();

        //TODO scissors aktivieren
//        Window win = Minecraft.getInstance().getWindow();
//        double scale = win.getGuiScale();
//        int sciBottom = this.height - this.footerHeight;
//        RenderSystem.enableScissor((int)(this.borderLeft * scale), (int)(win.getHeight() - (sciBottom * scale)), (int)(editorAreaWidth * scale), (int)(editorAreaHeight * scale));

        this.updateLines((line) -> {
            line.render(matrix, mouseX, mouseY, partial);
        });

//        RenderSystem.disableScissor();

        this.verticalScrollBar.render(matrix);
        this.horizontalScrollBar.render(matrix);

        super.render(matrix, mouseX, mouseY, partial);

        this.lastTickFocusedLineIndex = this.getFocusedLineIndex();
        this.triggeredFocusedLineWasTooHighInCursorPosMethod = false;

        this.renderBorder(matrix);

        this.tickMouseHighlighting();

    }

    protected void renderBorder(PoseStack matrix) {
        //top
        fill(matrix, this.borderLeft - 1, this.headerHeight - 1, this.width - this.borderRight + 1, this.headerHeight, Color.RED.getRGB());
        //left
        fill(matrix, this.borderLeft - 1, this.headerHeight, this.borderLeft, this.height - this.footerHeight, Color.RED.getRGB());
        //right
        fill(matrix, this.width - this.borderRight, this.headerHeight, this.width - this.borderRight+1, this.height - this.footerHeight, Color.RED.getRGB());
        //down
        fill(matrix, this.borderLeft - 1, this.height - this.footerHeight, this.width - this.borderRight + 1, this.height - this.footerHeight + 1, Color.RED.getRGB());
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
            LOGGER.info("START INDEX: " + this.startHighlightLineIndex + " | END INDEX: " + this.endHighlightLineIndex);

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
                LOGGER.info("START INDEX: " + this.startHighlightLineIndex + " | END INDEX: " + this.endHighlightLineIndex);
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
                this.startHighlightLineIndex = -1;
                this.endHighlightLineIndex = -1;
                LOGGER.info("RESETTING START END INDEXES!");
            }
        }

    }

    protected void updateLines(@Nullable Consumer<TextEditorInputBox> doAfterEachLineUpdate) {
        try {
            //Update positions and size of lines and render them
            int index = 0;
            for (TextEditorInputBox line : this.textFieldLines) {
                line.y = this.headerHeight + (this.lineHeight * index) + this.getLineRenderOffsetY();
                line.x = this.borderLeft + this.getLineRenderOffsetX();
                line.setWidth(this.currentLineWidth);
                line.setHeight(this.lineHeight);
                ((IMixinEditBox)line).setDisplayPosFancyMenu(0);
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
            int textWidth = Minecraft.getInstance().font.width(f.getValue());
            if (textWidth > longestTextWidth) {
                longestTextWidth = textWidth;
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
                LOGGER.info("addLineAtIndex: NEW LINE Y: " + f.getY());
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
        if (this.isLineFocused()) {
            return this.getLine(this.getFocusedLineIndex());
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
//            TextEditorInputBox lastLine = this.getLine(this.getLineCount()-1);
//            int lastLineYBeforeAdding = lastLine.getY();
            LOGGER.info("goDownLine: OLD LINE Y: " + this.getLine(current).getY());
            if (isNewLine) {
//                LOGGER.info("goDownLine -----------------------");
//                LOGGER.info("goDownLine: LAST LINE Y BEFORE ADDING: " + lastLineYBeforeAdding);
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
            //TODO remove debug
//            if (isNewLine) {
//                this.updateLines(null);
//                LOGGER.info("goDownLine: LAST LINE Y AFTER ADDING AND UPDATING: " + lastLine.getY());
//                LOGGER.info("goDownLine: DIFF BEFORE AND AFTER ADDING: " + (Math.max(lastLineYBeforeAdding, lastLine.getY()) - Math.min(lastLineYBeforeAdding, lastLine.getY())));
//            }
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

    @NotNull
    public String getHighlightedText() {
        LOGGER.info("CALL getHighlightedText");
        try {
            if ((this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1)) {
                LOGGER.info("----- trigger 1");
                List<TextEditorInputBox> lines = new ArrayList<>();
                lines.add(this.getLine(this.startHighlightLineIndex));
                if (this.startHighlightLineIndex != this.endHighlightLineIndex) {
                    LOGGER.info("----- trigger 2");
                    lines.addAll(this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex));
                    lines.add(this.getLine(this.endHighlightLineIndex));
                }
                StringBuilder s = new StringBuilder();
                for (TextEditorInputBox t : lines) {
                    if (!s.toString().equals("")) {
                        s.append("\n");
                    }
                    s.append(t.getHighlighted());
                }
                String ret = s.toString();
                LOGGER.info("----- trigger 3: " + ret);
                return ret;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.info("----- trigger 4");
        return "";
    }

    @NotNull
    public String cutHighlightedText() {
        String highlighted = this.getHighlightedText();
        this.deleteHighlightedText();
        return highlighted;
    }

    public void deleteHighlightedText() {
        try {
            if ((this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1)) {
                if (this.startHighlightLineIndex == this.endHighlightLineIndex) {
                    this.getLine(this.startHighlightLineIndex).insertText("");
                } else {
                    this.getLine(this.startHighlightLineIndex).insertText("");
                    TextEditorInputBox end = this.getLine(this.endHighlightLineIndex);
                    end.insertText("");
                    if ((this.endHighlightLineIndex - this.startHighlightLineIndex) > 1) {
                        this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex).forEach((line) -> {
                            this.removeLineAtIndex(this.getLineIndex(line));
                        });
                    }
                    if (end.getValue().length() == 0) {
                        this.removeLineAtIndex(this.getLineIndex(end));
                    }
                    this.setFocusedLine(this.startHighlightLineIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.startHighlightLineIndex = -1;
        this.endHighlightLineIndex = -1;
    }

    public void pasteText(String text) {
        //TODO support für ersetzen von markiertem text adden (in input box class blocken)
        try {
            if (this.isLineFocused() && (text != null)) {
                String[] lines = new String[]{text};
                if (text.contains("\n")) {
                    lines = text.split("\n");
                }
                if (lines.length == 1) {
                    this.getFocusedLine().insertText(lines[0]);
                } else if (lines.length > 1) {
                    int index = -1;
                    for (String s : lines) {
                        if (index == -1) {
                            index = this.getFocusedLineIndex();
                        } else {
                            this.addLineAtIndex(index);
                        }
                        this.getLine(index).insertText(s);
                        index++;
                    }
                    this.setFocusedLine(index-1);
                    this.getLine(index-1).moveCursorToEnd();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.startHighlightLineIndex = -1;
        this.endHighlightLineIndex = -1;
    }

    @Override
    public boolean keyPressed(int keycode, int i1, int i2) {

        //TODO wenn CTRL + V wenn mouseHighlighting -> hightlighted clearen + leere zeilen löschen + kopiertes einfügen

        //ENTER
        if (keycode == 257) {
            if (!this.isInMouseHighlightingMode()) {
                if (this.isLineFocused()) {
                    this.resetHighlighting();
                    this.goDownLine(true);
                    this.correctYScroll(true, false);
                }
            }
            return true;
        }
        //ARROW UP
        if (keycode == InputConstants.KEY_UP) {
            if (!this.isInMouseHighlightingMode()) {
                this.resetHighlighting();
                this.goUpLine();
                this.correctYScroll(false, false);
            }
            return true;
        }
        //ARROW DOWN
        if (keycode == InputConstants.KEY_DOWN) {
            if (!this.isInMouseHighlightingMode()) {
                this.resetHighlighting();
                this.goDownLine(false);
                this.correctYScroll(false, false);
            }
            return true;
        }

        //BACKSPACE
        if (keycode == InputConstants.KEY_BACKSPACE) {
            if (!this.isInMouseHighlightingMode()) {
                if (!this.getHighlightedText().equals("")) {
                    this.deleteHighlightedText();
                } else {
                    if (this.isLineFocused()) {
                        TextEditorInputBox focused = this.getFocusedLine();
                        focused.getAsAccessor().setShiftPressedFancyMenu(false);
                        focused.getAsAccessor().invokeDeleteTextFancyMenu(-1);
                        focused.getAsAccessor().setShiftPressedFancyMenu(Screen.hasShiftDown());
                    }
                }
                this.startHighlightLineIndex = -1;
                this.endHighlightLineIndex = -1;
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
            this.startHighlightLineIndex = -1;
            this.endHighlightLineIndex = -1;
            return true;
        }
        //Reset highlighting when pressing left/right arrow keys while NOT in mouse-highlighting mod
        if (((keycode == InputConstants.KEY_RIGHT) || (keycode == InputConstants.KEY_LEFT)) && !this.isInMouseHighlightingMode()) {
            this.resetHighlighting();
            return true;
        }

        return super.keyPressed(keycode, i1, i2);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (this.isMouseInsideEditorArea()) {
            if (button == 0) {
                LOGGER.info("RESETTING START END HIGHLIGHT INDEXES!");
                this.startHighlightLineIndex = -1;
                this.endHighlightLineIndex = -1;
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
//        int totalLineHeight = this.getTotalScrollHeight();
//        float f = (float)Math.max(0, ((lineIndex + 1) * this.lineHeight) - this.lineHeight) / (float)totalLineHeight;
//        if (!top) {
//            f -= ((float)Math.max(0, this.getEditorAreaHeight() - this.lineHeight) / (float)totalLineHeight);
//        }
//        this.verticalScrollBar.setScroll(f);
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

    protected void correctYScroll(boolean lineAdded, boolean lineRemoved) {

        //TODO callen, wenn Zeilen durch highlighting gelöscht werden (mehrere) + support für removen von mehr als einer Zeile auf einmal adden
        //TODO callen, wenn Zeilen durch highlighting gelöscht werden (mehrere) + support für removen von mehr als einer Zeile auf einmal adden
        //TODO callen, wenn Zeilen durch highlighting gelöscht werden (mehrere) + support für removen von mehr als einer Zeile auf einmal adden
        //TODO callen, wenn Zeilen durch highlighting gelöscht werden (mehrere) + support für removen von mehr als einer Zeile auf einmal adden
        //TODO callen, wenn Zeilen durch highlighting gelöscht werden (mehrere) + support für removen von mehr als einer Zeile auf einmal adden
        //TODO callen, wenn Zeilen durch highlighting gelöscht werden (mehrere) + support für removen von mehr als einer Zeile auf einmal adden

        //Don't fix scroll if in mouse-highlighting mode or no line is focused
        if (this.isInMouseHighlightingMode() || !this.isLineFocused()) {
            return;
        }

        int minY = this.getEditorAreaY();
        int maxY = this.getEditorAreaY() + this.getEditorAreaHeight();
        int currentLineY = this.getFocusedLine().getY();

//        LOGGER.info("---------------------------------------------------------------");
//        LOGGER.info("correctYScroll: MIN Y: " + minY);
//        LOGGER.info("correctYScroll: MAX Y: " + maxY);
//        LOGGER.info("correctYScroll: CURRENT LINE Y: " + currentLineY);

        LOGGER.info("correctYScroll: CURRENT LINE Y: " + this.getFocusedLine().getY());

        if (currentLineY < minY) {
//            LOGGER.info("correctYScroll: LINE Y < MIN Y --------");
            this.scrollToLine(this.getFocusedLineIndex(), false);
        } else if ((currentLineY + this.lineHeight) > maxY) {
//            LOGGER.info("correctYScroll: LINE Y > MAX Y --------");
            this.scrollToLine(this.getFocusedLineIndex(), true);
        } else if (lineAdded || lineRemoved) {
//            LOGGER.info("correctYScroll: LINE ADDED/REMOVED");
            this.overriddenTotalScrollHeight = -1;
            if (lineAdded) {
                this.overriddenTotalScrollHeight = this.getTotalScrollHeight() - this.lineHeight;
            } else if (lineRemoved) {
                this.overriddenTotalScrollHeight = this.getTotalScrollHeight() + this.lineHeight;
            }
            this.updateLines(null);
            this.overriddenTotalScrollHeight = -1;
            int diffToTop = Math.max(0, this.getFocusedLine().getY() - this.getEditorAreaY());
            this.scrollToLine(this.getFocusedLineIndex(), -diffToTop);
            this.correctYScroll(false, false);
        }

        if (this.getTotalLineHeight() <= this.getEditorAreaHeight()) {
            this.verticalScrollBar.setScroll(0.0F);
        }

    }

    protected void fixXYScroll(TextEditorInputBox calledIn) {

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

    public boolean isInMouseHighlightingMode() {
        return MouseInput.isLeftMouseDown() && (this.startHighlightLine != null);
    }

    public void resetHighlighting() {
        this.startHighlightLineIndex = -1;
        this.endHighlightLineIndex = -1;
        for (TextEditorInputBox t : this.textFieldLines) {
            t.setHighlightPos(t.getCursorPosition());
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

        public TextEditorInputBox(Font font, int x, int y, int width, int height, boolean handleSelf, @Nullable CharacterFilter characterFilter, TextEditorScreen parent) {
            super(font, x, y, width, height, handleSelf, characterFilter);
            this.parent = parent;
        }

        @Override
        public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {

            super.render(p_93657_, p_93658_, p_93659_, p_93660_);

            this.lastTickValue = this.getValue();

        }

        public int getActualHeight() {
            int h = this.height;
            if (((IMixinEditBox)this).getBorderedFancyMenu()) {
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

            this.parent.fixXYScroll(this);
            this.cursorPositionTicked = true;

        }

        @Override
        public void onTick() {

            this.cursorPositionTicked = false;

            if (!MouseInput.isLeftMouseDown() && this.isInMouseHighlightingMode) {
                this.isInMouseHighlightingMode = false;
            }

            super.onTick();

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
                        this.parent.correctYScroll(false, true);
                    }
                } else {
                    super.deleteChars(i);
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

            if (!this.parent.isMouseInsideEditorArea()) {
                return false;
            }

            //TODO remove debug
            if (this.parent.getFocusedLine() == this) {
                LOGGER.info("mouseClicked: focused Line Y: " + this.y);
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

    }

}
