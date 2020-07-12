package de.keksuccino.core.gui.content;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.core.input.CharData;
import de.keksuccino.core.input.CharacterFilter;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.reflection.ReflectionHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;

public class AdvancedTextField extends TextFieldWidget {

	private int tick = 0;
	private boolean handle;
	private CharacterFilter filter;
	private boolean leftDown = false;
	
	public AdvancedTextField(FontRenderer fontrenderer, int x, int y, int width, int height, boolean handleTextField, @Nullable CharacterFilter filter) {
		super(fontrenderer, x, y, width, height, new StringTextComponent(""));
		this.handle = handleTextField;
		this.filter = filter;
		
		if (this.handle) {
			KeyboardHandler.addKeyPressedListener(this::onKeyPress);
			KeyboardHandler.addKeyReleasedListener(this::onKeyReleased);
			KeyboardHandler.addCharTypedListener(this::onCharTyped);
		}
	}
	
	public boolean isHovered() {
		int mouseX = MouseInput.getMouseX();
		int mouseY = MouseInput.getMouseY();
		if ((mouseX >= this.getX()) && (mouseX <= this.getX() + this.getWidth()) && (mouseY >= this.getY()) && mouseY <= this.getY() + this.getHeight()) {
			return true;
		}
		return false;
	}
	
	//charTyped
	@Override
	public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
		if (this.getVisible() && this.isFocused()) {
			if ((this.filter == null) || this.filter.isAllowed(p_charTyped_1_)) {
				if (this.isEnabled()) {
					this.writeText(Character.toString(p_charTyped_1_));
				}
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
	
	@Override
	public void writeText(String textToWrite) {
		String s = "";
		String s1 = textToWrite;
		if (this.filter != null) {
			s1 = this.filter.filterForAllowedChars(textToWrite);
		}
		int i = this.getCursorPosition() < this.getSelectionEnd() ? this.getCursorPosition() : this.getSelectionEnd();
		int j = this.getCursorPosition() < this.getSelectionEnd() ? this.getSelectionEnd() : this.getCursorPosition();
		int k = this.getMaxStringLength() - this.getText().length() - (i - j);
		if (!this.getText().isEmpty()) {
			s = s + this.getText().substring(0, i);
		}

		int l;
		if (k < s1.length()) {
			s = s + s1.substring(0, k);
			l = k;
		} else {
			s = s + s1;
			l = s1.length();
		}

		if (!this.getText().isEmpty() && j < this.getText().length()) {
			s = s + this.getText().substring(j);
		}

		this.setText(s);
		this.clampCursorPosition(i + l);
		this.setSelectionPos(this.getCursorPosition());
		this.setResponderEntryValue(this.getText());
	}
	
	protected void setResponderEntryValue(String text) {
		try {
			Method m = ReflectionHelper.findMethod(TextFieldWidget.class, "func_212951_d", String.class);
			m.invoke(this, text);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public int getMaxStringLength() {
		try {
			Field f = ReflectionHelper.findField(TextFieldWidget.class, "field_146217_k");
			return f.getInt(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int getSelectionEnd() {
		try {
			Field f = ReflectionHelper.findField(TextFieldWidget.class, "field_146223_s");
			return f.getInt(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public boolean isEnabled() {
		try {
			Field f = ReflectionHelper.findField(TextFieldWidget.class, "field_146226_p");
			return f.getBoolean(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean isLeftClicked() {
		return (this.isHovered() && MouseInput.isLeftMouseDown());
	}
	
	//renderButton
	@Override
	public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float p_renderButton_3_) {
		super.renderButton(matrix, mouseX, mouseY, p_renderButton_3_);
		
		if (this.handle) {
			if (this.tick > 7) {
				this.tick();
				this.tick = 0;
			} else {
				tick++;
			}
			
			if (MouseInput.isLeftMouseDown() && !this.leftDown) {
				super.mouseClicked(mouseX, mouseY, 0);
				this.leftDown = true;
			}
			if (!MouseInput.isLeftMouseDown()) {
				this.leftDown = false;
			}
		}
	}
	
	public void onKeyPress(KeyboardData d) {
		super.keyPressed(d.keycode, d.scancode, d.modfiers);
	}
	
	public void onKeyReleased(KeyboardData d) {
		super.keyReleased(d.keycode, d.scancode, d.modfiers);
	}
	
	public void onCharTyped(CharData d) {
		this.charTyped(d.typedChar, d.modfiers);
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

	public void setFocused(boolean b) {
		this.setFocused(b);
	}

}
