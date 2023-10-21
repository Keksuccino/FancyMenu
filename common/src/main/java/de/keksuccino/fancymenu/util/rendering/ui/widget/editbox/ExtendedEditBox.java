package de.keksuccino.fancymenu.util.rendering.ui.widget.editbox;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinEditBox;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Color;

@SuppressWarnings("unused")
public class ExtendedEditBox extends EditBox implements UniqueWidget, NavigatableWidget {

    //TODO add text prefix and suffix (non-editable)

    protected CharacterFilter characterFilter;
    protected CharacterRenderFormatter characterRenderFormatter;
    protected DrawableColor backgroundColor = DrawableColor.of(new Color(0, 0, 0));
    protected DrawableColor borderNormalColor = DrawableColor.of(new Color(-6250336));
    protected DrawableColor borderFocusedColor = DrawableColor.of(new Color(255, 255, 255));
    protected DrawableColor textColor = DrawableColor.of(new Color(14737632));
    protected DrawableColor textColorUneditable = DrawableColor.of(new Color(7368816));
    protected DrawableColor suggestionTextColor = DrawableColor.of(new Color(-8355712));
    protected boolean textShadow = true;
    protected final Font font;
    @Nullable
    protected String identifier;
    protected boolean focusable = true;
    protected boolean navigatable = true;
    protected boolean canConsumeUserInput = true;
    @Nullable
    protected String inputPrefix;
    @Nullable
    protected String inputSuffix;

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
                if (this.textShadow) {
                    this.font.drawShadow(pose, hint, (float) textXAfterCursor, (float) textY, textColor);
                } else {
                    this.font.draw(pose, hint, (float) textXAfterCursor, (float) textY, textColor);
                }
            }

            if (!renderSmallCursor && access.getSuggestionFancyMenu() != null) {
                if (this.textShadow) {
                    this.font.drawShadow(pose, access.getSuggestionFancyMenu(), (float) (finalTextXAfterCursor - 1), (float) textY, this.suggestionTextColor.getColorInt());
                } else {
                    this.font.draw(pose, access.getSuggestionFancyMenu(), (float)finalTextXAfterCursor, (float) textY, this.suggestionTextColor.getColorInt());
                }
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

    public void setHeight(int height) {
        this.height = height;
    }

    public int getDisplayPosition() {
        return ((IMixinEditBox)this).getDisplayPosFancyMenu();
    }

    public void setDisplayPosition(int position) {
        ((IMixinEditBox)this).setDisplayPosFancyMenu(position);
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

    @NotNull
    public DrawableColor getSuggestionTextColor() {
        return this.suggestionTextColor;
    }

    public ExtendedEditBox setSuggestionTextColor(@NotNull DrawableColor suggestionTextColor) {
        this.suggestionTextColor = suggestionTextColor;
        return this;
    }

    public boolean canConsumeUserInput() {
        return this.canConsumeUserInput;
    }

    public ExtendedEditBox setCanConsumeUserInput(boolean canConsumeUserInput) {
        this.canConsumeUserInput = canConsumeUserInput;
        return this;
    }

    public @Nullable String getInputPrefix() {
        return inputPrefix;
    }

    public ExtendedEditBox setInputPrefix(@Nullable String inputPrefix) {
        this.inputPrefix = inputPrefix;
        this.setValue(this.getValue());
        return this;
    }

    public @Nullable String getInputSuffix() {
        return inputSuffix;
    }

    public ExtendedEditBox setInputSuffix(@Nullable String inputSuffix) {
        this.inputSuffix = inputSuffix;
        this.setValue(this.getValue());
        return this;
    }

    public ExtendedEditBox applyInputPrefixSuffixCharacterRenderFormatter() {
        this.setCharacterRenderFormatter((editBox, component, characterIndex, character, visiblePartOfLine, fullLine) -> {
            if ((this.inputSuffix != null) && (characterIndex >= Math.max(0, (editBox.getValue().length() - this.inputSuffix.length())-1))) {
                component.withStyle(Style.EMPTY.withColor(this.getTextColorUneditable().getColorInt()));
            }
            if ((this.inputPrefix != null) && (characterIndex < this.inputPrefix.length())) {
                component.withStyle(Style.EMPTY.withColor(this.getTextColorUneditable().getColorInt()));
            }
            return component;
        });
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
    public void setCursorPosition(int pos) {
        pos = Mth.clamp(pos, 0, this.getValue().length());
        if ((this.inputPrefix != null) && (pos < this.inputPrefix.length())) pos = this.inputPrefix.length();
        if ((this.inputSuffix != null) && (pos >= (this.getValue().length() - this.inputSuffix.length()))) pos = (this.getValue().length() - this.inputSuffix.length());
        super.setCursorPosition(pos);
    }

    @Override
    public void setHighlightPos(int pos) {
        pos = Mth.clamp(pos, 0, this.getValue().length());
        if ((this.inputPrefix != null) && (pos < this.inputPrefix.length())) pos = this.inputPrefix.length();
        if ((this.inputSuffix != null) && (pos >= (this.getValue().length() - this.inputSuffix.length()))) pos = (this.getValue().length() - this.inputSuffix.length());
        super.setHighlightPos(pos);
    }

    @Override
    public void setValue(@NotNull String value) {
        if ((this.inputPrefix != null) && !value.startsWith(this.inputPrefix)) value = this.inputPrefix + value;
        if ((this.inputSuffix != null) && !value.endsWith(this.inputSuffix)) value = value + this.inputSuffix;
        super.setValue(value);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (this.inputSuffix != null) {
            if (this.getCursorPosition() >= Math.max(0, this.getValue().length() - this.inputSuffix.length())) {
                return false;
            }
        }
        if (this.inputPrefix != null) {
            if (this.getCursorPosition() < this.inputPrefix.length()) {
                return false;
            }
        }
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

    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        if (!this.canConsumeUserInput) return false;
        boolean isNotArrowKeys = (keycode != InputConstants.KEY_LEFT) && (keycode != InputConstants.KEY_RIGHT) && (keycode != InputConstants.KEY_UP) && (keycode != InputConstants.KEY_DOWN);
        //If select all, only select parts that are not prefix or suffix
        if (Screen.isSelectAll(keycode) && ((this.inputPrefix != null) || (this.inputSuffix != null))) {
            if (this.inputSuffix != null) {
                this.moveCursorTo(this.getValue().length() - this.inputSuffix.length());
            } else {
                this.moveCursorToEnd();
            }
            this.setHighlightPos((this.inputPrefix != null) ? this.inputPrefix.length() : 0);
            return true;
        }
        if (this.inputSuffix != null) {
            if (Math.max(0, this.getCursorPosition()) >= Math.max(0, this.getValue().length() - this.inputSuffix.length())) {
                if (isNotArrowKeys) return false;
            }
        }
        if (this.inputPrefix != null) {
            if (Math.max(0, this.getCursorPosition()) < this.inputPrefix.length()) {
                if (isNotArrowKeys) return false;
            }
        }
        return super.keyPressed(keycode, scancode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.canConsumeUserInput) return false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    //This is to make the edit box work in FocuslessEventHandlers
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public ExtendedEditBox setWidgetIdentifierFancyMenu(@Nullable String identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public @Nullable String getWidgetIdentifierFancyMenu() {
        return this.identifier;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!this.focusable) {
            super.setFocused(false);
            return;
        }
        super.setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        if (!this.focusable) return false;
        return super.isFocused();
    }

    @Override
    public boolean isFocusable() {
        return this.focusable;
    }

    @Override
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    @Override
    public boolean isNavigatable() {
        return this.navigatable;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        this.navigatable = navigatable;
    }

    @FunctionalInterface
    public interface CharacterRenderFormatter {
        @NotNull MutableComponent formatComponent(@NotNull ExtendedEditBox editBox, @NotNull MutableComponent component, int characterIndex, char character, @NotNull String visiblePartOfLine, @NotNull String fullLine);
    }

}
