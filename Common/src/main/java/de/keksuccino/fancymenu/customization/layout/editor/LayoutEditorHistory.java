package de.keksuccino.fancymenu.customization.layout.editor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import de.keksuccino.fancymenu.properties.PropertyContainer;
import de.keksuccino.fancymenu.properties.PropertyContainerSet;
import net.minecraft.client.Minecraft;

public class LayoutEditorHistory {

	protected LayoutEditorScreen editor;
	private List<Snapshot> history = new ArrayList<Snapshot>();
	private int current = -1;
	private boolean preventSnapshotSaving = false;
	
	public LayoutEditorHistory(LayoutEditorScreen editor) {
		
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
				List<PropertyContainerSet> l = new ArrayList<PropertyContainerSet>();
				l.add(snap.snapshot);

				this.current--;

				snap.preSnapshotState = this.createSnapshot();

				PreloadedLayoutEditorScreen neweditor = new PreloadedLayoutEditorScreen(this.editor.screenToCustomize, l);
				neweditor.history = this;
				String single = null;
				if (this.editor instanceof PreloadedLayoutEditorScreen) {
					single = ((PreloadedLayoutEditorScreen)this.editor).single;
				}
				neweditor.single = single;
				this.editor = neweditor;

				Minecraft.getInstance().setScreen(neweditor);

			}
			
		}
	}
	
	public void stepForward() {
		if (this.current >= -1) {

			if (this.current < this.history.size()-1) {

				this.current++;

				Snapshot snap = this.history.get(this.current).preSnapshotState;

				if (snap != null) {
					List<PropertyContainerSet> l = new ArrayList<PropertyContainerSet>();
					l.add(snap.snapshot);

					PreloadedLayoutEditorScreen neweditor = new PreloadedLayoutEditorScreen(this.editor.screenToCustomize, l);
					neweditor.history = this;
					String single = null;
					if (this.editor instanceof PreloadedLayoutEditorScreen) {
						single = ((PreloadedLayoutEditorScreen)this.editor).single;
					}
					neweditor.single = single;
					this.editor = neweditor;

					Minecraft.getInstance().setScreen(neweditor);
				}

			}

		}
	}
	
	public static class Snapshot {
		
		public PropertyContainerSet snapshot = new PropertyContainerSet("menu");
		public Snapshot preSnapshotState = null;
		private Runnable run; 
		
		public Snapshot(LayoutEditorScreen editor, @Nullable Runnable onSnapshotRestore) {

			this.run = onSnapshotRestore;
			
			for (PropertyContainer s : editor.getAllProperties()) {
				this.snapshot.putContainer(s);
			}
			
		}
		
		public void runSnapshotActions() {
			if (this.run != null) {
				this.run.run();
			}
		}
		
	}

}
