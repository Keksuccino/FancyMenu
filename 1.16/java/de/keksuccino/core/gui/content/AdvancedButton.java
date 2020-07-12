package de.keksuccino.core.gui.content;

import java.awt.Color;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.resources.ExternalTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;

public class AdvancedButton extends Button {

	private boolean handleClick = false;
	private static boolean leftDown = false;
	private boolean leftDownNotHovered = false;
	public boolean ignoreBlockedInput = false;
	private boolean useable = true;
	
	private Color idleColor;
	private Color hoveredColor;
	private Color idleBorderColor;
	private Color hoveredBorderColor;
	private int borderWidth = 2;
	private ResourceLocation backgroundHover;
	private ResourceLocation backgroundNormal;
	
	public AdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText, IPressable onPress) {
		super(x, y, widthIn, heightIn, new StringTextComponent(buttonText), onPress);
	}
	
	public AdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText, boolean handleClick, IPressable onPress) {
		super(x, y, widthIn, heightIn, new StringTextComponent(buttonText), onPress);
		this.handleClick = handleClick;
	}
	
	//renderButton
	@Override
	public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		if (this.visible) {
			Minecraft mc = Minecraft.getInstance();
			FontRenderer font = mc.fontRenderer;
			
			//Widget:
			//field_230692_n_ = isHovered
			//func_230449_g_() = isHovered()
			//field_230690_l_ = x
			//field_230691_m_ = y
			//field_230688_j_ = width
			//field_230689_k_ = height
			//field_230694_p_ = visible
			//func_230989_a_() = getYImage()
			//func_238472_a_() = drawCenteredString
			//func_230458_i_() = getMessage
			//func_238482_a_() = setMessage
			//func_230998_h_() = getWidth
			//func_230991_b_() = setWidth
			//func_230982_a_() = onClick
			//func_230431_b_() = renderButton
			//func_230430_a_() = render
			
			//FontRenderer:
			//func_238405_a_() = drawStringWithShadow
			//func_238421_b_() = drawString
			//func_238412_a_() = trimStringToWidth
			
			//Screen & AbstractGUI:
			//func_230430_a_() = render
			//func_230446_a_() = renderBackground
			//func_231178_ax__() = shouldCloseOnEsc
			//func_238467_a_() = fill()
			//func_238463_a_() = blit(int, int, float, float, int, int, int, int)
			//field_230708_k_ = width
			//field_230709_l_ = height
			//func_231160_c_() = init
			//func_231039_at__() = children
			//func_231171_q_() = getTitle
			
			//AbstractList:
			//func_230513_b_() = addEntry
			//func_230958_g_() = getSelected
			//func_230951_c_() = centerScrollOn
			//func_230966_l_() = getScrollAmount
			//func_230937_a_() = scroll
			//func_230932_a_() = setScrollAmount
			//func_230962_i_() = getRowTop
			//func_230968_n_() = getRowLeft
			//func_230948_b_() = getRowBottom
			//field_230670_d_ = width
			//field_230671_e_ = height
			//field_230672_i_ = y0
			//field_230673_j_ = y1
			//field_230674_k_ = x1
			//field_230675_l_ = x0
			
			this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
			
			RenderSystem.enableBlend();
			if (this.hasColorBackground()) {
				Color border;
				if (!isHovered) {
					fill(matrix, this.x, this.y, this.x + this.width, this.y + this.height, this.idleColor.getRGB());
					border = this.idleBorderColor;
				} else {
					fill(matrix, this.x, this.y, this.x + this.width, this.y + this.height, this.hoveredColor.getRGB());
					border = this.hoveredBorderColor;
				}
				if (this.hasBorder()) {
					//top
					fill(matrix, this.x, this.y, this.x + this.width, this.y + this.borderWidth, border.getRGB());
					//bottom
					fill(matrix, this.x, this.y + this.height - this.borderWidth, this.x + this.width, this.y + this.height, border.getRGB());
					//left
					fill(matrix, this.x, this.y + this.borderWidth, this.x + this.borderWidth, this.y + this.height - this.borderWidth, border.getRGB());
					//right
					fill(matrix, this.x + this.width - this.borderWidth, this.y + this.borderWidth, this.x + this.width, this.y + this.height - this.borderWidth, border.getRGB());
				}
			} else if (this.hasCustomTextureBackground()) {
				if (this.isHovered()) {
					mc.textureManager.bindTexture(backgroundHover);
				} else {
					mc.textureManager.bindTexture(backgroundNormal);
				}
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				blit(matrix, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
			} else {
				mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				int i = this.getYImage(this.isHovered());
				RenderSystem.defaultBlendFunc();
				RenderSystem.enableDepthTest();
				this.blit(matrix, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
				this.blit(matrix, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
				RenderSystem.disableDepthTest();
			}
			
			//func_230441_a_ = renderBg
			this.renderBg(matrix, mc, mouseX, mouseY);

			this.drawCenteredString(matrix, font, new StringTextComponent(getMessageString()), this.x + this.width / 2, this.y + (this.height - 8) / 2, getFGColor());
		}

		if (!this.isHovered() && MouseInput.isLeftMouseDown()) {
			this.leftDownNotHovered = true;
		}
		if (!MouseInput.isLeftMouseDown()) {
			this.leftDownNotHovered = false;
		}
		
		if (this.handleClick && this.useable) {
			if (this.isHovered() && MouseInput.isLeftMouseDown() && !leftDown && !leftDownNotHovered && !this.isInputBlocked()) {
				this.onClick(mouseX, mouseY);
				this.playDownSound(Minecraft.getInstance().getSoundHandler());
				leftDown = true;
			}
			if (!MouseInput.isLeftMouseDown()) {
				leftDown = false;
			}
		}
	}
	
	private boolean isInputBlocked() {
		if (this.ignoreBlockedInput) {
			return false;
		}
		return MouseInput.isVanillaInputBlocked();
	}
	
	public void setBackgroundColor(@Nullable Color idle, @Nullable Color hovered, @Nullable Color idleBorder, @Nullable Color hoveredBorder, int borderWidth) {
		this.idleColor = idle;
		this.hoveredColor = hovered;
		this.hoveredBorderColor = hoveredBorder;
		this.idleBorderColor = idleBorder;
		
		if (borderWidth >= 0) {
			this.borderWidth = borderWidth;
		} else {
			borderWidth = 0;
		}
	}
	
	public void setBackgroundTexture(ResourceLocation normal, ResourceLocation hovered) {
		this.backgroundNormal = normal;
		this.backgroundHover = hovered;
	}
	
	public void setBackgroundTexture(ExternalTextureResourceLocation normal, ExternalTextureResourceLocation hovered) {
		if (!normal.isReady()) {
			normal.loadTexture();
		}
		if (!hovered.isReady()) {
			hovered.loadTexture();
		}
		this.backgroundHover = hovered.getResourceLocation();
		this.backgroundNormal = normal.getResourceLocation();
	}
	
	public boolean hasBorder() {
		return (this.hasColorBackground() && (this.idleBorderColor != null) && (this.hoveredBorderColor != null));
	}
	
	public boolean hasColorBackground() {
		return ((this.idleColor != null) && (this.hoveredColor != null));
	}
	
	public boolean hasCustomTextureBackground() {
		return ((this.backgroundHover != null) && (this.backgroundNormal != null));
	}
	
	//mouseClicked
	@Override
	public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
		if (!this.handleClick && this.useable) {
			return super.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
		}
		return false;
	}
	
	//keyPressed
	@Override
	public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (this.handleClick) {
			return false;
		}
		return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
	}
	
	public void setUseable(boolean b) {
		this.useable = b;
	}
	
	public boolean isUseable() {
		return this.useable;
	}
	
	public void setHandleClick(boolean b) {
		this.handleClick = b;
	}
	
	public String getMessageString() {
		return this.getMessage().getString();
	}
	
	public void setMessage(String msg) {
		this.setMessage(new StringTextComponent(msg));
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public int getX() {
		return this.x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return this.y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public void setHovered(boolean b) {
		this.isHovered = b;
	}
	
	public static boolean isAnyButtonLeftClicked() {
		return leftDown;
	}

}
