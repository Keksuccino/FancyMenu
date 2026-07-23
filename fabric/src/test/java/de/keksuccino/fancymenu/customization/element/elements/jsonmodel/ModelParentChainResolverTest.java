package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelParentChainResolverTest {

    @Test
    void rejectsNegativeParentDepth() {
        assertThrows(IllegalArgumentException.class, () -> new ModelParentChainResolver<String, TestModel>(-1));
    }

    @Test
    void resolvesModelsInChildToAncestorOrderAndStopsBeforeBuiltinParents() {
        TestModel root = new TestModel("root", "test:first");
        TestModel first = new TestModel("first", "test:second");
        TestModel second = new TestModel("second", "minecraft:builtin/entity");
        Map<String, TestModel> models = Map.of("test:first", first, "test:second", second);
        List<String> resolvedIdentifiers = new ArrayList<>();

        ModelParentChainResolver.Resolution<String, TestModel> resolution = new ModelParentChainResolver<String, TestModel>().resolve(root, TestModel::parent, identifier -> {
            resolvedIdentifiers.add(identifier);
            return models.get(identifier);
        }, identifier -> identifier.startsWith("minecraft:builtin/"));

        assertAll(() -> assertEquals(ModelParentChainResolver.Status.COMPLETE, resolution.status()), () -> assertTrue(resolution.isUsable()), () -> assertEquals(List.of(root, first, second), resolution.chain()), () -> assertEquals(List.of("test:first", "test:second"), resolvedIdentifiers));
    }

    @Test
    void addsGeneratedItemModelAndCountsItAsAParent() {
        TestModel root = new TestModel("root", "minecraft:item/generated");
        TestModel generated = new TestModel("generated", null);
        AtomicInteger loads = new AtomicInteger();

        ModelParentChainResolver.Resolution<String, TestModel> accepted = new ModelParentChainResolver<String, TestModel>(1).resolve(root, TestModel::parent, identifier -> {
            loads.incrementAndGet();
            return generated;
        }, identifier -> identifier.startsWith("minecraft:builtin/"));
        assertAll(() -> assertEquals(ModelParentChainResolver.Status.COMPLETE, accepted.status()), () -> assertEquals(List.of(root, generated), accepted.chain()), () -> assertEquals(1, loads.get()));

        loads.set(0);
        ModelParentChainResolver.Resolution<String, TestModel> rejected = new ModelParentChainResolver<String, TestModel>(0).resolve(root, TestModel::parent, identifier -> {
            loads.incrementAndGet();
            return generated;
        }, identifier -> identifier.startsWith("minecraft:builtin/"));
        assertAll(() -> assertEquals(ModelParentChainResolver.Status.DEPTH_LIMIT, rejected.status()), () -> assertEquals("minecraft:item/generated", rejected.nextParent()), () -> assertEquals(0, loads.get()));
    }

    @Test
    void reportsSelfCycleWithClosedPathAndBoundedResolution() {
        TestModel root = new TestModel("root", "test:a");
        TestModel cyclicParent = new TestModel("a", "test:a");
        AtomicInteger loads = new AtomicInteger();

        ModelParentChainResolver.Resolution<String, TestModel> resolution = new ModelParentChainResolver<String, TestModel>().resolve(root, TestModel::parent, identifier -> {
            loads.incrementAndGet();
            return cyclicParent;
        }, identifier -> false);

        assertAll(() -> assertEquals(ModelParentChainResolver.Status.CYCLE, resolution.status()), () -> assertFalse(resolution.isUsable()), () -> assertEquals(List.of("test:a", "test:a"), resolution.diagnosticPath()), () -> assertEquals(List.of(root, cyclicParent), resolution.chain()), () -> assertEquals(1, loads.get()));
    }

    @Test
    void reportsMultiModelCycleWithClosedPathAndBoundedResolution() {
        TestModel root = new TestModel("root", "test:a");
        TestModel parentA = new TestModel("a", "test:b");
        TestModel parentB = new TestModel("b", "test:a");
        Map<String, TestModel> models = Map.of("test:a", parentA, "test:b", parentB);
        AtomicInteger loads = new AtomicInteger();

        ModelParentChainResolver.Resolution<String, TestModel> resolution = new ModelParentChainResolver<String, TestModel>().resolve(root, TestModel::parent, identifier -> {
            loads.incrementAndGet();
            return models.get(identifier);
        }, identifier -> false);

        assertAll(() -> assertEquals(ModelParentChainResolver.Status.CYCLE, resolution.status()), () -> assertEquals(List.of("test:a", "test:b", "test:a"), resolution.diagnosticPath()), () -> assertEquals(List.of(root, parentA, parentB), resolution.chain()), () -> assertEquals(2, loads.get()));
    }

    @Test
    void prefersCycleDiagnosticAtTheDepthBoundary() {
        TestModel root = new TestModel("root", "test:a");
        Map<String, TestModel> models = Map.of("test:a", new TestModel("a", "test:b"), "test:b", new TestModel("b", "test:a"));

        ModelParentChainResolver.Resolution<String, TestModel> resolution = new ModelParentChainResolver<String, TestModel>(2).resolve(root, TestModel::parent, models::get, identifier -> false);

        assertAll(() -> assertEquals(ModelParentChainResolver.Status.CYCLE, resolution.status()), () -> assertEquals(List.of("test:a", "test:b", "test:a"), resolution.diagnosticPath()));
    }

    @Test
    void acceptsExactlyTheDefaultDepthLimit() {
        ChainFixture fixture = chainWithParents(ModelParentChainResolver.DEFAULT_MAX_PARENT_DEPTH, false);
        AtomicInteger loads = new AtomicInteger();

        ModelParentChainResolver.Resolution<String, TestModel> resolution = new ModelParentChainResolver<String, TestModel>().resolve(fixture.root(), TestModel::parent, identifier -> {
            loads.incrementAndGet();
            return fixture.models().get(identifier);
        }, identifier -> false);

        assertAll(() -> assertEquals(ModelParentChainResolver.Status.COMPLETE, resolution.status()), () -> assertEquals(ModelParentChainResolver.DEFAULT_MAX_PARENT_DEPTH + 1, resolution.chain().size()), () -> assertEquals(ModelParentChainResolver.DEFAULT_MAX_PARENT_DEPTH, loads.get()));
    }

    @Test
    void rejectsTheFirstParentBeyondTheLimitBeforeResolvingIt() {
        ChainFixture fixture = chainWithParents(ModelParentChainResolver.DEFAULT_MAX_PARENT_DEPTH, true);
        AtomicInteger loads = new AtomicInteger();

        ModelParentChainResolver.Resolution<String, TestModel> resolution = new ModelParentChainResolver<String, TestModel>().resolve(fixture.root(), TestModel::parent, identifier -> {
            loads.incrementAndGet();
            return fixture.models().get(identifier);
        }, identifier -> false);

        assertAll(() -> assertEquals(ModelParentChainResolver.Status.DEPTH_LIMIT, resolution.status()), () -> assertFalse(resolution.isUsable()), () -> assertEquals(ModelParentChainResolver.DEFAULT_MAX_PARENT_DEPTH + 1, resolution.chain().size()), () -> assertEquals(ModelParentChainResolver.DEFAULT_MAX_PARENT_DEPTH, resolution.diagnosticPath().size()), () -> assertEquals("test:parent_65", resolution.nextParent()), () -> assertEquals(ModelParentChainResolver.DEFAULT_MAX_PARENT_DEPTH, loads.get()));
    }

    @Test
    void preservesPartialChainWhenMissingOrMalformedParentCannotResolve() {
        for (String failure : List.of("missing", "malformed")) {
            TestModel root = new TestModel("root", "test:" + failure);
            ModelParentChainResolver.Resolution<String, TestModel> resolution = new ModelParentChainResolver<String, TestModel>().resolve(root, TestModel::parent, identifier -> null, identifier -> false);
            assertAll(() -> assertEquals(ModelParentChainResolver.Status.MISSING_PARENT, resolution.status()), () -> assertTrue(resolution.isUsable()), () -> assertEquals(List.of(root), resolution.chain()), () -> assertEquals(List.of("test:" + failure), resolution.diagnosticPath()), () -> assertEquals("test:" + failure, resolution.nextParent()));
        }
    }

    private static ChainFixture chainWithParents(int acceptedParentCount, boolean addOverflowParent) {
        Map<String, TestModel> models = new HashMap<>();
        int totalParents = acceptedParentCount + (addOverflowParent ? 1 : 0);
        for (int index = 1; index <= totalParents; index++) {
            String parent = index < totalParents ? "test:parent_" + (index + 1) : null;
            models.put("test:parent_" + index, new TestModel("parent_" + index, parent));
        }
        return new ChainFixture(new TestModel("root", "test:parent_1"), models);
    }

    private record TestModel(String name, String parent) {
    }

    private record ChainFixture(TestModel root, Map<String, TestModel> models) {
    }

}
