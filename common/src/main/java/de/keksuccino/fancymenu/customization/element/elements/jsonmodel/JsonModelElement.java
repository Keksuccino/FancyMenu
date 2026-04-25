package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.opengl.GlStateManager;
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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.world.item.ItemDisplayContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.Color;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JsonModelElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();
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
    private BakedJsonModel cachedModel = null;
    @Nullable
    private Identifier cachedRenderTexture = null;
    @Nullable
    private ModelTextureSprite cachedOverrideSprite = null;
    private final Map<Identifier, BlockModel> parentModelCache = new HashMap<>();

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

        GlStateManager._enableBlend();
        GlStateManager._enableDepthTest();
        GlStateManager._disableCull();

        RenderType renderType = this.renderTranslucent.getBoolean()
                ? RenderTypes.entityTranslucent(this.cachedRenderTexture)
                : RenderTypes.entityCutoutNoCull(this.cachedRenderTexture);

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(renderType);
        renderModelQuads(this.cachedModel.quads(), pose, consumer, r, g, b, a);
        buffer.endBatch();

        GlStateManager._enableCull();
        GlStateManager._disableDepthTest();
        GlStateManager._disableBlend();

        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
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
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
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

    private void renderModelQuads(@NotNull QuadCollection quads, @NotNull PoseStack poseStack, @NotNull VertexConsumer consumer, float r, float g, float b, float a) {
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        PoseStack.Pose pose = poseStack.last();

        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            List<BakedQuad> directionQuads = quads.getQuads(direction);
            for (BakedQuad quad : directionQuads) {
                consumer.putBulkData(pose, quad, r, g, b, a, light, overlay);
            }
        }
        for (BakedQuad quad : quads.getQuads(null)) {
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
            BlockModel model = BlockModel.fromStream(new StringReader(json));

            TextureData overrideTexture = null;
            if (override) {
                overrideTexture = buildOverrideTexture(textureSupplier);
                if (overrideTexture == null) {
                    return;
                }
                this.cachedOverrideSprite = overrideTexture.sprite;
            }

            final TextureData overrideTextureFinal = overrideTexture;
            this.cachedModel = bakeBlockModel(model, overrideTextureFinal);
            this.cachedRenderTexture = (overrideTextureFinal != null) ? overrideTextureFinal.renderLocation : TextureAtlas.LOCATION_BLOCKS;

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to load JSON model element!", ex);
        }
    }

    @Nullable
    private BlockModel resolveParentModel(@NotNull Identifier location) {
        if (this.parentModelCache.containsKey(location)) {
            return this.parentModelCache.get(location);
        }

        try {
            Identifier fileLocation = location.withPath(path -> "models/" + path + ".json");
            BlockModel model;
            try (var stream = Minecraft.getInstance().getResourceManager().open(fileLocation);
                 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                model = BlockModel.fromStream(reader);
            }
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
        Identifier location = texture.getResourceLocation();
        if (location == null) {
            return null;
        }
        int width = Math.max(1, texture.getWidth());
        int height = Math.max(1, texture.getHeight());
        return new TextureData(location, new ModelTextureSprite(location, width, height));
    }

    private BakedJsonModel bakeBlockModel(@NotNull BlockModel model, @Nullable TextureData overrideTexture) {
        List<UnbakedModel> chain = collectModelChain(model);
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

    private List<UnbakedModel> collectModelChain(@NotNull BlockModel model) {
        List<UnbakedModel> chain = new ArrayList<>();
        UnbakedModel current = model;
        while (current != null) {
            chain.add(current);
            Identifier parent = current.parent();
            if (parent == null) {
                break;
            }
            if (ItemModelGenerator.GENERATED_ITEM_MODEL_ID.equals(parent)) {
                chain.add(ITEM_MODEL_GENERATOR);
                break;
            }
            if (parent.getPath().startsWith("builtin/")) {
                break;
            }
            current = resolveParentModel(parent);
        }
        return chain;
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
            public TextureAtlasSprite get(Material material, ModelDebugName debugName) {
                if (overrideTexture != null) {
                    return overrideTexture.sprite;
                }
                try {
                    return Minecraft.getInstance().getAtlasManager().get(material);
                } catch (Exception ex) {
                    LOGGER.warn("[FANCYMENU] Failed to resolve JSON model texture: {}", material, ex);
                    return this.reportMissingReference(material.texture().toString(), debugName);
                }
            }

            @Override
            public TextureAtlasSprite reportMissingReference(String name, ModelDebugName debugName) {
                return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).missingSprite();
            }
        };
    }

    private ModelBaker createModelBaker(@NotNull SpriteGetter spriteGetter) {
        return new ModelBaker() {
            private final PartCache partCache = vector3fc -> new Vector3f(vector3fc);

            @Override
            public net.minecraft.client.resources.model.ResolvedModel getModel(Identifier identifier) {
                throw new UnsupportedOperationException("JSON model element bakes resolved parents before geometry");
            }

            @Override
            public net.minecraft.client.renderer.block.model.BlockModelPart missingBlockModelPart() {
                throw new UnsupportedOperationException("JSON model element does not use block model parts");
            }

            @Override
            public SpriteGetter sprites() {
                return spriteGetter;
            }

            @Override
            public PartCache parts() {
                return this.partCache;
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
