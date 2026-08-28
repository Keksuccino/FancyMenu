package de.keksuccino.fancymenu.customization.element;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Keeps appearance playback state across element rebuilding. Screen resizes and soft reloads rebuild every element,
 * while a real switch to another menu ends an interrupted first-time effect and resets every-time effects.
 */
public final class ElementAppearanceStateHandler {

    private static final Map<String, Map<String, ElementState>> STATES = new HashMap<>();

    private ElementAppearanceStateHandler() {
    }

    @NotNull
    static synchronized ElementState getState(@NotNull String screenIdentifier, @NotNull String elementIdentifier) {
        Map<String, ElementState> screenStates = STATES.computeIfAbsent(Objects.requireNonNull(screenIdentifier), ignored -> new HashMap<>());
        return screenStates.computeIfAbsent(Objects.requireNonNull(elementIdentifier), ignored -> new ElementState());
    }

    /**
     * Ends playback owned by the previous menu only when the menu context actually changes. Custom GUI identifiers are
     * passed in as screen identifiers, so separate custom GUIs never share state just because they use the same class.
     */
    public static synchronized void onScreenChanged(@NotNull String previousScreenIdentifier, @Nullable String newScreenIdentifier) {
        if (previousScreenIdentifier.equals(newScreenIdentifier)) return;
        Map<String, ElementState> screenStates = STATES.get(previousScreenIdentifier);
        if (screenStates != null) screenStates.values().forEach(ElementState::onScreenLeft);
    }

    public static synchronized void clear() {
        STATES.clear();
    }

    static final class ElementState {

        @Nullable private Boolean fadeInFirstTime;
        private PlaybackStatus fadeInStatus = PlaybackStatus.NOT_STARTED;
        private float fadeInOpacity = 0.02F;

        @Nullable private Boolean appearanceDelayFirstTime;
        private PlaybackStatus appearanceDelayStatus = PlaybackStatus.NOT_STARTED;
        private long appearanceDelayEndTime = -1L;

        @Nullable
        synchronized Float beginOrResumeFadeIn(boolean firstTime, float startingOpacity) {
            this.ensureFadeInMode(firstTime);
            if (this.fadeInStatus == PlaybackStatus.FINISHED) return null;
            if (this.fadeInStatus == PlaybackStatus.NOT_STARTED) {
                this.fadeInStatus = PlaybackStatus.PLAYING;
                this.fadeInOpacity = startingOpacity;
            }
            return this.fadeInOpacity;
        }

        synchronized void updateFadeIn(float opacity) {
            if (this.fadeInStatus != PlaybackStatus.PLAYING) return;
            this.fadeInOpacity = opacity;
        }

        synchronized void finishFadeIn() {
            if (this.fadeInStatus == PlaybackStatus.NOT_STARTED) return;
            this.fadeInStatus = PlaybackStatus.FINISHED;
        }

        synchronized void restartEveryTimeFadeIn() {
            this.resetFadeIn(false);
        }

        synchronized long beginOrResumeAppearanceDelay(boolean firstTime, long proposedEndTime, long currentTime) {
            this.ensureAppearanceDelayMode(firstTime);
            if (this.appearanceDelayStatus == PlaybackStatus.FINISHED) return -1L;
            if (this.appearanceDelayStatus == PlaybackStatus.NOT_STARTED) {
                this.appearanceDelayStatus = PlaybackStatus.PLAYING;
                this.appearanceDelayEndTime = proposedEndTime;
            }
            if (this.appearanceDelayEndTime <= currentTime) {
                this.appearanceDelayStatus = PlaybackStatus.FINISHED;
                return -1L;
            }
            return this.appearanceDelayEndTime;
        }

        synchronized void finishAppearanceDelay() {
            if (this.appearanceDelayStatus == PlaybackStatus.NOT_STARTED) return;
            this.appearanceDelayStatus = PlaybackStatus.FINISHED;
            this.appearanceDelayEndTime = -1L;
        }

        synchronized void restartEveryTimeAppearanceDelay() {
            this.resetAppearanceDelay(false);
        }

        private synchronized void onScreenLeft() {
            if (Boolean.TRUE.equals(this.fadeInFirstTime)) {
                if (this.fadeInStatus == PlaybackStatus.PLAYING) this.fadeInStatus = PlaybackStatus.FINISHED;
            } else if (Boolean.FALSE.equals(this.fadeInFirstTime)) {
                this.resetFadeIn(false);
            }
            if (Boolean.TRUE.equals(this.appearanceDelayFirstTime)) {
                if (this.appearanceDelayStatus == PlaybackStatus.PLAYING) this.finishAppearanceDelay();
            } else if (Boolean.FALSE.equals(this.appearanceDelayFirstTime)) {
                this.resetAppearanceDelay(false);
            }
        }

        private void ensureFadeInMode(boolean firstTime) {
            if ((this.fadeInFirstTime != null) && (this.fadeInFirstTime == firstTime)) return;
            this.resetFadeIn(firstTime);
        }

        private void resetFadeIn(boolean firstTime) {
            this.fadeInFirstTime = firstTime;
            this.fadeInStatus = PlaybackStatus.NOT_STARTED;
            this.fadeInOpacity = 0.02F;
        }

        private void ensureAppearanceDelayMode(boolean firstTime) {
            if ((this.appearanceDelayFirstTime != null) && (this.appearanceDelayFirstTime == firstTime)) return;
            this.resetAppearanceDelay(firstTime);
        }

        private void resetAppearanceDelay(boolean firstTime) {
            this.appearanceDelayFirstTime = firstTime;
            this.appearanceDelayStatus = PlaybackStatus.NOT_STARTED;
            this.appearanceDelayEndTime = -1L;
        }

    }

    private enum PlaybackStatus {
        NOT_STARTED,
        PLAYING,
        FINISHED
    }

}
