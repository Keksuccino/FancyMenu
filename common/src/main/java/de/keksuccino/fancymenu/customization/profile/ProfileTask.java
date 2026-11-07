package de.keksuccino.fancymenu.customization.profile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A long-running profile operation that runs in the background.
 */
public class ProfileTask {

    @NotNull
    private final Type type;
    @NotNull
    private final ProfileTaskProgress progress = new ProfileTaskProgress();
    @NotNull
    private volatile State state = State.RUNNING;
    @Nullable
    private volatile Throwable error;
    /** The ID of the profile the task created or changed, if any. */
    @Nullable
    private volatile String resultProfileId;

    public ProfileTask(@NotNull Type type) {
        this.type = type;
    }

    @NotNull
    public Type getType() {
        return this.type;
    }

    @NotNull
    public ProfileTaskProgress getProgress() {
        return this.progress;
    }

    @NotNull
    public State getState() {
        return this.state;
    }

    @Nullable
    public Throwable getError() {
        return this.error;
    }

    @Nullable
    public String getResultProfileId() {
        return this.resultProfileId;
    }

    public boolean isDone() {
        return this.state != State.RUNNING;
    }

    public boolean isCancellable() {
        return this.type.cancellable;
    }

    public void cancel() {
        if (this.isCancellable()) this.progress.requestCancel();
    }

    void succeed(@Nullable String resultProfileId) {
        this.resultProfileId = resultProfileId;
        this.state = State.SUCCEEDED;
    }

    void fail(@NotNull Throwable error) {
        this.error = error;
        this.state = State.FAILED;
    }

    void markCancelled() {
        this.state = State.CANCELLED;
    }

    public enum Type {

        CREATE(false),
        SNAPSHOT(true),
        DUPLICATE(true),
        DELETE(false),
        EXPORT(true),
        IMPORT(true),
        SET_ICON(false);

        private final boolean cancellable;

        Type(boolean cancellable) {
            this.cancellable = cancellable;
        }

    }

    public enum State {
        RUNNING,
        SUCCEEDED,
        FAILED,
        CANCELLED
    }

}
