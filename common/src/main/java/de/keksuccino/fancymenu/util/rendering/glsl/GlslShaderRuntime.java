package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
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

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            new UniformDefinition("fmOpacity", "uniform float fmOpacity;")
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
            "fmPartialTick", "fmGameDeltaTicks", "fmRealtimeDeltaTicks", "fmInWorld", "fmIsPaused", "fmOpacity"
    };

    private static final Pattern VERSION_DIRECTIVE_PATTERN = Pattern.compile("(?m)^\\s*#version\\s+.+$");
    private static final Pattern PRECISION_DIRECTIVE_PATTERN = Pattern.compile("(?m)^\\s*precision\\s+\\w+\\s+\\w+\\s*;\\s*$");

    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private int vaoId;
    private int vboId;

    @Nullable
    private String currentProgramKey;
    @Nullable
    private String lastFailedProgramKey;
    @Nullable
    private String lastCompileError;
    private boolean sourceMissing;

    private final Map<String, Integer> uniformLocations = new HashMap<>();

    private long frameCounter;
    private long startNanoTime = -1L;
    private long lastFrameNanoTime = -1L;
    private double accumulatedTimeSeconds;
    private float lastFrameDeltaSeconds;
    private float lastFrameRate;
    private double lastMouseScrollTotalX;
    private double lastMouseScrollTotalY;

    public enum CompileMode {
        AUTO,
        DIRECT,
        SHADERTOY
    }

    public record RenderSettings(
            @NotNull CompileMode compileMode,
            boolean forceShadertoyCompatibility,
            float timeScale,
            boolean freezeTime,
            boolean enableBlend,
            boolean useInput,
            float opacity
    ) {
    }

    private record UniformDefinition(@NotNull String name, @NotNull String declaration) {
    }

    private record FragmentVariant(@NotNull String label, @NotNull String source) {
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

        if (!this.ensureProgram(fragmentSource, settings)) {
            return false;
        }

        if (this.programId <= 0) {
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

        this.ensureGeometry();
        if (this.vaoId <= 0 || this.vboId <= 0) {
            return false;
        }

        this.tickTime(settings);

        int[] previousViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, previousViewport);

        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int[] previousTextureBindings = new int[CHANNEL_COUNT];

        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);

        try {
            graphics.flush();

            GL11.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);

            if (settings.enableBlend()) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            } else {
                RenderSystem.disableBlend();
            }

            GL20.glUseProgram(this.programId);
            GL30.glBindVertexArray(this.vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboId);

            int missingTextureId = minecraft.getTextureManager().getTexture(MissingTextureAtlasSprite.getLocation()).getId();
            for (int i = 0; i < CHANNEL_COUNT; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                previousTextureBindings[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, missingTextureId);
            }

            this.uploadUniforms(minecraft, window, settings, partialTick,
                    areaX, areaY, areaWidth, areaHeight,
                    areaXPx, areaYPxTop, areaWidthPx, areaHeightPx, areaYPxBottom,
                    screenWidthPx, screenHeightPx);

            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed rendering GLSL shader.", ex);
            this.lastCompileError = "Render error: " + ex.getMessage();
            return false;
        } finally {
            for (int i = 0; i < CHANNEL_COUNT; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTextureBindings[i]);
            }
            GL13.glActiveTexture(previousActiveTexture);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
            GL20.glUseProgram(previousProgram);
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
        this.deleteProgram();
        this.deleteGeometry();
        this.currentProgramKey = null;
        this.lastFailedProgramKey = null;
        this.lastCompileError = null;
        this.sourceMissing = false;
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

    private boolean ensureProgram(@Nullable String source, @NotNull RenderSettings settings) {

        if (source == null || source.isBlank()) {
            this.sourceMissing = true;
            this.lastFailedProgramKey = null;
            this.lastCompileError = null;
            return false;
        }

        this.sourceMissing = false;

        String normalizedSource = normalizeSource(source);
        String programKey = settings.compileMode().name() + "::" + settings.forceShadertoyCompatibility() + "::" + normalizedSource;
        if (Objects.equals(this.currentProgramKey, programKey) && this.programId > 0) {
            return true;
        }
        if (Objects.equals(this.lastFailedProgramKey, programKey) && this.programId <= 0) {
            return false;
        }

        List<FragmentVariant> variants = buildFragmentVariants(normalizedSource, settings.compileMode(), settings.forceShadertoyCompatibility());
        if (variants.isEmpty()) {
            this.deleteProgram();
            this.currentProgramKey = null;
            this.lastFailedProgramKey = programKey;
            this.lastCompileError = "Shader source does not contain a valid entry point.";
            return false;
        }

        List<String> errors = new ArrayList<>();
        for (FragmentVariant variant : variants) {
            if (tryCompileVariant(variant, errors)) {
                this.currentProgramKey = programKey;
                this.lastFailedProgramKey = null;
                this.lastCompileError = null;
                return true;
            }
        }

        this.deleteProgram();
        this.currentProgramKey = null;
        this.lastFailedProgramKey = programKey;
        this.lastCompileError = String.join("\n", errors);
        return false;
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

    private boolean tryCompileVariant(@NotNull FragmentVariant variant, @NotNull List<String> errors) {
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

            this.deleteProgram();
            this.programId = program;
            this.vertexShaderId = vertex;
            this.fragmentShaderId = fragment;
            this.cacheUniformLocations();
            return true;

        } catch (Exception ex) {
            String message = "[" + variant.label() + "] " + ex.getMessage();
            errors.add(message);
            LOGGER.warn("[FANCYMENU] GLSL variant '{}' failed to compile/link: {}", variant.label(), ex.getMessage());
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

    private void cacheUniformLocations() {
        this.uniformLocations.clear();
        for (String lookup : CACHED_UNIFORM_LOOKUPS) {
            this.uniformLocations.put(lookup, GL20.glGetUniformLocation(this.programId, lookup));
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

    private void deleteProgram() {
        if (this.programId > 0) {
            if (this.vertexShaderId > 0) {
                GL20.glDetachShader(this.programId, this.vertexShaderId);
            }
            if (this.fragmentShaderId > 0) {
                GL20.glDetachShader(this.programId, this.fragmentShaderId);
            }
            GL20.glDeleteProgram(this.programId);
        }
        if (this.vertexShaderId > 0) {
            GL20.glDeleteShader(this.vertexShaderId);
        }
        if (this.fragmentShaderId > 0) {
            GL20.glDeleteShader(this.fragmentShaderId);
        }

        this.programId = 0;
        this.vertexShaderId = 0;
        this.fragmentShaderId = 0;
        this.uniformLocations.clear();
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
                                int screenHeightPx) {

        GlslRuntimeEventTracker.InputSnapshot input = GlslRuntimeEventTracker.snapshot();
        if (!settings.useInput()) {
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

        float frameRate = minecraft.getFps();
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
        if (input.lastMouseClickX().length > 0 && input.lastMouseClickY().length > 0) {
            clickX = (input.lastMouseClickX()[0] - areaX) * window.getGuiScale();
            clickY = (areaHeight - (input.lastMouseClickY()[0] - areaY)) * window.getGuiScale();
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
                (float) localMousePxX,
                (float) localMousePxYBottom,
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
        float[] iChannelResolution = new float[]{
                areaWidthPx, areaHeightPx, 1.0F,
                0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F
        };
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
        setUniform1f("fmGameDeltaTicks", minecraft.getTimer().getGameTimeDeltaTicks());
        setUniform1f("fmRealtimeDeltaTicks", minecraft.getTimer().getRealtimeDeltaTicks());
        setUniform1i("fmInWorld", minecraft.level != null ? 1 : 0);
        setUniform1i("fmIsPaused", minecraft.isPaused() ? 1 : 0);
        setUniform1f("fmOpacity", Mth.clamp(settings.opacity(), 0.0F, 1.0F));
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

    private void setUniform1f(@NotNull String name, float value) {
        Integer location = this.uniformLocations.get(name);
        if (location != null && location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    private void setUniform2f(@NotNull String name, float v1, float v2) {
        Integer location = this.uniformLocations.get(name);
        if (location != null && location >= 0) {
            GL20.glUniform2f(location, v1, v2);
        }
    }

    private void setUniform3f(@NotNull String name, float v1, float v2, float v3) {
        Integer location = this.uniformLocations.get(name);
        if (location != null && location >= 0) {
            GL20.glUniform3f(location, v1, v2, v3);
        }
    }

    private void setUniform4f(@NotNull String name, float v1, float v2, float v3, float v4) {
        Integer location = this.uniformLocations.get(name);
        if (location != null && location >= 0) {
            GL20.glUniform4f(location, v1, v2, v3, v4);
        }
    }

    private void setUniform1i(@NotNull String name, int value) {
        Integer location = this.uniformLocations.get(name);
        if (location != null && location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    private void setUniform4i(@NotNull String name, int v1, int v2, int v3, int v4) {
        Integer location = this.uniformLocations.get(name);
        if (location != null && location >= 0) {
            GL20.glUniform4i(location, v1, v2, v3, v4);
        }
    }

    private void setUniform1fv(@NotNull String name, @NotNull float[] values) {
        Integer location = this.uniformLocations.get(name);
        if (location != null && location >= 0) {
            GL20.glUniform1fv(location, values);
        }
    }

    private void setUniform3fv(@NotNull String name, @NotNull float[] values) {
        Integer location = this.uniformLocations.get(name);
        if (location != null && location >= 0) {
            GL20.glUniform3fv(location, values);
        }
    }

}
