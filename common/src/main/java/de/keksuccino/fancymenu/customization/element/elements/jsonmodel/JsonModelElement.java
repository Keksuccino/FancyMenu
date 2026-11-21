package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
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
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private ResourceSupplier<IText> lastModelSupplier = null;
    @Nullable
    private ResourceSupplier<ITexture> lastTextureSupplier = null;
    @Nullable
    private IText lastModelResource = null;
    @Nullable
    private ITexture lastTextureResource = null;
    @Nullable
    private ResourceLocation lastOverrideTextureLocation = null;
    private int lastOverrideTextureWidth = 0;
    private int lastOverrideTextureHeight = 0;
    private boolean lastOverrideTextureReady = false;
    private boolean lastOverrideTextureReadFailed = false;
    private boolean lastModelResourceReadFailed = false;
    private boolean lastUseTextureOverride = false;
    private long modelInputRevision = 0L;
    private final Map<ResourceLocation, BlockModel> parentModelCache = new HashMap<>();
    private final ModelBuildAttemptTracker modelBuildAttempts = new ModelBuildAttemptTracker();
    private final ModelCacheLifecycle<JsonModelCache> modelCache = new ModelCacheLifecycle<>();

    public JsonModelElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.setSupportsRotation(false);
        this.setSupportsTilting(false);
    }

    @Override
    public void onDestroyElement() {
        super.onDestroyElement();
        this.modelCache.destroy();
        this.parentModelCache.clear();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) {
            return;
        }

        this.ensureModelCache();

        JsonModelCache cache = this.modelCache.current();
        if (cache == null) {
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
            cache.model().getTransforms().getTransform(ItemTransforms.TransformType.GUI).apply(false, pose);
        }

        pose.mulPose(Quaternion.fromXYZ(
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
                ? RenderType.entityTranslucent(cache.renderTexture())
                : RenderType.entityCutoutNoCull(cache.renderTexture());

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(renderType);
        renderModelQuads(cache.model(), pose, consumer, r, g, b, a);
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

        Quaternion rotation = Quaternion.fromXYZ(
                (float) Math.toRadians(this.lightRotationX.getFloat()),
                (float) Math.toRadians(this.lightRotationY.getFloat()),
                (float) Math.toRadians(this.lightRotationZ.getFloat())
        );
        light0.transform(rotation);
        light1.transform(rotation);
        light0.normalize();
        light1.normalize();
        RenderSystem.setShaderLights(light0, light1);
    }

    private void renderModelQuads(@NotNull BakedModel model, @NotNull PoseStack poseStack, @NotNull VertexConsumer consumer, float r, float g, float b, float a) {
        RandomSource random = RandomSource.create();
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        PoseStack.Pose pose = poseStack.last();

        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            List<BakedQuad> quads = model.getQuads(null, direction, random);
            for (BakedQuad quad : quads) {
                consumer.putBulkData(pose, quad, r, g, b, light, overlay);
            }
        }
        random.setSeed(42L);
        List<BakedQuad> quads = model.getQuads(null, null, random);
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, r, g, b, light, overlay);
        }
    }

    private void ensureModelCache() {
        if (this.modelCache.isDestroyed()) return;

        ResourceSupplier<IText> modelSupplier = null;
        String modelKey = null;
        IText modelResource = null;
        List<String> modelLines = null;
        RuntimeException modelReadFailure = null;
        try {
            modelSupplier = this.modelSource.get();
            if (modelSupplier != null) {
                modelKey = modelSupplier.getSourceWithPrefix();
                modelResource = modelSupplier.get();
                if (modelResource != null && modelResource.isReady()) modelLines = modelResource.getTextLines();
            }
        } catch (RuntimeException ex) {
            modelReadFailure = ex;
        }
        boolean modelReadFailed = modelReadFailure != null;

        boolean override = this.useTextureOverride.getBoolean();
        ResourceSupplier<ITexture> textureSupplier = null;
        String textureKey = null;
        ITexture textureResource = null;
        ResourceLocation textureLocation = null;
        int textureWidth = 0;
        int textureHeight = 0;
        boolean textureReady = false;
        RuntimeException textureReadFailure = null;
        try {
            if (override) {
                textureSupplier = this.textureSource.get();
                if (textureSupplier != null) {
                    textureKey = textureSupplier.getSourceWithPrefix();
                    textureResource = textureSupplier.get();
                    if (textureResource != null && textureResource.isReady()) {
                        textureLocation = textureResource.getResourceLocation();
                        if (textureLocation != null) {
                            textureWidth = Math.max(1, textureResource.getWidth());
                            textureHeight = Math.max(1, textureResource.getHeight());
                            textureReady = true;
                        }
                    }
                }
            }
        } catch (RuntimeException ex) {
            textureReadFailure = ex;
        }
        boolean textureReadFailed = textureReadFailure != null;

        boolean modelSupplierChanged = modelSupplier != this.lastModelSupplier;
        boolean modelSourceChanged = !Objects.equals(modelKey, this.lastModelSource);
        boolean modelResourceChanged = modelResource != this.lastModelResource;
        boolean modelReadStateChanged = modelReadFailed != this.lastModelResourceReadFailed;
        boolean textureSupplierChanged = textureSupplier != this.lastTextureSupplier;
        boolean textureSourceChanged = !Objects.equals(textureKey, this.lastTextureSource);
        boolean textureResourceChanged = textureResource != this.lastTextureResource;
        boolean textureLocationChanged = !Objects.equals(textureLocation, this.lastOverrideTextureLocation);
        boolean textureStateChanged = textureResourceChanged || textureWidth != this.lastOverrideTextureWidth || textureHeight != this.lastOverrideTextureHeight || textureReady != this.lastOverrideTextureReady || textureReadFailed != this.lastOverrideTextureReadFailed;
        boolean overrideChanged = override != this.lastUseTextureOverride;
        boolean inputStateChanged = modelSupplierChanged || modelSourceChanged || modelResourceChanged || modelReadStateChanged || textureSupplierChanged || textureSourceChanged || textureStateChanged || overrideChanged;
        boolean newModelReadFailure = modelReadFailed && (!this.lastModelResourceReadFailed || modelSupplierChanged || modelSourceChanged || modelResourceChanged);
        boolean newTextureReadFailure = textureReadFailed && (!this.lastOverrideTextureReadFailed || textureSupplierChanged || textureSourceChanged || textureResourceChanged);

        if (inputStateChanged) {
            this.modelInputRevision++;
            this.modelCache.invalidate();
            if (modelSupplierChanged || modelSourceChanged || modelResourceChanged) this.parentModelCache.clear();
            this.lastModelSupplier = modelSupplier;
            this.lastTextureSupplier = textureSupplier;
            this.lastModelSource = modelKey;
            this.lastTextureSource = textureKey;
            this.lastModelResource = modelResource;
            this.lastTextureResource = textureResource;
            this.lastOverrideTextureLocation = textureLocation;
            this.lastOverrideTextureWidth = textureWidth;
            this.lastOverrideTextureHeight = textureHeight;
            this.lastOverrideTextureReady = textureReady;
            this.lastOverrideTextureReadFailed = textureReadFailed;
            this.lastModelResourceReadFailed = modelReadFailed;
            this.lastUseTextureOverride = override;
        } else if (textureLocationChanged) {
            this.lastOverrideTextureLocation = textureLocation;
            JsonModelCache cache = this.modelCache.current();
            if (cache != null && textureReady) cache.updateRenderTexture(textureLocation);
        }

        if (newModelReadFailure) LOGGER.error("[FANCYMENU] Failed to inspect JSON model source '{}'; the element will retry when the resource becomes readable", modelKey, modelReadFailure);
        if (newTextureReadFailure) LOGGER.error("[FANCYMENU] Failed to inspect JSON model override texture '{}'; the element will retry when the resource becomes readable", textureKey, textureReadFailure);

        ModelBuildAttemptTracker.Observation observation = this.modelBuildAttempts.observe(this.modelInputRevision, modelLines);
        if (observation.contentChanged() && !inputStateChanged) this.modelCache.invalidate();
        if (modelReadFailed || textureReadFailed || this.modelCache.current() != null || !observation.hasContent() || (override && !textureReady)) return;

        ModelBuildAttemptTracker.Attempt attempt = this.modelBuildAttempts.beginAttempt();
        if (attempt == null) return;
        OverrideTextureInput overrideTextureInput = override ? new OverrideTextureInput(Objects.requireNonNull(textureLocation), textureWidth, textureHeight) : null;
        this.buildModelCache(attempt, overrideTextureInput, modelKey);
    }

    private void buildModelCache(@NotNull ModelBuildAttemptTracker.Attempt attempt, @Nullable OverrideTextureInput overrideTextureInput, @Nullable String modelSource) {
        ModelCacheLifecycle.BuildToken buildToken = this.modelCache.beginBuild();
        if (buildToken == null) return;
        try (ModelBuildResourceScope resources = new ModelBuildResourceScope()) {
            BlockModel model = BlockModel.fromString(attempt.modelJson());
            model.name = modelSource != null ? modelSource : "fancymenu_json_model";
            model.getMaterials(this::resolveParentModel, new java.util.HashSet<>());

            TextureData overrideTexture = null;
            ModelTextureSprite overrideSprite = null;
            if (overrideTextureInput != null) {
                overrideSprite = resources.own(ModelTextureSprite.create(overrideTextureInput.location(), overrideTextureInput.width(), overrideTextureInput.height()));
                overrideTexture = new TextureData(overrideTextureInput.location(), overrideSprite);
            }

            TextureData resolvedOverrideTexture = overrideTexture;
            java.util.function.Function<Material, TextureAtlasSprite> spriteGetter = material -> resolvedOverrideTexture != null ? resolvedOverrideTexture.sprite : material.sprite();
            BlockModel bakeModel = model;
            if (bakeModel.getRootModel() == ModelBakery.GENERATION_MARKER) bakeModel = ITEM_MODEL_GENERATOR.generateBlockModel(spriteGetter, model);
            BakedModel bakedModel = bakeBlockModel(bakeModel, resolvedOverrideTexture, spriteGetter);
            ResourceLocation renderTexture = resolvedOverrideTexture != null ? resolvedOverrideTexture.renderLocation : TextureAtlas.LOCATION_BLOCKS;
            JsonModelCache candidate = new JsonModelCache(bakedModel, renderTexture, overrideSprite);
            if (overrideSprite != null) resources.replaceOwnership(overrideSprite, candidate);
            else resources.own(candidate);
            this.modelCache.publish(buildToken, resources.transfer(candidate));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to load JSON model element '{}'; the same input revision will not be retried", modelSource != null ? modelSource : "fancymenu_json_model", ex);
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
                ResourceLocation fileLocation = new ResourceLocation(location.getNamespace(), "models/" + location.getPath() + ".json");
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

    private BakedModel bakeBlockModel(@NotNull BlockModel model, @Nullable TextureData overrideTexture, @NotNull java.util.function.Function<Material, TextureAtlasSprite> spriteGetter) {
        SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(model, ItemOverrides.EMPTY, true);

        TextureAtlasSprite particleSprite = overrideTexture != null ? overrideTexture.sprite : spriteGetter.apply(model.getMaterial("particle"));
        builder.particle(particleSprite);

        Map<String, TextureAtlasSprite> spriteCache = new HashMap<>();

        for (BlockElement element : model.getElements()) {
            for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                BlockElementFace face = entry.getValue();
                TextureAtlasSprite sprite = overrideTexture != null ? overrideTexture.sprite : spriteCache.computeIfAbsent(face.texture, key -> {
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
                        element.shade,
                        new ResourceLocation("fancymenu", "json_model_element")
                );
                builder.addUnculledFace(quad);
            }
        }

        return builder.build();
    }

    private record TextureData(@NotNull ResourceLocation renderLocation, @NotNull ModelTextureSprite sprite) {
    }

    private record OverrideTextureInput(@NotNull ResourceLocation location, int width, int height) {
    }

    private static final class JsonModelCache implements AutoCloseable {

        private final BakedModel model;
        private volatile ResourceLocation renderTexture;
        @Nullable
        private final ModelTextureSprite overrideSprite;
        private final AtomicBoolean closed = new AtomicBoolean();

        private JsonModelCache(@NotNull BakedModel model, @NotNull ResourceLocation renderTexture, @Nullable ModelTextureSprite overrideSprite) {
            this.model = Objects.requireNonNull(model);
            this.renderTexture = Objects.requireNonNull(renderTexture);
            this.overrideSprite = overrideSprite;
        }

        @NotNull
        private BakedModel model() {
            return this.model;
        }

        @NotNull
        private ResourceLocation renderTexture() {
            return this.renderTexture;
        }

        private void updateRenderTexture(@NotNull ResourceLocation renderTexture) {
            this.renderTexture = Objects.requireNonNull(renderTexture);
        }

        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true) && this.overrideSprite != null) this.overrideSprite.close();
        }

    }

    private static final class ModelTextureSprite extends TextureAtlasSprite implements AutoCloseable {

        private final AtomicBoolean closed = new AtomicBoolean();

        private ModelTextureSprite(@NotNull ResourceLocation textureLocation, int width, int height, @NotNull NativeImage image) {
            super(Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS),
                    new TextureAtlasSprite.Info(textureLocation, width, height, AnimationMetadataSection.EMPTY),
                    0,
                    width,
                    height,
                    0,
                    0,
                    image);
        }

        @NotNull
        private static ModelTextureSprite create(@NotNull ResourceLocation textureLocation, int width, int height) {
            try (ModelBuildResourceScope resources = new ModelBuildResourceScope()) {
                NativeImage image = resources.own(new NativeImage(width, height, true));
                ModelTextureSprite sprite = new ModelTextureSprite(textureLocation, width, height, image);
                resources.replaceOwnership(image, sprite);
                return resources.transfer(sprite);
            }
        }

        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true)) super.close();
        }
    }

}
