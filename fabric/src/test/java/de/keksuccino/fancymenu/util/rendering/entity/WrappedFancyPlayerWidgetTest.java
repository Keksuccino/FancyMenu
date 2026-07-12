package de.keksuccino.fancymenu.util.rendering.entity;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrappedFancyPlayerWidgetTest {

    private static final String WRAPPER_CLASS_RESOURCE = "de/keksuccino/fancymenu/util/rendering/entity/WrappedFancyPlayerWidget.class";
    private static final String PLAYER_ENTITY_ELEMENT_CLASS = "de.keksuccino.fancymenu.customization.element.elements.playerentity.PlayerEntityElement";
    private static final String FER_WIDGET_CLASS = "it.crystalnest.fancy_entity_renderer.api.entity.player.FancyPlayerWidget";
    private static final String FER_WIDGET_OWNER = "it/crystalnest/fancy_entity_renderer/api/entity/player/FancyPlayerWidget";
    private static final String FER_WIDGET_DESCRIPTOR = "L" + FER_WIDGET_OWNER + ";";

    @Test
    void delegatesRemappedVanillaMethodsThroughTypedCalls() throws IOException {
        ClassNode wrapperClass = readWrapperClass();

        FieldNode wrappedField = wrapperClass.fields.stream().filter(field -> field.name.equals("wrapped")).findFirst().orElse(null);
        assertNotNull(wrappedField, "The FER delegate field must remain available");
        assertTrue(wrappedField.desc.equals(FER_WIDGET_DESCRIPTOR), "The FER delegate must stay typed so loader remappers can rewrite inherited Minecraft method calls");

        assertDirectFerCall(wrapperClass, "renderWidget", "render");
        assertDirectFerCall(wrapperClass, "updateWidgetNarration", "updateNarration");
        assertDirectFerCall(wrapperClass, "setX", "setX");
        assertDirectFerCall(wrapperClass, "setY", "setY");
        assertDirectFerCall(wrapperClass, "setWidth", "setWidth");
    }

    @Test
    void keepsPlayerEntityElementLoadableWithoutOptionalFer() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        assertThrows(ClassNotFoundException.class, () -> Class.forName(FER_WIDGET_CLASS, false, classLoader), "The test runtime must model an installation without the compile-only FER dependency");
        assertDoesNotThrow(() -> Class.forName(PLAYER_ENTITY_ELEMENT_CLASS, false, classLoader), "Loading FancyMenu's element registry class must not resolve the optional FER widget");
    }

    private static ClassNode readWrapperClass() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(WRAPPER_CLASS_RESOURCE)) {
            assertNotNull(input, "Compiled wrapper class was not found on the test classpath");
            ClassNode classNode = new ClassNode();
            new ClassReader(input).accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return classNode;
        }
    }

    private static void assertDirectFerCall(ClassNode wrapperClass, String wrapperMethodName, String delegateMethodName) {
        MethodNode wrapperMethod = wrapperClass.methods.stream().filter(method -> method.name.equals(wrapperMethodName)).findFirst().orElse(null);
        assertNotNull(wrapperMethod, "Wrapper method not found: " + wrapperMethodName);
        boolean hasDirectCall = false;
        for (AbstractInsnNode instruction : wrapperMethod.instructions) {
            if (instruction instanceof MethodInsnNode methodCall && methodCall.owner.equals(FER_WIDGET_OWNER) && methodCall.name.equals(delegateMethodName)) {
                hasDirectCall = true;
                break;
            }
        }
        assertTrue(hasDirectCall, wrapperMethodName + " must call FancyPlayerWidget." + delegateMethodName + " directly");
    }
}
