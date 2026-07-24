package de.keksuccino.fancymenu.util.rendering.ui;

/**
 * Marks a component whose focused state alone is not enough to determine whether it owns a mouse release.
 * The capture query is intentionally made before dispatching the release because dispatch normally clears ownership.
 */
public interface MouseButtonCaptureOwner {

    boolean hasMouseButtonCapture(int button);

}
