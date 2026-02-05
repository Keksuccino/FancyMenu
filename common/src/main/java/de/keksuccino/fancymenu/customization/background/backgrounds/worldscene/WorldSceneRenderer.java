package de.keksuccino.fancymenu.customization.background.backgrounds.worldscene;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.PalettedContainer.Strategy;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class WorldSceneRenderer {

    public static final String MEMORY_KEY = "world_scene_renderer";

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Object REGISTRY_LOCK = new Object();
    private static @Nullable RegistryAccess.Frozen FALLBACK_REGISTRY_ACCESS;

    private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(
            Block.BLOCK_STATE_REGISTRY,
            BlockState.CODEC,
            Strategy.SECTION_STATES,
            Blocks.AIR.defaultBlockState()
    );

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Executor ioExecutor = net.minecraft.Util.backgroundExecutor();
    private final AtomicInteger loadToken = new AtomicInteger();

    private @Nullable CompletableFuture<LoadResult> loadingFuture;
    private @Nullable LoadKey pendingKey;
    private @Nullable SceneContext context;
    private SceneState state = SceneState.UNLOADED;
    private @Nullable String stateMessage;

    public SceneState getState() {
        return this.state;
    }

    public void ensureLoaded(@Nullable Path levelDatPath, int chunkRadius, int centerChunkX, int centerChunkZ) {
        if (levelDatPath == null) {
            setState(SceneState.UNLOADED, "No level.dat selected");
            disposeContext();
            return;
        }
        LoadKey key = new LoadKey(levelDatPath.toAbsolutePath().normalize(), Math.max(1, chunkRadius), centerChunkX, centerChunkZ);
        if (this.context != null && this.context.key.equals(key)) {
            return;
        }
        if (this.pendingKey != null && this.pendingKey.equals(key) && this.loadingFuture != null && !this.loadingFuture.isDone()) {
            return;
        }
        startLoad(key);
    }

    public void renderPlaceholder(@NotNull GuiGraphics graphics, @NotNull SceneState state, float opacity) {
        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();
        int alpha = Mth.clamp((int)(opacity * 255.0F), 0, 255);
        int baseColor = (alpha << 24) | 0x00101010;
        graphics.fill(0, 0, width, height, baseColor);

        String text = switch (state) {
            case ERROR -> this.stateMessage != null ? this.stateMessage : "World scene failed to load";
            case LOADING -> "Loading world scene...";
            case UNLOADED -> "No world selected";
            case READY -> "";
        };
        if (!text.isEmpty()) {
            graphics.drawCenteredString(this.minecraft.font, text, width / 2, height / 2, 0xFFFFFF);
        }
    }

    public void renderScene(@NotNull GuiGraphics graphics, float partial, @NotNull SceneSettings settings) {
        if (this.context == null) return;

        applyTimeAndWeather(settings);
        updateCamera(settings);

        ClientLevel previousLevel = this.minecraft.level;
        LocalPlayer previousPlayer = this.minecraft.player;
        Entity previousCamera = this.minecraft.cameraEntity;

        this.minecraft.level = this.context.level;
        this.minecraft.player = this.context.player;
        this.minecraft.cameraEntity = this.context.player;

        WorldSceneRenderContext.begin();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();

        Matrix4f projection = createProjectionMatrix(settings.fov, this.context.chunkRadius);
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, settings.opacity);
        RenderSystem.clear(256, Minecraft.ON_OSX);

        try {
            LightTexture lightTexture = this.minecraft.gameRenderer.lightTexture();
            lightTexture.updateLightTexture(partial);

            Camera camera = this.context.camera;
            Matrix4f frustumMatrix = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
            this.context.levelRenderer.prepareCullFrustum(camera.getPosition(), frustumMatrix, projection);
            this.context.levelRenderer.renderLevel(this.minecraft.getTimer(), false, camera, this.minecraft.gameRenderer, lightTexture, frustumMatrix, projection);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] World scene render failed", ex);
            setState(SceneState.ERROR, "World scene render error");
            disposeContext();
        } finally {
            WorldSceneRenderContext.end();
            this.minecraft.level = previousLevel;
            this.minecraft.player = previousPlayer;
            this.minecraft.cameraEntity = previousCamera;

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableDepthTest();
            Matrix4f ortho = new Matrix4f().setOrtho(
                    0.0F,
                    (float)((double)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getGuiScale()),
                    (float)((double)this.minecraft.getWindow().getHeight() / this.minecraft.getWindow().getGuiScale()),
                    0.0F,
                    1000.0F,
                    21000.0F
            );
            RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    public void dispose() {
        disposeContext();
        if (this.loadingFuture != null) {
            this.loadingFuture.cancel(true);
            this.loadingFuture = null;
        }
        this.pendingKey = null;
        setState(SceneState.UNLOADED, null);
    }

    private void applyTimeAndWeather(@NotNull SceneSettings settings) {
        if (this.context == null) return;
        LevelData data = this.context.level.getLevelData();
        if (data instanceof ClientLevel.ClientLevelData clientData) {
            long time = settings.timeOfDay;
            clientData.setDayTime(time);
            clientData.setGameTime(time);
            clientData.setRaining(settings.weather != WorldSceneMenuBackground.WeatherMode.CLEAR);
        }
        switch (settings.weather) {
            case CLEAR -> {
                this.context.level.setRainLevel(0.0F);
                this.context.level.setThunderLevel(0.0F);
            }
            case RAIN -> {
                this.context.level.setRainLevel(1.0F);
                this.context.level.setThunderLevel(0.0F);
            }
            case THUNDER -> {
                this.context.level.setRainLevel(1.0F);
                this.context.level.setThunderLevel(1.0F);
            }
        }
    }

    private void updateCamera(@NotNull SceneSettings settings) {
        if (this.context == null) return;
        this.context.player.setPos(settings.cameraX, settings.cameraY, settings.cameraZ);
        this.context.player.setYRot(settings.yaw);
        this.context.player.setXRot(settings.pitch);

        this.context.camera.setManual(settings.cameraX, settings.cameraY, settings.cameraZ, settings.yaw, settings.pitch);

        int centerChunkX = Mth.floor(settings.cameraX) >> 4;
        int centerChunkZ = Mth.floor(settings.cameraZ) >> 4;
        this.context.level.getChunkSource().updateViewCenter(centerChunkX, centerChunkZ);
    }

    private Matrix4f createProjectionMatrix(float fov, int chunkRadius) {
        float aspect = (float)this.minecraft.getWindow().getWidth() / (float)this.minecraft.getWindow().getHeight();
        float renderDistance = Math.max(2, chunkRadius) * 16.0F;
        float farPlane = Math.max(256.0F, renderDistance * 4.0F);
        return new Matrix4f().perspective((float)(fov * (Math.PI / 180.0)), aspect, 0.05F, farPlane);
    }

    private void startLoad(@NotNull LoadKey key) {
        this.pendingKey = key;
        setState(SceneState.LOADING, "Loading world scene...");
        disposeContext();

        int token = this.loadToken.incrementAndGet();
        this.loadingFuture = CompletableFuture.supplyAsync(() -> loadChunkNbt(key), this.ioExecutor)
                .whenComplete((result, throwable) -> this.minecraft.execute(() -> {
                    if (token != this.loadToken.get()) return;
                    if (throwable != null) {
                        LOGGER.error("[FANCYMENU] Failed to load world scene", throwable);
                        setState(SceneState.ERROR, "Failed to load world scene");
                        return;
                    }
                    if (result.error != null) {
                        setState(SceneState.ERROR, result.error);
                        return;
                    }
                    try {
                        this.context = buildContext(key, result);
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Failed to build world scene context", ex);
                        this.context = null;
                    }
                    if (this.context == null) {
                        setState(SceneState.ERROR, "Failed to build world scene");
                        return;
                    }
                    setState(SceneState.READY, null);
                }));
    }

    private @NotNull LoadResult loadChunkNbt(@NotNull LoadKey key) {
        Path levelDatPath = key.levelDatPath;
        if (!Files.isRegularFile(levelDatPath)) {
            return LoadResult.error(key, "level.dat not found");
        }
        if (!"level.dat".equalsIgnoreCase(levelDatPath.getFileName().toString())) {
            return LoadResult.error(key, "Invalid level.dat file");
        }
        Path worldFolder = levelDatPath.getParent();
        if (worldFolder == null) {
            return LoadResult.error(key, "Invalid world folder");
        }
        Path regionFolder = worldFolder.resolve("region");
        if (!Files.isDirectory(regionFolder)) {
            return LoadResult.error(key, "Missing region folder");
        }

        List<ChunkNbtData> chunks = new ArrayList<>();
        RegionStorageInfo storageInfo = new RegionStorageInfo(worldFolder.getFileName().toString(), Level.OVERWORLD, "chunk");
        try (RegionReader regionReader = new RegionReader(storageInfo, regionFolder)) {
            for (int dx = -key.chunkRadius; dx <= key.chunkRadius; dx++) {
                for (int dz = -key.chunkRadius; dz <= key.chunkRadius; dz++) {
                    ChunkPos pos = new ChunkPos(key.centerChunkX + dx, key.centerChunkZ + dz);
                    CompoundTag tag = regionReader.read(pos);
                    if (tag != null) {
                        chunks.add(new ChunkNbtData(pos, tag));
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed reading chunk data", ex);
            return LoadResult.error(key, "Failed reading chunk data");
        }

        return new LoadResult(key, worldFolder, chunks, null);
    }

    private @Nullable SceneContext buildContext(@NotNull LoadKey key, @NotNull LoadResult result) {
        RegistryAccess.Frozen registryAccess = getRegistryAccess();
        FeatureFlagSet enabledFeatures = getEnabledFeatures();
        CommonListenerCookie cookie = new CommonListenerCookie(
                this.minecraft.getGameProfile(),
                this.minecraft.getTelemetryManager().createWorldSessionManager(false, null, null),
                registryAccess,
                enabledFeatures,
                null,
                null,
                null,
                Map.of(),
                null,
                false,
                Map.of(),
                ServerLinks.EMPTY
        );

        Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        ClientPacketListener packetListener = new ClientPacketListener(this.minecraft, connection, cookie);
        LevelRenderer levelRenderer = new LevelRenderer(this.minecraft, this.minecraft.getEntityRenderDispatcher(), this.minecraft.getBlockEntityRenderDispatcher(), this.minecraft.renderBuffers());

        ClientLevel.ClientLevelData levelData = new ClientLevel.ClientLevelData(Difficulty.NORMAL, false, false);
        levelData.setSpawn(BlockPos.ZERO, 0.0F);

        ResourceKey<Level> dimension = Level.OVERWORLD;
        Holder<DimensionType> dimensionType = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD);
        int viewDistance = Math.max(2, key.chunkRadius);

        ClientLevel level = new ClientLevel(
                packetListener,
                levelData,
                dimension,
                dimensionType,
                viewDistance,
                viewDistance,
                this.minecraft::getProfiler,
                levelRenderer,
                false,
                0L
        );

        int oldRenderDistance = this.minecraft.options.renderDistance().get();
        this.minecraft.options.renderDistance().set(viewDistance);
        try {
            levelRenderer.setLevel(level);
        } finally {
            this.minecraft.options.renderDistance().set(oldRenderDistance);
        }

        LocalPlayer player = new LocalPlayer(this.minecraft, level, packetListener, new net.minecraft.stats.StatsCounter(), new net.minecraft.client.ClientRecipeBook(), false, false);
        SceneCamera camera = new SceneCamera();
        camera.setup(level, player, false, false, 1.0F);

        SceneContext context = new SceneContext(key, level, levelRenderer, packetListener, connection, player, camera, viewDistance);
        level.getChunkSource().updateViewCenter(key.centerChunkX, key.centerChunkZ);
        level.getChunkSource().updateViewRadius(viewDistance);

        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec = makeBiomeCodec(biomeRegistry);

        for (ChunkNbtData chunkData : result.chunks) {
            applyChunk(level, chunkData, biomeRegistry, biomeCodec);
        }

        return context;
    }

    private void applyChunk(@NotNull ClientLevel level, @NotNull ChunkNbtData chunkData, @NotNull Registry<Biome> biomeRegistry, @NotNull Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec) {
        CompoundTag tag = chunkData.tag;
        ListTag sectionsTag = tag.getList("sections", 10);
        LevelChunkSection[] sections = new LevelChunkSection[level.getSectionsCount()];

        LevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        boolean hasLightData = false;

        for (int i = 0; i < sectionsTag.size(); i++) {
            CompoundTag sectionTag = sectionsTag.getCompound(i);
            int sectionY = sectionTag.getByte("Y");
            int sectionIndex = level.getSectionIndexFromSectionY(sectionY);
            if (sectionIndex < 0 || sectionIndex >= sections.length) continue;

            PalettedContainer<BlockState> blockStates;
            if (sectionTag.contains("block_states", 10)) {
                DataResult<PalettedContainer<BlockState>> parsed = BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, sectionTag.getCompound("block_states"));
                blockStates = parsed.result().orElseGet(() -> new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), Strategy.SECTION_STATES));
            } else {
                blockStates = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), Strategy.SECTION_STATES);
            }

            PalettedContainerRO<Holder<Biome>> biomes;
            if (sectionTag.contains("biomes", 10)) {
                DataResult<PalettedContainerRO<Holder<Biome>>> parsed = biomeCodec.parse(NbtOps.INSTANCE, sectionTag.getCompound("biomes"));
                biomes = parsed.result().orElseGet(() -> new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS), Strategy.SECTION_BIOMES));
            } else {
                biomes = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS), Strategy.SECTION_BIOMES);
            }

            sections[sectionIndex] = new LevelChunkSection(blockStates, biomes);

            boolean hasBlockLight = sectionTag.contains("BlockLight", 7);
            boolean hasSkyLight = sectionTag.contains("SkyLight", 7);
            if (hasBlockLight || hasSkyLight) {
                if (!hasLightData) {
                    lightEngine.retainData(chunkData.pos, true);
                    hasLightData = true;
                }
                if (hasBlockLight) {
                    lightEngine.queueSectionData(net.minecraft.world.level.LightLayer.BLOCK, SectionPos.of(chunkData.pos, sectionY), new DataLayer(sectionTag.getByteArray("BlockLight")));
                }
                if (hasSkyLight) {
                    lightEngine.queueSectionData(net.minecraft.world.level.LightLayer.SKY, SectionPos.of(chunkData.pos, sectionY), new DataLayer(sectionTag.getByteArray("SkyLight")));
                }
            }
        }

        long inhabited = tag.contains("InhabitedTime") ? tag.getLong("InhabitedTime") : 0L;
        LevelChunk tempChunk = new LevelChunk(level, chunkData.pos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), inhabited, sections, null, null);

        CompoundTag heightmapsTag = tag.getCompound("Heightmaps");
        for (Heightmap.Types type : Heightmap.Types.values()) {
            String key = type.getSerializationKey();
            if (heightmapsTag.contains(key, 12)) {
                tempChunk.setHeightmap(type, heightmapsTag.getLongArray(key));
            }
        }

        ListTag blockEntities = tag.getList("block_entities", 10);
        for (int i = 0; i < blockEntities.size(); i++) {
            CompoundTag blockEntityTag = blockEntities.getCompound(i);
            BlockPos pos = BlockEntity.getPosFromTag(blockEntityTag);
            BlockState state = tempChunk.getBlockState(pos);
            BlockEntity blockEntity = BlockEntity.loadStatic(pos, state, blockEntityTag, level.registryAccess());
            if (blockEntity != null) {
                tempChunk.setBlockEntity(blockEntity);
            }
        }

        ClientboundLevelChunkPacketData packetData = new ClientboundLevelChunkPacketData(tempChunk);
        level.getChunkSource().replaceWithPacketData(
                chunkData.pos.x,
                chunkData.pos.z,
                packetData.getReadBuffer(),
                packetData.getHeightmaps(),
                packetData.getBlockEntitiesTagsConsumer(chunkData.pos.x, chunkData.pos.z)
        );
    }

    private RegistryAccess.Frozen getRegistryAccess() {
        if (this.minecraft.getConnection() != null) {
            return this.minecraft.getConnection().registryAccess();
        }
        RegistryAccess.Frozen cached = FALLBACK_REGISTRY_ACCESS;
        if (cached != null) {
            return cached;
        }
        synchronized (REGISTRY_LOCK) {
            if (FALLBACK_REGISTRY_ACCESS != null) {
                return FALLBACK_REGISTRY_ACCESS;
            }
            RegistryAccess.Frozen staticAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            RegistryAccess.Frozen merged = staticAccess;
            try {
                RegistryAccess.Frozen worldgen = RegistryDataLoader.load(this.minecraft.getResourceManager(), staticAccess, RegistryDataLoader.SYNCHRONIZED_REGISTRIES);
                merged = mergeRegistryAccess(staticAccess, worldgen);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to load world scene registries, using static registries only.", ex);
            }
            if (!hasRequiredRegistries(merged)) {
                merged = mergeRegistryAccess(merged, createMinimalRegistryAccess());
            }
            FALLBACK_REGISTRY_ACCESS = merged;
            return FALLBACK_REGISTRY_ACCESS;
        }
    }

    private FeatureFlagSet getEnabledFeatures() {
        if (this.minecraft.getConnection() != null) {
            return this.minecraft.getConnection().enabledFeatures();
        }
        return FeatureFlags.DEFAULT_FLAGS;
    }

    private void disposeContext() {
        if (this.context == null) return;
        try {
            this.context.levelRenderer.close();
        } catch (Exception ex) {
            LOGGER.debug("[FANCYMENU] Failed to close world scene level renderer", ex);
        }
        try {
            this.context.levelRenderer.setLevel(null);
        } catch (Exception ignore) {
        }
        try {
            this.context.packetListener.close();
        } catch (Exception ignore) {
        }
        this.context = null;
    }

    private void setState(@NotNull SceneState state, @Nullable String message) {
        this.state = state;
        this.stateMessage = message;
    }

    private static boolean hasRequiredRegistries(@NotNull RegistryAccess.Frozen access) {
        return access.registry(Registries.DIMENSION_TYPE).isPresent()
                && access.registry(Registries.BIOME).isPresent()
                && access.registry(Registries.DAMAGE_TYPE).isPresent();
    }

    private static RegistryAccess.Frozen mergeRegistryAccess(@NotNull RegistryAccess.Frozen base, @NotNull RegistryAccess.Frozen extra) {
        Map<ResourceKey<? extends Registry<?>>, Registry<?>> merged = new HashMap<>();
        base.registries().forEach(entry -> merged.put(entry.key(), entry.value()));
        extra.registries().forEach(entry -> merged.put(entry.key(), entry.value()));
        return new RegistryAccess.ImmutableRegistryAccess(merged).freeze();
    }

    private static RegistryAccess.Frozen createMinimalRegistryAccess() {
        RegistryAccess.Frozen staticAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

        MappedRegistry<DimensionType> dimensionRegistry = new MappedRegistry<>(Registries.DIMENSION_TYPE, Lifecycle.stable());
        DimensionTypes.bootstrap(new SimpleBootstrapContext<>(dimensionRegistry, staticAccess));
        dimensionRegistry.freeze();

        MappedRegistry<DamageType> damageRegistry = new MappedRegistry<>(Registries.DAMAGE_TYPE, Lifecycle.stable());
        DamageTypes.bootstrap(new SimpleBootstrapContext<>(damageRegistry, staticAccess));
        damageRegistry.freeze();

        MappedRegistry<Biome> biomeRegistry = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        biomeRegistry.register(Biomes.PLAINS, createFallbackBiome(), RegistrationInfo.BUILT_IN);
        biomeRegistry.freeze();

        return new RegistryAccess.ImmutableRegistryAccess(List.of(dimensionRegistry, damageRegistry, biomeRegistry)).freeze();
    }

    private static Biome createFallbackBiome() {
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .fogColor(12638463)
                .waterColor(4159204)
                .waterFogColor(329011)
                .skyColor(7907327)
                .build();
        return new Biome.BiomeBuilder()
                .temperature(0.8F)
                .downfall(0.4F)
                .specialEffects(effects)
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }

    private static final class SimpleBootstrapContext<T> implements BootstrapContext<T> {

        private final MappedRegistry<T> registry;
        private final RegistryAccess access;

        private SimpleBootstrapContext(@NotNull MappedRegistry<T> registry, @NotNull RegistryAccess access) {
            this.registry = registry;
            this.access = access;
        }

        @Override
        public Holder.Reference<T> register(ResourceKey<T> resourceKey, T object, Lifecycle lifecycle) {
            RegistrationInfo info = new RegistrationInfo(Optional.empty(), lifecycle);
            return this.registry.register(resourceKey, object, info);
        }

        @Override
        public <S> net.minecraft.core.HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> resourceKey) {
            return this.access.lookup(resourceKey).orElseThrow(() -> new IllegalStateException("Missing registry " + resourceKey.location()));
        }
    }

    private static Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(Registry<Biome> biomeRegistry) {
        return PalettedContainer.codecRO(
                biomeRegistry.asHolderIdMap(),
                biomeRegistry.holderByNameCodec(),
                Strategy.SECTION_BIOMES,
                biomeRegistry.getHolderOrThrow(Biomes.PLAINS)
        );
    }

    public enum SceneState {
        UNLOADED,
        LOADING,
        ERROR,
        READY
    }

    public record SceneSettings(
            float cameraX,
            float cameraY,
            float cameraZ,
            float yaw,
            float pitch,
            float fov,
            WorldSceneMenuBackground.WeatherMode weather,
            int timeOfDay,
            float opacity
    ) {
    }

    private record LoadKey(Path levelDatPath, int chunkRadius, int centerChunkX, int centerChunkZ) {
    }

    private record ChunkNbtData(ChunkPos pos, CompoundTag tag) {
    }

    private record LoadResult(LoadKey key, @Nullable Path worldFolder, List<ChunkNbtData> chunks, @Nullable String error) {
        static LoadResult error(LoadKey key, String message) {
            return new LoadResult(key, null, List.of(), message);
        }
    }

    private record SceneContext(LoadKey key, ClientLevel level, LevelRenderer levelRenderer, ClientPacketListener packetListener, Connection connection, LocalPlayer player, SceneCamera camera, int chunkRadius) {
    }

    private static final class RegionReader implements AutoCloseable {

        private final RegionStorageInfo info;
        private final Path folder;
        private final Long2ObjectOpenHashMap<RegionFile> cache = new Long2ObjectOpenHashMap<>();

        private RegionReader(@NotNull RegionStorageInfo info, @NotNull Path folder) {
            this.info = info;
            this.folder = folder;
        }

        private @Nullable CompoundTag read(@NotNull ChunkPos pos) throws IOException {
            int regionX = pos.getRegionX();
            int regionZ = pos.getRegionZ();
            long key = ChunkPos.asLong(regionX, regionZ);
            RegionFile region = this.cache.get(key);
            if (region == null) {
                Path regionPath = this.folder.resolve("r." + regionX + "." + regionZ + ".mca");
                if (!Files.isRegularFile(regionPath)) {
                    return null;
                }
                region = new RegionFile(this.info, regionPath, this.folder, false);
                this.cache.put(key, region);
            }
            try (DataInputStream input = region.getChunkDataInputStream(pos)) {
                if (input == null) {
                    return null;
                }
                return NbtIo.read(input);
            }
        }

        @Override
        public void close() throws IOException {
            IOException error = null;
            for (RegionFile region : this.cache.values()) {
                try {
                    region.close();
                } catch (IOException ex) {
                    if (error == null) {
                        error = ex;
                    } else {
                        error.addSuppressed(ex);
                    }
                }
            }
            if (error != null) {
                throw error;
            }
        }
    }

    private static final class SceneCamera extends Camera {
        public void setManual(float x, float y, float z, float yaw, float pitch) {
            this.setPosition(x, y, z);
            this.setRotation(yaw, pitch);
        }
    }
}
