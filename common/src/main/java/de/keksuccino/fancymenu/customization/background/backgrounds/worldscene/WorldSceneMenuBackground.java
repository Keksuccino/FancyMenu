package de.keksuccino.fancymenu.customization.background.backgrounds.worldscene;

import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WorldSceneMenuBackground extends MenuBackground<WorldSceneMenuBackground> {

    private static final FileFilter LEVEL_DAT_FILTER = file -> "level.dat".equalsIgnoreCase(file.getName());

    private static final Property.NumericInputBehavior<Float> FREE_FLOAT_INPUT = Property.NumericInputBehavior.<Float>builder().freeInput().build();
    private static final Property.NumericInputBehavior<Float> ROTATION_INPUT = Property.NumericInputBehavior.<Float>builder().rangeInput(-180.0F, 180.0F).build();
    private static final Property.NumericInputBehavior<Float> PITCH_INPUT = Property.NumericInputBehavior.<Float>builder().rangeInput(-90.0F, 90.0F).build();
    private static final Property.NumericInputBehavior<Float> FOV_INPUT = Property.NumericInputBehavior.<Float>builder().rangeInput(30.0F, 110.0F).build();
    private static final Property.NumericInputBehavior<Float> SPEED_INPUT = Property.NumericInputBehavior.<Float>builder().rangeInput(0.0F, 30.0F).build();
    private static final Property.NumericInputBehavior<Integer> TIME_INPUT = Property.NumericInputBehavior.<Integer>builder().rangeInput(0, 24000).build();
    private static final Property.NumericInputBehavior<Integer> CHUNK_RADIUS_INPUT = Property.NumericInputBehavior.<Integer>builder().rangeInput(1, 8).build();

    public final Property<ResourceSource> levelDatSource = putProperty(Property.resourceSourceProperty(
            "level_dat",
            null,
            "fancymenu.backgrounds.world_scene.level_dat",
            true,
            false,
            false,
            LEVEL_DAT_FILTER,
            null,
            FileMediaType.OTHER
    ));

    public final Property.FloatProperty cameraX = putProperty(Property.floatProperty("camera_x", 0.0F, "fancymenu.backgrounds.world_scene.camera_x", FREE_FLOAT_INPUT));
    public final Property.FloatProperty cameraY = putProperty(Property.floatProperty("camera_y", 80.0F, "fancymenu.backgrounds.world_scene.camera_y", FREE_FLOAT_INPUT));
    public final Property.FloatProperty cameraZ = putProperty(Property.floatProperty("camera_z", 0.0F, "fancymenu.backgrounds.world_scene.camera_z", FREE_FLOAT_INPUT));
    public final Property.FloatProperty cameraYaw = putProperty(Property.floatProperty("camera_yaw", 0.0F, "fancymenu.backgrounds.world_scene.camera_yaw", ROTATION_INPUT));
    public final Property.FloatProperty cameraPitch = putProperty(Property.floatProperty("camera_pitch", 20.0F, "fancymenu.backgrounds.world_scene.camera_pitch", PITCH_INPUT));
    public final Property.FloatProperty fov = putProperty(Property.floatProperty("fov", 70.0F, "fancymenu.backgrounds.world_scene.fov", FOV_INPUT));

    public final Property<Boolean> rotateX = putProperty(Property.booleanProperty("rotate_x", false, "fancymenu.backgrounds.world_scene.rotate_x"));
    public final Property<Boolean> rotateY = putProperty(Property.booleanProperty("rotate_y", true, "fancymenu.backgrounds.world_scene.rotate_y"));
    public final Property.FloatProperty rotationSpeed = putProperty(Property.floatProperty("rotation_speed", 2.0F, "fancymenu.backgrounds.world_scene.rotation_speed", SPEED_INPUT));

    public final Property.StringProperty weatherMode = putProperty(Property.stringProperty("weather", WeatherMode.CLEAR.id, false, false, "fancymenu.backgrounds.world_scene.weather"));
    public final Property.IntegerProperty timeOfDay = putProperty(Property.integerProperty("time_of_day", 1000, "fancymenu.backgrounds.world_scene.time_of_day", TIME_INPUT));
    public final Property.IntegerProperty chunkRadius = putProperty(Property.integerProperty("chunk_radius", 2, "fancymenu.backgrounds.world_scene.chunk_radius", CHUNK_RADIUS_INPUT));

    private float rotationYawOffset = 0.0F;
    private float rotationPitchOffset = 0.0F;
    private long lastRotationUpdateNanos = System.nanoTime();

    private @Nullable WorldSceneRenderer renderer;

    public WorldSceneMenuBackground(MenuBackgroundBuilder<WorldSceneMenuBackground> builder) {
        super(builder);
        this.cameraPitch.setValueSetProcessor(value -> Mth.clamp(value, -90.0F, 90.0F));
        this.fov.setValueSetProcessor(value -> Mth.clamp(value, 30.0F, 110.0F));
        this.rotationSpeed.setValueSetProcessor(value -> Math.max(0.0F, value));
        this.timeOfDay.setValueSetProcessor(value -> Mth.clamp(value, 0, 24000));
        this.chunkRadius.setValueSetProcessor(value -> Math.max(1, Math.min(8, value)));
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.levelDatSource.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> UITooltip.of(Component.translatable("fancymenu.backgrounds.world_scene.level_dat.desc")))
                .setIcon(MaterialIcons.FOLDER_OPEN);

        menu.addSeparatorEntry("separator_after_world_scene_source");

        this.cameraX.buildContextMenuEntryAndAddTo(menu, this).setIcon(MaterialIcons.MY_LOCATION);
        this.cameraY.buildContextMenuEntryAndAddTo(menu, this).setIcon(MaterialIcons.MY_LOCATION);
        this.cameraZ.buildContextMenuEntryAndAddTo(menu, this).setIcon(MaterialIcons.MY_LOCATION);
        this.cameraYaw.buildContextMenuEntryAndAddTo(menu, this).setIcon(MaterialIcons.ROTATE_LEFT);
        this.cameraPitch.buildContextMenuEntryAndAddTo(menu, this).setIcon(MaterialIcons.ROTATE_RIGHT);
        this.fov.buildContextMenuEntryAndAddTo(menu, this).setIcon(MaterialIcons.ZOOM_IN);

        menu.addSeparatorEntry("separator_after_world_scene_camera");

        this.rotateX.buildContextMenuEntryAndAddTo(menu, this).setIcon(MaterialIcons.ROTATE_LEFT);
        this.rotateY.buildContextMenuEntryAndAddTo(menu, this).setIcon(MaterialIcons.ROTATE_RIGHT);
        this.rotationSpeed.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> UITooltip.of(Component.translatable("fancymenu.backgrounds.world_scene.rotation_speed.desc")))
                .setIcon(MaterialIcons.ROTATE_AUTO);

        menu.addSeparatorEntry("separator_after_world_scene_rotation");

        List<WeatherMode> weathers = List.of(WeatherMode.values());
        this.addCycleContextMenuEntryTo(menu, "weather", weathers, WorldSceneMenuBackground.class,
                background -> WeatherMode.fromId(background.weatherMode.getString()),
                (background, mode) -> background.weatherMode.set(mode.id),
                (menu1, entry, mode) -> {
                    Component value = Component.translatable(mode.getLocalizationKey())
                            .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
                    return Component.translatable("fancymenu.backgrounds.world_scene.weather", value);
                }).setIcon(MaterialIcons.SUNNY);

        this.timeOfDay.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> UITooltip.of(Component.translatable("fancymenu.backgrounds.world_scene.time_of_day.desc")))
                .setIcon(MaterialIcons.TIMELAPSE);

        this.chunkRadius.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> UITooltip.of(Component.translatable("fancymenu.backgrounds.world_scene.chunk_radius.desc")))
                .setIcon(MaterialIcons.GRID_ON);

    }

    @Override
    public void tick() {
        super.tick();
        long now = System.nanoTime();
        float deltaSeconds = (now - this.lastRotationUpdateNanos) / 1_000_000_000.0F;
        this.lastRotationUpdateNanos = now;

        float speed = this.rotationSpeed.getFloat();
        if (this.rotateY.tryGetNonNull()) {
            this.rotationYawOffset = (this.rotationYawOffset + speed * deltaSeconds) % 360.0F;
        }
        if (this.rotateX.tryGetNonNull()) {
            this.rotationPitchOffset = (this.rotationPitchOffset + speed * deltaSeconds) % 360.0F;
        }
    }

    @Override
    public void onDisableOrRemove() {
        super.onDisableOrRemove();
        if (this.renderer != null) {
            this.renderer.dispose();
            this.renderer = null;
        }
        if (this.getMemory().hasProperty(WorldSceneRenderer.MEMORY_KEY)) {
            this.getMemory().removeProperty(WorldSceneRenderer.MEMORY_KEY);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        WorldSceneRenderer renderer = getRenderer();
        Path levelDatPath = getLevelDatPath();

        int radius = this.chunkRadius.getInteger();
        int centerChunkX = Mth.floor(this.cameraX.getFloat()) >> 4;
        int centerChunkZ = Mth.floor(this.cameraZ.getFloat()) >> 4;
        renderer.ensureLoaded(levelDatPath, radius, centerChunkX, centerChunkZ);

        WorldSceneRenderer.SceneState state = renderer.getState();
        if (state != WorldSceneRenderer.SceneState.READY) {
            renderer.renderPlaceholder(graphics, state, this.opacity);
            return;
        }

        float finalYaw = this.cameraYaw.getFloat() + this.rotationYawOffset;
        float finalPitch = this.cameraPitch.getFloat() + this.rotationPitchOffset;

        renderer.renderScene(graphics, partial, new WorldSceneRenderer.SceneSettings(
                this.cameraX.getFloat(),
                this.cameraY.getFloat(),
                this.cameraZ.getFloat(),
                finalYaw,
                finalPitch,
                this.fov.getFloat(),
                WeatherMode.fromId(this.weatherMode.getString()),
                this.timeOfDay.getInteger(),
                this.opacity
        ));
    }

    private @Nullable Path getLevelDatPath() {
        ResourceSource source = this.levelDatSource.get();
        if (source == null) return null;
        if (source.getSourceType() != ResourceSourceType.LOCAL) return null;
        try {
            return Paths.get(source.getSourceWithoutPrefix());
        } catch (Exception ex) {
            return null;
        }
    }

    private WorldSceneRenderer getRenderer() {
        if (this.renderer != null) return this.renderer;
        WorldSceneRenderer cached = this.getMemory().getProperty(WorldSceneRenderer.MEMORY_KEY, WorldSceneRenderer.class);
        if (cached != null) {
            this.renderer = cached;
            return cached;
        }
        this.renderer = new WorldSceneRenderer();
        this.getMemory().putProperty(WorldSceneRenderer.MEMORY_KEY, this.renderer);
        return this.renderer;
    }

    public enum WeatherMode {
        CLEAR("clear"),
        RAIN("rain"),
        THUNDER("thunder");

        private final String id;

        WeatherMode(String id) {
            this.id = id;
        }

        public String getLocalizationKey() {
            return "fancymenu.backgrounds.world_scene.weather." + this.id;
        }

        public static WeatherMode fromId(@Nullable String id) {
            if (id == null) return CLEAR;
            for (WeatherMode mode : values()) {
                if (mode.id.equalsIgnoreCase(id)) return mode;
            }
            return CLEAR;
        }
    }
}
