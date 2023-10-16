package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.texture.ImageResourceHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImageMenuBackground extends MenuBackground {

    private static final DrawableColor BACKGROUND_COLOR = DrawableColor.BLACK;

    public String imagePathOrUrl;
    @NotNull
    public BackgroundImageType type = BackgroundImageType.LOCAL;
    public String webImageFallbackPath;
    public boolean slideLeftRight = false;
    protected double slidePos = 0.0D;
    protected boolean slideMoveBack = false;
    protected boolean slideStop = false;
    protected int slideTick = 0;

    public ImageMenuBackground(MenuBackgroundBuilder<ImageMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        fill(pose, 0, 0, getScreenWidth(), getScreenHeight(), BACKGROUND_COLOR.getColorInt());

        RenderingUtils.resetShaderColor();

        ResourceLocation resourceLocation = null;
        AspectRatio ratio = new AspectRatio(10, 10);
        if (this.imagePathOrUrl != null) {
            String finalImagePathOrUrl = StringUtils.convertFormatCodes(PlaceholderParser.replacePlaceholders(this.imagePathOrUrl), "§", "&");
            ITexture background = (this.type == BackgroundImageType.WEB) ? ImageResourceHandler.INSTANCE.getWebTexture(finalImagePathOrUrl) : ImageResourceHandler.INSTANCE.getTexture(finalImagePathOrUrl);
            if (background != null) {
                ratio = background.getAspectRatio();
                resourceLocation = background.getResourceLocation();
            }
        }
        if ((resourceLocation == null) && (this.type == BackgroundImageType.WEB) && (this.webImageFallbackPath != null)) {
            ITexture fallback = ImageResourceHandler.INSTANCE.getTexture(this.webImageFallbackPath);
            if (fallback != null) {
                ratio = fallback.getAspectRatio();
                resourceLocation = fallback.getResourceLocation();
            }
        }

        if (resourceLocation != null) {

            RenderSystem.enableBlend();
            RenderUtils.bindTexture(resourceLocation);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);

            if (this.slideLeftRight) {
                int w = ratio.getAspectRatioWidth(getScreenHeight());
                //Check if background should move to the left or the right side
                if ((slidePos + (w - getScreenWidth())) <= 0) {
                    slideMoveBack = true;
                }
                if (slidePos >= 0) {
                    slideMoveBack = false;
                }
                //Fix pos after resizing
                if (slidePos + (w - getScreenWidth()) < 0) {
                    slidePos = -(w - getScreenWidth());
                }
                if (slidePos > 0) {
                    slidePos = 0;
                }
                if (!slideStop) {
                    if (slideTick >= 1) {
                        slideTick = 0;
                        if (slideMoveBack) {
                            slidePos = slidePos + 0.5;
                        } else {
                            slidePos = slidePos - 0.5;
                        }

                        if (slidePos + (w - getScreenWidth()) == 0) {
                            slideStop = true;
                        }
                        if (slidePos == 0) {
                            slideStop = true;
                        }
                    } else {
                        slideTick++;
                    }
                } else {
                    if (slideTick >= 300) {
                        slideStop = false;
                        slideTick = 0;
                    } else {
                        slideTick++;
                    }
                }
                if (w <= getScreenWidth()) {
                    blit(pose, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
                } else {
                    RenderUtils.doubleBlit(slidePos, 0, 0.0F, 0.0F, w,getScreenHeight());
                }
            } else if (this.keepBackgroundAspectRatio) {
                int[] size = ratio.getAspectRatioSizeByMinimumSize(getScreenWidth(), getScreenHeight());
                int x = 0;
                if (size[0] > getScreenWidth()) {
                    x = -((size[0] - getScreenWidth()) / 2);
                }
                int y = 0;
                if (size[1] > getScreenHeight()) {
                    y = -((size[1] - getScreenHeight()) / 2);
                }
                blit(pose, x, y, 0.0F, 0.0F, size[0], size[1], size[0], size[1]);
            } else {
                blit(pose, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
            }

        }

        RenderingUtils.resetShaderColor();

    }

    public enum BackgroundImageType implements LocalizedCycleEnum<BackgroundImageType> {

        LOCAL("local"),
        WEB("web");

        private final String name;

        BackgroundImageType(@NotNull String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.background.image.type";
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return WARNING_TEXT_STYLE.get();
        }

        @Override
        public @NotNull BackgroundImageType[] getValues() {
            return BackgroundImageType.values();
        }

        @Override
        public @Nullable BackgroundImageType getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static BackgroundImageType getByName(@NotNull String name) {
            for (BackgroundImageType type : BackgroundImageType.values()) {
                if (type.name.equals(name)) return type;
            }
            return null;
        }

    }

}
