package de.keksuccino.core.gui.content;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import de.keksuccino.core.input.CharacterFilter;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.math.MathUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class AdvancedTextField extends GuiTextField {

	private int tick = 0;
	private boolean handle;
	private CharacterFilter filter;
	private boolean leftDown = false;
	
	public AdvancedTextField(FontRenderer fontrenderer, int x, int y, int width, int height, boolean handleTextField, @Nullable CharacterFilter filter) {
		super(MathUtils.getRandomNumberInRange(50, 400), fontrenderer, x, y, width, height);
		this.handle = handleTextField;
		this.filter = filter;
		
		if (this.handle) {
			KeyboardHandler.addKeyPressedListener(this::onKeyPress);
		}
	}
	
	public boolean isHovered() {
		int mouseX = MouseInput.getMouseX();
		int mouseY = MouseInput.getMouseY();
		if ((mouseX >= this.x) && (mouseX <= this.x + this.width) && (mouseY >= this.y) && mouseY <= this.y + this.height) {
			return true;
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
		this.moveCursorBy(i - this.getSelectionEnd() + l);
		this.setResponderEntryValue(this.getId(), this.getText());
	}
	
	public boolean isEnabled() {
		try {
			Field f = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiTextField.class, "field_146226_p", "isEnabled");
			return f.getBoolean(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean isLeftClicked() {
		return (this.isHovered() && MouseInput.isLeftMouseDown());
	}
	
	@Override
	public void drawTextBox() {
		super.drawTextBox();
		
		if (this.handle) {
			if (this.tick > 7) {
				this.updateCursorCounter();
				this.tick = 0;
			} else {
				tick++;
			}
			
			if (MouseInput.isLeftMouseDown() && !this.leftDown) {
				super.mouseClicked(MouseInput.getMouseX(), MouseInput.getMouseY(), 0);
				this.leftDown = true;
			}
			if (!MouseInput.isLeftMouseDown()) {
				this.leftDown = false;
			}
		}
	}
	
	@Override
	public boolean textboxKeyTyped(char typedChar, int keyCode) {
		if (!this.isFocused()) {
			return false;
		} else if (GuiScreen.isKeyComboCtrlA(keyCode)) {
			this.setCursorPositionEnd();
			this.setSelectionPos(0);
			return true;
		} else if (GuiScreen.isKeyComboCtrlC(keyCode)) {
			GuiScreen.setClipboardString(this.getSelectedText());
			return true;
		} else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
			if (this.isEnabled()) {
				this.writeText(GuiScreen.getClipboardString());
			}

			return true;
		} else if (GuiScreen.isKeyComboCtrlX(keyCode)) {
			GuiScreen.setClipboardString(this.getSelectedText());

			if (this.isEnabled()) {
				this.writeText("");
			}

			return true;
		} else {
			switch (keyCode) {
			case 14:

				if (GuiScreen.isCtrlKeyDown()) {
					if (this.isEnabled()) {
						this.deleteWords(-1);
					}
				} else if (this.isEnabled()) {
					this.deleteFromCursor(-1);
				}

				return true;
			case 199:

				if (GuiScreen.isShiftKeyDown()) {
					this.setSelectionPos(0);
				} else {
					this.setCursorPositionZero();
				}

				return true;
			case 203:

				if (GuiScreen.isShiftKeyDown()) {
					if (GuiScreen.isCtrlKeyDown()) {
						this.setSelectionPos(this.getNthWordFromPos(-1, this.getSelectionEnd()));
					} else {
						this.setSelectionPos(this.getSelectionEnd() - 1);
					}
				} else if (GuiScreen.isCtrlKeyDown()) {
					this.setCursorPosition(this.getNthWordFromCursor(-1));
				} else {
					this.moveCursorBy(-1);
				}

				return true;
			case 205:

				if (GuiScreen.isShiftKeyDown()) {
					if (GuiScreen.isCtrlKeyDown()) {
						this.setSelectionPos(this.getNthWordFromPos(1, this.getSelectionEnd()));
					} else {
						this.setSelectionPos(this.getSelectionEnd() + 1);
					}
				} else if (GuiScreen.isCtrlKeyDown()) {
					this.setCursorPosition(this.getNthWordFromCursor(1));
				} else {
					this.moveCursorBy(1);
				}

				return true;
			case 207:

				if (GuiScreen.isShiftKeyDown()) {
					this.setSelectionPos(this.getText().length());
				} else {
					this.setCursorPositionEnd();
				}

				return true;
			case 211:

				if (GuiScreen.isCtrlKeyDown()) {
					if (this.isEnabled()) {
						this.deleteWords(1);
					}
				} else if (this.isEnabled()) {
					this.deleteFromCursor(1);
				}

				return true;
			default:

				if ((this.filter == null) || this.filter.isAllowed(typedChar)) {
					if (this.isEnabled()) {
						this.writeText(Character.toString(typedChar));
					}

					return true;
				} else {
					return false;
				}
			}
		}
	}
	
	public void onKeyPress(KeyboardData d) {
		this.textboxKeyTyped(d.typedChar, d.keycode);
	}

}
