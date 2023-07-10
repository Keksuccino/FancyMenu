package de.keksuccino.fancymenu.util.rendering.ui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinEditBox;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Color;

@SuppressWarnings("unused")
public class ExtendedEditBox extends EditBox {

    protected CharacterFilter characterFilter;
    protected CharacterRenderFormatter characterRenderFormatter;
    protected DrawableColor backgroundColor = DrawableColor.of(new Color(0, 0, 0));
    protected DrawableColor borderNormalColor = DrawableColor.of(new Color(-6250336));
    protected DrawableColor borderFocusedColor = DrawableColor.of(new Color(255, 255, 255));
    protected DrawableColor textColor = DrawableColor.of(new Color(14737632));
    protected DrawableColor textColorUneditable = DrawableColor.of(new Color(7368816));
    protected boolean textShadow = true;
    protected final Font font;

    public ExtendedEditBox(Font font, int x, int y, int width, int height, Component hint) {
        super(font, x, y, width, height, hint);
        this.font = font;
    }

    public ExtendedEditBox(Font font, int x, int y, int width, int height, @Nullable EditBox editBox, Component hint) {
        super(font, x, y, width, height, editBox, hint);
        this.font = font;
    }

    @Override
    public void renderWidget(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        IMixinEditBox access = ((IMixinEditBox)this);
        boolean bordered = access.getBorderedFancyMenu();

        if (this.isVisible()) {

            fill(pose, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, this.backgroundColor.getColorInt());
            if (bordered) {
                int borderColor = this.isFocused() ? this.borderFocusedColor.getColorInt() : this.borderNormalColor.getColorInt();
//                fill(pose, this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, borderColor);
                UIBase.renderBorder(pose, this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, 1, borderColor, true, true, true, true);
            }

            int textColor = access.getIsEditableFancyMenu() ? this.textColor.getColorInt() : this.textColorUneditable.getColorInt();
            int cursorPos = this.getCursorPosition() - access.getDisplayPosFancyMenu();
            int highlightPos = access.getHighlightPosFancyMenu() - access.getDisplayPosFancyMenu();
            String text = this.font.plainSubstrByWidth(this.getValue().substring(access.getDisplayPosFancyMenu()), this.getInnerWidth());
            boolean isCursorInsideVisibleText = cursorPos >= 0 && cursorPos <= text.length();
            boolean isCursorVisible = this.isFocused() && access.getFrameFancyMenu() / 6 % 2 == 0 && isCursorInsideVisibleText;
            int textX = bordered ? this.getX() + 4 : this.getX();
            int textY = bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
            int textXAfterCursor = textX;
            if (highlightPos > text.length()) {
                highlightPos = text.length();
            }

            int textCharacterRenderIndex = access.getDisplayPosFancyMenu();

            if (!text.isEmpty()) {
                String textBeforeCursor = isCursorInsideVisibleText ? text.substring(0, cursorPos) : text;
                MutableComponent beforeCursorComp = Component.literal("");
                if (this.characterRenderFormatter == null) {
                    beforeCursorComp = Component.literal(textBeforeCursor);
                } else {
                    for (char c : textBeforeCursor.toCharArray()) {
                        MutableComponent comp = this.characterRenderFormatter.formatComponent(this, Component.literal(String.valueOf(c)), textCharacterRenderIndex, c, text, this.getValue());
                        beforeCursorComp.append(comp);
                        textCharacterRenderIndex++;
                    }
                }
                if (this.textShadow) {
                    textXAfterCursor = this.font.drawShadow(pose, beforeCursorComp, (float)textX, (float)textY, textColor);
                } else {
                    textXAfterCursor = this.font.draw(pose, beforeCursorComp, (float)textX, (float)textY, textColor);
                }
            }

            boolean renderSmallCursor = (this.getCursorPosition() < this.getValue().length()) || (this.getValue().length() >= access.getMaxLengthFancyMenu());
            int finalTextXAfterCursor = textXAfterCursor;
            if (!isCursorInsideVisibleText) {
                finalTextXAfterCursor = (cursorPos > 0) ? (textX + this.width) : textX;
            } else if (renderSmallCursor) {
                finalTextXAfterCursor = textXAfterCursor - 1;
                if (this.textShadow) --textXAfterCursor;
            }

            if (!text.isEmpty() && isCursorInsideVisibleText && cursorPos < text.length()) {
                String textAfterCursor = text.substring(cursorPos);
                MutableComponent afterCursorComp = Component.literal("");
                if (this.characterRenderFormatter == null) {
                    afterCursorComp = Component.literal(textAfterCursor);
                } else {
                    for (char c : textAfterCursor.toCharArray()) {
                        MutableComponent comp = this.characterRenderFormatter.formatComponent(this, Component.literal(String.valueOf(c)), textCharacterRenderIndex, c, text, this.getValue());
                        afterCursorComp.append(comp);
                        textCharacterRenderIndex++;
                    }
                }
                if (this.textShadow) {
                    this.font.drawShadow(pose, afterCursorComp, (float)textXAfterCursor, (float)textY, textColor);
                } else {
                    this.font.draw(pose, afterCursorComp, (float)textXAfterCursor, (float)textY, textColor);
                }
            }

            Component hint = access.getHintFancyMenu();
            if ((hint != null) && text.isEmpty() && !this.isFocused()) {
                this.font.drawShadow(pose, hint, (float)textXAfterCursor, (float)textY, textColor);
            }

            if (!renderSmallCursor && access.getSuggestionFancyMenu() != null) {
                this.font.drawShadow(pose, access.getSuggestionFancyMenu(), (float)(finalTextXAfterCursor - 1), (float)textY, -8355712);
            }

            if (isCursorVisible) {
                if (renderSmallCursor) {
                    fill(pose, finalTextXAfterCursor, textY - 1, finalTextXAfterCursor + 1, textY + 1 + 9, textColor);
                } else {
                    fill(pose, finalTextXAfterCursor, textY + this.font.lineHeight - 2, finalTextXAfterCursor + 5, textY + this.font.lineHeight - 1, textColor);
                }
            }

            if (highlightPos != cursorPos) {
                int l1 = textX + this.font.width(text.substring(0, highlightPos));
                access.invokeRenderHighlightFancyMenu(pose, finalTextXAfterCursor, textY - 1, l1 - 1, textY + 1 + 9);
            }

        }

    }

