//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A text compiled for replacing its placeholders, so texts that get parsed again and again only get scanned once.
 *
 * <p>A template holds the text and the placeholders {@link PlaceholderScanner} finds in it, as nodes in text order. Each node
 * knows its {@link Placeholder}, identifier, raw text and values. Values that contain placeholders or {@code $$} references get
 * compiled into templates of their own, after the BEFORE processors ran on them, like at parse time.</p>
 *
 * <p>Only placeholders that can get replaced become nodes. Unknown placeholders, placeholders without identifier and placeholders
 * whose values can't be read always keep their raw text, so they are just part of the text between the nodes.</p>
 *
 * <p>Templates never change after compiling, so they are safe to share between threads. A cached template is only valid for the
 * processor revision its values got processed with and the registry revision its identifiers got resolved with, so placeholders
 * registered later (by add-ons, for example) don't stay unknown.</p>
 */
final class PlaceholderTemplate {

    private static final int CACHE_MAX_ENTRIES = 1024;
    private static final long CACHE_MAX_WEIGHT = 4_194_304L;
    private static final long OVERHEAD_WEIGHT = 64L;
    private static final Node[] NO_NODES = new Node[0];
    private static final ValueSlot[] NO_VALUES = new ValueSlot[0];
    private static final char QUOTE = '"';

    // Weights count the retained UTF-16 code units of all texts of a template, plus a fixed allowance for each of its objects
    private static final BoundedConcurrentCache<String, PlaceholderTemplate> CACHE = new BoundedConcurrentCache<>(CACHE_MAX_ENTRIES, CACHE_MAX_WEIGHT, (text, template) -> template.weight);

    /** The compiled text. Nodes refer to it by index. */
    @NotNull
    final String source;
    /** The placeholders that can get replaced, in text order. */
    @NotNull
    final Node[] nodes;
    final long processorRevision;
    final long registryRevision;
    private final long weight;

    private PlaceholderTemplate(@NotNull String source, @NotNull Node[] nodes, long processorRevision, long registryRevision, long weight) {
        this.source = source;
        this.nodes = nodes;
        this.processorRevision = processorRevision;
        this.registryRevision = registryRevision;
        this.weight = weight;
    }

    /**
     * Returns the cached template of the text, or compiles and caches it if there is no valid one.
     *
     * @param text The text after the BEFORE processors of the context ran on it.
     */
    @NotNull
    static PlaceholderTemplate get(@NotNull String text, @NotNull PlaceholderEvaluator.ParsingContext context) {
        long registryRevision = PlaceholderRegistry.getRevision();
        PlaceholderTemplate cached = CACHE.get(text);
        if ((cached != null) && (cached.processorRevision == context.processors().revision()) && (cached.registryRevision == registryRevision)) return cached;
        PlaceholderTemplate compiled = compile(text, context, registryRevision);
        CACHE.put(text, compiled);
        return compiled;
    }

    /**
     * Compiles the text without caching it.
     *
     * @param text The text after the BEFORE processors of the context ran on it.
     */
    @NotNull
    static PlaceholderTemplate compile(@NotNull String text, @NotNull PlaceholderEvaluator.ParsingContext context) {
        return compile(text, context, PlaceholderRegistry.getRevision());
    }

    /**
     * @param registryRevision The registry revision from before the first identifier got resolved, so a registration while compiling makes the template invalid.
     */
    @NotNull
    private static PlaceholderTemplate compile(@NotNull String text, @NotNull PlaceholderEvaluator.ParsingContext context, long registryRevision) {
        List<Node> nodes = null;
        long weight = OVERHEAD_WEIGHT + text.length();
        PlaceholderScanner scanner = new PlaceholderScanner(text);
        while (scanner.next()) {
            Node node = compileNode(text, scanner.start(), scanner.end(), scanner.identifierStart(), context, registryRevision);
            if (node == null) continue;
            if (nodes == null) nodes = new ArrayList<>();
            nodes.add(node);
            weight += node.weight;
        }
        return new PlaceholderTemplate(text, (nodes != null) ? nodes.toArray(NO_NODES) : NO_NODES, context.processors().revision(), registryRevision, weight);
    }

