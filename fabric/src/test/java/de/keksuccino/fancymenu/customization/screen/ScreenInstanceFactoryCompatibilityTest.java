package de.keksuccino.fancymenu.customization.screen;

import net.minecraft.client.gui.screens.Screen;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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
    void explicitPostDisconnectContextRetainsWorldState() {
        ScreenConstructionContext context = new ScreenConstructionContext(null, false);

        assertNull(context.parentScreen());
        assertFalse(context.inWorld());
    }

}
