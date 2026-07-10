package de.keksuccino.fancymenu.customization.screen;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScreenInstanceFactoryCompatibilityTest {

    @Test
    void retainsLegacySupplierProviderApi() throws ReflectiveOperationException {
        Method registerMethod = ScreenInstanceFactory.class.getDeclaredMethod("registerScreenProvider", String.class, Supplier.class);
        Method getterMethod = ScreenInstanceFactory.class.getDeclaredMethod("getScreenProvider", String.class);
        Method constructionMethod = ScreenInstanceFactory.class.getDeclaredMethod("tryConstruct", String.class);

        assertSame(void.class, registerMethod.getReturnType());
        assertSame(Supplier.class, getterMethod.getReturnType());
        assertSame(Screen.class, constructionMethod.getReturnType());
    }

    @Test
    void exposesContextAwareProviderAndConstructionApi() throws ReflectiveOperationException {
        Method registerMethod = ScreenInstanceFactory.class.getDeclaredMethod("registerContextAwareScreenProvider", String.class, Function.class);
        Method getterMethod = ScreenInstanceFactory.class.getDeclaredMethod("getContextAwareScreenProvider", String.class);
        Method constructionMethod = ScreenInstanceFactory.class.getDeclaredMethod("tryConstruct", String.class, ScreenConstructionContext.class);

        assertSame(void.class, registerMethod.getReturnType());
        assertSame(Function.class, getterMethod.getReturnType());
        assertSame(Screen.class, constructionMethod.getReturnType());
    }

    @Test
    void explicitPostDisconnectContextRetainsWorldState() {
        ScreenConstructionContext context = new ScreenConstructionContext(null, false);

        assertNull(context.parentScreen());
        assertFalse(context.inWorld());
    }

    @Test
    void minecraft1201OptionsScreensUseParentAndOptionsConstructors() throws ReflectiveOperationException {
        assertSame(OptionsScreen.class, OptionsScreen.class.getConstructor(Screen.class, Options.class).getDeclaringClass());
        assertSame(AccessibilityOptionsScreen.class, AccessibilityOptionsScreen.class.getConstructor(Screen.class, Options.class).getDeclaringClass());
    }

}
