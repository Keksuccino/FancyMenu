package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.resources.WebTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.HeadLayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.ParrotModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

public class PlayerEntityCustomizationItem extends CustomizationItemBase {
	
	public MenuPlayerEntity entity;
	public int scale = 30;
	public String playerName = null;
	
	public boolean autoRotation = true;
	public float bodyRotationX = 0;
	public float bodyRotationY = 0;
	public float headRotationX = 0;
	public float headRotationY = 0;

	private static final World DUMMY_WORLD = DummyWorldFactory.getDummyWorld();
	private static final BlockPos BLOCK_POS = new BlockPos(0, 0, 0);
	
	private static final MenuPlayerRenderer PLAYER_RENDERER = new MenuPlayerRenderer(false);
	private static final MenuPlayerRenderer SLIM_PLAYER_RENDERER = new MenuPlayerRenderer(true);
	
	public PlayerEntityCustomizationItem(PropertiesSection item) {
		super(item);
		
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
			ExternalTextureResourceLocation r = TextureHandler.getResource(skin);
			if (r != null) {
				if (r.getHeight() < 64) {
					File f = new File(skin);
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

		//TODO übernehmen
		this.setWidth((int)(this.entity.getWidth()*this.scale));
		this.setHeight((int)(this.entity.getHeight()*this.scale));
		//--------------
		
	}

	@Override
	public void render(MatrixStack matrix, Screen menu) throws IOException {
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
					//TODO übernehmen
					this.setWidth((int)(this.entity.getWidth()*this.scale));
					this.setHeight((int)(this.entity.getHeight()*this.scale));
					//--------------
					
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
		float entityHeight = item.entity.getHeight() * item.scale;
		float rotationX = (float)Math.atan((double)((mouseX - item.getPosX(Minecraft.getInstance().currentScreen)) / 40.0F));
		float rotationY = (float)Math.atan((double)((mouseY - (item.getPosY(Minecraft.getInstance().currentScreen) - (entityHeight / 2))) / 40.0F));
		RenderSystem.pushMatrix();
		RenderSystem.translatef((float)posX, (float)posY, 1050.0F);
		RenderSystem.scalef(1.0F, 1.0F, -1.0F);
		MatrixStack matrix = new MatrixStack();
		matrix.translate(0.0D, 0.0D, 1000.0D);
		matrix.scale((float)scale, (float)scale, (float)scale);

		if (!item.autoRotation) {
			
			//vertical rotation body
			Quaternion q = Vector3f.ZP.rotationDegrees(180.0F);
			Quaternion q2 = Vector3f.XP.rotationDegrees(item.bodyRotationY);
			q.multiply(q2);
			matrix.rotate(q);
			//-----------
			
			//horizontal rotation body
			item.entity.renderYawOffset = item.bodyRotationX;
			
			//vertical rotation head
			item.entity.rotationPitch = item.headRotationY;
			
			//horizontal rotation head
			item.entity.rotationYawHead = item.headRotationX;
			
		} else {
			
			Quaternion q = Vector3f.ZP.rotationDegrees(180.0F);
			Quaternion q2 = Vector3f.XP.rotationDegrees(Math.negateExact((long) (rotationY * 20.0F)));
			q.multiply(q2);
			matrix.rotate(q);

			item.entity.renderYawOffset = Math.negateExact((long)(180.0F + rotationX * 20.0F));

			item.entity.rotationPitch = Math.negateExact((long)(-rotationY * 20.0F));

			item.entity.rotationYawHead = Math.negateExact((long)(180.0F + rotationX * 40.0F));
			
		}

		IRenderTypeBuffer.Impl rb = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
		RenderSystem.runAsFancy(() -> {
			item.renderEntityStatic(0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrix, rb, 15728880);
		});
		rb.finish();
		
		RenderSystem.popMatrix();
	}
	
	public void renderEntityStatic(double xIn, double yIn, double zIn, float rotationYawIn, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
		try {
			
			Vector3d vector3d;
			if (this.entity.isSlimSkin()) {
				vector3d = SLIM_PLAYER_RENDERER.getRenderOffset(this.entity, partialTicks);
			} else {
				vector3d = PLAYER_RENDERER.getRenderOffset(this.entity, partialTicks);
			}
			
			double d2 = xIn + vector3d.getX();
			double d3 = yIn + vector3d.getY();
			double d0 = zIn + vector3d.getZ();
			matrixStackIn.push();
			matrixStackIn.translate(d2, d3, d0);
			
			if (this.entity.isSlimSkin()) {
				SLIM_PLAYER_RENDERER.render(this.entity, rotationYawIn, partialTicks, matrixStackIn, bufferIn, packedLightIn);
			} else {
				PLAYER_RENDERER.render(this.entity, rotationYawIn, partialTicks, matrixStackIn, bufferIn, packedLightIn);
			}
			
			matrixStackIn.translate(-vector3d.getX(), -vector3d.getY(), -vector3d.getZ());

			matrixStackIn.pop();
			
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

	public static class MenuPlayerRenderer extends LivingRenderer<MenuPlayerEntity, PlayerModel<MenuPlayerEntity>> {

		@SuppressWarnings("rawtypes")
		public MenuPlayerRenderer(boolean useSmallArms) {
			super(Minecraft.getInstance().getRenderManager(), new PlayerModel<>(0.0F, useSmallArms), 0.5F);
			this.addLayer(new BipedArmorLayer<>(this, new BipedModel(0.5F), new BipedModel(1.0F)));
			this.addLayer(new MenuPlayerCapeLayer(this));
			this.addLayer(new HeadLayer<>(this));
			this.addLayer(new MenuPlayerParrotLayer(this));
		}

		@Override
		public void render(MenuPlayerEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
			this.setModelVisibilities(entityIn);
			super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
		}

		@Override
		public Vector3d getRenderOffset(MenuPlayerEntity entityIn, float partialTicks) {
			return entityIn.isCrouching() ? new Vector3d(0.0D, -0.125D, 0.0D) : super.getRenderOffset(entityIn, partialTicks);
		}

		private void setModelVisibilities(MenuPlayerEntity clientPlayer) {
			PlayerModel<MenuPlayerEntity> playermodel = this.getEntityModel();
			playermodel.setVisible(true);
			playermodel.bipedHeadwear.showModel = true;
			playermodel.bipedBodyWear.showModel = true;
			playermodel.bipedLeftLegwear.showModel = true;
			playermodel.bipedRightLegwear.showModel = true;
			playermodel.bipedLeftArmwear.showModel = true;
			playermodel.bipedRightArmwear.showModel = true;
			playermodel.isSneak = clientPlayer.isCrouching();
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
				if (entity.getDisplayName() != null) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		protected void renderName(MenuPlayerEntity playerEntity, ITextComponent displayNameIn, MatrixStack matrix, IRenderTypeBuffer bufferIn, int packedLightIn) {
			if (playerEntity.showName) {
				boolean flag = !playerEntity.isDiscrete();
				float f = playerEntity.getHeight() + 0.5F;
				matrix.push();
				matrix.translate(0.0D, (double)f, 0.0D);
				matrix.rotate(new Quaternion(0, 0, 0, 0));
				matrix.scale(-0.025F, -0.025F, 0.025F);
				Matrix4f matrix4f = matrix.getLast().getMatrix();
				float f1 = Minecraft.getInstance().gameSettings.getTextBackgroundOpacity(0.25F);
				int j = (int)(f1 * 255.0F) << 24;
				FontRenderer fontrenderer = this.getFontRendererFromRenderManager();
				float f2 = (float)(-fontrenderer.getStringPropertyWidth(displayNameIn) / 2);
				fontrenderer.func_243247_a(displayNameIn, f2, 0, 553648127, false, matrix4f, bufferIn, flag, j, packedLightIn);
				if (flag) {
					fontrenderer.func_243247_a(displayNameIn, f2, 0, -1, false, matrix4f, bufferIn, false, 0, packedLightIn);
				}

				matrix.pop();
			}
		}
		
		@Override
		protected void preRenderCallback(MenuPlayerEntity entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
			matrixStackIn.scale(0.9375F, 0.9375F, 0.9375F);
		}

		@Override
		protected void applyRotations(MenuPlayerEntity entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
			float f = entityLiving.getSwimAnimation(partialTicks);
			if (entityLiving.isElytraFlying()) {
				super.applyRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
				float f1 = (float)entityLiving.getTicksElytraFlying() + partialTicks;
				float f2 = MathHelper.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
				if (!entityLiving.isSpinAttacking()) {
					matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f2 * (-90.0F - entityLiving.rotationPitch)));
				}

				Vector3d vector3d = entityLiving.getLook(partialTicks);
				Vector3d vector3d1 = entityLiving.getMotion();
				double d0 = Entity.horizontalMag(vector3d1);
				double d1 = Entity.horizontalMag(vector3d);
				if (d0 > 0.0D && d1 > 0.0D) {
					double d2 = (vector3d1.x * vector3d.x + vector3d1.z * vector3d.z) / Math.sqrt(d0 * d1);
					double d3 = vector3d1.x * vector3d.z - vector3d1.z * vector3d.x;
					matrixStackIn.rotate(Vector3f.YP.rotation((float)(Math.signum(d3) * Math.acos(d2))));
				}
			} else if (f > 0.0F) {
				super.applyRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
				float f3 = entityLiving.isInWater() ? -90.0F - entityLiving.rotationPitch : -90.0F;
				float f4 = MathHelper.lerp(f, 0.0F, f3);
				matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f4));
				if (entityLiving.isActualySwimming()) {
					matrixStackIn.translate(0.0D, -1.0D, (double)0.3F);
				}
			} else {
				super.applyRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
			}
		}

	}
	
	public static class MenuPlayerEntity extends PlayerEntity {

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
			super(DUMMY_WORLD, BLOCK_POS, 0, new GameProfile(PlayerEntity.getOfflineUUID(getRawPlayerName(playerName)), getRawPlayerName(playerName)));
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
		public boolean isCrouching() {
			return this.crouching;
		}
		
		@Override
		public ITextComponent getDisplayName() {
			if (this.playerName != null) {
				return new StringTextComponent(this.playerName);
			}
			return null;
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

	public static class MenuPlayerCapeLayer extends LayerRenderer<MenuPlayerEntity, PlayerModel<MenuPlayerEntity>> {

		public MenuPlayerCapeLayer(IEntityRenderer<MenuPlayerEntity, PlayerModel<MenuPlayerEntity>> entityRendererIn) {
			super(entityRendererIn);
		}

		@Override
		public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, MenuPlayerEntity playerEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
			if (playerEntity.hasCape()) {
				matrixStackIn.push();
				matrixStackIn.translate(0.0D, 0.0D, 0.125D);
				double d0 = MathHelper.lerp((double)partialTicks, playerEntity.prevChasingPosX, playerEntity.chasingPosX) - MathHelper.lerp((double)partialTicks, playerEntity.prevPosX, playerEntity.getPosX());
				double d1 = MathHelper.lerp((double)partialTicks, playerEntity.prevChasingPosY, playerEntity.chasingPosY) - MathHelper.lerp((double)partialTicks, playerEntity.prevPosY, playerEntity.getPosY());
				double d2 = MathHelper.lerp((double)partialTicks, playerEntity.prevChasingPosZ, playerEntity.chasingPosZ) - MathHelper.lerp((double)partialTicks, playerEntity.prevPosZ, playerEntity.getPosZ());
				float f = playerEntity.prevRenderYawOffset + (playerEntity.renderYawOffset - playerEntity.prevRenderYawOffset);
				double d3 = (double)MathHelper.sin(f * ((float)Math.PI / 180F));
				double d4 = (double)(-MathHelper.cos(f * ((float)Math.PI / 180F)));
				float f1 = (float)d1 * 10.0F;
	            f1 = MathHelper.clamp(f1, -6.0F, 32.0F);
				float f2 = (float)(d0 * d3 + d2 * d4) * 100.0F;
				f2 = MathHelper.clamp(f2, 0.0F, 150.0F);

				float f4 = MathHelper.lerp(partialTicks, playerEntity.prevCameraYaw, playerEntity.cameraYaw);
				f1 = f1 + MathHelper.sin(MathHelper.lerp(partialTicks, playerEntity.prevDistanceWalkedModified, playerEntity.distanceWalkedModified) * 6.0F) * 32.0F * f4;
				if (playerEntity.isCrouching()) {
					f1 += 25.0F;
				}
				
				//vertikale neigung
				matrixStackIn.rotate(Vector3f.XP.rotationDegrees(6.0F + f2 / 2.0F + f1));

				matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(0.0F));

				matrixStackIn.rotate(Vector3f.YP.rotationDegrees(180.0F));
				
				IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.getEntitySolid(playerEntity.getCape()));
				this.getEntityModel().renderCape(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY);
				matrixStackIn.pop();
			}
		}
	}

