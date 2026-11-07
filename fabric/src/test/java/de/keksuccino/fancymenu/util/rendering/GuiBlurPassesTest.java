package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurPassesTest {

    @Test
    void levelPassesReplaceOneHalfFloatAttachment() throws IllegalAccessException {
        for (RenderPipeline pipeline : List.of(pipeline("DOWNSAMPLE_PIPELINE"), pipeline("GAUSSIAN_PIPELINE"))) {
            ColorTargetState target = singleTarget(pipeline);
            assertAll(() -> assertEquals(GpuFormat.RGBA16_FLOAT, target.format()), () -> assertEquals(ColorTargetState.WRITE_ALL, target.writeMask()), () -> assertTrue(target.blendFunction().isEmpty()), () -> assertNull(pipeline.getDepthStencilState()));
        }
    }

    @Test
    void compositeBlendsPremultipliedColorAndKeepsTheMainTargetAlpha() throws IllegalAccessException {
        RenderPipeline pipeline = pipeline("COMPOSITE_PIPELINE");
        ColorTargetState target = singleTarget(pipeline);
        BlendFunction blend = target.blendFunction().orElseThrow();

        assertAll(() -> assertEquals(GpuFormat.RGBA8_UNORM, target.format()), () -> assertEquals(ColorTargetState.WRITE_COLOR, target.writeMask()), () -> assertNull(pipeline.getDepthStencilState()), () -> assertEquals(BlendFactor.ONE, blend.color().sourceFactor()), () -> assertEquals(BlendFactor.ONE_MINUS_SRC_ALPHA, blend.color().destFactor()));
    }

    @Test
    void rendersWithExactlyTheThreeBlurPipelines() throws IllegalAccessException {
        List<RenderPipeline> pipelines = new ArrayList<>();
        // Inspect the actual renderer pipelines without exposing implementation details in the production API.
        for (Field field : GuiBlurPasses.class.getDeclaredFields()) {
            if (field.getType() == RenderPipeline.class) {
                field.setAccessible(true);
                pipelines.add((RenderPipeline) field.get(null));
            }
        }
        assertEquals(3, pipelines.size());
    }

    private static ColorTargetState singleTarget(RenderPipeline pipeline) {
        assertEquals(1, pipeline.getColorTargetStates().length);
        return pipeline.getColorTargetStates()[0];
    }

    private static RenderPipeline pipeline(String name) throws IllegalAccessException {
        try {
            Field field = GuiBlurPasses.class.getDeclaredField(name);
            field.setAccessible(true);
            return (RenderPipeline) field.get(null);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing blur pipeline " + name, e);
        }
    }

}
