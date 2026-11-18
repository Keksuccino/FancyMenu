package de.keksuccino.fancymenu.customization.aiexport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the Markdown text of an AI export.<br>
 * Bullets get indented by two spaces per {@link #indent()} level. Raw values get written as inline code, or as fenced code blocks
 * when they span multiple lines, with fences that are longer than any run of backticks in the value, so values never break the Markdown around them.
 */
public class AiExportWriter {

    private static final String INDENT_UNIT = "  ";
    private static final String EMPTY_VALUE = "(empty)";
    private static final String NO_VALUE = "(none)";

    private final StringBuilder builder = new StringBuilder(4096);
    private int indentLevel = 0;

    /**
     * Writes a heading and resets the indentation, because headings always start a new top-level section.
     */
    @NotNull
    public AiExportWriter heading(int level, @NotNull String text) {
        this.indentLevel = 0;
        this.ensureBlankLine();
        this.builder.append("#".repeat(Math.max(1, Math.min(6, level)))).append(' ').append(toSingleLine(text)).append('\n').append('\n');
        return this;
    }

    @NotNull
    public AiExportWriter paragraph(@NotNull String text) {
        String stripped = text.strip();
        if (stripped.isEmpty()) return this;
        this.ensureBlankLine();
        this.builder.append(stripped).append('\n').append('\n');
        return this;
    }

    /**
     * Writes a bullet with plain text. Lines after the first one get indented, so they stay part of the bullet.
     */
    @NotNull
    public AiExportWriter bullet(@NotNull String text) {
        String indent = this.currentIndent();
        String[] lines = normalizeLineBreaks(text).split("\n", -1);
        this.builder.append(indent).append("- ").append(lines[0]).append('\n');
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) continue;
            this.builder.append(indent).append(INDENT_UNIT).append(lines[i]).append('\n');
        }
        return this;
    }

    /**
     * Writes a bullet with a label and a raw value. Multi-line values get written as code block below the label.
     */
    @NotNull
    public AiExportWriter field(@NotNull String label, @Nullable Object value) {
        if (value == null) {
            return this.bullet("**" + label + ":** " + NO_VALUE);
        }
        String s = normalizeLineBreaks(String.valueOf(value));
        if (s.indexOf('\n') < 0) {
            return this.bullet("**" + label + ":** " + inlineCode(s));
        }
        this.bullet("**" + label + ":**");
        this.indent();
        this.codeBlock(null, s);
        this.outdent();
        return this;
    }

    /**
     * Writes a bullet with a label and a raw value, followed by a plain text note, like {@code - **Value:** `abc` (note)}.
     */
    @NotNull
    public AiExportWriter field(@NotNull String label, @Nullable Object value, @Nullable String note) {
        if ((note == null) || note.isBlank() || (value == null) || (String.valueOf(value).indexOf('\n') >= 0)) {
            this.field(label, value);
            if ((note != null) && !note.isBlank() && (value != null) && (String.valueOf(value).indexOf('\n') >= 0)) {
                this.indent();
                this.bullet(note);
                this.outdent();
            }
            return this;
        }
        return this.bullet("**" + label + ":** " + inlineCode(normalizeLineBreaks(String.valueOf(value))) + " (" + note + ")");
    }

    /**
     * Writes a bullet with a label and plain text, for text that is no raw value, like descriptions.
     */
    @NotNull
    public AiExportWriter fieldText(@NotNull String label, @Nullable String text) {
        if ((text == null) || text.isBlank()) {
            return this.bullet("**" + label + ":** " + NO_VALUE);
        }
        return this.bullet("**" + label + ":** " + text.strip());
    }

    @NotNull
    public AiExportWriter codeBlock(@Nullable String language, @NotNull String content) {
        String normalized = normalizeLineBreaks(content);
        String fence = "`".repeat(Math.max(3, longestBacktickRun(normalized) + 1));
        String indent = this.currentIndent();
        boolean topLevel = (this.indentLevel == 0);
        if (topLevel) this.ensureBlankLine();
        this.builder.append(indent).append(fence);
        if (language != null) this.builder.append(language);
        this.builder.append('\n');
        for (String line : normalized.split("\n", -1)) {
            if (line.isEmpty()) {
                this.builder.append('\n');
            } else {
                this.builder.append(indent).append(line).append('\n');
            }
        }
        this.builder.append(indent).append(fence).append('\n');
        if (topLevel) this.builder.append('\n');
        return this;
    }

    @NotNull
    public AiExportWriter indent() {
        this.indentLevel++;
        return this;
    }

    @NotNull
    public AiExportWriter outdent() {
        if (this.indentLevel > 0) this.indentLevel--;
        return this;
    }

    /**
     * Runs the task one indentation level deeper.
     */
    @NotNull
    public AiExportWriter nested(@NotNull Runnable task) {
        this.indent();
        try {
            task.run();
        } finally {
            this.outdent();
        }
        return this;
    }

    public int getIndentLevel() {
        return this.indentLevel;
    }

    /**
     * @return The finished Markdown text, with paths in the home folder of the user hidden by {@link AiExportRedactor}.
     */
    @NotNull
    public String build() {
        return AiExportRedactor.redact(this.builder.toString().stripTrailing() + "\n");
    }

    @NotNull
    public static String inlineCode(@NotNull String value) {
        if (value.isEmpty()) return EMPTY_VALUE;
        int longestRun = longestBacktickRun(value);
        String fence = "`".repeat(longestRun + 1);
        // Markdown needs spaces between the fence and a value that starts or ends with a backtick
        boolean pad = (longestRun > 0);
        return fence + (pad ? " " : "") + value + (pad ? " " : "") + fence;
    }

    @NotNull
    private String currentIndent() {
        return INDENT_UNIT.repeat(this.indentLevel);
    }

    private void ensureBlankLine() {
        int length = this.builder.length();
        if (length == 0) return;
        if (this.builder.charAt(length - 1) != '\n') {
            this.builder.append('\n').append('\n');
        } else if ((length < 2) || (this.builder.charAt(length - 2) != '\n')) {
            this.builder.append('\n');
        }
    }

    private static int longestBacktickRun(@NotNull String s) {
        int longest = 0;
        int current = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '`') {
                current++;
                if (current > longest) longest = current;
            } else {
                current = 0;
            }
        }
        return longest;
    }

    @NotNull
    private static String normalizeLineBreaks(@NotNull String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }

    @NotNull
    private static String toSingleLine(@NotNull String s) {
        return normalizeLineBreaks(s).replace('\n', ' ').strip();
    }

}
