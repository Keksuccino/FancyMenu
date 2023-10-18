package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class RenderListBackgroundEvent extends EventBase {
	
	protected AbstractSelectionList<?> list;
	protected GuiGraphics graphics;
	
	public RenderListBackgroundEvent(@NotNull GuiGraphics graphics, @NotNull AbstractSelectionList<?> list) {
		this.list = Objects.requireNonNull(list);
		this.graphics = Objects.requireNonNull(graphics);
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public AbstractSelectionList<?> getList() {
		return this.list;
	}

	public GuiGraphics getGuiGraphics() {
		return this.graphics;
	}

	public PoseStack getPoseStack() {
		return this.graphics.pose();
	}

	@Deprecated
	public PoseStack getMatrixStack() {
		return this.graphics.pose();
	}

	public static class Pre extends RenderListBackgroundEvent {

		public Pre(@NotNull GuiGraphics graphics, @NotNull AbstractSelectionList<?> list) {
			super(graphics, list);
		}

	}

	public static class Post extends RenderListBackgroundEvent {

		public Post(@NotNull GuiGraphics graphics, @NotNull AbstractSelectionList<?> list) {
			super(graphics, list);
		}

	}
	
}
