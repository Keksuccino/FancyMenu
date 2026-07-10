package de.keksuccino.fancymenu.compat;

import de.keksuccino.fancymenu.compat.forcecloseloadingscreen.ForceCloseWorldLoadingScreenCompat;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.test.ScreenTestFactory;
import eu.kennytv.forcecloseloadingscreen.TitleBridgeScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScreenCompatibilityTest {

    private static final String OPTIONAL_SCREEN_CLASS = "eu.kennytv.forcecloseloadingscreen.TitleBridgeScreen";

    @Test
    void aliasesExactForceCloseTitleBridgeToLogicalTitleScreenWhenPresent() throws ClassNotFoundException {
        assertSame(TitleBridgeScreen.class, Class.forName(OPTIONAL_SCREEN_CLASS));
        assertSame(TitleScreen.class, ScreenCompatibility.getCompatibleScreenClass(TitleBridgeScreen.class));
        assertEquals(TitleScreen.class.getName(), ScreenCompatibility.getCompatibleScreenClassName(TitleBridgeScreen.class.getName()));
        assertEquals(TitleScreen.class.getName(), ScreenIdentifierHandler.getClassIdentifierOfScreen(ScreenTestFactory.allocateScreen(TitleBridgeScreen.class)));
        assertEquals("title_screen", ScreenIdentifierHandler.getIdentifierOfScreen(ScreenTestFactory.allocateScreen(TitleBridgeScreen.class)));
        assertEquals("title_screen", ScreenIdentifierHandler.getBestIdentifier(TitleBridgeScreen.class.getName()));
    }

    @Test
    void canonicalizesPersistedBridgeNameWithoutLoadingOptionalClass() throws ReflectiveOperationException {
        OptionalClassDenyingLoader loader = new OptionalClassDenyingLoader();
        Class<?> isolatedCompatClass = loader.loadClass(ForceCloseWorldLoadingScreenCompat.class.getName());
        Method canonicalizer = isolatedCompatClass.getDeclaredMethod("getCompatibleScreenClassName", String.class);

        assertEquals(TitleScreen.class.getName(), canonicalizer.invoke(null, OPTIONAL_SCREEN_CLASS));
    }

    @Test
    void migratesPersistedBridgeIdentifierBeforeValidityChecks() {
        assertEquals(TitleScreen.class.getName(), ScreenIdentifierHandler.tryFixInvalidIdentifierWithNonUniversal(OPTIONAL_SCREEN_CLASS));
        assertEquals("title_screen", ScreenIdentifierHandler.getBestIdentifier(OPTIONAL_SCREEN_CLASS));
    }

    @Test
    void leavesUnrelatedTitleScreenSubclassesAndNamesIndependent() {
        assertSame(UnrelatedTitleScreen.class, ScreenCompatibility.getCompatibleScreenClass(UnrelatedTitleScreen.class));
        assertEquals(UnrelatedTitleScreen.class.getName(), ScreenCompatibility.getCompatibleScreenClassName(UnrelatedTitleScreen.class.getName()));
        assertEquals(UnrelatedTitleScreen.class.getName(), ScreenIdentifierHandler.getIdentifierOfScreen(ScreenTestFactory.allocateScreen(UnrelatedTitleScreen.class)));
        assertEquals("example.forcecloseloadingscreen.TitleBridgeScreen", ScreenCompatibility.getCompatibleScreenClassName("example.forcecloseloadingscreen.TitleBridgeScreen"));
    }

    private static class UnrelatedTitleScreen extends TitleScreen {
    }

    private static final class OptionalClassDenyingLoader extends ClassLoader {

        private OptionalClassDenyingLoader() {
            super(ScreenCompatibilityTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (OPTIONAL_SCREEN_CLASS.equals(name)) throw new ClassNotFoundException(name);
            if (!ForceCloseWorldLoadingScreenCompat.class.getName().equals(name)) return super.loadClass(name, resolve);
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass == null) loadedClass = defineCompatClass(name);
            if (resolve) resolveClass(loadedClass);
            return loadedClass;
        }

        private Class<?> defineCompatClass(String name) throws ClassNotFoundException {
            String resourceName = name.replace('.', '/') + ".class";
            try (InputStream input = getParent().getResourceAsStream(resourceName)) {
                if (input == null) throw new ClassNotFoundException(name);
                byte[] bytecode = input.readAllBytes();
                return defineClass(name, bytecode, 0, bytecode.length);
            } catch (IOException ex) {
                throw new ClassNotFoundException(name, ex);
            }
        }

    }

}
