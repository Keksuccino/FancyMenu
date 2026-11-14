package de.keksuccino.fancymenu.customization.element.container;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerTestElements.Child;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerTestElements.Container;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementContainerHierarchyTest {

    private static List<String> ids(List<? extends AbstractElement> elements) {
        List<String> ids = new ArrayList<>();
        for (AbstractElement element : elements) ids.add(element.getInstanceIdentifier());
        return ids;
    }

    private static Child childOf(String identifier, String containerIdentifier) {
        Child child = new Child(identifier);
        child.setParentContainerIdentifier(containerIdentifier);
        return child;
    }

    @Test
    void listsWithoutChildrenStayAsTheyAre() {
        List<AbstractElement> elements = new ArrayList<>(List.of(new Child("a"), new Container("area"), new Child("b")));
        List<AbstractElement> before = new ArrayList<>(elements);
        ElementContainerHierarchy.sortChildrenBehindContainers(elements, Function.identity());
        assertEquals(before, elements);
    }

    @Test
    void childrenMoveBehindTheirContainerInTheirOwnOrder() {
        List<AbstractElement> elements = new ArrayList<>(List.of(
                childOf("c1", "area"), new Child("a"), new Container("area"), new Child("b"), childOf("c2", "area"), childOf("c3", "area")));
        ElementContainerHierarchy.sortChildrenBehindContainers(elements, Function.identity());
        assertEquals(List.of("a", "area", "c1", "c2", "c3", "b"), ids(elements));
    }

    @Test
    void childrenOfChildrenFollowTheirOwnContainer() {
        Container inner = new Container("inner");
        inner.setParentContainerIdentifier("outer");
        List<AbstractElement> elements = new ArrayList<>(List.of(
                childOf("deep", "inner"), new Child("a"), childOf("flat", "outer"), inner, new Container("outer"), childOf("last", "outer")));
        ElementContainerHierarchy.sortChildrenBehindContainers(elements, Function.identity());
        assertEquals(List.of("a", "outer", "flat", "inner", "deep", "last"), ids(elements));
    }

    @Test
    void childrenOfMissingContainersStayWhereTheyAre() {
        List<AbstractElement> elements = new ArrayList<>(List.of(childOf("lost", "gone"), new Container("area"), childOf("c1", "area"), new Child("a")));
        ElementContainerHierarchy.sortChildrenBehindContainers(elements, Function.identity());
        assertEquals(List.of("lost", "area", "c1", "a"), ids(elements));
    }

    @Test
    void childrenOfSomethingThatIsNoContainerStayWhereTheyAre() {
        List<AbstractElement> elements = new ArrayList<>(List.of(childOf("c1", "plain"), new Child("plain")));
        ElementContainerHierarchy.sortChildrenBehindContainers(elements, Function.identity());
        assertEquals(List.of("c1", "plain"), ids(elements));
    }

    @Test
    void containersThatHoldEachOtherStayInTheList() {
        Container first = new Container("first");
        first.setParentContainerIdentifier("second");
        Container second = new Container("second");
        second.setParentContainerIdentifier("first");
        List<AbstractElement> elements = new ArrayList<>(List.of(first, new Child("a"), second, childOf("c", "first")));
        ElementContainerHierarchy.sortChildrenBehindContainers(elements, Function.identity());
        assertEquals(4, elements.size());
        assertTrue(elements.containsAll(List.of(first, second)));
        assertEquals("a", elements.get(0).getInstanceIdentifier());
    }

    @Test
    void listsOfWrappersGetSortedByTheirElements() {
        record Wrapper(AbstractElement element) {
        }
        List<Wrapper> wrappers = new ArrayList<>(List.of(new Wrapper(childOf("c1", "area")), new Wrapper(new Container("area"))));
        ElementContainerHierarchy.sortChildrenBehindContainers(wrappers, Wrapper::element);
        assertEquals("area", wrappers.get(0).element().getInstanceIdentifier());
        assertEquals("c1", wrappers.get(1).element().getInstanceIdentifier());
    }

    @Test
    void connectingFindsContainersInTheList() {
        Container area = new Container("area");
        Child child = childOf("c1", "area");
        Child lost = childOf("lost", "gone");
        Child plain = new Child("plain");
        ElementContainerHierarchy.connect(List.of(child, area, lost, plain));
        assertSame(area, child.getParentContainer());
        assertNull(plain.getParentContainer());
        assertNull(plain.getContainerLink());
    }

    @Test
    void containersGetTheirDirectChildrenInListOrder() {
        Container outer = new Container("outer");
        Container inner = new Container("inner");
        inner.setParentContainer(outer);
        Child first = new Child("first");
        first.setParentContainer(outer);
        Child deep = new Child("deep");
        deep.setParentContainer(inner);
        Child last = new Child("last");
        last.setParentContainer(outer);

        ElementContainerHierarchy.assignChildren(List.of(outer, first, inner, deep, last, new Child("plain")));
        assertEquals(List.of("first", "inner", "last"), ids(outer.children));
        assertEquals(List.of("deep"), ids(inner.children));

        // Children a container had before are gone when the list does not have them anymore
        ElementContainerHierarchy.assignChildren(List.of(outer, inner));
        assertEquals(List.of("inner"), ids(outer.children));
        assertTrue(inner.children.isEmpty());
    }

    @Test
    void elementsKnowIfTheyAreSomewhereInAContainer() {
        Container outer = new Container("outer");
        Container inner = new Container("inner");
        inner.setParentContainer(outer);
        Child deep = new Child("deep");
        deep.setParentContainer(inner);
        Child plain = new Child("plain");

        assertTrue(ElementContainerHierarchy.isInContainer(deep, inner));
        assertTrue(ElementContainerHierarchy.isInContainer(deep, outer));
        assertTrue(ElementContainerHierarchy.isInContainer(inner, outer));
        assertFalse(ElementContainerHierarchy.isInContainer(outer, inner));
        assertFalse(ElementContainerHierarchy.isInContainer(plain, outer));

        assertSame(outer, ElementContainerHierarchy.getOutermostContainer(deep));
        assertSame(outer, ElementContainerHierarchy.getOutermostContainer(inner));
        assertNull(ElementContainerHierarchy.getOutermostContainer(outer));
    }

    @Test
    void containersCanNotHoldThemselves() {
        Container outer = new Container("outer");
        Container inner = new Container("inner");
        inner.setParentContainer(outer);

        // A container as its own child, directly or through its children, could never work out where it is
        outer.setParentContainer(inner);
        assertNull(outer.getParentContainer());
        outer.setParentContainerIdentifier("outer");
        assertNull(outer.getParentContainerIdentifier());
    }

}
