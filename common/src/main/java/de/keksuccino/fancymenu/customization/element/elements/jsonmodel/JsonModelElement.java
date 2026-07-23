package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.awt.Color;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JsonModelElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final ModelParentChainResolver<Identifier, UnbakedModel> PARENT_CHAIN_RESOLVER = new ModelParentChainResolver<>();

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
    private IText lastModelResource = null;
    @Nullable
    private ITexture lastTextureResource = null;
    @Nullable
    private Identifier lastOverrideTextureLocation = null;
    private int lastOverrideTextureWidth = 0;
    private int lastOverrideTextureHeight = 0;
    private boolean lastOverrideTextureReady = false;
    private boolean lastOverrideTextureReadFailed = false;
    private boolean lastModelResourceReadFailed = false;
    private boolean lastUseTextureOverride = false;
    private long lastParentResourceGeneration = JsonModelResourceReloadGeneration.current();
    private long modelInputRevision = 0L;
    @Nullable
    private BakedJsonModel cachedModel = null;
    @Nullable
    private Identifier cachedRenderTexture = null;
    @Nullable
    private ModelTextureSprite cachedOverrideSprite = null;
    private final Map<Identifier, CuboidModel> parentModelCache = new HashMap<>();
    private final ModelBuildAttemptTracker modelBuildAttempts = new ModelBuildAttemptTracker();

    public JsonModelElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.setSupportsRotation(false);
        this.setSupportsTilting(false);
    }

    @Override
    public void onDestroyElement() {
        super.onDestroyElement();
        this.invalidateCache(true);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

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

        PoseStack pose = new PoseStack();
        pose.pushPose();
        pose.translate(centerX, centerY, 200.0F);
        pose.translate(this.modelOffsetX.getFloat(), this.modelOffsetY.getFloat(), this.modelOffsetZ.getFloat());
        pose.scale(scale, scale, scale);

        if (this.useModelDisplayTransform.getBoolean()) {
            applyDisplayTransform(this.cachedModel.transforms().getTransform(ItemDisplayContext.GUI), pose, false);
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

        RenderType renderType = this.renderTranslucent.getBoolean()
                ? RenderTypes.entityTranslucent(this.cachedRenderTexture)
                : RenderTypes.entityCutout(this.cachedRenderTexture);

        SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
        QuadCollection quads = this.cachedModel.quads();
        float red = r;
        float green = g;
        float blue = b;
        float alpha = a;
        submitNodeStorage.submitCustomGeometry(pose, renderType, (modelPose, consumer) -> renderModelQuads(quads, modelPose, consumer, red, green, blue, alpha));
        Minecraft.getInstance().gameRenderer.featureRenderDispatcher().renderAllFeatures(submitNodeStorage);

        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
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
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
    }

    private static void applyDisplayTransform(@NotNull ItemTransform transform, @NotNull PoseStack poseStack, boolean leftHand) {
        if (transform == ItemTransform.NO_TRANSFORM) {
            return;
        }
        float rotationX = transform.rotation().x();
        float rotationY = transform.rotation().y();
        float rotationZ = transform.rotation().z();
        if (leftHand) {
            rotationY = -rotationY;
            rotationZ = -rotationZ;
        }
        int side = leftHand ? -1 : 1;
        poseStack.translate(side * transform.translation().x(), transform.translation().y(), transform.translation().z());
        poseStack.mulPose(new Quaternionf().rotationXYZ(
                rotationX * ((float) Math.PI / 180.0F),
                rotationY * ((float) Math.PI / 180.0F),
                rotationZ * ((float) Math.PI / 180.0F)
        ));
        poseStack.scale(transform.scale().x(), transform.scale().y(), transform.scale().z());
    }

    private static void renderModelQuads(@NotNull QuadCollection quads, @NotNull PoseStack.Pose pose, @NotNull VertexConsumer consumer, float r, float g, float b, float a) {
        QuadInstance instance = new QuadInstance();
        instance.setColor(ARGB.colorFromFloat(a, r, g, b));
        instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
        instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            List<BakedQuad> directionQuads = quads.getQuads(direction);
            for (BakedQuad quad : directionQuads) {
                consumer.putBakedQuad(pose, quad, instance);
            }
        }
        for (BakedQuad quad : quads.getQuads(null)) {
            consumer.putBakedQuad(pose, quad, instance);
        }
    }

    private void ensureModelCache() {
        ResourceSupplier<IText> modelSupplier = this.modelSource.get();
        String modelKey = (modelSupplier != null) ? modelSupplier.getSourceWithPrefix() : null;
        IText modelResource = null;
        List<String> modelLines = null;
        RuntimeException modelResourceReadFailure = null;
        try {
            modelResource = (modelSupplier != null) ? modelSupplier.get() : null;
            if (modelResource != null && modelResource.isReady()) modelLines = modelResource.getTextLines();
        } catch (RuntimeException ex) {
            modelResourceReadFailure = ex;
        }
        boolean modelResourceReadFailed = modelResourceReadFailure != null;

        boolean override = this.useTextureOverride.getBoolean();
        ResourceSupplier<ITexture> textureSupplier = override ? this.textureSource.get() : null;
        String textureKey = (textureSupplier != null) ? textureSupplier.getSourceWithPrefix() : null;

        ITexture textureResource = null;
        Identifier overrideTextureLocation = null;
        int overrideTextureWidth = 0;
        int overrideTextureHeight = 0;
        boolean overrideTextureReady = false;
        RuntimeException overrideTextureReadFailure = null;
        try {
            textureResource = (textureSupplier != null) ? textureSupplier.get() : null;
            if (textureResource != null && textureResource.isReady()) {
                overrideTextureLocation = textureResource.getResourceLocation();
                if (overrideTextureLocation != null) {
                    overrideTextureWidth = Math.max(1, textureResource.getWidth());
                    overrideTextureHeight = Math.max(1, textureResource.getHeight());
                    overrideTextureReady = true;
                }
            }
        } catch (RuntimeException ex) {
            overrideTextureReadFailure = ex;
        }
        boolean overrideTextureReadFailed = overrideTextureReadFailure != null;

        long parentResourceGeneration = JsonModelResourceReloadGeneration.current();
        boolean modelSourceChanged = !Objects.equals(modelKey, this.lastModelSource);
        boolean modelResourceChanged = modelResource != this.lastModelResource;
        boolean modelReadStateChanged = modelResourceReadFailed != this.lastModelResourceReadFailed;
        boolean textureSourceChanged = !Objects.equals(textureKey, this.lastTextureSource);
        boolean textureResourceChanged = textureResource != this.lastTextureResource;
        boolean textureRenderLocationChanged = ModelOverrideTextureChangeDetector.hasRenderLocationChanged(overrideTextureLocation, this.lastOverrideTextureLocation);
        boolean textureStateChanged = ModelOverrideTextureChangeDetector.requiresModelRebuild(textureResource, this.lastTextureResource, overrideTextureWidth, this.lastOverrideTextureWidth, overrideTextureHeight, this.lastOverrideTextureHeight, overrideTextureReady, this.lastOverrideTextureReady, overrideTextureReadFailed, this.lastOverrideTextureReadFailed);
        boolean overrideChanged = override != this.lastUseTextureOverride;
        boolean parentResourcesChanged = parentResourceGeneration != this.lastParentResourceGeneration;
        boolean inputStateChanged = modelSourceChanged || modelResourceChanged || modelReadStateChanged || textureSourceChanged || textureStateChanged || overrideChanged || parentResourcesChanged;
        boolean newModelResourceReadFailure = modelResourceReadFailed && (!this.lastModelResourceReadFailed || modelSourceChanged || modelResourceChanged || parentResourcesChanged);
        boolean newOverrideTextureReadFailure = overrideTextureReadFailed && (!this.lastOverrideTextureReadFailed || textureSourceChanged || textureResourceChanged || textureStateChanged || parentResourcesChanged);

        if (inputStateChanged) {
            this.modelInputRevision++;
            this.invalidateCache(modelSourceChanged || modelResourceChanged || parentResourcesChanged);
            this.lastModelSource = modelKey;
            this.lastTextureSource = textureKey;
            this.lastModelResource = modelResource;
            this.lastTextureResource = textureResource;
            this.lastOverrideTextureLocation = overrideTextureLocation;
            this.lastOverrideTextureWidth = overrideTextureWidth;
            this.lastOverrideTextureHeight = overrideTextureHeight;
            this.lastOverrideTextureReady = overrideTextureReady;
            this.lastOverrideTextureReadFailed = overrideTextureReadFailed;
            this.lastModelResourceReadFailed = modelResourceReadFailed;
            this.lastUseTextureOverride = override;
            this.lastParentResourceGeneration = parentResourceGeneration;
        } else if (textureRenderLocationChanged) {
            // Animated textures rotate locations as frames advance; the baked full-texture UVs remain valid.
            this.lastOverrideTextureLocation = overrideTextureLocation;
            if (this.cachedModel != null && overrideTextureReady) this.cachedRenderTexture = overrideTextureLocation;
        }
        if (newModelResourceReadFailure) LOGGER.error("[FANCYMENU] Failed to inspect JSON model source '{}'; the element will retry when the resource becomes readable", modelKey, modelResourceReadFailure);
        if (newOverrideTextureReadFailure) LOGGER.error("[FANCYMENU] Failed to inspect JSON model override texture '{}'; the element will retry when the resource becomes readable", textureKey, overrideTextureReadFailure);

        ModelBuildAttemptTracker.Observation observation;
        try {
            observation = this.modelBuildAttempts.observe(this.modelInputRevision, modelLines);
        } catch (RuntimeException ignored) {
            // A custom asynchronous IText may mutate its list during comparison/copying. Retry once it publishes a stable view.
            return;
        }
        if (observation.contentChanged()) {
            if (inputStateChanged) this.parentModelCache.clear();
            else this.invalidateCache(true);
        }

        if (modelResourceReadFailed || overrideTextureReadFailed || this.cachedModel != null || !observation.hasContent() || (override && !overrideTextureReady)) return;
        if (!this.modelBuildAttempts.beginAttempt()) return;

        OverrideTextureInput overrideTextureInput = override ? new OverrideTextureInput(Objects.requireNonNull(overrideTextureLocation), overrideTextureWidth, overrideTextureHeight) : null;
        this.buildModelCache(overrideTextureInput);
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
        }
    }

    private void buildModelCache(@Nullable OverrideTextureInput overrideTextureInput) {
        TextureData overrideTexture = null;
        boolean overrideTextureTransferred = false;
        try {
            String json = this.modelBuildAttempts.modelJson();
            CuboidModel model = CuboidModel.fromStream(new StringReader(json));
            ModelParentChainResolver.Resolution<Identifier, UnbakedModel> resolution = this.collectModelChain(model);
            if (!resolution.isUsable()) {
                this.logParentChainFailure(resolution);
                return;
            }

            if (overrideTextureInput != null) {
                ModelTextureSprite sprite = new ModelTextureSprite(overrideTextureInput.location(), overrideTextureInput.width(), overrideTextureInput.height());
                overrideTexture = new TextureData(overrideTextureInput.location(), sprite);
            }

            BakedJsonModel bakedModel = this.bakeBlockModel(resolution.chain(), overrideTexture);
            this.cachedRenderTexture = overrideTexture != null ? overrideTexture.renderLocation() : TextureAtlas.LOCATION_BLOCKS;
            if (overrideTexture != null) {
                this.cachedOverrideSprite = overrideTexture.sprite();
                overrideTextureTransferred = true;
            }
            this.cachedModel = bakedModel;
        } catch (Exception ex) {
            String source = this.lastModelSource != null ? this.lastModelSource : "fancymenu_json_model";
            LOGGER.error("[FANCYMENU] Failed to load JSON model element '{}'", source, ex);
        } finally {
            if (overrideTexture != null && !overrideTextureTransferred) overrideTexture.sprite().close();
        }
    }

    @Nullable
    private CuboidModel resolveParentModel(@NotNull Identifier location) {
        if (this.parentModelCache.containsKey(location)) {
            return this.parentModelCache.get(location);
        }

        try {
            Identifier fileLocation = location.withPath(path -> "models/" + path + ".json");
            CuboidModel model;
            try (var stream = Minecraft.getInstance().getResourceManager().open(fileLocation);
                 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                model = CuboidModel.fromStream(reader);
            }
            this.parentModelCache.put(location, model);
            return model;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to resolve model parent: " + location, ex);
        }

        return null;
    }

    private BakedJsonModel bakeBlockModel(@NotNull List<UnbakedModel> chain, @Nullable TextureData overrideTexture) {
        ModelDebugName debugName = () -> (this.lastModelSource != null) ? this.lastModelSource : "fancymenu_json_model";

        TextureSlots.Resolver textureResolver = new TextureSlots.Resolver();
        for (UnbakedModel chainModel : chain) {
            textureResolver.addLast(chainModel.textureSlots());
        }
        TextureSlots textureSlots = textureResolver.resolve(debugName);

        UnbakedGeometry geometry = findTopGeometry(chain);
        SpriteGetter spriteGetter = createSpriteGetter(overrideTexture);
        ModelBaker baker = createModelBaker(spriteGetter);
        QuadCollection quads = geometry.bake(textureSlots, baker, BlockModelRotation.IDENTITY, debugName);

        return new BakedJsonModel(quads, findTopTransforms(chain));
    }

    @NotNull
    private ModelParentChainResolver.Resolution<Identifier, UnbakedModel> collectModelChain(@NotNull CuboidModel model) {
        return PARENT_CHAIN_RESOLVER.resolve(model, UnbakedModel::parent, this::resolveParentForChain, parent -> parent.getPath().startsWith("builtin/"));
    }

    @Nullable
    private UnbakedModel resolveParentForChain(@NotNull Identifier location) {
        if (ItemModelGenerator.GENERATED_ITEM_MODEL_ID.equals(location)) return ITEM_MODEL_GENERATOR;
        return this.resolveParentModel(location);
    }

    private void logParentChainFailure(@NotNull ModelParentChainResolver.Resolution<Identifier, UnbakedModel> resolution) {
        String source = this.lastModelSource != null ? this.lastModelSource : "fancymenu_json_model";
        if (resolution.status() == ModelParentChainResolver.Status.CYCLE) {
            LOGGER.error("[FANCYMENU] JSON model '{}' has a cyclic parent chain: {}", source, formatIdentifierPath(resolution.diagnosticPath()));
            return;
        }
        if (resolution.status() == ModelParentChainResolver.Status.DEPTH_LIMIT) {
            List<Identifier> resolvedPath = resolution.diagnosticPath();
            LOGGER.error("[FANCYMENU] JSON model '{}' exceeds the maximum parent depth of {}. Resolved path: {}. Refusing to resolve next parent: {}", source, ModelParentChainResolver.DEFAULT_MAX_PARENT_DEPTH, formatIdentifierPath(resolvedPath), resolution.nextParent());
        }
    }

    @NotNull
    private static String formatIdentifierPath(@NotNull List<Identifier> path) {
        StringBuilder builder = new StringBuilder();
        for (Identifier identifier : path) {
            if (!builder.isEmpty()) builder.append(" -> ");
            builder.append(identifier);
        }
        return builder.toString();
    }

    private UnbakedGeometry findTopGeometry(@NotNull List<UnbakedModel> chain) {
        for (UnbakedModel model : chain) {
            UnbakedGeometry geometry = model.geometry();
            if (geometry != null) {
                return geometry;
            }
        }
        return UnbakedGeometry.EMPTY;
    }

    private ItemTransforms findTopTransforms(@NotNull List<UnbakedModel> chain) {
        return new ItemTransforms(
                findTopTransform(chain, ItemDisplayContext.THIRD_PERSON_LEFT_HAND),
                findTopTransform(chain, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND),
                findTopTransform(chain, ItemDisplayContext.FIRST_PERSON_LEFT_HAND),
                findTopTransform(chain, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND),
                findTopTransform(chain, ItemDisplayContext.HEAD),
                findTopTransform(chain, ItemDisplayContext.GUI),
                findTopTransform(chain, ItemDisplayContext.GROUND),
                findTopTransform(chain, ItemDisplayContext.FIXED),
                findTopTransform(chain, ItemDisplayContext.ON_SHELF)
        );
    }

    private ItemTransform findTopTransform(@NotNull List<UnbakedModel> chain, @NotNull ItemDisplayContext context) {
        for (UnbakedModel model : chain) {
            ItemTransforms transforms = model.transforms();
            if (transforms == null) {
                continue;
            }
            ItemTransform transform = transforms.getTransform(context);
            if (transform != ItemTransform.NO_TRANSFORM) {
                return transform;
            }
        }
        return ItemTransform.NO_TRANSFORM;
    }

    private SpriteGetter createSpriteGetter(@Nullable TextureData overrideTexture) {
        return new SpriteGetter() {
            @Override
            public TextureAtlasSprite get(SpriteId id) {
                if (overrideTexture != null) {
                    return overrideTexture.sprite();
                }
                try {
                    return Minecraft.getInstance().getAtlasManager().get(id);
                } catch (Exception ex) {
                    LOGGER.warn("[FANCYMENU] Failed to resolve JSON model texture: {}", id, ex);
                    return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).missingSprite();
                }
            }
        };
    }

    private ModelBaker createModelBaker(@NotNull SpriteGetter spriteGetter) {
        return new ModelBaker() {
            private final Interner interner = new Interner() {
                @Override
                public Vector3fc vector(Vector3fc vector) {
                    return new Vector3f(vector);
                }

                @Override
                public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material) {
                    return material;
                }
            };
            private final TextureAtlasSprite missingBlockSprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).missingSprite();
            private final MaterialBaker materialBaker = new MaterialBaker(this.missingBlockSprite) {
                @Override
                protected Material.Baked bake(Material material) {
                    TextureAtlasSprite itemSprite = spriteGetter.get(new SpriteId(TextureAtlas.LOCATION_ITEMS, material.sprite()));
                    TextureAtlasSprite missingItemSprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
                    if (itemSprite != missingItemSprite) {
                        return new Material.Baked(itemSprite, material.forceTranslucent());
                    }
                    return new Material.Baked(spriteGetter.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, material.sprite())), material.forceTranslucent());
                }

                @Override
                public Material.Baked reportMissingReference(String reference, ModelDebugName name) {
                    return new Material.Baked(missingBlockSprite, false);
                }
            };

            @Override
            public ResolvedModel getModel(Identifier identifier) {
                throw new UnsupportedOperationException("JSON model element bakes resolved parents before geometry");
            }

            @Override
            public BlockStateModelPart missingBlockModelPart() {
                throw new UnsupportedOperationException("JSON model element does not use block model parts");
            }

            @Override
            public MaterialBaker materials() {
                return this.materialBaker;
            }

            @Override
            public Interner interner() {
                return this.interner;
            }

            @Override
            public <T> T compute(SharedOperationKey<T> key) {
                return key.compute(this);
            }
        };
    }

    private record BakedJsonModel(@NotNull QuadCollection quads, @NotNull ItemTransforms transforms) {
    }

    private record TextureData(@NotNull Identifier renderLocation, @NotNull ModelTextureSprite sprite) {
    }

    private record OverrideTextureInput(@NotNull Identifier location, int width, int height) {
    }

    private static final class ModelTextureSprite extends TextureAtlasSprite implements AutoCloseable {

        private ModelTextureSprite(@NotNull Identifier textureLocation, int width, int height) {
            super(textureLocation,
                    new SpriteContents(textureLocation, new FrameSize(width, height),
                            new com.mojang.blaze3d.platform.NativeImage(width, height, true)),
                    width,
                    height,
                    0,
                    0,
                    0);
        }

        @Override
        public void close() {
            this.contents().close();
        }
    }

}
