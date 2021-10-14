package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.resources.WebTextureResourceLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.ParrotEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.ParrotEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@SuppressWarnings("resource")
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

		String skin = item.getEntryValue("skinpath");
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
		
		String cape = item.getEntryValue("capepath");
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

		this.setWidth((int)(this.entity.getWidth()*this.scale));
		this.setHeight((int)(this.entity.getHeight()*this.scale));
		
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
					this.setWidth((int)(this.entity.getWidth()*this.scale));
					this.setHeight((int)(this.entity.getHeight()*this.scale));
					
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
		float rotationX = (float)Math.atan((double)((mouseX - item.getPosX(MinecraftClient.getInstance().currentScreen)) / 40.0F));
		float rotationY = (float)Math.atan((double)((mouseY - (item.getPosY(MinecraftClient.getInstance().currentScreen) - (entityHeight / 2))) / 40.0F));
		RenderSystem.pushMatrix();
		RenderSystem.translatef((float)posX, (float)posY, 1050.0F);
		RenderSystem.scalef(1.0F, 1.0F, -1.0F);
		MatrixStack matrix = new MatrixStack();
		matrix.translate(0.0D, 0.0D, 1000.0D);
		matrix.scale((float)scale, (float)scale, (float)scale);

		if (!item.autoRotation) {

			//vertical rotation body
			Quaternion q = Vector3f.POSITIVE_Z.getDegreesQuaternion(180.0F);
			Quaternion q2 = Vector3f.POSITIVE_X.getDegreesQuaternion(item.bodyRotationY);
			q.hamiltonProduct(q2);
			matrix.multiply(q);
			//-----------

			//horizontal rotation body
			item.entity.bodyYaw = item.bodyRotationX;

			//vertical rotation head
			item.entity.pitch = item.headRotationY;

			//horizontal rotation head
			item.entity.headYaw = item.headRotationX;

		} else {

			Quaternion q = Vector3f.POSITIVE_Z.getDegreesQuaternion(180.0F);
			Quaternion q2 = Vector3f.POSITIVE_X.getDegreesQuaternion(Math.negateExact((long) (rotationY * 20.0F)));
			q.hamiltonProduct(q2);
			matrix.multiply(q);

			item.entity.bodyYaw = Math.negateExact((long)(180.0F + rotationX * 20.0F));

			item.entity.pitch = Math.negateExact((long)(-rotationY * 20.0F));

			item.entity.headYaw = Math.negateExact((long)(180.0F + rotationX * 40.0F));

		}

		VertexConsumerProvider.Immediate rb = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
		RenderSystem.runAsFancy(() -> {
			item.renderEntityStatic(0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrix, rb, 15728880);
		});
		rb.draw();

		RenderSystem.popMatrix();
	}
	
	public void renderEntityStatic(double xIn, double yIn, double zIn, float rotationYawIn, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
		try {
			
			Vec3d vector3d;
			if (this.entity.isSlimSkin()) {
				vector3d = SLIM_PLAYER_RENDERER.getPositionOffset(this.entity, partialTicks);
			} else {
				vector3d = PLAYER_RENDERER.getPositionOffset(this.entity, partialTicks);
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

	public static class MenuPlayerRenderer extends LivingEntityRenderer<MenuPlayerEntity, PlayerEntityModel<MenuPlayerEntity>> {

		@SuppressWarnings("rawtypes")
		public MenuPlayerRenderer(boolean useSmallArms) {
			super(MinecraftClient.getInstance().getEntityRenderDispatcher(), new PlayerEntityModel<>(0.0F, useSmallArms), 0.5F);
			this.addFeature(new ArmorFeatureRenderer(this, new BipedEntityModel(0.5F), new BipedEntityModel(1.0F)));
			this.addFeature(new MenuPlayerCapeLayer(this));
			this.addFeature(new HeadFeatureRenderer(this));
			this.addFeature(new MenuPlayerParrotLayer(this));
		}

		@Override
		public void render(MenuPlayerEntity entityIn, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
			this.setModelVisibilities(entityIn);
			super.render(entityIn, f, g, matrixStack, vertexConsumerProvider, i);
		}

		@Override
		public Vec3d getPositionOffset(MenuPlayerEntity playerEntity, float f) {
			return playerEntity.isInSneakingPose() ? new Vec3d(0.0D, -0.125D, 0.0D) : super.getPositionOffset(playerEntity, f);
		}

		private void setModelVisibilities(MenuPlayerEntity clientPlayer) {
			PlayerEntityModel<MenuPlayerEntity> playermodel = this.getModel();
			playermodel.setVisible(true);
			playermodel.head.visible = true;
			playermodel.torso.visible = true;
			playermodel.leftLeg.visible = true;
			playermodel.rightLeg.visible = true;
			playermodel.leftArm.visible = true;
			playermodel.rightArm.visible = true;
			playermodel.sneaking = clientPlayer.isSneaking();
		}

		@Override
		public Identifier getTexture(MenuPlayerEntity entity) {
			Identifier l = entity.getSkin();
			if (l != null) {
				return l;
			}
			return DefaultSkinHelper.getTexture();
		}

		@Override
		protected boolean hasLabel(MenuPlayerEntity entity) {
			if (entity.showName) {
				if (entity.getDisplayName() != null) {
					return true;
				}
			}
			return false;
		}

		@Override
		protected void renderLabelIfPresent(MenuPlayerEntity playerEntity, Text displayNameIn, MatrixStack matrix, VertexConsumerProvider bufferIn, int packedLightIn) {
			if (playerEntity.showName) {
				boolean flag = !playerEntity.isSneaky();
				float f = playerEntity.getHeight() + 0.5F;
				matrix.push();
				matrix.translate(0.0D, (double)f, 0.0D);
				matrix.multiply(new Quaternion(0, 0, 0, 0));
				matrix.scale(-0.025F, -0.025F, 0.025F);
				Matrix4f matrix4f = matrix.peek().getModel();
				float f1 = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
				int j = (int)(f1 * 255.0F) << 24;
				TextRenderer fontrenderer = this.getFontRenderer();
				float f2 = (float)(-fontrenderer.getWidth(displayNameIn) / 2);
				fontrenderer.draw(displayNameIn, f2, 0, 553648127, false, matrix4f, bufferIn, flag, j, packedLightIn);
				if (flag) {
					fontrenderer.draw(displayNameIn, f2, 0, -1, false, matrix4f, bufferIn, false, 0, packedLightIn);
				}

				matrix.pop();
			}
		}
		
		@Override
		protected void scale(MenuPlayerEntity entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
			matrixStackIn.scale(0.9375F, 0.9375F, 0.9375F);
		}

		@Override
		protected void setupTransforms(MenuPlayerEntity entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
			float f = entityLiving.getLeaningPitch(partialTicks);
			if (entityLiving.isFallFlying()) {
				super.setupTransforms(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
				float f1 = (float)entityLiving.getRoll() + partialTicks;
				float f2 = MathHelper.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
				if (!entityLiving.isUsingRiptide()) {
					matrixStackIn.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(f2 * (-90.0F - entityLiving.pitch)));
				}

				Vec3d vector3d = entityLiving.getRotationVec(partialTicks);
				Vec3d vector3d1 = entityLiving.getVelocity();
				double d0 = Entity.squaredHorizontalLength(vector3d1);
				double d1 = Entity.squaredHorizontalLength(vector3d);
				if (d0 > 0.0D && d1 > 0.0D) {
					double d2 = (vector3d1.x * vector3d.x + vector3d1.z * vector3d.z) / Math.sqrt(d0 * d1);
					double d3 = vector3d1.x * vector3d.z - vector3d1.z * vector3d.x;
					matrixStackIn.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion((float)(Math.signum(d3) * Math.acos(d2))));
				}
			} else if (f > 0.0F) {
				super.setupTransforms(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
				float f3 = entityLiving.isTouchingWater() ? -90.0F - entityLiving.pitch : -90.0F;
				float f4 = MathHelper.lerp(f, 0.0F, f3);
				matrixStackIn.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(f4));
				if (entityLiving.isInSwimmingPose()) {
					matrixStackIn.translate(0.0D, -1.0D, (double)0.3F);
				}
			} else {
				super.setupTransforms(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
			}
		}

	}
	
	public static class MenuPlayerEntity extends PlayerEntity {

		public volatile Identifier skinLocation;
		public volatile Identifier capeLocation;
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
			super(DUMMY_WORLD, BLOCK_POS, 0, new GameProfile(UUID.randomUUID(), getRawPlayerName(playerName)));
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
		public Text getDisplayName() {
			if (this.playerName != null) {
				return new LiteralText(this.playerName);
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
			return (this.skinLocation != DefaultSkinHelper.getTexture());
		}
		
		public boolean hasCape() {
			return (this.getCape() != null);
		}
		
		public Identifier getSkin() {
			
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
													skinLocation = DefaultSkinHelper.getTexture();
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
																		skinLocation = DefaultSkinHelper.getTexture();
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
								return DefaultSkinHelper.getTexture();
							}
						} else {
							skinLocation = PlayerEntityCache.getSkin(playerName);
							skinChecked = true;
						}
					} else {
						this.skinLocation = DefaultSkinHelper.getTexture();
						this.slimSkin = false;
						this.slimSkinChecked = true;
					}
				}
			} else if (this.skinLocation == null) {
				this.skinLocation = DefaultSkinHelper.getTexture();
				this.slimSkin = false;
				this.slimSkinChecked = true;
			}
			
			return this.skinLocation;
		}
		
		public Identifier getCape() {
			
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

	public static class MenuPlayerCapeLayer extends FeatureRenderer<MenuPlayerEntity, PlayerEntityModel<MenuPlayerEntity>> {

		public MenuPlayerCapeLayer(FeatureRendererContext<MenuPlayerEntity, PlayerEntityModel<MenuPlayerEntity>> entityRendererIn) {
			super(entityRendererIn);
		}

		@Override
		public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, MenuPlayerEntity playerEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
			if (playerEntity.hasCape()) {
				matrixStackIn.push();
				matrixStackIn.translate(0.0D, 0.0D, 0.125D);
				double d0 = MathHelper.lerp((double)partialTicks, playerEntity.prevCapeX, playerEntity.capeX) - MathHelper.lerp((double)partialTicks, playerEntity.prevX, playerEntity.getX());
				double d1 = MathHelper.lerp((double)partialTicks, playerEntity.prevCapeY, playerEntity.capeY) - MathHelper.lerp((double)partialTicks, playerEntity.prevY, playerEntity.getY());
				double d2 = MathHelper.lerp((double)partialTicks, playerEntity.prevCapeZ, playerEntity.capeZ) - MathHelper.lerp((double)partialTicks, playerEntity.prevZ, playerEntity.getZ());
				float f = playerEntity.prevBodyYaw + (playerEntity.bodyYaw - playerEntity.prevBodyYaw);
				double d3 = (double)MathHelper.sin(f * ((float)Math.PI / 180F));
				double d4 = (double)(-MathHelper.cos(f * ((float)Math.PI / 180F)));
				float f1 = (float)d1 * 10.0F;
				f1 = MathHelper.clamp(f1, -6.0F, 32.0F);
				float f2 = (float)(d0 * d3 + d2 * d4) * 100.0F;
				f2 = MathHelper.clamp(f2, 0.0F, 150.0F);

				float f4 = MathHelper.lerp(partialTicks, playerEntity.prevStrideDistance, playerEntity.strideDistance);
				f1 = f1 + MathHelper.sin(MathHelper.lerp(partialTicks, playerEntity.prevHorizontalSpeed, playerEntity.horizontalSpeed) * 6.0F) * 32.0F * f4;
				if (playerEntity.isSneaking()) {
					f1 += 25.0F;
				}

				//vertikale neigung
				matrixStackIn.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(6.0F + f2 / 2.0F + f1));

				matrixStackIn.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(0.0F));

				matrixStackIn.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180.0F));

				VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntitySolid(playerEntity.getCape()));
				this.getContextModel().renderCape(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV);
				matrixStackIn.pop();
			}
		}
	}

	public static class MenuPlayerParrotLayer extends FeatureRenderer<MenuPlayerEntity, PlayerEntityModel<MenuPlayerEntity>> {

		private final ParrotEntityModel parrotModel = new ParrotEntityModel();

		public MenuPlayerParrotLayer(FeatureRendererContext<MenuPlayerEntity, PlayerEntityModel<MenuPlayerEntity>> entityRendererIn) {
			super(entityRendererIn);
		}

		@Override
		public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, MenuPlayerEntity playerEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
			if (playerEntity.hasParrot) {
				this.renderParrot(matrixStackIn, bufferIn, packedLightIn, playerEntity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, 0);
			}
		}

		private void renderParrot(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, MenuPlayerEntity playerEntity, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, int parrotVariant) {
			matrixStackIn.push();
			matrixStackIn.translate((double)-0.4F, playerEntity.isSneaking() ? (double)-1.3F : -1.5D, 0.0D);
			VertexConsumer ivertexbuilder = bufferIn.getBuffer(this.parrotModel.getLayer(ParrotEntityRenderer.TEXTURES[parrotVariant]));
			this.parrotModel.poseOnShoulder(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, limbSwing, limbSwingAmount, netHeadYaw, headPitch, playerEntity.age);
			matrixStackIn.pop();
		}
	}

}
