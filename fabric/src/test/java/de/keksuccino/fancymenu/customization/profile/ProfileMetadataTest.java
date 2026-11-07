package de.keksuccino.fancymenu.customization.profile;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileMetadataTest {

    @Test
    void jsonRoundTripKeepsAllFields() {
        ProfileMetadata metadata = new ProfileMetadata("cool_setup_1a2b3c");
        metadata.setTitle("Cool Setup");
        metadata.setVersion("1.2.0");
        metadata.setAuthors(List.of("Alice", "Bob"));
        metadata.setDescription("# Hello\nSome **markdown**.");
        metadata.setIssueTrackerUrl("https://github.com/example/setup/issues");
        metadata.setLinks(List.of(
                new ProfileMetadata.Link(ProfileLinkType.DISCORD, "", "https://discord.gg/abc"),
                new ProfileMetadata.Link(ProfileLinkType.OTHER, "Blog", "https://example.com/blog")));
        metadata.setFancyMenuVersion("3.10.0");

        ProfileMetadata parsed = ProfileMetadata.fromJson(metadata.toJson(), "fallback");

        assertEquals("cool_setup_1a2b3c", parsed.getId());
        assertEquals("Cool Setup", parsed.getTitle());
        assertEquals("1.2.0", parsed.getVersion());
        assertEquals(List.of("Alice", "Bob"), parsed.getAuthors());
        assertEquals("# Hello\nSome **markdown**.", parsed.getDescription());
        assertEquals("https://github.com/example/setup/issues", parsed.getIssueTrackerUrl());
        assertEquals(metadata.getLinks(), parsed.getLinks());
        assertEquals("3.10.0", parsed.getFancyMenuVersion());
    }

    @Test
    void linksOnlyAcceptWebUrls() {
        ProfileMetadata metadata = new ProfileMetadata("test");
        metadata.setLinks(List.of(
                new ProfileMetadata.Link(ProfileLinkType.WEBSITE, "", "file:///C:/Windows/System32/calc.exe"),
                new ProfileMetadata.Link(ProfileLinkType.WEBSITE, "", "javascript:alert(1)"),
                new ProfileMetadata.Link(ProfileLinkType.WEBSITE, "", "ftp://example.com/file"),
                new ProfileMetadata.Link(ProfileLinkType.WEBSITE, "", "localhost"),
                new ProfileMetadata.Link(ProfileLinkType.DISCORD, "", "discord.gg/abc")));

        assertEquals(1, metadata.getLinks().size());
        assertEquals("https://discord.gg/abc", metadata.getLinks().get(0).url());

        metadata.setIssueTrackerUrl("file:///etc/passwd");
        assertNull(metadata.getIssueTrackerUrl());
    }

    @Test
    void parsingDropsInvalidLinksFromSharedFiles() {
        String json = "{\"id\":\"shared\",\"links\":[{\"type\":\"website\",\"url\":\"file:///evil\"},{\"type\":\"youtube\",\"url\":\"https://youtube.com/@me\"},\"broken\"]}";

        ProfileMetadata parsed = ProfileMetadata.fromJson(json, "fallback");

        assertEquals(1, parsed.getLinks().size());
        assertEquals(ProfileLinkType.YOUTUBE, parsed.getLinks().get(0).type());
    }

    @Test
    void singleLineValuesGetSanitizedAndCut() {
        ProfileMetadata metadata = new ProfileMetadata("test");
        metadata.setTitle("  Line\nBreak\u0000" + "x".repeat(200));

        assertFalse(metadata.getTitle().contains("\n"));
        assertFalse(metadata.getTitle().contains("\u0000"));
        assertTrue(metadata.getTitle().startsWith("Line Break"));
        assertEquals(ProfileMetadata.MAX_TITLE_LENGTH, metadata.getTitle().length());
    }

    @Test
    void labelsOnlyStayForCustomLinks() {
        assertEquals("", ProfileMetadata.Link.of(ProfileLinkType.DISCORD, "Ignored", "https://discord.gg/abc").label());
        assertEquals("Blog", ProfileMetadata.Link.of(ProfileLinkType.OTHER, " Blog ", "https://example.com").label());
    }

    @Test
    void authorsFromStringDropBlanksAndDuplicates() {
        ProfileMetadata metadata = new ProfileMetadata("test");
        metadata.setAuthorsFromString("Alice, , Bob,Alice ,  ");

        assertEquals(List.of("Alice", "Bob"), metadata.getAuthors());
        assertEquals("Alice, Bob", metadata.getAuthorsAsString());
    }

    @Test
    void invalidIdFallsBack() {
        assertEquals("fallback", ProfileMetadata.fromJson("{\"id\":\"../../evil\"}", "fallback").getId());
        assertEquals("fallback", ProfileMetadata.fromJson("{}", "fallback").getId());
    }

    @Test
    void brokenJsonThrows() {
        assertThrows(IllegalArgumentException.class, () -> ProfileMetadata.fromJson("{not json", "fallback"));
        assertThrows(IllegalArgumentException.class, () -> ProfileMetadata.fromJson("[]", "fallback"));
    }

    @Test
    void descriptionKeepsLineBreaksButNormalizesThem() {
        ProfileMetadata metadata = new ProfileMetadata("test");
        metadata.setDescription("a\r\nb\rc\n");

        assertEquals("a\nb\nc", metadata.getDescription());
    }

    @Test
    void linkTypeGetsDetectedFromUrl() {
        assertEquals(ProfileLinkType.DISCORD, ProfileLinkType.detectFromUrl("https://discord.gg/abc"));
        assertEquals(ProfileLinkType.YOUTUBE, ProfileLinkType.detectFromUrl("www.youtube.com/@creator"));
        assertEquals(ProfileLinkType.X, ProfileLinkType.detectFromUrl("https://twitter.com/someone"));
        assertEquals(ProfileLinkType.MODRINTH, ProfileLinkType.detectFromUrl("https://modrinth.com/mod/fancymenu"));
        assertEquals(ProfileLinkType.WEBSITE, ProfileLinkType.detectFromUrl("https://example.com"));
        assertEquals(ProfileLinkType.WEBSITE, ProfileLinkType.detectFromUrl("not a url"));
    }

    @Test
    void unknownLinkTypesBecomeOther() {
        assertEquals(ProfileLinkType.OTHER, ProfileLinkType.fromSerializedName("myspace"));
        assertEquals(ProfileLinkType.DISCORD, ProfileLinkType.fromSerializedName(" Discord "));
    }

}
