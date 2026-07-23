package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Iteratively resolves a single-parent model chain with deterministic cycle and depth failures.
 * The root model is depth zero, so {@code maxParentDepth} is the number of parent models that may be added.
 * The default of 64 leaves generous room for normal, shallow model inheritance while strictly bounding synchronous
 * resource opens performed on the render thread.
 */
final class ModelParentChainResolver<K, M> {

    static final int DEFAULT_MAX_PARENT_DEPTH = 64;

    private final int maxParentDepth;

    ModelParentChainResolver() {
        this(DEFAULT_MAX_PARENT_DEPTH);
    }

    ModelParentChainResolver(int maxParentDepth) {
        if (maxParentDepth < 0) throw new IllegalArgumentException("maxParentDepth must not be negative");
        this.maxParentDepth = maxParentDepth;
    }

    @NotNull
    Resolution<K, M> resolve(@NotNull M root, @NotNull Function<M, K> parentIdentifierGetter, @NotNull Function<K, M> parentResolver, @NotNull Predicate<K> terminalParentPredicate) {
        Objects.requireNonNull(root);
        Objects.requireNonNull(parentIdentifierGetter);
        Objects.requireNonNull(parentResolver);
        Objects.requireNonNull(terminalParentPredicate);

        List<M> chain = new ArrayList<>(this.maxParentDepth + 1);
        List<K> parentPath = new ArrayList<>(this.maxParentDepth);
        Map<K, Integer> firstParentIndices = new HashMap<>();
        chain.add(root);

        M current = root;
        int parentDepth = 0;
        while (true) {
            K parentIdentifier = parentIdentifierGetter.apply(current);
            if (parentIdentifier == null || terminalParentPredicate.test(parentIdentifier)) return Resolution.complete(chain);

            Integer cycleStart = firstParentIndices.get(parentIdentifier);
            if (cycleStart != null) {
                List<K> cyclePath = new ArrayList<>(parentPath.subList(cycleStart, parentPath.size()));
                cyclePath.add(parentIdentifier);
                return Resolution.cycle(chain, cyclePath, parentIdentifier);
            }

            // Check the repeated identifier first so a cycle at the boundary receives the more useful diagnostic.
            if (parentDepth >= this.maxParentDepth) return Resolution.depthLimit(chain, parentPath, parentIdentifier);

            firstParentIndices.put(parentIdentifier, parentPath.size());
            parentPath.add(parentIdentifier);
            M parentModel = parentResolver.apply(parentIdentifier);
            if (parentModel == null) return Resolution.missingParent(chain, parentPath, parentIdentifier);

            chain.add(parentModel);
            current = parentModel;
            parentDepth++;
        }
    }

    enum Status {
        COMPLETE,
        MISSING_PARENT,
        CYCLE,
        DEPTH_LIMIT
    }

    record Resolution<K, M>(@NotNull List<M> chain, @NotNull Status status, @NotNull List<K> diagnosticPath, @Nullable K nextParent) {

        Resolution {
            chain = List.copyOf(Objects.requireNonNull(chain));
            status = Objects.requireNonNull(status);
            diagnosticPath = List.copyOf(Objects.requireNonNull(diagnosticPath));
        }

        private static <K, M> Resolution<K, M> complete(List<M> chain) {
            return new Resolution<>(chain, Status.COMPLETE, List.of(), null);
        }

        private static <K, M> Resolution<K, M> missingParent(List<M> chain, List<K> parentPath, K missingParent) {
            return new Resolution<>(chain, Status.MISSING_PARENT, parentPath, missingParent);
        }

        private static <K, M> Resolution<K, M> cycle(List<M> chain, List<K> cyclePath, K repeatedParent) {
            return new Resolution<>(chain, Status.CYCLE, cyclePath, repeatedParent);
        }

        private static <K, M> Resolution<K, M> depthLimit(List<M> chain, List<K> parentPath, K nextParent) {
            return new Resolution<>(chain, Status.DEPTH_LIMIT, parentPath, nextParent);
        }

        boolean isUsable() {
            return this.status == Status.COMPLETE || this.status == Status.MISSING_PARENT;
        }

    }

}
