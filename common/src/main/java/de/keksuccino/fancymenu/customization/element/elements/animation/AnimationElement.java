package de.keksuccino.fancymenu.customization.element.elements.animation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

//TODO übernehmen (whole class)
@Deprecated(forRemoval = true)
public class AnimationElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String animationName;

    protected IAnimationRenderer animation = null;
    protected String lastName;
    protected int originalWidth;
    protected int originalHeight;

    public AnimationElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {}

    protected void updateResources() {}

    public void restoreAspectRatio() {
        AspectRatio ratio = new AspectRatio(this.originalWidth, this.originalHeight);
        this.baseWidth = ratio.getAspectRatioWidth(this.getAbsoluteHeight());
    }

}
