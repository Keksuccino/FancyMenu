package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.customization.profile.Profile;
import de.keksuccino.fancymenu.customization.profile.ProfileArchive;
import de.keksuccino.fancymenu.customization.profile.ProfileHandler;
import de.keksuccino.fancymenu.customization.profile.ProfileMetadata;
import de.keksuccino.fancymenu.customization.profile.ProfileTasks;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.ScrollableMarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.UIIconButton;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The profile manager. Lists all profiles on the left and shows the selected one with all its details on the right.
 */
public class ManageProfilesWindowBody extends PiPWindowBody {

    public static final int PIP_WINDOW_WIDTH = 760;
    public static final int PIP_WINDOW_HEIGHT = 480;
    private static final int MIN_WINDOW_WIDTH = 640;
    private static final int MIN_WINDOW_HEIGHT = 420;
    private static final int MARGIN = 20;
    private static final int COLUMN_GAP = 16;
    private static final int LIST_WIDTH = 236;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ICON_BUTTON_GAP = 2;
    private static final int DESTRUCTIVE_ACTION_GAP = 8;
    private static final int DESCRIPTION_PADDING = 8;

    @Nullable
    private static ManageProfilesWindowBody openInstance;

    @NotNull
    private final ProfileIconCache iconCache = new ProfileIconCache();
    @NotNull
    private final ProfileDetailsPanel detailsPanel = new ProfileDetailsPanel(this);
    @NotNull
    private final ProfileTaskOverlay taskOverlay = new ProfileTaskOverlay();
    @NotNull
    private final ProfileActions actions = new ProfileActions(this);
    @NotNull
    private final Map<String, ProfileTasks.ProfileStats> stats = new HashMap<>();
    @NotNull
    private final Set<String> statsRequested = new HashSet<>();
    @NotNull
    private List<Profile> profiles = new ArrayList<>();
    @Nullable
    private String selectedId;
    private ScrollArea listArea;
    private ScrollableMarkdownRenderer descriptionRenderer;
    @Nullable
    private String shownDescription;
    private float[] descriptionBounds = new float[4];
    private float descriptionBoxY = 0;
    private UIIconButton newButton;
    private UIIconButton importButton;
    private UIIconButton editButton;
    private UIIconButton exportButton;
    private UIIconButton duplicateButton;
    private UIIconButton openFolderButton;
    private UIIconButton deleteButton;
    private ExtendedButton loadButton;

    /**
     * Opens the profile manager, or brings it to the front if it is already open.
     */
    public static void openInWindow() {
        ManageProfilesWindowBody existing = openInstance;
        if ((existing != null) && (existing.getWindow() != null) && PiPWindowHandler.INSTANCE.getOpenWindows().contains(existing.getWindow())) {
            PiPWindowHandler.INSTANCE.bringToFront(existing.getWindow());
            return;
        }
        ManageProfilesWindowBody body = new ManageProfilesWindowBody();
        PiPWindow window = new PiPWindow(Component.translatable("fancymenu.profiles.manage"))
                .setScreen(body)
                .setIcon(MaterialIcons.STACKS)
                .setForceFancyMenuUiScale(true)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        openInstance = body;
    }

    private ManageProfilesWindowBody() {
        super(Component.translatable("fancymenu.profiles.manage"));
        this.refreshProfiles(null);
    }

