package de.keksuccino.fancymenu.customization.action.ui;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.CommentExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.FolderExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.DelayExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ExecuteLaterExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ForEachExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds the texts that describe the actions and blocks of action scripts: the labels of blocks, which the {@link ActionScriptEditorWindowBody}
 * shows, and a compact overview of a whole script, which the description areas of the Manage Listeners and Manage Schedulers windows show.
 */
public final class ActionScriptDescriptions {

    /** Makes the labels show the texts users typed in (names, comments, list names and delays) in full. */
    public static final int NO_TEXT_LIMIT = -1;
    /**
     * How many characters the overview shows of texts that can get long, like comments, action values and conditions.<br>
     * The description areas are narrow and their wrapped lines start at the left edge, so longer texts get shortened to keep the nesting readable.
     */
    static final int OVERVIEW_TEXT_LIMIT = 32;
    static final String UNKNOWN_KEY = "fancymenu.actions.blocks.unknown";
    static final String COLLAPSED_ONE_KEY = "fancymenu.actions.blocks.overview.collapsed.one";
    static final String COLLAPSED_OTHER_KEY = "fancymenu.actions.blocks.overview.collapsed.other";
    private static final String NONE_KEY = "fancymenu.actions.screens.manage_screen.info.value.none";
    private static final String VALUE_KEY = "fancymenu.actions.screens.manage_screen.info.value";
    /** Spaces of the UI font are narrow, so every level needs a few of them to make the nesting visible. */
    static final String INDENT = "    ";
    private static final String BULLET = "• ";
    /** Value lines sit deeper than the contents of a block would, so they read as part of their action. */
    private static final String VALUE_BULLET = "      ◦ ";
    private static final Pattern LINE_BREAK = Pattern.compile("\\s*\\R\\s*");

    private ActionScriptDescriptions() {
    }

    /**
     * Builds the label of a block, like {@code IF (Is Variable Value)} or {@code Folder: Setup}, without its collapse state.
     *
     * @param maxTextLength How many characters to show of the texts users typed in, or {@link #NO_TEXT_LIMIT}.
     * @return The label, or {@code null} if the type of the block is unknown.
     */
    @Nullable
    public static MutableComponent buildBlockLabel(@NotNull AbstractExecutableBlock block, int maxTextLength) {
        if (block instanceof IfExecutableBlock ifBlock) return Component.translatable("fancymenu.actions.blocks.if", buildConditionLabel(ifBlock.condition, maxTextLength));
        if (block instanceof ElseIfExecutableBlock elseIfBlock) return Component.translatable("fancymenu.actions.blocks.else_if", buildConditionLabel(elseIfBlock.condition, maxTextLength));
        if (block instanceof ElseExecutableBlock) return Component.translatable("fancymenu.actions.blocks.else");
        if (block instanceof WhileExecutableBlock whileBlock) return Component.translatable("fancymenu.actions.blocks.while", buildConditionLabel(whileBlock.condition, maxTextLength));
        if (block instanceof ForEachExecutableBlock forEachBlock) return Component.translatable("fancymenu.actions.blocks.for_each", buildUserText(forEachBlock.getListVariableName(), maxTextLength));
        if (block instanceof DelayExecutableBlock delayBlock) return Component.translatable("fancymenu.actions.blocks.delay", buildUserText(delayBlock.getDelayMsRaw(), maxTextLength));
        if (block instanceof ExecuteLaterExecutableBlock executeLaterBlock) return Component.translatable("fancymenu.actions.blocks.execute_later", buildUserText(executeLaterBlock.getDelayMsRaw(), maxTextLength));
        if (block instanceof FolderExecutableBlock folder) return Component.translatable("fancymenu.actions.blocks.folder.display", buildUserText(folder.getName(), maxTextLength));
        if (block instanceof CommentExecutableBlock comment) return Component.translatable("fancymenu.actions.blocks.comment.display", buildUserText(comment.getComment(), maxTextLength));
        return null;
    }

