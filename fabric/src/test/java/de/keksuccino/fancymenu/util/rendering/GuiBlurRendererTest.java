package de.keksuccino.fancymenu.util.rendering;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurRendererTest {

    @ParameterizedTest
    @MethodSource("blurPipelines")
    void matchesSingleColorAttachmentWithoutBlendingOrDepthWrites(RenderPipeline pipeline) {
        assertEquals(1, pipeline.getColorTargetStates().size());
        ColorTargetState target = pipeline.getColorTargetStates().getFirst();
        assertNotNull(target);
        // Copy and blur replace pixels; the composite shader already applies its mask and tint.
        assertAll(() -> assertEquals(GpuFormat.RGBA8_UNORM, target.format()), () -> assertEquals(ColorTargetState.WRITE_ALL, target.writeMask()), () -> assertTrue(target.blendFunction().isEmpty()), () -> assertNull(pipeline.getDepthStencilState()));
    }

    private static List<RenderPipeline> blurPipelines() throws IllegalAccessException {
        List<RenderPipeline> pipelines = new ArrayList<>();
        // Inspect the actual renderer pipelines without exposing implementation details in the production API.
        for (Field field : GuiBlurRenderer.class.getDeclaredFields()) {
            if (field.getType() == RenderPipeline.class) {
                field.setAccessible(true);
                pipelines.add((RenderPipeline) field.get(null));
            }
        }
        assertEquals(3, pipelines.size());
        return pipelines;
    }

}