    /**
     * @return The node, or null if the placeholder always keeps its raw text.
     */
    @Nullable
    private static Node compileNode(@NotNull String text, int start, int end, int identifierStart, @NotNull PlaceholderEvaluator.ParsingContext context, long registryRevision) {
        // The identifier is everything between its opening quote and the next quote of the placeholder, see ParsedPlaceholder#getIdentifier()
        int identifierEnd = text.indexOf(QUOTE, identifierStart);
        if ((identifierEnd <= identifierStart) || (identifierEnd >= end)) return null;
        String identifier = text.substring(identifierStart, identifierEnd);
        Placeholder placeholder = PlaceholderRegistry.getPlaceholder(identifier);
        if (placeholder == null) return null;
        String raw = text.substring(start, end);
        HashMap<String, String> values;
        try {
            values = PlaceholderValueParser.parseValues(raw, identifierStart - start, PlaceholderRegistry.getValueNames(placeholder));
        } catch (Exception ex) {
            PlaceholderEvaluator.logError("[FANCYMENU] Failed to parse values of placeholder: " + raw, ex);
            return null;
        }
        ValueSlot[] slots = NO_VALUES;
        long weight = OVERHEAD_WEIGHT + raw.length();
        if (!values.isEmpty()) {
            slots = new ValueSlot[values.size()];
            int index = 0;
            // Placeholders get their values in the iteration order of the parsed values, like ParsedPlaceholder#getReplacement() passes them
            for (Map.Entry<String, String> value : values.entrySet()) {
                ValueSlot slot = compileValue(value.getKey(), value.getValue(), context, registryRevision);
                slots[index++] = slot;
                weight += slot.weight;
            }
        }
        return new Node(start, end, raw, identifier, placeholder, slots, weight);
    }

    @NotNull
    private static ValueSlot compileValue(@NotNull String name, @NotNull String raw, @NotNull PlaceholderEvaluator.ParsingContext context, long registryRevision) {
        String processed = context.processors().processBeforeReplacement(raw);
        PlaceholderTemplate template = PlaceholderEvaluator.passesGate(processed) ? compile(processed, context, registryRevision) : null;
        long weight = OVERHEAD_WEIGHT + raw.length() + ((processed != raw) ? processed.length() : 0L) + ((template != null) ? (template.weight - processed.length()) : 0L);
        return new ValueSlot(name, raw, processed, template, weight);
    }

    /**
     * A placeholder of a template that can get replaced.
     */
    static final class Node {

        /** The index of the opening brace in the source of the template. */
        final int start;
        /** The index after the closing brace in the source of the template. */
        final int end;
        /** The raw text of the placeholder, as it stands in the source of the template. */
        @NotNull
        final String raw;
        @NotNull
        final String identifier;
        @NotNull
        final Placeholder placeholder;
        /** The values the placeholder has in its raw text. Value names of the placeholder that are missing in its text are missing here too. */
        @NotNull
        final ValueSlot[] values;
        private final long weight;

        private Node(int start, int end, @NotNull String raw, @NotNull String identifier, @NotNull Placeholder placeholder, @NotNull ValueSlot[] values, long weight) {
            this.start = start;
            this.end = end;
            this.raw = raw;
            this.identifier = identifier;
            this.placeholder = placeholder;
            this.values = values;
            this.weight = weight;
        }

    }

    /**
     * A value of a node.
     *
     * <p>BEFORE processors still run on the raw value every time it gets parsed, because a processor can return something else
     * the next time. The compiled template only gets used while they return the same text as when it got compiled.</p>
     */
    static final class ValueSlot {

        @NotNull
        final String name;
        /** The value as it stands in the raw text of the placeholder. */
        @NotNull
        final String raw;
        /** The value after the BEFORE processors ran on it while compiling. */
        @NotNull
        final String processed;
        /** The template of the processed value, or null if it contains no placeholders or {@code $$} references, so it gets used as it is. */
        @Nullable
        final PlaceholderTemplate template;
        private final long weight;

        private ValueSlot(@NotNull String name, @NotNull String raw, @NotNull String processed, @Nullable PlaceholderTemplate template, long weight) {
            this.name = name;
            this.raw = raw;
            this.processed = processed;
            this.template = template;
            this.weight = weight;
        }

        /**
         * If the BEFORE processors returned the same text as while compiling. They usually return the same instance, which makes this a reference check.
         */
        boolean isProcessedText(@NotNull String processedNow) {
            return (processedNow == this.processed) || processedNow.equals(this.processed);
        }

    }

}
