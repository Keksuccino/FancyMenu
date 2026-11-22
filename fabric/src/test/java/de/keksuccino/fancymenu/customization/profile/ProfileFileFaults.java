package de.keksuccino.fancymenu.customization.profile;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Makes chosen moves and deletes of profile files fail like files that another program keeps open on Windows.
 * {@link #install()} puts it into {@link ProfileFiles#operationHook}, {@link #uninstall()} takes it out again.
 */
final class ProfileFileFaults implements ProfileFiles.OperationHook {

    private final List<Rule<BiPredicate<Path, Path>>> moveRules = new ArrayList<>();
    private final List<Rule<Predicate<Path>>> deleteRules = new ArrayList<>();
    final List<String> operations = new ArrayList<>();

    @NotNull
    static ProfileFileFaults install() {
        ProfileFileFaults faults = new ProfileFileFaults();
        ProfileFiles.operationHook = faults;
        return faults;
    }

    static void uninstall() {
        ProfileFiles.operationHook = null;
    }

    /**
     * Fails the matching moves, at most {@code times} times. Negative values fail them until {@link #clear()}.
     */
    @NotNull
    ProfileFileFaults failMoves(@NotNull BiPredicate<Path, Path> matcher, int times) {
        this.moveRules.add(new Rule<>(matcher, times));
        return this;
    }

    @NotNull
    ProfileFileFaults failDeletes(@NotNull Predicate<Path> matcher, int times) {
        this.deleteRules.add(new Rule<>(matcher, times));
        return this;
    }

    void clear() {
        this.moveRules.clear();
        this.deleteRules.clear();
    }

    @Override
    public void beforeMove(@NotNull Path source, @NotNull Path target) throws IOException {
        this.operations.add("move " + source + " -> " + target);
        for (Rule<BiPredicate<Path, Path>> rule : this.moveRules) {
            if ((rule.timesLeft != 0) && rule.matcher.test(source, target)) {
                rule.timesLeft--;
                throw new AccessDeniedException(source.toString(), target.toString(), "Simulated file in use");
            }
        }
    }

    @Override
    public void beforeDelete(@NotNull Path path) throws IOException {
        this.operations.add("delete " + path);
        for (Rule<Predicate<Path>> rule : this.deleteRules) {
            if ((rule.timesLeft != 0) && rule.matcher.test(path)) {
                rule.timesLeft--;
                throw new AccessDeniedException(path.toString(), null, "Simulated file in use");
            }
        }
    }

    private static final class Rule<T> {

        private final T matcher;
        private int timesLeft;

        private Rule(T matcher, int timesLeft) {
            this.matcher = matcher;
            this.timesLeft = timesLeft;
        }

    }

}
