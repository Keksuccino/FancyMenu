package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.customization.variables.UserVariableSnapshot;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Owns per-runtime clock/input lifecycle and captures one immutable uniform snapshot for every pass in a frame. */
final class GlslFrameUniformState {

    private static final Pattern VARIABLE_COMPONENT_SPLIT_PATTERN = Pattern.compile("[\\s,;|]+");

    private long lastFrameNanoTime = -1L;
    private double accumulatedTimeSeconds;
    private float lastFrameDeltaSeconds;
    private float lastFrameRate;
    private final GlslInputDeltaState inputDeltaState = new GlslInputDeltaState();
    private float lastShadertoyMouseX;
    private float lastShadertoyMouseY;
    private boolean hasShadertoyMousePosition;

    void onRuntimeResourcesClosed() {
        // close() is also the resize hook. Re-baseline frame deltas and persistent global input while animation time
        // itself remains continuous; the runtime resets each pass's iFrame alongside its GPU resources.
        this.lastFrameNanoTime = -1L;
        this.lastFrameDeltaSeconds = 0.0F;
        this.lastFrameRate = 0.0F;
        this.inputDeltaState.reset();
        this.lastShadertoyMouseX = 0.0F;
        this.lastShadertoyMouseY = 0.0F;
        this.hasShadertoyMousePosition = false;
    }

