package de.keksuccino.core.gui.screens.popup;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.io.Files;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.scrollarea.ScrollArea;
import de.keksuccino.core.gui.content.scrollarea.ScrollAreaEntry;
import de.keksuccino.core.gui.screens.popup.FilePickerPopup.FileChooserEntry.Type;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.fancymenu.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;

public class FilePickerPopup extends Popup {

	private static ResourceLocation fileIcon = new ResourceLocation("keksuccino", "filechooser/file_icon.png");
	private static ResourceLocation folderIcon = new ResourceLocation("keksuccino", "filechooser/folder_icon.png");
	private static ResourceLocation backIcon = new ResourceLocation("keksuccino", "filechooser/back_icon.png");
	
	public Color overlayColor = new Color(26, 26, 26);
	private ScrollArea scroll;
	public File directory;
	public FilePickerPopup parent;
	private Popup fallback;
	private List<String> filetypes = new ArrayList<String>();
	private Consumer<File> callback;
	
	private int lastWidth = 0;
	private int lastHeight = 0;
	
	private AdvancedButton chooseButton;
	private AdvancedButton closeButton;
	
	private FileChooserEntry focused;
	
	public FilePickerPopup(String directory, @Nullable FilePickerPopup parent, @Nullable Popup fallback, Consumer<File> callback, @Nullable String... filetypes) {
		super(240);
		this.fallback = fallback;
		this.parent = parent;
		this.directory = new File(directory);
		this.callback = callback;
		if (filetypes != null) {
			for (String s : filetypes) {
				this.filetypes.add(s.toLowerCase());
			}
		}
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
		
		this.updateFileList();
		
		this.chooseButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.choosefile.choose"), true, (press) -> {
			if (this.focused != null) {
				this.focused.onClick();
			}
		});
		this.addButton(this.chooseButton);
		this.colorizePopupButton(this.chooseButton);
		
		this.closeButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.yesno.cancel"), true, (press) -> {
			if (this.callback != null) {
				this.callback.accept(null);
			}
			this.setDisplayed(false);
			if (this.fallback != null) {
				PopupHandler.displayPopup(this.fallback);
			}
		});
		this.addButton(this.closeButton);
		this.colorizePopupButton(this.closeButton);
	}
	
	public FilePickerPopup(String directory, @Nullable FilePickerPopup parent, @Nullable Popup fallback, Consumer<File> callback) {
		this(directory, parent, fallback, callback, (String[])null);
	}
	
	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, Screen renderIn) {
		super.render(matrix, mouseX, mouseY, renderIn);
		
		if ((lastWidth != renderIn.width) || (lastHeight != renderIn.height)) {
			this.updateFileList();
			this.focused = null;
		}
		this.lastWidth = renderIn.width;
		this.lastHeight = renderIn.height;
		
		this.scroll.height = renderIn.height - 100;
		this.scroll.y = 40;
		this.scroll.x = (renderIn.width / 2) - (this.scroll.width / 2);
		this.scroll.render(matrix);
		
		//Draw top and bottom overlay
		fill(matrix, 0, 0, renderIn.width, 40, this.overlayColor.getRGB());
		fill(matrix, 0, renderIn.height - 60, renderIn.width, renderIn.height, this.overlayColor.getRGB());
		
		drawCenteredString(matrix, Minecraft.getInstance().fontRenderer, "§l" + Locals.localize("popup.choosefile.title"), renderIn.width / 2, 17, Color.WHITE.getRGB());
		
		this.chooseButton.x = (renderIn.width / 2) - this.chooseButton.getWidth() - 5;
		this.chooseButton.y = renderIn.height - 40;
		
		this.closeButton.x = (renderIn.width / 2) + 5;
		this.closeButton.y = renderIn.height - 40;
		
		this.renderButtons(matrix, mouseX, mouseY);
		
		//Reset focused entry
		if ((this.focused != null) && !this.focused.focused) {
			this.focused = null;
		}

	}
	
	public void updateFileList() {
		this.scroll = new ScrollArea(0, 0, 200, 0);
		this.scroll.backgroundColor = new Color(255, 255, 255, 20);
		
		if (this.directory.exists() && this.directory.isDirectory()) {
			
			if (this.parent != null) {
				this.scroll.addEntry(new FileChooserEntry(null, this, Type.BACK));
			}
			
			for (File f : this.directory.listFiles()) {
				if (f.isDirectory()) {
					this.scroll.addEntry(new FileChooserEntry(f, this, Type.FOLDER));
				} else {
					if (!this.filetypes.isEmpty()) {
						if (this.filetypes.contains(Files.getFileExtension(f.getName().toLowerCase()))) {
							this.scroll.addEntry(new FileChooserEntry(f, this, Type.FILE));
						}
					} else {
						this.scroll.addEntry(new FileChooserEntry(f, this, Type.FILE));
					}
				}
			}
		}
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 257) && this.isDisplayed()) {
			if (this.focused != null) {
				this.focused.onClick();
			}
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 256) && this.isDisplayed()) {
			if (this.callback != null) {
				this.callback.accept(null);
			}
			this.setDisplayed(false);
			if (this.fallback != null) {
				PopupHandler.displayPopup(this.fallback);
			}
		}
	}
	
	public static class FileChooserEntry extends ScrollAreaEntry {

		public File file;
		public Type type;
		public FilePickerPopup filechooser;
		
		private int clickTick = 0;
		private boolean clickPre = false;
		private boolean click = false;
		
		private boolean focused = false;
		
		public FileChooserEntry(File file, FilePickerPopup filechooser, Type type) {
			super(filechooser.scroll);
			this.file = file;
			this.type = type;
			this.filechooser = filechooser;
		}
		
		@Override
		public void render(MatrixStack matrix) {
			
			//Handle entry focus and prepare double-click
			if (this.isHovered() && this.isVisible() && MouseInput.isLeftMouseDown()) {
				this.focused = true;
				this.filechooser.focused = this;
				if (!this.click) {
					this.clickPre = true;
					this.clickTick = 0;
				}
			}
			if (!this.isHovered() && MouseInput.isLeftMouseDown()) {
				this.focused = false;
			}
			
			super.render(matrix);
		}

		@Override
		public void renderEntry(MatrixStack matrix) {
			RenderSystem.enableBlend();
			
			if (this.type == Type.FILE) {
				Minecraft.getInstance().getTextureManager().bindTexture(fileIcon);
			}
			if (this.type == Type.FOLDER) {
				Minecraft.getInstance().getTextureManager().bindTexture(folderIcon);
			}
			if (this.type == Type.BACK) {
				Minecraft.getInstance().getTextureManager().bindTexture(backIcon);
			}

			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			blit(matrix, this.x, this.y, 0.0F, 0.0F, 20, 20, 20, 20);
			
			if (this.type == Type.BACK) {
				Minecraft.getInstance().fontRenderer.drawStringWithShadow(matrix, Locals.localize("popup.choosefile.back"), this.x + 30, this.y + 7, Color.WHITE.getRGB());
			} else {
				Minecraft.getInstance().fontRenderer.drawStringWithShadow(matrix, this.file.getName(), this.x + 30, this.y + 7, Color.WHITE.getRGB());
			}
			
			//Handle double-click
			if (!MouseInput.isLeftMouseDown() && this.clickPre) {
				this.click = true;
				this.clickPre = false;
				this.clickTick = 0;
			}
			if (this.click) {
				if (this.clickTick < 15) {
					this.clickTick++;
				} else {
					this.click = false;
					this.clickTick = 0;
				}
				
				if (MouseInput.isLeftMouseDown() && this.isHovered()) {
					this.onClick();
					this.click = false;
					this.clickTick = 0;
				}
			}
			
			if (this.focused) {
				this.renderBorder(matrix);
			}
			
		}

		private void renderBorder(MatrixStack matrix) {
			//left
			fill(matrix, this.x, this.y, this.x + 1, this.y + this.getHeight(), Color.WHITE.getRGB());
			//right
			fill(matrix, this.x + this.getWidth() - 1, this.y, this.x + this.getWidth(), this.y + this.getHeight(), Color.WHITE.getRGB());
			//top
			fill(matrix, this.x, this.y, this.x + this.getWidth(), this.y + 1, Color.WHITE.getRGB());
			//bottom
			fill(matrix, this.x, this.y + this.getHeight() - 1, this.x + this.getWidth(), this.y + this.getHeight(), Color.WHITE.getRGB());
		}
		
		public void onClick() {
			if (this.type == Type.BACK) {
				if (this.filechooser.parent != null) {
					PopupHandler.displayPopup(this.filechooser.parent);
				}
			}
			if (this.type == Type.FOLDER) {
				PopupHandler.displayPopup(new FilePickerPopup(this.file.getPath(), this.filechooser, this.filechooser.fallback, this.filechooser.callback, this.filechooser.filetypes.toArray(new String[0])));
			}
			if (this.type == Type.FILE) {
				if (this.filechooser.callback != null) {
					this.filechooser.callback.accept(new File(this.file.getAbsolutePath().replace("\\", "/")));
				}
				this.filechooser.setDisplayed(false);
				if (this.filechooser.fallback != null) {
					PopupHandler.displayPopup(this.filechooser.fallback);
				}
			}
		}
		
		@Override
		public int getHeight() {
			return 20;
		}
		
		public static enum Type {
			FILE,
			FOLDER,
			BACK;
		}
		
	}

}
