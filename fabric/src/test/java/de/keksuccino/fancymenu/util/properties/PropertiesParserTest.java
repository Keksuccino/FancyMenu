package de.keksuccino.fancymenu.util.properties;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertiesParserTest {

    @Test
    void parsesCompleteUtf8ConfigurationWithoutClosingBorrowedStream() throws Exception {
        TrackingInputStream stream = new TrackingInputStream("type = example\nentry {\n  label = Grüße\n}\n".getBytes(StandardCharsets.UTF_8));

        PropertyContainerSet result = PropertiesParser.deserializeSetFromStream(stream);

        assertNotNull(result);
        PropertyContainer entry = result.getFirstContainerOfType("entry");
        assertAll(() -> assertEquals("example", result.getType()), () -> assertNotNull(entry), () -> assertEquals("Grüße", entry.getValue("label")), () -> assertFalse(stream.closed));
    }

    @Test
    void readFailureCannotBeParsedAsCompleteConfiguration() {
        byte[] readablePrefix = "type = example\nentry {\n  label = partial\n}\n".getBytes(StandardCharsets.UTF_8);
        byte[] completeContent = "type = example\nentry {\n  label = partial\n}\nunread {\n  label = missing\n}\n".getBytes(StandardCharsets.UTF_8);
        FailingInputStream stream = new FailingInputStream(completeContent, readablePrefix.length);
        AtomicReference<PropertyContainerSet> publishedResult = new AtomicReference<>();

        assertThrows(IOException.class, () -> publishedResult.set(PropertiesParser.deserializeSetFromStream(stream)));

        assertAll(() -> assertNull(publishedResult.get()), () -> assertFalse(stream.closed));
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private TrackingInputStream(byte[] content) {
            super(content);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }

    }

    private static final class FailingInputStream extends InputStream {

        private final byte[] content;
        private final int failurePosition;
        private int position;
        private boolean closed;

        private FailingInputStream(byte[] content, int failurePosition) {
            this.content = content;
            this.failurePosition = failurePosition;
        }

        @Override
        public int read() throws IOException {
            if (this.position >= this.failurePosition) throw new IOException("simulated configuration read failure");
            return this.content[this.position++] & 0xFF;
        }

        @Override
        public int read(byte[] target, int offset, int length) throws IOException {
            if (this.position >= this.failurePosition) throw new IOException("simulated configuration read failure");
            int count = Math.min(length, this.failurePosition - this.position);
            System.arraycopy(this.content, this.position, target, offset, count);
            this.position += count;
            return count;
        }

        @Override
        public void close() {
            this.closed = true;
        }

    }

}
