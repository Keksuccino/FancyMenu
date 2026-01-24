package de.keksuccino.fancymenu.customization.background.backgrounds.slideshow;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class SlideshowMenuBackground extends MenuBackground<SlideshowMenuBackground> {

    private static final ResourceLocation MISSING = ITexture.MISSING_TEXTURE_LOCATION;

    public final Property.StringProperty slideshowName = putProperty(Property.stringProperty("slideshow_name", null, false, false, "fancymenu.backgrounds.slideshow.name"));

    protected String lastSlideshowName;
    protected ExternalTextureSlideshowRenderer slideshow;

    public SlideshowMenuBackground(MenuBackgroundBuilder<SlideshowMenuBackground> builder) {
        super(builder);
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        List<String> slideshowNames = SlideshowHandler.getSlideshowNames();
        slideshowNames.addFirst("fancymenu.backgrounds.slideshow.name.none");

        this.addCycleContextMenuEntryTo(menu, "slideshow_name", slideshowNames, SlideshowMenuBackground.class, background -> {
            var name = background.slideshowName.get();
            return (name == null) ? "fancymenu.backgrounds.slideshow.name.none" : name;
        }, (background, name) -> background.slideshowName.set(name), (menu1, entry, switcherValue) -> {
            Component name;
            if (switcherValue.equals("fancymenu.backgrounds.slideshow.name.none")) {
                name = Component.translatable("fancymenu.backgrounds.slideshow.name.none").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt()));
            } else {
                name = Component.literal(switcherValue).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()));
            }
            return Component.translatable("fancymenu.backgrounds.slideshow.name", name);
        }).setIcon(MaterialIcons.SLIDESHOW);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        String slideshowName = this.slideshowName.getString();

        if (slideshowName != null) {
            if ((this.lastSlideshowName == null) || !this.lastSlideshowName.equals(slideshowName)) {
                this.slideshow = SlideshowHandler.getSlideshow(slideshowName);
            }
            this.lastSlideshowName = slideshowName;
        } else {
            this.slideshow = null;
        }

        if (this.slideshow != null) {

            if (!this.slideshow.isReady()) {
                this.slideshow.prepareSlideshow();
            }

            int imageWidth = this.slideshow.getImageWidth();
            int imageHeight = this.slideshow.getImageHeight();

            if (!this.keepBackgroundAspectRatio) {
                this.slideshow.x = 0;
                this.slideshow.y = 0;
                this.slideshow.width = getScreenWidth();
                this.slideshow.height = getScreenHeight();
            } else {
                AspectRatio ratio = new AspectRatio(imageWidth, imageHeight);
                int[] size = ratio.getAspectRatioSizeByMinimumSize(getScreenWidth(), getScreenHeight());
                int x = 0;
                if (size[0] > getScreenWidth()) {
                    x = -((size[0] - getScreenWidth()) / 2);
                }
                int y = 0;
                if (size[1] > getScreenHeight()) {
                    y = -((size[1] - getScreenHeight()) / 2);
                }
                this.slideshow.width = size[0];
                this.slideshow.height = size[1];
                this.slideshow.x = x;
                this.slideshow.y = y;
            }

            this.slideshow.slideshowOpacity = this.opacity;

            this.slideshow.render(graphics);

            this.slideshow.slideshowOpacity = 1.0F;

        } else {
            RenderSystem.enableBlend();
            graphics.blit(MISSING, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}
