package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSet;
import net.minecraft.client.MinecraftClient;

public class EditHistory {

	protected LayoutEditorScreen editor;
	private List<Snapshot> history = new ArrayList<Snapshot>();
	private int current = -1;
	private boolean preventSnapshotSaving = false;
	
	public EditHistory(LayoutEditorScreen editor) {
		
		this.editor = editor;
		
	}
	
	public void saveSnapshot(Snapshot snap) {
		if (!this.preventSnapshotSaving) {
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
	}
	
	public Snapshot createSnapshot(Runnable onSnapshotRestore) {
		return new Snapshot(editor, onSnapshotRestore);
	}
	
	public Snapshot createSnapshot() {
		return new Snapshot(editor, null);
	}

	public void setPreventSnapshotSaving(boolean b) {
		this.preventSnapshotSaving = b;
	}
	
	public void stepBack() {
		if (this.current > -1) {

			if (this.current <= this.history.size()-1) {

				Snapshot snap = this.history.get(this.current);
				List<PropertiesSet> l = new ArrayList<PropertiesSet>();
				l.add(snap.snapshot);

				this.current--;

				snap.preSnapshotState = this.createSnapshot();

				PreloadedLayoutEditorScreen neweditor = new PreloadedLayoutEditorScreen(this.editor.screen, l);
				neweditor.history = this;
				String single = null;
				if (this.editor instanceof PreloadedLayoutEditorScreen) {
					single = ((PreloadedLayoutEditorScreen)this.editor).single;
				}
				neweditor.single = single;
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

					PreloadedLayoutEditorScreen neweditor = new PreloadedLayoutEditorScreen(this.editor.screen, l);
					neweditor.history = this;
					String single = null;
					if (this.editor instanceof PreloadedLayoutEditorScreen) {
						single = ((PreloadedLayoutEditorScreen)this.editor).single;
					}
					neweditor.single = single;
					this.editor = neweditor;

					MinecraftClient.getInstance().openScreen(neweditor);
				}

			}

		}
	}
	
	public static class Snapshot {
		
		public PropertiesSet snapshot = new PropertiesSet("menu");
		public Snapshot preSnapshotState = null;
		private Runnable run; 
		
		public Snapshot(LayoutEditorScreen editor, @Nullable Runnable onSnapshotRestore) {

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
