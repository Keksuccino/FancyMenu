package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.eventbus.api.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DrawWidgetBackgroundEvent extends Event {

	private static final Logger LOGGER = LogManager.getLogger();
	
	protected Widget widget;
	protected float alpha;
	protected MatrixStack matrix;

	/**
	 * DON'T USE THIS CONSTRUCTOR! JUST HERE TO FIX A BUG!
	 */
	public DrawWidgetBackgroundEvent() {

		LOGGER.error("[FANCYMENU] CONSTRUCTING DrawWidgetBackgroundEvent via INVALID non-parameter constructor! This shouldn't happen!");
		new Throwable().printStackTrace();

		this.alpha = 1;
		this.matrix = new MatrixStack();
		this.widget = new AdvancedButton(0,0,0,0,"",(press) -> {});

	}
	
	public DrawWidgetBackgroundEvent(MatrixStack matrix, Widget widget, float alpha) {
		this.widget = widget;
		this.alpha = alpha;
		this.matrix = matrix;
//		LOGGER.info("####################### CONSTRUCTING DrawWidgetBackgroundEvent via normal constructor! This is good!");
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public Widget getWidget() {
		return this.widget;
	}
	
	public float getAlpha() {
		return this.alpha;
	}
	
	public MatrixStack getMatrixStack() {
		return this.matrix;
	}
	
	public static class Pre extends DrawWidgetBackgroundEvent {

		public Pre(MatrixStack matrix, Widget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends DrawWidgetBackgroundEvent {

		public Post(MatrixStack matrix, Widget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
