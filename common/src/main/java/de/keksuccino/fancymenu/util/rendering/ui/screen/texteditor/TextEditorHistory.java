package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class TextEditorHistory {

    protected TextEditorScreen parent;
    protected List<Snapshot> snapshots = new ArrayList<>();
    protected int index = 0;

    protected TextEditorHistory(@NotNull TextEditorScreen parent) {
        this.parent = parent;
    }

    public void saveSnapshot() {
        if (this.index < this.snapshots.size()) {
            if (this.index == 0) {
                this.snapshots.clear();
            } else {
                //TODO check if index needs -/+ 1
                this.snapshots = this.snapshots.subList(0, this.index);
            }
        }
        TextEditorLine focusedLine = this.parent.getFocusedLine();
        int cursorPos = (focusedLine != null) ? focusedLine.getCursorPosition() : 0;
        this.snapshots.add(new Snapshot(this.parent.getText(), this.parent.getFocusedLineIndex(), cursorPos, this.parent.verticalScrollBar.getScroll(), this.parent.horizontalScrollBar.getScroll()));
        this.index = this.snapshots.size();
    }

    public void stepBack() {
        if (this.index > 0) {
            this.index--;
            this.restoreFrom(this.snapshots.get(this.index));
        }
    }

    //TODO Fixen: bei step forward wird letzter char nicht zurückgebracht
    //TODO Fixen: bei step forward wird letzter char nicht zurückgebracht
    //TODO Fixen: bei step forward wird letzter char nicht zurückgebracht
    //TODO Fixen: bei step forward wird letzter char nicht zurückgebracht
    public void stepForward() {
        if (this.index < this.snapshots.size()-1) {
            this.index++;
            this.restoreFrom(this.snapshots.get(this.index));
        } else if (this.index < this.snapshots.size()) {
            this.restoreFrom(this.snapshots.get(this.index));
            this.index++;
        } else if (this.index == this.snapshots.size()) {
            this.restoreFrom(this.snapshots.get(this.index-1));
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
