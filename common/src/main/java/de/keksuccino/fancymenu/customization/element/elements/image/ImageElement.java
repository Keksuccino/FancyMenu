package de.keksuccino.fancymenu.customization.element.elements.image;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.texture.ImageResourceHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;

public class ImageElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation MISSING = TextureManager.INTENTIONAL_MISSING_TEXTURE;

    @Nullable
    public String source;
    public SourceMode sourceMode = SourceMode.LOCAL;
    @Nullable
    protected ITexture texture;
    protected boolean webTextureInitialized = false;
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

            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);

            if ((this.texture != null) && this.texture.isReady()) {
                ResourceLocation loc = this.texture.getResourceLocation();
                if (loc != null) {
                    RenderUtils.bindTexture(loc);
                }
                blit(pose, x, y, 0.0F, 0.0F, this.getAbsoluteWidth(), this.getAbsoluteHeight(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
            } else if (isEditor()) {
                RenderUtils.bindTexture(MISSING);
                blit(pose, x, y, 0.0F, 0.0F, this.getAbsoluteWidth(), this.getAbsoluteHeight(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
            }

            RenderingUtils.resetShaderColor();
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
                    this.texture = ImageResourceHandler.INSTANCE.getTexture(this.source);
                    if (this.texture != null) {
                        this.originalWidth = this.texture.getWidth();
                        this.originalHeight = this.texture.getHeight();
                    }
                }
            } else {
                this.webTextureInitialized = false;
                this.texture = null;
                if (TextValidators.BASIC_URL_TEXT_VALIDATOR.get(this.source)) {
                    this.texture = ImageResourceHandler.INSTANCE.getWebTexture(this.source);
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
        this.baseWidth = ratio.getAspectRatioWidth(this.getAbsoluteHeight());
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
