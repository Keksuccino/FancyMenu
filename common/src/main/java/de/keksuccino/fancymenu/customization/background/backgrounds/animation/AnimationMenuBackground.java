package de.keksuccino.fancymenu.customization.background.backgrounds.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@Deprecated(forRemoval = true)
public class AnimationMenuBackground extends MenuBackground<AnimationMenuBackground> {

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String animationName;
    public boolean restartOnMenuLoad = false;

    protected String lastAnimationName;
    protected IAnimationRenderer animation;
    protected boolean restarted = false;

    public AnimationMenuBackground(MenuBackgroundBuilder<AnimationMenuBackground> builder) {
        super(builder);
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {}

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {}

}
