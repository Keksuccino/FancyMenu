package de.keksuccino.fancymenu.rendering.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class Button extends AdvancedButton {

    public Button(int x, int y, int widthIn, int heightIn, @NotNull String label, @NotNull OnPress onPress) {
        super(x, y, widthIn, heightIn, label, onPress);
    }

    public Button(int x, int y, int widthIn, int heightIn, @NotNull String label, boolean handleClick, @NotNull OnPress onPress) {
        super(x, y, widthIn, heightIn, label, handleClick, onPress);
    }

    public Button(int x, int y, int widthIn, int heightIn, @NotNull Component label, @NotNull OnPress onPress) {
        super(x, y, widthIn, heightIn, "", onPress);
        this.setMessage(label);
    }

    public Button(int x, int y, int widthIn, int heightIn, @NotNull Component label, boolean handleClick, @NotNull OnPress onPress) {
        super(x, y, widthIn, heightIn, "", handleClick, onPress);
        this.setMessage(label);
    }

    @Override
    protected void renderLabel(PoseStack pose) {
        if (this.renderLabel) {
            int k = this.active ? 0xFFFFFF : 0xA0A0A0;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
            this.renderString(pose, Minecraft.getInstance().font, k | Mth.ceil(this.alpha * 255.0f) << 24);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

}
