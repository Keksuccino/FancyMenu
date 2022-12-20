package de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render.layers;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render.PlayerEntityItemRenderer;
import de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render.PlayerEntityProperties;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

import javax.annotation.Nullable;

public class PlayerEntityCapeLayer extends PlayerEntityRenderLayer {

    public final PlayerEntityProperties properties;
    public final PlayerEntityItemRenderer renderer;

    public PlayerEntityCapeLayer(PlayerEntityItemRenderer renderer, PlayerEntityProperties properties) {
        super(renderer);
        this.properties = properties;
        this.renderer = renderer;
    }

    @Override
    public void render(MatrixStack matrix, IRenderTypeBuffer p_116616_, int p_116617_, @Nullable Entity entity, float p_116619_, float p_116620_, float p_116621_, float p_116622_, float p_116623_, float p_116624_) {
        if (!this.properties.invisible && this.properties.isModelPartShown(PlayerModelPart.CAPE) && this.properties.getCapeTextureLocation() != null) {
            matrix.pushPose();
            matrix.translate(0.0F, 0.0F, 0.125F);
            double d0 = MathHelper.lerp((double)p_116621_, this.properties.xCloakO, this.properties.xCloak) - MathHelper.lerp((double)p_116621_, this.properties.xo, 0);
            double d1 = MathHelper.lerp((double)p_116621_, this.properties.yCloakO, this.properties.yCloak) - MathHelper.lerp((double)p_116621_, this.properties.yo, 0);
            double d2 = MathHelper.lerp((double)p_116621_, this.properties.zCloakO, this.properties.zCloak) - MathHelper.lerp((double)p_116621_, this.properties.zo, 0);
            float f = this.properties.yBodyRotO + (this.properties.yBodyRot - this.properties.yBodyRotO);
            double d3 = (double)MathHelper.sin(f * ((float)Math.PI / 180F));
            double d4 = (double)(-MathHelper.cos(f * ((float)Math.PI / 180F)));
            float f1 = (float)d1 * 10.0F;
            f1 = MathHelper.clamp(f1, -6.0F, 32.0F);
            float f2 = (float)(d0 * d3 + d2 * d4) * 100.0F;
            f2 = MathHelper.clamp(f2, 0.0F, 150.0F);
            float f3 = (float)(d0 * d4 - d2 * d3) * 100.0F;
            f3 = MathHelper.clamp(f3, -20.0F, 20.0F);
            if (f2 < 0.0F) {
                f2 = 0.0F;
            }

            float f4 = MathHelper.lerp(p_116621_, this.properties.oBob, this.properties.bob);
            f1 += MathHelper.sin(MathHelper.lerp(p_116621_, 0, 0) * 6.0F) * 32.0F * f4;
            if (this.properties.isCrouching()) {
                f1 += 25.0F;
            }

            matrix.mulPose(Vector3f.XP.rotationDegrees(6.0F + f2 / 2.0F + f1));
            matrix.mulPose(Vector3f.ZP.rotationDegrees(f3 / 2.0F));
            matrix.mulPose(Vector3f.YP.rotationDegrees(180.0F - f3 / 2.0F));
            IVertexBuilder vertexconsumer = p_116616_.getBuffer(RenderType.entitySolid(this.properties.getCapeTextureLocation()));
            this.renderer.getModel().renderCloak(matrix, vertexconsumer, p_116617_, OverlayTexture.NO_OVERLAY);
            matrix.popPose();
        }
    }

}