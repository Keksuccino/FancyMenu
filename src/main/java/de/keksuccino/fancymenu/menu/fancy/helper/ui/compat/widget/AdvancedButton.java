package de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.widget;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.AbstractGui;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.RenderSystem;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.component.Component;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class AdvancedButton extends de.keksuccino.konkrete.gui.content.AdvancedButton {

    protected ITextComponent message = Component.literal("");
    protected FontRenderer font = Minecraft.getMinecraft().fontRenderer;
    protected String lastDisplayString = null;

    public AdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText, Consumer<AdvancedButton> onPress) {
        super(x, y, widthIn, heightIn, buttonText, (b) -> {
            onPress.accept((AdvancedButton)b);
        });
        if (buttonText != null) {
            this.message = Component.literal(buttonText);
        }
    }

    public AdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText, boolean handleClick, Consumer<AdvancedButton> onPress) {
        super(x, y, widthIn, heightIn, buttonText, handleClick, (b) -> {
            onPress.accept((AdvancedButton) b);
        });
        if (buttonText != null) {
            this.message = Component.literal(buttonText);
        }
    }

    public void render(int mouseX, int mouseY, float partial) {
        this.renderButton(mouseX, mouseY, partial);
    }

    public void renderButton(int mouseX, int mouseY, float partial) {
        super.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partial);
    }

    /** Use renderButton() instead **/
    @Deprecated
    @Override
    public final void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        this.renderButton(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());
    }

    @Override
    protected void renderLabel() {
        //Fix cases where displayString was set directly
        if ((this.lastDisplayString != null) && (this.displayString != null) && (!this.lastDisplayString.equals(this.displayString))) {
            this.setMessage(this.displayString);
        }
        if (this.renderLabel) {
            int stringWidth = font.getStringWidth(this.getMessage().getFormattedText());
            int stringHeight = 8;
            int pX = (int)(((float)(this.x + this.width / 2) - (float)stringWidth * this.labelScale / 2.0F) / this.labelScale);
            int pY = (int)(((float)(this.y + this.height / 2) - (float)stringHeight * this.labelScale / 2.0F) / this.labelScale);
            RenderSystem.pushMatrix();
            RenderSystem.scale(this.labelScale, this.labelScale, this.labelScale);
            if (this.labelShadow) {
                AbstractGui.drawFormattedStringWithShadow(this.font, this.message, pX, pY, this.getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24);
            } else {
                AbstractGui.drawFormattedString(this.font, this.message, pX, pY, this.getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24);
            }
            RenderSystem.popMatrix();
        }
    }

    @Override
    protected int getFGColor() {
        return this.enabled ? 16777215 : 10526880;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getX() {
        return this.x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getY() {
        return this.y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return this.width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean isHovered() {
        return super.isMouseOver();
    }

    /** Use isHovered() instead **/
    @Deprecated
    @Override
    public final boolean isMouseOver() {
        return this.isHovered();
    }

    public void playDownSound(SoundHandler handler) {
        super.playPressSound(handler);
    }

    /** Use playDownSound() instead **/
    @Deprecated
    @Override
    public final void playPressSound(SoundHandler soundHandlerIn) {
        this.playDownSound(soundHandlerIn);
    }

    public void onClick(double mouseX, double mouseY) {
        super.mousePressed(Minecraft.getMinecraft(), (int) mouseX, (int) mouseY);
    }

    /** Use onClick() instead **/
    @Deprecated
    @Override
    public final boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        this.onClick(mouseX, mouseY);
        return true;
    }

    public void setMessage(@Nonnull String msg) {
        this.message = Component.literal(msg);
        this.displayString = msg;
        this.lastDisplayString = msg;
    }

    public void setMessage(@Nonnull ITextComponent msg) {
        this.message = msg;
        this.displayString = msg.getFormattedText();
        this.lastDisplayString = this.displayString;
    }

    public ITextComponent getMessage() {
        return this.message;
    }

}
