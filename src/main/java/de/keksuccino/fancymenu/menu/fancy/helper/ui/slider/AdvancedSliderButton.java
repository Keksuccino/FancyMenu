
package de.keksuccino.fancymenu.menu.fancy.helper.ui.slider;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public abstract class AdvancedSliderButton extends AbstractSliderButton {

    protected static boolean leftDownGlobal = false;

    public boolean handleClick;
    public boolean enableRightClick = false;
    public boolean ignoreBlockedInput = false;
    public boolean ignoreGlobalLeftMouseDown = false;
    protected String messagePrefix = null;
    protected String messageSuffix = null;
    protected Consumer<AdvancedSliderButton> applyValueCallback;

    protected boolean leftDownNotHovered = false;
    protected boolean leftDownThis = false;

    public AdvancedSliderButton(int x, int y, int width, int height, boolean handleClick, double value, Consumer<AdvancedSliderButton> applyValueCallback) {
        super(x, y, width, height, CommonComponents.EMPTY, value);
        this.handleClick = handleClick;
        this.applyValueCallback = applyValueCallback;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {

        if (this.visible) {

            this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

            if (!this.isHoveredOrFocused() && MouseInput.isLeftMouseDown()) {
                this.leftDownNotHovered = true;
            }
            if (!MouseInput.isLeftMouseDown()) {
                this.leftDownNotHovered = false;
            }

            if (this.handleClick) {
                if (this.isHoveredOrFocused() && (MouseInput.isLeftMouseDown() || (this.enableRightClick && MouseInput.isRightMouseDown())) && (!leftDownGlobal || this.ignoreGlobalLeftMouseDown) && !leftDownNotHovered && !this.isInputBlocked() && this.active && this.visible) {
                    if (!this.leftDownThis) {
                        this.onClick(mouseX, mouseY);
                        leftDownGlobal = true;
                        this.leftDownThis = true;
                    }
                }
                if (!MouseInput.isLeftMouseDown() && !(MouseInput.isRightMouseDown() && this.enableRightClick)) {
                    leftDownGlobal = false;
                    if (this.leftDownThis) {
                        this.onRelease(mouseX, mouseY);
                    }
                    this.leftDownThis = false;
                }
                if (this.leftDownThis) {
                    this.onDrag(mouseX, mouseY, 0, 0);
                }
            }

        }

        super.render(matrix, mouseX, mouseY, partialTicks);

    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double d1, double d2) {
        super.onDrag(mouseX, mouseY, d1, d2);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
    }

    @Override
    protected void applyValue() {
        if (this.applyValueCallback != null) {
            this.applyValueCallback.accept(this);
        }
    }

    @Override
    public void updateMessage() {
        String s = "";
        if (this.messagePrefix != null) {
            s += this.messagePrefix;
        }
        s += this.getSliderMessageWithoutPrefixSuffix();
        if (this.messageSuffix != null) {
            s += this.messageSuffix;
        }
        this.setMessage(Component.literal(s));
    }

    public abstract String getSliderMessageWithoutPrefixSuffix();

    public void setLabelPrefix(String prefix) {
        this.messagePrefix = prefix;
        this.updateMessage();
    }

    public void setLabelSuffix(String suffix) {
        this.messageSuffix = suffix;
        this.updateMessage();
    }

    public void setValue(double value) {
        double d0 = this.value;
        this.value = Mth.clamp(value, 0.0D, 1.0D);
        if (d0 != this.value) {
            this.applyValue();
        }
        this.updateMessage();
    }

    public double getValue() {
        return this.value;
    }

    protected boolean isInputBlocked() {
        if (this.ignoreBlockedInput) {
            return false;
        }
        return MouseInput.isVanillaInputBlocked();
    }

}
