package de.keksuccino.fancymenu.mainwindow;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;

public class AdvancedScaledResolution {
	
    private final double scaledWidthD;
    private final double scaledHeightD;
    private int scaleFactor;
    private int scaledWidth;
    private int scaledHeight;

    //TODO unicode müll
    public AdvancedScaledResolution(int scale) {
    	Minecraft mc = Minecraft.getMinecraft();
        int w = mc.displayWidth;
        int h = mc.displayHeight;
//        boolean flag = mc.isUnicode();

        if (scale <= 0) {
            scale = 1;
        }
        
        this.scaleFactor = scale;

        this.scaledWidthD = (double) w / (double) scale;
        this.scaledHeightD = (double) h / (double) scale;
        this.scaledWidth = MathHelper.ceil(this.scaledWidthD);
        this.scaledHeight = MathHelper.ceil(this.scaledHeightD);
    }
    
    public AdvancedScaledResolution() {
    	this(Minecraft.getMinecraft().gameSettings.guiScale);
	}

    public int getScaledWidth() {
        return this.scaledWidth;
    }

    public int getScaledHeight() {
        return this.scaledHeight;
    }

    public double getScaledWidth_double() {
        return this.scaledWidthD;
    }

    public double getScaledHeight_double() {
        return this.scaledHeightD;
    }

    public int getScaleFactor() {
        return this.scaleFactor;
    }
}