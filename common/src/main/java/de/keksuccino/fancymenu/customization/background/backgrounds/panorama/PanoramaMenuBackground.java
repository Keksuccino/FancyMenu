package de.keksuccino.fancymenu.customization.background.backgrounds.panorama;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.ChoosePanoramaScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

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

        menu.addClickableEntry("panorama_name", Component.empty(), (menu1, entry) -> {
            String currentPanorama = this.panoramaName.get();
            ChoosePanoramaScreen screen = new ChoosePanoramaScreen(currentPanorama, panoramaName -> {
                if (!Objects.equals(this.panoramaName.get(), panoramaName)) {
                    this.saveSnapshot();
                    this.panoramaName.set(panoramaName);
                }
            });
            menu1.closeMenuChain();
            ChoosePanoramaScreen.openInWindow(screen);
        }).setLabelSupplier((menu1, entry) -> Component.translatable("fancymenu.backgrounds.panorama.name", this.buildPanoramaDisplayName(this.panoramaName.get())))
                .setIcon(MaterialIcons.PANORAMA);

        menu.addClickableEntry("clear_panorama", Component.translatable("fancymenu.global_customizations.background_panorama.clear"), (menu1, entry) -> {
            if (this.panoramaName.get() != null) {
                this.saveSnapshot();
                this.panoramaName.set(null);
            }
        }).addIsActiveSupplier((menu1, entry) -> this.panoramaName.get() != null)
                .setIcon(MaterialIcons.DELETE);

    }

    private @NotNull Component buildPanoramaDisplayName(String panoramaName) {
        if (panoramaName == null) {
            return Component.translatable("fancymenu.backgrounds.panorama.name.none")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_color.getColorInt()));
        }
        return Component.literal(panoramaName)
                .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
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
