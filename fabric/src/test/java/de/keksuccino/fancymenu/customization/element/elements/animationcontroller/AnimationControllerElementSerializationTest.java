package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationControllerElementSerializationTest {

    @Test
    void keyframePackageRefactorPreservesAndNormalizesSerializedLayoutData() {
        AnimationControllerElementBuilder builder = new AnimationControllerElementBuilder();
        AnimationControllerElement source = builder.buildDefaultInstance();
        source.keyframes = new ArrayList<>();
        source.keyframes.add(new AnimationKeyframe(200L, 2, 3, 4, 5, ElementAnchorPoints.BOTTOM_RIGHT, true));
        source.keyframes.add(new AnimationKeyframe(-10L, 6, 7, 8, 9, ElementAnchorPoints.TOP_LEFT, false));

        SerializedElement serialized = builder.serializeElementInternal(source);
        String keyframesJson = serialized.getValue("keyframes");
        AnimationControllerElement restored = builder.deserializeElement(serialized);

        assertTrue(keyframesJson.contains("\"timestamp\""));
        assertTrue(keyframesJson.contains("\"uniqueIdentifier\""));
        assertFalse(keyframesJson.contains("animationcontroller.keyframe"));
        assertEquals(0L, restored.keyframes.get(0).timestamp);
        assertEquals(200L, restored.keyframes.get(restored.keyframes.size() - 1).timestamp);
        assertSame(ElementAnchorPoints.TOP_LEFT, restored.keyframes.get(0).anchorPoint);
        assertSame(ElementAnchorPoints.BOTTOM_RIGHT, restored.keyframes.get(restored.keyframes.size() - 1).anchorPoint);
    }

}
