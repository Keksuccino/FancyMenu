package de.keksuccino.fancymenu.customization;

import org.jetbrains.annotations.NotNull;

/**
 * Marks code of the current thread that runs script logic, like action scripts, and therefore must read fresh values
 * instead of values from caches that only exist to make rendering cheaper.
 *
 * <p>Rendering reads the same values every frame, so some caches serve a calculated value again for a short time.
 * Scripts change values and read them again right away, for example a WHILE loop whose condition depends on a variable
 * its contents change, or two actions in a row where the second one reads what the first one set. Inside this scope,
 * these caches must not serve old values.</p>
 *
 * <p>Contract for caches:</p>
 * <ul>
 *     <li><b>Time-based result caches</b>, which serve an already calculated result for some time, must skip their lookup
 *     while {@link #isActive()} returns {@code true}, but still store the fresh result, so rendering keeps profiting from it.
 *     These are the result caches of the placeholder parser for texts and for placeholder values (the parser checks the scope
 *     once per parse, and every parse that a placeholder starts itself while it gets replaced checks it again) and the
 *     requirement cache of {@link de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer#requirementsMet()}.</li>
 *     <li><b>Content-only caches</b>, whose entries only depend on the text they belong to and never get outdated
 *     (whether a text contains placeholders, compiled placeholder templates, compiled single-line texts, the memo for
 *     identical placeholders within one parse), stay active.</li>
 *     <li>Caches and cooldowns of <b>asynchronous loads</b> (Web Text's cache of downloaded texts, the file and web content
 *     of other placeholders, values requested from the server) stay active too. Skipping them would only start more
 *     loads, the value would still arrive later.</li>
 * </ul>
 *
 * <p>Where the scope gets opened: {@link de.keksuccino.fancymenu.customization.action.ActionInstance#execute()} (its value
 * and everything the action reads while it executes), the condition checks of IF, ELSE IF and WHILE blocks, the list names of
 * FOR EACH blocks, and the delay values of Delay and Execute Later blocks. These are the only places where action scripts read values, so script entry
 * points (buttons, tickers in both modes, listeners, schedulers, menu open and close scripts, browser actions, the
 * scheduled runs of Execute Later blocks) don't need to open it themselves. Script code that reads values somewhere else
 * has to open it itself.</p>
 *
 * <p>Usage, always with try-with-resources, so the scope also gets closed when the code throws:</p>
 * <pre>{@code
 * try (FreshValuesScope ignored = FreshValuesScope.enter()) {
 *     // read values
 * }
 * }</pre>
 * <p>Scopes nest: the scope stays active until every {@link #enter()} got closed again. Every thread has its own scope,
 * so a script running on the thread of an Async Ticker never affects rendering on the main thread, and the other way around.
 * Everything that runs on the thread while the scope is open is inside it, including parses that placeholders start
 * themselves while they get replaced (like Stringify does).</p>
 */
public final class FreshValuesScope implements AutoCloseable {

    // One object per thread, which is also the handle enter() returns, so entering and closing never allocates
    private static final ThreadLocal<FreshValuesScope> SCOPES = ThreadLocal.withInitial(FreshValuesScope::new);

    // How many scopes are open on the thread this object belongs to. Only that thread ever reads or writes it.
    private int depth;

    private FreshValuesScope() {
    }

    /**
     * @return If code of the current thread runs inside a fresh-values scope, so time-based result caches must not serve cached values.
     */
    public static boolean isActive() {
        return SCOPES.get().depth > 0;
    }

    /**
     * Opens a fresh-values scope for the current thread. Close it with try-with-resources, on the same thread.
     */
    @NotNull
    public static FreshValuesScope enter() {
        FreshValuesScope scope = SCOPES.get();
        scope.depth++;
        return scope;
    }

    /**
     * Closes the scope opened by the {@link #enter()} call that returned this handle.
     * Call it exactly once per {@link #enter()} and only on the thread that called {@link #enter()}.
     *
     * @throws IllegalStateException If every scope of the thread is already closed.
     */
    @Override
    public void close() {
        if (this.depth <= 0) throw new IllegalStateException("Tried to close a fresh-values scope that is not open");
        this.depth--;
    }

}
