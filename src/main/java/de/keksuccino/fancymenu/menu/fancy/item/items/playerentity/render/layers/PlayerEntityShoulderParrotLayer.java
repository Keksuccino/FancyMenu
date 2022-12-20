package de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render.layers;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render.PlayerEntityItemRenderer;
import de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render.PlayerEntityProperties;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.model.ParrotModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;

import javax.annotation.Nullable;

public class PlayerEntityShoulderParrotLayer extends PlayerEntityRenderLayer {

    private final ParrotModel model = new ParrotModel();
    public final PlayerEntityProperties properties;

    public PlayerEntityShoulderParrotLayer(PlayerEntityItemRenderer renderer, PlayerEntityProperties properties) {
        super(renderer);
        this.properties = properties;
    }

    public void render(MatrixStack p_117307_, IRenderTypeBuffer p_117308_, int p_117309_, @Nullable Entity entity, float p_117311_, float p_117312_, float p_117313_, float p_117314_, float p_117315_, float p_117316_) {
        if (this.properties.hasParrotOnShoulder) {
            this.render(p_117307_, p_117308_, p_117309_, p_117311_, p_117312_, p_117315_, p_117316_, this.properties.parrotOnLeftShoulder);
        }
    }

    private void render(MatrixStack p_117318_, IRenderTypeBuffer p_117319_, int p_117320_, float p_117322_, float p_117323_, float p_117324_, float p_117325_, boolean onLeftShoulder) {
        p_117318_.pushPose();
        p_117318_.translate(onLeftShoulder ? 0.4F : -0.4F, this.properties.isCrouching() ? -1.3F : -1.5F, 0.0F);
        IVertexBuilder vertexconsumer = p_117319_.getBuffer(this.model.renderType(ParrotRenderer.PARROT_LOCATIONS[this.properties.shoulderParrotVariant]));
        this.model.renderOnShoulder(p_117318_, vertexconsumer, p_117320_, OverlayTexture.NO_OVERLAY, p_117322_, p_117323_, p_117324_, p_117325_, this.properties.tickCount);
        p_117318_.popPose();
    }

}