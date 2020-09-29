//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

public interface ILayoutButton {
	
	public void setAppearanceDelay(String sec, boolean onlyfirsttime);
	
	public boolean isDelayedOnlyFirstTime();
	
	public double getAppearanceDelay();

}
