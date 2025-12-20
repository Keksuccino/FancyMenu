package de.keksuccino.fancymenu.customization.global;

import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ManageGlobalMenuMusicTracksScreen extends CellScreen {

    private final List<String> cachedTracks = new ArrayList<>();
    private final Consumer<Boolean> callback;

    public ManageGlobalMenuMusicTracksScreen(@NotNull Consumer<Boolean> callback) {
        super(Component.translatable("fancymenu.global_customizations.menu_music_tracks.manage"));
        this.callback = callback;
        this.cachedTracks.addAll(GlobalCustomizationHandler.getCustomMenuMusicTracks());
        this.setSearchBarEnabled(true);
    }

    @Override
    protected void initCells() {

        this.addCellGroupEndSpacerCell().setIgnoreSearch();

        if (this.cachedTracks.isEmpty()) {
            this.addLabelCell(Component.translatable("fancymenu.global_customizations.menu_music_tracks.empty")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())));
        } else {
            for (String source : this.cachedTracks) {
                String display = ResourceSourceType.getWithoutSourcePrefix(source);
                this.addLabelCell(Component.literal(display)
                                .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt())))
                        .putMemoryValue("source", source)
                        .setSelectable(true);
            }
        }

        this.addCellGroupEndSpacerCell().setIgnoreSearch();

    }

    @Override
    protected void initRightSideWidgets() {

        this.addRightSideButton(20, Component.translatable("fancymenu.global_customizations.menu_music_tracks.add"), button -> {
            Minecraft.getInstance().setScreen(ResourceChooserScreen.audio(null, source -> {
                if (source != null) {
                    addTrack(source);
                }
                Minecraft.getInstance().setScreen(this);
            }));
        });

        this.addRightSideDefaultSpacer();

        this.addRightSideButton(20, Component.translatable("fancymenu.global_customizations.menu_music_tracks.remove"), button -> {
            String selected = this.getSelectedSource();
            if (selected == null) return;
            ConfirmationScreen s = ConfirmationScreen.warning(result -> {
                if (result) {
                    this.cachedTracks.remove(selected);
                    this.rebuild();
                }
                Minecraft.getInstance().setScreen(this);
            }, LocalizationUtils.splitLocalizedLines("fancymenu.global_customizations.menu_music_tracks.remove.confirm"));
            Minecraft.getInstance().setScreen(s);
        }).setIsActiveSupplier(consumes -> (this.getSelectedSource() != null));

        this.addRightSideButton(20, Component.translatable("fancymenu.global_customizations.menu_music_tracks.clear"), button -> {
            ConfirmationScreen s = ConfirmationScreen.warning(result -> {
                if (result) {
                    this.cachedTracks.clear();
                    this.rebuild();
                }
                Minecraft.getInstance().setScreen(this);
            }, LocalizationUtils.splitLocalizedLines("fancymenu.global_customizations.menu_music_tracks.clear.confirm"));
            Minecraft.getInstance().setScreen(s);
        }).setIsActiveSupplier(consumes -> !this.cachedTracks.isEmpty());

    }

    @Nullable
    private String getSelectedSource() {
        RenderCell cell = this.getSelectedCell();
        if (cell != null) {
            return cell.getMemoryValue("source");
        }
        return null;
    }

    private void addTrack(@NotNull String source) {
        if (!this.cachedTracks.contains(source)) {
            this.cachedTracks.add(source);
            this.rebuild();
        }
    }

    @Override
    protected void onCancel() {
        this.callback.accept(false);
    }

    @Override
    protected void onDone() {
        GlobalCustomizationHandler.saveCustomMenuMusicTracks(this.cachedTracks);
        this.callback.accept(true);
    }
}
