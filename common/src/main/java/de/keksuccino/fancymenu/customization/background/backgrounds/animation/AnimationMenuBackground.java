package de.keksuccino.fancymenu.customization.background.backgrounds.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

//TODO übernehmen (whole class)
@Deprecated(forRemoval = true)
public class AnimationMenuBackground extends MenuBackground {

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
    public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {}

}
