package de.keksuccino.fancymenu.util.minecraftuser.v2;

import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinecraftUsersTest {

    private static final MinecraftProfileTexture SKIN = new MinecraftProfileTexture("https://textures.example/skin", Map.of("model", "slim"));
    private static final MinecraftProfileTexture CAPE = new MinecraftProfileTexture("https://textures.example/cape", Map.of());
    private static final MinecraftProfileTexture ELYTRA = new MinecraftProfileTexture("https://textures.example/elytra", Map.of());
    private static final MinecraftProfileTextures COMPLETE_TEXTURES = new MinecraftProfileTextures(SKIN, CAPE, ELYTRA, SignatureState.SIGNED);

    @Test
    void skinTypeReturnsSkinTexture() {
        assertSame(SKIN, MinecraftUsers.selectProfileTexture(COMPLETE_TEXTURES, MinecraftProfileTexture.Type.SKIN));
    }

    @Test
    void capeTypeReturnsCapeTexture() {
        assertSame(CAPE, MinecraftUsers.selectProfileTexture(COMPLETE_TEXTURES, MinecraftProfileTexture.Type.CAPE));
    }

    @Test
    void elytraTypeReturnsDistinctElytraTexture() {
        assertSame(ELYTRA, MinecraftUsers.selectProfileTexture(COMPLETE_TEXTURES, MinecraftProfileTexture.Type.ELYTRA));
    }

    @Test
    void elytraOnlyProfileReturnsElytraTexture() {
        MinecraftProfileTextures elytraOnly = new MinecraftProfileTextures(null, null, ELYTRA, SignatureState.SIGNED);

        assertSame(ELYTRA, MinecraftUsers.selectProfileTexture(elytraOnly, MinecraftProfileTexture.Type.ELYTRA));
    }

    @Test
    void absentTextureSlotsRemainAbsent() {
        MinecraftProfileTextures missingSkin = new MinecraftProfileTextures(null, CAPE, ELYTRA, SignatureState.SIGNED);
        MinecraftProfileTextures missingCape = new MinecraftProfileTextures(SKIN, null, ELYTRA, SignatureState.SIGNED);
        MinecraftProfileTextures missingElytra = new MinecraftProfileTextures(SKIN, CAPE, null, SignatureState.SIGNED);

        assertNull(MinecraftUsers.selectProfileTexture(missingSkin, MinecraftProfileTexture.Type.SKIN));
        assertNull(MinecraftUsers.selectProfileTexture(missingCape, MinecraftProfileTexture.Type.CAPE));
        assertNull(MinecraftUsers.selectProfileTexture(missingElytra, MinecraftProfileTexture.Type.ELYTRA));
    }

    @Test
    void failedLookupSentinelsRemainTypeSpecific() {
        assertSame(MinecraftUsers.MISSING_SKIN_TEXTURE, MinecraftUsers.selectProfileTexture(MinecraftUsers.MISSING_PROFILE_TEXTURES, MinecraftProfileTexture.Type.SKIN));
        assertSame(MinecraftUsers.MISSING_CAPE_TEXTURE, MinecraftUsers.selectProfileTexture(MinecraftUsers.MISSING_PROFILE_TEXTURES, MinecraftProfileTexture.Type.CAPE));
        assertSame(MinecraftUsers.MISSING_ELYTRA_TEXTURE, MinecraftUsers.selectProfileTexture(MinecraftUsers.MISSING_PROFILE_TEXTURES, MinecraftProfileTexture.Type.ELYTRA));
    }

    @Test
    void nullMappingInputsAreRejected() {
        assertThrows(NullPointerException.class, () -> MinecraftUsers.selectProfileTexture(null, MinecraftProfileTexture.Type.SKIN));
        assertThrows(NullPointerException.class, () -> MinecraftUsers.selectProfileTexture(COMPLETE_TEXTURES, null));
    }

}
