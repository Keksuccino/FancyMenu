package de.keksuccino.fancymenu.customization.item.v2.items.playerentity;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.customization.MenuCustomization;
import de.keksuccino.fancymenu.customization.customizationgui.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.item.v2.items.playerentity.render.PlayerEntityItemRenderer;
import de.keksuccino.fancymenu.customization.item.v2.items.playerentity.render.PlayerEntityProperties;
import de.keksuccino.fancymenu.customization.placeholder.v2.PlaceholderParser;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Quaternionf;

import java.io.File;

public class PlayerEntityCustomizationItem extends CustomizationItem {

    
    private static final Logger LOGGER = LogManager.getLogger();

    public PlayerEntityItemRenderer normalRenderer = new PlayerEntityItemRenderer(false);
    public PlayerEntityItemRenderer slimRenderer = new PlayerEntityItemRenderer(true);

    public volatile boolean copyClientPlayer = false;
    public volatile String playerName = "Steve";
    public boolean showPlayerName = true;
    public boolean hasParrotOnShoulder = false;
    public boolean parrotOnLeftShoulder = false;
    public boolean crouching = false;
    public boolean isBaby = false;
    public int scale = 30;

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
    

    public boolean followMouse = true;
    public float bodyRotationX;
    public float bodyRotationY;
    public float headRotationX;
    public float headRotationY;

