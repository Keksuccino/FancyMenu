package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.profile.Profile;
import de.keksuccino.fancymenu.customization.profile.ProfileHandler;
import de.keksuccino.fancymenu.customization.profile.ProfileLinkType;
import de.keksuccino.fancymenu.customization.profile.ProfileMetadata;
import de.keksuccino.fancymenu.customization.profile.ProfileTask;
import de.keksuccino.fancymenu.customization.profile.ProfileTasks;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.UIIconButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Edits the metadata and icon of a profile. Nothing gets saved before the user clicks "Done".
 */
public class EditProfileWindowBody extends PiPCellWindowBody {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final int PIP_WINDOW_WIDTH = 580;
    public static final int PIP_WINDOW_HEIGHT = 500;
    private static final int ICON_PREVIEW_SIZE = 48;

    @NotNull
    private final Profile profile;
    @NotNull
    private final ProfileMetadata draft;
    @NotNull
    private final List<LinkDraft> links = new ArrayList<>();
    @NotNull
    private final Consumer<Boolean> callback;
    @NotNull
    private final ProfileIconCache previewCache = new ProfileIconCache();
    /** Authors and the issue tracker URL get parsed on save, because parsing them while typing would change what the user types. */
    @NotNull
    private String authorsText = "";
    @NotNull
    private String issueTrackerText = "";
    @Nullable
    private Path pendingIconSource;
    private boolean removeIcon = false;
    private boolean finished = false;

    static void openInWindow(@NotNull Profile profile, @Nullable PiPWindow parentWindow, @NotNull Consumer<Boolean> callback) {
        EditProfileWindowBody body = new EditProfileWindowBody(profile, callback);
        PiPWindow window = new PiPWindow(body.getTitle())
                .setScreen(body)
                .setIcon(MaterialIcons.EDIT)
                .setForceFancyMenuUiScale(true)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
    }

    private EditProfileWindowBody(@NotNull Profile profile, @NotNull Consumer<Boolean> callback) {
        super(Component.translatable("fancymenu.profiles.edit.title"));
        this.profile = profile;
        this.draft = profile.metadata().copy();
        this.callback = callback;
        for (ProfileMetadata.Link link : this.draft.getLinks()) {
            this.links.add(new LinkDraft(link.type(), link.label(), link.url(), true));
        }
        this.setAllowCloseOnEsc(false);
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        this.addCell(new IconCell());

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.profiles.edit.profile_title"));
        TextInputCell titleCell = this.addTextInputCell(null, false, false).setText(this.draft.getTitle());
        titleCell.editBox.setMaxLength(ProfileMetadata.MAX_TITLE_LENGTH);
        titleCell.setEditListener(this.draft::setTitle);

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.profiles.edit.version"));
        TextInputCell versionCell = this.addTextInputCell(null, false, false).setText(this.draft.getVersion());
        versionCell.editBox.setMaxLength(ProfileMetadata.MAX_VERSION_LENGTH);
        versionCell.editBox.setHintFancyMenu(consumes -> Component.translatable("fancymenu.profiles.edit.version.hint"));
        versionCell.setEditListener(this.draft::setVersion);

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.profiles.edit.authors"));
        TextInputCell authorsCell = this.addTextInputCell(null, false, false).setText(this.draft.getAuthorsAsString());
        authorsCell.editBox.setHintFancyMenu(consumes -> Component.translatable("fancymenu.profiles.edit.authors.hint"));
        authorsCell.setTooltip(() -> UITooltip.of(Component.translatable("fancymenu.profiles.edit.authors.desc")));
        authorsCell.setEditListener(value -> this.authorsText = value);
        this.authorsText = this.draft.getAuthorsAsString();

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.profiles.edit.description"));
        TextInputCell descriptionCell = this.addTextInputCell(null, true, false).setEditorMultiLineMode(true).setText(escapeLineBreaks(this.draft.getDescription()));
        descriptionCell.editBox.setMaxLength(ProfileMetadata.MAX_DESCRIPTION_LENGTH * 2);
        descriptionCell.editBox.setHintFancyMenu(consumes -> Component.translatable("fancymenu.profiles.edit.description.hint"));
        descriptionCell.setTooltip(() -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.profiles.edit.description.desc")));
        descriptionCell.setEditListener(value -> this.draft.setDescription(unescapeLineBreaks(value)));

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.profiles.edit.issue_tracker"));
        TextInputCell issueCell = this.addTextInputCell(CharacterFilter.buildUrlFilter(), false, false).setText((this.draft.getIssueTrackerUrl() != null) ? this.draft.getIssueTrackerUrl() : "");
        issueCell.editBox.setHintFancyMenu(consumes -> Component.translatable("fancymenu.profiles.edit.url.hint"));
        issueCell.setTooltip(() -> UITooltip.of(Component.translatable("fancymenu.profiles.edit.issue_tracker.desc")));
        issueCell.setEditListener(value -> this.issueTrackerText = value);
        this.issueTrackerText = issueCell.getText();

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.profiles.edit.links"));
        for (LinkDraft link : this.links) {
            this.addCell(new LinkCell(link));
        }
        ExtendedButton addLinkButton = new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.profiles.edit.links.add"), button -> {
            if (this.links.size() >= ProfileMetadata.MAX_LINKS) return;
            this.links.add(new LinkDraft(ProfileLinkType.WEBSITE, "", "", false));
            this.rebuild();
        });
        addLinkButton.setIsActiveSupplier(consumes -> this.links.size() < ProfileMetadata.MAX_LINKS);
        addLinkButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.profiles.edit.links.add.desc")));
        this.addWidgetCell(addLinkButton, true);

