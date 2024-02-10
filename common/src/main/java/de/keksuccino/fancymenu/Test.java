package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.resources.ResourceLocation;

public class Test {

    private static final ResourceSupplier<ITexture> IMAGE = ResourceSupplier.image(ResourceSource.of("https://www.alleycat.org/wp-content/uploads/2019/03/FELV-cat.jpg", ResourceSourceType.WEB).getSourceWithPrefix());

    //TODO remove debug
    @EventListener
    public void onRenderScreenPost(RenderScreenEvent.Post e) {

        ITexture tex = IMAGE.get();
        if (tex != null) {
            ResourceLocation loc = tex.getResourceLocation();
            if (loc != null) {

//                RenderingUtils.blitRepeat(e.getGraphics(), loc, 40, 40, 320, 320, 50, 50);

                RenderingUtils.blitNineSliced(e.getGraphics(), loc, 50, 50, 300, 300, tex.getWidth(), tex.getHeight(), 10, 10, 10);

            }
        }

    }

}