    public PlayerEntityCustomizationItem(CustomizationItemContainer parentContainer, PropertiesSection item) {

        super(parentContainer, item);

        
        if (isEditorActive()) {
            PlayerEntityCustomizationItemContainer.ELEMENT_CACHE.clear();
        }

        String copyClientPlayerString = item.getEntryValue("copy_client_player");
        if ((copyClientPlayerString != null) && copyClientPlayerString.equals("true")) {
            this.setCopyClientPlayer(true);
        }

        if (!this.copyClientPlayer) {

            String playerNameString = item.getEntryValue("playername");
            if (playerNameString != null) {
                this.setPlayerName(playerNameString, true);
            }

            String autoSkinString = item.getEntryValue("auto_skin");
            if ((autoSkinString != null) && autoSkinString.equalsIgnoreCase("true")) {
                this.autoSkin = true;
            }

            String autoCapeString = item.getEntryValue("auto_cape");
            if ((autoCapeString != null) && autoCapeString.equalsIgnoreCase("true")) {
                this.autoCape = true;
            }

            String slim = item.getEntryValue("slim");
            if (slim != null) {
                if (slim.replace(" ", "").equalsIgnoreCase("true")) {
                    this.slim = true;
                }
            }

            if (!this.autoSkin) {
                this.skinUrl = item.getEntryValue("skinurl");
                if (this.skinUrl != null) {
                    this.setSkinTextureBySource(this.skinUrl, true);
                }
                this.skinPath = fixBackslashPath(item.getEntryValue("skinpath"));
                if ((this.skinPath != null) && (this.skinUrl == null)) {
                    this.setSkinTextureBySource(this.skinPath, false);
                }
            } else {
                this.setSkinByPlayerName();
            }

            if (!this.autoCape) {
                this.capeUrl = item.getEntryValue("capeurl");
                if (this.capeUrl != null) {
                    this.setCapeTextureBySource(this.capeUrl, true);
                }
                this.capePath = fixBackslashPath(item.getEntryValue("capepath"));
                if ((this.capePath != null) && (this.capeUrl == null)) {
                    this.setCapeTextureBySource(this.capePath, false);
                }
            } else {
                this.setCapeByPlayerName();
            }

        }

        String scaleString = item.getEntryValue("scale");
        if ((scaleString != null) && MathUtils.isDouble(scaleString)) {
            this.scale = (int) Double.parseDouble(scaleString);
        }

        String hasParrotString = item.getEntryValue("parrot");
        if (hasParrotString != null) {
            if (hasParrotString.replace(" ", "").equalsIgnoreCase("true")) {
                this.setHasParrotOnShoulder(true, false);
            }
        }

        String parrotLeftShoulderString = item.getEntryValue("parrot_left_shoulder");
        if (parrotLeftShoulderString != null) {
            if (parrotLeftShoulderString.replace(" ", "").equalsIgnoreCase("true")) {
                this.setHasParrotOnShoulder(this.hasParrotOnShoulder, true);
            }
        }

        String isBabyString = item.getEntryValue("is_baby");
        if (isBabyString != null) {
            if (isBabyString.replace(" ", "").equalsIgnoreCase("true")) {
                this.setIsBaby(true);
            }
        }

        String crouching = item.getEntryValue("crouching");
        if (crouching != null) {
            if (crouching.replace(" ", "").equalsIgnoreCase("true")) {
                this.setCrouching(true);
            }
        }

        String showName = item.getEntryValue("showname");
        if (showName != null) {
            if (showName.replace(" ", "").equalsIgnoreCase("false")) {
                this.setShowPlayerName(false);
            }
        }

        String followMouseString = item.getEntryValue("follow_mouse");
        if (followMouseString != null) {
            if (followMouseString.replace(" ", "").equalsIgnoreCase("false")) {
                this.followMouse = false;
            }
        }

        String rotX = item.getEntryValue("headrotationx");
        if (rotX != null) {
            rotX = rotX.replace(" ", "");
            if (MathUtils.isFloat(rotX)) {
                this.headRotationX = Float.parseFloat(rotX);
            }
        }

        String rotY = item.getEntryValue("headrotationy");
        if (rotY != null) {
            rotY = rotY.replace(" ", "");
            if (MathUtils.isFloat(rotY)) {
                this.headRotationY = Float.parseFloat(rotY);
            }
        }

        String bodyrotX = item.getEntryValue("bodyrotationx");
        if (bodyrotX != null) {
            bodyrotX = bodyrotX.replace(" ", "");
            if (MathUtils.isFloat(bodyrotX)) {
                this.bodyRotationX = Float.parseFloat(bodyrotX);
            }
        }

        String bodyrotY = item.getEntryValue("bodyrotationy");
        if (bodyrotY != null) {
            bodyrotY = bodyrotY.replace(" ", "");
            if (MathUtils.isFloat(bodyrotY)) {
                this.bodyRotationY = Float.parseFloat(bodyrotY);
            }
        }

        
        if (!isEditorActive()) {
            PlayerEntityCustomizationItemContainer.ELEMENT_CACHE.put(this.actionId, this);
        }

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
        
        PlayerEntityCustomizationItem cachedInstance = PlayerEntityCustomizationItemContainer.ELEMENT_CACHE.get(this.actionId);
        if ((cachedInstance != null) && (cachedInstance.currentCapeLocation != null)) {
            this.setCapeTextureLocation(cachedInstance.currentCapeLocation);
            return;
        }
        
        new Thread(() -> {
            try {
                if (this.playerName != null) {
                    String playerCapeUrl = PlayerEntityUtils.getCapeURL(this.playerName);
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
        
        PlayerEntityCustomizationItem cachedInstance = PlayerEntityCustomizationItemContainer.ELEMENT_CACHE.get(this.actionId);
        if ((cachedInstance != null) && (cachedInstance.currentSkinLocation != null)) {
            this.setSkinTextureLocation(cachedInstance.currentSkinLocation);
            return;
        }
        
        new Thread(() -> {
            try {
                if (this.playerName != null) {
                    String playerSkinUrl = PlayerEntityUtils.getSkinURL(this.playerName);
                    if (playerSkinUrl != null) {
                        //Set skin
                        this.setSkinTextureBySource(playerSkinUrl, true);
                        //Set slim
                        if (!PlayerEntityElementCache.isSlimSkinInfoCached(playerName)) {
                            this.slim = PlayerEntityUtils.hasSlimSkin(this.playerName);
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
        
        PlayerEntityCustomizationItem cachedInstance = PlayerEntityCustomizationItemContainer.ELEMENT_CACHE.get(this.actionId);
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
                                CustomizationOverlay.runTaskInMainThread(() -> sr.loadTexture());
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
                        String path = MenuCustomization.getAbsoluteGameDirectoryPath(sourcePathOrLink);
                        File f = new File(path);
                        if (f.isFile() && (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png"))) {
                            String sha1 = PlayerEntityElementCache.calculateSHA1(f);
                            if (sha1 != null) {
                                
                                if (!PlayerEntityElementCache.isSkinCached(sha1)) {
                                    SkinExternalTextureResourceLocation sr = new SkinExternalTextureResourceLocation(path);
                                    CustomizationOverlay.runTaskInMainThread(() -> sr.loadTexture());
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
        
        PlayerEntityCustomizationItem cachedInstance = PlayerEntityCustomizationItemContainer.ELEMENT_CACHE.get(this.actionId);
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
                                CustomizationOverlay.runTaskInMainThread(() -> sr.loadTexture());
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
                        String path = MenuCustomization.getAbsoluteGameDirectoryPath(sourcePathOrLink);
                        File f = new File(path);
                        if (f.isFile() && (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png"))) {
                            String sha1 = PlayerEntityElementCache.calculateSHA1(f);
                            
                            if (sha1 != null) {
                                if (!PlayerEntityElementCache.isCapeCached(sha1)) {
                                    ExternalTextureResourceLocation er = new ExternalTextureResourceLocation(path);
                                    CustomizationOverlay.runTaskInMainThread(() -> er.loadTexture());
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

    
//    @Nullable
//    protected ResourceLocation getResourceLocationOfWebResource(String url) {
//        try {
//            WebTextureResourceLocation wt = TextureHandler.getWebResource(url, false);
//            if (wt != null) {
//                if (!wt.isReady()) {
//                    wt.loadTexture();
//                }
//                ResourceLocation loc = wt.getResourceLocation();
//                if (loc != null) {
//                    return loc;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public PlayerEntityItemRenderer getActiveRenderer() {
        if (this.slim) {
            return this.slimRenderer;
        }
        return this.normalRenderer;
    }

    public PlayerEntityProperties getActiveEntityProperties() {
        return this.getActiveRenderer().properties;
    }

    @Override
    public void render(PoseStack matrix, Screen menu) {

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

            //Update element size based on entity size
            this.setWidth((int)(this.getActiveEntityProperties().getDimensions().width*this.scale));
            this.setHeight((int)(this.getActiveEntityProperties().getDimensions().height*this.scale));

            //Update dummy value for layout editor
            if (this.playerName != null) {
                this.value = this.playerName;
            } else {
                this.value = "Player Entity";
            }

            RenderSystem.enableBlend();
            int x = this.getPosX(menu);
            int y = this.getPosY(menu);
            this.renderPlayerEntity(x, y, this.scale, (float)x - MouseInput.getMouseX(), (float)(y - 50) - MouseInput.getMouseY());

        }

    }

    protected void renderPlayerEntity(int i11, int i12, int i13, float angleXComponent, float angleYComponent) {
        float f = (float)Math.atan(angleXComponent / 40.0F);
        float f1 = (float)Math.atan(angleYComponent / 40.0F);
        innerRenderPlayerEntity(i11, i12, i13, f, f1, this.getActiveEntityProperties(), this.getActiveRenderer());
    }

    protected void innerRenderPlayerEntity(int posX, int posY, int scale, float angleXComponent, float angleYComponent, PlayerEntityProperties props, PlayerEntityItemRenderer renderer) {
        float angleX = angleXComponent;
        float angleY = angleYComponent;
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate((posX+((this.getActiveEntityProperties().getDimensions().width / 2) * scale)), (posY+(this.getActiveEntityProperties().getDimensions().height * this.scale)), 1050.0F);
        modelViewStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        PoseStack innerMatrix = new PoseStack();
        innerMatrix.translate(0.0F, 0.0F, 1000.0F);
        innerMatrix.scale((float)scale, (float)scale, (float)scale);
        Quaternionf quat1;
        Quaternionf quat2;
        if (this.followMouse) {
            quat1 = (new Quaternionf()).rotateZ((float)Math.PI);
            quat2 = (new Quaternionf()).rotateX(angleY * 20.0F * ((float)Math.PI / 180F));
            quat1.mul(quat2);
            innerMatrix.mulPose(quat1);
            props.yBodyRot = 180.0F + angleX * 20.0F;
            props.yRot = 180.0F + angleX * 40.0F;
            props.xRot = -angleY * 20.0F;
            props.yHeadRot = props.yRot;
            props.yHeadRotO = props.yRot;
        } else {
            quat1 = Axis.ZP.rotationDegrees(180.0F);
            quat2 = Axis.XP.rotationDegrees(this.bodyRotationY);
            quat1.mul(quat2);
            innerMatrix.mulPose(quat1);
            props.yBodyRot = 180.0F + this.bodyRotationX;
            props.xRot = this.headRotationY;
            props.yHeadRot = 180.0F + this.headRotationX;
            props.yHeadRotO = 180.0F + this.headRotationX;
        }
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quat2.conjugate();
        dispatcher.overrideCameraOrientation(quat2);
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            renderer.renderPlayerEntityItem(0.0D, 0.0D, 0.0D, 0.0F, 1.0F, innerMatrix, bufferSource, 15728880);
        });
        bufferSource.endBatch();
        dispatcher.setRenderShadow(true);
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

}
