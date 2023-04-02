package de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.component;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;

public class AdvancedTextComponentString extends TextComponentString {

    public AdvancedTextComponentString(String msg) {
        super(msg);
    }

    public AdvancedTextComponentString append(ITextComponent component) {
        return (AdvancedTextComponentString) super.appendSibling(component);
    }

    /** Use append() instead **/
    @Deprecated
    @Override
    public final AdvancedTextComponentString appendSibling(ITextComponent component) {
        return this.append(component);
    }

    /** Use append() instead **/
    @Deprecated
    @Override
    public final AdvancedTextComponentString appendText(String text) {
        return this.append(Component.literal(text));
    }

    @Override
    public AdvancedTextComponentString setStyle(Style style) {
        return (AdvancedTextComponentString) super.setStyle(style);
    }

}
