package de.keksuccino.fancymenu.util.rendering.ui.widget.editbox;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinEditBox;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Color;
import java.util.function.Supplier;

/**
 * An extended version of Minecraft's EditBox with custom styling and functionality.
 * <p>
 * NOTE: This class relies on {@link IMixinEditBox} to access private members of the vanilla {@link EditBox}.
 * Ensure that the mixin accessors/invokers are targeting the correct field and method names for the
 * current Minecraft version.
 */
@SuppressWarnings("unused")
public class ExtendedEditBox extends EditBox implements UniqueWidget, NavigatableWidget, FancyMenuWidget {

    private static final Logger LOGGER = LogManager.getLogger();

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
    protected boolean deleteAllAllowed = true;
    @Nullable
    protected ConsumingSupplier<ExtendedEditBox, Boolean> isActiveSupplier = null;
    @Nullable
    protected ConsumingSupplier<ExtendedEditBox, Boolean> isVisibleSupplier = null;
    @Nullable
    protected Supplier<Tooltip> customTooltip;
    protected boolean forceDefaultTooltipStyle = true;
    @Nullable
    protected ConsumingSupplier<ExtendedEditBox, Component> hintFancymenu = null;

    public ExtendedEditBox(Font font, int x, int y, int width, int height, Component narrationMessage) {
        super(font, x, y, width, height, narrationMessage);
        this.font = font;
    }

    public ExtendedEditBox(Font font, int x, int y, int width, int height, @Nullable EditBox editBox, Component narrationMessage) {
        super(font, x, y, width, height, editBox, narrationMessage);
        this.font = font;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        IMixinEditBox access = ((IMixinEditBox)this);
        boolean bordered = access.getBorderedFancyMenu();

        if (this.isVisible()) {
            // Background and Border Rendering (Custom)
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, this.backgroundColor.getColorInt());
            if (bordered) {
                int borderColor = this.isFocused() ? this.borderFocusedColor.getColorInt() : this.borderNormalColor.getColorInt();
                // Assuming UIBase.renderBorder is compatible with GuiGraphics
                UIBase.renderBorder(graphics, this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, 1, borderColor, true, true, true, true);
            }

            // Text and Cursor Rendering
            int textColor = access.getIsEditableFancyMenu() ? this.textColor.getColorInt() : this.textColorUneditable.getColorInt();
            int cursorPos = this.getCursorPosition() - access.getDisplayPosFancyMenu();
            int highlightPos = access.getHighlightPosFancyMenu() - access.getDisplayPosFancyMenu();
            String text = this.font.plainSubstrByWidth(this.getValue().substring(access.getDisplayPosFancyMenu()), this.getInnerWidth());
            boolean isCursorInsideVisibleText = cursorPos >= 0 && cursorPos <= text.length();
            boolean isCursorVisible = this.isFocused() && (Util.getMillis() - access.getFocusedTimeFancyMenu()) / 300L % 2L == 0L && isCursorInsideVisibleText;
            int textX = bordered ? this.getX() + 4 : this.getX();
            int textY = bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
            int textXAfterCursor = textX;
            if (highlightPos > text.length()) {
                highlightPos = text.length();
            }

            int textCharacterRenderIndex = access.getDisplayPosFancyMenu();

            // Render text before the cursor
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

                graphics.drawString(this.font, beforeCursorComp, textX, textY, textColor, this.textShadow);
                textXAfterCursor = textX + this.font.width(beforeCursorComp);

            }

            // Calculate cursor's final render position
            boolean renderSmallCursor = (this.getCursorPosition() < this.getValue().length()) || (this.getValue().length() >= access.getMaxLengthFancyMenu());
            int finalTextXAfterCursor = textXAfterCursor;
            if (!isCursorInsideVisibleText) {
                finalTextXAfterCursor = (cursorPos > 0) ? (textX + this.width) : textX;
            } else if (renderSmallCursor) {
                finalTextXAfterCursor = textXAfterCursor - 1;
                if (this.textShadow) {
                    --textXAfterCursor;
                }
            }

