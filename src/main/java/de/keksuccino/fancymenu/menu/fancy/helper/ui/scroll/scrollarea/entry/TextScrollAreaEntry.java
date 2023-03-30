package de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.ScrollArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.function.Consumer;

public class TextScrollAreaEntry extends ScrollAreaEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    protected ITextComponent text;
    protected int textWidth;
    public FontRenderer font = Minecraft.getInstance().font;
    protected Consumer<TextScrollAreaEntry> onClickCallback;

    public TextScrollAreaEntry(ScrollArea parent, ITextComponent text,  Consumer<TextScrollAreaEntry> onClick) {
        super(parent, 0, 14);
        this.onClickCallback = onClick;
        this.setText(text);
    }

    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partial) {

        super.render(matrix, mouseX, mouseY, partial);

        int centerY = this.getY() + (this.getHeight() / 2);

        this.font.draw(matrix, this.text, (float)(this.getX() + 5), (float)(centerY - (this.font.lineHeight / 2)), -1);

    }

    @Override
    public void onClick(ScrollAreaEntry entry) {
        this.onClickCallback.accept((TextScrollAreaEntry) entry);
    }

    public void setText(ITextComponent text) {
        this.text = text;
        this.textWidth = this.font.width(this.text);
        this.setWidth(5 + this.textWidth + 5);
    }

    public ITextComponent getText() {
        return this.text;
    }

    public int getTextWidth() {
        return this.textWidth;
    }

}
