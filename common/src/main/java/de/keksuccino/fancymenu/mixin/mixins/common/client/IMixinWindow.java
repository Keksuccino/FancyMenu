package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.server.packs.resources.IoSupplier;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

@Mixin(Window.class)
public interface IMixinWindow {

    @Accessor("fullscreen") boolean get_fullscreen_FancyMenu();

    @Accessor("handle") long get_handle_FancyMenu();

    @Accessor("guiScaledWidth") void set_guiScaledWidth_FancyMenu(int width);

    @Accessor("guiScaledHeight") void set_guiScaledHeight_FancyMenu(int height);

    @Invoker("setIcon")
    void invoke_setIcon_FancyMenu(List<IoSupplier<InputStream>> icons) throws IOException;

}
