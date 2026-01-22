package de.keksuccino.fancymenu.util.resource.preload;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.layout.editor.ChoosePanoramaScreen;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseSlideshowScreen;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserWindowBody;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class ManageResourcePreLoadScreen extends PiPCellWindowBody {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

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

            this.addLabelCell(Component.literal(sourceString).setStyle(Style.EMPTY.withColor(this.getLabelColor())))
                    .putMemoryValue("source", source.getSerializationSource())
                    .setSelectable(true);

        }

        this.addCellGroupEndSpacerCell().setIgnoreSearch();

    }

    @Override
    protected void initRightSideWidgets() {

        this.addRightSideButton(20, Component.translatable("fancymenu.resources.pre_loading.manage.add"), extendedButton -> {
            PiPWindow parentWindow = this.getWindow();
            if (parentWindow != null) {
                parentWindow.setVisible(false);
            }
            ResourceChooserWindowBody<?,?> s = ResourceChooserWindowBody.generic(FileTypeGroup.allSupported(), null, source -> {
                if (source != null) {
                    this.cachedSerialized = ResourcePreLoader.addResourceSource(ResourcePreLoader.buildSourceFromString(source), this.cachedSerialized, false);
                }
                this.rebuild();
            });
            PiPWindow chooserWindow = s.openInWindow(parentWindow);
            if (parentWindow != null) {
                chooserWindow.setPosition(parentWindow.getX(), parentWindow.getY());
                chooserWindow.addCloseCallback(() -> parentWindow.setVisible(true));
            }
        });

        this.addRightSideButton(20, Component.translatable("fancymenu.resources.pre_loading.manage.add.panorama"), extendedButton -> {
            PiPWindow parentWindow = this.getWindow();
            if (parentWindow != null) {
                parentWindow.setVisible(false);
            }
            ChoosePanoramaScreen s = new ChoosePanoramaScreen(null, panoramaName -> {
                if (panoramaName != null) {
                    this.cachedSerialized = ResourcePreLoader.addResourceSource(ResourcePreLoader.buildSourceFromString(ResourcePreLoader.CUBIC_PANORAMA_SOURCE_PREFIX + panoramaName), this.cachedSerialized, false);
                }
                this.rebuild();
            });
            PiPWindow chooseWindow = ChoosePanoramaScreen.openInWindow(s, parentWindow);
            if (parentWindow != null) {
                chooseWindow.setPosition(parentWindow.getX(), parentWindow.getY());
                chooseWindow.addCloseCallback(() -> parentWindow.setVisible(true));
            }
        });

        this.addRightSideButton(20, Component.translatable("fancymenu.resources.pre_loading.manage.add.slideshow"), extendedButton -> {
            PiPWindow parentWindow = this.getWindow();
            if (parentWindow != null) {
                parentWindow.setVisible(false);
            }
            ChooseSlideshowScreen s = new ChooseSlideshowScreen(null, slideshowName -> {
                if (slideshowName != null) {
                    this.cachedSerialized = ResourcePreLoader.addResourceSource(ResourcePreLoader.buildSourceFromString(ResourcePreLoader.SLIDESHOW_SOURCE_PREFIX + slideshowName), this.cachedSerialized, false);
                }
                this.rebuild();
            });
            PiPWindow chooseWindow = ChooseSlideshowScreen.openInWindow(s, parentWindow);
            if (parentWindow != null) {
                chooseWindow.setPosition(parentWindow.getX(), parentWindow.getY());
                chooseWindow.addCloseCallback(() -> parentWindow.setVisible(true));
            }
        });

        this.addRightSideDefaultSpacer();

        this.addRightSideButton(20, Component.translatable("fancymenu.resources.pre_loading.manage.remove"), extendedButton -> {
            String source = this.getSelectedSource();
            if (source != null) {
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.resources.pre_loading.manage.remove.confirm"), MessageDialogStyle.WARNING, aBoolean -> {
                    if (aBoolean) {
                        this.cachedSerialized = ResourcePreLoader.removeResourceSource(ResourcePreLoader.buildSourceFromString(source), this.cachedSerialized, false);
                        this.rebuild();
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

    protected int getLabelColor() {
        return UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
    }

    @Override
    protected void onCancel() {
        this.callback.accept(false);
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        FancyMenu.getOptions().preLoadResources.setValue(this.cachedSerialized);
        this.callback.accept(true);
        this.closeWindow();
    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(false);
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageResourcePreLoadScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(false)
                .setForceFocus(false)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageResourcePreLoadScreen screen) {
        return openInWindow(screen, null);
    }

}
