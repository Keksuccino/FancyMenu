package de.keksuccino.fancymenu.customization.action.ui;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.CommentExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.FolderExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.DelayExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ExecuteLaterExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ForEachExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import de.keksuccino.fancymenu.testing.TestLocalizations;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionScriptDescriptionsTest {

    private static final String FOLDER_KEY = "fancymenu.actions.blocks.folder.display";
    private static final String COMMENT_KEY = "fancymenu.actions.blocks.comment.display";
    private static final String IF_KEY = "fancymenu.actions.blocks.if";
    private static final String ELSE_IF_KEY = "fancymenu.actions.blocks.else_if";
    private static final String ELSE_KEY = "fancymenu.actions.blocks.else";
    private static final String WHILE_KEY = "fancymenu.actions.blocks.while";
    private static final String FOR_EACH_KEY = "fancymenu.actions.blocks.for_each";
    private static final String DELAY_KEY = "fancymenu.actions.blocks.delay";
    private static final String EXECUTE_LATER_KEY = "fancymenu.actions.blocks.execute_later";
    private static final String VALUE_KEY = "fancymenu.actions.screens.manage_screen.info.value";
    private static final String NONE_KEY = "fancymenu.actions.screens.manage_screen.info.value.none";

    private static final UITheme THEME = new UITheme("test_theme", "Test Theme");
    private final TestAction valueAction = new TestAction(true);
    private final TestAction noValueAction = new TestAction(false);

    @Test
    void everyKnownBlockTypeGetsItsOwnLabel() {
        ElseIfExecutableBlock elseIf = new ElseIfExecutableBlock();
        elseIf.condition = conditionWithGroup("else_if_group");
        Map<AbstractExecutableBlock, String> blocks = Map.of(new IfExecutableBlock(conditionWithGroup("if_group")), IF_KEY, elseIf, ELSE_IF_KEY, new ElseExecutableBlock(), ELSE_KEY,
                new WhileExecutableBlock(conditionWithGroup("while_group")), WHILE_KEY, new ForEachExecutableBlock("my_list"), FOR_EACH_KEY, new DelayExecutableBlock("250"), DELAY_KEY,
                new ExecuteLaterExecutableBlock("500"), EXECUTE_LATER_KEY, new FolderExecutableBlock(), FOLDER_KEY, new CommentExecutableBlock(), COMMENT_KEY);
        for (Map.Entry<AbstractExecutableBlock, String> block : blocks.entrySet()) {
            MutableComponent label = ActionScriptDescriptions.buildBlockLabel(block.getKey(), ActionScriptDescriptions.NO_TEXT_LIMIT);
            assertNotNull(label, block.getValue());
            assertEquals(block.getValue(), assertInstanceOf(TranslatableContents.class, label.getContents()).getKey());
        }
        assertNull(ActionScriptDescriptions.buildBlockLabel(new GenericExecutableBlock(), ActionScriptDescriptions.NO_TEXT_LIMIT));
    }

    @Test
    void foldersAndCommentsGetDescribedWithTheirTextsAndNeverAsUnknown() {
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(comment("Plays the intro once"));
        FolderExecutableBlock folder = new FolderExecutableBlock();
        folder.setName("Intro");
        folder.addExecutable(new ActionInstance(this.valueAction, "folder_value"));
        script.addExecutable(folder);

        List<Component> lines = overview(script);

        assertEquals(4, lines.size(), "comment, folder, the folder's action and its value");
        assertEquals("Plays the intro once", argText(lines.get(0), COMMENT_KEY));
        assertEquals("Intro", argText(lines.get(1), FOLDER_KEY));
        assertEquals(0, levelOf(lines.get(1)));
        assertEquals(1, levelOf(lines.get(2)), "the folder's content is one level deeper than the folder");
        assertTrue(keysOf(lines.get(3)).contains(VALUE_KEY));
        assertTrue(indentOf(lines.get(3)) > indentOf(lines.get(2)), "the value line belongs to its action");
        lines.forEach(line -> assertFalse(keysOf(line).contains(ActionScriptDescriptions.UNKNOWN_KEY)));
    }

    @Test
    void blockContentsAreNestedAndElseStatementsStayOnTheLevelOfTheirIf() {
        GenericExecutableBlock script = new GenericExecutableBlock();
        IfExecutableBlock ifBlock = new IfExecutableBlock(conditionWithGroup("if_group"));
        ifBlock.addExecutable(new ActionInstance(this.noValueAction, null));
        ElseIfExecutableBlock elseIf = new ElseIfExecutableBlock();
        elseIf.condition = conditionWithGroup("else_if_group");
        elseIf.addExecutable(new ActionInstance(this.noValueAction, null));
        ElseExecutableBlock elseBlock = new ElseExecutableBlock();
        elseBlock.addExecutable(new ActionInstance(this.noValueAction, null));
        elseIf.setAppendedBlock(elseBlock);
        ifBlock.setAppendedBlock(elseIf);
        script.addExecutable(ifBlock);
        WhileExecutableBlock whileBlock = new WhileExecutableBlock(conditionWithGroup("while_group"));
        ForEachExecutableBlock forEach = new ForEachExecutableBlock("my_list");
        DelayExecutableBlock delay = new DelayExecutableBlock("250");
        delay.addExecutable(new ExecuteLaterExecutableBlock("500").addExecutable(new ActionInstance(this.noValueAction, null)));
        forEach.addExecutable(delay);
        whileBlock.addExecutable(forEach);
        script.addExecutable(whileBlock);

        List<Component> lines = overview(script);

        List<String> expectedKeys = List.of(IF_KEY, TestAction.NAME_KEY, ELSE_IF_KEY, TestAction.NAME_KEY, ELSE_KEY, TestAction.NAME_KEY, WHILE_KEY, FOR_EACH_KEY, DELAY_KEY, EXECUTE_LATER_KEY, TestAction.NAME_KEY);
        List<Integer> expectedLevels = List.of(0, 1, 0, 1, 0, 1, 0, 1, 2, 3, 4);
        assertEquals(expectedKeys.size(), lines.size(), "actions without a value get no value line");
        for (int i = 0; i < lines.size(); i++) {
            assertEquals(expectedKeys.get(i), firstKeyOf(lines.get(i)), "line " + i);
            assertEquals(expectedLevels.get(i), levelOf(lines.get(i)), "level of line " + i);
        }
        assertEquals("if_group", argText(lines.get(0), IF_KEY));
        assertEquals("my_list", argText(lines.get(7), FOR_EACH_KEY));
        assertEquals("250", argText(lines.get(8), DELAY_KEY));
        assertEquals("500", argText(lines.get(9), EXECUTE_LATER_KEY));
    }

    @Test
    void collapsedBlocksHideTheirContentAndCountTheActionsInside() {
        GenericExecutableBlock script = new GenericExecutableBlock();
        FolderExecutableBlock folder = new FolderExecutableBlock();
        folder.addExecutable(new ActionInstance(this.valueAction, "a"));
        folder.addExecutable(comment("not an action"));
        FolderExecutableBlock nested = new FolderExecutableBlock();
        nested.addExecutable(new ActionInstance(this.valueAction, "b"));
        nested.addExecutable(new ActionInstance(this.valueAction, "c"));
        folder.addExecutable(nested);
        folder.setCollapsed(true);
        script.addExecutable(folder);
        IfExecutableBlock ifBlock = new IfExecutableBlock(conditionWithGroup("if_group"));
        ElseExecutableBlock elseBlock = new ElseExecutableBlock();
        elseBlock.addExecutable(new ActionInstance(this.noValueAction, null));
        ifBlock.setAppendedBlock(elseBlock);
        ifBlock.setCollapsed(true);
        script.addExecutable(ifBlock);
        script.addExecutable(new ActionInstance(this.noValueAction, null));

        List<Component> lines = overview(script);

        assertEquals(3, lines.size(), "a collapsed IF hides the rest of its chain too, like in the editor");
        assertEquals(List.of(3), argsOf(lines.get(0), ActionScriptDescriptions.COLLAPSED_OTHER_KEY));
        assertTrue(keysOf(lines.get(1)).contains(ActionScriptDescriptions.COLLAPSED_ONE_KEY), "the action of the hidden ELSE counts");
        assertEquals(TestAction.NAME_KEY, firstKeyOf(lines.get(2)));
        assertEquals(0, levelOf(lines.get(2)));
    }

    @Test
    void longTextsGetShortenedToOneLine() {
        GenericExecutableBlock script = new GenericExecutableBlock();
        String longComment = "This comment is way too long for the narrow description area\nand it even has a second line";
        script.addExecutable(comment(longComment));
        script.addExecutable(new ActionInstance(this.valueAction, "{\n    \"first\": \"value\",\n    \"second\": \"another value that goes on and on\"\n}"));

        List<Component> lines = overview(script);

        String shortComment = argText(lines.get(0), COMMENT_KEY);
        assertTrue(shortComment.codePointCount(0, shortComment.length()) <= ActionScriptDescriptions.OVERVIEW_TEXT_LIMIT);
        assertTrue(longComment.startsWith(shortComment.substring(0, shortComment.length() - 1)));
        String value = valueTextOf(lines.get(2));
        assertFalse(value.contains("\n"));
        assertTrue(value.codePointCount(0, value.length()) <= ActionScriptDescriptions.OVERVIEW_TEXT_LIMIT);
    }

    @Test
    void valueSummariesGetShownInsteadOfTheValueAndShortenedLikeIt() {
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(new ActionInstance(new TestAction(true, "short summary"), "raw|||value"));
        script.addExecutable(new ActionInstance(new TestAction(true, "a summary that is way too long for the description area"), "raw|||value"));

        List<Component> lines = overview(script);

        assertEquals("short summary", valueTextOf(lines.get(1)));
        String longSummary = valueTextOf(lines.get(3));
        assertTrue(longSummary.codePointCount(0, longSummary.length()) <= ActionScriptDescriptions.OVERVIEW_TEXT_LIMIT);
        assertTrue(longSummary.startsWith("a summary"));
    }

    @Test
    void shortenKeepsShortTextsAndNeverSplitsCharacters() {
        assertAll(() -> assertEquals("short text", ActionScriptDescriptions.shorten("short text", 40)),
                () -> assertEquals("first second", ActionScriptDescriptions.shorten("  first\n   second  ", 40)),
                () -> assertEquals("line\nbreak", ActionScriptDescriptions.shorten("line\nbreak", ActionScriptDescriptions.NO_TEXT_LIMIT)),
                () -> assertEquals(10, ActionScriptDescriptions.shorten("a".repeat(30), 10).length()));
        String emojis = "😀".repeat(20);
        String shortEmojis = ActionScriptDescriptions.shorten(emojis, 10);
        assertEquals(10, shortEmojis.codePointCount(0, shortEmojis.length()));
        shortEmojis.codePoints().limit(9).forEach(codePoint -> assertEquals(0x1F600, codePoint));
    }

    @Test
    void entriesOfUnknownTypesGetTheLocalizedFallbackAndShowTheirContents() {
        GenericExecutableBlock script = new GenericExecutableBlock();
        GenericExecutableBlock unknown = new GenericExecutableBlock();
        unknown.addExecutable(new ActionInstance(this.noValueAction, null));
        script.addExecutable(unknown);

        List<Component> lines = overview(script);

        assertEquals(2, lines.size());
        assertEquals(ActionScriptDescriptions.UNKNOWN_KEY, firstKeyOf(lines.get(0)));
        assertEquals(1, levelOf(lines.get(1)));
    }

    @Test
    void emptyScriptsAndEmptyConditionsSayNone() {
        List<Component> lines = overview(new GenericExecutableBlock());
        assertEquals(1, lines.size());
        assertEquals(NONE_KEY, firstKeyOf(lines.get(0)));
        IfExecutableBlock ifBlock = new IfExecutableBlock(new RequirementContainer());
        assertEquals(NONE_KEY, keyOf(argsOf(ActionScriptDescriptions.buildBlockLabel(ifBlock, ActionScriptDescriptions.NO_TEXT_LIMIT), IF_KEY).get(0)));
    }

    @Test
    void newTextsAreLocalizedInEveryLanguage() throws IOException, URISyntaxException {
        TestLocalizations.assertLocalizedEverywhere(List.of(ActionScriptDescriptions.UNKNOWN_KEY, ActionScriptDescriptions.COLLAPSED_ONE_KEY, ActionScriptDescriptions.COLLAPSED_OTHER_KEY));
        TestLocalizations.readAllLanguages().forEach((language, translations) -> assertTrue(translations.get(ActionScriptDescriptions.COLLAPSED_OTHER_KEY).getAsString().contains("%s"), language));
    }

    @NotNull
    private static List<Component> overview(@NotNull GenericExecutableBlock script) {
        return ActionScriptDescriptions.buildScriptOverview(script, THEME, false);
    }

    @NotNull
    private static RequirementContainer conditionWithGroup(@NotNull String groupIdentifier) {
        RequirementContainer container = new RequirementContainer();
        assertNotNull(container.createAndAddGroup(groupIdentifier, RequirementGroup.GroupMode.AND));
        return container;
    }

    @NotNull
    private static CommentExecutableBlock comment(@NotNull String text) {
        CommentExecutableBlock comment = new CommentExecutableBlock();
        comment.setComment(text);
        return comment;
    }

    /**
     * The indentation of a line, from the text in front of its label, which is plain text.
     */
    private static int indentOf(@NotNull Component line) {
        String prefix = line.plainCopy().getString();
        return prefix.length() - prefix.stripLeading().length();
    }

    private static int levelOf(@NotNull Component line) {
        return indentOf(line) / ActionScriptDescriptions.INDENT.length();
    }

    @Nullable
    private static String keyOf(@NotNull Object component) {
        return (component instanceof Component c) && (c.getContents() instanceof TranslatableContents contents) ? contents.getKey() : null;
    }

    /**
     * The key of the label of a line, which can be wrapped in an empty component when the line has a suffix.
     */
    @Nullable
    private static String firstKeyOf(@NotNull Component line) {
        List<String> keys = keysOf(line);
        return keys.isEmpty() ? null : keys.get(0);
    }

    @NotNull
    private static List<String> keysOf(@NotNull Component component) {
        List<String> keys = new ArrayList<>();
        collectKeys(component, keys);
        return keys;
    }

    private static void collectKeys(@NotNull Component component, @NotNull List<String> keys) {
        if (component.getContents() instanceof TranslatableContents contents) {
            keys.add(contents.getKey());
            for (Object arg : contents.getArgs()) {
                if (arg instanceof Component c) collectKeys(c, keys);
            }
        }
        component.getSiblings().forEach(sibling -> collectKeys(sibling, keys));
    }

    @NotNull
    private static List<Object> argsOf(@NotNull Component component, @NotNull String key) {
        if ((component.getContents() instanceof TranslatableContents contents) && contents.getKey().equals(key)) return List.of(contents.getArgs());
        for (Component sibling : component.getSiblings()) {
            List<Object> args = argsOf(sibling, key);
            if (!args.isEmpty()) return args;
        }
        return List.of();
    }

    /**
     * The text users typed in that the label with the key shows, which is plain text.
     */
    @NotNull
    private static String argText(@NotNull Component line, @NotNull String key) {
        List<Object> args = argsOf(line, key);
        assertEquals(1, args.size(), key);
        return assertInstanceOf(Component.class, args.get(0)).getString();
    }

    /**
     * The value that a value line shows after its label, which is plain text for actions without a value summary.
     */
    @NotNull
    private static String valueTextOf(@NotNull Component line) {
        for (Component part : line.getSiblings()) {
            if (VALUE_KEY.equals(keyOf(part))) {
                List<Component> valueParts = part.getSiblings();
                return valueParts.get(valueParts.size() - 1).getString();
            }
        }
        throw new AssertionError("No value line");
    }

    private static final class TestAction extends Action {

        private static final String NAME_KEY = "test.action_script_descriptions.action";

        private final boolean hasValue;
        @Nullable
        private final String valueSummary;

        private TestAction(boolean hasValue) {
            this(hasValue, null);
        }

        private TestAction(boolean hasValue, @Nullable String valueSummary) {
            super("test_action_script_descriptions_" + hasValue);
            this.hasValue = hasValue;
            this.valueSummary = valueSummary;
        }

        @Override
        public @Nullable Component getValueSummary(@NotNull String value) {
            return (this.valueSummary != null) ? Component.literal(this.valueSummary) : null;
        }

        @Override
        public boolean hasValue() {
            return this.hasValue;
        }

        @Override
        public void execute(@Nullable String value) {
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable(NAME_KEY);
        }

        @Override
        public Component getDescription() {
            return Component.empty();
        }

        @Override
        public Component getValueDisplayName() {
            return Component.empty();
        }

        @Override
        public String getValuePreset() {
            return "";
        }

    }

}
