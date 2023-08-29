package de.keksuccino.fancymenu.util.rendering.ui.widget.editbox;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinCommandSuggestions;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class EditBoxSuggestions extends CommandSuggestions {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
    protected static final Style UNPARSED_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);
    protected static final Style LITERAL_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY);
    protected static final List<Style> ARGUMENT_STYLES = Stream.of(ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD).map(Style.EMPTY::withColor).collect(ImmutableList.toImmutableList());

    protected final Minecraft minecraft;
    protected final Screen screen;
    protected final EditBox input;
    protected final Font font;
    protected final boolean commandsOnly;
    protected final boolean onlyShowIfCursorPastError;
    protected final int lineStartOffset;
    protected final int suggestionLineLimit;
    protected final boolean anchorToBottom;
    protected final int fillColor;
    protected final List<String> customSuggestionsList = new ArrayList<>();
    protected boolean onlyCustomSuggestions = false;
    protected boolean allowRenderUsage = true;
    @NotNull
    protected SuggestionsRenderPosition renderPosition = SuggestionsRenderPosition.VANILLA;

    public EditBoxSuggestions(Minecraft mc, Screen parentScreen, EditBox targetEditBox, Font font, boolean commandsOnly, boolean onlyShowIfCursorPastError, int lineStartOffset, int suggestionLineLimit, boolean anchorToBottom, int fillColor) {
        super(mc, parentScreen, targetEditBox, font, commandsOnly, onlyShowIfCursorPastError, lineStartOffset, suggestionLineLimit, anchorToBottom, fillColor);
        this.minecraft = mc;
        this.screen = parentScreen;
        this.input = targetEditBox;
        this.font = font;
        this.commandsOnly = commandsOnly;
        this.onlyShowIfCursorPastError = onlyShowIfCursorPastError;
        this.lineStartOffset = lineStartOffset;
        this.suggestionLineLimit = suggestionLineLimit;
        this.anchorToBottom = anchorToBottom;
        this.fillColor = fillColor;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY) {
        if (!this.input.isFocused()) {
            this.setSuggestions(null);
        }
        super.render(pose, mouseX, mouseY);
    }

    @Override
    public void renderUsage(@NotNull PoseStack pose) {
        if (!this.isAllowRenderUsage()) return;
        super.renderUsage(pose);
    }

    @Override
    public void updateCommandInfo() {

        String editBoxValue = this.input.getValue();

        if ((this.getCurrentParse() != null) && !this.getCurrentParse().getReader().getString().equals(editBoxValue)) {
            this.setCurrentParse(null);
        }

        if (!this.isKeepSuggestions()) {
            this.input.setSuggestion(null);
            this.setSuggestions(null);
        }

        this.getCommandUsage().clear();
        StringReader valueReader = new StringReader(editBoxValue);
        boolean isReaderCursorAtSlash = valueReader.canRead() && valueReader.peek() == '/';
        if (isReaderCursorAtSlash) {
            valueReader.skip();
        }

        boolean treatAsCommand = this.commandsOnly || isReaderCursorAtSlash;
        if (this.onlyCustomSuggestions) treatAsCommand = false;
        int editBoxCursorPos = this.input.getCursorPosition();
        if (treatAsCommand) {
            if (this.minecraft.player != null) {
                CommandDispatcher<SharedSuggestionProvider> commands = this.minecraft.player.connection.getCommands();
                if (this.getCurrentParse() == null) {
                    this.setCurrentParse(commands.parse(valueReader, this.minecraft.player.connection.getSuggestionsProvider()));
                }
                int readerCursorPos = this.onlyShowIfCursorPastError ? valueReader.getCursor() : 1;
                if ((editBoxCursorPos >= readerCursorPos) && ((this.getSuggestions() == null) || !this.isKeepSuggestions())) {
                    this.setPendingSuggestions(commands.getCompletionSuggestions(this.getCurrentParse(), editBoxCursorPos));
                    this.getPendingSuggestions().thenRun(() -> {
                        if (this.getPendingSuggestions().isDone()) {
                            this.updateUsageInfo();
                        }
                    });
                }
            }
        } else {
            String editBoxSubValue = editBoxValue.substring(0, editBoxCursorPos);
            int lastWordIndex = getLastWordIndex(editBoxSubValue);
            Collection<String> suggestionStringList = new ArrayList<>(this.customSuggestionsList);
            if (suggestionStringList.isEmpty() && (this.minecraft.player != null)) {
                suggestionStringList = this.minecraft.player.connection.getSuggestionsProvider().getCustomTabSugggestions();
            }
            this.setPendingSuggestions(SharedSuggestionProvider.suggest(suggestionStringList, new SuggestionsBuilder(editBoxSubValue, lastWordIndex)));
        }

    }

    @Override
    public void showSuggestions(boolean someNarratingRelatedBoolean) {
        if ((this.getPendingSuggestions() != null) && this.getPendingSuggestions().isDone()) {

            Suggestions suggestions = this.getPendingSuggestions().join();
            if (!suggestions.isEmpty()) {

                List<Suggestion> sortedSuggestions = this.sortSuggestions(suggestions);

                int totalSuggestionsWidth = 0;
                for(Suggestion suggestion : suggestions.getList()) {
                    totalSuggestionsWidth = Math.max(totalSuggestionsWidth, this.font.width(suggestion.getText()));
                }

                int listX = Mth.clamp(this.input.getScreenX(suggestions.getRange().getStart()), 0, this.input.getScreenX(0) + this.input.getInnerWidth() - totalSuggestionsWidth);
                int listY = this.anchorToBottom ? this.screen.height - 12 : 72;
                int listHeight = Math.min(sortedSuggestions.size(), this.suggestionLineLimit) * 12;
                if (this.renderPosition == SuggestionsRenderPosition.ABOVE_EDIT_BOX) {
                    listY = this.input.getY() - listHeight - 2;
                }
                if (this.renderPosition == SuggestionsRenderPosition.BELOW_EDIT_BOX) {
                    listY = this.input.getY() + this.input.getHeight() + 2;
                }
                this.setSuggestions(new SuggestionsList(listX, listY, totalSuggestionsWidth, sortedSuggestions, someNarratingRelatedBoolean));

            }

        }
    }

    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        if (!this.input.isFocused()) return false;
        if ((this.getSuggestions() != null) && this.getSuggestions().keyPressed(keycode, scancode, modifiers)) {
            return true;
        } else if (keycode == 258) {
            this.showSuggestions(true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseClicked(double $$0, double $$1, int $$2) {
        if (!this.input.isFocused()) return false;
        return super.mouseClicked($$0, $$1, $$2);
    }

    @Override
    public boolean mouseScrolled(double $$0) {
        if (!this.input.isFocused()) return false;
        return super.mouseScrolled($$0);
    }

    protected void updateUsageInfo() {
        this.getAccessor().invokeUpdateUsageInfoFancyMenu();
    }

    protected List<Suggestion> sortSuggestions(Suggestions suggestions) {
        return this.getAccessor().invokeSortSuggestionsFancyMenu(suggestions);
    }

    public void setSuggestionsRenderPosition(@NotNull SuggestionsRenderPosition position) {
        this.renderPosition = Objects.requireNonNull(position);
    }

    @NotNull
    public SuggestionsRenderPosition getSuggestionsRenderPosition() {
        return this.renderPosition;
    }

    public void setAllowRenderUsage(boolean allow) {
        this.allowRenderUsage = allow;
    }

    public boolean isAllowRenderUsage() {
        return this.allowRenderUsage;
    }

    public void enableOnlyCustomSuggestionsMode(boolean enable) {
        this.onlyCustomSuggestions = enable;
    }

    public boolean isOnlyCustomSuggestionsMode() {
        return this.onlyCustomSuggestions;
    }

    public void setCustomSuggestions(@Nullable List<String> customSuggestions) {
        if (this.commandsOnly) throw new RuntimeException("Can't set custom suggestions in commands-only mode!");
        this.customSuggestionsList.clear();
        if (customSuggestions != null) {
            this.customSuggestionsList.addAll(customSuggestions);
        }
    }

    public CommandSuggestions.SuggestionsList getSuggestions() {
        return this.getAccessor().getSuggestionsFancyMenu();
    }

    public void setSuggestions(CommandSuggestions.SuggestionsList suggestions) {
        this.getAccessor().setSuggestionsFancyMenu(suggestions);
    }

    public CompletableFuture<Suggestions> getPendingSuggestions() {
        return this.getAccessor().getPendingSuggestionsFancyMenu();
    }

    public void setPendingSuggestions(CompletableFuture<Suggestions> pendingSuggestions) {
        this.getAccessor().setPendingSuggestionsFancyMenu(pendingSuggestions);
    }

    public ParseResults<SharedSuggestionProvider> getCurrentParse() {
        return this.getAccessor().getCurrentParseFancyMenu();
    }

    public void setCurrentParse(ParseResults<SharedSuggestionProvider> currentParse) {
        this.getAccessor().setCurrentParseFancyMenu(currentParse);
    }

    public boolean isKeepSuggestions() {
        return this.getAccessor().getKeepSuggestionsFancyMenu();
    }

    public IMixinCommandSuggestions getAccessor() {
        return ((IMixinCommandSuggestions)this);
    }

    public List<FormattedCharSequence> getCommandUsage() {
        return this.getAccessor().getCommandUsageFancyMenu();
    }

    @SuppressWarnings("all")
    protected static int getLastWordIndex(String editBoxValue) {
        if (Strings.isNullOrEmpty(editBoxValue)) {
            return 0;
        } else {
            int index = 0;
            for(Matcher matcher = WHITESPACE_PATTERN.matcher(editBoxValue); matcher.find(); index = matcher.end()) {}
            return index;
        }
    }

    public enum SuggestionsRenderPosition {
        VANILLA,
        ABOVE_EDIT_BOX,
        BELOW_EDIT_BOX
    }

}
