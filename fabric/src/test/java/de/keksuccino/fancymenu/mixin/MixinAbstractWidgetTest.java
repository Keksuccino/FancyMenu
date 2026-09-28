package de.keksuccino.fancymenu.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.mixin.mixins.common.client.MixinAbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinAbstractWidgetTest {

    @Test
    void rotatedClicksContinueToVanillaButtonValidationAndActionDispatch() throws Exception {
        TestWidget widget = new TestWidget();
        widget.setHitboxRotationFancyMenu(90.0F, 0.0F, 0.0F);

        assertFalse(clickHook(widget, 60, 70, InputConstants.MOUSE_BUTTON_LEFT).isCancelled());
        assertFalse(clickHook(widget, 60, 70, InputConstants.MOUSE_BUTTON_RIGHT).isCancelled());
        assertFalse(clickHook(widget, 15, 30, InputConstants.MOUSE_BUTTON_LEFT).isCancelled());
    }

    @Test
    void tiltedClicksAndInactiveWidgetsStillUseVanillaDispatch() throws Exception {
        TestWidget widget = new TestWidget();
        widget.setHitboxRotationFancyMenu(37.0F, -42.0F, 53.0F);
        assertFalse(clickHook(widget, 60, 30, InputConstants.MOUSE_BUTTON_LEFT).isCancelled());

        widget.active = false;
        assertFalse(clickHook(widget, 60, 30, InputConstants.MOUSE_BUTTON_LEFT).isCancelled());
        widget.visible = false;
        assertFalse(clickHook(widget, 60, 30, InputConstants.MOUSE_BUTTON_LEFT).isCancelled());
    }

    @Test
    void hiddenWidgetsStillCancelClicksAndResetRestoresDispatch() throws Exception {
        TestWidget widget = new TestWidget();
        widget.setHitboxRotationFancyMenu(90.0F, 0.0F, 0.0F);
        widget.setHiddenFancyMenu(true);
        CallbackInfoReturnable<Boolean> hiddenClick = clickHook(widget, 60, 70, InputConstants.MOUSE_BUTTON_LEFT);
        assertTrue(hiddenClick.isCancelled());
        assertFalse(hiddenClick.getReturnValue());

        widget.setHiddenFancyMenu(false);
        widget.setHitboxRotationFancyMenu(0.0F, 0.0F, 0.0F);
        assertFalse(clickHook(widget, 60, 30, InputConstants.MOUSE_BUTTON_LEFT).isCancelled());
    }

    @Test
    void sliderPointerMappingUsesTheSameWidgetHitboxAndCurrentDimensions() {
        TestWidget widget = new TestWidget();
        widget.setHitboxRotationFancyMenu(90.0F, 0.0F, 0.0F);
        assertEquals(100, widget.getUntransformedMouseX_FancyMenu(60, 70), 1.0E-9);
        widget.resize(200, 40);
        assertEquals(140, widget.getUntransformedMouseX_FancyMenu(110, 70), 1.0E-9);
        widget.setHitboxRotationFancyMenu(0.0F, 0.0F, 0.0F);
        assertEquals(110, widget.getUntransformedMouseX_FancyMenu(110, 70));
    }

    private static CallbackInfoReturnable<Boolean> clickHook(TestWidget widget, double mouseX, double mouseY, int button) throws Exception {
        // Invoke the production hook directly: a unit-test JVM does not apply Minecraft mixins.
        Method hook = MixinAbstractWidget.class.getDeclaredMethod("before_clicked_FancyMenu", MouseButtonEvent.class, boolean.class, CallbackInfoReturnable.class);
        hook.setAccessible(true);
        CallbackInfoReturnable<Boolean> result = new CallbackInfoReturnable<>("mouseClicked", true);
        hook.invoke(widget, new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false, result);
        return result;
    }

    private static final class TestWidget extends MixinAbstractWidget {

        private boolean focused;

        private TestWidget() {
            this.active = true;
            this.visible = true;
            this.width = 100;
            this.height = 20;
        }

        private void resize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }

        @Override
        public boolean isHoveredOrFocused() {
            return this.isHovered || this.focused;
        }

        @Override
        public int getX() {
            return 10;
        }

        @Override
        public int getY() {
            return 20;
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

    }

}