    @NotNull
    FrameSnapshot capture(@NotNull Minecraft minecraft, @NotNull Window window, @NotNull GlslShaderRuntime.RenderSettings settings, float partialTick, int areaX, int areaY, int areaWidth, int areaHeight, int areaWidthPx, int areaHeightPx, int screenWidthPx, int screenHeightPx) {
        this.tickTime(settings);
        if (settings.useInput()) {
            GlslRuntimeEventTracker.syncMouseButtonsFromWindow(window.handle());
        }
        GlslRuntimeEventTracker.InputSnapshot trackedInput = GlslRuntimeEventTracker.snapshot();
        GlslRuntimeEventTracker.InputSnapshot input = settings.useInput() ? trackedInput : disabledInput(areaX, areaY, areaHeight);
        GlslInputDeltaState.Delta inputDelta = this.inputDeltaState.capture(trackedInput.mouseScrollTotalX(), trackedInput.mouseScrollTotalY(), trackedInput.mouseDeltaX(), trackedInput.mouseDeltaY(), trackedInput.mouseMoveEventCounter(), settings.useInput());

        float timeDelta = settings.freezeTime() ? 0.0F : this.lastFrameDeltaSeconds * Math.max(0.0F, settings.timeScale());
        float frameRate = minecraft.getFps();
        if (frameRate <= 0.0F && this.lastFrameRate > 0.0F) {
            frameRate = this.lastFrameRate;
        }

        double guiScale = window.getGuiScale();
        double localMousePxX = (input.mouseX() - areaX) * guiScale;
        double localMousePxYBottom = (areaHeight - (input.mouseY() - areaY)) * guiScale;
        double localMouseNormX = areaWidthPx > 0 ? localMousePxX / areaWidthPx : 0.0D;
        double localMouseNormY = areaHeightPx > 0 ? localMousePxYBottom / areaHeightPx : 0.0D;
        boolean leftMouseDown = input.mouseButtonStates().length > 0 && input.mouseButtonStates()[0];
        boolean hasClickData = input.lastMouseClickNanos().length > 0 && input.lastMouseClickNanos()[0] > 0L;
        double clickX = hasClickData && input.lastMouseClickX().length > 0 ? (input.lastMouseClickX()[0] - areaX) * guiScale : 0.0D;
        double clickY = hasClickData && input.lastMouseClickY().length > 0 ? (areaHeight - (input.lastMouseClickY()[0] - areaY)) * guiScale : 0.0D;

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

        double mouseScrollDeltaX = inputDelta.scrollX();
        double mouseScrollDeltaY = inputDelta.scrollY();
        double mouseDeltaX = inputDelta.mouseX();
        double mouseDeltaY = inputDelta.mouseY();
        if (!settings.useInput()) {
            this.lastShadertoyMouseX = 0.0F;
            this.lastShadertoyMouseY = 0.0F;
            this.hasShadertoyMousePosition = false;
        }

        ZonedDateTime now = ZonedDateTime.now();
        Instant instant = now.toInstant();
        int unixSeconds = (int) instant.getEpochSecond();
        int unixMillis = now.getNano() / 1_000_000;
        int secondOfDay = now.getHour() * 3600 + now.getMinute() * 60 + now.getSecond();
        float dateSeconds = secondOfDay + now.getNano() / 1_000_000_000.0F;
        int[] mouseButtons = new int[]{buttonState(input.mouseButtonStates(), 0), buttonState(input.mouseButtonStates(), 1), buttonState(input.mouseButtonStates(), 2), buttonState(input.mouseButtonStates(), 3)};
        int[] clickCounts = new int[]{arrayValue(input.mouseClickCounts(), 0), arrayValue(input.mouseClickCounts(), 1), arrayValue(input.mouseClickCounts(), 2), arrayValue(input.mouseClickCounts(), 3)};
        int[] releaseCounts = new int[]{arrayValue(input.mouseReleaseCounts(), 0), arrayValue(input.mouseReleaseCounts(), 1), arrayValue(input.mouseReleaseCounts(), 2), arrayValue(input.mouseReleaseCounts(), 3)};

        VariableSnapshot variableSnapshot = buildVariableValues();
        return new FrameSnapshot(
                (float) this.accumulatedTimeSeconds,
                timeDelta,
                frameRate,
                new float[]{iMouseX, iMouseY, (float) (leftMouseDown ? clickX : -Math.abs(clickX)), (float) (leftMouseDown ? clickY : -Math.abs(clickY))},
                new float[]{now.getYear(), now.getMonthValue(), now.getDayOfMonth(), dateSeconds},
                (float) guiScale,
                new float[]{(float) localMousePxX, (float) localMousePxYBottom, (float) localMouseNormX, (float) localMouseNormY},
                new float[]{(float) (mouseDeltaX * guiScale), (float) (-mouseDeltaY * guiScale)},
                mouseButtons,
                clickCounts,
                releaseCounts,
                new float[]{(float) mouseScrollDeltaX, (float) mouseScrollDeltaY},
                new float[]{settings.useInput() ? (float) input.mouseScrollTotalX() : 0.0F, settings.useInput() ? (float) input.mouseScrollTotalY() : 0.0F},
                new int[]{input.lastKeyCode(), input.lastScanCode(), input.lastKeyModifiers(), input.lastKeyAction()},
                input.keyEventCounter(),
                new int[]{input.lastCharCodePoint(), input.lastCharModifiers(), 0, 0},
                input.charEventCounter(),
                new int[]{now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getDayOfWeek().getValue()},
                new int[]{now.getHour(), now.getMinute(), now.getSecond(), unixMillis},
                now.getDayOfYear(),
                now.get(WeekFields.ISO.weekOfWeekBasedYear()),
                unixSeconds,
                unixMillis,
                partialTick,
                minecraft.getDeltaTracker().getGameTimeDeltaTicks(),
                minecraft.getDeltaTracker().getRealtimeDeltaTicks(),
                minecraft.level != null ? 1 : 0,
                minecraft.isPaused() ? 1 : 0,
                Mth.clamp(settings.opacity(), 0.0F, 1.0F),
                screenWidthPx,
                screenHeightPx,
                areaWidthPx,
                areaHeightPx,
                variableSnapshot.values(),
                variableSnapshot.originalCount()
        );
    }

    private void tickTime(@NotNull GlslShaderRuntime.RenderSettings settings) {
        long now = System.nanoTime();
        if (this.lastFrameNanoTime < 0L) {
            this.lastFrameNanoTime = now;
        }
        double rawDelta = Math.max(0.0D, (now - this.lastFrameNanoTime) / 1_000_000_000.0D);
        if (rawDelta > 1.5D) {
            rawDelta = 0.0D;
        }
        this.lastFrameNanoTime = now;
        this.lastFrameDeltaSeconds = (float) rawDelta;
        this.lastFrameRate = this.lastFrameDeltaSeconds > 0.0F ? 1.0F / this.lastFrameDeltaSeconds : 0.0F;
        if (!settings.freezeTime()) {
            this.accumulatedTimeSeconds += rawDelta * Math.max(0.0F, settings.timeScale());
        }
    }

