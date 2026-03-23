package de.keksuccino.fancymenu.util.ffmpeg.downloader;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.io.File;

public class FFMPEGDownloaderScreen extends Screen {

    private static final File DOWNLOAD_DIRECTORY = new File(FancyMenu.INSTANCE_DATA_DIR, "ffmpeg_cache");

    protected FFMPEGDownloaderScreen() {
        super(Component.empty());
    }

}
