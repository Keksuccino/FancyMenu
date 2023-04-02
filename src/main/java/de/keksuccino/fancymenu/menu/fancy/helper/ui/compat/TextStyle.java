package de.keksuccino.fancymenu.menu.fancy.helper.ui.compat;

import de.keksuccino.fancymenu.mixin.client.IMixinStyle;
import net.minecraft.util.text.Style;

import javax.annotation.Nullable;

public class TextStyle extends Style {

    public static final TextStyle EMPTY = new TextStyle();

    private Integer colorRGB = null;

    public TextStyle setColorRGB(@Nullable Integer colorRGB) {
        this.colorRGB = colorRGB;
        return this;
    }

    public int getColorRGB() {
        return ((this.colorRGB == null) && (this.getAccessor().getParentStyleFancyMenu() instanceof TextStyle)) ? ((TextStyle) this.getAccessor().getParentStyleFancyMenu()).getColorRGB() : ((this.colorRGB != null) ? this.colorRGB : -1);
    }

    public IMixinStyle getAccessor() {
        return (IMixinStyle) this;
    }

    public TextStyle applyTo(TextStyle style) {
        if ((this == EMPTY) || this.isEmpty()) {
            return style;
        }
        if ((style == EMPTY) || style.isEmpty()) {
            return this;
        }
        TextStyle s = new TextStyle();
        if (this.colorRGB != null) {
            s.setColorRGB(this.getColorRGB());
        }
        if (this.getAccessor().getColorFancyMenu() != null) {
            s.setColor(this.getAccessor().getColorFancyMenu());
        }
        if (this.getAccessor().getBoldFancyMenu() != null) {
            s.setBold(this.getAccessor().getBoldFancyMenu());
        }
        if (this.getAccessor().getItalicFancyMenu() != null) {
            s.setItalic(this.getAccessor().getItalicFancyMenu());
        }
        if (this.getAccessor().getUnderlinedFancyMenu() != null) {
            s.setUnderlined(this.getAccessor().getUnderlinedFancyMenu());
        }
        if (this.getAccessor().getStrikethroughFancyMenu() != null) {
            s.setStrikethrough(this.getAccessor().getStrikethroughFancyMenu());
        }
        if (this.getAccessor().getObfuscatedFancyMenu() != null) {
            s.setObfuscated(this.getAccessor().getObfuscatedFancyMenu());
        }
        if (this.getAccessor().getClickEventFancyMenu() != null) {
            s.setClickEvent(this.getAccessor().getClickEventFancyMenu());
        }
        if (this.getAccessor().getHoverEventFancyMenu() != null) {
            s.setHoverEvent(this.getAccessor().getHoverEventFancyMenu());
        }
        if (this.getAccessor().getInsertionFancyMenu() != null) {
            s.setInsertion(this.getAccessor().getInsertionFancyMenu());
        }
        return s;
    }

    @Override
    public TextStyle createDeepCopy() {
        TextStyle style = new TextStyle();
        style.setColorRGB(this.getColorRGB());
        style.setBold(this.getBold());
        style.setItalic(this.getItalic());
        style.setStrikethrough(this.getStrikethrough());
        style.setUnderlined(this.getUnderlined());
        style.setObfuscated(this.getObfuscated());
        style.setColor(this.getColor());
        style.setClickEvent(this.getClickEvent());
        style.setHoverEvent(this.getHoverEvent());
        style.setInsertion(this.getInsertion());
        return style;
    }

    @Override
    public TextStyle createShallowCopy() {
        TextStyle style = new TextStyle();
        style.setColorRGB(this.colorRGB);
        style.setBold(this.getAccessor().getBoldFancyMenu());
        style.setItalic(this.getAccessor().getItalicFancyMenu());
        style.setStrikethrough(this.getAccessor().getStrikethroughFancyMenu());
        style.setUnderlined(this.getAccessor().getUnderlinedFancyMenu());
        style.setObfuscated(this.getAccessor().getObfuscatedFancyMenu());
        style.setColor(this.getAccessor().getColorFancyMenu());
        style.setClickEvent(this.getAccessor().getClickEventFancyMenu());
        style.setHoverEvent(this.getAccessor().getHoverEventFancyMenu());
        style.setParentStyle(this.getAccessor().getParentStyleFancyMenu());
        style.setInsertion(this.getAccessor().getInsertionFancyMenu());
        return style;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && (this.colorRGB == null);
    }

}
