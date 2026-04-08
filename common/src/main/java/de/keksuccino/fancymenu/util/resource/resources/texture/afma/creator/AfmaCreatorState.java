package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.file.FilenameComparator;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AfmaCreatorState {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final @NotNull AfmaOptimizationPreset DEFAULT_PRESET = AfmaOptimizationPreset.SMALLEST_FILE;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-AFMA-Creator");
        thread.setDaemon(true);
        return thread;
    });
    private final @NotNull AfmaEncodePlanner planner = new AfmaEncodePlanner();
    private final @NotNull AfmaArchiveWriter archiveWriter = new AfmaArchiveWriter();

    private volatile @Nullable File mainFramesDirectory;
    private volatile @Nullable File introFramesDirectory;
    private volatile @Nullable AfmaSourceSequence mainFramesSequenceOverride;
    private volatile @Nullable AfmaSourceSequence introFramesSequenceOverride;
    private volatile @NotNull String mainFramesInputText = "";
    private volatile @NotNull String introFramesInputText = "";
    private volatile @Nullable File outputFile;
    private volatile long frameTimeMs = 41L;
    private volatile long introFrameTimeMs = 41L;
    private volatile int loopCount = 0;
    private volatile int keyframeInterval = DEFAULT_PRESET.getKeyframeInterval();
    private volatile boolean rectCopyEnabled = DEFAULT_PRESET.isRectCopyEnabled();
    private volatile boolean duplicateFrameElision = DEFAULT_PRESET.isDuplicateFrameElision();
    private volatile boolean nearLosslessEnabled = DEFAULT_PRESET.isNearLosslessEnabledByDefault();
    private volatile int maxCopySearchDistance = DEFAULT_PRESET.getMaxCopySearchDistance();
    private volatile int maxCandidateAxisOffsets = DEFAULT_PRESET.getMaxCandidateAxisOffsets();
    private volatile @NotNull AfmaOptimizationPreset optimizationPreset = DEFAULT_PRESET;
    private volatile @Nullable AfmaEncodeJob currentJob;

    public @Nullable File getMainFramesDirectory() {
        return this.mainFramesDirectory;
    }

    public @NotNull String getMainFramesInputText() {
        return this.mainFramesInputText;
    }

    public void setMainFramesDirectory(@Nullable File mainFramesDirectory) {
        this.mainFramesDirectory = normalizeFile(mainFramesDirectory);
        this.mainFramesSequenceOverride = null;
        this.mainFramesInputText = (this.mainFramesDirectory != null) ? this.mainFramesDirectory.getPath().replace("\\", "/") : "";
        this.markDirty();
    }

    public @Nullable File getIntroFramesDirectory() {
        return this.introFramesDirectory;
    }

    public @NotNull String getIntroFramesInputText() {
        return this.introFramesInputText;
    }

    public void setIntroFramesDirectory(@Nullable File introFramesDirectory) {
        this.introFramesDirectory = normalizeFile(introFramesDirectory);
        this.introFramesSequenceOverride = null;
        this.introFramesInputText = (this.introFramesDirectory != null) ? this.introFramesDirectory.getPath().replace("\\", "/") : "";
        this.markDirty();
    }

    public void setMainFramesList(@NotNull List<File> frames) {
        this.mainFramesSequenceOverride = AfmaSourceSequence.ofFiles(frames);
        this.mainFramesDirectory = null;
        this.mainFramesInputText = summarizeSourceList("Ordered PNG List", frames);
        this.markDirty();
    }

    public void setIntroFramesList(@NotNull List<File> frames) {
        this.introFramesSequenceOverride = AfmaSourceSequence.ofFiles(frames);
        this.introFramesDirectory = null;
        this.introFramesInputText = summarizeSourceList("Ordered PNG List", frames);
        this.markDirty();
    }

    public @Nullable File getOutputFile() {
        return this.outputFile;
    }

    public void setOutputFile(@Nullable File outputFile) {
        this.outputFile = normalizeFile(outputFile);
        this.markDirty();
    }

    public long getFrameTimeMs() {
        return this.frameTimeMs;
    }

    public void setFrameTimeMs(long frameTimeMs) {
        this.frameTimeMs = frameTimeMs;
        this.markDirty();
    }

    public long getIntroFrameTimeMs() {
        return this.introFrameTimeMs;
    }

    public void setIntroFrameTimeMs(long introFrameTimeMs) {
        this.introFrameTimeMs = introFrameTimeMs;
        this.markDirty();
    }

    public int getLoopCount() {
        return this.loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
        this.markDirty();
    }

    public int getKeyframeInterval() {
        return this.keyframeInterval;
    }

    public void setKeyframeInterval(int keyframeInterval) {
        this.keyframeInterval = keyframeInterval;
        this.markDirty();
    }

    public boolean isRectCopyEnabled() {
        return this.rectCopyEnabled;
    }

    public void setRectCopyEnabled(boolean rectCopyEnabled) {
        this.rectCopyEnabled = rectCopyEnabled;
        this.markDirty();
    }

    public boolean isDuplicateFrameElision() {
        return this.duplicateFrameElision;
    }

    public void setDuplicateFrameElision(boolean duplicateFrameElision) {
        this.duplicateFrameElision = duplicateFrameElision;
        this.markDirty();
    }

    public boolean isNearLosslessEnabled() {
        return this.nearLosslessEnabled;
    }

    public void setNearLosslessEnabled(boolean nearLosslessEnabled) {
        this.nearLosslessEnabled = nearLosslessEnabled;
        this.markDirty();
    }

    public @NotNull AfmaOptimizationPreset getOptimizationPreset() {
        return this.optimizationPreset;
    }

    public void applyPreset(@NotNull AfmaOptimizationPreset preset) {
        this.optimizationPreset = Objects.requireNonNull(preset);
        this.keyframeInterval = preset.getKeyframeInterval();
        this.rectCopyEnabled = preset.isRectCopyEnabled();
        this.duplicateFrameElision = preset.isDuplicateFrameElision();
        this.nearLosslessEnabled = preset.isNearLosslessEnabledByDefault();
        this.maxCopySearchDistance = preset.getMaxCopySearchDistance();
        this.maxCandidateAxisOffsets = preset.getMaxCandidateAxisOffsets();
        this.markDirty();
    }

    public @Nullable AfmaEncodeJob getCurrentJob() {
        return this.currentJob;
    }

    public boolean isJobRunning() {
        AfmaEncodeJob job = this.currentJob;
        return (job != null) && job.isRunning();
    }

    public void cancelCurrentJob() {
        AfmaEncodeJob job = this.currentJob;
        if (job != null) {
            job.cancel();
        }
    }

    public void clearIntroFramesDirectory() {
        this.introFramesDirectory = null;
        this.introFramesSequenceOverride = null;
        this.introFramesInputText = "";
        this.markDirty();
    }

    public void markDirty() {
    }

    public void startExport() {
        if (this.isJobRunning()) {
            throw new IllegalStateException("Another AFMA creator job is already running");
        }

        PreparedInputs prepared = this.prepare(true);
        AfmaEncodeJob job = new AfmaEncodeJob();
        this.currentJob = job;
        this.executor.execute(() -> this.runExportJob(job, prepared));
    }

    protected void runExportJob(@NotNull AfmaEncodeJob job, @NotNull PreparedInputs prepared) {
        File tempFile = null;
        try {
            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.VALIDATING_SOURCES, "Validating AFMA creator input...", null, 0.10D));
            checkCancelled(job);

            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Analyzing AFMA frames for export...", null, 0.15D));
            AfmaEncodePlan exportPlan = this.planner.plan(prepared.mainSequence, prepared.introSequence, prepared.options, job::isCancellationRequested,
                    (detail, progress) -> job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Analyzing AFMA frames for export...", detail, 0.15D + (0.63D * progress))));
            checkCancelled(job);

            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.PACKING_ARCHIVE, "Packing AFMA archive...", prepared.outputFile.getName(), 0.82D));
            tempFile = new File(prepared.outputFile.getParentFile(), prepared.outputFile.getName() + ".tmp");
            org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            this.archiveWriter.write(exportPlan, tempFile, job::isCancellationRequested,
                    (path, progress) -> job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.PACKING_ARCHIVE, "Packing AFMA archive...", path, 0.82D + (0.16D * progress))));
            checkCancelled(job);

            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.PACKING_ARCHIVE, "Validating AFMA archive...", prepared.outputFile.getName(), 0.985D));
            this.validateWrittenArchive(tempFile);
            checkCancelled(job);

            Files.move(tempFile.toPath(), prepared.outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tempFile = null;

            job.completeSuccess(prepared.outputFile);
        } catch (CancellationException ex) {
            org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            job.completeCancelled();
        } catch (Throwable throwable) {
            org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            LOGGER.error("[FANCYMENU] AFMA creator export job failed", throwable);
            job.completeFailure(throwable);
        }
    }

    protected @NotNull PreparedInputs prepare(boolean requireOutput) {
        File mainDirectory = normalizeFile(this.mainFramesDirectory);
        File introDirectory = normalizeFile(this.introFramesDirectory);
        File output = normalizeFile(this.outputFile);
        if ((mainDirectory != null) && !mainDirectory.isDirectory()) {
            throw new IllegalArgumentException("Select a valid main frames directory or clear the main path.");
        }
        if ((introDirectory != null) && !introDirectory.isDirectory()) {
            throw new IllegalArgumentException("Select a valid intro frames directory or clear the intro path.");
        }
        if ((mainDirectory == null) && (introDirectory == null) && (this.mainFramesSequenceOverride == null) && (this.introFramesSequenceOverride == null)) {
            throw new IllegalArgumentException("Select a valid main or intro frames directory first.");
        }
        if (requireOutput) {
            if (output == null) {
                throw new IllegalArgumentException("Select a valid AFMA output path first.");
            }
            if (output.isDirectory()) {
                throw new IllegalArgumentException("The AFMA output path must point to a file, not a directory.");
            }
            if (!output.getName().toLowerCase(Locale.ROOT).endsWith(".afma")) {
                output = (output.getParentFile() != null)
                        ? new File(output.getParentFile(), output.getName() + ".afma")
                        : new File(output.getPath() + ".afma");
            }
        }

        ResolvedSource main = (this.mainFramesSequenceOverride != null)
                ? new ResolvedSource(this.mainFramesSequenceOverride)
                : ((mainDirectory != null) ? this.resolveSource(mainDirectory, true, "main") : ResolvedSource.empty());
        ResolvedSource intro = (this.introFramesSequenceOverride != null)
                ? new ResolvedSource(this.introFramesSequenceOverride)
                : ((introDirectory != null && introDirectory.isDirectory()) ? this.resolveSource(introDirectory, true, "intro") : ResolvedSource.empty());
        if (main.sequence.isEmpty() && intro.sequence.isEmpty()) {
            throw new IllegalArgumentException("The selected source directories do not contain any PNG frame files.");
        }

        AfmaEncodeOptions options = new AfmaEncodeOptions()
                .setFrameTimeMs(parsePositiveLong(this.frameTimeMs, "main frame time"))
                .setIntroFrameTimeMs(parsePositiveLong(this.introFrameTimeMs, "intro frame time"))
                .setLoopCount(this.loopCount)
                .setKeyframeInterval(parsePositiveInt(this.keyframeInterval, "keyframe interval"))
                .setRectCopyEnabled(this.rectCopyEnabled)
                .setDuplicateFrameElision(this.duplicateFrameElision)
                .setNearLosslessMaxChannelDelta(this.nearLosslessEnabled ? AfmaEncodeOptions.DEFAULT_NEAR_LOSSLESS_MAX_CHANNEL_DELTA : 0)
                .setMaxCopySearchDistance(this.maxCopySearchDistance)
                .setMaxCandidateAxisOffsets(this.maxCandidateAxisOffsets);
        this.applyAdvancedCompressionDefaults(options);

        return new PreparedInputs(
                main.sequence,
                intro.sequence,
                output,
                options
        );
    }

    protected void applyAdvancedCompressionDefaults(@NotNull AfmaEncodeOptions options) {
        int preferredKeyframeInterval = Math.max(1, options.getKeyframeInterval());
        switch (this.optimizationPreset) {
            case SMALLEST_FILE -> options
                    .setAdaptiveKeyframePlacement(true)
                    .setAdaptiveMaxKeyframeInterval(Math.max(preferredKeyframeInterval * 4, preferredKeyframeInterval + 180))
                    .setAdaptiveContinuationMinSavingsBytes(128L)
                    .setAdaptiveContinuationMinSavingsRatio(0.0025D);
            case BALANCED -> options
                    .setAdaptiveKeyframePlacement(true)
                    .setAdaptiveMaxKeyframeInterval(Math.max(preferredKeyframeInterval * 2, preferredKeyframeInterval + 36))
                    .setAdaptiveContinuationMinSavingsBytes(768L)
                    .setAdaptiveContinuationMinSavingsRatio(0.0075D);
            case FASTEST_DECODE -> options
                    .setAdaptiveKeyframePlacement(false)
                    .setAdaptiveMaxKeyframeInterval(preferredKeyframeInterval)
                    .setAdaptiveContinuationMinSavingsBytes(0L)
                    .setAdaptiveContinuationMinSavingsRatio(0D);
        }

        if (!this.nearLosslessEnabled) {
            options
                    .setPerceptualBinIntraMaxVisibleColorDelta(0)
                    .setPerceptualBinIntraMaxAlphaDelta(0)
                    .setPerceptualBinIntraMaxAverageError(0D);
            return;
        }

        switch (this.optimizationPreset) {
            case SMALLEST_FILE -> options
                    .setPerceptualBinIntraMaxVisibleColorDelta(16)
                    .setPerceptualBinIntraMaxAlphaDelta(32)
                    .setPerceptualBinIntraMaxAverageError(7.5D);
            case BALANCED -> options
                    .setPerceptualBinIntraMaxVisibleColorDelta(10)
                    .setPerceptualBinIntraMaxAlphaDelta(20)
                    .setPerceptualBinIntraMaxAverageError(4.0D);
            case FASTEST_DECODE -> options
                    .setPerceptualBinIntraMaxVisibleColorDelta(8)
                    .setPerceptualBinIntraMaxAlphaDelta(16)
                    .setPerceptualBinIntraMaxAverageError(3.0D);
        }
    }

    protected void validateWrittenArchive(@NotNull File archiveFile) throws IOException {
        try (AfmaDecoder decoder = new AfmaDecoder()) {
            decoder.read(archiveFile);
            decoder.validateAllReferencedPayloadHeaders();
        }
    }

    public void close() {
        this.cancelCurrentJob();
        this.executor.shutdownNow();
        this.currentJob = null;
    }

    protected @NotNull ResolvedSource resolveSource(@NotNull File directory, boolean optional, @NotNull String sequenceName) {
        File[] files = directory.listFiles(file -> file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".png"));
        if ((files == null) || (files.length == 0)) {
            if (optional) {
                return ResolvedSource.empty();
            }
            throw new IllegalArgumentException("The selected " + sequenceName + " directory does not contain any PNG frame files.");
        }

        List<File> naturalOrder = new ArrayList<>(List.of(files));
        naturalOrder.sort(Comparator.comparing(File::getName, new FilenameComparator()));

        return new ResolvedSource(AfmaSourceSequence.ofFiles(naturalOrder));
    }

    protected static int parsePositiveInt(int value, @NotNull String label) {
        if (value <= 0) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a value greater than 0.");
        }
        return value;
    }

    protected static long parsePositiveLong(long value, @NotNull String label) {
        if (value <= 0L) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a value greater than 0.");
        }
        return value;
    }

    protected static void checkCancelled(@Nullable AfmaEncodeJob job) {
        if ((job != null) && job.isCancellationRequested()) {
            throw new CancellationException("AFMA creator job was cancelled");
        }
    }

    protected static @Nullable File normalizeFile(@Nullable File file) {
        if (file == null) return null;
        return new File(file.getPath().replace("\\", "/"));
    }

    protected static @NotNull String summarizeSourceList(@NotNull String label, @NotNull List<File> frames) {
        String summary = label + " (" + frames.size() + " frames)";
        if (!frames.isEmpty()) {
            summary += ": " + frames.get(0).getName();
        }
        return summary;
    }

    protected record PreparedInputs(
            @NotNull AfmaSourceSequence mainSequence,
            @NotNull AfmaSourceSequence introSequence,
            @Nullable File outputFile,
            @NotNull AfmaEncodeOptions options
    ) {
    }

    protected record ResolvedSource(@NotNull AfmaSourceSequence sequence) {
        public static @NotNull ResolvedSource empty() {
            return new ResolvedSource(AfmaSourceSequence.empty());
        }
    }

}
