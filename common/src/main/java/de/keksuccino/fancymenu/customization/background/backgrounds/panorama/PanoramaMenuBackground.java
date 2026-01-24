package de.keksuccino.fancymenu.customization.background.backgrounds.panorama;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class PanoramaMenuBackground extends MenuBackground<PanoramaMenuBackground> {

    private static final ResourceLocation MISSING = TextureManager.INTENTIONAL_MISSING_TEXTURE;

    public final Property.StringProperty panoramaName = putProperty(Property.stringProperty("panorama_name", null, false, false, "fancymenu.backgrounds.panorama.name"));

    protected String lastPanoramaName;
    protected LocalTexturePanoramaRenderer panorama;

    public PanoramaMenuBackground(MenuBackgroundBuilder<PanoramaMenuBackground> builder) {
        super(builder);
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        List<String> panoNames = PanoramaHandler.getPanoramaNames();
        panoNames.addFirst("fancymenu.backgrounds.panorama.name.none");

        this.addCycleContextMenuEntryTo(menu, "panorama_name", panoNames, PanoramaMenuBackground.class, background -> {
            var name = background.panoramaName.get();
            return (name == null) ? "fancymenu.backgrounds.panorama.name.none" : name;
        }, (background, name) -> background.panoramaName.set(name), (menu1, entry, switcherValue) -> {
            Component name;
            if (switcherValue.equals("fancymenu.backgrounds.panorama.name.none")) {
                name = Component.translatable("fancymenu.backgrounds.panorama.name.none").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt()));
            } else {
                name = Component.literal(switcherValue).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()));
            }
            return Component.translatable("fancymenu.backgrounds.panorama.name", name);
        }).setIcon(MaterialIcons.PANORAMA);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        String panoName = this.panoramaName.getString();

        if (panoName == null) {
            this.panorama = null;
        } else {
            if ((this.lastPanoramaName == null) || !this.lastPanoramaName.equals(panoName)) {
                this.panorama = PanoramaHandler.getPanorama(panoName);
            }
            this.lastPanoramaName = panoName;
        }

        if (this.panorama != null) {
            this.panorama.opacity = this.opacity;
            this.panorama.render(graphics, mouseX, mouseY, partial);
            this.panorama.opacity = 1.0F;
        } else {
            RenderSystem.enableBlend();
            graphics.blit(MISSING, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}
