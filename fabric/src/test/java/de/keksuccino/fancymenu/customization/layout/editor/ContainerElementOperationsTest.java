package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ContainerElementOperationsTest {

    private static SerializedElement copy(String identifier, String container) {
        SerializedElement copy = new SerializedElement();
        copy.putProperty("instance_identifier", identifier);
        if (container != null) copy.putProperty("parent_container", container);
        return copy;
    }

    private static Supplier<String> numberedIdentifiers() {
        AtomicInteger count = new AtomicInteger();
        return () -> "new-" + count.incrementAndGet();
    }

    @Test
    void copiesOfChildrenBelongToTheCopyOfTheirContainer() {
        SerializedElement area = copy("area", null);
        SerializedElement child = copy("child", "area");
        SerializedElement inner = copy("inner", "area");
        SerializedElement deep = copy("deep", "inner");
        ContainerElementOperations.assignNewIdentifiers(List.of(area, child, inner, deep), numberedIdentifiers());

        assertEquals("new-1", area.getValue("instance_identifier"));
        assertNull(area.getValue("parent_container"));
        assertEquals("new-1", child.getValue("parent_container"));
        assertEquals("new-1", inner.getValue("parent_container"));
        assertEquals("new-3", inner.getValue("instance_identifier"));
        assertEquals("new-3", deep.getValue("parent_container"));
    }

    @Test
    void aChildThatGotCopiedWithoutItsContainerStaysInTheOneItHad() {
        SerializedElement child = copy("child", "area");
        SerializedElement plain = copy("plain", null);
        ContainerElementOperations.assignNewIdentifiers(List.of(child, plain), numberedIdentifiers());
        assertEquals("new-1", child.getValue("instance_identifier"));
        assertEquals("area", child.getValue("parent_container"));
        assertNull(plain.getValue("parent_container"));
    }

    @Test
    void theOrderOfTheCopiesDoesNotMatter() {
        SerializedElement child = copy("child", "area");
        SerializedElement area = copy("area", null);
        ContainerElementOperations.assignNewIdentifiers(List.of(child, area), numberedIdentifiers());
        assertEquals("new-2", area.getValue("instance_identifier"));
        assertEquals("new-2", child.getValue("parent_container"));
    }

    @Test
    void everyPasteMakesAHierarchyOfItsOwn() {
        SerializedElement area = copy("area", null);
        SerializedElement child = copy("child", "area");
        List<SerializedElement> clipboard = List.of(area, child);
        Supplier<String> identifiers = numberedIdentifiers();
        ContainerElementOperations.assignNewIdentifiers(clipboard, identifiers);
        ContainerElementOperations.assignNewIdentifiers(clipboard, identifiers);
        // The clipboard keeps the identifiers of the last paste, and the next one starts from those
        assertEquals("new-3", area.getValue("instance_identifier"));
        assertEquals("new-3", child.getValue("parent_container"));
        assertEquals("new-4", child.getValue("instance_identifier"));
    }

}
