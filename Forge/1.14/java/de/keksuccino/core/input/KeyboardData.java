package de.keksuccino.core.input;

public class KeyboardData {
	
	public final int keycode;
	public final int scancode;
	public final int modfiers;
	
	public KeyboardData(int keycode, int scancode, int modifiers) {
		this.keycode = keycode;
		this.scancode = scancode;
		this.modfiers = modifiers;
	}

}
