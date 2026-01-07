package de.keksuccino.fancymenu.util.resource.preload;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.layout.editor.ChoosePanoramaScreen;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseSlideshowScreen;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class ManageResourcePreLoadScreen extends CellScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    protected String cachedSerialized = FancyMenu.getOptions().preLoadResources.getValue();
    protected Consumer<Boolean> callback;

    public ManageResourcePreLoadScreen(@NotNull Consumer<Boolean> callback) {
        super(Component.translatable("fancymenu.resources.pre_loading.manage"));
        this.callback = callback;
        this.setSearchBarEnabled(true);
    }

    @Override
    protected void initCells() {

        this.addCellGroupEndSpacerCell().setIgnoreSearch();

        for (ResourceSource source : ResourcePreLoader.getRegisteredResourceSources(this.cachedSerialized)) {

            String sourceString;
            if (source instanceof ResourcePreLoader.CubicPanoramaSource) {
                sourceString = "[" + I18n.get("fancymenu.backgrounds.panorama") + "] " + source.getSourceWithoutPrefix();
            } else if (source instanceof ResourcePreLoader.SlideshowSource) {
                sourceString = "[" + I18n.get("fancymenu.backgrounds.slideshow") + "] " + source.getSourceWithoutPrefix();
            } else {
                sourceString = ResourceSourceType.getWithoutSourcePrefix(source.getSerializationSource());
            }

            this.addLabelCell(Component.literal(sourceString).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt())))
                    .putMemoryValue("source", source.getSerializationSource())
                    .setSelectable(true);

        }

        this.addCellGroupEndSpacerCell().setIgnoreSearch();

    }

    @Override
    protected void initRightSideWidgets() {

        this.addRightSideButton(20, Component.translatable("fancymenu.resources.pre_loading.manage.add"), extendedButton -> {
            ResourceChooserScreen<?,?> s = ResourceChooserScreen.generic(FileTypeGroup.allSupported(), null, source -> {
                if (source != null) {
                    this.cachedSerialized = ResourcePreLoader.addResourceSource(ResourcePreLoader.buildSourceFromString(source), this.cachedSerialized, false);
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });

        this.addRightSideButton(20, Component.translatable("fancymenu.resources.pre_loading.manage.add.panorama"), extendedButton -> {
            ChoosePanoramaScreen s = new ChoosePanoramaScreen(null, panoramaName -> {
                if (panoramaName != null) {
                    this.cachedSerialized = ResourcePreLoader.addResourceSource(ResourcePreLoader.buildSourceFromString(ResourcePreLoader.CUBIC_PANORAMA_SOURCE_PREFIX + panoramaName), this.cachedSerialized, false);
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });

        this.addRightSideButton(20, Component.translatable("fancymenu.resources.pre_loading.manage.add.slideshow"), extendedButton -> {
            ChooseSlideshowScreen s = new ChooseSlideshowScreen(null, slideshowName -> {
                if (slideshowName != null) {
                    this.cachedSerialized = ResourcePreLoader.addResourceSource(ResourcePreLoader.buildSourceFromString(ResourcePreLoader.SLIDESHOW_SOURCE_PREFIX + slideshowName), this.cachedSerialized, false);
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });

        this.addRightSideDefaultSpacer();

        this.addRightSideButton(20, Component.translatable("fancymenu.resources.pre_loading.manage.remove"), extendedButton -> {
            String source = this.getSelectedSource();
            if (source != null) {
                MessageDialogs.openWithCallback(Component.translatable("fancymenu.resources.pre_loading.manage.remove.confirm"), MessageDialogStyle.WARNING, aBoolean -> {
                    if (aBoolean) {
                        this.cachedSerialized = ResourcePreLoader.removeResourceSource(ResourcePreLoader.buildSourceFromString(source), this.cachedSerialized, false);
                    }
                });
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedSource() != null));

    }

    @Nullable
    protected String getSelectedSource() {
        RenderCell cell = this.getSelectedCell();
        if (cell != null) {
            return cell.getMemoryValue("source");
        }
        return null;
    }

    @Override
    protected void onCancel() {
        this.callback.accept(false);
    }

    @Override
    protected void onDone() {
        FancyMenu.getOptions().preLoadResources.setValue(this.cachedSerialized);
        this.callback.accept(true);
    }

}