    @Override
    protected void init() {

        boolean blur = UIBase.shouldBlur();

        float oldScroll = (this.listArea != null) ? this.listArea.verticalScrollBar.getScroll() : 0.0F;
        this.listArea = new ScrollArea(MARGIN, MARGIN, LIST_WIDTH, this.height - (MARGIN * 2) - BUTTON_HEIGHT - 8);
        this.listArea.setSetupForBlurInterface(blur);
        this.listArea.makeEntriesWidthOfArea = true;
        this.listArea.horizontalScrollBar.active = false;
        this.rebuildListEntries();
        this.listArea.verticalScrollBar.setScroll(oldScroll);
        this.addRenderableWidget(this.listArea);

        int bottomY = this.height - MARGIN - BUTTON_HEIGHT;
        this.newButton = this.addIconButton(MaterialIcons.ADD, "fancymenu.profiles.new", button -> this.actions.createEmpty())
                .setIsActiveSupplier(consumes -> !ProfileHandler.isBusy());
        this.importButton = this.addIconButton(MaterialIcons.DOWNLOAD, "fancymenu.profiles.import", button -> this.actions.chooseArchiveToImport())
                .setIsActiveSupplier(consumes -> !ProfileHandler.isBusy());

        this.editButton = this.addIconButton(MaterialIcons.EDIT, "fancymenu.profiles.edit", button -> {
            Profile selected = this.getSelectedProfile();
            if (selected != null) this.actions.edit(selected);
        }).setIsActiveSupplier(consumes -> this.canUseSelectedProfile());
        this.exportButton = this.addIconButton(MaterialIcons.IOS_SHARE, "fancymenu.profiles.export", button -> {
            Profile selected = this.getSelectedProfile();
            if (selected != null) this.actions.export(selected);
        }).setIsActiveSupplier(consumes -> this.canUseSelectedProfile());
        this.duplicateButton = this.addIconButton(MaterialIcons.CONTENT_COPY, "fancymenu.profiles.duplicate", button -> {
            Profile selected = this.getSelectedProfile();
            if (selected != null) this.actions.duplicate(selected);
        }).setIsActiveSupplier(consumes -> this.canUseSelectedProfile());
        this.openFolderButton = this.addIconButton(MaterialIcons.FOLDER_OPEN, "fancymenu.profiles.open_folder", button -> {
            Profile selected = this.getSelectedProfile();
            if (selected != null) this.actions.openFolder(selected);
        }).setIsActiveSupplier(consumes -> this.canUseSelectedProfile());
        UITooltip deleteTooltip = createActionTooltip("fancymenu.profiles.delete", "fancymenu.profiles.delete.desc");
        UITooltip deleteActiveTooltip = createActionTooltip("fancymenu.profiles.delete", "fancymenu.profiles.delete.active");
        this.deleteButton = this.addIconButton(MaterialIcons.DELETE, "fancymenu.profiles.delete", button -> {
            Profile selected = this.getSelectedProfile();
            if (selected != null) this.actions.delete(selected);
        }).setIsActiveSupplier(consumes -> {
            Profile selected = this.getSelectedProfile();
            return !ProfileHandler.isBusy() && (selected != null) && !selected.active();
        }).setTooltipSupplier(consumes -> {
            Profile selected = this.getSelectedProfile();
            return ((selected != null) && selected.active()) ? deleteActiveTooltip : deleteTooltip;
        });

        this.loadButton = this.addButton(new ExtendedButton(0, bottomY, 130, BUTTON_HEIGHT, Component.translatable("fancymenu.profiles.load"), button -> {
            Profile selected = this.getSelectedProfile();
            if (selected != null) this.loadProfile(selected);
        }));
        this.loadButton.setIsActiveSupplier(consumes -> {
            Profile selected = this.getSelectedProfile();
            return (selected != null) && !selected.active() && !ProfileHandler.isBusy();
        });
        this.loadButton.setLabelSupplier(consumes -> {
            Profile selected = this.getSelectedProfile();
            return Component.translatable(((selected != null) && selected.active()) ? "fancymenu.profiles.load.active" : "fancymenu.profiles.load");
        });
        this.loadButton.setUITooltipSupplier(consumes -> {
            Profile selected = this.getSelectedProfile();
            return UITooltip.of(LocalizationUtils.splitLocalizedLines(((selected != null) && selected.active()) ? "fancymenu.profiles.load.active.desc" : "fancymenu.profiles.load.desc"));
        });

        if (this.descriptionRenderer == null) this.descriptionRenderer = new ScrollableMarkdownRenderer(0, 0, 10, 10);
        this.descriptionBounds = new float[4];
        this.shownDescription = null;
        this.addRenderableWidget(this.descriptionRenderer);

    }

    @NotNull
    private ExtendedButton addButton(@NotNull ExtendedButton button) {
        UIBase.applyDefaultWidgetSkinTo(button, UIBase.shouldBlur());
        return this.addRenderableWidget(button);
    }

    /**
     * Adds a toolbar icon button with a tooltip made of the {@code <key>} and {@code <key>.desc} localizations.
     */
    @NotNull
    private UIIconButton addIconButton(@NotNull MaterialIcon icon, @NotNull String localizationKey, @NotNull Consumer<UIIconButton> clickAction) {
        UIIconButton button = new UIIconButton(0, 0, BUTTON_HEIGHT, BUTTON_HEIGHT, icon, clickAction)
                .setTooltip(createActionTooltip(localizationKey, localizationKey + ".desc"));
        return this.addRenderableWidget(button);
    }

