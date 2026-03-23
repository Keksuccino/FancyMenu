package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class AfmaCreatorState {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-AFMA-Creator");
        thread.setDaemon(true);
        return thread;
    });
    private static final Gson GSON = new GsonBuilder().create();

    private final @NotNull AfmaEncodePlanner planner = new AfmaEncodePlanner();
    private final @NotNull AfmaEncodeAnalyzer analyzer = new AfmaEncodeAnalyzer();
    private final @NotNull AfmaArchiveWriter archiveWriter = new AfmaArchiveWriter();
    private final @NotNull AtomicLong configurationVersion = new AtomicLong(0L);

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
    private volatile int keyframeInterval = AfmaOptimizationPreset.BALANCED.getKeyframeInterval();
    private volatile boolean rectCopyEnabled = AfmaOptimizationPreset.BALANCED.isRectCopyEnabled();
    private volatile boolean duplicateFrameElision = AfmaOptimizationPreset.BALANCED.isDuplicateFrameElision();
    private volatile int maxCopySearchDistance = AfmaOptimizationPreset.BALANCED.getMaxCopySearchDistance();
    private volatile int maxCandidateAxisOffsets = AfmaOptimizationPreset.BALANCED.getMaxCandidateAxisOffsets();
    private volatile boolean generateThumbnail = AfmaOptimizationPreset.BALANCED.isThumbnailEnabledByDefault();
    private volatile @NotNull String customFrameTimesText = "";
    private volatile @NotNull String customIntroFrameTimesText = "";
    private volatile @NotNull AfmaOptimizationPreset optimizationPreset = AfmaOptimizationPreset.BALANCED;
    private volatile boolean analysisDirty = true;
    private volatile @Nullable AfmaCreatorAnalysisResult analysisResult;
    private volatile @Nullable AfmaEncodeJob currentJob;
    private volatile @Nullable CachedPlan cachedPlan;
    private volatile long successfulAnalysisVersion = -1L;

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

    public boolean isGenerateThumbnail() {
        return this.generateThumbnail;
    }

    public void setGenerateThumbnail(boolean generateThumbnail) {
        this.generateThumbnail = generateThumbnail;
        this.markDirty();
    }

    public @NotNull String getCustomFrameTimesText() {
        return this.customFrameTimesText;
    }

    public void setCustomFrameTimesText(@Nullable String customFrameTimesText) {
        this.customFrameTimesText = (customFrameTimesText != null) ? customFrameTimesText : "";
        this.markDirty();
    }

    public @NotNull String getCustomIntroFrameTimesText() {
        return this.customIntroFrameTimesText;
    }

    public void setCustomIntroFrameTimesText(@Nullable String customIntroFrameTimesText) {
        this.customIntroFrameTimesText = (customIntroFrameTimesText != null) ? customIntroFrameTimesText : "";
        this.markDirty();
    }

    public @NotNull AfmaOptimizationPreset getOptimizationPreset() {
        return this.optimizationPreset;
    }

    public void setOptimizationPreset(@NotNull AfmaOptimizationPreset optimizationPreset) {
        this.applyPreset(optimizationPreset);
    }

    public void applyPreset(@NotNull AfmaOptimizationPreset preset) {
        this.optimizationPreset = Objects.requireNonNull(preset);
        this.keyframeInterval = preset.getKeyframeInterval();
        this.rectCopyEnabled = preset.isRectCopyEnabled();
        this.duplicateFrameElision = preset.isDuplicateFrameElision();
        this.maxCopySearchDistance = preset.getMaxCopySearchDistance();
        this.maxCandidateAxisOffsets = preset.getMaxCandidateAxisOffsets();
        this.generateThumbnail = preset.isThumbnailEnabledByDefault();
        this.markDirty();
    }

    public boolean isAnalysisDirty() {
        return this.analysisDirty;
    }

    public @Nullable AfmaCreatorAnalysisResult getAnalysisResult() {
        return this.analysisResult;
    }

    public @Nullable AfmaEncodeJob getCurrentJob() {
        return this.currentJob;
    }

    public boolean isJobRunning() {
        AfmaEncodeJob job = this.currentJob;
        return (job != null) && job.isRunning();
    }

    public @Nullable AfmaEncodePlan getPreviewPlan() {
        CachedPlan plan = this.cachedPlan;
        if ((plan == null) || (plan.configurationVersion != this.successfulAnalysisVersion)) {
            return null;
        }
        try {
            return plan.openPlan();
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to open cached AFMA preview plan", ex);
            this.clearCachedPlan();
            return null;
        }
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
        this.clearCachedPlan();
        this.analysisDirty = true;
        this.configurationVersion.incrementAndGet();
    }

    public void startAnalysis() {
        if (this.isJobRunning()) {
            throw new IllegalStateException("Another AFMA creator job is already running");
        }

        PreparedInputs prepared = this.prepare(false);
        AfmaEncodeJob job = new AfmaEncodeJob(AfmaEncodeJob.Kind.ANALYZE);
        this.currentJob = job;
        this.executor.execute(() -> this.runAnalysisJob(job, prepared));
    }

    public void startExport() {
        if (this.isJobRunning()) {
            throw new IllegalStateException("Another AFMA creator job is already running");
        }

        PreparedInputs prepared = this.prepare(true);
        AfmaEncodeJob job = new AfmaEncodeJob(AfmaEncodeJob.Kind.EXPORT);
        this.currentJob = job;
        this.executor.execute(() -> this.runExportJob(job, prepared));
    }

    protected void runAnalysisJob(@NotNull AfmaEncodeJob job, @NotNull PreparedInputs prepared) {
        try {
            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.VALIDATING_SOURCES, "Validating AFMA creator input...", null, 0.10D));
            checkCancelled(job);

            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Analyzing AFMA frames...", null, 0.15D));
            AfmaEncodePlan plan = this.planner.plan(prepared.mainSequence, prepared.introSequence, prepared.options, job::isCancellationRequested,
                    (detail, progress) -> job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Analyzing AFMA frames...", detail, 0.15D + (0.60D * progress))));
            checkCancelled(job);

            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Summarizing AFMA analysis...", null, 0.80D));
            AfmaCreatorAnalysisResult result = this.buildAnalysisResult(plan, prepared.mainSequence, prepared.introSequence, prepared.warnings, job);
            checkCancelled(job);

            CachedPlan cachedPlan = this.createCachedPlan(prepared.configurationVersion, plan);
            if (this.configurationVersion.get() == prepared.configurationVersion) {
                this.analysisResult = result;
                this.analysisDirty = false;
                this.replaceCachedPlan(cachedPlan);
                this.successfulAnalysisVersion = prepared.configurationVersion;
            } else {
                cachedPlan.close();
            }

            job.completeSuccess(result, null);
        } catch (CancellationException ex) {
            job.completeCancelled();
        } catch (Throwable throwable) {
            LOGGER.error("[FANCYMENU] AFMA creator analysis job failed", throwable);
            job.completeFailure(throwable);
        }
    }

    protected void runExportJob(@NotNull AfmaEncodeJob job, @NotNull PreparedInputs prepared) {
        File tempFile = null;
        CachedPlan refreshedCache = null;
        try {
            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.VALIDATING_SOURCES, "Validating AFMA creator input...", null, 0.10D));
            checkCancelled(job);

            AfmaEncodePlan exportPlan = this.resolveExportPlan(prepared, job);
            checkCancelled(job);

            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Building AFMA export summary...", null, 0.68D));
            AfmaCreatorAnalysisResult exportAnalysis = this.buildAnalysisResult(exportPlan, prepared.mainSequence, prepared.introSequence, prepared.warnings, job);
            checkCancelled(job);

            LinkedHashMap<String, byte[]> exportPayloads = new LinkedHashMap<>(exportPlan.getPayloads());
            if (prepared.generateThumbnail) {
                job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Generating AFMA thumbnail...", null, 0.78D));
                byte[] thumbnail = this.buildThumbnailBytes(prepared.mainSequence, prepared.introSequence);
                if (thumbnail != null) {
                    exportPayloads.put("thumbnail.png", thumbnail);
                }
            }

            exportPlan = new AfmaEncodePlan(exportPlan.getMetadata(), exportPlan.getFrameIndex(), exportPayloads, exportPlan.getTotalPayloadBytes());

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

            if ((this.cachedPlan == null) || (this.cachedPlan.configurationVersion != prepared.configurationVersion)) {
                refreshedCache = this.createCachedPlan(prepared.configurationVersion, exportPlan);
            }
            if (this.configurationVersion.get() == prepared.configurationVersion) {
                this.analysisResult = exportAnalysis;
                this.analysisDirty = false;
                if (refreshedCache != null) {
                    this.replaceCachedPlan(refreshedCache);
                    refreshedCache = null;
                }
                this.successfulAnalysisVersion = prepared.configurationVersion;
            }

            job.completeSuccess(exportAnalysis, prepared.outputFile);
        } catch (CancellationException ex) {
            org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            closeQuietly(refreshedCache);
            job.completeCancelled();
        } catch (Throwable throwable) {
            org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            closeQuietly(refreshedCache);
            LOGGER.error("[FANCYMENU] AFMA creator export job failed", throwable);
            job.completeFailure(throwable);
        }
    }

    protected @NotNull AfmaEncodePlan resolveExportPlan(@NotNull PreparedInputs prepared, @NotNull AfmaEncodeJob job) throws IOException {
        CachedPlan cachedPlan = this.cachedPlan;
        if ((cachedPlan != null) && (cachedPlan.configurationVersion == prepared.configurationVersion)) {
            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Reusing analyzed AFMA export plan...", null, 0.55D));
            return cachedPlan.openPlan();
        }

        job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Analyzing AFMA frames for export...", null, 0.15D));
        return this.planner.plan(prepared.mainSequence, prepared.introSequence, prepared.options, job::isCancellationRequested,
                (detail, progress) -> job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Analyzing AFMA frames for export...", detail, 0.15D + (0.50D * progress))));
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
                ? new ResolvedSource(this.mainFramesSequenceOverride, List.of())
                : ((mainDirectory != null) ? this.resolveSource(mainDirectory, true, "main") : ResolvedSource.empty());
        ResolvedSource intro = (this.introFramesSequenceOverride != null)
                ? new ResolvedSource(this.introFramesSequenceOverride, List.of())
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
                .setMaxCopySearchDistance(this.maxCopySearchDistance)
                .setMaxCandidateAxisOffsets(this.maxCandidateAxisOffsets)
                .setCustomFrameTimes(parseCustomFrameTimes(this.customFrameTimesText))
                .setCustomIntroFrameTimes(parseCustomFrameTimes(this.customIntroFrameTimesText));

        List<String> warnings = new ArrayList<>();
        warnings.addAll(main.warnings);
        warnings.addAll(intro.warnings);
        if ((output != null) && output.isFile()) {
            warnings.add("The selected output file already exists and will be overwritten.");
        }

        return new PreparedInputs(
                this.configurationVersion.get(),
                main.sequence,
                intro.sequence,
                output,
                options,
                this.generateThumbnail,
                warnings
        );
    }

    protected @NotNull AfmaCreatorAnalysisResult buildAnalysisResult(@NotNull AfmaEncodePlan plan, @NotNull AfmaSourceSequence mainSequence,
                                                                    @NotNull AfmaSourceSequence introSequence, @NotNull List<String> preparedWarnings,
                                                                    @Nullable AfmaEncodeJob job) throws IOException {
        AfmaEncodeAnalyzer.Summary summary = this.analyzer.summarize(plan);
        List<String> warnings = new ArrayList<>(preparedWarnings);
        if ((summary.fullFrames() == (summary.mainFrameCount() + summary.introFrameCount())) && ((summary.mainFrameCount() + summary.introFrameCount()) > 1)) {
            warnings.add("No useful AFMA frame optimization was found for this input sequence.");
        }

        if (job != null) {
            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Checking AFMA alpha usage...", null, 0.88D));
        }
        boolean alphaUsed = this.detectAlphaUsage(mainSequence, introSequence, job);
        long estimatedArchiveBytes = estimateArchiveBytes(plan);
        return new AfmaCreatorAnalysisResult(plan.withoutPayloads(), mainSequence, introSequence, summary, alphaUsed, estimatedArchiveBytes, List.copyOf(warnings));
    }

    protected boolean detectAlphaUsage(@NotNull AfmaSourceSequence mainSequence, @NotNull AfmaSourceSequence introSequence, @Nullable AfmaEncodeJob job) throws IOException {
        AfmaFrameNormalizer normalizer = new AfmaFrameNormalizer();
        List<File> frames = new ArrayList<>(mainSequence.getFrames());
        frames.addAll(introSequence.getFrames());
        for (int i = 0; i < frames.size(); i++) {
            checkCancelled(job);
            if (job != null) {
                double progress = 0.88D + (0.07D * ((double) (i + 1) / Math.max(1, frames.size())));
                job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Checking AFMA alpha usage...", frames.get(i).getName(), progress));
            }
            try (AfmaPixelFrame frame = normalizer.loadFrame(frames.get(i))) {
                if (frame.hasAlpha()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected @Nullable byte[] buildThumbnailBytes(@NotNull AfmaSourceSequence mainSequence, @NotNull AfmaSourceSequence introSequence) throws IOException {
        File sourceFile = !introSequence.isEmpty() ? introSequence.getFrame(0) : mainSequence.getFrame(0);
        if (sourceFile == null) {
            return null;
        }

        AfmaFrameNormalizer normalizer = new AfmaFrameNormalizer();
        try (AfmaPixelFrame source = normalizer.loadFrame(sourceFile)) {
            int maxWidth = 320;
            int maxHeight = 180;
            double scale = Math.min((double) maxWidth / source.getWidth(), (double) maxHeight / source.getHeight());
            scale = Math.min(1.0D, scale);
            int outWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int outHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));

            int[] thumbnailPixels = new int[outWidth * outHeight];
            for (int y = 0; y < outHeight; y++) {
                int srcY = Math.min(source.getHeight() - 1, (int) (((double) y / Math.max(1, outHeight - 1)) * Math.max(0, source.getHeight() - 1)));
                for (int x = 0; x < outWidth; x++) {
                    int srcX = Math.min(source.getWidth() - 1, (int) (((double) x / Math.max(1, outWidth - 1)) * Math.max(0, source.getWidth() - 1)));
                    thumbnailPixels[(y * outWidth) + x] = source.getPixelRGBA(srcX, srcY);
                }
            }

            try (AfmaPixelFrame thumbnail = new AfmaPixelFrame(outWidth, outHeight, thumbnailPixels)) {
                return thumbnail.asByteArray();
            }
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
        this.clearCachedPlan();
        this.currentJob = null;
    }

    protected void replaceCachedPlan(@Nullable CachedPlan cachedPlan) {
        CachedPlan previousPlan = this.cachedPlan;
        this.cachedPlan = cachedPlan;
        closeQuietly(previousPlan);
    }

    protected void clearCachedPlan() {
        this.replaceCachedPlan(null);
    }

    protected @NotNull CachedPlan createCachedPlan(long configurationVersion, @NotNull AfmaEncodePlan plan) throws IOException {
        Path payloadDirectory = Files.createTempDirectory("fancymenu_afma_creator_plan_");
        boolean success = false;
        try {
            List<String> payloadPaths = new ArrayList<>(plan.getPayloads().size());
            for (var entry : plan.getPayloads().entrySet()) {
                Path outputPath = payloadDirectory.resolve(entry.getKey());
                Path parent = outputPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(outputPath, entry.getValue());
                payloadPaths.add(entry.getKey());
            }
            success = true;
            return new CachedPlan(configurationVersion, plan.withoutPayloads(), payloadDirectory.toFile(), List.copyOf(payloadPaths));
        } finally {
            if (!success) {
                org.apache.commons.io.FileUtils.deleteQuietly(payloadDirectory.toFile());
            }
        }
    }

    protected static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
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

        List<File> lexicalOrder = new ArrayList<>(List.of(files));
        lexicalOrder.sort(Comparator.comparing(file -> file.getName().toLowerCase(Locale.ROOT)));

        List<String> warnings = new ArrayList<>();
        if (!sameOrder(naturalOrder, lexicalOrder)) {
            warnings.add("The " + sequenceName + " frame filenames are not lexically stable. The creator uses natural number ordering.");
        }

        return new ResolvedSource(AfmaSourceSequence.ofFiles(naturalOrder), warnings);
    }

    protected static boolean sameOrder(@NotNull List<File> first, @NotNull List<File> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            if (!Objects.equals(first.get(i).getName(), second.get(i).getName())) {
                return false;
            }
        }
        return true;
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

    protected static @NotNull LinkedHashMap<Integer, Long> parseCustomFrameTimes(@Nullable String value) {
        LinkedHashMap<Integer, Long> result = new LinkedHashMap<>();
        if ((value == null) || value.isBlank()) {
            return result;
        }

        String[] parts = value.split("[,;\\n]+");
        for (String rawPart : parts) {
            String part = rawPart.trim();
            if (part.isEmpty()) continue;

            String[] pair = part.split("=");
            if (pair.length != 2) {
                throw new IllegalArgumentException("Invalid custom frame timing entry: '" + part + "'. Use index=milliseconds.");
            }

            int frameIndex;
            long delay;
            try {
                frameIndex = Integer.parseInt(pair[0].trim());
                delay = Long.parseLong(pair[1].trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid custom frame timing entry: '" + part + "'. Use index=milliseconds.");
            }

            if (frameIndex < 0 || delay <= 0L) {
                throw new IllegalArgumentException("Invalid custom frame timing entry: '" + part + "'. Frame index must be >= 0 and delay must be > 0.");
            }
            result.put(frameIndex, delay);
        }
        return result;
    }

    protected static long estimateArchiveBytes(@NotNull AfmaEncodePlan plan) {
        long total = plan.getTotalPayloadBytes();
        total += GSON.toJson(plan.getMetadata()).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        total += GSON.toJson(plan.getFrameIndex()).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        total += (long) plan.getPayloads().size() * 96L;
        return total;
    }

    protected static void checkCancelled(@NotNull AfmaEncodeJob job) {
        if (job.isCancellationRequested()) {
            throw new CancellationException("AFMA creator job was cancelled");
        }
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
            long configurationVersion,
            @NotNull AfmaSourceSequence mainSequence,
            @NotNull AfmaSourceSequence introSequence,
            @Nullable File outputFile,
            @NotNull AfmaEncodeOptions options,
            boolean generateThumbnail,
            @NotNull List<String> warnings
    ) {
    }

    protected record CachedPlan(long configurationVersion, @NotNull AfmaEncodePlan summaryPlan,
                                @NotNull File payloadDirectory, @NotNull List<String> payloadPaths) implements AutoCloseable {
        public @NotNull AfmaEncodePlan openPlan() throws IOException {
            return AfmaEncodePlan.lazy(
                    this.summaryPlan.getMetadata(),
                    this.summaryPlan.getFrameIndex(),
                    this.payloadPaths,
                    this.summaryPlan.getTotalPayloadBytes(),
                    this::readPayload
            );
        }

        private @NotNull byte[] readPayload(@NotNull String path) throws IOException {
            return Files.readAllBytes(this.payloadDirectory.toPath().resolve(path));
        }

        @Override
        public void close() {
            org.apache.commons.io.FileUtils.deleteQuietly(this.payloadDirectory);
        }
    }

    protected record ResolvedSource(@NotNull AfmaSourceSequence sequence, @NotNull List<String> warnings) {
        public static @NotNull ResolvedSource empty() {
            return new ResolvedSource(AfmaSourceSequence.empty(), List.of());
        }
    }

}
