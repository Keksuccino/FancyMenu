package de.keksuccino.fancymenu;

import java.awt.Color;
import java.util.Optional;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.ITextProperties.IStyledTextAcceptor;
import net.minecraft.util.text.ITextProperties.ITextAcceptor;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Test {
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		
		FontRenderer font = Minecraft.getInstance().fontRenderer;
		MatrixStack matrix = e.getMatrixStack();
		final int zLevel = 400;
		
		RenderSystem.disableRescaleNormal();
        RenderSystem.disableDepthTest();
        
        matrix.push();
        
//        IRenderTypeBuffer.Impl renderType = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
        matrix.translate(0.0D, 0.0D, zLevel);
        
//        font.func_238416_a_(LanguageMap.getInstance().func_241870_a(""), (float)tooltipX, (float)tooltipY, -1, true, mat, renderType, false, 0, 15728880);
        font.drawStringWithShadow(e.getMatrixStack(), "dis a test boi", e.getGui().width - 80, e.getGui().height - 50, Color.white.getRGB());
        
        matrix.pop();
        
        RenderSystem.enableDepthTest();
        RenderSystem.enableRescaleNormal();
		
	}

}