    public @Nullable CharacterFilter getCharacterFilter() {
        return this.characterFilter;
    }

    public ExtendedEditBox setCharacterFilter(@Nullable CharacterFilter characterFilter) {
        this.characterFilter = characterFilter;
        return this;
    }

    public boolean hasTextShadow() {
        return this.textShadow;
    }

    public ExtendedEditBox setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    @NotNull
    public DrawableColor getBackgroundColor() {
        return this.backgroundColor;
    }

    public ExtendedEditBox setBackgroundColor(@NotNull DrawableColor backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    @NotNull
    public DrawableColor getBorderNormalColor() {
        return borderNormalColor;
    }

    public ExtendedEditBox setBorderNormalColor(@NotNull DrawableColor borderNormalColor) {
        this.borderNormalColor = borderNormalColor;
        return this;
    }

    @NotNull
    public DrawableColor getBorderFocusedColor() {
        return this.borderFocusedColor;
    }

    public ExtendedEditBox setBorderFocusedColor(@NotNull DrawableColor borderFocusedColor) {
        this.borderFocusedColor = borderFocusedColor;
        return this;
    }

    @Nullable
    public ExtendedEditBox.CharacterRenderFormatter getCharacterRenderFormatter() {
        return this.characterRenderFormatter;
    }

    public ExtendedEditBox setCharacterRenderFormatter(@Nullable ExtendedEditBox.CharacterRenderFormatter characterRenderFormatter) {
        this.characterRenderFormatter = characterRenderFormatter;
        return this;
    }

    @NotNull
    public DrawableColor getTextColor() {
        return this.textColor;
    }

    public ExtendedEditBox setTextColor(@NotNull DrawableColor textColor) {
        this.textColor = textColor;
        return this;
    }

    @NotNull
    public DrawableColor getTextColorUneditable() {
        return this.textColorUneditable;
    }

    public ExtendedEditBox setTextColorUneditable(@NotNull DrawableColor textColorUneditable) {
        this.textColorUneditable = textColorUneditable;
        return this;
    }

    @Deprecated
    @Override
    public void setTextColor(int color) {
        this.textColor = DrawableColor.of(new Color(color));
    }

    @Deprecated
    @Override
    public void setTextColorUneditable(int color) {
        this.textColorUneditable = DrawableColor.of(new Color(color));
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if ((this.characterFilter != null) && !this.characterFilter.isAllowedChar(character)) {
            return false;
        }
        return super.charTyped(character, modifiers);
    }

    @Override
    public void insertText(@NotNull String textToWrite) {
        if (this.characterFilter != null) {
            textToWrite = this.characterFilter.filterForAllowedChars(textToWrite);
        }
        super.insertText(textToWrite);
    }

    @FunctionalInterface
    public interface CharacterRenderFormatter {
        @NotNull MutableComponent formatComponent(@NotNull ExtendedEditBox editBox, @NotNull MutableComponent component, int characterIndex, char character, @NotNull String visiblePartOfLine, @NotNull String fullLine);
    }

}