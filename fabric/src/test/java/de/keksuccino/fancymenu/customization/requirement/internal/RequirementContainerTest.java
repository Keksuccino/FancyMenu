package de.keksuccino.fancymenu.customization.requirement.internal;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementContainerTest {

    @Test
    void cachingIdentifierStaysAvailableToSubclassesOfAddOns() throws NoSuchFieldException {
        Field field = RequirementContainer.class.getDeclaredField("cachingIdentifier");
        int modifiers = field.getModifiers();
        assertEquals(String.class, field.getType());
        assertTrue(Modifier.isProtected(modifiers));
        assertTrue(Modifier.isFinal(modifiers));
        assertFalse(Modifier.isStatic(modifiers));
        assertTrue(field.isAnnotationPresent(Deprecated.class));

        AddOnContainer first = new AddOnContainer();
        AddOnContainer second = new AddOnContainer();
        assertNotNull(first.readCachingIdentifier());
        assertFalse(first.readCachingIdentifier().isBlank());
        assertSame(first.readCachingIdentifier(), first.readCachingIdentifier());
        assertNotEquals(first.readCachingIdentifier(), second.readCachingIdentifier());
    }

    /** A container subclass like add-ons have them, which reads the protected caching identifier. */
    private static final class AddOnContainer extends RequirementContainer {

        @SuppressWarnings("deprecation")
        private String readCachingIdentifier() {
            return this.cachingIdentifier;
        }

    }

}
