package de.keksuccino.fancymenu.util.rendering.ui.widget.editbox;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinCommandSuggestions;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinSuggestionsList;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
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
    /** The height of one suggestion in the list, see {@link CommandSuggestions.SuggestionsList}. */
    protected static final int LIST_LINE_HEIGHT = 12;
    /** The space between the edit box and a list that shows up above or below it. */
    protected static final int LIST_SPACING = 2;
    /**
     * The space a list can take beyond its suggestions at its top and bottom: lists of bordered edit boxes start 1 pixel higher, and lists
     * with more suggestions than lines draw a line above and below them, see {@link CommandSuggestions.SuggestionsList}.
     */
    protected static final int LIST_EDGE_SPACE = 2;

    protected final Minecraft minecraft;
    protected final Screen screen;
    protected final EditBox input;
    protected final Font font;
    protected final boolean commandsOnly;
    protected final boolean onlyShowIfCursorPastError;
    protected final int lineStartOffset;
    protected final int suggestionLineLimit;
    protected final boolean anchorToBottom;
    protected final List<String> customSuggestionsList = new ArrayList<>();
    protected boolean onlyCustomSuggestions = false;
    protected boolean allowRenderUsage = true;
    @NotNull
    protected SuggestionsRenderPosition renderPosition = SuggestionsRenderPosition.VANILLA;
    protected DrawableColor backgroundColor = DrawableColor.of(new Color(0,0,0));
    protected DrawableColor normalTextColor = DrawableColor.of(new Color(-5592406));
    protected DrawableColor selectedTextColor = DrawableColor.of(new Color(-256));
    protected boolean textShadow = true;
    protected boolean autoSuggestions = true;

    @NotNull
    public static EditBoxSuggestions createWithCustomSuggestions(@NotNull Screen screen, @NotNull EditBox editBox, @NotNull SuggestionsRenderPosition renderPosition, @NotNull List<String> suggestions) {
        EditBoxSuggestions variableNameSuggestions = new EditBoxSuggestions(Minecraft.getInstance(), screen, editBox, Minecraft.getInstance().font, false, true, 0, 7, false);
        variableNameSuggestions.setAllowSuggestions(true);
        variableNameSuggestions.enableOnlyCustomSuggestionsMode(true);
        variableNameSuggestions.setSuggestionsRenderPosition(renderPosition);
        variableNameSuggestions.setAllowRenderUsage(false);
        variableNameSuggestions.setCustomSuggestions(suggestions);
        variableNameSuggestions.updateCommandInfo();
        // The list of suggestions gets placed at the edit box when it gets built. Windows create their suggestions while they build their widgets,
        // when edit boxes in cells don't have their position yet, so a list built now would show up in the wrong spot. Edit boxes without focus
        // don't show suggestions anyway, and an edit box that gets the focus later shows them as soon as the user types or presses Tab.
        if (!editBox.isFocused()) variableNameSuggestions.setSuggestions(null);
        return variableNameSuggestions;
    }

    public EditBoxSuggestions(@NotNull Minecraft mc, @NotNull Screen parentScreen, @NotNull EditBox targetEditBox, @NotNull Font font, boolean commandsOnly, boolean onlyShowIfCursorPastError, int lineStartOffset, int suggestionLineLimit, boolean anchorToBottom) {
        super(mc, parentScreen, targetEditBox, font, commandsOnly, onlyShowIfCursorPastError, lineStartOffset, suggestionLineLimit, anchorToBottom, Integer.MIN_VALUE);
        this.minecraft = mc;
        this.screen = parentScreen;
        this.input = targetEditBox;
        this.font = font;
        this.commandsOnly = commandsOnly;
        this.onlyShowIfCursorPastError = onlyShowIfCursorPastError;
        this.lineStartOffset = lineStartOffset;
        this.suggestionLineLimit = suggestionLineLimit;
        this.anchorToBottom = anchorToBottom;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.input.isFocused()) {
            this.setSuggestions(null);
            this.input.setSuggestion(null);
        }
        super.render(graphics, mouseX, mouseY);
    }

    @Override
    public void renderUsage(@NotNull GuiGraphics graphics) {
        if (!this.isAllowRenderUsage()) return;
        super.renderUsage(graphics);
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
            // Fields that only suggest their own values (like variable names) suggest nothing when they have none, not the server's chat suggestions
            if (suggestionStringList.isEmpty() && !this.onlyCustomSuggestions && (this.minecraft.player != null)) {
                suggestionStringList = this.minecraft.player.connection.getSuggestionsProvider().getCustomTabSugggestions();
            }
            this.setPendingSuggestions(SharedSuggestionProvider.suggest(suggestionStringList, new SuggestionsBuilder(editBoxSubValue, lastWordIndex)));
            //Always show suggestions without pressing TAB
            if (this.autoSuggestions && this.suggestionsAllowed() && this.minecraft.options.autoSuggestions().get()) {
                this.showSuggestions(false);
            }
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
                if (this.renderPosition != SuggestionsRenderPosition.VANILLA) {
                    int listHeight = Math.min(sortedSuggestions.size(), this.suggestionLineLimit) * LIST_LINE_HEIGHT;
                    listY = getListYNextToEditBox(this.renderPosition == SuggestionsRenderPosition.ABOVE_EDIT_BOX, this.input.getY(), this.input.getHeight(), listHeight, this.screen.height);
                }
                this.setSuggestions(new EditBoxSuggestionsList(listX, listY, totalSuggestionsWidth, sortedSuggestions, someNarratingRelatedBoolean));

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

    /**
     * @return If the list of suggestions is open and covers the given point, in the coordinates of the screen of the edit box.
     */
    public boolean isMouseOverList(double mouseX, double mouseY) {
        if (!this.input.isFocused()) return false;
        CommandSuggestions.SuggestionsList list = this.getSuggestions();
        return (list != null) && ((IMixinSuggestionsList) list).getRectFancyMenu().contains(Mth.floor(mouseX), Mth.floor(mouseY));
    }

    protected void updateUsageInfo() {
        this.getAccessor().invokeUpdateUsageInfoFancyMenu();
    }

    protected List<Suggestion> sortSuggestions(Suggestions suggestions) {
        return this.getAccessor().invokeSortSuggestionsFancyMenu(suggestions);
    }

    public boolean suggestionsAllowed() {
        return this.getAccessor().getAllowSuggestionsFancyMenu();
    }

    public boolean autoSuggestionsEnabled() {
        return this.autoSuggestions;
    }

    public void setAutoSuggestionsEnabled(boolean enabled) {
        this.autoSuggestions = enabled;
    }

    public boolean isTextShadow() {
        return this.textShadow;
    }

    public void setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
    }

    public DrawableColor getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(@NotNull DrawableColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public DrawableColor getNormalTextColor() {
        return this.normalTextColor;
    }

    public void setNormalTextColor(@NotNull DrawableColor normalTextColor) {
        this.normalTextColor = normalTextColor;
    }

    public DrawableColor getSelectedTextColor() {
        return this.selectedTextColor;
    }

    public void setSelectedTextColor(@NotNull DrawableColor selectedTextColor) {
        this.selectedTextColor = selectedTextColor;
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

    /**
     * Places a list of suggestions above or below its edit box. Screens and windows cut off everything outside of them, so the list goes to
     * the other side of the edit box when it only fits there, like for the first field of a window, which has no room above it. When it fits
     * on neither side, it goes to the side with more room.
     *
     * @param above If the list should go above the edit box when it fits on both sides.
     * @param listHeight The height of the suggestions the list shows at once.
     * @param screenHeight The height of the screen the list gets rendered in.
     * @return The Y position of the list.
     */
    protected static int getListYNextToEditBox(boolean above, int editBoxY, int editBoxHeight, int listHeight, int screenHeight) {
        int belowY = editBoxY + editBoxHeight + LIST_SPACING;
        int spaceAbove = editBoxY - LIST_SPACING - LIST_EDGE_SPACE;
        int spaceBelow = screenHeight - belowY - LIST_EDGE_SPACE;
        boolean fitsAbove = listHeight <= spaceAbove;
        boolean fitsBelow = listHeight <= spaceBelow;
        boolean useAbove;
        if (fitsAbove != fitsBelow) {
            useAbove = fitsAbove;
        } else if (fitsAbove || (spaceAbove == spaceBelow)) {
            useAbove = above;
        } else {
            useAbove = spaceAbove > spaceBelow;
        }
        return useAbove ? (editBoxY - LIST_SPACING - listHeight) : belowY;
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
    
    public class EditBoxSuggestionsList extends SuggestionsList {

        protected List<Suggestion> suggestionList;

        public EditBoxSuggestionsList(int x, int y, int width, List<Suggestion> suggestionList, boolean someNarratingRelatedBoolean) {
            super(x, y, width, suggestionList, someNarratingRelatedBoolean);
            this.suggestionList = suggestionList;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
            Message message;
            int suggestionLineCount = Math.min(this.suggestionList.size(), EditBoxSuggestions.this.suggestionLineLimit);
            boolean bl = this.getOffset() > 0;
            boolean bl2 = this.suggestionList.size() > this.getOffset() + suggestionLineCount;
            boolean bl3 = bl || bl2;
            boolean bl4 = (this.getLastMouse().x != (float)mouseX) || (this.getLastMouse().y != (float)mouseY);
            if (bl4) {
                this.setLastMouse(new Vec2(mouseX, mouseY));
            }
            if (bl3) {
                int m;
                graphics.fill(this.getRect().getX(), this.getRect().getY() - 1, this.getRect().getX() + this.getRect().getWidth(), this.getRect().getY(), EditBoxSuggestions.this.backgroundColor.getColorInt());
                graphics.fill(this.getRect().getX(), this.getRect().getY() + this.getRect().getHeight(), this.getRect().getX() + this.getRect().getWidth(), this.getRect().getY() + this.getRect().getHeight() + 1, EditBoxSuggestions.this.backgroundColor.getColorInt());
                if (bl) {
                    for (m = 0; m < this.getRect().getWidth(); ++m) {
                        if (m % 2 != 0) continue;
                        graphics.fill(this.getRect().getX() + m, this.getRect().getY() - 1, this.getRect().getX() + m + 1, this.getRect().getY(), -1);
                    }
                }
                if (bl2) {
                    for (m = 0; m < this.getRect().getWidth(); ++m) {
                        if (m % 2 != 0) continue;
                        graphics.fill(this.getRect().getX() + m, this.getRect().getY() + this.getRect().getHeight(), this.getRect().getX() + m + 1, this.getRect().getY() + this.getRect().getHeight() + 1, -1);
                    }
                }
            }
            boolean bl52 = false;
            for (int n = 0; n < suggestionLineCount; ++n) {
                Suggestion suggestion = this.suggestionList.get(n + this.getOffset());
                graphics.fill(this.getRect().getX(), this.getRect().getY() + 12 * n, this.getRect().getX() + this.getRect().getWidth(), this.getRect().getY() + 12 * n + 12, EditBoxSuggestions.this.backgroundColor.getColorInt());
                if (mouseX > this.getRect().getX() && mouseX < this.getRect().getX() + this.getRect().getWidth() && mouseY > this.getRect().getY() + 12 * n && mouseY < this.getRect().getY() + 12 * n + 12) {
                    if (bl4) {
                        this.select(n + this.getOffset());
                    }
                    bl52 = true;
                }
                graphics.drawString(EditBoxSuggestions.this.font, suggestion.getText(), (this.getRect().getX() + 1), (this.getRect().getY() + 2 + 12 * n), ((n + this.getOffset()) == this.getCurrent()) ? EditBoxSuggestions.this.selectedTextColor.getColorInt() : EditBoxSuggestions.this.normalTextColor.getColorInt(), EditBoxSuggestions.this.textShadow);
            }
            if (bl52 && (message = this.suggestionList.get(this.getCurrent()).getTooltip()) != null) {
                graphics.renderTooltip(EditBoxSuggestions.this.font, ComponentUtils.fromMessage(message), mouseX, mouseY);
            }
        }

        /**
         * Vanilla checks if the pointer is over the list with the position of the pointer on the game window, which is not where the pointer is
         * in screens that render somewhere else, like the bodies of PiP windows. The position the list got rendered with is in the coordinates
         * of the screen of the edit box, like the list itself, so it gets checked instead.
         */
        @Override
        public boolean mouseScrolled(double scroll) {
            Vec2 mouse = this.getLastMouse();
            if (!this.getRect().contains(Mth.floor(mouse.x), Mth.floor(mouse.y))) return false;
            int maxOffset = Math.max(this.suggestionList.size() - EditBoxSuggestions.this.suggestionLineLimit, 0);
            this.getAccessor().setOffsetFancyMenu(Mth.clamp((int) (this.getOffset() - scroll), 0, maxOffset));
            return true;
        }

        public Rect2i getRect() {
            return this.getAccessor().getRectFancyMenu();
        }

        public int getOffset() {
            return this.getAccessor().getOffsetFancyMenu();
        }

        public int getCurrent() {
            return this.getAccessor().getCurrentFancyMenu();
        }
        
        public Vec2 getLastMouse() {
            return this.getAccessor().getLastMouseFancyMenu();
        }

        public void setLastMouse(Vec2 lastMouse) {
            this.getAccessor().setLastMouseFancyMenu(lastMouse);
        }

        public IMixinSuggestionsList getAccessor() {
            return (IMixinSuggestionsList) this;
        }
        
    }

}
