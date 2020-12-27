package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class EditHistory {
	
	private static final Identifier BACK = new Identifier("keksuccino", "arrow_left.png");
	private static final Identifier FORWARD = new Identifier("keksuccino", "arrow_right.png");
	
	protected LayoutCreatorScreen editor;
	private List<Snapshot> history = new ArrayList<Snapshot>();
	private int current = -1;
	
	private AdvancedButton backBtn;
	private AdvancedButton forwardBtn;
	
	public EditHistory(LayoutCreatorScreen editor) {
		
		this.editor = editor;
		
		this.backBtn = new AdvancedImageButton(10, 10, 19, 19, BACK, true, (press) -> {
			this.stepBack();
		});
		LayoutCreatorScreen.colorizeCreatorButton(this.backBtn);

		this.forwardBtn = new AdvancedImageButton(35, 10, 19, 19, FORWARD, true, (press) -> {
			this.stepForward();
		});
		LayoutCreatorScreen.colorizeCreatorButton(this.forwardBtn);
		
		KeyboardHandler.addKeyPressedListener(this::onCtrlAltZCtrlAltY);
		
	}
	
	public void saveSnapshot(Snapshot snap) {
		if (this.current < 0) {
			this.history.clear();
			this.history.add(snap);
			this.current = 0;
		} else {
			if (this.current <= this.history.size()-1) {
				List<Snapshot> l = new ArrayList<Snapshot>();
				int i = 0;
				while (i <= this.current) {
					l.add(this.history.get(i));
					i++;
				}
				l.add(snap);
				this.history = l;
				this.current = this.history.size()-1;
			} else {
				this.current = this.history.size()-1;
				this.saveSnapshot(snap);
			}
		}
	}
	
	public Snapshot createSnapshot(Runnable onSnapshotRestore) {
		return new Snapshot(editor, onSnapshotRestore);
	}
	
	public Snapshot createSnapshot() {
		return new Snapshot(editor, null);
	}
	
	public void stepBack() {
		if (this.current > -1) {
			
			if (this.current <= this.history.size()-1) {
				
				Snapshot snap = this.history.get(this.current);
				List<PropertiesSet> l = new ArrayList<PropertiesSet>();
				l.add(snap.snapshot);
				
				this.current--;
				
				snap.preSnapshotState = this.createSnapshot();
				
				PreloadedLayoutCreatorScreen neweditor = new PreloadedLayoutCreatorScreen(this.editor.screen, l);
				neweditor.history = this;
				String single = null;
				if (this.editor instanceof PreloadedLayoutCreatorScreen) {
					single = ((PreloadedLayoutCreatorScreen)this.editor).single;
				}
				neweditor.single = single;
				neweditor.expanded = this.editor.expanded;
				this.editor = neweditor;
				
				MinecraftClient.getInstance().openScreen(neweditor);
				
			}
			
		}
	}
	
	public void stepForward() {
		if (this.current >= -1) {
			
			if (this.current < this.history.size()-1) {

				this.current++;
				
				Snapshot snap = this.history.get(this.current).preSnapshotState;
				
				if (snap != null) {
					List<PropertiesSet> l = new ArrayList<PropertiesSet>();
					l.add(snap.snapshot);
					
					PreloadedLayoutCreatorScreen neweditor = new PreloadedLayoutCreatorScreen(this.editor.screen, l);
					neweditor.history = this;
					String single = null;
					if (this.editor instanceof PreloadedLayoutCreatorScreen) {
						single = ((PreloadedLayoutCreatorScreen)this.editor).single;
					}
					neweditor.single = single;
					neweditor.expanded = this.editor.expanded;
					this.editor = neweditor;
					
					MinecraftClient.getInstance().openScreen(neweditor);
				}
				
			}
			
		}
	}
	
	public void render(MatrixStack matrix) {
		if (this.editor.expanded && (this.editor.addObjectButton != null)) {
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			float partial = MinecraftClient.getInstance().getTickDelta();
			
			AdvancedButton addBtn = this.editor.addObjectButton;
			
			this.backBtn.x = addBtn.x;
			this.backBtn.y = addBtn.y - this.backBtn.getHeight() - 2;
			
			this.forwardBtn.x = this.backBtn.x + this.backBtn.getWidth() + 2;
			this.forwardBtn.y = addBtn.y - this.backBtn.getHeight() - 2;
			
			this.backBtn.render(matrix, mouseX, mouseY, partial);
			this.forwardBtn.render(matrix, mouseX, mouseY, partial);
		}
	}
	
	private void onCtrlAltZCtrlAltY(KeyboardData d) {
		if (KeyboardHandler.isCtrlPressed() && KeyboardHandler.isAltPressed() && (this.editor == MinecraftClient.getInstance().currentScreen) && (this.editor.history == this)) {
			if (d.keycode == 89) {
				this.stepBack();
			}
			if (d.keycode == 90) {
				this.stepForward();
			}
		}
	}
	
	public static class Snapshot {
		
		public PropertiesSet snapshot = new PropertiesSet("menu");
		public Snapshot preSnapshotState = null;
		private Runnable run; 
		
		public Snapshot(LayoutCreatorScreen editor, @Nullable Runnable onSnapshotRestore) {

			this.run = onSnapshotRestore;
			
			for (PropertiesSection s : editor.getAllProperties()) {
				this.snapshot.addProperties(s);
			}
			
		}
		
		public void runSnapshotActions() {
			if (this.run != null) {
				this.run.run();
			}
		}
		
	}

}
