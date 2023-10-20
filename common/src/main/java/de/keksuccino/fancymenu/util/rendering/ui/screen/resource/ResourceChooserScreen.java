package de.keksuccino.fancymenu.util.rendering.ui.screen.resource;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfiguratorScreen;
import de.keksuccino.fancymenu.util.resources.Resource;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ResourceChooserScreen<T extends Resource> extends ConfiguratorScreen {

    //TODO give screen list with available FileTypes (or FileTypeGroup)

    protected ResourceChooserScreen(@NotNull Component title) {
        super(title);
    }

    @Override
    protected void initCells() {

        //TODO Label: "Source"

        //TODO Text Field for source input (lock when set to Local source, so you have to use the File Chooser)

        //TODO Cycle Button: Modes: LOCATION, LOCAL, WEB (filter available modes by allowed source types of FileTypes)

    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {

        super.render(pose, mouseX, mouseY, partial);

        //TODO render preview on the right, if source allows it

    }

    @Override
    protected void onCancel() {
    }

    @Override
    protected void onDone() {
    }

}
