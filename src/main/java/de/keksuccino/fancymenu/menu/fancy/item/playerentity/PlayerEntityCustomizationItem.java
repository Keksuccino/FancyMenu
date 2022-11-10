package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;

import com.mojang.authlib.GameProfile;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.resources.WebTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.model.ModelParrot;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderParrot;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlayerEntityCustomizationItem extends CustomizationItemBase {

	private static final Logger LOGGER = LogManager.getLogger();
	
	public MenuPlayerEntity entity;
	public int scale = 30;
	public String playerName = null;
	
	public boolean autoRotation = true;
	public float bodyRotationX = 0;
	public float bodyRotationY = 0;
	public float headRotationX = 0;
	public float headRotationY = 0;
	
	private static final World DUMMY_WORLD = DummyWorldFactory.getDummyWorld();
	
	private static final MenuPlayerRenderer PLAYER_RENDERER = new MenuPlayerRenderer(false);
	private static final MenuPlayerRenderer SLIM_PLAYER_RENDERER = new MenuPlayerRenderer(true);
	
	public PlayerEntityCustomizationItem(PropertiesSection item) {
		super(item);
		if (!FancyMenu.config.getOrDefault("allow_level_registry_interactions", false)) {
			LOGGER.warn("CRITICAL WARNING: Player Entity element constructed while level registry interactions were disabled! Please report this to the dev of FancyMenu!");
		}
		String scaleString = item.getEntryValue("scale");
		if ((scaleString != null) && MathUtils.isDouble(scaleString)) {
			//Avoiding errors when trying to set a double/long value as scale
			this.scale = (int) Double.parseDouble(scaleString);
		}
		
		this.playerName = item.getEntryValue("playername");
		
		if (this.playerName != null) {
			this.playerName = DynamicValueHelper.convertFromRaw(this.playerName);
		}
		
		this.entity = new MenuPlayerEntity(this.playerName);
		
		String skinUrl = item.getEntryValue("skinurl");
		if (skinUrl != null) {
			skinUrl = DynamicValueHelper.convertFromRaw(skinUrl);
			WebTextureResourceLocation wt = TextureHandler.getWebResource(skinUrl);
			if (wt != null) {
				this.entity.skinLocation = wt.getResourceLocation();
			}
		}

		String skin = fixBackslashPath(item.getEntryValue("skinpath"));
		if ((skin != null) && (this.entity.skinLocation == null)) {
			File f = new File(skin);
			if (!f.exists() || !f.getAbsolutePath().startsWith(Minecraft.getMinecraft().mcDataDir.getAbsolutePath())) {
				skin = Minecraft.getMinecraft().mcDataDir.getAbsolutePath() + "/" + skin;
				f = new File(skin);
			}
			ExternalTextureResourceLocation r = TextureHandler.getResource(skin);
			if (r != null) {
				if (r.getHeight() < 64) {
					if (f.isFile() && (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png"))) {
						String sha1 = PlayerEntityCache.calculateSHA1(f);
						if (sha1 != null) {
							if (!PlayerEntityCache.isSkinCached(sha1)) {
								SkinExternalTextureResourceLocation sr = new SkinExternalTextureResourceLocation(skin);
								sr.loadTexture();
								PlayerEntityCache.cacheSkin(sha1, sr.getResourceLocation());
								this.entity.skinLocation = sr.getResourceLocation();
							} else {
								this.entity.skinLocation = PlayerEntityCache.getSkin(sha1);
							}
						}
					}
				} else {
					this.entity.skinLocation = r.getResourceLocation();
				}
			}
		}
		
		String capeUrl = item.getEntryValue("capeurl");
		if (capeUrl != null) {
			capeUrl = DynamicValueHelper.convertFromRaw(capeUrl);
			WebTextureResourceLocation wt = TextureHandler.getWebResource(capeUrl);
			if (wt != null) {
				this.entity.capeLocation = wt.getResourceLocation();
			}
		}

		String cape = fixBackslashPath(item.getEntryValue("capepath"));
		if ((cape != null) && (this.entity.capeLocation == null)) {
			File f = new File(cape);
			if (!f.exists() || !f.getAbsolutePath().startsWith(Minecraft.getMinecraft().mcDataDir.getAbsolutePath())) {
				cape = Minecraft.getMinecraft().mcDataDir.getAbsolutePath() + "/" + cape;
			}
			ExternalTextureResourceLocation r = TextureHandler.getResource(cape);
			if (r != null) {
				this.entity.capeLocation = r.getResourceLocation();
			}
		}
		
		String slim = item.getEntryValue("slim");
		if (slim != null) {
			if (slim.replace(" ", "").equalsIgnoreCase("true")) {
				this.entity.setSlimSkin(true);
			}
		}
		
		String parrot = item.getEntryValue("parrot");
		if (parrot != null) {
			if (parrot.replace(" ", "").equalsIgnoreCase("true")) {
				this.entity.hasParrot = true;
			}
		}
		
		String crouching = item.getEntryValue("crouching");
		if (crouching != null) {
			if (crouching.replace(" ", "").equalsIgnoreCase("true")) {
				this.entity.crouching = true;
			}
		}
		
		String showName = item.getEntryValue("showname");
		if (showName != null) {
			if (showName.replace(" ", "").equalsIgnoreCase("false")) {
				this.entity.showName = false;
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
		
		String autoRot = item.getEntryValue("autorotation");
		if (autoRot != null) {
			if (autoRot.replace(" ", "").equalsIgnoreCase("false")) {
				this.autoRotation = false;
			}
		}
		
		if (this.playerName != null) {
			this.value = this.playerName;
		} else {
			this.value = "Player Entity";
		}
		
		this.width = (int) (this.entity.width*this.scale);
		this.height = (int) (this.entity.height*this.scale);
		
	}

	@Override
	public void render(GuiScreen menu) throws IOException {
		try {
			if (this.shouldRender()) {
				if (this.entity != null) {
					
					//Update dummy value for layout editor
					if (this.playerName != null) {
						this.value = this.playerName;
					} else {
						this.value = "Player Entity";
					}
					
					//Update object width and height for layout editor
					this.width = (int) (this.entity.width*this.scale);
					this.height = (int) (this.entity.height*this.scale);
					
					int mX = MouseInput.getMouseX();
					int mY = MouseInput.getMouseY();

				    renderPlayerEntity(this.getPosX(menu), this.getPosY(menu), this.scale, mX, mY, this);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void renderPlayerEntity(int posX, int posY, int scale, float mouseX, float mouseY, PlayerEntityCustomizationItem item) {
		float entityHeight = item.entity.height * item.scale;
		float rotationX = (float)Math.atan((double)((mouseX - item.getPosX(Minecraft.getMinecraft().currentScreen)) / 40.0F));
		float rotationY = (float)Math.atan((double)((mouseY - (item.getPosY(Minecraft.getMinecraft().currentScreen) - (entityHeight / 2))) / 40.0F));
		
		GlStateManager.pushMatrix();
		GlStateManager.enableDepth();
		GlStateManager.translate((float)posX, (float)posY, 1050.0F);
		GlStateManager.scale(1.0F, 1.0F, -1.0F);
		GlStateManager.translate(0.0D, 0.0D, 1000.0D);
		GlStateManager.scale((float)scale, (float)scale, (float)scale);
		GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        
		if (!item.autoRotation) {
			
			//vertical rotation body
			GlStateManager.rotate(-((float)Math.atan(item.bodyRotationY)) * 20.0F, 1.0F, 0.0F, 0.0F);
			
			//horizontal rotation body
			item.entity.renderYawOffset = item.bodyRotationX;
			
			//vertical rotation head
			item.entity.rotationPitch = item.headRotationY;
			
			//horizontal rotation head
			item.entity.rotationYawHead = item.headRotationX;
			
		} else {

			GlStateManager.rotate(-((float)Math.atan(rotationY)) * 20.0F, 1.0F, 0.0F, 0.0F);

			item.entity.renderYawOffset = Math.negateExact((long)(180.0F + rotationX * 20.0F));

			item.entity.rotationPitch = Math.negateExact((long)(-rotationY * 20.0F));

			item.entity.rotationYawHead = Math.negateExact((long)(180.0F + rotationX * 40.0F));
			
		}
		
		renderAsFancy(() -> {
			item.renderEntityStatic(0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
		});
		
		GlStateManager.popMatrix();
		
		RenderHelper.disableStandardItemLighting();
	}

	private static void renderAsFancy(Runnable renderFancy) {
		boolean b = Minecraft.isFancyGraphicsEnabled();
		if (b) {
			renderFancy.run();
		} else {
			GameSettings g = Minecraft.getMinecraft().gameSettings;
			g.fancyGraphics = true;
			renderFancy.run();
			g.fancyGraphics = false;
		}
	}
	
	public void renderEntityStatic(double xIn, double yIn, double zIn, float rotationYawIn, float partialTicks) {
		try {
			
			Vec3d vector3d;
			if (this.entity.isSlimSkin()) {
				vector3d = SLIM_PLAYER_RENDERER.getRenderOffset(this.entity, partialTicks);
			} else {
				vector3d = PLAYER_RENDERER.getRenderOffset(this.entity, partialTicks);
			}
			
			double d2 = xIn + vector3d.x;
			double d3 = yIn + vector3d.y;
			double d0 = zIn + vector3d.z;
			GlStateManager.pushMatrix();
			GlStateManager.translate(d2, d3, d0);
			
			if (this.entity.isSlimSkin()) {
				SLIM_PLAYER_RENDERER.doRender(this.entity, xIn, yIn, zIn, rotationYawIn, partialTicks);
			} else {
				PLAYER_RENDERER.doRender(this.entity, xIn, yIn, zIn, rotationYawIn, partialTicks);
			}
			
			GlStateManager.translate(-vector3d.x, -vector3d.y, -vector3d.z);

			GlStateManager.popMatrix();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String getSkinURL(String playerName) {
		String skinUrl = null;
		
		try {
			URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + playerName);
			Scanner scanner = new Scanner(new InputStreamReader(url.openStream()));
			boolean b = false;
			boolean b2 = false;
			while (scanner.hasNextLine()) {
				String line = scanner.next();
				if (b) {
					skinUrl = line.substring(1, line.length()-2);
					break;
				}
				if (line.contains("\"skin\":")) {
					b2 = true;
				}
				if (line.contains("\"code\":")) {
					break;
				}
				if (line.contains("\"url\":")) {
					if (b2) {
						b = true;
					}
				}
			}
			scanner.close();
		} catch (IOException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return skinUrl;
	}
	
	private static String getCapeURL(String playerName) {
		String capeUrl = null;
		
		try {
			URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + playerName);
			Scanner scanner = new Scanner(new InputStreamReader(url.openStream()));
			boolean b = false;
			boolean b2 = false;
			while (scanner.hasNextLine()) {
				String line = scanner.next();
				if (b) {
					capeUrl = line.substring(1, line.length()-2);
					break;
				}
				if (line.contains("\"cape\":")) {
					b2 = true;
				}
				if (line.contains("\"code\":")) {
					break;
				}
				if (line.contains("\"url\":")) {
					if (b2) {
						b = true;
					}
				}
			}
			scanner.close();
		} catch (IOException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return capeUrl;
	}
	
	private static boolean getIsSlimSkin(String playerName) {
		boolean slim = false;
		
		try {
			URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + playerName);
			Scanner scanner = new Scanner(new InputStreamReader(url.openStream()));
			boolean b = false;
			boolean b2 = false;
			while (scanner.hasNextLine()) {
				String line = scanner.next();
				if (b) {
					String slimString = line.substring(1, line.length()-2);
					if (slimString.equalsIgnoreCase("true")) {
						slim = true;
					}
					break;
				}
				if (line.contains("\"textures\":")) {
					b2 = true;
				}
				if (line.contains("\"code\":")) {
					break;
				}
				if (line.contains("\"slim\":")) {
					if (b2) {
						b = true;
					}
				}
			}
			scanner.close();
		} catch (IOException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return slim;
	}

	public static class MenuPlayerRenderer extends RenderLivingBase<MenuPlayerEntity> {

		public MenuPlayerRenderer(boolean useSmallArms) {
			super(Minecraft.getMinecraft().getRenderManager(), new ModelPlayer(0.0F, useSmallArms), 0.5F);
			this.addLayer(new LayerBipedArmor(this));
			this.addLayer(new MenuPlayerCapeLayer(this));
			this.addLayer(new LayerCustomHead(this.getMainModel().bipedHead));
			this.addLayer(new MenuPlayerParrotLayer());
		}

		public Vec3d getRenderOffset(MenuPlayerEntity entityIn, float partialTicks) {
			return entityIn.isSneaking() ? new Vec3d(0.0D, -0.125D, 0.0D) : Vec3d.ZERO;
		}
		
		@Override
		public void doRender(MenuPlayerEntity entity, double x, double y, double z, float entityYaw, float partialTicks) {
			
			double d0 = y;

            if (entity.isSneaking()) {
                d0 = y - 0.125D;
            }

            this.setModelVisibilities(entity);
            GlStateManager.enableBlendProfile(GlStateManager.Profile.PLAYER_SKIN);
            
            GlStateManager.pushMatrix();
            GlStateManager.disableCull();
            this.mainModel.swingProgress = this.getSwingProgress(entity, partialTicks);
            boolean shouldSit = entity.isRiding() && (entity.getRidingEntity() != null && entity.getRidingEntity().shouldRiderSit());
            this.mainModel.isRiding = shouldSit;
            this.mainModel.isChild = entity.isChild();

            try {
                float f = this.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
                float f1 = this.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
                float f2 = f1 - f;

                if (shouldSit && entity.getRidingEntity() instanceof EntityLivingBase) {
                    EntityLivingBase entitylivingbase = (EntityLivingBase)entity.getRidingEntity();
                    f = this.interpolateRotation(entitylivingbase.prevRenderYawOffset, entitylivingbase.renderYawOffset, partialTicks);
                    f2 = f1 - f;
                    float f3 = MathHelper.wrapDegrees(f2);

                    if (f3 < -85.0F) {
                        f3 = -85.0F;
                    }

                    if (f3 >= 85.0F) {
                        f3 = 85.0F;
                    }

                    f = f1 - f3;

                    if (f3 * f3 > 2500.0F) {
                        f += f3 * 0.2F;
                    }

                    f2 = f1 - f;
                }

                float f7 = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
                this.renderLivingAt(entity, x, d0, z);
                float f8 = this.handleRotationFloat(entity, partialTicks);
                this.applyRotations(entity, f8, f, partialTicks);
                float f4 = this.prepareScale(entity, partialTicks);
                float f5 = 0.0F;
                float f6 = 0.0F;

                if (!entity.isRiding()) {
                	
                    f5 = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
                    f6 = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);

                    if (entity.isChild()) {
                        f6 *= 3.0F;
                    }

                    if (f5 > 1.0F) {
                        f5 = 1.0F;
                    }
                    f2 = f1 - f;
                    
                }

                GlStateManager.enableAlpha();
                this.mainModel.setLivingAnimations(entity, f6, f5, partialTicks);
                this.mainModel.setRotationAngles(f6, f5, f8, f2, f7, f4, entity);

                if (this.renderOutlines) {

                    if (!this.renderMarker) {
                        this.renderModel(entity, f6, f5, f8, f2, f7, f4);
                    }

                    if (!(entity instanceof EntityPlayer) || !((EntityPlayer)entity).isSpectator()) {
                        this.renderLayers(entity, f6, f5, partialTicks, f8, f2, f7, f4);
                    }
                    
                } else {

                    this.renderModel(entity, f6, f5, f8, f2, f7, f4);

                    GlStateManager.depthMask(true);

                    if (!(entity instanceof EntityPlayer) || !((EntityPlayer)entity).isSpectator()) {
                        this.renderLayers(entity, f6, f5, partialTicks, f8, f2, f7, f4);
                    }
                    
                    GlStateManager.depthMask(false);
                    
                }

                GlStateManager.disableRescaleNormal();
                
            } catch (Exception e) {
            	e.printStackTrace();
            }

            GlStateManager.enableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
            
            if (!this.renderOutlines) {
                this.renderName(entity, x, d0, z);
            }
            
            GlStateManager.disableBlendProfile(GlStateManager.Profile.PLAYER_SKIN);
	    }
		
		@Override
		public ModelPlayer getMainModel() {
			return (ModelPlayer)super.getMainModel();
		}

		private void setModelVisibilities(MenuPlayerEntity clientPlayer) {
			ModelPlayer playermodel = this.getMainModel();
			playermodel.setVisible(true);
			playermodel.bipedHeadwear.showModel = true;
			playermodel.bipedBodyWear.showModel = true;
			playermodel.bipedLeftLegwear.showModel = true;
			playermodel.bipedRightLegwear.showModel = true;
			playermodel.bipedLeftArmwear.showModel = true;
			playermodel.bipedRightArmwear.showModel = true;
			playermodel.isSneak = clientPlayer.isSneaking();
		}

		@Override
		public ResourceLocation getEntityTexture(MenuPlayerEntity entity) {
			ResourceLocation l = entity.getSkin();
			if (l != null) {
				return l;
			}
			return DefaultPlayerSkin.getDefaultSkinLegacy();
		}
		
		@Override
		protected boolean canRenderName(MenuPlayerEntity entity) {
			if (entity.showName) {
				if (entity.playerName != null) {
					return true;
				}
			}
			return false;
		}
		 
		@Override
		public void renderName(MenuPlayerEntity playerEntity, double x, double y, double z) {
			if (playerEntity.showName) {
				boolean flag = playerEntity.isSneaking();
	            float f = Minecraft.getMinecraft().getRenderManager().playerViewY;
	            float f1 = Minecraft.getMinecraft().getRenderManager().playerViewX;
	            boolean flag1 = Minecraft.getMinecraft().gameSettings.thirdPersonView == 2;
	            float f2 = playerEntity.height + 0.5F - (flag ? 0.25F : 0.0F);
	            int i = "deadmau5".equals(playerEntity.getDisplayName().getFormattedText()) ? -10 : 0;
	            EntityRenderer.drawNameplate(Minecraft.getMinecraft().fontRenderer, playerEntity.getDisplayName().getFormattedText(), (float)x, (float)y + f2, (float)z, i, f, f1, flag1, flag);
			}
		}
		
		@Override
		protected void preRenderCallback(MenuPlayerEntity entitylivingbaseIn, float partialTickTime) {
	        GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
		}

		@Override
		protected void applyRotations(MenuPlayerEntity entityLiving, float p_77043_2_, float rotationYaw, float partialTicks) {
			if (entityLiving.isEntityAlive() && entityLiving.isPlayerSleeping()) {
	            GlStateManager.rotate(entityLiving.getBedOrientationInDegrees(), 0.0F, 1.0F, 0.0F);
	            GlStateManager.rotate(this.getDeathMaxRotation(entityLiving), 0.0F, 0.0F, 1.0F);
	            GlStateManager.rotate(270.0F, 0.0F, 1.0F, 0.0F);
	        }
	        else if (entityLiving.isElytraFlying()) {
	            super.applyRotations(entityLiving, p_77043_2_, rotationYaw, partialTicks);
	            float f = (float)entityLiving.getTicksElytraFlying() + partialTicks;
	            float f1 = MathHelper.clamp(f * f / 100.0F, 0.0F, 1.0F);
	            GlStateManager.rotate(f1 * (-90.0F - entityLiving.rotationPitch), 1.0F, 0.0F, 0.0F);
	            Vec3d vec3d = entityLiving.getLook(partialTicks);
	            double d0 = entityLiving.motionX * entityLiving.motionX + entityLiving.motionZ * entityLiving.motionZ;
	            double d1 = vec3d.x * vec3d.x + vec3d.z * vec3d.z;

	            if (d0 > 0.0D && d1 > 0.0D)
	            {
	                double d2 = (entityLiving.motionX * vec3d.x + entityLiving.motionZ * vec3d.z) / (Math.sqrt(d0) * Math.sqrt(d1));
	                double d3 = entityLiving.motionX * vec3d.z - entityLiving.motionZ * vec3d.x;
	                GlStateManager.rotate((float)(Math.signum(d3) * Math.acos(d2)) * 180.0F / (float)Math.PI, 0.0F, 1.0F, 0.0F);
	            }
	        } else {
	            super.applyRotations(entityLiving, p_77043_2_, rotationYaw, partialTicks);
	        }
		}

	}
	
	public static class MenuPlayerEntity extends EntityPlayer {

		public volatile ResourceLocation skinLocation;
		public volatile ResourceLocation capeLocation;
		private volatile boolean capeChecked = false;
		private volatile boolean capeGettingChecked = false;
		private volatile boolean skinChecked = false;
		private volatile boolean skinGettingChecked = false;
		private volatile boolean slimSkin = false;
		private volatile boolean slimSkinChecked = false;
		private volatile boolean slimSkinGettingChecked = false;
		public boolean hasParrot = false;
		public boolean crouching = false;
		public boolean showName = true;
		public volatile String playerName;
		
		private volatile Runnable getSkinCallback;
		private volatile Runnable getCapeCallback;
		
		public MenuPlayerEntity(String playerName) {
			super(DUMMY_WORLD, new GameProfile(EntityPlayer.getOfflineUUID(getRawPlayerName(playerName)), getRawPlayerName(playerName)));
			if (playerName != null) {
				this.playerName = playerName;
			}
		}
		
		private static String getRawPlayerName(String playerName) {
			if (playerName == null) {
				return "steve";
			}
			return playerName;
		}
		
		@Override
		public boolean isSpectator() {
			return false;
		}

		@Override
		public boolean isCreative() {
			return false;
		}
		
		@Override
		public boolean isSneaking() {
			return this.crouching;
		}
		
		@Override
		public ITextComponent getDisplayName() {
			if (this.playerName != null) {
				return new TextComponentString(this.playerName);
			}
			return new TextComponentString("steve");
		}
		
		public void setSlimSkin(boolean slim) {
			this.slimSkin = slim;
			this.slimSkinChecked = true;
		}
		
		public boolean isSlimSkin() {
			if (this.playerName != null) {
				if (!this.slimSkinChecked) {
					if (!PlayerEntityCache.isSlimSkinInfoCached(playerName)) {
						if (!this.slimSkinGettingChecked) {
							this.slimSkinGettingChecked = true;
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										boolean b = getIsSlimSkin(playerName);
										if (!slimSkinChecked) {
											slimSkin = b;
											PlayerEntityCache.cacheIsSlimSkin(playerName, b);
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
									slimSkinChecked = true;
									slimSkinGettingChecked = false;
								}
							}).start();
						}
					} else {
						slimSkin = PlayerEntityCache.getIsSlimSkin(playerName);
						slimSkinChecked = true;
					}
				}
			}
			
			return this.slimSkin;
		}
		
		public boolean hasNonDefaultSkin() {
			return (this.skinLocation != DefaultPlayerSkin.getDefaultSkinLegacy());
		}
		
		public boolean hasCape() {
			return (this.getCape() != null);
		}
		
		public ResourceLocation getSkin() {
			
			if (this.getSkinCallback != null) {
				this.getSkinCallback.run();
				this.getSkinCallback = null;
			}
			
			if (this.playerName != null) {
				if (this.skinLocation == null) {
					if (!skinChecked) {
						if (!PlayerEntityCache.isSkinCached(playerName)) {
							if (!skinGettingChecked) {
								this.skinGettingChecked = true;
								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											String skinUrl = getSkinURL(playerName);
											if (skinLocation == null) {
												if (skinUrl == null) {
													skinLocation = DefaultPlayerSkin.getDefaultSkinLegacy();
													slimSkin = false;
													slimSkinChecked = true;
												} else {
													if (getSkinCallback == null) {
														getSkinCallback = new Runnable() {
															@Override
															public void run() {
																WebTextureResourceLocation wt = TextureHandler.getWebResource(skinUrl);
																if (skinLocation == null) {
																	if (wt != null) {
																		if (wt.getHeight() < 64) {
																			wt = new SkinWebTextureResourceLocation(skinUrl);
																			wt.loadTexture();
																		}
																		skinLocation = wt.getResourceLocation();
																		PlayerEntityCache.cacheSkin(playerName, wt.getResourceLocation());
																	} else {
																		skinLocation = DefaultPlayerSkin.getDefaultSkinLegacy();
																		slimSkin = false;
																		slimSkinChecked = true;
																	}
																}
															}
														};
													}
												}
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
										skinChecked = true;
										skinGettingChecked = false;
									}
								}).start();
							} else {
								return DefaultPlayerSkin.getDefaultSkinLegacy();
							}
						} else {
							skinLocation = PlayerEntityCache.getSkin(playerName);
							skinChecked = true;
						}
					} else {
						this.skinLocation = DefaultPlayerSkin.getDefaultSkinLegacy();
						this.slimSkin = false;
						this.slimSkinChecked = true;
					}
				}
			} else if (this.skinLocation == null) {
				this.skinLocation = DefaultPlayerSkin.getDefaultSkinLegacy();
				this.slimSkin = false;
				this.slimSkinChecked = true;
			}
			
			return this.skinLocation;
		}
		
		public ResourceLocation getCape() {
			
			if (this.getCapeCallback != null) {
				this.getCapeCallback.run();
				this.getCapeCallback = null;
			}
			
			if (this.playerName != null) {
				if ((this.capeLocation == null) && !this.capeChecked) {
					if (!PlayerEntityCache.isCapeCached(playerName)) {
						if (!capeGettingChecked) {
							capeGettingChecked = true;
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										String capeUrl = getCapeURL(playerName);
										if (!capeChecked) {
											if (capeUrl != null) {
												if (getCapeCallback == null) {
													getCapeCallback = new Runnable() {
														@Override
														public void run() {
															WebTextureResourceLocation wt = TextureHandler.getWebResource(capeUrl);
															if (wt != null) {
																capeLocation = wt.getResourceLocation();
																PlayerEntityCache.cacheCape(playerName, wt.getResourceLocation());
															}
														}
													};
												}
											} else {
											}
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
									capeGettingChecked = false;
									capeChecked = true;
								}
							}).start();
						}
					} else {
						capeLocation = PlayerEntityCache.getCape(playerName);
						capeChecked = true;
					}
				}
			}
			
			return this.capeLocation;
		}
		
	}

	public static class MenuPlayerCapeLayer implements LayerRenderer<MenuPlayerEntity> {

		private MenuPlayerRenderer render;
		
		public MenuPlayerCapeLayer(MenuPlayerRenderer menuPlayerRenderer) {
			this.render = menuPlayerRenderer;
		}

		@Override
		public void doRenderLayer(MenuPlayerEntity playerEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
			if (playerEntity.hasCape()) {
				
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.render.bindTexture(playerEntity.getCape());
                
				GlStateManager.pushMatrix();
				GlStateManager.translate(0.0D, 0.0D, 0.125D);
				double d0 = MathHelper.clampedLerp((double)partialTicks, playerEntity.prevChasingPosX, playerEntity.chasingPosX) - MathHelper.clampedLerp((double)partialTicks, playerEntity.prevPosX, playerEntity.posX);
				double d1 = MathHelper.clampedLerp((double)partialTicks, playerEntity.prevChasingPosY, playerEntity.chasingPosY) - MathHelper.clampedLerp((double)partialTicks, playerEntity.prevPosY, playerEntity.posY);
				double d2 = MathHelper.clampedLerp((double)partialTicks, playerEntity.prevChasingPosZ, playerEntity.chasingPosZ) - MathHelper.clampedLerp((double)partialTicks, playerEntity.prevPosZ, playerEntity.posZ);
				float f = playerEntity.prevRenderYawOffset + (playerEntity.renderYawOffset - playerEntity.prevRenderYawOffset);
				double d3 = (double)MathHelper.sin(f * ((float)Math.PI / 180F));
				double d4 = (double)(-MathHelper.cos(f * ((float)Math.PI / 180F)));
				float f1 = (float)d1 * 10.0F;
	            f1 = MathHelper.clamp(f1, -6.0F, 32.0F);
				float f2 = (float)(d0 * d3 + d2 * d4) * 100.0F;
				f2 = MathHelper.clamp(f2, 0.0F, 150.0F);

				float f4 = (float) MathHelper.clampedLerp(partialTicks, playerEntity.prevCameraYaw, playerEntity.cameraYaw);
				f1 = f1 + MathHelper.sin((float) (MathHelper.clampedLerp(partialTicks, playerEntity.prevDistanceWalkedModified, playerEntity.distanceWalkedModified) * 6.0F)) * 32.0F * f4;
				if (playerEntity.isSneaking()) {
					f1 += 25.0F;
				}

				GlStateManager.rotate(6.0F + f2 / 2.0F + f1, 1.0F, 0.0F, 0.0F);
				
				GlStateManager.rotate(0.0F, 0.0F, 0.0F, 0.0F);
				
				GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
				
				this.render.getMainModel().renderCape(0.0625F);
				
				GlStateManager.popMatrix();
			}
		}
		
	    public boolean shouldCombineTextures() {
	        return false;
	    }
	    
	}

	public static class MenuPlayerParrotLayer implements LayerRenderer<MenuPlayerEntity> {

		private final ModelParrot parrotModel = new ModelParrot();
		private final RenderParrot parrotRenderer = new RenderParrot(Minecraft.getMinecraft().getRenderManager());

		public void doRenderLayer(MenuPlayerEntity playerEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
			if (playerEntity.hasParrot) {
				this.renderParrot(playerEntity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale, 0);
			}
		}

		private void renderParrot(MenuPlayerEntity playerEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, int parrotVariant) {
	        this.parrotRenderer.bindTexture(RenderParrot.PARROT_TEXTURES[parrotVariant]);
	        GlStateManager.pushMatrix();
	        float f = playerEntity.isSneaking() ? -1.3F : -1.5F;
	        float f1 = -0.4F;
	        GlStateManager.translate(f1, f, 0.0F);

	        this.parrotModel.setLivingAnimations(playerEntity, limbSwing, limbSwingAmount, partialTicks);
	        this.parrotModel.setRotationAngles(limbSwing, limbSwingAmount, 0.0F, netHeadYaw, headPitch, scale, playerEntity);
	        this.parrotModel.render(playerEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
	        GlStateManager.popMatrix();
		}

		@Override
		public boolean shouldCombineTextures() {
			return false;
		}
	}

}
