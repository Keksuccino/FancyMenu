package de.keksuccino.fancymenu.customization.element.elements.playerentity.model.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.model.PlayerEntityElementRenderer;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.model.PlayerEntityProperties;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Parrot;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

public class PlayerEntityShoulderParrotLayer extends PlayerEntityRenderLayer {

    private final ParrotModel model;
    public final PlayerEntityProperties properties;

    public PlayerEntityShoulderParrotLayer(PlayerEntityElementRenderer renderer, EntityModelSet modelSet, PlayerEntityProperties properties) {
        super(renderer);
        this.model = new ParrotModel(modelSet.bakeLayer(ModelLayers.PARROT));
        this.properties = properties;
    }

    public void render(PoseStack p_117307_, MultiBufferSource p_117308_, int p_117309_, @Nullable Entity entity, float p_117311_, float p_117312_, float p_117313_, float p_117314_, float p_117315_, float p_117316_) {
        if (this.properties.hasParrotOnShoulder) {
            this.render(p_117307_, p_117308_, p_117309_, p_117311_, p_117312_, p_117315_, p_117316_, this.properties.parrotOnLeftShoulder);
        }
    }

    private void render(PoseStack p_117318_, MultiBufferSource p_117319_, int p_117320_, float p_117322_, float p_117323_, float p_117324_, float p_117325_, boolean onLeftShoulder) {
        p_117318_.pushPose();
        p_117318_.translate(onLeftShoulder ? 0.4F : -0.4F, this.properties.isCrouching() ? -1.3F : -1.5F, 0.0F);
        Variant variant = Variant.byId(this.properties.shoulderParrotVariant);
        if (variant == null) variant = Variant.RED_BLUE;
        VertexConsumer vertexconsumer = p_117319_.getBuffer(this.model.renderType(ParrotVariants.getVariantTexture(variant)));
        this.model.renderOnShoulder(p_117318_, vertexconsumer, p_117320_, OverlayTexture.NO_OVERLAY, p_117322_, p_117323_, p_117324_, p_117325_, this.properties.tickCount);
        p_117318_.popPose();
    }

    public static class ParrotVariants {

        private static final ResourceLocation RED_BLUE = new ResourceLocation("textures/entity/parrot/parrot_red_blue.png");
        private static final ResourceLocation BLUE = new ResourceLocation("textures/entity/parrot/parrot_blue.png");
        private static final ResourceLocation GREEN = new ResourceLocation("textures/entity/parrot/parrot_green.png");
        private static final ResourceLocation YELLOW_BLUE = new ResourceLocation("textures/entity/parrot/parrot_yellow_blue.png");
        private static final ResourceLocation GREY = new ResourceLocation("textures/entity/parrot/parrot_grey.png");

        public static ResourceLocation getVariantTexture(Variant variant) {
            return switch(variant) {
                case RED_BLUE -> RED_BLUE;
                case BLUE -> BLUE;
                case GREEN -> GREEN;
                case YELLOW_BLUE -> YELLOW_BLUE;
                case GRAY -> GREY;
            };
        }

    }

    public enum Variant {

        RED_BLUE(0),
        BLUE(1),
        GREEN(2),
        YELLOW_BLUE(3),
        GRAY(4);

        final int id;

        Variant(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        @Nullable
        public static Variant byId(int id) {
            for (Variant v : Variant.values()) {
                if (v.id == id) return v;
            }
            return null;
        }

    }

}