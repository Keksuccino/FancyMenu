package de.keksuccino.fancymenu.util.watermedia;

import com.mojang.blaze3d.textures.GpuTexture;
import de.keksuccino.fancymenu.util.mcef.BrowserFrameTexture;

public class WatermediaFrameTexture extends BrowserFrameTexture {

    public WatermediaFrameTexture(int id) {
        super(id, "FancyMenu WaterMedia frame", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING);
    }

}
