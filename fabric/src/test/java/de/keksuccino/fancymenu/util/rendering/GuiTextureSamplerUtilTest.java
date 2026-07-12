package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiTextureSamplerUtilTest {

    private static final int TARGET_TEXTURE = 41;
    private static final int PREVIOUS_TEXTURE = 17;

    @Test
    void leavesAlreadyClampedTextureUnchanged() {
        FakeTextureParameterBackend backend = backendWithTargetWrap(GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);

        GuiTextureSamplerUtil.runWithClampToEdge(TARGET_TEXTURE, () -> assertEquals(new WrapModes(GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE), backend.wrapModes(TARGET_TEXTURE)), backend);

        assertEquals(new WrapModes(GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE), backend.wrapModes(TARGET_TEXTURE));
        assertEquals(0, backend.textureParameterChanges);
    }

    @Test
    void restoresRepeatWrappingAfterAction() {
        FakeTextureParameterBackend backend = backendWithTargetWrap(GL11.GL_REPEAT, GL11.GL_REPEAT);

        GuiTextureSamplerUtil.runWithClampToEdge(TARGET_TEXTURE, () -> assertEquals(new WrapModes(GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE), backend.wrapModes(TARGET_TEXTURE)), backend);

        assertEquals(new WrapModes(GL11.GL_REPEAT, GL11.GL_REPEAT), backend.wrapModes(TARGET_TEXTURE));
    }

    @Test
    void clampsAndRestoresAxesIndependently() {
        FakeTextureParameterBackend backend = backendWithTargetWrap(GL11.GL_REPEAT, GL12.GL_CLAMP_TO_EDGE);

        GuiTextureSamplerUtil.runWithClampToEdge(TARGET_TEXTURE, () -> assertEquals(new WrapModes(GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE), backend.wrapModes(TARGET_TEXTURE)), backend);

        assertEquals(new WrapModes(GL11.GL_REPEAT, GL12.GL_CLAMP_TO_EDGE), backend.wrapModes(TARGET_TEXTURE));
        assertEquals(2, backend.textureParameterChanges);
    }

    @Test
    void restoresWrappingWhenActionThrows() {
        FakeTextureParameterBackend backend = backendWithTargetWrap(GL11.GL_REPEAT, GL14.GL_MIRRORED_REPEAT);
        RuntimeException failure = new RuntimeException("post effect failed");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> GuiTextureSamplerUtil.runWithClampToEdge(TARGET_TEXTURE, () -> {
            backend.setActiveTexture(GL13.GL_TEXTURE2);
            backend.bindTexture2D(59);
            throw failure;
        }, backend));

        assertSame(failure, thrown);
        assertEquals(new WrapModes(GL11.GL_REPEAT, GL14.GL_MIRRORED_REPEAT), backend.wrapModes(TARGET_TEXTURE));
        assertEquals(GL13.GL_TEXTURE0, backend.getActiveTexture());
        assertEquals(PREVIOUS_TEXTURE, backend.getBoundTexture2D());
    }

    @Test
    void restoresActiveTextureUnitAndItsPreviousBinding() {
        FakeTextureParameterBackend backend = backendWithTargetWrap(GL11.GL_REPEAT, GL11.GL_REPEAT);
        int previousUnit = GL13.GL_TEXTURE2;
        int changedUnit = GL13.GL_TEXTURE3;
        int actionTexture = 73;
        backend.setActiveTexture(previousUnit);
        backend.bindTexture2D(PREVIOUS_TEXTURE);

        GuiTextureSamplerUtil.runWithClampToEdge(TARGET_TEXTURE, () -> {
            assertEquals(previousUnit, backend.getActiveTexture());
            assertEquals(PREVIOUS_TEXTURE, backend.getBoundTexture2D());
            assertEquals(new WrapModes(GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE), backend.wrapModes(TARGET_TEXTURE));
            backend.setActiveTexture(changedUnit);
            backend.bindTexture2D(actionTexture);
        }, backend);

        assertEquals(previousUnit, backend.getActiveTexture());
        assertEquals(PREVIOUS_TEXTURE, backend.getBoundTexture2D());
        assertEquals(actionTexture, backend.boundTexture(changedUnit));
    }

    @Test
    void sequentialActionsDoNotLeakWrappingOrBindingState() {
        FakeTextureParameterBackend backend = backendWithTargetWrap(GL11.GL_REPEAT, GL14.GL_MIRRORED_REPEAT);

        for (int i = 0; i < 3; i++) {
            GuiTextureSamplerUtil.runWithClampToEdge(TARGET_TEXTURE, () -> assertEquals(new WrapModes(GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE), backend.wrapModes(TARGET_TEXTURE)), backend);
            assertEquals(new WrapModes(GL11.GL_REPEAT, GL14.GL_MIRRORED_REPEAT), backend.wrapModes(TARGET_TEXTURE));
            assertEquals(GL13.GL_TEXTURE0, backend.getActiveTexture());
            assertEquals(PREVIOUS_TEXTURE, backend.getBoundTexture2D());
        }
    }

    private static FakeTextureParameterBackend backendWithTargetWrap(int wrapS, int wrapT) {
        FakeTextureParameterBackend backend = new FakeTextureParameterBackend();
        backend.bindTexture2D(PREVIOUS_TEXTURE);
        backend.defineTexture(TARGET_TEXTURE, wrapS, wrapT);
        return backend;
    }

    private record WrapModes(int wrapS, int wrapT) {
    }

    private static final class FakeTextureParameterBackend implements GuiTextureSamplerUtil.TextureParameterBackend {

        private final Map<Integer, Integer> bindings = new HashMap<>();
        private final Map<Integer, WrapModes> textures = new HashMap<>();
        private int activeTexture = GL13.GL_TEXTURE0;
        private int textureParameterChanges;

        private FakeTextureParameterBackend() {
            this.bindings.put(this.activeTexture, 0);
        }

        private void defineTexture(int textureId, int wrapS, int wrapT) {
            this.textures.put(textureId, new WrapModes(wrapS, wrapT));
        }

        private WrapModes wrapModes(int textureId) {
            return this.textures.get(textureId);
        }

        private int boundTexture(int textureUnit) {
            return this.bindings.getOrDefault(textureUnit, 0);
        }

        @Override
        public int getActiveTexture() {
            return this.activeTexture;
        }

        @Override
        public int getBoundTexture2D() {
            return this.boundTexture(this.activeTexture);
        }

        @Override
        public int getTextureParameter(int parameter) {
            WrapModes wrapModes = this.currentWrapModes();
            if (parameter == GL11.GL_TEXTURE_WRAP_S) {
                return wrapModes.wrapS();
            }
            if (parameter == GL11.GL_TEXTURE_WRAP_T) {
                return wrapModes.wrapT();
            }
            throw new IllegalArgumentException("Unexpected texture parameter: " + parameter);
        }

        @Override
        public void setActiveTexture(int textureUnit) {
            this.activeTexture = textureUnit;
        }

        @Override
        public void bindTexture2D(int textureId) {
            this.bindings.put(this.activeTexture, textureId);
        }

        @Override
        public void setTextureParameter(int parameter, int value) {
            int textureId = this.getBoundTexture2D();
            WrapModes wrapModes = this.currentWrapModes();
            if (parameter == GL11.GL_TEXTURE_WRAP_S) {
                this.textures.put(textureId, new WrapModes(value, wrapModes.wrapT()));
            } else if (parameter == GL11.GL_TEXTURE_WRAP_T) {
                this.textures.put(textureId, new WrapModes(wrapModes.wrapS(), value));
            } else {
                throw new IllegalArgumentException("Unexpected texture parameter: " + parameter);
            }
            this.textureParameterChanges++;
        }

        private WrapModes currentWrapModes() {
            int textureId = this.getBoundTexture2D();
            WrapModes wrapModes = this.textures.get(textureId);
            if (wrapModes == null) {
                throw new IllegalStateException("No texture state defined for texture " + textureId);
            }
            return wrapModes;
        }
    }
}
