package de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry;

import com.mojang.blaze3d.matrix.MatrixStack;
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
    public FontRenderer font = Minecraft.getInstance().font;
    protected Consumer<TextListScrollAreaEntry> onClickCallback;

    public TextListScrollAreaEntry(ScrollArea parent, ITextComponent text,  Color listDotColor,  Consumer<TextListScrollAreaEntry> onClick) {
        super(parent, 0, 16);
        this.listDotColor = listDotColor;
        this.onClickCallback = onClick;
        this.setText(text);
    }

    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partial) {

        super.render(matrix, mouseX, mouseY, partial);

        int centerY = this.getY() + (this.getHeight() / 2);

        renderListingDot(matrix, this.getX() + 5, centerY - 2, this.listDotColor);

        this.font.draw(matrix, this.text, (float)(this.getX() + 5 + 4 + 3), (float)(centerY - (this.font.lineHeight / 2)), -1);

    }

    @Override
    public void onClick(ScrollAreaEntry entry) {
        this.onClickCallback.accept((TextListScrollAreaEntry) entry);
    }

    public void setText(ITextComponent text) {
        this.text = text;
        this.textWidth = this.font.width(this.text);
        this.setWidth(5 + 4 + 3 + this.textWidth + 5);
    }

    public ITextComponent getText() {
        return this.text;
    }

    public int getTextWidth() {
        return this.textWidth;
    }

}
