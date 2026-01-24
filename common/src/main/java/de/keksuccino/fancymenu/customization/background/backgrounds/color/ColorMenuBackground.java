package de.keksuccino.fancymenu.customization.background.backgrounds.color;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import java.awt.*;

public class ColorMenuBackground extends MenuBackground<ColorMenuBackground> {

    public final Property.ColorProperty color = putProperty(Property.hexColorProperty("color", DrawableColor.of(Color.ORANGE).getHex(), true, "fancymenu.backgrounds.color.hex"));

    public ColorMenuBackground(MenuBackgroundBuilder<ColorMenuBackground> builder) {
        super(builder);
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.color.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();
        graphics.fill(RenderType.gui(), 0, 0, getScreenWidth(), getScreenHeight(), this.color.getDrawable().getColorIntWithAlpha(this.opacity));
        graphics.flush();

    }

}
