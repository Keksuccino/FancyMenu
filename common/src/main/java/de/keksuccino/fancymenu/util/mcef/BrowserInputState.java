package de.keksuccino.fancymenu.util.mcef;

import java.util.HashSet;
import java.util.Set;

/** Tracks browser focus and mouse-button ownership independently from Chromium's native focus state. */
final class BrowserInputState {

    private final Set<Integer> capturedMouseButtons = new HashSet<>();
    private boolean focused;

    boolean forwardMousePress(boolean interactable, boolean mouseOver, int button, Runnable forwardingAction) {
        // A new press supersedes stale ownership for the same button if an earlier release was lost during a screen transition.
        this.capturedMouseButtons.remove(button);
        if (!interactable || !mouseOver) {
            this.focused = false;
            return false;
        }
        this.focused = true;
        this.capturedMouseButtons.add(button);
        forwardingAction.run();
        return true;
    }

    boolean forwardMouseRelease(int button, Runnable forwardingAction) {
        if (!this.capturedMouseButtons.remove(button)) return false;
        forwardingAction.run();
        return true;
    }

    boolean forwardMouseScroll(boolean interactable, boolean mouseOver, Runnable forwardingAction) {
        if (!interactable || !mouseOver) return false;
        forwardingAction.run();
        return true;
    }

    boolean forwardKeyboardInput(boolean interactable, Runnable forwardingAction) {
        if (!interactable || !this.focused) return false;
        forwardingAction.run();
        return true;
    }

    boolean forwardCharacterInput(boolean interactable, char codePoint, Runnable forwardingAction) {
        if (!interactable || !this.focused) return false;
        if (codePoint != (char)0) forwardingAction.run();
        return true;
    }

    boolean hasMouseButtonCapture(int button) {
        return this.capturedMouseButtons.contains(button);
    }

    boolean isFocused() {
        return this.focused;
    }

    void setFocused(boolean focused) {
        this.focused = focused;
    }

    void reset() {
        this.focused = false;
        this.capturedMouseButtons.clear();
    }

}
