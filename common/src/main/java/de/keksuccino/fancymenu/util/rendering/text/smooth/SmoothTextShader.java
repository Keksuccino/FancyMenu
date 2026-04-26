package de.keksuccino.fancymenu.util.rendering.text.smooth;

final class SmoothTextShader {

    private static final float DEFAULT_SDF_SHARPNESS = 1.0F;
    private static final float DEFAULT_SDF_EDGE = 0.5F;

    private static volatile Float runtimeSharpness;
    private static volatile Float runtimeEdge;

    private SmoothTextShader() {
    }

    static void applyDefaults() {
    }

    static void applySdfRange(float sdfRange) {
    }

    static void applyEdge(float edge) {
    }

    static void applySharpness(float sharpness) {
    }

    static void setRuntimeSharpness(float sharpness) {
        runtimeSharpness = Math.max(0.05F, sharpness);
    }

    static void clearRuntimeSharpness() {
        runtimeSharpness = null;
    }

    static float getResolvedSharpness() {
        return runtimeSharpness != null ? runtimeSharpness : DEFAULT_SDF_SHARPNESS;
    }

    static void setRuntimeEdge(float edge) {
        runtimeEdge = edge;
    }

    static void clearRuntimeEdge() {
        runtimeEdge = null;
    }

    static float getResolvedEdge() {
        return runtimeEdge != null ? runtimeEdge : DEFAULT_SDF_EDGE;
    }

    static void clear() {
    }

}