            // Render text after the cursor
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
                graphics.drawString(this.font, afterCursorComp, textXAfterCursor, textY, textColor, this.textShadow);
            }

            // Vanilla Hint
            Component hint = access.getHintFancyMenu();
            boolean vanillaHintRendered = false;
            if ((hint != null) && text.isEmpty() && !this.isFocused()) {
                graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
                graphics.drawString(this.font, hint, textXAfterCursor, textY, textColor, this.textShadow);
                graphics.disableScissor();
                vanillaHintRendered = true;
            }

            // FancyMenu's Custom Hint Implementation
            Component hintFm = this.getHintFancyMenu();
            if (!vanillaHintRendered && (hintFm != null) && text.isEmpty()) {
                graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
                graphics.drawString(this.font, hintFm, this.getX() + 4, this.getY() + (this.getHeight() / 2) - (this.font.lineHeight / 2), -1, false);
                graphics.disableScissor();
            }

            // Render suggestion text
            if (!renderSmallCursor && access.getSuggestionFancyMenu() != null) {
                graphics.drawString(this.font, access.getSuggestionFancyMenu(), (finalTextXAfterCursor - 1), textY, this.suggestionTextColor.getColorInt(), this.textShadow);
            }

            // Render the cursor
            if (isCursorVisible) {
                if (renderSmallCursor) {
                    graphics.fill(finalTextXAfterCursor, textY - 1, finalTextXAfterCursor + 1, textY + 1 + 9, textColor);
                } else {
                    // This is the underscore-style cursor at the end
                    graphics.drawString(this.font, "_", finalTextXAfterCursor, textY, textColor, this.textShadow);
                }
            }

            // Render the text highlight
            if (highlightPos != cursorPos) {
                int l1 = textX + this.font.width(text.substring(0, highlightPos));
                graphics.textHighlight(finalTextXAfterCursor, textY - 1, l1 - 1, textY + 1 + 9);
            }

        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.isActiveSupplier != null) this.active = this.isActiveSupplier.get(this);
        if (this.isVisibleSupplier != null) this.visible = this.isVisibleSupplier.get(this);
        super.render(graphics, mouseX, mouseY, partial);

        if ((this.customTooltip != null) && this.visible && this.isHovered()) {
            Tooltip tt = this.customTooltip.get();
            if (tt != null) {
                if (this.forceDefaultTooltipStyle) {
                    tt.setDefaultStyle();
                }
                TooltipHandler.INSTANCE.addTooltip(tt, () -> true, true, true);
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

    public int getHighlightPosition() {
        return ((IMixinEditBox)this).getHighlightPosFancyMenu();
    }

    public @Nullable CharacterFilter getCharacterFilter() {
        return this.characterFilter;
    }

    public ExtendedEditBox setCharacterFilter(@Nullable CharacterFilter characterFilter) {
        this.characterFilter = characterFilter;
        return this;
    }

    @NotNull
    public ExtendedEditBox setDeleteAllAllowed(boolean allowed) {
        this.deleteAllAllowed = allowed;
        return this;
    }

    public boolean isDeleteAllAllowed() {
        return this.deleteAllAllowed;
    }

    public boolean hasTextShadow() {
        return this.textShadow;
    }

    /**
     * Had to rename this in 1.21.1+, because NeoForge seems to add its own setTextShadow() method to the {@link EditBox} class.
     */
    public ExtendedEditBox setTextShadow_FancyMenu(boolean textShadow) {
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
        this.setValue(this.getValueWithoutPrefixSuffix());
        return this;
    }

    public @Nullable String getInputSuffix() {
        return inputSuffix;
    }

    public ExtendedEditBox setInputSuffix(@Nullable String inputSuffix) {
        this.inputSuffix = inputSuffix;
        this.setValue(this.getValueWithoutPrefixSuffix());
        return this;
    }

    public ExtendedEditBox applyInputPrefixSuffixCharacterRenderFormatter() {
        this.setCharacterRenderFormatter((editBox, component, characterIndex, character, visiblePartOfLine, fullLine) -> {
            if ((this.inputSuffix != null) && (characterIndex > Math.max(0, (editBox.getValue().length() - this.inputSuffix.length())-1))) {
                component.withStyle(Style.EMPTY.withColor(this.getTextColorUneditable().getColorInt()));
            }
            if ((this.inputPrefix != null) && (characterIndex < this.inputPrefix.length())) {
                component.withStyle(Style.EMPTY.withColor(this.getTextColorUneditable().getColorInt()));
            }
            return component;
        });
        return this;
    }

    public void setIsActiveSupplier(@Nullable ConsumingSupplier<ExtendedEditBox, Boolean> isActiveSupplier) {
        this.isActiveSupplier = isActiveSupplier;
    }

    public void setIsVisibleSupplier(@Nullable ConsumingSupplier<ExtendedEditBox, Boolean> isVisibleSupplier) {
        this.isVisibleSupplier = isVisibleSupplier;
    }

    public boolean isEditable() {
        return ((IMixinEditBox)this).getIsEditableFancyMenu();
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
    public void setValue(@NotNull String value) {
        String v = this.getWithoutPrefixSuffix(value);
        if (this.inputPrefix != null) v = this.inputPrefix + v;
        if (this.inputSuffix != null) v = v + this.inputSuffix;
        super.setValue(v);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if ((this.characterFilter != null) && !this.characterFilter.isAllowedChar((char) event.codepoint())) {
            return false;
        }
        return super.charTyped(event);
    }

    @Override
    public void insertText(@NotNull String textToWrite) {
        if (this.isInPrefixSuffix(this.getCursorPosition(), 0, 0)) return;
        if (this.isInPrefixSuffix(this.getHighlightPosition(), 0, 0)) return;
        if (this.characterFilter != null) {
            textToWrite = this.characterFilter.filterForAllowedChars(textToWrite);
        }
        super.insertText(textToWrite);
    }

    @Override
    public void deleteChars(int i) {
        if (this.isInPrefixSuffix(this.getCursorPosition(), -1, -1) || this.isInPrefixSuffix(this.getCursorPosition(), 0, 0)) return;
        if (this.isInPrefixSuffix(this.getHighlightPosition(), 0, 0)) return;
        super.deleteChars(i);
    }

    @SuppressWarnings("all")
    public boolean isInPrefixSuffix(int index, int prefixIndexOffset, int suffixIndexOffset) {
        int cursorPrefix = index + prefixIndexOffset;
        int cursorSuffix = index + suffixIndexOffset;
        if (this.inputPrefix != null) {
            if (cursorPrefix < this.inputPrefix.length()) return true;
        }
        if (this.inputSuffix != null) {
            int i = (this.inputPrefix != null) ? this.inputPrefix.length() + this.getValueWithoutPrefixSuffix().length() : this.getValueWithoutPrefixSuffix().length();
            if (cursorSuffix > i) return true;
        }
        return false;
    }

    public String getValueWithoutPrefixSuffix() {
        return this.getWithoutPrefixSuffix(this.getValue());
    }

    protected String getWithoutPrefixSuffix(@NotNull String value) {
        if (value.isEmpty()) return value;
        boolean containsPrefix = (this.inputPrefix != null) && value.startsWith(this.inputPrefix);
        boolean containsSuffix = (this.inputSuffix != null) && value.endsWith(this.inputSuffix);
        String v = containsPrefix ? value.substring(this.inputPrefix.length()) : value;
        if (containsSuffix) v = v.substring(0, Math.max(0, v.length() - this.inputSuffix.length()));
        return v;
    }

    @Override
    public void deleteText(int i, boolean hasControlDown) {
        if (this.deleteAllAllowed) {
            super.deleteText(i, hasControlDown);
        } else {
            this.deleteChars(i);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.canConsumeUserInput) return false;
        //If select all, only select parts that are not prefix or suffix
        if (event.isSelectAll() && ((this.inputPrefix != null) || (this.inputSuffix != null))) {
            if (this.inputSuffix != null) {
                this.moveCursorTo(this.getValue().length() - this.inputSuffix.length(), false);
            } else {
                this.moveCursorToEnd(false);
            }
            this.setHighlightPos((this.inputPrefix != null) ? this.inputPrefix.length() : 0);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (!this.canConsumeUserInput) return false;
        return super.mouseClicked(event, isDoubleClick);
    }

    // This is to make the edit box work in FocuslessEventHandlers
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
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

    @NotNull
    public ExtendedEditBox setTooltip(@Nullable Supplier<Tooltip> tooltip) {
        this.customTooltip = tooltip;
        return this;
    }

    public boolean isForceDefaultTooltipStyle() {
        return forceDefaultTooltipStyle;
    }

    public void setForceDefaultTooltipStyle(boolean forceDefaultTooltipStyle) {
        this.forceDefaultTooltipStyle = forceDefaultTooltipStyle;
    }

    @NotNull
    public ExtendedEditBox setHintFancyMenu(@Nullable ConsumingSupplier<ExtendedEditBox, Component> hint) {
        this.hintFancymenu = hint;
        return this;
    }

    @Nullable
    protected MutableComponent getHintFancyMenu() {
        if (this.hintFancymenu == null) return null;
        Component c = this.hintFancymenu.get(this);
        if (c != null) {
            return c.copy().withColor(UIBase.getUIColorTheme().edit_box_text_color_uneditable.getColorInt());
        }
        return null;
    }

    @FunctionalInterface
    public interface CharacterRenderFormatter {
        @NotNull MutableComponent formatComponent(@NotNull ExtendedEditBox editBox, @NotNull MutableComponent component, int characterIndex, char character, @NotNull String visiblePartOfLine, @NotNull String fullLine);
    }

}