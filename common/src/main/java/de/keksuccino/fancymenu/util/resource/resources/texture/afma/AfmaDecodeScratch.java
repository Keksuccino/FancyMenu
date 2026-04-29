package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class AfmaDecodeScratch {

    @NotNull
    private byte[] previousRowBytes = new byte[0];
    @NotNull
    private byte[] filteredRowBytes = new byte[0];
    @NotNull
    private byte[] decodedRowBytes = new byte[0];
    @NotNull
    private int[] pixelRowColors = new int[0];
    @NotNull
    private int[] sparsePixelIndices = new int[0];

    @NotNull
    public byte[] borrowPreviousRow(int requiredLength) {
        this.previousRowBytes = ensureByteArrayCapacity(this.previousRowBytes, requiredLength);
        return this.previousRowBytes;
    }

    @NotNull
    public byte[] borrowFilteredRow(int requiredLength) {
        this.filteredRowBytes = ensureByteArrayCapacity(this.filteredRowBytes, requiredLength);
        return this.filteredRowBytes;
    }

    @NotNull
    public byte[] borrowDecodedRow(int requiredLength) {
        this.decodedRowBytes = ensureByteArrayCapacity(this.decodedRowBytes, requiredLength);
        return this.decodedRowBytes;
    }

    @NotNull
    public int[] borrowPixelRow(int requiredLength) {
        this.pixelRowColors = ensureIntArrayCapacity(this.pixelRowColors, requiredLength);
        return this.pixelRowColors;
    }

    @NotNull
    public int[] borrowSparsePixelIndices(int requiredLength) {
        this.sparsePixelIndices = ensureIntArrayCapacity(this.sparsePixelIndices, requiredLength);
        return this.sparsePixelIndices;
    }

    public void clearPreviousRow(int usedLength) {
        byte[] previousRow = this.borrowPreviousRow(usedLength);
        if (usedLength > 0) {
            Arrays.fill(previousRow, 0, usedLength, (byte) 0);
        }
    }

    public void clear() {
        this.previousRowBytes = new byte[0];
        this.filteredRowBytes = new byte[0];
        this.decodedRowBytes = new byte[0];
        this.pixelRowColors = new int[0];
        this.sparsePixelIndices = new int[0];
    }

    @NotNull
    private static byte[] ensureByteArrayCapacity(@NotNull byte[] current, int requiredLength) {
        int normalizedLength = Math.max(0, requiredLength);
        return (current.length >= normalizedLength) ? current : new byte[normalizedLength];
    }

    @NotNull
    private static int[] ensureIntArrayCapacity(@NotNull int[] current, int requiredLength) {
        int normalizedLength = Math.max(0, requiredLength);
        return (current.length >= normalizedLength) ? current : new int[normalizedLength];
    }

}