	public static class MenuPlayerParrotLayer extends LayerRenderer<MenuPlayerEntity, PlayerModel<MenuPlayerEntity>> {

		private final ParrotModel parrotModel = new ParrotModel();

		public MenuPlayerParrotLayer(IEntityRenderer<MenuPlayerEntity, PlayerModel<MenuPlayerEntity>> entityRendererIn) {
			super(entityRendererIn);
		}

		@Override
		public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, MenuPlayerEntity playerEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
			if (playerEntity.hasParrot) {
				this.renderParrot(matrixStackIn, bufferIn, packedLightIn, playerEntity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, 0);
			}
		}

		private void renderParrot(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, MenuPlayerEntity playerEntity, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, int parrotVariant) {
			matrixStackIn.push();
			matrixStackIn.translate((double)-0.4F, playerEntity.isCrouching() ? (double)-1.3F : -1.5D, 0.0D);
			IVertexBuilder ivertexbuilder = bufferIn.getBuffer(this.parrotModel.getRenderType(ParrotRenderer.PARROT_TEXTURES[parrotVariant]));
			this.parrotModel.renderOnShoulder(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, limbSwing, limbSwingAmount, netHeadYaw, headPitch, playerEntity.ticksExisted);
			matrixStackIn.pop();
		}
	}

}