        this.addStartEndSpacerCell();

    }

    @Override
    protected void onCancel() {
        this.finish(false);
    }

    @Override
    protected void onDone() {

        if (this.draft.getTitle().isBlank()) {
            Dialogs.openMessage(Component.translatable("fancymenu.profiles.edit.error.no_title"), MessageDialogStyle.ERROR);
            return;
        }
        if (!this.issueTrackerText.isBlank() && (ProfileMetadata.normalizeWebUrl(this.issueTrackerText) == null)) {
            Dialogs.openMessage(Component.translatable("fancymenu.profiles.edit.error.invalid_url", this.issueTrackerText), MessageDialogStyle.ERROR);
            return;
        }
        List<ProfileMetadata.Link> validLinks = new ArrayList<>();
        for (LinkDraft link : this.links) {
            if (link.url.isBlank()) continue;
            ProfileMetadata.Link valid = ProfileMetadata.Link.of(link.type, link.label, link.url);
            if (valid == null) {
                Dialogs.openMessage(Component.translatable("fancymenu.profiles.edit.error.invalid_url", link.url), MessageDialogStyle.ERROR);
                return;
            }
            validLinks.add(valid);
        }

        this.draft.setAuthorsFromString(this.authorsText);
        this.draft.setIssueTrackerUrl(this.issueTrackerText);
        this.draft.setLinks(validLinks);
        try {
            ProfileHandler.saveMetadata(this.profile, this.draft);
            if (this.removeIcon) ProfileTasks.removeIcon(this.profile);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to save profile metadata!", ex);
            Dialogs.openMessage(Component.translatable("fancymenu.profiles.error.save").append("\n\n").append(Component.translatable("fancymenu.profiles.error.reason.generic", String.valueOf(ex.getMessage()))), MessageDialogStyle.ERROR);
            return;
        }

        Path iconSource = this.pendingIconSource;
        if (iconSource == null) {
            this.finish(true);
            return;
        }
        if (ProfileHandler.isBusy()) {
            Dialogs.openMessage(Component.translatable("fancymenu.profiles.error.busy"), MessageDialogStyle.ERROR);
            return;
        }
        ProfileTasks.setIcon(this.profile, iconSource, task -> {
            if (task.getState() == ProfileTask.State.FAILED) {
                Dialogs.openMessage(Component.translatable("fancymenu.profiles.edit.error.icon"), MessageDialogStyle.ERROR);
            }
            this.callback.accept(true);
        });
        this.finished = true;
        this.closeWindow();

    }

    private void finish(boolean changed) {
        if (this.finished) return;
        this.finished = true;
        this.callback.accept(changed);
        this.closeWindow();
    }

    @Override
    public void onWindowClosedExternally() {
        if (!this.finished) {
            this.finished = true;
            this.callback.accept(false);
        }
    }

    @Override
    public void onScreenClosed() {
        this.previewCache.clear();
    }

    @Override
    public boolean allowEnterForDone() {
        return false;
    }

    private void chooseIcon() {
        ChooseFileWindowBody chooser = new ChooseFileWindowBody(null, LayoutHandler.ASSETS_DIR, file -> {
            if (file == null) return;
            this.pendingIconSource = file.toPath();
            this.removeIcon = false;
        });
        chooser.setFileTypes(FileTypeGroup.of(FileTypes.PNG_IMAGE));
        chooser.setBlockResourceUnfriendlyFileNames(false);
        chooser.openInWindow(this.getWindow());
    }

    private boolean hasIcon() {
        return (this.pendingIconSource != null) || (!this.removeIcon && (this.previewCache.get(this.profile.getIconFile()) != null));
    }

    /**
     * Setting a value moves the cursor to its end, which hides the start of long URLs.
     */
    private static void showStart(@NotNull ExtendedEditBox editBox) {
        editBox.setCursorPosition(0);
        editBox.setHighlightPos(0);
        editBox.setDisplayPosition(0);
    }

    @NotNull
    private static String escapeLineBreaks(@NotNull String text) {
        return text.replace("\n", "\\n");
    }

    @NotNull
    private static String unescapeLineBreaks(@NotNull String text) {
        return text.replace("\\n", "\n");
    }

    /**
     * Shows the icon the profile will have after saving, with buttons to change or remove it.
     */
    private class IconCell extends RenderCell {

        private final ExtendedButton chooseButton;
        private final ExtendedButton removeButton;

        private IconCell() {
            this.chooseButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.profiles.edit.icon.choose"), button -> EditProfileWindowBody.this.chooseIcon());
            this.chooseButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.profiles.edit.icon.choose.desc")));
            UIBase.applyDefaultWidgetSkinTo(this.chooseButton, UIBase.shouldBlur());
            this.removeButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.profiles.edit.icon.remove"), button -> {
                EditProfileWindowBody.this.pendingIconSource = null;
                EditProfileWindowBody.this.removeIcon = true;
            });
            this.removeButton.setIsActiveSupplier(consumes -> EditProfileWindowBody.this.hasIcon());
            UIBase.applyDefaultWidgetSkinTo(this.removeButton, UIBase.shouldBlur());
            this.children().add(this.chooseButton);
            this.children().add(this.removeButton);
            this.setSelectable(false);
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            EditProfileWindowBody body = EditProfileWindowBody.this;
            float x = this.getX();
            float y = this.getY();
            if (body.pendingIconSource != null) {
                ITexture texture = body.previewCache.get(body.pendingIconSource);
                if (texture != null) ProfileUiRendering.renderCenteredSquare(graphics, texture, x, y, ICON_PREVIEW_SIZE);
                UIBase.renderRoundedBorder(graphics, x, y, x + ICON_PREVIEW_SIZE, y + ICON_PREVIEW_SIZE, 1, 0, 0, 0, 0, ProfileUiRendering.getBorderColor());
            } else {
                ITexture icon = body.removeIcon ? null : body.previewCache.get(body.profile.getIconFile());
                ProfileUiRendering.renderProfileIcon(graphics, icon, body.draft.getTitle(), body.profile.getId(), x, y, ICON_PREVIEW_SIZE);
            }
            int buttonX = (int) (x + ICON_PREVIEW_SIZE + 12);
            int buttonWidth = Math.max(60, Math.min(170, this.getWidth() - (ICON_PREVIEW_SIZE + 12)));
            this.chooseButton.setX(buttonX);
            this.chooseButton.setY((int) y + 2);
            this.chooseButton.setWidth(buttonWidth);
            this.removeButton.setX(buttonX);
            this.removeButton.setY((int) y + ICON_PREVIEW_SIZE - 22);
            this.removeButton.setWidth(buttonWidth);
        }

        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            this.setWidth((int) (EditProfileWindowBody.this.scrollArea.getInnerWidth() - 40));
            this.setHeight(ICON_PREVIEW_SIZE);
        }

    }

    /**
     * One link: its platform, a label for custom links, the URL and a button to remove it.
     */
    private class LinkCell extends RenderCell {

        private static final int TYPE_BUTTON_WIDTH = 100;
        private static final int LABEL_BOX_WIDTH = 80;
        private static final int GAP = 4;

        @NotNull
        private final LinkDraft link;
        private final ExtendedButton typeButton;
        private final ExtendedEditBox labelBox;
        private final ExtendedEditBox urlBox;
        private final UIIconButton removeButton;

        private LinkCell(@NotNull LinkDraft link) {
            this.link = link;
            this.typeButton = new ExtendedButton(0, 0, TYPE_BUTTON_WIDTH, 20, Component.empty(), button -> this.openTypeMenu());
            this.typeButton.setLabelSupplier(consumes -> ProfileUiRendering.getLinkTypeName(this.link.type));
            this.typeButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.profiles.edit.links.type.desc")));
            UIBase.applyDefaultWidgetSkinTo(this.typeButton, UIBase.shouldBlur());

            this.labelBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, LABEL_BOX_WIDTH, 18, Component.empty());
            this.labelBox.setMaxLength(ProfileMetadata.MAX_LINK_LABEL_LENGTH);
            this.labelBox.setValue(link.label);
            showStart(this.labelBox);
            this.labelBox.setHintFancyMenu(consumes -> Component.translatable("fancymenu.profiles.edit.links.label.hint"));
            this.labelBox.setResponder(value -> this.link.label = value);
            UIBase.applyDefaultWidgetSkinTo(this.labelBox, UIBase.shouldBlur());

            this.urlBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 100, 18, Component.empty());
            this.urlBox.setMaxLength(ProfileMetadata.MAX_URL_LENGTH);
            this.urlBox.setCharacterFilter(CharacterFilter.buildUrlFilter());
            this.urlBox.setValue(link.url);
            showStart(this.urlBox);
            this.urlBox.setHintFancyMenu(consumes -> Component.translatable("fancymenu.profiles.edit.url.hint"));
            this.urlBox.setResponder(value -> {
                this.link.url = value;
                // Pasted links pick their platform on their own, until the user chooses one
                if (!this.link.typeChosen) this.link.type = ProfileLinkType.detectFromUrl(value);
            });
            UIBase.applyDefaultWidgetSkinTo(this.urlBox, UIBase.shouldBlur());

            this.removeButton = new UIIconButton(0, 0, 20, 20, MaterialIcons.CLOSE, button -> {
                EditProfileWindowBody.this.links.remove(this.link);
                EditProfileWindowBody.this.rebuild();
            }).setIconPadding(5);

            this.children().add(this.typeButton);
            this.children().add(this.labelBox);
            this.children().add(this.urlBox);
            this.children().add(this.removeButton);
            this.setSearchStringSupplier(() -> this.link.url);
        }

        private void openTypeMenu() {
            ContextMenu menu = new ContextMenu();
            for (ProfileLinkType type : ProfileLinkType.values()) {
                menu.addClickableEntry("type_" + type.getSerializedName(), ProfileUiRendering.getLinkTypeName(type), (m, entry) -> {
                    this.link.type = type;
                    this.link.typeChosen = true;
                    m.closeMenu();
                }).setIcon(ProfileUiRendering.getLinkIcon(type));
            }
            ContextMenuHandler.INSTANCE.setAndOpenAtMouse(menu);
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            int x = this.getX();
            int y = this.getY();
            boolean showLabel = this.link.type == ProfileLinkType.OTHER;
            this.typeButton.setX(x);
            this.typeButton.setY(y);
            int urlX = x + TYPE_BUTTON_WIDTH + GAP;
            this.labelBox.visible = showLabel;
            if (showLabel) {
                this.labelBox.setX(urlX + 1);
                this.labelBox.setY(y + 1);
                urlX += LABEL_BOX_WIDTH + GAP;
            }
            int removeX = x + this.getWidth() - 20;
            this.urlBox.setX(urlX + 1);
            this.urlBox.setY(y + 1);
            this.urlBox.setWidth(Math.max(40, removeX - GAP - urlX - 2));
            this.removeButton.setX(removeX);
            this.removeButton.setY(y);
        }

    }

    private static final class LinkDraft {

        @NotNull
        private ProfileLinkType type;
        @NotNull
        private String label;
        @NotNull
        private String url;
        /** Links of saved metadata and links whose type the user picked keep their type when the URL changes. */
        private boolean typeChosen;

        private LinkDraft(@NotNull ProfileLinkType type, @NotNull String label, @NotNull String url, boolean typeChosen) {
            this.type = type;
            this.label = label;
            this.url = url;
            this.typeChosen = typeChosen;
        }

    }

}
