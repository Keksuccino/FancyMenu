package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.model.PlayerEntityElementRenderer;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.model.PlayerEntityProperties;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.textures.CapeWebTextureResourceLocation;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.textures.SkinExternalTextureResourceLocation;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.textures.SkinWebTextureResourceLocation;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.PlayerUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import java.io.File;

public class PlayerEntityElement extends AbstractElement {
    
    private static final Logger LOGGER = LogManager.getLogger();

    public PlayerEntityElementRenderer normalRenderer = new PlayerEntityElementRenderer(false);
    public PlayerEntityElementRenderer slimRenderer = new PlayerEntityElementRenderer(true);

    public volatile boolean copyClientPlayer = false;
    public volatile String playerName = "Steve";
    public boolean showPlayerName = true;
    public boolean hasParrotOnShoulder = false;
    public boolean parrotOnLeftShoulder = false;
    public boolean crouching = false;
    public boolean isBaby = false;
    public String scale = "30";
    public boolean headFollowsMouse = true;
    public boolean bodyFollowsMouse = true;
    public volatile boolean slim = false;
    public volatile boolean autoSkin = false;
    public volatile boolean autoCape = false;
    public volatile String skinUrl;
    protected volatile String oldSkinUrl = null;
    public volatile String skinPath;
    public volatile String capeUrl;
    protected volatile String oldCapeUrl = null;
    public volatile String capePath;
    protected volatile ResourceLocation currentSkinLocation = null;
    protected volatile ResourceLocation currentCapeLocation = null;
    public String bodyXRot;
    public String bodyYRot;
    public String headXRot;
    public String headYRot;
    public String headZRot;
    public String leftArmXRot;
    public String leftArmYRot;
    public String leftArmZRot;
    public String rightArmXRot;
    public String rightArmYRot;
    public String rightArmZRot;
    public String leftLegXRot;
    public String leftLegYRot;
    public String leftLegZRot;
    public String rightLegXRot;
    public String rightLegYRot;
    public String rightLegZRot;
    public boolean bodyXRotAdvancedMode;
    public boolean bodyYRotAdvancedMode;
    public boolean headXRotAdvancedMode;
    public boolean headYRotAdvancedMode;
    public boolean headZRotAdvancedMode;
    public boolean leftArmXRotAdvancedMode;
    public boolean leftArmYRotAdvancedMode;
    public boolean leftArmZRotAdvancedMode;
    public boolean rightArmXRotAdvancedMode;
    public boolean rightArmYRotAdvancedMode;
    public boolean rightArmZRotAdvancedMode;
    public boolean leftLegXRotAdvancedMode;
    public boolean leftLegYRotAdvancedMode;
    public boolean leftLegZRotAdvancedMode;
    public boolean rightLegXRotAdvancedMode;
    public boolean rightLegYRotAdvancedMode;
    public boolean rightLegZRotAdvancedMode;

