//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules.FormattingRules;
import de.keksuccino.fancymenu.mixin.client.IMixinEditBox;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class TextEditorScreen extends Screen {

    //TODO ganze Zeile markieren, wenn zwischen highlightStart und highlightEnd index
    //TODO bei highlight start und end Zeilen alles markieren, was innerhalb von markiertem bereich liegt, selbst wenn eigentlicher Text kürzer (also alles NACH cursor bei end und alles VOR cursor bei start)
    //TODO Style.withFont() nutzen, um eventuell in editor mit eigener Font zu arbeiten
    //TODO fixen: manchmal funktioniert multi-line highlighting nicht mehr, nachdem große menge an Text (viele zeilen auf einmal) markiert und dann per mausklick resettet wurden

    private static final Logger LOGGER = LogManager.getLogger();

    protected Screen parentScreen;
    protected CharacterFilter characterFilter;
    protected List<TextEditorLine> textFieldLines = new ArrayList<>();
    protected ScrollBar verticalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, 10, 40, 0, 0, 0, 0, UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor());
    protected ScrollBar horizontalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, 40, 10, 0, 0, 0, 0, UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor());
    protected FMContextMenu rightClickContextMenu;
    protected int lastCursorPosSetByUser = 0;
    protected boolean justSwitchedLineByWordDeletion = false;
    protected boolean triggeredFocusedLineWasTooHighInCursorPosMethod = false;
    public int headerHeight = 50;
    public int footerHeight = 50;
    public int borderLeft = 40;
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
    public Color lineNumberTextColorNormal = new Color(91, 92, 94);
    public Color lineNumberTextColorFocused = new Color(137, 147, 150);
    protected int currentLineWidth;
    protected int lastTickFocusedLineIndex = -1;
    protected TextEditorLine startHighlightLine = null;
    protected int startHighlightLineIndex = -1;
    protected int endHighlightLineIndex = -1;
    protected int overriddenTotalScrollHeight = -1;
    protected List<Runnable> lineNumberRenderQueue = new ArrayList<>();
    protected List<TextEditorFormattingRule> formattingRules = new ArrayList<>();
    protected int currentRenderCharacterIndexTotal = 0;

    public TextEditorScreen(Component name, @Nullable Screen parent, @Nullable CharacterFilter characterFilter) {
        super(name);
        this.parentScreen = parent;
        this.characterFilter = characterFilter;
        this.addLine();
        this.getLine(0).setFocus(true);
        this.verticalScrollBar.setScrollWheelAllowed(true);
        this.updateRightClickContextMenu();
        this.formattingRules.addAll(Arrays.asList(FormattingRules.DEFAULT_RULES));
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

    protected void updateRightClickContextMenu() {

        TextEditorLine hoveredLine = this.getHoveredLine();

        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
        }
        this.rightClickContextMenu = new FMContextMenu();

        AdvancedButton cutButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.ui.text_editor.cut"), true, (press) -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.cutHighlightedText());
            this.rightClickContextMenu.closeMenu();
        });
        this.rightClickContextMenu.addContent(cutButton);
        if ((hoveredLine == null) || !hoveredLine.isHighlightedHovered()) {
            cutButton.active = false;
        }

        AdvancedButton copyButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.ui.text_editor.copy"), true, (press) -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlightedText());
            this.rightClickContextMenu.closeMenu();
        });
        this.rightClickContextMenu.addContent(copyButton);
        if ((hoveredLine == null) || !hoveredLine.isHighlightedHovered()) {
            copyButton.active = false;
        }

        AdvancedButton pasteButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.ui.text_editor.paste"), true, (press) -> {
            this.pasteText(Minecraft.getInstance().keyboardHandler.getClipboard());
            this.rightClickContextMenu.closeMenu();
        });
        this.rightClickContextMenu.addContent(pasteButton);

        this.rightClickContextMenu.addSeparator();

        AdvancedButton selectAllButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.ui.text_editor.select_all"), true, (press) -> {
            for (TextEditorLine t : this.textFieldLines) {
                t.setHighlightPos(0);
                t.setCursorPosition(t.getValue().length());
            }
            this.setFocusedLine(this.getLineCount()-1);
            this.startHighlightLineIndex = 0;
            this.endHighlightLineIndex = this.getLineCount()-1;
            this.rightClickContextMenu.closeMenu();
        });
        this.rightClickContextMenu.addContent(selectAllButton);

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

        this.formattingRules.forEach((rule) -> rule.resetRule());
        this.currentRenderCharacterIndexTotal = 0;
        this.lineNumberRenderQueue.clear();
        //Update positions and size of lines and render them
        this.updateLines((line) -> {
            if (line.isInEditorArea()) {
                this.lineNumberRenderQueue.add(() -> this.renderLineNumber(matrix, line));
            }
            line.render(matrix, mouseX, mouseY, partial);
        });

        RenderSystem.disableScissor();

        this.renderLineNumberBackground(matrix, this.borderLeft);

        RenderSystem.enableScissor(0, (int)(win.getHeight() - (sciBottom * scale)), (int)(this.borderLeft * scale), (int)(this.getEditorAreaHeight() * scale));
        for (Runnable r : this.lineNumberRenderQueue) {
            r.run();
        }
        RenderSystem.disableScissor();

        this.verticalScrollBar.render(matrix);
        this.horizontalScrollBar.render(matrix);

        this.lastTickFocusedLineIndex = this.getFocusedLineIndex();
        this.triggeredFocusedLineWasTooHighInCursorPosMethod = false;

        this.renderBorder(matrix);

        UIBase.renderScaledContextMenu(matrix, this.rightClickContextMenu);

        this.tickMouseHighlighting();

    }

    protected void renderLineNumberBackground(PoseStack matrix, int width) {
        fill(matrix, this.getEditorAreaX(), this.getEditorAreaY() - 1, this.getEditorAreaX() - width - 1, this.getEditorAreaY() + this.getEditorAreaHeight() + 1, this.sideBarColor.getRGB());
    }

    protected void renderLineNumber(PoseStack matrix, TextEditorLine line) {
        String lineNumberString = "" + (line.lineIndex+1);
        int lineNumberWidth = this.font.width(lineNumberString);
        this.font.draw(matrix, lineNumberString, this.getEditorAreaX() - 3 - lineNumberWidth, line.getY() + (line.getHeight() / 2) - (this.font.lineHeight / 2), line.isFocused() ? this.lineNumberTextColorFocused.getRGB() : this.lineNumberTextColorNormal.getRGB());
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

        TextEditorLine first = this.startHighlightLine;
        TextEditorLine hovered = this.getHoveredLine();
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
            for (TextEditorLine t : this.textFieldLines) {
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

        TextEditorLine focused = this.getFocusedLine();
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

    protected void updateLines(@Nullable Consumer<TextEditorLine> doAfterEachLineUpdate) {
        try {
            int index = 0;
            for (TextEditorLine line : this.textFieldLines) {
                line.lineIndex = index;
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
        for (TextEditorLine f : this.textFieldLines) {
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

    protected TextEditorLine addLineAtIndex(int index) {
        TextEditorLine f = new TextEditorLine(Minecraft.getInstance().font, 0, 0, 50, this.lineHeight, true, this.characterFilter, this);
        f.setMaxLength(Integer.MAX_VALUE);
        f.lineIndex = index;
        if (index > 0) {
            TextEditorLine before = this.getLine(index-1);
            if (before != null) {
                f.setY(before.getY() + this.lineHeight);
            }
        }
        this.textFieldLines.add(index, f);
        return f;
    }

    protected TextEditorLine addLine() {
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
    public TextEditorLine getLine(int index) {
        return this.textFieldLines.get(index);
    }

    protected void setFocusedLine(int index) {
        if (index <= this.getLineCount()-1) {
            for (TextEditorLine f : this.textFieldLines) {
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
        for (TextEditorLine f : this.textFieldLines) {
            if (f.isFocused()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    @Nullable
    public TextEditorLine getFocusedLine() {
        int index = this.getFocusedLineIndex();
        if (index != -1) {
            return this.getLine(index);
        }
        return null;
    }

    public boolean isLineFocused() {
        return (this.getFocusedLineIndex() > -1);
    }

    @Nullable
    protected TextEditorLine getLineAfter(TextEditorLine line) {
        int index = this.getLineIndex(line);
        if ((index > -1) && (index < (this.getLineCount()-1))) {
            return this.getLine(index+1);
        }
        return null;
    }

    @Nullable
    protected TextEditorLine getLineBefore(TextEditorLine line) {
        int index = this.getLineIndex(line);
        if (index > 0) {
            return this.getLine(index-1);
        }
        return null;
    }

    protected boolean isAtLeastOneLineInHighlightMode() {
        for (TextEditorLine t : this.textFieldLines) {
            if (t.isInMouseHighlightingMode) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    /** Returns the lines between two indexes, EXCLUDING start AND end indexes! **/
    protected List<TextEditorLine> getLinesBetweenIndexes(int startIndex, int endIndex) {
        startIndex = Math.min(Math.max(startIndex, 0), this.textFieldLines.size()-1);
        endIndex = Math.min(Math.max(endIndex, 0), this.textFieldLines.size()-1);
        List<TextEditorLine> l = new ArrayList<>();
        l.addAll(this.textFieldLines.subList(startIndex, endIndex));
        if (!l.isEmpty()) {
            l.remove(0);
        }
        return l;
    }

    @Nullable
    public TextEditorLine getHoveredLine() {
        for (TextEditorLine t : this.textFieldLines) {
            if (t.isHovered()) {
                return t;
            }
        }
        return null;
    }

    public int getLineIndex(TextEditorLine inputBox) {
        return this.textFieldLines.indexOf(inputBox);
    }

    protected void goUpLine() {
        if (this.isLineFocused()) {
            int current = Math.max(0, this.getFocusedLineIndex());
            if (current > 0) {
                TextEditorLine currentLine = this.getLine(current);
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
            TextEditorLine currentLine = this.getLine(current);
            this.setFocusedLine(current+1);
            if (currentLine != null) {
                TextEditorLine nextLine = this.getFocusedLine();
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

    public List<TextEditorLine> getCopyOfLines() {
        List<TextEditorLine> l = new ArrayList<>();
        for (TextEditorLine t : this.textFieldLines) {
            TextEditorLine n = new TextEditorLine(this.font, 0, 0, 0, 0, true, this.characterFilter, this);
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

    public boolean isHighlightedTextHovered() {
        if (this.isTextHighlighted()) {
            List<TextEditorLine> highlightedLines = new ArrayList<>();
            if (this.endHighlightLineIndex <= this.getLineCount()-1) {
                highlightedLines.addAll(this.textFieldLines.subList(this.startHighlightLineIndex, this.endHighlightLineIndex+1));
            }
            for (TextEditorLine t : highlightedLines) {
                if (t.isHighlightedHovered()) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public String getHighlightedText() {
        try {
            if ((this.startHighlightLineIndex != -1) && (this.endHighlightLineIndex != -1)) {
                List<TextEditorLine> lines = new ArrayList<>();
                lines.add(this.getLine(this.startHighlightLineIndex));
                if (this.startHighlightLineIndex != this.endHighlightLineIndex) {
                    lines.addAll(this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex));
                    lines.add(this.getLine(this.endHighlightLineIndex));
                }
                StringBuilder s = new StringBuilder();
                boolean b = false;
                for (TextEditorLine t : lines) {
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
                    TextEditorLine start = this.getLine(this.startHighlightLineIndex);
                    start.insertText("");
                    TextEditorLine end = this.getLine(this.endHighlightLineIndex);
                    end.insertText("");
                    if ((this.endHighlightLineIndex - this.startHighlightLineIndex) > 1) {
                        for (TextEditorLine line : this.getLinesBetweenIndexes(this.startHighlightLineIndex, this.endHighlightLineIndex)) {
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
        LOGGER.info("------------ RESET HIGHLIGHTING");
        this.startHighlightLineIndex = -1;
        this.endHighlightLineIndex = -1;
        for (TextEditorLine t : this.textFieldLines) {
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
                TextEditorLine focusedLine = this.getFocusedLine();
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
                    lines = text.split("\n", -1);
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
        TextEditorLine t = this.getLine(0);
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
        for (TextEditorLine t : this.textFieldLines) {
            if (b) {
                s.append("\n");
            }
            s.append(t.getValue());
            b = true;
        }
        return s.toString();
    }

    /**
     * @return The text BEFORE the cursor or NULL if no line is focused.
     */
    @Nullable
    public String getTextBeforeCursor() {
        if (!this.isLineFocused()) {
            return null;
        }
        int focusedLineIndex = this.getFocusedLineIndex();
        List<TextEditorLine> lines = new ArrayList<>();
        if (focusedLineIndex == 0) {
            lines.add(this.getLine(0));
        } else if (focusedLineIndex > 0) {
            lines.addAll(this.textFieldLines.subList(0, focusedLineIndex+1));
        }
        TextEditorLine lastLine = lines.get(lines.size()-1);
        StringBuilder s = new StringBuilder();
        boolean b = false;
        for (TextEditorLine t : lines) {
            if (b) {
                s.append("\n");
            }
            if (t != lastLine) {
                s.append(t.getValue());
            } else {
                s.append(t.getValue().substring(0, t.getCursorPosition()));
            }
            b = true;
        }
        return s.toString();
    }

    /**
     * @return The text AFTER the cursor or NULL if no line is focused.
     */
    @Nullable
    public String getTextAfterCursor() {
        if (!this.isLineFocused()) {
            return null;
        }
        int focusedLineIndex = this.getFocusedLineIndex();
        List<TextEditorLine> lines = new ArrayList<>();
        if (focusedLineIndex == this.getLineCount()-1) {
            lines.add(this.getLine(this.getLineCount()-1));
        } else if (focusedLineIndex < this.getLineCount()-1) {
            lines.addAll(this.textFieldLines.subList(focusedLineIndex, this.getLineCount()));
        }
        TextEditorLine firstLine = lines.get(0);
        StringBuilder s = new StringBuilder();
        boolean b = false;
        for (TextEditorLine t : lines) {
            if (b) {
                s.append("\n");
            }
            if (t != firstLine) {
                s.append(t.getValue());
            } else {
                s.append(t.getValue().substring(t.getCursorPosition(), t.getValue().length()));
            }
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
                        TextEditorLine focused = this.getFocusedLine();
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
            for (TextEditorLine t : this.textFieldLines) {
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
            if (button == 1) {
                this.rightClickContextMenu.closeMenu();
            }
            if ((button == 0) || (button == 1)) {
                boolean isHighlightedHovered = this.isHighlightedTextHovered();
                TextEditorLine hoveredLine = this.getHoveredLine();
                if (!this.rightClickContextMenu.isOpen()) {
                    if ((button == 0) || !isHighlightedHovered) {
                        this.resetHighlighting();
                    }
                    if (hoveredLine == null) {
                        TextEditorLine focus = this.getLine(this.getLineCount()-1);
                        for (TextEditorLine t : this.textFieldLines) {
                            if ((MouseInput.getMouseY() >= t.y) && (MouseInput.getMouseY() <= t.y + t.getHeight())) {
                                focus = t;
                                break;
                            }
                        }
                        this.setFocusedLine(this.getLineIndex(focus));
                        this.getFocusedLine().moveCursorToEnd();
                        this.correctYScroll(0);
                    } else if ((button == 1) && !isHighlightedHovered) {
                        //Focus line in case it is right-clicked
                        this.setFocusedLine(this.getLineIndex(hoveredLine));
                        //Set cursor in case line is right-clicked
                        String s = this.font.plainSubstrByWidth(hoveredLine.getValue().substring(hoveredLine.getAsAccessor().getDisplayPosFancyMenu()), hoveredLine.getInnerWidth());
                        hoveredLine.moveCursorTo(this.font.plainSubstrByWidth(s, MouseInput.getMouseX() - hoveredLine.getX()).length() + hoveredLine.getAsAccessor().getDisplayPosFancyMenu());
                    }
                }
                if (button == 1) {
                    this.updateRightClickContextMenu();
                    UIBase.openScaledContextMenuAtMouse(this.rightClickContextMenu);
                } else if (this.rightClickContextMenu.isOpen() && !this.rightClickContextMenu.isHoveredOrFocused()) {
                    this.rightClickContextMenu.closeMenu();
                    //Call mouseClicked of lines after closing the menu, so the focused line and cursor pos gets updated
                    this.textFieldLines.forEach((line) -> {
                        line.mouseClicked(mouseX, mouseY, button);
                    });
                    //Call mouseClicked of editor again to do everything that would happen when clicked without the context menu opened
                    this.mouseClicked(mouseX, mouseY, button);
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

    protected void correctXScroll(TextEditorLine calledIn) {

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

}