    @NotNull
    private static GlslRuntimeEventTracker.InputSnapshot disabledInput(int areaX, int areaY, int areaHeight) {
        return new GlslRuntimeEventTracker.InputSnapshot(areaX, areaY + areaHeight, 0.0D, 0.0D, 0.0D, 0.0D, new boolean[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS], new int[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS], new int[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS], new double[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS], new double[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS], new long[GlslRuntimeEventTracker.TRACKED_MOUSE_BUTTONS], 0, 0, -1, -1, 0, GlslRuntimeEventTracker.KEY_ACTION_RELEASE, 0, -1, 0);
    }

    private static int buttonState(@NotNull boolean[] values, int index) {
        return index >= 0 && index < values.length && values[index] ? 1 : 0;
    }

    private static int arrayValue(@NotNull int[] values, int index) {
        return index >= 0 && index < values.length ? values[index] : 0;
    }

    @NotNull
    private static VariableSnapshot buildVariableValues() {
        List<UserVariableSnapshot> variables = new ArrayList<>(VariableHandler.getVariableSnapshots());
        variables.sort(Comparator.comparing(UserVariableSnapshot::name));
        Map<String, VariableValue> values = new LinkedHashMap<>();
        for (UserVariableSnapshot variable : variables) {
            String suffix = toVariableUniformSuffix(variable.name());
            if (!suffix.isEmpty()) {
                values.put(suffix, parseVariableValue(variable.value()));
            }
        }
        return new VariableSnapshot(Map.copyOf(values), variables.size());
    }

    @NotNull
    private static String toVariableUniformSuffix(@Nullable String variableName) {
        if (variableName == null || variableName.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(variableName.length() + 2);
        for (int i = 0; i < variableName.length(); i++) {
            char c = variableName.charAt(i);
            builder.append((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' ? c : '_');
        }
        if (!builder.isEmpty() && builder.charAt(0) >= '0' && builder.charAt(0) <= '9') {
            builder.insert(0, '_');
        }
        return builder.toString();
    }

    @NotNull
    private static VariableValue parseVariableValue(@Nullable String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim();
        float floatValue = parseFloatValue(normalized);
        int intValue = parseIntValue(normalized, floatValue);
        int boolValue = parseBoolValue(normalized, floatValue);
        float[] vec4 = parseVec4Value(normalized, floatValue);
        return new VariableValue(floatValue, intValue, boolValue, vec4);
    }

    private static float parseFloatValue(@NotNull String value) {
        if (value.isEmpty()) {
            return 0.0F;
        }
        try {
            return Float.parseFloat(value);
        } catch (Exception ignored) {
        }
        float[] components = parseFloatComponents(value);
        return components.length > 0 ? components[0] : parseBoolValue(value, 0.0F);
    }

    private static int parseIntValue(@NotNull String value, float fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
        }
        try {
            return (int) Float.parseFloat(value);
        } catch (Exception ignored) {
        }
        float[] components = parseFloatComponents(value);
        return components.length > 0 ? (int) components[0] : (int) fallback;
    }

    private static int parseBoolValue(@NotNull String value, float fallback) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.equals("true") || lower.equals("yes") || lower.equals("on") || lower.equals("enabled")) {
            return 1;
        }
        if (lower.equals("false") || lower.equals("no") || lower.equals("off") || lower.equals("disabled")) {
            return 0;
        }
        try {
            return Float.parseFloat(value) != 0.0F ? 1 : 0;
        } catch (Exception ignored) {
        }
        return fallback != 0.0F ? 1 : 0;
    }

    @NotNull
    private static float[] parseVec4Value(@NotNull String value, float fallback) {
        float[] components = parseFloatComponents(value);
        if (components.length == 0) {
            return new float[]{fallback, fallback, fallback, fallback};
        }
        float[] vec4 = new float[4];
        for (int i = 0; i < vec4.length; i++) {
            vec4[i] = i < components.length ? components[i] : components[components.length - 1];
        }
        return vec4;
    }

