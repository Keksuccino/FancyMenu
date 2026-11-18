package de.keksuccino.fancymenu.customization.aiexport;

import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Context menu entries for exporting things for AI chats.
 */
public final class AiExportMenus {

    private static final long DONE_LABEL_DURATION_MS = 2500L;

    private AiExportMenus() {
    }

    /**
     * Adds a sub menu that copies the export to the clipboard or saves it as file.
     *
     * @param exportBuilder Builds the export when the user picks what to do with it, so the export always shows the current state.
     */
    @NotNull
    public static ContextMenu.SubMenuContextMenuEntry addExportSubMenu(@NotNull ContextMenu menu, @NotNull String identifier, @NotNull Component label, @NotNull Supplier<AiExportDocument> exportBuilder) {
        return menu.addSubMenuEntry(identifier, label, buildExportMenu(exportBuilder))
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.ai_export.desc")))
                .setIcon(MaterialIcons.ROBOT_2);
    }

    /**
     * The entries stay open after a click and say for a moment that it worked, so users see that something happened without leaving FancyMenu's UI.
     *
     * @return A menu that copies the export to the clipboard or saves it as file.
     */
    @NotNull
    public static ContextMenu buildExportMenu(@NotNull Supplier<AiExportDocument> exportBuilder) {
        ContextMenu menu = new ContextMenu();
        AtomicLong copiedAt = new AtomicLong(-1L);
        AtomicLong savedAt = new AtomicLong(-1L);
        menu.addClickableEntry("copy_to_clipboard", Component.translatable("fancymenu.ai_export.copy"), (menu1, entry) -> {
                    if (AiExportActions.copyToClipboard(exportBuilder)) copiedAt.set(System.currentTimeMillis());
                }).setLabelSupplier((menu1, entry) -> Component.translatable(isRecent(copiedAt) ? "fancymenu.ai_export.copied" : "fancymenu.ai_export.copy"))
                .setIcon(MaterialIcons.CONTENT_COPY);
        menu.addClickableEntry("save_as_file", Component.translatable("fancymenu.ai_export.save"), (menu1, entry) -> {
                    if (AiExportActions.saveToFile(exportBuilder)) savedAt.set(System.currentTimeMillis());
                }).setLabelSupplier((menu1, entry) -> Component.translatable(isRecent(savedAt) ? "fancymenu.ai_export.saved" : "fancymenu.ai_export.save"))
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.ai_export.save.desc")))
                .setIcon(MaterialIcons.FILE_SAVE);
        return menu;
    }

    private static boolean isRecent(@NotNull AtomicLong time) {
        return (time.get() >= 0L) && ((System.currentTimeMillis() - time.get()) < DONE_LABEL_DURATION_MS);
    }

    /**
     * Opens a menu at the mouse, for buttons that offer exports.
     */
    public static void openMenuAtMouse(@NotNull ContextMenu menu) {
        ContextMenuHandler.INSTANCE.setAndOpenAtMouse(menu);
    }

}
