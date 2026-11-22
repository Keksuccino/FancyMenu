package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import de.keksuccino.fancymenu.test.ScreenTestFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PiPCellWindowBodyLayoutTest {

    @Test
    void contentStartsAtTheWindowMarginBecauseTheTitleBarShowsTheTitle() {
        TestBody body = ScreenTestFactory.allocateScreen(TestBody.class);
        assertEquals(PiPWindowBody.CONTENT_MARGIN, body.getContentTop());
    }

    private static final class TestBody extends PiPCellWindowBody {

        @Override
        protected void onCancel() {
        }

        @Override
        protected void onDone() {
        }

    }

}
