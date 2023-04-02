package de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.ScrollArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;

import java.awt.*;
import java.util.function.Consumer;

public class TextListScrollAreaEntry extends ScrollAreaEntry {

    public Color listDotColor;
    protected ITextComponent text;
    protected int textWidth;
    public FontRenderer font = Minecraft.getMinecraft().fontRenderer;
    protected Consumer<TextListScrollAreaEntry> onClickCallback;

    public TextListScrollAreaEntry(ScrollArea parent, ITextComponent text,  Color listDotColor,  Consumer<TextListScrollAreaEntry> onClick) {
        super(parent, 0, 16);
        this.listDotColor = listDotColor;
        this.onClickCallback = onClick;
        this.setText(text);
    }

    @Override
    public void render(int mouseX, int mouseY, float partial) {

        super.render(mouseX, mouseY, partial);

        int centerY = this.getY() + (this.getHeight() / 2);

        renderListingDot(this.getX() + 5, centerY - 2, this.listDotColor);

        this.font.drawString(this.text.getFormattedText(), (this.getX() + 5 + 4 + 3), (centerY - (this.font.FONT_HEIGHT / 2)), -1);

    }

    @Override
    public void onClick(ScrollAreaEntry entry) {
        this.onClickCallback.accept((TextListScrollAreaEntry) entry);
    }

    public void setText(ITextComponent text) {
        this.text = text;
        this.textWidth = this.font.getStringWidth(this.text.getFormattedText());
        this.setWidth(5 + 4 + 3 + this.textWidth + 5);
    }

    public ITextComponent getText() {
        return this.text;
    }

    public int getTextWidth() {
        return this.textWidth;
    }

}
