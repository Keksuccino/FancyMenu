package de.keksuccino.fancymenu.util.window;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomWindowTitleControllerTest {

    private static final String PLACEHOLDER = "{\"placeholder\":\"getvariable\",\"values\":{\"name\":\"modpack_version\"}}";

    @Test
    void emptyRawTitleUsesVanillaTitle() {
        TitleFixture fixture = new TitleFixture("");

        assertNull(fixture.controller.createTitle());
        fixture.controller.tick();

        assertEquals(0, fixture.refreshCount);
        assertEquals(0, fixture.resolveCount);
    }

    @Test
    void staticTitleIsUsedWithoutResolvingPlaceholders() {
        TitleFixture fixture = new TitleFixture("My Modpack");
        fixture.placeholdersReady = false;

        assertEquals("My Modpack", fixture.controller.createTitle());
        assertEquals(0, fixture.resolveCount);
    }

    @Test
    void placeholderTitleIsResolved() {
        TitleFixture fixture = new TitleFixture("My Modpack v" + PLACEHOLDER);
        fixture.placeholderValue = "1.2.3";

        assertEquals("My Modpack v1.2.3", fixture.controller.createTitle());
    }

    @Test
    void placeholderTitleUsesVanillaTitleUntilPlaceholdersAreReady() {
        TitleFixture fixture = new TitleFixture("My Modpack v" + PLACEHOLDER);
        fixture.placeholdersReady = false;
        fixture.placeholderValue = "1.2.3";

        assertNull(fixture.controller.createTitle());
        fixture.controller.tick();
        assertEquals(0, fixture.refreshCount);
        assertEquals(0, fixture.resolveCount);

        fixture.placeholdersReady = true;
        fixture.controller.tick();

        assertEquals(1, fixture.refreshCount);
        assertEquals("My Modpack v1.2.3", fixture.lastAppliedTitle);
    }

    @Test
    void tickRefreshesOnlyWhenResolvedTitleChanges() {
        TitleFixture fixture = new TitleFixture("My Modpack v" + PLACEHOLDER);
        fixture.placeholderValue = "1.2.3";
        fixture.controller.createTitle();

        fixture.controller.tick();
        fixture.controller.tick();
        assertEquals(0, fixture.refreshCount);

        fixture.placeholderValue = "1.2.4";
        fixture.controller.tick();
        fixture.controller.tick();

        assertEquals(1, fixture.refreshCount);
        assertEquals("My Modpack v1.2.4", fixture.lastAppliedTitle);
    }

    @Test
    void tickDoesNotResolveUnchangedStaticTitle() {
        TitleFixture fixture = new TitleFixture("My Modpack");
        fixture.controller.createTitle();

        fixture.controller.tick();
        fixture.controller.tick();

        assertEquals(0, fixture.refreshCount);
    }

    @Test
    void tickRefreshesAfterRawTitleChanged() {
        TitleFixture fixture = new TitleFixture("My Modpack");
        fixture.controller.createTitle();

        fixture.rawTitle = "Other Modpack";
        fixture.controller.tick();
        fixture.controller.tick();
        assertEquals(1, fixture.refreshCount);
        assertEquals("Other Modpack", fixture.lastAppliedTitle);

        fixture.rawTitle = "";
        fixture.controller.tick();
        assertEquals(2, fixture.refreshCount);
        assertNull(fixture.lastAppliedTitle);
    }

    @Test
    void tickRefreshesWhenFirstTickHappensBeforeAnyTitleWasCreated() {
        TitleFixture fixture = new TitleFixture("My Modpack");

        fixture.controller.tick();

        assertEquals(1, fixture.refreshCount);
        assertEquals("My Modpack", fixture.lastAppliedTitle);
    }

    @Test
    void refreshThatDoesNotRebuildTitleIsNotRepeated() {
        TitleFixture fixture = new TitleFixture("My Modpack v" + PLACEHOLDER);
        fixture.refreshRebuildsTitle = false;
        fixture.placeholderValue = "1.2.3";
        fixture.controller.createTitle();

        fixture.placeholderValue = "1.2.4";
        fixture.controller.tick();
        fixture.controller.tick();

        assertEquals(1, fixture.refreshCount);
    }

    @Test
    void formattingCodesAndLineBreaksAreRemoved() {
        TitleFixture fixture = new TitleFixture("§6My Modpack " + PLACEHOLDER);
        fixture.placeholderValue = "§aline\r\nbreak\nhere";

        assertEquals("My Modpack line break here", fixture.controller.createTitle());
    }

    @Test
    void ampersandIsKeptLiterally() {
        TitleFixture fixture = new TitleFixture("R&D Pack " + PLACEHOLDER);
        fixture.placeholderValue = "&a1.2.3";

        assertEquals("R&D Pack &a1.2.3", fixture.controller.createTitle());
    }

    @Test
    void blankResolvedTitleUsesVanillaTitle() {
        TitleFixture fixture = new TitleFixture(PLACEHOLDER);
        fixture.placeholderValue = "§a  ";

        assertNull(fixture.controller.createTitle());

        fixture.placeholderValue = "1.2.3";
        fixture.controller.tick();
        assertEquals(1, fixture.refreshCount);
        assertEquals("1.2.3", fixture.lastAppliedTitle);

        fixture.placeholderValue = "";
        fixture.controller.tick();
        assertEquals(2, fixture.refreshCount);
        assertNull(fixture.lastAppliedTitle);
    }

    private static final class TitleFixture {

        private final CustomWindowTitleController controller;
        private String rawTitle;
        private String placeholderValue = "";
        private boolean placeholdersReady = true;
        private boolean refreshRebuildsTitle = true;
        private String lastAppliedTitle;
        private int refreshCount;
        private int resolveCount;

        private TitleFixture(String rawTitle) {
            this.rawTitle = rawTitle;
            this.controller = new CustomWindowTitleController(() -> this.rawTitle, title -> title.contains(PLACEHOLDER), () -> this.placeholdersReady, this::resolve, this::refresh);
        }

        private String resolve(String title) {
            this.resolveCount++;
            return title.replace(PLACEHOLDER, this.placeholderValue);
        }

        private void refresh() {
            this.refreshCount++;
            if (this.refreshRebuildsTitle) this.lastAppliedTitle = this.controller.createTitle();
        }

    }

}
