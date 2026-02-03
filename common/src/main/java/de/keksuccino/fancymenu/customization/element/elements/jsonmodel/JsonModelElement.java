package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JsonModelElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();

    public final Property<ResourceSupplier<IText>> modelSource = putProperty(Property.resourceSupplierProperty( IText.class, "model_source", null, "fancymenu.elements.json_model.model_source", true, true, true, file -> FileTypes.JSON_TEXT.isFileTypeLocal(file) ));
    public final Property<ResourceSupplier<ITexture>> textureSource = putProperty(Property.resourceSupplierProperty( ITexture.class, "texture_source", null, "fancymenu.elements.json_model.texture_source", true, true, true, FileFilter.IMAGE_FILE_FILTER ));
    public final Property.BooleanProperty useTextureOverride = putProperty(Property.booleanProperty( "use_texture_override", false, "fancymenu.elements.json_model.use_texture_override" ));
    public final Property.BooleanProperty useModelDisplayTransform = putProperty(Property.booleanProperty( "use_model_display_transform", true, "fancymenu.elements.json_model.use_model_display_transform" ));
    public final Property.BooleanProperty renderTranslucent = putProperty(Property.booleanProperty( "render_translucent", false, "fancymenu.elements.json_model.render_translucent" ));

    public final Property.FloatProperty modelScale = putProperty(Property.floatProperty( "model_scale", 1.0F, "fancymenu.elements.json_model.model_scale", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty modelRotationX = putProperty(Property.floatProperty( "model_rotation_x", 0.0F, "fancymenu.elements.json_model.model_rotation_x", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty modelRotationY = putProperty(Property.floatProperty( "model_rotation_y", 0.0F, "fancymenu.elements.json_model.model_rotation_y", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty modelRotationZ = putProperty(Property.floatProperty( "model_rotation_z", 0.0F, "fancymenu.elements.json_model.model_rotation_z", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty modelOffsetX = putProperty(Property.floatProperty( "model_offset_x", 0.0F, "fancymenu.elements.json_model.model_offset_x", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty modelOffsetY = putProperty(Property.floatProperty( "model_offset_y", 0.0F, "fancymenu.elements.json_model.model_offset_y", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty modelOffsetZ = putProperty(Property.floatProperty( "model_offset_z", 0.0F, "fancymenu.elements.json_model.model_offset_z", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));

    public final Property.ColorProperty lightHue = putProperty(Property.hexColorProperty( "light_hue", "#FFFFFF", true, "fancymenu.elements.json_model.light_hue" ));
    public final Property.FloatProperty lightRotationX = putProperty(Property.floatProperty( "light_rotation_x", 0.0F, "fancymenu.elements.json_model.light_rotation_x", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty lightRotationY = putProperty(Property.floatProperty( "light_rotation_y", 0.0F, "fancymenu.elements.json_model.light_rotation_y", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty lightRotationZ = putProperty(Property.floatProperty( "light_rotation_z", 0.0F, "fancymenu.elements.json_model.light_rotation_z", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty light0X = putProperty(Property.floatProperty( "light_0_x", 0.2F, "fancymenu.elements.json_model.light_0_x", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty light0Y = putProperty(Property.floatProperty( "light_0_y", -1.0F, "fancymenu.elements.json_model.light_0_y", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty light0Z = putProperty(Property.floatProperty( "light_0_z", 1.0F, "fancymenu.elements.json_model.light_0_z", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty light1X = putProperty(Property.floatProperty( "light_1_x", -0.2F, "fancymenu.elements.json_model.light_1_x", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty light1Y = putProperty(Property.floatProperty( "light_1_y", -1.0F, "fancymenu.elements.json_model.light_1_y", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));
    public final Property.FloatProperty light1Z = putProperty(Property.floatProperty( "light_1_z", 0.0F, "fancymenu.elements.json_model.light_1_z", Property.NumericInputBehavior.<Float>builder().freeInput().build() ));

    @Nullable
    private String lastModelSource = null;
    @Nullable
    private String lastTextureSource = null;
    @Nullable
    private String lastModelJson = null;
    private boolean lastUseTextureOverride = false;
    @Nullable
    private BakedModel cachedModel = null;
    @Nullable
    private ResourceLocation cachedRenderTexture = null;
    @Nullable
    private ModelTextureSprite cachedOverrideSprite = null;
    private final Map<ResourceLocation, BlockModel> parentModelCache = new HashMap<>();

    public JsonModelElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.setSupportsRotation(false);
        this.setSupportsTilting(false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) {
            return;
        }

        this.ensureModelCache();

        if (this.cachedModel == null || this.cachedRenderTexture == null) {
            if (isEditor()) {
                RenderingUtils.renderMissing(graphics, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
            }
            return;
        }

        int width = this.getAbsoluteWidth();
        int height = this.getAbsoluteHeight();
        int minSize = Math.min(width, height);
        if (minSize <= 0) {
            return;
        }

        float scale = Math.max(0.0001F, minSize * this.modelScale.getFloat());
        float centerX = this.getAbsoluteX() + (width / 2.0F);
        float centerY = this.getAbsoluteY() + (height / 2.0F);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 200.0F);
        pose.translate(this.modelOffsetX.getFloat(), this.modelOffsetY.getFloat(), this.modelOffsetZ.getFloat());
        pose.scale(scale, scale, scale);

        if (this.useModelDisplayTransform.getBoolean()) {
            this.cachedModel.getTransforms().getTransform(ItemDisplayContext.GUI).apply(false, pose);
        }

        pose.mulPose(new Quaternionf().rotationXYZ(
                (float) Math.toRadians(this.modelRotationX.getFloat()),
                (float) Math.toRadians(this.modelRotationY.getFloat()),
                (float) Math.toRadians(this.modelRotationZ.getFloat())
        ));

        pose.translate(-0.5F, -0.5F, -0.5F);

        this.applyLightSettings();

        DrawableColor lightColor = this.lightHue.getDrawable();
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        float a = this.opacity;
        if (lightColor != null && lightColor != DrawableColor.EMPTY) {
            Color color = lightColor.getColor();
            r = color.getRed() / 255.0F;
            g = color.getGreen() / 255.0F;
            b = color.getBlue() / 255.0F;
            a = (color.getAlpha() / 255.0F) * this.opacity;
        }

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        RenderType renderType = this.renderTranslucent.getBoolean()
                ? RenderType.entityTranslucent(this.cachedRenderTexture)
                : RenderType.entityCutoutNoCull(this.cachedRenderTexture);

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(renderType);
        renderModelQuads(this.cachedModel, pose, consumer, r, g, b, a);
        buffer.endBatch();

        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();

        Lighting.setupFor3DItems();
        RenderingUtils.resetShaderColor(graphics);
        pose.popPose();
    }

    private void applyLightSettings() {
        Vector3f light0 = new Vector3f(this.light0X.getFloat(), this.light0Y.getFloat(), this.light0Z.getFloat());
        Vector3f light1 = new Vector3f(this.light1X.getFloat(), this.light1Y.getFloat(), this.light1Z.getFloat());

        Quaternionf rotation = new Quaternionf().rotationXYZ(
                (float) Math.toRadians(this.lightRotationX.getFloat()),
                (float) Math.toRadians(this.lightRotationY.getFloat()),
                (float) Math.toRadians(this.lightRotationZ.getFloat())
        );
        rotation.transform(light0);
        rotation.transform(light1);
        light0.normalize();
        light1.normalize();
        RenderSystem.setShaderLights(light0, light1);
    }

    private void renderModelQuads(@NotNull BakedModel model, @NotNull PoseStack poseStack, @NotNull VertexConsumer consumer, float r, float g, float b, float a) {
        RandomSource random = RandomSource.create();
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        PoseStack.Pose pose = poseStack.last();

        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            random.setSeed(42L);
            List<BakedQuad> quads = model.getQuads(null, direction, random);
            for (BakedQuad quad : quads) {
                consumer.putBulkData(pose, quad, r, g, b, a, light, overlay);
            }
        }
        random.setSeed(42L);
        List<BakedQuad> quads = model.getQuads(null, null, random);
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, r, g, b, a, light, overlay);
        }
    }

    private void ensureModelCache() {
        ResourceSupplier<IText> modelSupplier = this.modelSource.get();
        String modelKey = (modelSupplier != null) ? modelSupplier.getSourceWithPrefix() : null;

        ResourceSupplier<ITexture> textureSupplier = this.textureSource.get();
        String textureKey = (textureSupplier != null) ? textureSupplier.getSourceWithPrefix() : null;

        boolean override = this.useTextureOverride.getBoolean();

        boolean sourceChanged = !Objects.equals(modelKey, this.lastModelSource);
        boolean textureChanged = !Objects.equals(textureKey, this.lastTextureSource);
        boolean overrideChanged = override != this.lastUseTextureOverride;

        if (sourceChanged || textureChanged || overrideChanged) {
            invalidateCache(sourceChanged);
            this.lastModelSource = modelKey;
            this.lastTextureSource = textureKey;
            this.lastUseTextureOverride = override;
        }

        if (this.cachedModel != null) {
            return;
        }

        if (modelSupplier == null) {
            return;
        }

        IText text = modelSupplier.get();
        if (text == null || !text.isReady()) {
            return;
        }

        List<String> lines = text.getTextLines();
        if (lines == null || lines.isEmpty()) {
            return;
        }

        String json = String.join("\n", lines);
        if (!Objects.equals(json, this.lastModelJson)) {
            this.lastModelJson = json;
        }

        buildModelCache(json, override, textureSupplier);
    }

    private void invalidateCache(boolean clearParents) {
        this.cachedModel = null;
        this.cachedRenderTexture = null;
        if (this.cachedOverrideSprite != null) {
            this.cachedOverrideSprite.close();
            this.cachedOverrideSprite = null;
        }
        if (clearParents) {
            this.parentModelCache.clear();
            this.lastModelJson = null;
        }
    }

    private void buildModelCache(@NotNull String json, boolean override, @Nullable ResourceSupplier<ITexture> textureSupplier) {
        try {
            BlockModel model = BlockModel.fromString(json);
            model.name = (this.lastModelSource != null) ? this.lastModelSource : "fancymenu_json_model";
            model.resolveParents(this::resolveParentModel);

            TextureData overrideTexture = null;
            if (override) {
                overrideTexture = buildOverrideTexture(textureSupplier);
                if (overrideTexture == null) {
                    return;
                }
                this.cachedOverrideSprite = overrideTexture.sprite;
            }

            final TextureData overrideTextureFinal = overrideTexture;
            java.util.function.Function<Material, TextureAtlasSprite> spriteGetter = material -> {
                if (overrideTextureFinal != null) {
                    return overrideTextureFinal.sprite;
                }
                return material.sprite();
            };

            BlockModel bakeModel = model;
            if (bakeModel.getRootModel() == ModelBakery.GENERATION_MARKER) {
                bakeModel = ITEM_MODEL_GENERATOR.generateBlockModel(spriteGetter, model);
            }

            this.cachedModel = bakeBlockModel(bakeModel, overrideTextureFinal, spriteGetter);
            this.cachedRenderTexture = (overrideTextureFinal != null) ? overrideTextureFinal.renderLocation : TextureAtlas.LOCATION_BLOCKS;

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to load JSON model element!", ex);
        }
    }

    @Nullable
    private BlockModel resolveParentModel(@NotNull ResourceLocation location) {
        if (this.parentModelCache.containsKey(location)) {
            return this.parentModelCache.get(location);
        }

        try {
            BlockModel model;
            String path = location.getPath();
            if ("builtin/generated".equals(path)) {
                model = ModelBakery.GENERATION_MARKER;
            } else if ("builtin/entity".equals(path)) {
                model = ModelBakery.BLOCK_ENTITY_MARKER;
            } else if ("builtin/missing".equals(path)) {
                model = BlockModel.fromString(ModelBakery.MISSING_MODEL_MESH);
            } else {
                ResourceLocation fileLocation = ModelBakery.MODEL_LISTER.idToFile(location);
                try (var stream = Minecraft.getInstance().getResourceManager().open(fileLocation)) {
                    model = BlockModel.fromStream(new java.io.InputStreamReader(stream));
                }
            }
            model.name = location.toString();
            this.parentModelCache.put(location, model);
            return model;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to resolve model parent: " + location, ex);
        }

        return null;
    }

    @Nullable
    private TextureData buildOverrideTexture(@Nullable ResourceSupplier<ITexture> supplier) {
        if (supplier == null) {
            return null;
        }
        ITexture texture = supplier.get();
        if (texture == null || !texture.isReady()) {
            return null;
        }
        ResourceLocation location = texture.getResourceLocation();
        if (location == null) {
            return null;
        }
        int width = Math.max(1, texture.getWidth());
        int height = Math.max(1, texture.getHeight());
        return new TextureData(location, new ModelTextureSprite(location, width, height));
    }

    private BakedModel bakeBlockModel(@NotNull BlockModel model, @Nullable TextureData overrideTexture, @NotNull java.util.function.Function<Material, TextureAtlasSprite> spriteGetter) {
        SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(model, ItemOverrides.EMPTY, true);

        TextureAtlasSprite particleSprite = overrideTexture != null ? overrideTexture.sprite : spriteGetter.apply(model.getMaterial("particle"));
        builder.particle(particleSprite);

        Map<String, TextureAtlasSprite> spriteCache = new HashMap<>();

        for (BlockElement element : model.getElements()) {
            for (Map.Entry<net.minecraft.core.Direction, BlockElementFace> entry : element.faces.entrySet()) {
                BlockElementFace face = entry.getValue();
                TextureAtlasSprite sprite = overrideTexture != null ? overrideTexture.sprite : spriteCache.computeIfAbsent(face.texture(), key -> {
                    Material material = model.getMaterial(key);
                    return spriteGetter.apply(material);
                });
                BakedQuad quad = FACE_BAKERY.bakeQuad(
                        element.from,
                        element.to,
                        face,
                        sprite,
                        entry.getKey(),
                        BlockModelRotation.X0_Y0,
                        element.rotation,
                        element.shade
                );
                builder.addUnculledFace(quad);
            }
        }

        return builder.build();
    }

    private record TextureData(@NotNull ResourceLocation renderLocation, @NotNull ModelTextureSprite sprite) {
    }

    private static final class ModelTextureSprite extends TextureAtlasSprite implements AutoCloseable {

        private ModelTextureSprite(@NotNull ResourceLocation textureLocation, int width, int height) {
            super(textureLocation,
                    new SpriteContents(textureLocation, new net.minecraft.client.resources.metadata.animation.FrameSize(width, height),
                            new com.mojang.blaze3d.platform.NativeImage(width, height, true), ResourceMetadata.EMPTY),
                    width,
                    height,
                    0,
                    0);
        }

        @Override
        public void close() {
            this.contents().close();
        }
    }

}