    @NotNull
    private static float[] parseFloatComponents(@NotNull String value) {
        if (value.isEmpty()) {
            return new float[0];
        }
        String[] tokens = VARIABLE_COMPONENT_SPLIT_PATTERN.split(value);
        List<Float> components = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            try {
                components.add(Float.parseFloat(token));
            } catch (Exception ignored) {
            }
        }
        float[] result = new float[components.size()];
        for (int i = 0; i < components.size(); i++) {
            result[i] = components.get(i);
        }
        return result;
    }

    record PassContext(int areaXPx, int areaYPxTop, int areaYPxBottom, int renderAreaX, int renderAreaYBottom, int renderAreaWidth, int renderAreaHeight, int renderTargetWidth, int renderTargetHeight, int frame) {
    }

    record ChannelResolution(float width, float height) {
    }

    record FrameSnapshot(float time, float timeDelta, float frameRate, @NotNull float[] iMouse, @NotNull float[] date, float guiScale, @NotNull float[] mouse, @NotNull float[] mouseDelta, @NotNull int[] mouseButtons, @NotNull int[] mouseClickCount, @NotNull int[] mouseReleaseCount, @NotNull float[] mouseScroll, @NotNull float[] mouseScrollTotal, @NotNull int[] keyEvent, int keyEventCount, @NotNull int[] charEvent, int charEventCount, @NotNull int[] dateParts, @NotNull int[] timeParts, int dayOfYear, int weekOfYear, int unixTimeSeconds, int unixTimeMilliseconds, float partialTick, float gameDeltaTicks, float realtimeDeltaTicks, int inWorld, int paused, float opacity, int screenWidth, int screenHeight, int areaWidth, int areaHeight, @NotNull Map<String, VariableValue> variables, int originalVariableCount) {

        void write(@NotNull GlslStd140Layout layout, @NotNull ByteBuffer buffer, @NotNull PassContext pass, @NotNull ChannelResolution[] channels) {
            layout.writeFloats(buffer, GlslShaderSourceTransformer.RENDER_AREA_UNIFORM, pass.renderAreaX(), pass.renderAreaYBottom(), pass.renderAreaWidth(), pass.renderAreaHeight());
            layout.writeFloats(buffer, GlslShaderSourceTransformer.RENDER_TARGET_SIZE_UNIFORM, pass.renderTargetWidth(), pass.renderTargetHeight());
            layout.writeFloats(buffer, "iResolution", this.areaWidth, this.areaHeight, 1.0F);
            layout.writeFloats(buffer, "iTime", this.time);
            layout.writeFloats(buffer, "iTimeDelta", this.timeDelta);
            layout.writeFloats(buffer, "iFrameRate", this.frameRate);
            layout.writeInts(buffer, "iFrame", pass.frame());
            layout.writeFloats(buffer, "iMouse", this.iMouse);
            layout.writeFloats(buffer, "iDate", this.date);
            layout.writeFloats(buffer, "iSampleRate", 44_100.0F);
            layout.writeFloats(buffer, "iChannelTime", this.time, this.time, this.time, this.time);
            float[] channelResolution = new float[12];
            for (int i = 0; i < Math.min(4, channels.length); i++) {
                channelResolution[i * 3] = channels[i].width();
                channelResolution[i * 3 + 1] = channels[i].height();
                channelResolution[i * 3 + 2] = channels[i].width() > 0.0F && channels[i].height() > 0.0F ? 1.0F : 0.0F;
            }
            layout.writeFloats(buffer, "iChannelResolution", channelResolution);
            layout.writeFloats(buffer, "fmAreaOffset", pass.areaXPx(), pass.areaYPxBottom());
            layout.writeFloats(buffer, "fmAreaSize", this.areaWidth, this.areaHeight);
            layout.writeFloats(buffer, "fmAreaPosition", pass.areaXPx(), pass.areaYPxBottom());
            layout.writeFloats(buffer, "fmAreaTopLeft", pass.areaXPx(), pass.areaYPxTop());
            layout.writeFloats(buffer, "fmScreenSize", this.screenWidth, this.screenHeight);
            layout.writeFloats(buffer, "fmGuiScale", this.guiScale);
            layout.writeFloats(buffer, "fmMouse", this.mouse);
            layout.writeFloats(buffer, "fmMouseDelta", this.mouseDelta);
            layout.writeInts(buffer, "fmMouseButtons", this.mouseButtons);
            layout.writeInts(buffer, "fmMouseClickCount", this.mouseClickCount);
            layout.writeInts(buffer, "fmMouseReleaseCount", this.mouseReleaseCount);
            layout.writeFloats(buffer, "fmMouseScroll", this.mouseScroll);
            layout.writeFloats(buffer, "fmMouseScrollTotal", this.mouseScrollTotal);
            layout.writeInts(buffer, "fmKeyEvent", this.keyEvent);
            layout.writeInts(buffer, "fmKeyEventCount", this.keyEventCount);
            layout.writeInts(buffer, "fmCharEvent", this.charEvent);
            layout.writeInts(buffer, "fmCharEventCount", this.charEventCount);
            layout.writeInts(buffer, "fmDateParts", this.dateParts);
            layout.writeInts(buffer, "fmTimeParts", this.timeParts);
            layout.writeInts(buffer, "fmDayOfYear", this.dayOfYear);
            layout.writeInts(buffer, "fmWeekOfYear", this.weekOfYear);
            layout.writeInts(buffer, "fmUnixTimeSeconds", this.unixTimeSeconds);
            layout.writeInts(buffer, "fmUnixTimeMilliseconds", this.unixTimeMilliseconds);
            layout.writeFloats(buffer, "fmPartialTick", this.partialTick);
            layout.writeFloats(buffer, "fmGameDeltaTicks", this.gameDeltaTicks);
            layout.writeFloats(buffer, "fmRealtimeDeltaTicks", this.realtimeDeltaTicks);
            layout.writeInts(buffer, "fmInWorld", this.inWorld);
            layout.writeInts(buffer, "fmIsPaused", this.paused);
            layout.writeFloats(buffer, "fmOpacity", this.opacity);
            layout.writeInts(buffer, "fmVariableCount", this.originalVariableCount);
            this.writeVariables(layout, buffer);
        }

        private void writeVariables(@NotNull GlslStd140Layout layout, @NotNull ByteBuffer buffer) {
            for (Map.Entry<String, VariableValue> entry : this.variables.entrySet()) {
                String suffix = entry.getKey();
                VariableValue value = entry.getValue();
                writeFloatIfPresent(layout, buffer, "fmVarFloat_" + suffix, value.floatValue());
                writeIntIfPresent(layout, buffer, "fmVarInt_" + suffix, value.intValue());
                writeIntIfPresent(layout, buffer, "fmVarBool_" + suffix, value.boolValue());
                writeFloatsIfPresent(layout, buffer, "fmVarVec2_" + suffix, value.vector()[0], value.vector()[1]);
                writeFloatsIfPresent(layout, buffer, "fmVarVec3_" + suffix, value.vector()[0], value.vector()[1], value.vector()[2]);
                writeFloatsIfPresent(layout, buffer, "fmVarVec4_" + suffix, value.vector());
                writeIntIfPresent(layout, buffer, "fmVarExists_" + suffix, 1);
            }
        }

        private static void writeFloatIfPresent(@NotNull GlslStd140Layout layout, @NotNull ByteBuffer buffer, @NotNull String name, float value) {
            if (layout.member(name) != null) {
                layout.writeFloats(buffer, name, value);
            }
        }

        private static void writeFloatsIfPresent(@NotNull GlslStd140Layout layout, @NotNull ByteBuffer buffer, @NotNull String name, float... values) {
            if (layout.member(name) != null) {
                layout.writeFloats(buffer, name, values);
            }
        }

        private static void writeIntIfPresent(@NotNull GlslStd140Layout layout, @NotNull ByteBuffer buffer, @NotNull String name, int value) {
            if (layout.member(name) != null) {
                layout.writeInts(buffer, name, value);
            }
        }
    }

    record VariableValue(float floatValue, int intValue, int boolValue, @NotNull float[] vector) {
    }

    private record VariableSnapshot(@NotNull Map<String, VariableValue> values, int originalCount) {
    }
}