    /**
     * The label of entries whose type is unknown, like blocks of a type that {@link #buildBlockLabel(AbstractExecutableBlock, int)} doesn't know.
     */
    @NotNull
    public static MutableComponent buildUnknownLabel() {
        return Component.translatable(UNKNOWN_KEY);
    }

    /**
     * Lists the requirement groups and requirements of a condition, separated by commas.
     *
     * @param maxTextLength How many characters to show, or {@link #NO_TEXT_LIMIT}.
     */
    @NotNull
    public static MutableComponent buildConditionLabel(@NotNull RequirementContainer condition, int maxTextLength) {
        MutableComponent label = Component.empty();
        boolean empty = true;
        for (RequirementGroup group : condition.getGroups()) {
            if (!empty) label.append(", ");
            label.append(group.identifier);
            empty = false;
        }
        for (RequirementInstance instance : condition.getInstances()) {
            if (!empty) label.append(", ");
            label.append(instance.requirement.getDisplayName());
            empty = false;
        }
        if (empty) return Component.translatable(NONE_KEY);
        if (maxTextLength < 0) return label;
        return Component.literal(shorten(label.getString(), maxTextLength));
    }

    /**
     * Builds a compact overview of a whole script for description areas: one line per action or block, with the contents of a block
     * one level deeper than the block and the ELSE-IF and ELSE statements of an IF on the same level as the IF.<br>
     * To keep long scripts readable, actions without a value get no value line, texts that can get long get shortened to one short line
     * (see {@link #OVERVIEW_TEXT_LIMIT}), and blocks that are collapsed in the action script editor only tell how many actions they hide.
     *
     * @param blur If the overview gets shown in a window with a blurred background, which uses other label colors.
     */
    @NotNull
    public static List<Component> buildScriptOverview(@NotNull AbstractExecutableBlock script, @NotNull UITheme theme, boolean blur) {
        OverviewColors colors = OverviewColors.of(theme, blur);
        List<Component> lines = new ArrayList<>();
        for (Executable executable : script.getExecutables()) {
            addOverviewLines(lines, executable, 0, colors);
        }
        if (lines.isEmpty()) {
            lines.add(buildLine(0, BULLET, colors.detailBullet(), Component.translatable(NONE_KEY).setStyle(colors.text())));
        }
        return lines;
    }

    private static void addOverviewLines(@NotNull List<Component> lines, @NotNull Executable executable, int level, @NotNull OverviewColors colors) {
        if (executable instanceof ActionInstance instance) {
            lines.add(buildLine(level, BULLET, colors.actionBullet(), instance.action.getDisplayName().copy().setStyle(colors.text())));
            if (instance.action.hasValue()) {
                lines.add(buildLine(level, VALUE_BULLET, colors.detailBullet(), Component.translatable(VALUE_KEY).append(" ").append(buildValueText(instance)).setStyle(colors.text())));
            }
        } else if (executable instanceof AbstractExecutableBlock block) {
            boolean comment = (block instanceof CommentExecutableBlock);
            MutableComponent blockLabel = buildBlockLabel(block, OVERVIEW_TEXT_LIMIT);
            MutableComponent label = (blockLabel != null) ? blockLabel.setStyle(comment ? colors.note() : colors.text()) : buildUnknownLabel().setStyle(colors.error());
            boolean collapsed = ActionScriptEntryMover.isCollapsed(block);
            if (collapsed) {
                int actions = countActions(block);
                MutableComponent suffix = (actions == 1) ? Component.translatable(COLLAPSED_ONE_KEY) : Component.translatable(COLLAPSED_OTHER_KEY, actions);
                label = Component.empty().append(label).append(Component.literal(" ").append(suffix).setStyle(colors.collapsed()));
            }
            lines.add(buildLine(level, BULLET, comment ? colors.detailBullet() : colors.blockBullet(), label));
            // Collapsing an IF also hides the rest of its chain in the editor, see ActionScriptEntryMover#isCollapsed
            if (collapsed) return;
            for (Executable child : block.getExecutables()) {
                addOverviewLines(lines, child, level + 1, colors);
            }
            AbstractExecutableBlock appended = block.getAppendedBlock();
            if (appended != null) {
                addOverviewLines(lines, appended, level, colors);
            }
        } else {
            lines.add(buildLine(level, BULLET, colors.blockBullet(), buildUnknownLabel().setStyle(colors.error())));
        }
    }

