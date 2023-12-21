package de.keksuccino.fancymenu.util.resource.preload;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import net.minecraft.client.Minecraft;
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
        super(Components.translatable("fancymenu.resources.pre_loading.manage"));
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        this.addSpacerCell(10);

        for (ResourceSource source : ResourcePreLoader.getRegisteredResourceSources(this.cachedSerialized)) {

            this.addLabelCell(Components.literal(source.getSourceWithoutPrefix()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt())))
                    .putMemoryValue("source", source.getSerializationSource())
                    .setSelectable(true);

        }

        this.addStartEndSpacerCell();

    }

    @Override
    protected void initRightSideWidgets() {

        this.addRightSideButton(20, Components.translatable("fancymenu.resources.pre_loading.manage.add"), extendedButton -> {
            ResourceChooserScreen<?,?> s = ResourceChooserScreen.generic(FileTypeGroup.allSupported(), null, source -> {
                if (source != null) {
                    this.cachedSerialized = ResourcePreLoader.addResourceSource(ResourceSource.of(source), this.cachedSerialized, false);
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });

        this.addRightSideDefaultSpacer();

        this.addRightSideButton(20, Components.translatable("fancymenu.resources.pre_loading.manage.remove"), extendedButton -> {
            String source = this.getSelectedSource();
            if (source != null) {
                ConfirmationScreen s = ConfirmationScreen.warning(aBoolean -> {
                    if (aBoolean) {
                        this.cachedSerialized = ResourcePreLoader.removeResourceSource(ResourceSource.of(source), this.cachedSerialized, false);
                    }
                    Minecraft.getInstance().setScreen(this);
                }, LocalizationUtils.splitLocalizedLines("fancymenu.resources.pre_loading.manage.remove.confirm"));
                Minecraft.getInstance().setScreen(s);
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
