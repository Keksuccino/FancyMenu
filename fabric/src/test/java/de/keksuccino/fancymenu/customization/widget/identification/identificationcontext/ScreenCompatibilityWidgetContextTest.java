package de.keksuccino.fancymenu.customization.widget.identification.identificationcontext;

import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.contexts.TitleScreenWidgetIdentificationContext;
import eu.kennytv.forcecloseloadingscreen.TitleBridgeScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScreenCompatibilityWidgetContextTest {

    private Map<Class<? extends Screen>, WidgetIdentificationContext> contexts;
    private Map<Class<? extends Screen>, WidgetIdentificationContext> previousContexts;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preserveContexts() throws ReflectiveOperationException {
        Field contextsField = WidgetIdentificationContextRegistry.class.getDeclaredField("CONTEXTS");
        contextsField.setAccessible(true);
        this.contexts = (Map<Class<? extends Screen>, WidgetIdentificationContext>) contextsField.get(null);
        this.previousContexts = new LinkedHashMap<>(this.contexts);
        this.contexts.clear();
    }

    @AfterEach
    void restoreContexts() {
        this.contexts.clear();
        this.contexts.putAll(this.previousContexts);
    }

    @Test
    void bridgeUsesVanillaTitleWidgetIdentificationContext() {
        WidgetIdentificationContext context = new TitleScreenWidgetIdentificationContext();
        WidgetIdentificationContextRegistry.register(context);

        assertSame(context, WidgetIdentificationContextRegistry.getContextForScreen(TitleScreen.class));
        assertSame(context, WidgetIdentificationContextRegistry.getContextForScreen(TitleBridgeScreen.class));
        assertNull(WidgetIdentificationContextRegistry.getContextForScreen(UnrelatedTitleScreen.class));
    }

    private static class UnrelatedTitleScreen extends TitleScreen {
    }

}
