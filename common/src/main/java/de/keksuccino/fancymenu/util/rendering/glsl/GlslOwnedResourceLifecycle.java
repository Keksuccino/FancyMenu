package de.keksuccino.fancymenu.util.rendering.glsl;

/** Pure ownership policy for releasing runtime resources when a GLSL owner stops producing render work. */
public final class GlslOwnedResourceLifecycle {

    private boolean resourcesMayBeOwned;

    public boolean completeExtractionCycle(boolean extractedThisCycle, boolean releaseAllowed) {
        if (extractedThisCycle) {
            this.resourcesMayBeOwned = true;
            return false;
        }
        if (!this.resourcesMayBeOwned || !releaseAllowed) {
            return false;
        }
        this.resourcesMayBeOwned = false;
        return true;
    }

    public void markResourcesReleased() {
        this.resourcesMayBeOwned = false;
    }
}
