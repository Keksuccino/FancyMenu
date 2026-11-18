package de.keksuccino.fancymenu.customization.action.blocks.statements;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WhileExecutableBlockTest {

    private static final long TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(WhileExecutableBlock.TIMEOUT_MILLIS);
    /** Whole milliseconds in nanoseconds are always divisible by 4, so a body taking this long runs exactly 4 times per timeout. */
    private static final long QUARTER_TIMEOUT_NANOS = TIMEOUT_NANOS / 4L;
    /** Fails a test whose loop never stops, instead of letting it hang the test run. */
    private static final int MAX_BODY_EXECUTIONS = 1000;
    /** System.nanoTime() has no fixed origin, so the tests don't start the clock at 0. */
    private static final long START_NANOS = 123_456_789L;

    @Test
    void runAfterATimedOutRunExecutesItsContentsAgain() {
        AtomicLong now = new AtomicLong(START_NANOS);
        AtomicInteger remainingIterations = new AtomicInteger(Integer.MAX_VALUE);
        AtomicLong bodyDuration = new AtomicLong(QUARTER_TIMEOUT_NANOS);
        ControlledWhileExecutableBlock loop = new ControlledWhileExecutableBlock(now, () -> remainingIterations.get() > 0);
        BodyExecutable body = new BodyExecutable(execution -> {
            remainingIterations.decrementAndGet();
            now.addAndGet(bodyDuration.get());
        });
        loop.addExecutable(body);

        loop.execute();
        assertEquals(4, body.getExecutions());

        remainingIterations.set(2);
        bodyDuration.set(0L);
        loop.execute();
        assertEquals(6, body.getExecutions());
    }

    @Test
    void everyRunThatTimesOutGetsTheFullTimeout() {
        AtomicLong now = new AtomicLong(START_NANOS);
        ControlledWhileExecutableBlock loop = new ControlledWhileExecutableBlock(now, () -> true);
        BodyExecutable body = new BodyExecutable(execution -> now.addAndGet(QUARTER_TIMEOUT_NANOS));
        loop.addExecutable(body);

        loop.execute();
        assertEquals(4, body.getExecutions());

        loop.execute();
        assertEquals(8, body.getExecutions());

        now.addAndGet(TIMEOUT_NANOS * 10L);
        loop.execute();
        assertEquals(12, body.getExecutions());
    }

    @Test
    void runStopsExactlyWhenTheTimeoutIsReached() {
        AtomicLong now = new AtomicLong(START_NANOS);
        ControlledWhileExecutableBlock loop = new ControlledWhileExecutableBlock(now, () -> true);
        // The first execution ends one nanosecond before the timeout, the second one exactly on it
        BodyExecutable body = new BodyExecutable(execution -> now.addAndGet((execution == 1) ? (TIMEOUT_NANOS - 1L) : 1L));
        loop.addExecutable(body);

        loop.execute();

        assertEquals(2, body.getExecutions());
    }

    @Test
    void timeoutStillWorksWhenTheClockValueOverflowsDuringARun() {
        AtomicLong now = new AtomicLong(Long.MAX_VALUE - QUARTER_TIMEOUT_NANOS);
        ControlledWhileExecutableBlock loop = new ControlledWhileExecutableBlock(now, () -> true);
        BodyExecutable body = new BodyExecutable(execution -> now.addAndGet(QUARTER_TIMEOUT_NANOS));
        loop.addExecutable(body);

        loop.execute();

        assertEquals(4, body.getExecutions());
    }

    @Test
    void runEndsWhenTheConditionIsNoLongerMet() {
        AtomicLong now = new AtomicLong(START_NANOS);
        AtomicInteger remainingIterations = new AtomicInteger(5);
        ControlledWhileExecutableBlock loop = new ControlledWhileExecutableBlock(now, () -> remainingIterations.get() > 0);
        BodyExecutable body = new BodyExecutable(execution -> remainingIterations.decrementAndGet());
        loop.addExecutable(body);

        loop.execute();
        assertEquals(5, body.getExecutions());

        loop.execute();
        assertEquals(5, body.getExecutions());
    }

    @Test
    void nestedRunOfTheSameBlockKeepsTheTimeoutOfTheRunAroundIt() {
        AtomicLong now = new AtomicLong(START_NANOS);
        ControlledWhileExecutableBlock loop = new ControlledWhileExecutableBlock(now, () -> true);
        BodyExecutable body = new BodyExecutable(execution -> {
            if (execution == 1) {
                // Runs the same block from inside its own contents, like a Set Variable action in the loop of an On Variable Updated listener's script
                loop.execute();
            }
            now.addAndGet(QUARTER_TIMEOUT_NANOS);
        });
        loop.addExecutable(body);

        loop.execute();

        // The nested run executes the contents 4 times until it times out, then the outer run is past its timeout too
        assertEquals(5, body.getExecutions());
    }

    private static final class ControlledWhileExecutableBlock extends WhileExecutableBlock {

        @NotNull
        private final BooleanSupplier loopCondition;

        private ControlledWhileExecutableBlock(@NotNull AtomicLong now, @NotNull BooleanSupplier loopCondition) {
            super(now::get);
            this.loopCondition = loopCondition;
        }

        @Override
        public boolean check() {
            return this.loopCondition.getAsBoolean();
        }

    }

    private static final class BodyExecutable implements Executable {

        private final AtomicInteger executions = new AtomicInteger();
        /** Gets the number of the current execution, starting at 1. */
        @NotNull
        private final IntConsumer action;
        @NotNull
        private final String identifier = UUID.randomUUID().toString();

        private BodyExecutable(@NotNull IntConsumer action) {
            this.action = action;
        }

        private int getExecutions() {
            return this.executions.get();
        }

        @Override
        public void execute() {
            int execution = this.executions.incrementAndGet();
            if (execution > MAX_BODY_EXECUTIONS) {
                throw new AssertionError("The WHILE loop never stopped");
            }
            this.action.accept(execution);
        }

        @Override
        public @NotNull String getIdentifier() {
            return this.identifier;
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            return new BodyExecutable(this.action);
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            return new PropertyContainer("while_body_test_executable");
        }

    }

}
