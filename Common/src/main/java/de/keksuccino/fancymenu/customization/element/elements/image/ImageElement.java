package de.keksuccino.fancymenu.customization.element.elements.image;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.fancymenu.resources.texture.ITexture;
import de.keksuccino.fancymenu.resources.texture.TextureHandler;
import de.keksuccino.fancymenu.utils.WebUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.ExternalGifAnimationRenderer;
import de.keksuccino.konkrete.resources.WebTextureResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ImageElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public static final Map<String, WebTextureResourceLocation> CACHED_WEB_IMAGES = new HashMap<>();

    @Nullable
    public String source;
    public SourceMode sourceMode = SourceMode.LOCAL;

    @Nullable
    protected ITexture texture;
    @Nullable
    protected ExternalGifAnimationRenderer gif;
    protected volatile boolean webTextureInitialized = false;
    @Nullable
    protected String lastSource;
    protected SourceMode lastSourceMode;
    protected int originalWidth = 10;
    protected int originalHeight = 10;

    public ImageElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.updateResources();

            int x = this.getX();
            int y = this.getY();

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);

            if (this.gif != null) {
                int w = this.gif.getWidth();
                int h = this.gif.getHeight();
                int x2 = this.gif.getPosX();
                int y2 = this.gif.getPosY();
                this.gif.setPosX(x);
                this.gif.setPosY(y);
                this.gif.setWidth(this.getWidth());
                this.gif.setHeight(this.getHeight());
                this.gif.setOpacity(this.opacity);
                this.gif.render(pose);
                this.gif.setPosX(x2);
                this.gif.setPosY(y2);
                this.gif.setWidth(w);
                this.gif.setHeight(h);
            } else if ((this.texture != null) && this.texture.isReady()) {
                RenderUtils.bindTexture(this.texture.getResourceLocation());
                blit(pose, x, y, 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            } else if (isEditor()) {
                RenderUtils.bindTexture(MISSING);
                blit(pose, x, y, 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();

        }

    }

    protected void updateResources() {

        if ((this.sourceMode != null) && ((this.lastSourceMode == null) || (this.sourceMode != this.lastSourceMode))) {
            this.lastSource = null;
        }
        this.lastSourceMode = this.sourceMode;

        if ((this.source != null) && ((this.lastSource == null) || (!this.lastSource.equals(this.source)))) {
            if (this.sourceMode == SourceMode.LOCAL) {
                File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.source));
                if (f.exists() && f.isFile() && (f.getName().endsWith(".png") || f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg") || f.getName().endsWith(".gif"))) {
                    if (f.getName().endsWith(".gif")) {
                        this.gif = TextureHandler.INSTANCE.getGifTexture(this.source);
                        if (this.gif != null) {
                            this.originalWidth = this.gif.getWidth();
                            this.originalHeight = this.gif.getHeight();
                        }
                    } else {
                        this.texture = TextureHandler.INSTANCE.getTexture(this.source);
                        if (this.texture != null) {
                            this.originalWidth = this.texture.getWidth();
                            this.originalHeight = this.texture.getHeight();
                        }
                    }
                }
                if (isEditor()) {
                    this.restoreAspectRatio();
                }
            } else {
                this.webTextureInitialized = false;
                this.texture = null;
                if (WebUtils.isValidUrl(this.source)) {
                    this.texture = TextureHandler.INSTANCE.getWebTexture(this.source);
                }
            }
        }
        this.lastSource = this.source;

        if ((this.sourceMode == SourceMode.WEB) && (this.texture != null) && this.texture.isReady() && !this.webTextureInitialized) {
            this.originalWidth = this.texture.getWidth();
            this.originalHeight = this.texture.getHeight();
            this.webTextureInitialized = true;
        }

    }

    public void restoreAspectRatio() {
        AspectRatio ratio = new AspectRatio(this.originalWidth, this.originalHeight);
        this.setWidth(ratio.getAspectRatioWidth(this.getHeight()));
    }

    public enum SourceMode {

        LOCAL("local"),
        WEB("web");

        final String name;

        SourceMode(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static SourceMode getByName(String name) {
            for (SourceMode i : SourceMode.values()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

    }

}