    /**
     * Icon buttons have no label, so their tooltip names the action in bold above its description.
     */
    @NotNull
    private static UITooltip createActionTooltip(@NotNull String nameKey, @NotNull String descriptionKey) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(nameKey).withStyle(Style.EMPTY.withBold(true)));
        lines.addAll(Arrays.asList(LocalizationUtils.splitLocalizedLines(descriptionKey)));
        return UITooltip.of(lines.toArray(new Component[0]));
    }

    private boolean canUseSelectedProfile() {
        return !ProfileHandler.isBusy() && (this.getSelectedProfile() != null);
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        float panelX = MARGIN + LIST_WIDTH + COLUMN_GAP;
        float panelWidth = this.width - panelX - MARGIN;
        int bottomY = this.height - MARGIN - BUTTON_HEIGHT;

        Profile selected = this.getSelectedProfile();

        // Toolbars: list actions below the list, profile actions below the details with the main action on the right
        this.layoutToolbar(MARGIN, bottomY, this.newButton, this.importButton);
        float deleteX = this.layoutToolbar(panelX, bottomY, this.editButton, this.exportButton, this.duplicateButton, this.openFolderButton);
        // Delete stays a bit apart from the other actions, so it doesn't get clicked by accident
        this.layoutToolbar(deleteX + DESTRUCTIVE_ACTION_GAP, bottomY, this.deleteButton);
        this.loadButton.setX((int) (panelX + panelWidth - this.loadButton.getWidth()));

        if (selected == null) {
            this.updateDescription(null, 0, 0, 0, 0);
            return;
        }

        if (this.statsRequested.add(selected.getId())) {
            String id = selected.getId();
            ProfileTasks.computeStats(selected, result -> this.stats.put(id, result));
        }

        float headerBottom = this.detailsPanel.render(graphics, selected, this.stats.get(selected.getId()), panelX, MARGIN, panelWidth, mouseX, mouseY);

        // Description box
        float boxY = headerBottom + 14;
        float boxBottom = bottomY - 12;
        float boxHeight = Math.max(24, boxBottom - boxY);
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        UIBase.renderRoundedRect(graphics, panelX, boxY, panelWidth, boxHeight, radius, radius, radius, radius, ProfileUiRendering.getAreaColor());
        UIBase.renderRoundedBorder(graphics, panelX, boxY, panelX + panelWidth, boxY + boxHeight, 1, radius, radius, radius, radius, ProfileUiRendering.getBorderColor());
        this.descriptionBoxY = boxY;

        String description = selected.metadata().getDescription();
        if (description.isEmpty()) {
            this.updateDescription(null, 0, 0, 0, 0);
            Component placeholder = Component.translatable("fancymenu.profiles.no_description");
            float textSize = UIBase.getUITextSizeNormal();
            float textWidth = UIBase.getUITextWidth(placeholder, textSize);
            UIBase.renderText(graphics, placeholder, panelX + ((panelWidth - textWidth) / 2.0F), boxY + ((boxHeight - UIBase.getUITextHeight(textSize)) / 2.0F), ProfileUiRendering.getDimTextColor(), textSize);
        } else {
            this.updateDescription(description, panelX + DESCRIPTION_PADDING, boxY + DESCRIPTION_PADDING, panelWidth - (DESCRIPTION_PADDING * 2) - 6, boxHeight - (DESCRIPTION_PADDING * 2));
        }

    }

    /**
     * Places the buttons next to each other and returns the X position after the last one.
     */
    private float layoutToolbar(float x, float y, @NotNull UIIconButton... buttons) {
        for (UIIconButton button : buttons) {
            button.setX(x).setY(y);
            x += button.getWidth() + ICON_BUTTON_GAP;
        }
        return x;
    }

    @Override
    public void renderLateBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.taskOverlay.render(graphics, mouseX, mouseY, partial, this.width, this.height, ProfileTasks.getCurrentTask(), ProfileHandler.isSwitching());
    }

    /**
     * The markdown renderer needs a rebuild when its area changes, so this only happens when the selected profile or the window size changes.
     */
    private void updateDescription(@Nullable String description, float x, float y, float width, float height) {
        if (description == null) {
            if (this.shownDescription != null) {
                this.descriptionRenderer.setText("");
                this.shownDescription = null;
            }
            return;
        }
        float[] bounds = {x, y, width, height};
        if (!Arrays.equals(bounds, this.descriptionBounds)) {
            this.descriptionBounds = bounds;
            this.descriptionRenderer.rebuild(x, y, width, height);
            this.styleDescriptionRenderer();
            this.shownDescription = null;
        }
        if (!description.equals(this.shownDescription)) {
            this.descriptionRenderer.setText(description);
            this.descriptionRenderer.getScrollArea().verticalScrollBar.setScroll(0.0F);
            this.shownDescription = description;
        }
    }

    private void styleDescriptionRenderer() {
        this.descriptionRenderer.getMarkdownRenderer().setAutoLineBreakingEnabled(true);
        this.descriptionRenderer.getMarkdownRenderer().setTextBaseColor(DrawableColor.of(ProfileUiRendering.getTextColor()));
        this.descriptionRenderer.getMarkdownRenderer().setTextShadow(false);
        this.descriptionRenderer.getMarkdownRenderer().setUIFontRenderingEnabled(true);
        // Descriptions come from other users, so links must never open local files or programs
        this.descriptionRenderer.getMarkdownRenderer().setHyperlinkFilter(link -> ProfileMetadata.parseWebUri(link) != null);
    }

    private void rebuildListEntries() {
        this.listArea.clearEntries();
        for (Profile profile : this.profiles) {
            ProfileListEntry entry = new ProfileListEntry(this.listArea, profile, this);
            this.listArea.addEntry(entry);
            if (profile.getId().equals(this.selectedId)) entry.setSelected(true);
        }
    }

    /**
     * Reads all profiles again and keeps the current selection, or selects the given profile.
     */
    void refreshProfiles(@Nullable String selectId) {
        this.profiles = ProfileHandler.getProfiles();
        // Profiles can change on disk at any time, so their stats get measured again, while the old values stay visible until then
        this.statsRequested.clear();
        String targetId = (selectId != null) ? selectId : this.selectedId;
        this.selectedId = null;
        for (Profile profile : this.profiles) {
            if (profile.getId().equals(targetId)) this.selectedId = targetId;
        }
        if ((this.selectedId == null) && !this.profiles.isEmpty()) this.selectedId = this.profiles.get(0).getId();
        if (this.listArea != null) {
            float scroll = this.listArea.verticalScrollBar.getScroll();
            this.rebuildListEntries();
            this.listArea.verticalScrollBar.setScroll(scroll);
            if (selectId != null) this.scrollToSelected();
        }
    }

    /**
     * Forgets everything cached about profiles, like after a switch, because then the files of the active profile belong to another profile.
     */
    void clearCaches() {
        this.iconCache.clear();
        this.stats.clear();
        this.statsRequested.clear();
    }

    void invalidateIcon(@NotNull Profile profile) {
        this.iconCache.invalidate(profile.getIconFile());
    }

    void invalidateStats(@NotNull String profileId) {
        this.stats.remove(profileId);
        this.statsRequested.remove(profileId);
    }

    @Nullable
    ITexture getIcon(@NotNull Profile profile) {
        return this.iconCache.get(profile.getIconFile());
    }

    @NotNull
    String getDisplayTitle(@NotNull Profile profile) {
        String title = profile.metadata().getTitle();
        return title.isEmpty() ? I18n.get("fancymenu.profiles.untitled") : title;
    }

    @Nullable
    Profile getSelectedProfile() {
        if (this.selectedId == null) return null;
        for (Profile profile : this.profiles) {
            if (profile.getId().equals(this.selectedId)) return profile;
        }
        return null;
    }

    void onEntrySelected(@NotNull Profile profile) {
        this.selectedId = profile.getId();
    }

    void loadProfile(@NotNull Profile profile) {
        if (ProfileHandler.isBusy() || profile.active()) return;
        this.actions.load(profile);
    }

    void openProfileContextMenu(@NotNull Profile profile) {
        if (ProfileHandler.isBusy()) return;
        ContextMenuHandler.INSTANCE.setAndOpenAtMouse(this.buildProfileMenu(profile));
    }

    @NotNull
    private ContextMenu buildProfileMenu(@NotNull Profile profile) {
        ContextMenu menu = new ContextMenu();
        menu.addClickableEntry("load", Component.translatable("fancymenu.profiles.load"), (m, entry) -> {
            m.closeMenu();
            this.loadProfile(profile);
        }).setIsActiveSupplier((m, e) -> !profile.active() && !ProfileHandler.isBusy()).setIcon(MaterialIcons.PLAY_ARROW);
        menu.addClickableEntry("edit", Component.translatable("fancymenu.profiles.edit"), (m, entry) -> {
            m.closeMenu();
            this.actions.edit(profile);
        }).setIcon(MaterialIcons.EDIT);
        menu.addClickableEntry("export", Component.translatable("fancymenu.profiles.export"), (m, entry) -> {
            m.closeMenu();
            this.actions.export(profile);
        }).setIcon(MaterialIcons.IOS_SHARE);
        menu.addSeparatorEntry("separator_after_main_actions");
        menu.addClickableEntry("duplicate", Component.translatable("fancymenu.profiles.duplicate"), (m, entry) -> {
            m.closeMenu();
            this.actions.duplicate(profile);
        }).setTooltipSupplier((m, e) -> UITooltip.of(Component.translatable("fancymenu.profiles.duplicate.desc"))).setIcon(MaterialIcons.CONTENT_COPY);
        menu.addClickableEntry("open_folder", Component.translatable("fancymenu.profiles.open_folder"), (m, entry) -> {
            m.closeMenu();
            this.actions.openFolder(profile);
        }).setIcon(MaterialIcons.FOLDER_OPEN);
        menu.addSeparatorEntry("separator_before_delete");
        menu.addClickableEntry("delete", Component.translatable("fancymenu.profiles.delete"), (m, entry) -> {
            m.closeMenu();
            this.actions.delete(profile);
        }).setIsActiveSupplier((m, e) -> !profile.active())
                .setTooltipSupplier((m, e) -> profile.active() ? UITooltip.of(Component.translatable("fancymenu.profiles.delete.active")) : null)
                .setIcon(MaterialIcons.DELETE);
        return menu;
    }

    private void scrollToSelected() {
        List<ScrollAreaEntry> entries = this.listArea.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            if ((entries.get(i) instanceof ProfileListEntry entry) && entry.profile.getId().equals(this.selectedId)) {
                float totalScroll = this.listArea.getTotalScrollHeight();
                if (totalScroll <= 0) return;
                float entryTop = i * ProfileListEntry.HEIGHT;
                float visibleTop = totalScroll * this.listArea.verticalScrollBar.getScroll();
                float visibleBottom = visibleTop + this.listArea.getInnerHeight();
                if (entryTop < visibleTop) {
                    this.listArea.verticalScrollBar.setScroll(entryTop / totalScroll);
                } else if ((entryTop + ProfileListEntry.HEIGHT) > visibleBottom) {
                    this.listArea.verticalScrollBar.setScroll(Math.min(1.0F, ((entryTop + ProfileListEntry.HEIGHT) - this.listArea.getInnerHeight()) / totalScroll));
                }
                return;
            }
        }
    }

    private void moveSelection(int direction) {
        if (this.profiles.isEmpty()) return;
        int index = 0;
        for (int i = 0; i < this.profiles.size(); i++) {
            if (this.profiles.get(i).getId().equals(this.selectedId)) index = i;
        }
        index = Math.max(0, Math.min(this.profiles.size() - 1, index + direction));
        this.selectedId = this.profiles.get(index).getId();
        for (ScrollAreaEntry entry : this.listArea.getEntries()) {
            if ((entry instanceof ProfileListEntry profileEntry) && profileEntry.profile.getId().equals(this.selectedId)) entry.setSelected(true);
        }
        this.scrollToSelected();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.taskOverlay.mouseClicked(mouseX, mouseY, button, ProfileHandler.isBusy())) return true;
        if (this.detailsPanel.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!ProfileHandler.isBusy()) {
            if (keyCode == InputConstants.KEY_UP) {
                this.moveSelection(-1);
                return true;
            }
            if (keyCode == InputConstants.KEY_DOWN) {
                this.moveSelection(1);
                return true;
            }
            Profile selected = this.getSelectedProfile();
            if ((selected != null) && ((keyCode == InputConstants.KEY_RETURN) || (keyCode == InputConstants.KEY_NUMPADENTER))) {
                this.loadProfile(selected);
                return true;
            }
            if ((selected != null) && (keyCode == InputConstants.KEY_DELETE) && !selected.active()) {
                this.actions.delete(selected);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (ProfileHandler.isBusy()) return true;
        if ((mouseY >= this.descriptionBoxY) && (mouseX > (MARGIN + LIST_WIDTH))) {
            return this.descriptionRenderer.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
        }
        return this.listArea.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    @Override
    public void onFilesDrop(@NotNull List<Path> paths) {
        for (Path path : paths) {
            if (ProfileArchive.hasArchiveExtension(path)) {
                this.actions.importArchive(path);
                return;
            }
        }
        if (!paths.isEmpty()) this.actions.showNoArchiveDropped();
    }

    @Override
    public void onScreenClosed() {
        this.iconCache.clear();
        if (openInstance == this) openInstance = null;
    }

    @Override
    public void onWindowClosedExternally() {
        this.onScreenClosed();
    }

    @NotNull
    List<Profile> getProfiles() {
        return this.profiles;
    }

    boolean isOpen() {
        PiPWindow window = this.getWindow();
        return (window != null) && PiPWindowHandler.INSTANCE.getOpenWindows().contains(Objects.requireNonNull(window));
    }

}
