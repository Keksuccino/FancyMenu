package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Runtime OpenGL shader pipeline for FancyMenu GLSL backgrounds/elements.
 */
public class GlslShaderRuntime {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DEFAULT_GLSL_VERSION = "#version 150";
    private static final String VERTEX_SHADER_SOURCE = """
            #version 150
            in vec2 Position;
            out vec2 fmUv_FancyMenu;
            void main() {
                fmUv_FancyMenu = (Position + 1.0) * 0.5;
                gl_Position = vec4(Position, 0.0, 1.0);
            }
            """;

    private static final int CHANNEL_COUNT = 4;
    private static final int BUFFER_PASS_COUNT = 4;
    private static final int TOTAL_PASS_COUNT = BUFFER_PASS_COUNT + 1;
    private static final int IMAGE_PASS_INDEX = BUFFER_PASS_COUNT;

    // FancyMenu variable API uniforms (replace <name> with the variable name):
    // fmVarFloat_<name>, fmVarInt_<name>, fmVarBool_<name>, fmVarVec2_<name>, fmVarVec3_<name>,
    // fmVarVec4_<name>, fmVarExists_<name>, and the global fmVariableCount.
    private static final String VARIABLE_UNIFORM_FLOAT_PREFIX = "fmVarFloat_";
    private static final String VARIABLE_UNIFORM_INT_PREFIX = "fmVarInt_";
    private static final String VARIABLE_UNIFORM_BOOL_PREFIX = "fmVarBool_";
    private static final String VARIABLE_UNIFORM_VEC2_PREFIX = "fmVarVec2_";
    private static final String VARIABLE_UNIFORM_VEC3_PREFIX = "fmVarVec3_";
    private static final String VARIABLE_UNIFORM_VEC4_PREFIX = "fmVarVec4_";
    private static final String VARIABLE_UNIFORM_EXISTS_PREFIX = "fmVarExists_";
    private static final Pattern VARIABLE_COMPONENT_SPLIT_PATTERN = Pattern.compile("[\\s,;|]+");
    private static final VariableUniformValue ZERO_VARIABLE_UNIFORM_VALUE_FANCYMENU = new VariableUniformValue(
            0.0F,
            0,
            0,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F
    );

    private static final UniformDefinition[] UNIFORMS = new UniformDefinition[]{
            new UniformDefinition("iResolution", "uniform vec3 iResolution;"),
            new UniformDefinition("iTime", "uniform float iTime;"),
            new UniformDefinition("iTimeDelta", "uniform float iTimeDelta;"),
            new UniformDefinition("iFrameRate", "uniform float iFrameRate;"),
            new UniformDefinition("iFrame", "uniform int iFrame;"),
            new UniformDefinition("iMouse", "uniform vec4 iMouse;"),
            new UniformDefinition("iDate", "uniform vec4 iDate;"),
            new UniformDefinition("iSampleRate", "uniform float iSampleRate;"),
            new UniformDefinition("iChannelTime", "uniform float iChannelTime[4];"),
            new UniformDefinition("iChannelResolution", "uniform vec3 iChannelResolution[4];"),
            new UniformDefinition("iChannel0", "uniform sampler2D iChannel0;"),
            new UniformDefinition("iChannel1", "uniform sampler2D iChannel1;"),
            new UniformDefinition("iChannel2", "uniform sampler2D iChannel2;"),
            new UniformDefinition("iChannel3", "uniform sampler2D iChannel3;"),
            new UniformDefinition("fmAreaOffset", "uniform vec2 fmAreaOffset;"),
            new UniformDefinition("fmAreaSize", "uniform vec2 fmAreaSize;"),
            new UniformDefinition("fmAreaPosition", "uniform vec2 fmAreaPosition;"),
            new UniformDefinition("fmAreaTopLeft", "uniform vec2 fmAreaTopLeft;"),
            new UniformDefinition("fmScreenSize", "uniform vec2 fmScreenSize;"),
            new UniformDefinition("fmGuiScale", "uniform float fmGuiScale;"),
            new UniformDefinition("fmMouse", "uniform vec4 fmMouse;"),
            new UniformDefinition("fmMouseDelta", "uniform vec2 fmMouseDelta;"),
            new UniformDefinition("fmMouseButtons", "uniform ivec4 fmMouseButtons;"),
            new UniformDefinition("fmMouseClickCount", "uniform ivec4 fmMouseClickCount;"),
            new UniformDefinition("fmMouseReleaseCount", "uniform ivec4 fmMouseReleaseCount;"),
            new UniformDefinition("fmMouseScroll", "uniform vec2 fmMouseScroll;"),
            new UniformDefinition("fmMouseScrollTotal", "uniform vec2 fmMouseScrollTotal;"),
            new UniformDefinition("fmKeyEvent", "uniform ivec4 fmKeyEvent;"),
            new UniformDefinition("fmKeyEventCount", "uniform int fmKeyEventCount;"),
            new UniformDefinition("fmCharEvent", "uniform ivec4 fmCharEvent;"),
            new UniformDefinition("fmCharEventCount", "uniform int fmCharEventCount;"),
            new UniformDefinition("fmDateParts", "uniform ivec4 fmDateParts;"),
            new UniformDefinition("fmTimeParts", "uniform ivec4 fmTimeParts;"),
            new UniformDefinition("fmDayOfYear", "uniform int fmDayOfYear;"),
            new UniformDefinition("fmWeekOfYear", "uniform int fmWeekOfYear;"),
            new UniformDefinition("fmUnixTimeSeconds", "uniform int fmUnixTimeSeconds;"),
            new UniformDefinition("fmUnixTimeMilliseconds", "uniform int fmUnixTimeMilliseconds;"),
            new UniformDefinition("fmPartialTick", "uniform float fmPartialTick;"),
            new UniformDefinition("fmGameDeltaTicks", "uniform float fmGameDeltaTicks;"),
            new UniformDefinition("fmRealtimeDeltaTicks", "uniform float fmRealtimeDeltaTicks;"),
            new UniformDefinition("fmInWorld", "uniform int fmInWorld;"),
            new UniformDefinition("fmIsPaused", "uniform int fmIsPaused;"),
            new UniformDefinition("fmOpacity", "uniform float fmOpacity;"),
            new UniformDefinition("fmVariableCount", "uniform int fmVariableCount;")
    };

    private static final String[] CACHED_UNIFORM_LOOKUPS = new String[]{
            "iResolution", "iTime", "iTimeDelta", "iFrameRate", "iFrame", "iMouse", "iDate", "iSampleRate",
            "iChannelTime[0]", "iChannelResolution[0]",
            "iChannel0", "iChannel1", "iChannel2", "iChannel3",
            "fmAreaOffset", "fmAreaSize", "fmAreaPosition", "fmAreaTopLeft", "fmScreenSize", "fmGuiScale",
            "fmMouse", "fmMouseDelta", "fmMouseButtons", "fmMouseClickCount", "fmMouseReleaseCount",
            "fmMouseScroll", "fmMouseScrollTotal",
            "fmKeyEvent", "fmKeyEventCount", "fmCharEvent", "fmCharEventCount",
            "fmDateParts", "fmTimeParts", "fmDayOfYear", "fmWeekOfYear",
            "fmUnixTimeSeconds", "fmUnixTimeMilliseconds",
            "fmPartialTick", "fmGameDeltaTicks", "fmRealtimeDeltaTicks", "fmInWorld", "fmIsPaused", "fmOpacity",
            "fmVariableCount"
    };