    /**
     * The summary of the action's value, like the action script editor shows it (see {@link de.keksuccino.fancymenu.customization.action.Action#getValueSummary(String)}),
     * or the value itself, shortened like all long texts of the overview.
     */
    @NotNull
    private static Component buildValueText(@NotNull ActionInstance instance) {
        String value = instance.value;
        if (value == null) return Component.translatable(NONE_KEY);
        Component summary = instance.action.getValueSummary(value);
        return buildUserText((summary != null) ? summary.getString() : value, OVERVIEW_TEXT_LIMIT);
    }

    /**
     * Counts the actions inside the block, also the ones in nested blocks and in the rest of the block's IF chain.
     */
    static int countActions(@NotNull AbstractExecutableBlock block) {
        int count = 0;
        for (Executable executable : block.getExecutables()) {
            if (executable instanceof ActionInstance) {
                count++;
            } else if (executable instanceof AbstractExecutableBlock child) {
                count += countActions(child);
            }
        }
        AbstractExecutableBlock appended = block.getAppendedBlock();
        if (appended != null) {
            count += countActions(appended);
        }
        return count;
    }

    @NotNull
    private static MutableComponent buildUserText(@NotNull String text, int maxTextLength) {
        if (text.isBlank()) return Component.translatable(NONE_KEY);
        return Component.literal(shorten(text, maxTextLength));
    }

    /**
     * Puts the text on one line and shortens it to at most the given number of characters, ending with "…" if it got shortened.
     *
     * @param maxLength The number of characters, or {@link #NO_TEXT_LIMIT} to keep the text as it is.
     */
    @NotNull
    static String shorten(@NotNull String text, int maxLength) {
        if (maxLength < 0) return text;
        String line = LINE_BREAK.matcher(text).replaceAll(" ").strip();
        if (line.codePointCount(0, line.length()) <= maxLength) return line;
        // Cut at code points, so characters made of two chars, like many emojis, don't get split
        String start = line.substring(0, line.offsetByCodePoints(0, Math.max(0, maxLength - 1)));
        return start.stripTrailing() + "…";
    }

    @NotNull
    private static MutableComponent buildLine(int level, @NotNull String bullet, int bulletColor, @NotNull Component text) {
        return Component.literal(INDENT.repeat(Math.max(0, level)) + bullet).setStyle(Style.EMPTY.withColor(bulletColor)).append(text);
    }

    private record OverviewColors(@NotNull Style text, @NotNull Style note, @NotNull Style collapsed, @NotNull Style error, int actionBullet, int blockBullet, int detailBullet) {

        @NotNull
        private static OverviewColors of(@NotNull UITheme theme, boolean blur) {
            int text = (blur ? theme.ui_blur_interface_widget_label_color_normal : theme.ui_interface_widget_label_color_normal).getColorInt();
            int note = (blur ? theme.ui_blur_interface_widget_label_color_inactive : theme.ui_interface_widget_label_color_inactive).getColorInt();
            return new OverviewColors(Style.EMPTY.withColor(text), Style.EMPTY.withColor(note), Style.EMPTY.withColor(theme.warning_color.getColorInt()), Style.EMPTY.withColor(theme.error_color.getColorInt()),
                    theme.bullet_list_dot_color_2.getColorInt(), theme.warning_color.getColorInt(), theme.bullet_list_dot_color_1.getColorInt());
        }

    }

}