    public PlayerEntityElement(@NotNull ElementBuilder<PlayerEntityElement, PlayerEntityEditorElement> builder) {
        super(builder);
        if (isEditor()) {
            PlayerEntityElementBuilder.ELEMENT_CACHE.clear();
        } else {
            PlayerEntityElementBuilder.ELEMENT_CACHE.put(this.getInstanceIdentifier(), this);
        }
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            //Update placeholders of skin URL
            if (this.skinUrl != null) {
                String currentSkinUrl = StringUtils.convertFormatCodes(PlaceholderParser.replacePlaceholders(this.skinUrl), "§", "&");
                if (!currentSkinUrl.equals(this.oldSkinUrl)) {
                    this.oldSkinUrl = currentSkinUrl;
                    this.setSkinTextureBySource(this.skinUrl, true);
                }
            }
            //Update placeholders of cape URL
            if (this.capeUrl != null) {
                String currentCapeUrl = StringUtils.convertFormatCodes(PlaceholderParser.replacePlaceholders(this.capeUrl), "§", "&");
                if (!currentCapeUrl.equals(this.oldCapeUrl)) {
                    this.oldCapeUrl = currentCapeUrl;
                    this.setCapeTextureBySource(this.capeUrl, true);
                }
            }
            //Update placeholders in player name
            this.setPlayerName(this.playerName, false);

            float scale = this.stringToFloat(this.scale);
            if (scale == 0.0F) scale = 30;

            //Update element size based on entity size
            this.baseWidth = (int)(this.getActiveEntityProperties().getDimensions().width * scale);
            this.baseHeight = (int)(this.getActiveEntityProperties().getDimensions().height * scale);

            RenderSystem.enableBlend();

            PlayerEntityProperties props = this.getActiveEntityProperties();
            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int mouseOffsetX = this.baseWidth / 2;
            int mouseOffsetY = (this.baseHeight / 4) / 2;
            if (props.isBaby) mouseOffsetY += (this.baseHeight / 2) - mouseOffsetY; //not exactly the same eye pos as for adult size, but good enough
            this.renderPlayerEntity(x, y, (int)scale, (float)x - mouseX + mouseOffsetX, (float)y - mouseY + mouseOffsetY, props);

            RenderingUtils.resetShaderColor();

        }

    }

    protected void renderPlayerEntity(int posX, int posY, int scale, float angleXComponent, float angleYComponent, PlayerEntityProperties props) {
        float f = (float)Math.atan(angleXComponent / 40.0F);
        float f1 = (float)Math.atan(angleYComponent / 40.0F);
        innerRenderPlayerEntity(posX, posY, scale, f, f1, props, this.getActiveRenderer());
    }

    @SuppressWarnings("all")
    protected void innerRenderPlayerEntity(int posX, int posY, int scale, float angleXComponent, float angleYComponent, PlayerEntityProperties props, PlayerEntityElementRenderer renderer) {

        float bodyXRot = this.stringToFloat(this.bodyXRot);
        float bodyYRot = this.stringToFloat(this.bodyYRot);
        float headXRot = this.stringToFloat(this.headXRot);
        float headYRot = this.stringToFloat(this.headYRot);
        float headZRot = this.stringToFloat(this.headZRot);
        float leftArmXRot = this.stringToFloat(this.leftArmXRot);
        float leftArmYRot = this.stringToFloat(this.leftArmYRot);
        float leftArmZRot = this.stringToFloat(this.leftArmZRot);
        float rightArmXRot = this.stringToFloat(this.rightArmXRot);
        float rightArmYRot = this.stringToFloat(this.rightArmYRot);
        float rightArmZRot = this.stringToFloat(this.rightArmZRot);
        float leftLegXRot = this.stringToFloat(this.leftLegXRot);
        float leftLegYRot = this.stringToFloat(this.leftLegYRot);
        float leftLegZRot = this.stringToFloat(this.leftLegZRot);
        float rightLegXRot = this.stringToFloat(this.rightLegXRot);
        float rightLegYRot = this.stringToFloat(this.rightLegYRot);
        float rightLegZRot = this.stringToFloat(this.rightLegZRot);

        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate((posX+((props.getDimensions().width / 2) * scale)), (posY+(props.getDimensions().height * scale)), 1050.0F);
        modelViewStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        PoseStack innerPoseStack = new PoseStack();
        innerPoseStack.pushPose();
        innerPoseStack.translate(0.0F, 0.0F, 1000.0F);
        innerPoseStack.scale((float)scale, (float)scale, (float)scale);

        Quaternionf quat1 = this.bodyFollowsMouse ? new Quaternionf().rotateZ((float)Math.PI) : Axis.ZP.rotationDegrees(180.0F);
        Quaternionf quat2 = this.bodyFollowsMouse ? new Quaternionf().rotateX(angleYComponent * 20.0F * ((float)Math.PI / 180F)) : Axis.XP.rotationDegrees(bodyYRot);
        quat1.mul(quat2);
        innerPoseStack.mulPose(quat1);

        //Apply follow-mouse values by default
        props.yBodyRot = 180.0F + angleXComponent * 20.0F;
        props.yRot = 180.0F + angleXComponent * 40.0F;
        props.xRot = -angleYComponent * 20.0F;
        props.yHeadRot = 180.0F + angleXComponent * 40.0F;
        props.yHeadRotO = 180.0F + angleXComponent * 40.0F;
        props.headZRot = 0;

        if (!this.bodyFollowsMouse) {
            props.yBodyRot = 180.0F + bodyXRot;
        }
        if (!this.headFollowsMouse) {
            props.xRot = headYRot;
            props.yRot = 0;
            props.yHeadRot = 180.0F + headXRot;
            props.yHeadRotO = 180.0F + headXRot;
            props.headZRot = headZRot;
        }
        props.leftArmXRot = leftArmXRot;
        props.leftArmYRot = leftArmYRot;
        props.leftArmZRot = leftArmZRot;
        props.rightArmXRot = rightArmXRot;
        props.rightArmYRot = rightArmYRot;
        props.rightArmZRot = rightArmZRot;
        props.leftLegXRot = leftLegXRot;
        props.leftLegYRot = leftLegYRot;
        props.leftLegZRot = leftLegZRot;
        props.rightLegXRot = rightLegXRot;
        props.rightLegYRot = rightLegYRot;
        props.rightLegZRot = rightLegZRot;

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quat2.conjugate();
        dispatcher.overrideCameraOrientation(quat2);
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            renderer.renderPlayerEntityItem(0.0D, 0.0D, 0.0D, 0.0F, 1.0F, innerPoseStack, bufferSource, 15728880);
        });
        bufferSource.endBatch();
        dispatcher.setRenderShadow(true);
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        innerPoseStack.popPose();
    }

    protected float stringToFloat(@Nullable String s) {
        if (s == null) return 0.0F;
        s = PlaceholderParser.replacePlaceholders(s);
        s = s.replace(" ", "");
        try {
            return Float.parseFloat(s);
        } catch (Exception ignore) {}
        return 0.0F;
    }

    public void setCopyClientPlayer(boolean copyClientPlayer) {
        if (copyClientPlayer) {
            this.copyClientPlayer = true;
            this.autoCape = false;
            this.autoSkin = false;
            this.slim = false;
            this.setPlayerName(Minecraft.getInstance().getUser().getName(), true);
            this.setSkinByPlayerName();
            this.setCapeByPlayerName();
        } else {
            this.copyClientPlayer = false;
            this.setSkinTextureBySource(null, false);
        }
    }

    public void setPlayerName(String playerName, boolean updatePlayerNameField) {
        if (playerName == null) {
            playerName = "Steve";
        }
        if (updatePlayerNameField) {
            this.playerName = playerName;
        }
        playerName = PlaceholderParser.replacePlaceholders(playerName);
        this.normalRenderer.properties.displayName = Component.literal(playerName);
        this.slimRenderer.properties.displayName = Component.literal(playerName);
    }

    public void setShowPlayerName(boolean showName) {
        this.showPlayerName = showName;
        this.normalRenderer.properties.showDisplayName = showName;
        this.slimRenderer.properties.showDisplayName = showName;
    }

    public void setHasParrotOnShoulder(boolean hasParrot, boolean onLeftShoulder) {
        this.hasParrotOnShoulder = hasParrot;
        this.parrotOnLeftShoulder = onLeftShoulder;
        this.normalRenderer.properties.hasParrotOnShoulder = hasParrot;
        this.slimRenderer.properties.hasParrotOnShoulder = hasParrot;
        this.normalRenderer.properties.parrotOnLeftShoulder = onLeftShoulder;
        this.slimRenderer.properties.parrotOnLeftShoulder = onLeftShoulder;
    }

    public void setCrouching(boolean crouching) {
        this.crouching = crouching;
        this.normalRenderer.properties.crouching = crouching;
        this.slimRenderer.properties.crouching = crouching;
    }

    public void setIsBaby(boolean isBaby) {
        this.isBaby = isBaby;
        this.normalRenderer.properties.isBaby = isBaby;
        this.slimRenderer.properties.isBaby = isBaby;
    }

    public void setCapeByPlayerName() {
        PlayerEntityElement cachedInstance = PlayerEntityElementBuilder.ELEMENT_CACHE.get(this.getInstanceIdentifier());
        if ((cachedInstance != null) && (cachedInstance.currentCapeLocation != null)) {
            this.setCapeTextureLocation(cachedInstance.currentCapeLocation);
            return;
        }
        new Thread(() -> {
            try {
                if (this.playerName != null) {
                    String playerCapeUrl = PlayerUtils.getCapeURL(this.playerName);
                    if (playerCapeUrl != null) {
                        this.setCapeTextureBySource(playerCapeUrl, true);
                    } else {
                        this.setCapeTextureBySource(null, false);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void setSkinByPlayerName() {
        PlayerEntityElement cachedInstance = PlayerEntityElementBuilder.ELEMENT_CACHE.get(this.getInstanceIdentifier());
        if ((cachedInstance != null) && (cachedInstance.currentSkinLocation != null)) {
            this.setSkinTextureLocation(cachedInstance.currentSkinLocation);
            return;
        }
        new Thread(() -> {
            try {
                if (this.playerName != null) {
                    String playerSkinUrl = PlayerUtils.getSkinURL(this.playerName);
                    if (playerSkinUrl != null) {
                        //Set skin
                        this.setSkinTextureBySource(playerSkinUrl, true);
                        //Set slim
                        if (!PlayerEntityElementCache.isSlimSkinInfoCached(playerName)) {
                            this.slim = PlayerUtils.hasSlimSkin(this.playerName);
                            PlayerEntityElementCache.cacheIsSlimSkin(this.playerName, this.slim);
                        } else {
                            this.slim = PlayerEntityElementCache.getIsSlimSkin(this.playerName);
                        }
                    } else {
                        this.setSkinTextureBySource(null, false);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void setSkinTextureBySource(String sourcePathOrLink, boolean web) {
        PlayerEntityElement cachedInstance = PlayerEntityElementBuilder.ELEMENT_CACHE.get(this.getInstanceIdentifier());
        if ((cachedInstance != null) && (cachedInstance.currentSkinLocation != null)) {
            this.setSkinTextureLocation(cachedInstance.currentSkinLocation);
            return;
        }
        if (sourcePathOrLink != null) {
            if (web) {
                new Thread(() -> {
                    try {
                        String url = StringUtils.convertFormatCodes(PlaceholderParser.replacePlaceholders(sourcePathOrLink), "§", "&");
                        this.skinUrl = sourcePathOrLink;
                        this.oldSkinUrl = url;
                        this.skinPath = null;
                        String sha1 = PlayerEntityElementCache.calculateWebSourceSHA1(url);
                        if (sha1 != null) {
                            
                            if (!PlayerEntityElementCache.isSkinCached(sha1)) {
                                SkinWebTextureResourceLocation sr = new SkinWebTextureResourceLocation(url);
                                sr.downloadTexture();
                                if (sr.getDownloadedTexture() == null) {
                                    return;
                                }
                                MainThreadTaskExecutor.executeInMainThread(sr::loadTexture, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                                long start = System.currentTimeMillis();
                                while (sr.getResourceLocation() == null) {
                                    long now = System.currentTimeMillis();
                                    if ((start + 15000) <= now) {
                                        LOGGER.error("[FANCYMENU] Failed to load web skin texture for Player Entity element!");
                                        return;
                                    }
                                    Thread.sleep(100);
                                }
                                PlayerEntityElementCache.cacheSkin(sha1, sr.getResourceLocation());
                                this.setSkinTextureLocation(sr.getResourceLocation());
                            } else {
                                this.setSkinTextureLocation(PlayerEntityElementCache.getSkin(sha1));
                            }
                            
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                new Thread(() -> {
                    try {
                        String path = ScreenCustomization.getAbsoluteGameDirectoryPath(sourcePathOrLink);
                        File f = new File(path);
                        if (f.isFile() && (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png"))) {
                            String sha1 = PlayerEntityElementCache.calculateSHA1(f);
                            if (sha1 != null) {
                                
                                if (!PlayerEntityElementCache.isSkinCached(sha1)) {
                                    SkinExternalTextureResourceLocation sr = new SkinExternalTextureResourceLocation(path);
                                    MainThreadTaskExecutor.executeInMainThread(sr::loadTexture, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                                    long start = System.currentTimeMillis();
                                    while (sr.getResourceLocation() == null) {
                                        long now = System.currentTimeMillis();
                                        if ((start + 15000) <= now) {
                                            LOGGER.error("[FANCYMENU] Failed to load local skin texture for Player Entity element!");
                                            return;
                                        }
                                        Thread.sleep(100);
                                    }
                                    PlayerEntityElementCache.cacheSkin(sha1, sr.getResourceLocation());
                                    this.setSkinTextureLocation(sr.getResourceLocation());
                                } else {
                                    this.setSkinTextureLocation(PlayerEntityElementCache.getSkin(sha1));
                                }
                                
                                this.skinUrl = null;
                                this.skinPath = sourcePathOrLink;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } else {
            this.setSkinTextureLocation(null);
            this.skinUrl = null;
            this.skinPath = null;
            this.oldSkinUrl = null;
        }
    }

    public void setCapeTextureBySource(String sourcePathOrLink, boolean web) {
        PlayerEntityElement cachedInstance = PlayerEntityElementBuilder.ELEMENT_CACHE.get(this.getInstanceIdentifier());
        if ((cachedInstance != null) && (cachedInstance.currentCapeLocation != null)) {
            this.setCapeTextureLocation(cachedInstance.currentCapeLocation);
            return;
        }
        if (sourcePathOrLink != null) {
            if (web) {
                new Thread(() -> {
                    try {
                        String url = StringUtils.convertFormatCodes(PlaceholderParser.replacePlaceholders(sourcePathOrLink), "§", "&");
                        this.capeUrl = sourcePathOrLink;
                        this.oldCapeUrl = url;
                        this.capePath = null;
                        String sha1 = PlayerEntityElementCache.calculateWebSourceSHA1(url);
                        if (sha1 != null) {
                            
                            if (!PlayerEntityElementCache.isCapeCached(sha1)) {
                                CapeWebTextureResourceLocation sr = new CapeWebTextureResourceLocation(url);
                                sr.downloadTexture();
                                if (sr.getDownloadedTexture() == null) {
                                    return;
                                }
                                MainThreadTaskExecutor.executeInMainThread(sr::loadTexture, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                                long start = System.currentTimeMillis();
                                while (sr.getResourceLocation() == null) {
                                    long now = System.currentTimeMillis();
                                    if ((start + 15000) <= now) {
                                        LOGGER.error("[FANCYMENU] Failed to load web cape texture for Player Entity element!");
                                        return;
                                    }
                                    Thread.sleep(100);
                                }
                                PlayerEntityElementCache.cacheCape(sha1, sr.getResourceLocation());
                                this.setCapeTextureLocation(sr.getResourceLocation());
                            } else {
                                this.setCapeTextureLocation(PlayerEntityElementCache.getCape(sha1));
                            }
                            
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                new Thread(() -> {
                    try {
                        String path = ScreenCustomization.getAbsoluteGameDirectoryPath(sourcePathOrLink);
                        File f = new File(path);
                        if (f.isFile() && (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png"))) {
                            String sha1 = PlayerEntityElementCache.calculateSHA1(f);
                            
                            if (sha1 != null) {
                                if (!PlayerEntityElementCache.isCapeCached(sha1)) {
                                    ExternalTextureResourceLocation er = new ExternalTextureResourceLocation(path);
                                    MainThreadTaskExecutor.executeInMainThread(er::loadTexture, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                                    long start = System.currentTimeMillis();
                                    while (er.getResourceLocation() == null) {
                                        long now = System.currentTimeMillis();
                                        if ((start + 15000) <= now) {
                                            LOGGER.error("[FANCYMENU] Failed to load local cape texture for Player Entity element!");
                                            return;
                                        }
                                        Thread.sleep(100);
                                    }
                                    PlayerEntityElementCache.cacheCape(sha1, er.getResourceLocation());
                                    this.setCapeTextureLocation(er.getResourceLocation());
                                } else {
                                    this.setCapeTextureLocation(PlayerEntityElementCache.getCape(sha1));
                                }
                                this.capeUrl = null;
                                this.capePath = sourcePathOrLink;
                            }
                            
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } else {
            this.setCapeTextureLocation(null);
            this.capeUrl = null;
            this.capePath = null;
            this.oldCapeUrl = null;
        }
    }

    protected void setSkinTextureLocation(ResourceLocation loc) {
        
        this.currentSkinLocation = loc;
        this.normalRenderer.properties.setSkinTextureLocation(loc);
        this.slimRenderer.properties.setSkinTextureLocation(loc);
    }

    protected void setCapeTextureLocation(ResourceLocation loc) {
        
        this.currentCapeLocation = loc;
        this.normalRenderer.properties.setCapeTextureLocation(loc);
        this.slimRenderer.properties.setCapeTextureLocation(loc);
    }

    public PlayerEntityElementRenderer getActiveRenderer() {
        if (this.slim) {
            return this.slimRenderer;
        }
        return this.normalRenderer;
    }

    public PlayerEntityProperties getActiveEntityProperties() {
        return this.getActiveRenderer().properties;
    }

}