    private static final Pattern VERSION_DIRECTIVE_PATTERN = Pattern.compile("(?m)^\\s*#version\\s+.+$");
    private static final Pattern PRECISION_DIRECTIVE_PATTERN = Pattern.compile("(?m)^\\s*precision\\s+\\w+\\s+\\w+\\s*;\\s*$");

    private int vaoId;
    private int vboId;

    @Nullable
    private String lastCompileError;
    private boolean sourceMissing;

    private final ProgramState[] passPrograms_FancyMenu = new ProgramState[TOTAL_PASS_COUNT];
    private final BufferTargetState[] bufferTargets_FancyMenu = new BufferTargetState[BUFFER_PASS_COUNT];
    @Nullable
    private ProgramState activeUniformProgram_FancyMenu;

    private long frameCounter;
    private long startNanoTime = -1L;
    private long lastFrameNanoTime = -1L;
    private double accumulatedTimeSeconds;
    private float lastFrameDeltaSeconds;
    private float lastFrameRate;
    private double lastMouseScrollTotalX;
    private double lastMouseScrollTotalY;
    private float lastShadertoyMouseX;
    private float lastShadertoyMouseY;
    private boolean hasShadertoyMousePosition;
    private final Set<String> lastVariableUniformSuffixes_FancyMenu = new HashSet<>();

    public enum CompileMode {
        AUTO,
        DIRECT,
        SHADERTOY
    }

    public enum ChannelInput {
        NONE("none", -1),
        RESOURCE0("resource0", -1),
        RESOURCE1("resource1", -1),
        RESOURCE2("resource2", -1),
        RESOURCE3("resource3", -1),
        BUFFER_A("buffer_a", 0),
        BUFFER_B("buffer_b", 1),
        BUFFER_C("buffer_c", 2),
        BUFFER_D("buffer_d", 3);

        @NotNull
        private final String serializedName;
        private final int bufferPassIndex;

        ChannelInput(@NotNull String serializedName, int bufferPassIndex) {
            this.serializedName = serializedName;
            this.bufferPassIndex = bufferPassIndex;
        }

        @NotNull
        public String serializedName() {
            return this.serializedName;
        }

        public int bufferPassIndex() {
            return this.bufferPassIndex;
        }

        @NotNull
        public static ChannelInput fromSerialized(@Nullable String serialized, @NotNull ChannelInput fallback) {
            if (serialized == null || serialized.isBlank()) {
                return fallback;
            }
            for (ChannelInput value : values()) {
                if (value.serializedName.equalsIgnoreCase(serialized.trim())) {
                    return value;
                }
            }
            return fallback;
        }
    }

    public record ChannelRouting(
            @NotNull ChannelInput channel0,
            @NotNull ChannelInput channel1,
            @NotNull ChannelInput channel2,
            @NotNull ChannelInput channel3
    ) {
        @NotNull
        public ChannelInput channelForIndex(int index) {
            return switch (index) {
                case 0 -> this.channel0;
                case 1 -> this.channel1;
                case 2 -> this.channel2;
                case 3 -> this.channel3;
                default -> ChannelInput.NONE;
            };
        }

        @NotNull
        public static ChannelRouting defaultResources() {
            return new ChannelRouting(ChannelInput.RESOURCE0, ChannelInput.RESOURCE1, ChannelInput.RESOURCE2, ChannelInput.RESOURCE3);
        }

        @NotNull
        public static ChannelRouting defaultNone() {
            return new ChannelRouting(ChannelInput.NONE, ChannelInput.NONE, ChannelInput.NONE, ChannelInput.NONE);
        }
    }

    public record RenderSettings(
            @NotNull CompileMode compileMode,
            boolean forceShadertoyCompatibility,
            float timeScale,
            boolean freezeTime,
            boolean enableBlend,
            boolean useInput,
            boolean mousePositionRequiresHold,
            float opacity,
            @Nullable ResourceSupplier<ITexture> channel0,
            @Nullable ResourceSupplier<ITexture> channel1,
            @Nullable ResourceSupplier<ITexture> channel2,
            @Nullable ResourceSupplier<ITexture> channel3,
            @Nullable String bufferASource,
            @Nullable String bufferBSource,
            @Nullable String bufferCSource,
            @Nullable String bufferDSource,
            @NotNull ChannelRouting imageRouting,
            @NotNull ChannelRouting bufferARouting,
            @NotNull ChannelRouting bufferBRouting,
            @NotNull ChannelRouting bufferCRouting,
            @NotNull ChannelRouting bufferDRouting
    ) {
        @Nullable
        public ResourceSupplier<ITexture> channelSupplier(int index) {
            return switch (index) {
                case 0 -> this.channel0;
                case 1 -> this.channel1;
                case 2 -> this.channel2;
                case 3 -> this.channel3;
                default -> null;
            };
        }

        @Nullable
        public String bufferSource(int bufferPassIndex) {
            return switch (bufferPassIndex) {
                case 0 -> this.bufferASource;
                case 1 -> this.bufferBSource;
                case 2 -> this.bufferCSource;
                case 3 -> this.bufferDSource;
                default -> null;
            };
        }

        @NotNull
        public ChannelRouting routingForPass(int passIndex) {
            return switch (passIndex) {
                case 0 -> this.bufferARouting;
                case 1 -> this.bufferBRouting;
                case 2 -> this.bufferCRouting;
                case 3 -> this.bufferDRouting;
                case IMAGE_PASS_INDEX -> this.imageRouting;
                default -> ChannelRouting.defaultNone();
            };
        }
    }

    private static final class ProgramState {
        private int programId;
        private int vertexShaderId;
        private int fragmentShaderId;
        @Nullable
        private String currentProgramKey;
        @Nullable
        private String lastFailedProgramKey;
        @Nullable
        private String lastCompileError;
        private final Map<String, Integer> uniformLocations = new HashMap<>();
    }

    private static final class BufferTargetState {
        private int framebufferId;
        private int readTextureId;
        private int writeTextureId;
        private int width;
        private int height;
    }

    private record UniformDefinition(@NotNull String name, @NotNull String declaration) {
    }

    private record FragmentVariant(@NotNull String label, @NotNull String source) {
    }

    private record ChannelTextureState(int textureId, float resolutionX, float resolutionY) {
    }

    private record VariableUniformValue(
            float floatValue,
            int intValue,
            int boolValue,
            float vec2X,
            float vec2Y,
            float vec3X,
            float vec3Y,
            float vec3Z,
            float vec4X,
            float vec4Y,
            float vec4Z,
            float vec4W
    ) {
    }

    private record VariableUniformSnapshot(
            @NotNull Map<String, VariableUniformValue> valuesBySuffix,
            @NotNull Set<String> removedSuffixes,
            int variableCount
    ) {
    }

    public GlslShaderRuntime() {
        for (int i = 0; i < TOTAL_PASS_COUNT; i++) {
            this.passPrograms_FancyMenu[i] = new ProgramState();
        }
        for (int i = 0; i < BUFFER_PASS_COUNT; i++) {
            this.bufferTargets_FancyMenu[i] = new BufferTargetState();
        }
    }

