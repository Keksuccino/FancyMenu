package de.keksuccino.core.input;

public class CharData {
	
	public final char typedChar;
	public final int modfiers;
	
	public CharData(char c, int modifiers) {
		this.modfiers = modifiers;
		this.typedChar = c;
	}

}
