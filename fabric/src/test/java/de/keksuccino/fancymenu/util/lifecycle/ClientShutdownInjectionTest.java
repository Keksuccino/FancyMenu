package de.keksuccino.fancymenu.util.lifecycle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class ClientShutdownInjectionTest {

    private static final String MIXIN_CLASS_RESOURCE = "/de/keksuccino/fancymenu/mixin/mixins/common/client/MixinMinecraft.class";
    private static final String SHUTDOWN_HANDLER_OWNER = "de/keksuccino/fancymenu/util/lifecycle/ClientShutdownHandler";
    private static final String SHUTDOWN_HANDLER_DESCRIPTOR = "()V";

    @Test
    void cleanupRunsAtCloseHeadOnly() throws IOException {
        MethodNode shutdownHook = findShutdownHook();
        AnnotationNode inject = findAnnotation(shutdownHook, Type.getDescriptor(Inject.class));
        List<String> targets = annotationValue(inject, "method");
        List<AnnotationNode> injectionPoints = annotationValue(inject, "at");

        assertEquals(List.of("close"), targets);
        assertEquals(1, injectionPoints.size());
        assertEquals("HEAD", annotationValue(injectionPoints.get(0), "value"));
        assertEquals(Type.getDescriptor(At.class), injectionPoints.get(0).desc);
    }

    private static MethodNode findShutdownHook() throws IOException {
        try (InputStream input = ClientShutdownInjectionTest.class.getResourceAsStream(MIXIN_CLASS_RESOURCE)) {
            assertNotNull(input, "Compiled MixinMinecraft class was not available on the test classpath");
            ClassNode mixinClass = new ClassNode();
            new ClassReader(input).accept(mixinClass, 0);
            List<MethodNode> shutdownHooks = new ArrayList<>();
            for (MethodNode method : mixinClass.methods) {
                if (invokesShutdownHandler(method)) shutdownHooks.add(method);
            }
            assertEquals(1, shutdownHooks.size(), "Client shutdown cleanup must have exactly one Minecraft lifecycle hook");
            return shutdownHooks.get(0);
        }
    }

    private static boolean invokesShutdownHandler(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode invocation && invocation.owner.equals(SHUTDOWN_HANDLER_OWNER) && invocation.name.equals("shutdown") && invocation.desc.equals(SHUTDOWN_HANDLER_DESCRIPTOR)) {
                return true;
            }
        }
        return false;
    }

    private static AnnotationNode findAnnotation(MethodNode method, String descriptor) {
        List<AnnotationNode> annotations = method.visibleAnnotations;
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (annotation.desc.equals(descriptor)) return annotation;
            }
        }
        return fail("Shutdown hook is missing its @Inject annotation");
    }

    @SuppressWarnings("unchecked")
    private static <T> T annotationValue(AnnotationNode annotation, String name) {
        if (annotation.values != null) {
            for (int i = 0; i < annotation.values.size(); i += 2) {
                if (annotation.values.get(i).equals(name)) return (T)annotation.values.get(i + 1);
            }
        }
        return fail("Annotation " + annotation.desc + " is missing value " + name);
    }

}