    public boolean render(@NotNull GuiGraphics graphics,
                          int areaX,
                          int areaY,
                          int areaWidth,
                          int areaHeight,
                          float partialTick,
                          @Nullable String fragmentSource,
                          @NotNull RenderSettings settings) {

        if (areaWidth <= 0 || areaHeight <= 0) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();

        double guiScale = window.getGuiScale();
        int screenWidthPx = window.getScreenWidth();
        int screenHeightPx = window.getScreenHeight();

        int areaXPx = Mth.floor(areaX * guiScale);
        int areaYPxTop = Mth.floor(areaY * guiScale);
        int areaWidthPx = Math.max(1, Mth.floor(areaWidth * guiScale));
        int areaHeightPx = Math.max(1, Mth.floor(areaHeight * guiScale));
        int areaYPxBottom = screenHeightPx - areaYPxTop - areaHeightPx;

        int viewportX = Math.max(0, areaXPx);
        int viewportY = Math.max(0, areaYPxBottom);
        int viewportRight = Math.min(screenWidthPx, areaXPx + areaWidthPx);
        int viewportTop = Math.min(screenHeightPx, areaYPxBottom + areaHeightPx);
        int viewportWidth = Math.max(0, viewportRight - viewportX);
        int viewportHeight = Math.max(0, viewportTop - viewportY);
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return false;
        }

        boolean[] activeBufferPasses = new boolean[BUFFER_PASS_COUNT];
        for (int i = 0; i < BUFFER_PASS_COUNT; i++) {
            String source = settings.bufferSource(i);
            activeBufferPasses[i] = source != null && !source.isBlank();
        }

        this.sourceMissing = false;
        this.lastCompileError = null;

        for (int i = 0; i < BUFFER_PASS_COUNT; i++) {
            if (!activeBufferPasses[i]) {
                this.deleteProgram(this.passPrograms_FancyMenu[i]);
                this.passPrograms_FancyMenu[i].currentProgramKey = null;
                this.passPrograms_FancyMenu[i].lastFailedProgramKey = null;
                this.passPrograms_FancyMenu[i].lastCompileError = null;
                continue;
            }
            if (!this.ensureProgramForPass(i, settings.bufferSource(i), settings, false)) {
                return false;
            }
        }

        if (!this.ensureProgramForPass(IMAGE_PASS_INDEX, fragmentSource, settings, true)) {
            return false;
        }

        this.ensureGeometry();
        if (this.vaoId <= 0 || this.vboId <= 0) {
            return false;
        }

        this.tickTime(settings);
        VariableUniformSnapshot variableUniformSnapshot = this.buildVariableUniformSnapshot();

