package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Test class to validate perspective distort implementation
 */
public class PerspectiveDistortTest {
    
    public static void testPerspectiveDistort() {
        // Create a test element
        TestElement element = new TestElement(null);
        
        // Test perspective distort support
        assert element.supportsPerspectiveDistort() : "Element should support perspective distort by default";
        
        // Test setting perspective distort values
        element.perspectiveDistortTopLeftX = 10.0F;
        element.perspectiveDistortTopLeftY = 5.0F;
        assert element.hasPerspectiveDistortion() : "Element should have perspective distortion";
        
        // Test resetting perspective distortion
        element.resetPerspectiveDistortion();
        assert !element.hasPerspectiveDistortion() : "Element should not have perspective distortion after reset";
        
        // Test disabling perspective distort support
        element.setSupportsPerspectiveDistort(false);
        assert !element.supportsPerspectiveDistort() : "Element should not support perspective distort when disabled";
        
        System.out.println("Perspective distort tests passed!");
    }
    
    static class TestElement extends AbstractElement {
        public TestElement(ElementBuilder<?, ?> builder) {
            super(builder);
        }
        
        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            // Test render implementation
        }
    }
}
