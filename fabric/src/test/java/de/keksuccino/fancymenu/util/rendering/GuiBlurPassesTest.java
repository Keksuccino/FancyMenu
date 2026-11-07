package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBlurPassesTest {

    @Test
    void levelPassesReplaceTheirAttachmentWithoutBlending() throws IllegalAccessException {
        for (RenderPipeline pipeline : List.of(pipeline("DOWNSAMPLE_PIPELINE"), pipeline("GAUSSIAN_PIPELINE"))) {
            assertAll(() -> assertTrue(pipeline.getBlendFunction().isEmpty()), () -> assertTrue(pipeline.isWriteColor()), () -> assertTrue(pipeline.isWriteAlpha()), () -> assertFalse(pipeline.isWriteDepth()));
        }
    }

    @Test
    void compositeBlendsPremultipliedColorAndKeepsTheMainTargetAlpha() throws IllegalAccessException {
        RenderPipeline pipeline = pipeline("COMPOSITE_PIPELINE");
        BlendFunction blend = pipeline.getBlendFunction().orElseThrow();

        assertAll(() -> assertTrue(pipeline.isWriteColor()), () -> assertFalse(pipeline.isWriteAlpha()), () -> assertFalse(pipeline.isWriteDepth()), () -> assertEquals(SourceFactor.ONE, blend.sourceColor()), () -> assertEquals(DestFactor.ONE_MINUS_SRC_ALPHA, blend.destColor()));
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