        int[] previousViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, previousViewport);

        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int[] previousTextureBindings = new int[CHANNEL_COUNT];

        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);

        try {
            graphics.flush();

            for (int i = 0; i < CHANNEL_COUNT; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                previousTextureBindings[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }

            int missingTextureId = minecraft.getTextureManager().getTexture(MissingTextureAtlasSprite.getLocation()).getId();
            ChannelTextureState[] externalChannelTextureStates = this.resolveExternalChannelTextureStates(minecraft, settings, missingTextureId);
            this.ensureBufferTargets(areaWidthPx, areaHeightPx);

            for (int passIndex = 0; passIndex < BUFFER_PASS_COUNT; passIndex++) {
                if (!activeBufferPasses[passIndex]) {
                    continue;
                }

                ProgramState program = this.passPrograms_FancyMenu[passIndex];
                BufferTargetState target = this.bufferTargets_FancyMenu[passIndex];
                this.bindBufferPassTarget(target);

                GL11.glViewport(0, 0, areaWidthPx, areaHeightPx);
                RenderSystem.disableBlend();

                GL20.glUseProgram(program.programId);
                GL30.glBindVertexArray(this.vaoId);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboId);

                ChannelTextureState[] routedChannels = this.resolveRoutedChannelStates(
                        settings.routingForPass(passIndex),
                        externalChannelTextureStates,
                        activeBufferPasses,
                        missingTextureId
                );
                this.bindChannelTextures(routedChannels);

                this.activeUniformProgram_FancyMenu = program;
                this.uploadUniforms(minecraft, window, settings, partialTick,
                        areaX, areaY, areaWidth, areaHeight,
                        0, 0, areaWidthPx, areaHeightPx, 0,
                        screenWidthPx, screenHeightPx, routedChannels, variableUniformSnapshot);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
                this.activeUniformProgram_FancyMenu = null;

                this.swapBufferTargetTextures(target);
            }

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);

            if (settings.enableBlend()) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            } else {
                RenderSystem.disableBlend();
            }

            ProgramState imageProgram = this.passPrograms_FancyMenu[IMAGE_PASS_INDEX];
            GL20.glUseProgram(imageProgram.programId);
            GL30.glBindVertexArray(this.vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboId);

            ChannelTextureState[] imageChannels = this.resolveRoutedChannelStates(
                    settings.routingForPass(IMAGE_PASS_INDEX),
                    externalChannelTextureStates,
                    activeBufferPasses,
                    missingTextureId
            );
            this.bindChannelTextures(imageChannels);

            this.activeUniformProgram_FancyMenu = imageProgram;
            this.uploadUniforms(minecraft, window, settings, partialTick,
                    areaX, areaY, areaWidth, areaHeight,
                    areaXPx, areaYPxTop, areaWidthPx, areaHeightPx, areaYPxBottom,
                    screenWidthPx, screenHeightPx, imageChannels, variableUniformSnapshot);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            this.activeUniformProgram_FancyMenu = null;

            this.lastCompileError = null;

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed rendering GLSL shader.", ex);
            this.lastCompileError = "Render error: " + ex.getMessage();
            return false;
        } finally {
            this.activeUniformProgram_FancyMenu = null;

            for (int i = 0; i < CHANNEL_COUNT; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTextureBindings[i]);
            }
            GL13.glActiveTexture(previousActiveTexture);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
            GL20.glUseProgram(previousProgram);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glViewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);

            if (blendWasEnabled) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            } else {
                RenderSystem.disableBlend();
            }

            RenderingUtils.resetShaderColor(graphics);
        }

        return true;
    }

    public boolean isSourceMissing() {
        return this.sourceMissing;
    }

    @Nullable
    public String getLastCompileError() {
        return this.lastCompileError;
    }

    public long getFrameCounter() {
        return this.frameCounter;
    }

    public void close() {
        for (ProgramState program : this.passPrograms_FancyMenu) {
            this.deleteProgram(program);
            program.currentProgramKey = null;
            program.lastFailedProgramKey = null;
            program.lastCompileError = null;
        }
        this.deleteBufferTargets();
        this.deleteGeometry();
        this.activeUniformProgram_FancyMenu = null;
        this.lastCompileError = null;
        this.sourceMissing = false;
        this.lastShadertoyMouseX = 0.0F;
        this.lastShadertoyMouseY = 0.0F;
        this.hasShadertoyMousePosition = false;
        this.lastVariableUniformSuffixes_FancyMenu.clear();
    }

    private void tickTime(@NotNull RenderSettings settings) {
        long now = System.nanoTime();
        if (this.startNanoTime < 0L) {
            this.startNanoTime = now;
        }
        if (this.lastFrameNanoTime < 0L) {
            this.lastFrameNanoTime = now;
        }

        double rawDelta = Math.max(0.0D, (now - this.lastFrameNanoTime) / 1_000_000_000.0D);
        if (rawDelta > 1.5D) {
            rawDelta = 0.0D;
        }
        this.lastFrameNanoTime = now;

        this.lastFrameDeltaSeconds = (float) rawDelta;
        this.lastFrameRate = (this.lastFrameDeltaSeconds > 0.0F) ? (1.0F / this.lastFrameDeltaSeconds) : 0.0F;

        if (!settings.freezeTime()) {
            this.accumulatedTimeSeconds += rawDelta * Math.max(0.0F, settings.timeScale());
        }

        this.frameCounter++;
    }

    private boolean ensureProgramForPass(int passIndex,
                                         @Nullable String source,
                                         @NotNull RenderSettings settings,
                                         boolean requiredSource) {

        ProgramState state = this.passPrograms_FancyMenu[passIndex];
        String passName = getPassName(passIndex);

        if (source == null || source.isBlank()) {
            if (requiredSource) {
                this.sourceMissing = true;
                state.lastFailedProgramKey = null;
                state.lastCompileError = null;
                this.lastCompileError = passName + ": Shader source is missing.";
                return false;
            }
            this.deleteProgram(state);
            state.currentProgramKey = null;
            state.lastFailedProgramKey = null;
            state.lastCompileError = null;
            return true;
        }

        if (passIndex == IMAGE_PASS_INDEX) {
            this.sourceMissing = false;
        }

        String normalizedSource = normalizeSource(source);
        String programKey = settings.compileMode().name() + "::" + settings.forceShadertoyCompatibility() + "::" + normalizedSource;
        if (Objects.equals(state.currentProgramKey, programKey) && state.programId > 0) {
            return true;
        }
        if (Objects.equals(state.lastFailedProgramKey, programKey) && state.programId <= 0) {
            if (state.lastCompileError != null && !state.lastCompileError.isBlank()) {
                this.lastCompileError = passName + ":\n" + state.lastCompileError;
            }
            return false;
        }

        List<FragmentVariant> variants = buildFragmentVariants(normalizedSource, settings.compileMode(), settings.forceShadertoyCompatibility());
        if (variants.isEmpty()) {
            this.deleteProgram(state);
            state.currentProgramKey = null;
            state.lastFailedProgramKey = programKey;
            state.lastCompileError = "Shader source does not contain a valid entry point.";
            this.lastCompileError = passName + ":\n" + state.lastCompileError;
            return false;
        }

        List<String> errors = new ArrayList<>();
        for (FragmentVariant variant : variants) {
            if (this.tryCompileVariantForPass(passIndex, state, variant, errors)) {
                state.currentProgramKey = programKey;
                state.lastFailedProgramKey = null;
                state.lastCompileError = null;
                return true;
            }
        }

        this.deleteProgram(state);
        state.currentProgramKey = null;
        state.lastFailedProgramKey = programKey;
        state.lastCompileError = String.join("\n", errors);
        this.lastCompileError = passName + ":\n" + state.lastCompileError;
        return false;
    }

    @NotNull
    private static String getPassName(int passIndex) {
        return switch (passIndex) {
            case 0 -> "Buffer A";
            case 1 -> "Buffer B";
            case 2 -> "Buffer C";
            case 3 -> "Buffer D";
            case IMAGE_PASS_INDEX -> "Image";
            default -> "Pass " + passIndex;
        };
    }

    @NotNull
    private static String normalizeSource(@NotNull String source) {
        String normalized = source.replace('\uFEFF', ' ');
        normalized = normalized.replace("\r\n", "\n").replace('\r', '\n');
        normalized = VERSION_DIRECTIVE_PATTERN.matcher(normalized).replaceAll("");
        normalized = PRECISION_DIRECTIVE_PATTERN.matcher(normalized).replaceAll("");
        return normalized.strip();
    }

    @NotNull
    private List<FragmentVariant> buildFragmentVariants(@NotNull String normalizedSource,
                                                        @NotNull CompileMode compileMode,
                                                        boolean forceShadertoyCompatibility) {

        boolean hasMainImage = containsMainImage(normalizedSource);
        boolean hasMain = containsMain(normalizedSource);
        boolean referencesGlFragColor = normalizedSource.contains("gl_FragColor");
        boolean definesOutColor = Pattern.compile("(?m)^\\s*(layout\\s*\\(.+\\)\\s*)?out\\s+vec4\\s+\\w+").matcher(normalizedSource).find();

        List<FragmentVariant> variants = new ArrayList<>();

        if (compileMode == CompileMode.SHADERTOY) {
            if (hasMainImage) {
                variants.add(new FragmentVariant("shadertoy", buildShadertoyFragment(normalizedSource)));
            }
            return variants;
        }

        if ((compileMode == CompileMode.AUTO) && (forceShadertoyCompatibility || hasMainImage) && hasMainImage) {
            variants.add(new FragmentVariant("shadertoy", buildShadertoyFragment(normalizedSource)));
        }

        if (compileMode == CompileMode.DIRECT || compileMode == CompileMode.AUTO) {
            boolean preferDirectWithoutCompat = definesOutColor && !referencesGlFragColor;
            if (preferDirectWithoutCompat) {
                variants.add(new FragmentVariant("direct_no_compat", buildDirectFragment(normalizedSource, false)));
                variants.add(new FragmentVariant("direct_glfragcolor_compat", buildDirectFragment(normalizedSource, true)));
            } else {
                variants.add(new FragmentVariant("direct_glfragcolor_compat", buildDirectFragment(normalizedSource, true)));
                variants.add(new FragmentVariant("direct_no_compat", buildDirectFragment(normalizedSource, false)));
            }
        }

        if (!hasMainImage && !hasMain) {
            return new ArrayList<>();
        }

        return variants;
    }

    private static boolean containsMainImage(@NotNull String source) {
        return Pattern.compile("(?m)^\\s*void\\s+mainImage\\s*\\(").matcher(source).find();
    }

    private static boolean containsMain(@NotNull String source) {
        return Pattern.compile("(?m)^\\s*void\\s+main\\s*\\(").matcher(source).find();
    }

    @NotNull
    private String buildShadertoyFragment(@NotNull String source) {
        StringBuilder uniforms = buildUniformDeclarations(source);
        StringBuilder fragment = new StringBuilder();
        fragment.append(DEFAULT_GLSL_VERSION).append('\n');
        fragment.append("in vec2 fmUv_FancyMenu;\n");
        fragment.append("out vec4 fmOutputColor_FancyMenu;\n");
        fragment.append("#define iGlobalTime iTime\n");
        fragment.append("#define texture2D texture\n");
        fragment.append("#define textureCube texture\n");
        fragment.append(uniforms);
        fragment.append('\n');
        fragment.append(source).append('\n');
        fragment.append("\nvoid main() {\n");
        fragment.append("    vec4 fmColor_FancyMenu = vec4(0.0);\n");
        fragment.append("    mainImage(fmColor_FancyMenu, gl_FragCoord.xy - fmAreaOffset);\n");
        fragment.append("    fmOutputColor_FancyMenu = vec4(fmColor_FancyMenu.rgb, fmColor_FancyMenu.a * fmOpacity);\n");
        fragment.append("}\n");
        return fragment.toString();
    }

    @NotNull
    private String buildDirectFragment(@NotNull String source, boolean withGlFragColorCompat) {
        StringBuilder uniforms = buildUniformDeclarations(source);
        StringBuilder fragment = new StringBuilder();
        fragment.append(DEFAULT_GLSL_VERSION).append('\n');
        fragment.append("in vec2 fmUv_FancyMenu;\n");
        if (withGlFragColorCompat) {
            fragment.append("out vec4 fmOutputColor_FancyMenu;\n");
            fragment.append("#define gl_FragColor fmOutputColor_FancyMenu\n");
        }
        fragment.append("#define iGlobalTime iTime\n");
        fragment.append("#define texture2D texture\n");
        fragment.append("#define textureCube texture\n");
        fragment.append(uniforms);
        fragment.append('\n');
        fragment.append(source).append('\n');
        return fragment.toString();
    }

    @NotNull
    private StringBuilder buildUniformDeclarations(@NotNull String source) {
        StringBuilder builder = new StringBuilder();
        for (UniformDefinition uniform : UNIFORMS) {
            if (!sourceDeclaresUniform(source, uniform.name())) {
                builder.append(uniform.declaration()).append('\n');
            }
        }
        return builder;
    }

    private boolean sourceDeclaresUniform(@NotNull String source, @NotNull String uniformName) {
        Pattern pattern = Pattern.compile("(?m)^\\s*uniform\\s+[^;]*\\b" + Pattern.quote(uniformName) + "\\b");
        return pattern.matcher(source).find();
    }

    private boolean tryCompileVariantForPass(int passIndex,
                                             @NotNull ProgramState state,
                                             @NotNull FragmentVariant variant,
                                             @NotNull List<String> errors) {
        int vertex = 0;
        int fragment = 0;
        int program = 0;
        try {
            vertex = compileShader(GL20.GL_VERTEX_SHADER, VERTEX_SHADER_SOURCE);
            fragment = compileShader(GL20.GL_FRAGMENT_SHADER, variant.source());
            program = GL20.glCreateProgram();
            if (program == 0) {
                throw new IllegalStateException("glCreateProgram returned 0.");
            }

            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glBindAttribLocation(program, 0, "Position");
            GL20.glLinkProgram(program);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(program);
                throw new IllegalStateException("Program link failed:\n" + log);
            }

            this.deleteProgram(state);
            state.programId = program;
            state.vertexShaderId = vertex;
            state.fragmentShaderId = fragment;
            this.cacheUniformLocations(state);
            return true;

        } catch (Exception ex) {
            String message = "[" + variant.label() + "] " + ex.getMessage();
            errors.add(message);
            LOGGER.warn("[FANCYMENU] GLSL {} variant '{}' failed to compile/link: {}", getPassName(passIndex), variant.label(), ex.getMessage());
            if (program > 0) {
                GL20.glDeleteProgram(program);
            }
            if (vertex > 0) {
                GL20.glDeleteShader(vertex);
            }
            if (fragment > 0) {
                GL20.glDeleteShader(fragment);
            }
            return false;
        }
    }

    private int compileShader(int type, @NotNull String source) {
        int shader = GL20.glCreateShader(type);
        if (shader == 0) {
            throw new IllegalStateException("glCreateShader returned 0.");
        }

        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed:\n" + log);
        }

        return shader;
    }

    private void cacheUniformLocations(@NotNull ProgramState state) {
        state.uniformLocations.clear();
        for (String lookup : CACHED_UNIFORM_LOOKUPS) {
            state.uniformLocations.put(lookup, GL20.glGetUniformLocation(state.programId, lookup));
        }
    }

    private void ensureGeometry() {
        if (this.vaoId > 0 && this.vboId > 0) {
            return;
        }

        this.vaoId = GL30.glGenVertexArrays();
        this.vboId = GL15.glGenBuffers();
        if (this.vaoId == 0 || this.vboId == 0) {
            LOGGER.error("[FANCYMENU] Failed creating GLSL geometry buffers (vao={}, vbo={}).", this.vaoId, this.vboId);
            return;
        }

        float[] vertices = new float[]{
                -1.0F, -1.0F,
                1.0F, -1.0F,
                1.0F, 1.0F,
                -1.0F, -1.0F,
                1.0F, 1.0F,
                -1.0F, 1.0F
        };

        GL30.glBindVertexArray(this.vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0L);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void deleteProgram(@NotNull ProgramState state) {
        if (state.programId > 0) {
            if (state.vertexShaderId > 0) {
                GL20.glDetachShader(state.programId, state.vertexShaderId);
            }
            if (state.fragmentShaderId > 0) {
                GL20.glDetachShader(state.programId, state.fragmentShaderId);
            }
            GL20.glDeleteProgram(state.programId);
        }
        if (state.vertexShaderId > 0) {
            GL20.glDeleteShader(state.vertexShaderId);
        }
        if (state.fragmentShaderId > 0) {
            GL20.glDeleteShader(state.fragmentShaderId);
        }

        state.programId = 0;
        state.vertexShaderId = 0;
        state.fragmentShaderId = 0;
        state.uniformLocations.clear();
    }

    private void deleteBufferTargets() {
        for (BufferTargetState target : this.bufferTargets_FancyMenu) {
            if (target.readTextureId > 0) {
                GL11.glDeleteTextures(target.readTextureId);
            }
            if (target.writeTextureId > 0) {
                GL11.glDeleteTextures(target.writeTextureId);
            }
            if (target.framebufferId > 0) {
                GL30.glDeleteFramebuffers(target.framebufferId);
            }
            target.readTextureId = 0;
            target.writeTextureId = 0;
            target.framebufferId = 0;
            target.width = 0;
            target.height = 0;
        }
    }

    private void ensureBufferTargets(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        for (BufferTargetState target : this.bufferTargets_FancyMenu) {
            if (target.framebufferId <= 0) {
                target.framebufferId = GL30.glGenFramebuffers();
            }
            boolean needsRecreate = target.readTextureId <= 0
                    || target.writeTextureId <= 0
                    || target.width != width
                    || target.height != height;
            if (needsRecreate) {
                this.recreateBufferTarget(target, width, height);
            }
        }
    }

    private void recreateBufferTarget(@NotNull BufferTargetState target, int width, int height) {
        if (target.readTextureId > 0) {
            GL11.glDeleteTextures(target.readTextureId);
        }
        if (target.writeTextureId > 0) {
            GL11.glDeleteTextures(target.writeTextureId);
        }
        target.readTextureId = this.createRenderTexture(width, height);
        target.writeTextureId = this.createRenderTexture(width, height);
        target.width = width;
        target.height = height;
        this.clearTexture(target, target.readTextureId);
        this.clearTexture(target, target.writeTextureId);
    }

    private int createRenderTexture(int width, int height) {
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        // Use floating-point buffers so Shadertoy multipass data channels can store
        // reprojection/state values outside [0,1] without clamping.
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return textureId;
    }

    private void clearTexture(@NotNull BufferTargetState target, int textureId) {
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        float[] previousClearColor = new float[4];
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, previousClearColor);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            throw new IllegalStateException("Buffer framebuffer incomplete: " + status);
        }
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glClearColor(previousClearColor[0], previousClearColor[1], previousClearColor[2], previousClearColor[3]);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
    }

    private void bindBufferPassTarget(@NotNull BufferTargetState target) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, target.writeTextureId, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Buffer framebuffer incomplete: " + status);
        }
    }

    private void swapBufferTargetTextures(@NotNull BufferTargetState target) {
        int temp = target.readTextureId;
        target.readTextureId = target.writeTextureId;
        target.writeTextureId = temp;
    }

    private void deleteGeometry() {
        if (this.vboId > 0) {
            GL15.glDeleteBuffers(this.vboId);
            this.vboId = 0;
        }
        if (this.vaoId > 0) {
            GL30.glDeleteVertexArrays(this.vaoId);
            this.vaoId = 0;
        }
    }

    @NotNull
    private ChannelTextureState[] resolveExternalChannelTextureStates(@NotNull Minecraft minecraft,
                                                                      @NotNull RenderSettings settings,
                                                                      int fallbackTextureId) {
        ChannelTextureState[] states = new ChannelTextureState[CHANNEL_COUNT];
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            states[i] = this.resolveChannelTextureState(minecraft, settings.channelSupplier(i), fallbackTextureId);
        }
        return states;
    }

    @NotNull
    private ChannelTextureState[] resolveRoutedChannelStates(@NotNull ChannelRouting routing,
                                                             @NotNull ChannelTextureState[] externalChannelStates,
                                                             @NotNull boolean[] activeBufferPasses,
                                                             int fallbackTextureId) {
        ChannelTextureState[] states = new ChannelTextureState[CHANNEL_COUNT];
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            states[i] = this.resolveChannelInputState(routing.channelForIndex(i), externalChannelStates, activeBufferPasses, fallbackTextureId);
        }
        return states;
    }

    @NotNull
    private ChannelTextureState resolveChannelInputState(@NotNull ChannelInput input,
                                                         @NotNull ChannelTextureState[] externalChannelStates,
                                                         @NotNull boolean[] activeBufferPasses,
                                                         int fallbackTextureId) {
        return switch (input) {
            case RESOURCE0 -> externalChannelStates[0];
            case RESOURCE1 -> externalChannelStates[1];
            case RESOURCE2 -> externalChannelStates[2];
            case RESOURCE3 -> externalChannelStates[3];
            case BUFFER_A, BUFFER_B, BUFFER_C, BUFFER_D -> this.resolveBufferChannelState(input.bufferPassIndex(), activeBufferPasses, fallbackTextureId);
            case NONE -> new ChannelTextureState(fallbackTextureId, 0.0F, 0.0F);
        };
    }

    @NotNull
    private ChannelTextureState resolveBufferChannelState(int bufferPassIndex,
                                                          @NotNull boolean[] activeBufferPasses,
                                                          int fallbackTextureId) {
        if (bufferPassIndex < 0 || bufferPassIndex >= BUFFER_PASS_COUNT) {
            return new ChannelTextureState(fallbackTextureId, 0.0F, 0.0F);
        }
        if (bufferPassIndex >= activeBufferPasses.length || !activeBufferPasses[bufferPassIndex]) {
            return new ChannelTextureState(fallbackTextureId, 0.0F, 0.0F);
        }
        BufferTargetState target = this.bufferTargets_FancyMenu[bufferPassIndex];
        if (target.readTextureId <= 0 || target.width <= 0 || target.height <= 0) {
            return new ChannelTextureState(fallbackTextureId, 0.0F, 0.0F);
        }
        return new ChannelTextureState(target.readTextureId, target.width, target.height);
    }

    private void bindChannelTextures(@NotNull ChannelTextureState[] channelTextureStates) {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, channelTextureStates[i].textureId());
        }
    }

    @NotNull
    private ChannelTextureState resolveChannelTextureState(@NotNull Minecraft minecraft,
                                                           @Nullable ResourceSupplier<ITexture> supplier,
                                                           int fallbackTextureId) {
        if (supplier == null) {
            return new ChannelTextureState(fallbackTextureId, 0.0F, 0.0F);
        }

        ITexture texture = supplier.get();
        if (texture == null || !texture.isReady()) {
            return new ChannelTextureState(fallbackTextureId, 0.0F, 0.0F);
        }

        ResourceLocation location = texture.getResourceLocation();
        if (location == null) {
            return new ChannelTextureState(fallbackTextureId, 0.0F, 0.0F);
        }

        int textureId;
        try {
            textureId = minecraft.getTextureManager().getTexture(location).getId();
        } catch (Exception ex) {
            return new ChannelTextureState(fallbackTextureId, 0.0F, 0.0F);
        }

        float width = Math.max(0.0F, texture.getWidth());
        float height = Math.max(0.0F, texture.getHeight());
        return new ChannelTextureState(textureId, width, height);
    }

    private void uploadUniforms(@NotNull Minecraft minecraft,
                                @NotNull Window window,
                                @NotNull RenderSettings settings,
                                float partialTick,
                                int areaX,
                                int areaY,
                                int areaWidth,
                                int areaHeight,
                                int areaXPx,
                                int areaYPxTop,
                                int areaWidthPx,
                                int areaHeightPx,
                                int areaYPxBottom,
                                int screenWidthPx,
                                int screenHeightPx,
                                @NotNull ChannelTextureState[] channelTextureStates,
                                @NotNull VariableUniformSnapshot variableUniformSnapshot) {

        if (settings.useInput()) {
            GlslRuntimeEventTracker.syncMouseButtonsFromWindow(window.getWindow());
        }
        GlslRuntimeEventTracker.InputSnapshot input = GlslRuntimeEventTracker.snapshot();
        if (!settings.useInput()) {
            this.lastShadertoyMouseX = 0.0F;
            this.lastShadertoyMouseY = 0.0F;
            this.hasShadertoyMousePosition = false;
            input = new GlslRuntimeEventTracker.InputSnapshot(
                    areaX,
                    areaY + areaHeight,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    new boolean[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS],
                    new int[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS],
                    new int[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS],
                    new double[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS],
                    new double[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS],
                    new long[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS],
                    0,
                    -1,
                    -1,
                    0,
                    GlslRuntimeEventTracker.KEY_ACTION_RELEASE,
                    0,
                    -1,
                    0
            );
        }

        float timeDelta = this.lastFrameDeltaSeconds;
        if (settings.freezeTime()) {
            timeDelta = 0.0F;
        } else {
            timeDelta *= Math.max(0.0F, settings.timeScale());
        }

        float frameRate = RenderingUtils.getMinecraftFps();
        if (frameRate <= 0.0F && this.lastFrameRate > 0.0F) {
            frameRate = this.lastFrameRate;
        }

        double mouseGuiX = input.mouseX();
        double mouseGuiY = input.mouseY();
        double localMouseGuiX = mouseGuiX - areaX;
        double localMouseGuiYTop = mouseGuiY - areaY;
        double localMousePxX = localMouseGuiX * window.getGuiScale();
        double localMousePxYBottom = (areaHeight - localMouseGuiYTop) * window.getGuiScale();

        double localMouseNormX = areaWidthPx > 0 ? (localMousePxX / areaWidthPx) : 0.0D;
        double localMouseNormY = areaHeightPx > 0 ? (localMousePxYBottom / areaHeightPx) : 0.0D;

        boolean leftMouseDown = input.mouseButtonStates().length > 0 && input.mouseButtonStates()[0];
        double clickX = 0.0D;
        double clickY = 0.0D;
        boolean hasClickData = input.lastMouseClickNanos().length > 0 && input.lastMouseClickNanos()[0] > 0L;
        if (hasClickData && input.lastMouseClickX().length > 0 && input.lastMouseClickY().length > 0) {
            clickX = (input.lastMouseClickX()[0] - areaX) * window.getGuiScale();
            clickY = (areaHeight - (input.lastMouseClickY()[0] - areaY)) * window.getGuiScale();
        }

        float iMouseX;
        float iMouseY;
        if (settings.mousePositionRequiresHold()) {
            if (leftMouseDown) {
                iMouseX = (float) localMousePxX;
                iMouseY = (float) localMousePxYBottom;
                this.lastShadertoyMouseX = iMouseX;
                this.lastShadertoyMouseY = iMouseY;
                this.hasShadertoyMousePosition = true;
            } else if (this.hasShadertoyMousePosition) {
                iMouseX = this.lastShadertoyMouseX;
                iMouseY = this.lastShadertoyMouseY;
            } else if (hasClickData) {
                iMouseX = (float) clickX;
                iMouseY = (float) clickY;
            } else {
                iMouseX = 0.0F;
                iMouseY = 0.0F;
            }
        } else {
            iMouseX = (float) localMousePxX;
            iMouseY = (float) localMousePxYBottom;
        }

        double mouseScrollDeltaX = input.mouseScrollTotalX() - this.lastMouseScrollTotalX;
        double mouseScrollDeltaY = input.mouseScrollTotalY() - this.lastMouseScrollTotalY;
        if (settings.useInput()) {
            this.lastMouseScrollTotalX = input.mouseScrollTotalX();
            this.lastMouseScrollTotalY = input.mouseScrollTotalY();
        } else {
            mouseScrollDeltaX = 0.0D;
            mouseScrollDeltaY = 0.0D;
        }

        ZonedDateTime now = ZonedDateTime.now();
        Instant instant = now.toInstant();
        int unixSeconds = (int) instant.getEpochSecond();
        int unixMillis = now.getNano() / 1_000_000;
        int secondOfDay = (now.getHour() * 3600) + (now.getMinute() * 60) + now.getSecond();
        float dateSeconds = secondOfDay + (now.getNano() / 1_000_000_000.0F);

        int dayOfWeek = now.getDayOfWeek().getValue();
        int dayOfYear = now.getDayOfYear();
        int weekOfYear = now.get(WeekFields.ISO.weekOfWeekBasedYear());

        setUniform3f("iResolution", areaWidthPx, areaHeightPx, 1.0F);
        setUniform1f("iTime", (float) this.accumulatedTimeSeconds);
        setUniform1f("iTimeDelta", timeDelta);
        setUniform1f("iFrameRate", frameRate);
        setUniform1i("iFrame", (int) Math.min(Integer.MAX_VALUE, this.frameCounter));
        setUniform4f("iMouse",
                iMouseX,
                iMouseY,
                (float) (leftMouseDown ? clickX : -Math.abs(clickX)),
                (float) (leftMouseDown ? clickY : -Math.abs(clickY)));
        setUniform4f("iDate", now.getYear(), now.getMonthValue(), now.getDayOfMonth(), dateSeconds);
        setUniform1f("iSampleRate", 44100.0F);

        float[] iChannelTime = new float[]{
                (float) this.accumulatedTimeSeconds,
                (float) this.accumulatedTimeSeconds,
                (float) this.accumulatedTimeSeconds,
                (float) this.accumulatedTimeSeconds
        };
        float[] iChannelResolution = new float[CHANNEL_COUNT * 3];
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            int baseIndex = i * 3;
            ChannelTextureState state = channelTextureStates[i];
            iChannelResolution[baseIndex] = state.resolutionX();
            iChannelResolution[baseIndex + 1] = state.resolutionY();
            iChannelResolution[baseIndex + 2] = (state.resolutionX() > 0.0F && state.resolutionY() > 0.0F) ? 1.0F : 0.0F;
        }
        setUniform1fv("iChannelTime[0]", iChannelTime);
        setUniform3fv("iChannelResolution[0]", iChannelResolution);

        setUniform1i("iChannel0", 0);
        setUniform1i("iChannel1", 1);
        setUniform1i("iChannel2", 2);
        setUniform1i("iChannel3", 3);

        setUniform2f("fmAreaOffset", areaXPx, areaYPxBottom);
        setUniform2f("fmAreaSize", areaWidthPx, areaHeightPx);
        setUniform2f("fmAreaPosition", areaXPx, areaYPxBottom);
        setUniform2f("fmAreaTopLeft", areaXPx, areaYPxTop);
        setUniform2f("fmScreenSize", screenWidthPx, screenHeightPx);
        setUniform1f("fmGuiScale", (float) window.getGuiScale());

        setUniform4f("fmMouse", (float) localMousePxX, (float) localMousePxYBottom, (float) localMouseNormX, (float) localMouseNormY);
        setUniform2f("fmMouseDelta", (float) (input.mouseDeltaX() * window.getGuiScale()), (float) (-input.mouseDeltaY() * window.getGuiScale()));

        int[] clickCounts = input.mouseClickCounts();
        int[] releaseCounts = input.mouseReleaseCounts();
        boolean[] buttonStates = input.mouseButtonStates();
        setUniform4i("fmMouseButtons",
                resolveButtonState(buttonStates, 0),
                resolveButtonState(buttonStates, 1),
                resolveButtonState(buttonStates, 2),
                resolveButtonState(buttonStates, 3));
        setUniform4i("fmMouseClickCount",
                resolveArrayValue(clickCounts, 0),
                resolveArrayValue(clickCounts, 1),
                resolveArrayValue(clickCounts, 2),
                resolveArrayValue(clickCounts, 3));
        setUniform4i("fmMouseReleaseCount",
                resolveArrayValue(releaseCounts, 0),
                resolveArrayValue(releaseCounts, 1),
                resolveArrayValue(releaseCounts, 2),
                resolveArrayValue(releaseCounts, 3));
        setUniform2f("fmMouseScroll", (float) mouseScrollDeltaX, (float) mouseScrollDeltaY);
        setUniform2f("fmMouseScrollTotal", settings.useInput() ? (float) input.mouseScrollTotalX() : 0.0F, settings.useInput() ? (float) input.mouseScrollTotalY() : 0.0F);

        setUniform4i("fmKeyEvent", input.lastKeyCode(), input.lastScanCode(), input.lastKeyModifiers(), input.lastKeyAction());
        setUniform1i("fmKeyEventCount", input.keyEventCounter());
        setUniform4i("fmCharEvent", input.lastCharCodePoint(), input.lastCharModifiers(), 0, 0);
        setUniform1i("fmCharEventCount", input.charEventCounter());

        setUniform4i("fmDateParts", now.getYear(), now.getMonthValue(), now.getDayOfMonth(), dayOfWeek);
        setUniform4i("fmTimeParts", now.getHour(), now.getMinute(), now.getSecond(), unixMillis);
        setUniform1i("fmDayOfYear", dayOfYear);
        setUniform1i("fmWeekOfYear", weekOfYear);
        setUniform1i("fmUnixTimeSeconds", unixSeconds);
        setUniform1i("fmUnixTimeMilliseconds", unixMillis);

        setUniform1f("fmPartialTick", partialTick);
        setUniform1f("fmGameDeltaTicks", minecraft.getDeltaFrameTime());
        setUniform1f("fmRealtimeDeltaTicks", minecraft.getDeltaFrameTime());
        setUniform1i("fmInWorld", minecraft.level != null ? 1 : 0);
        setUniform1i("fmIsPaused", minecraft.isPaused() ? 1 : 0);
        setUniform1f("fmOpacity", Mth.clamp(settings.opacity(), 0.0F, 1.0F));
        setUniform1i("fmVariableCount", variableUniformSnapshot.variableCount());
        this.uploadVariableUniforms(variableUniformSnapshot);
    }

    private static int resolveButtonState(@NotNull boolean[] array, int index) {
        if (index < 0 || index >= array.length) {
            return 0;
        }
        return array[index] ? 1 : 0;
    }

    private static int resolveArrayValue(@NotNull int[] array, int index) {
        if (index < 0 || index >= array.length) {
            return 0;
        }
        return array[index];
    }

    @NotNull
    private VariableUniformSnapshot buildVariableUniformSnapshot() {
        List<Variable> variables = VariableHandler.getVariables();
        variables.sort(Comparator.comparing(Variable::getName));

        Map<String, VariableUniformValue> valuesBySuffix = new HashMap<>();
        for (Variable variable : variables) {
            String uniformSuffix = toVariableUniformSuffix(variable.getName());
            if (uniformSuffix.isEmpty()) {
                continue;
            }
            valuesBySuffix.put(uniformSuffix, parseVariableUniformValue(variable.getValue()));
        }

        Set<String> removedSuffixes = new HashSet<>(this.lastVariableUniformSuffixes_FancyMenu);
        removedSuffixes.removeAll(valuesBySuffix.keySet());
        this.lastVariableUniformSuffixes_FancyMenu.clear();
        this.lastVariableUniformSuffixes_FancyMenu.addAll(valuesBySuffix.keySet());

        return new VariableUniformSnapshot(valuesBySuffix, removedSuffixes, variables.size());
    }

    private void uploadVariableUniforms(@NotNull VariableUniformSnapshot snapshot) {
        for (Map.Entry<String, VariableUniformValue> entry : snapshot.valuesBySuffix().entrySet()) {
            this.uploadVariableUniformValue(entry.getKey(), entry.getValue(), 1);
        }
        for (String removedSuffix : snapshot.removedSuffixes()) {
            this.uploadVariableUniformValue(removedSuffix, ZERO_VARIABLE_UNIFORM_VALUE_FANCYMENU, 0);
        }
    }

    private void uploadVariableUniformValue(@NotNull String suffix, @NotNull VariableUniformValue value, int existsFlag) {
        setUniform1f(VARIABLE_UNIFORM_FLOAT_PREFIX + suffix, value.floatValue());
        setUniform1i(VARIABLE_UNIFORM_INT_PREFIX + suffix, value.intValue());
        setUniform1i(VARIABLE_UNIFORM_BOOL_PREFIX + suffix, value.boolValue());
        setUniform2f(VARIABLE_UNIFORM_VEC2_PREFIX + suffix, value.vec2X(), value.vec2Y());
        setUniform3f(VARIABLE_UNIFORM_VEC3_PREFIX + suffix, value.vec3X(), value.vec3Y(), value.vec3Z());
        setUniform4f(VARIABLE_UNIFORM_VEC4_PREFIX + suffix, value.vec4X(), value.vec4Y(), value.vec4Z(), value.vec4W());
        setUniform1i(VARIABLE_UNIFORM_EXISTS_PREFIX + suffix, existsFlag);
    }

    @NotNull
    private static String toVariableUniformSuffix(@Nullable String variableName) {
        if (variableName == null || variableName.isEmpty()) {
            return "";
        }
        // Keep mapping deterministic and GLSL-safe so variable updates never require shader recompilation.
        StringBuilder builder = new StringBuilder(variableName.length() + 2);
        for (int i = 0; i < variableName.length(); i++) {
            char c = variableName.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_') {
                builder.append(c);
            } else {
                builder.append('_');
            }
        }
        if (builder.isEmpty()) {
            return "";
        }
        if (builder.charAt(0) >= '0' && builder.charAt(0) <= '9') {
            builder.insert(0, '_');
        }
        return builder.toString();
    }

    @NotNull
    private static VariableUniformValue parseVariableUniformValue(@Nullable String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim();
        float floatValue = parseFloatValue(normalized);
        int intValue = parseIntValue(normalized, floatValue);
        int boolValue = parseBoolValue(normalized, floatValue);
        float[] vec4 = parseVec4Value(normalized, floatValue);
        return new VariableUniformValue(
                floatValue,
                intValue,
                boolValue,
                vec4[0],
                vec4[1],
                vec4[0],
                vec4[1],
                vec4[2],
                vec4[0],
                vec4[1],
                vec4[2],
                vec4[3]
        );
    }

    private static float parseFloatValue(@NotNull String normalizedValue) {
        if (normalizedValue.isEmpty()) {
            return 0.0F;
        }
        try {
            return Float.parseFloat(normalizedValue);
        } catch (Exception ignored) {
        }
        float[] components = parseFloatComponents(normalizedValue);
        if (components.length > 0) {
            return components[0];
        }
        return parseBoolValue(normalizedValue, 0.0F);
    }

    private static int parseIntValue(@NotNull String normalizedValue, float fallbackFloat) {
        if (normalizedValue.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(normalizedValue);
        } catch (Exception ignored) {
        }
        try {
            return (int) Float.parseFloat(normalizedValue);
        } catch (Exception ignored) {
        }
        float[] components = parseFloatComponents(normalizedValue);
        if (components.length > 0) {
            return (int) components[0];
        }
        return (int) fallbackFloat;
    }

    private static int parseBoolValue(@NotNull String normalizedValue, float fallbackFloat) {
        if (normalizedValue.isEmpty()) {
            return 0;
        }
        String lower = normalizedValue.toLowerCase(Locale.ROOT);
        if (lower.equals("true") || lower.equals("yes") || lower.equals("on") || lower.equals("enabled")) {
            return 1;
        }
        if (lower.equals("false") || lower.equals("no") || lower.equals("off") || lower.equals("disabled")) {
            return 0;
        }
        try {
            return Float.parseFloat(normalizedValue) != 0.0F ? 1 : 0;
        } catch (Exception ignored) {
        }
        return fallbackFloat != 0.0F ? 1 : 0;
    }

    @NotNull
    private static float[] parseVec4Value(@NotNull String normalizedValue, float fallbackScalar) {
        float[] components = parseFloatComponents(normalizedValue);
        if (components.length == 0) {
            return new float[]{fallbackScalar, fallbackScalar, fallbackScalar, fallbackScalar};
        }

        float[] vec4 = new float[4];
        for (int i = 0; i < 4; i++) {
            if (i < components.length) {
                vec4[i] = components[i];
            } else {
                vec4[i] = components[components.length - 1];
            }
        }
        return vec4;
    }

    @NotNull
    private static float[] parseFloatComponents(@NotNull String value) {
        if (value.isEmpty()) {
            return new float[0];
        }
        String[] split = VARIABLE_COMPONENT_SPLIT_PATTERN.split(value);
        List<Float> components = new ArrayList<>(split.length);
        for (String token : split) {
            if (token == null || token.isEmpty()) {
                continue;
            }
            try {
                components.add(Float.parseFloat(token));
            } catch (Exception ignored) {
            }
        }
        float[] array = new float[components.size()];
        for (int i = 0; i < components.size(); i++) {
            array[i] = components.get(i);
        }
        return array;
    }

    private void setUniform1f(@NotNull String name, float value) {
        Integer location = this.getActiveUniformLocation(name);
        if (location != null && location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    private void setUniform2f(@NotNull String name, float v1, float v2) {
        Integer location = this.getActiveUniformLocation(name);
        if (location != null && location >= 0) {
            GL20.glUniform2f(location, v1, v2);
        }
    }

    private void setUniform3f(@NotNull String name, float v1, float v2, float v3) {
        Integer location = this.getActiveUniformLocation(name);
        if (location != null && location >= 0) {
            GL20.glUniform3f(location, v1, v2, v3);
        }
    }

    private void setUniform4f(@NotNull String name, float v1, float v2, float v3, float v4) {
        Integer location = this.getActiveUniformLocation(name);
        if (location != null && location >= 0) {
            GL20.glUniform4f(location, v1, v2, v3, v4);
        }
    }

    private void setUniform1i(@NotNull String name, int value) {
        Integer location = this.getActiveUniformLocation(name);
        if (location != null && location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    private void setUniform4i(@NotNull String name, int v1, int v2, int v3, int v4) {
        Integer location = this.getActiveUniformLocation(name);
        if (location != null && location >= 0) {
            GL20.glUniform4i(location, v1, v2, v3, v4);
        }
    }

    private void setUniform1fv(@NotNull String name, @NotNull float[] values) {
        Integer location = this.getActiveUniformLocation(name);
        if (location != null && location >= 0) {
            GL20.glUniform1fv(location, values);
        }
    }

    private void setUniform3fv(@NotNull String name, @NotNull float[] values) {
        Integer location = this.getActiveUniformLocation(name);
        if (location != null && location >= 0) {
            GL20.glUniform3fv(location, values);
        }
    }

    @Nullable
    private Integer getActiveUniformLocation(@NotNull String name) {
        ProgramState state = this.activeUniformProgram_FancyMenu;
        if (state == null) {
            return null;
        }
        return state.uniformLocations.computeIfAbsent(name, lookup -> GL20.glGetUniformLocation(state.programId, lookup));
    }

}
