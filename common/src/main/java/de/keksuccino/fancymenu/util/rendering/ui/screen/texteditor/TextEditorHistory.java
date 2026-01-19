package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class TextEditorHistory {

    private static final Logger LOGGER = LogManager.getLogger();

    protected TextEditorWindowBody parent;
    protected List<Snapshot> snapshots = new ArrayList<>();
    protected int index = 0;

    protected TextEditorHistory(@NotNull TextEditorWindowBody parent) {
        this.parent = parent;
    }

    public void saveSnapshot() {
        try {
            if (this.index > 0) {
                if (this.snapshots.get(this.index-1).text.equals(this.parent.getText())) {
                    return; //don't save snapshot if duplicate of index-1 (to not create two identical snaps in a row)
                }
            }
            if (this.index < this.snapshots.size()) {
                if (this.index == 0) {
                    this.snapshots.clear();
                } else {
                    this.snapshots = this.snapshots.subList(0, this.index);
                }
            }
            TextEditorLine focusedLine = this.parent.getFocusedLine();
            int cursorPos = (focusedLine != null) ? focusedLine.getCursorPosition() : 0;
            this.snapshots.add(new Snapshot(this.parent.getText(), this.parent.getFocusedLineIndex(), cursorPos, this.parent.verticalScrollBar.getScroll(), this.parent.horizontalScrollBar.getScroll()));
            this.index = this.snapshots.size();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void stepBack() {
        try {
            if (this.snapshots.isEmpty()) return;
            if (this.index > 0) {
                if (this.index == this.snapshots.size()) {
                    if (!this.snapshots.get(this.index-1).text.equals(this.parent.getText())) {
                        //save snapshot before going back if index at end of snapshots and no snapshot with the current content already exists
                        this.saveSnapshot();
                        this.index--;
                    } else if (this.index > 1) {
                        this.index--;
                    }
                }
                this.index--;
                this.restoreFrom(this.snapshots.get(this.index));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void stepForward() {
        try {
            if (this.snapshots.isEmpty()) return;
            this.index++;
            if (this.index >= this.snapshots.size()) this.index = this.snapshots.size()-1;
            this.restoreFrom(this.snapshots.get(this.index));
            if (this.index == this.snapshots.size()-1) this.index = this.snapshots.size();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void restoreFrom(@NotNull Snapshot snap) {
        this.parent.setText(snap.text);
        if (snap.focusedLineIndex != -1) {
            this.parent.setFocusedLine(snap.focusedLineIndex);
            TextEditorLine focused = this.parent.getFocusedLine();
            if (focused != null) {
                focused.setCursorPosition(snap.cursorPos);
                focused.setHighlightPos(snap.cursorPos);
            }
        }
        this.parent.verticalScrollBar.setScroll(snap.verticalScroll, false);
        this.parent.horizontalScrollBar.setScroll(snap.horizontalScroll, false);
    }

    public record Snapshot(@NotNull String text, int focusedLineIndex, int cursorPos, float verticalScroll, float horizontalScroll) {
    }

}
