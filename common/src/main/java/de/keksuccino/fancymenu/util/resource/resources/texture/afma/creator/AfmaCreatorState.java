package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-AFMA-Creator");
        thread.setDaemon(true);
        return thread;
    });
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final @NotNull AfmaEncodePlanner planner = new AfmaEncodePlanner();
    private final @NotNull AfmaEncodeAnalyzer analyzer = new AfmaEncodeAnalyzer();
    private final @NotNull AfmaArchiveWriter archiveWriter = new AfmaArchiveWriter();
    private final @NotNull AtomicLong configurationVersion = new AtomicLong(0L);

    private volatile @Nullable File mainFramesDirectory;
    private volatile @Nullable File introFramesDirectory;
    private volatile @Nullable File outputFile;
    private volatile long frameTimeMs = 41L;
    private volatile long introFrameTimeMs = 41L;
    private volatile int loopCount = 0;
    private volatile int keyframeInterval = 30;
    private volatile boolean rectCopyEnabled = true;
    private volatile boolean duplicateFrameElision = true;
    private volatile boolean generateThumbnail = true;
    private volatile @NotNull String customFrameTimesText = "";
    private volatile @NotNull String customIntroFrameTimesText = "";
    private volatile @NotNull AfmaOptimizationPreset optimizationPreset = AfmaOptimizationPreset.BALANCED;
    private volatile boolean analysisDirty = true;
    private volatile @Nullable AfmaCreatorAnalysisResult analysisResult;
    private volatile @Nullable AfmaEncodeJob currentJob;

    public @Nullable File getMainFramesDirectory() {
        return this.mainFramesDirectory;
    }

    public void setMainFramesDirectory(@Nullable File mainFramesDirectory) {
        this.mainFramesDirectory = normalizeFile(mainFramesDirectory);
        this.markDirty();
    }

    public @Nullable File getIntroFramesDirectory() {
        return this.introFramesDirectory;
    }

    public void setIntroFramesDirectory(@Nullable File introFramesDirectory) {
        this.introFramesDirectory = normalizeFile(introFramesDirectory);
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
        this.optimizationPreset = Objects.requireNonNull(optimizationPreset);
        this.markDirty();
    }

    public void applyPreset(@NotNull AfmaOptimizationPreset preset) {
        this.optimizationPreset = Objects.requireNonNull(preset);
        this.keyframeInterval = preset.getKeyframeInterval();
        this.rectCopyEnabled = preset.isRectCopyEnabled();
        this.duplicateFrameElision = preset.isDuplicateFrameElision();
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

    public void cancelCurrentJob() {
        AfmaEncodeJob job = this.currentJob;
        if (job != null) {
            job.cancel();
        }
    }

    public void clearIntroFramesDirectory() {
        this.introFramesDirectory = null;
        this.markDirty();
    }

    public void markDirty() {
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
        EXECUTOR.execute(() -> this.runAnalysisJob(job, prepared));
    }

    public void startExport() {
        if (this.isJobRunning()) {
            throw new IllegalStateException("Another AFMA creator job is already running");
        }

        PreparedInputs prepared = this.prepare(true);
        AfmaEncodeJob job = new AfmaEncodeJob(AfmaEncodeJob.Kind.EXPORT);
        this.currentJob = job;
        EXECUTOR.execute(() -> this.runExportJob(job, prepared));
    }

    protected void runAnalysisJob(@NotNull AfmaEncodeJob job, @NotNull PreparedInputs prepared) {
        try {
            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.VALIDATING_SOURCES, "Validating AFMA creator input...", null, 0.10D));
            checkCancelled(job);

            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Analyzing AFMA frames...", null, 0.55D));
            AfmaEncodePlan plan = this.planner.plan(prepared.mainSequence, prepared.introSequence, prepared.options, job::isCancellationRequested);
            checkCancelled(job);

            AfmaCreatorAnalysisResult result = this.buildAnalysisResult(plan, prepared.warnings);
            checkCancelled(job);

            if (this.configurationVersion.get() == prepared.configurationVersion) {
                this.analysisResult = result;
                this.analysisDirty = false;
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
        try {
            AfmaCreatorAnalysisResult exportAnalysis = this.analysisResult;
            if (this.analysisDirty || (exportAnalysis == null) || (this.configurationVersion.get() != prepared.configurationVersion)) {
                job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.VALIDATING_SOURCES, "Validating AFMA creator input...", null, 0.10D));
                checkCancelled(job);

                job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.ANALYZING_FRAMES, "Analyzing AFMA frames for export...", null, 0.55D));
                AfmaEncodePlan plan = this.planner.plan(prepared.mainSequence, prepared.introSequence, prepared.options, job::isCancellationRequested);
                checkCancelled(job);
                exportAnalysis = this.buildAnalysisResult(plan, prepared.warnings);
            }

            Objects.requireNonNull(exportAnalysis, "AFMA export analysis result is missing");

            LinkedHashMap<String, byte[]> exportPayloads = new LinkedHashMap<>(exportAnalysis.plan().getPayloads());
            if (prepared.generateThumbnail) {
                byte[] thumbnail = this.buildThumbnailBytes(exportAnalysis.plan());
                if (thumbnail != null) {
                    exportPayloads.put("thumbnail.png", thumbnail);
                }
            }

            AfmaEncodePlan exportPlan = new AfmaEncodePlan(exportAnalysis.plan().getMetadata(), exportAnalysis.plan().getFrameIndex(), exportPayloads);

            job.setProgress(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.PACKING_ARCHIVE, "Packing AFMA archive...", prepared.outputFile.getName(), 0.85D));
            tempFile = new File(prepared.outputFile.getParentFile(), prepared.outputFile.getName() + ".tmp");
            org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
            this.archiveWriter.write(exportPlan, tempFile, job::isCancellationRequested);
            checkCancelled(job);

            Files.move(tempFile.toPath(), prepared.outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tempFile = null;

            if (this.configurationVersion.get() == prepared.configurationVersion) {
                this.analysisResult = exportAnalysis;
                this.analysisDirty = false;
            }

            job.completeSuccess(exportAnalysis, prepared.outputFile);
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
        if ((mainDirectory == null) || !mainDirectory.isDirectory()) {
            throw new IllegalArgumentException("Select a valid main frames directory first.");
        }

        File introDirectory = normalizeFile(this.introFramesDirectory);
        File output = normalizeFile(this.outputFile);
        if ((introDirectory != null) && !introDirectory.isDirectory()) {
            throw new IllegalArgumentException("Select a valid intro frames directory or clear the intro path.");
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

        ResolvedSource main = this.resolveSource(mainDirectory, false, "main");
        ResolvedSource intro = (introDirectory != null && introDirectory.isDirectory()) ? this.resolveSource(introDirectory, true, "intro") : ResolvedSource.empty();

        AfmaEncodeOptions options = new AfmaEncodeOptions()
                .setFrameTimeMs(parsePositiveLong(this.frameTimeMs, "main frame time"))
                .setIntroFrameTimeMs(parsePositiveLong(this.introFrameTimeMs, "intro frame time"))
                .setLoopCount(this.loopCount)
                .setKeyframeInterval(parsePositiveInt(this.keyframeInterval, "keyframe interval"))
                .setRectCopyEnabled(this.rectCopyEnabled)
                .setDuplicateFrameElision(this.duplicateFrameElision)
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

    protected @NotNull AfmaCreatorAnalysisResult buildAnalysisResult(@NotNull AfmaEncodePlan plan, @NotNull List<String> preparedWarnings) throws IOException {
        AfmaEncodeAnalyzer.Summary summary = this.analyzer.summarize(plan);
        List<String> warnings = new ArrayList<>(preparedWarnings);
        if ((summary.fullFrames() == (summary.mainFrameCount() + summary.introFrameCount())) && ((summary.mainFrameCount() + summary.introFrameCount()) > 1)) {
            warnings.add("No useful AFMA frame optimization was found for this input sequence.");
        }

        boolean alphaUsed = this.detectAlphaUsage(plan);
        long estimatedArchiveBytes = estimateArchiveBytes(plan);
        return new AfmaCreatorAnalysisResult(plan, summary, alphaUsed, estimatedArchiveBytes, List.copyOf(warnings));
    }

    protected boolean detectAlphaUsage(@NotNull AfmaEncodePlan plan) throws IOException {
        for (byte[] payload : plan.getPayloads().values()) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(payload));
            if (image == null) {
                throw new IOException("Failed to decode AFMA payload while checking alpha usage");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            for (int color : pixels) {
                if (((color >>> 24) & 0xFF) != 0xFF) {
                    return true;
                }
            }
        }
        return false;
    }

    protected @Nullable byte[] buildThumbnailBytes(@NotNull AfmaEncodePlan plan) throws IOException {
        String sourcePath = null;
        if (!plan.getFrameIndex().getIntroFrames().isEmpty()) {
            sourcePath = plan.getFrameIndex().getIntroFrames().get(0).getPath();
        }
        if ((sourcePath == null) && !plan.getFrameIndex().getFrames().isEmpty()) {
            sourcePath = plan.getFrameIndex().getFrames().get(0).getPath();
        }
        if (sourcePath == null) {
            return null;
        }

        byte[] payload = plan.getPayloads().get(sourcePath);
        if (payload == null) {
            return null;
        }

        BufferedImage source = ImageIO.read(new ByteArrayInputStream(payload));
        if (source == null) {
            throw new IOException("Failed to decode AFMA payload while building thumbnail");
        }

        int maxWidth = 320;
        int maxHeight = 180;
        double scale = Math.min((double) maxWidth / source.getWidth(), (double) maxHeight / source.getHeight());
        scale = Math.min(1.0D, scale);
        int outWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int outHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage thumbnail = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < outHeight; y++) {
            int srcY = Math.min(source.getHeight() - 1, (int) (((double) y / Math.max(1, outHeight - 1)) * Math.max(0, source.getHeight() - 1)));
            for (int x = 0; x < outWidth; x++) {
                int srcX = Math.min(source.getWidth() - 1, (int) (((double) x / Math.max(1, outWidth - 1)) * Math.max(0, source.getWidth() - 1)));
                thumbnail.setRGB(x, y, source.getRGB(srcX, srcY));
            }
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(thumbnail, "png", out)) {
                throw new IOException("Failed to encode AFMA thumbnail PNG");
            }
            return out.toByteArray();
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

    protected static @Nullable File normalizeFile(@Nullable File file) {
        if (file == null) return null;
        return new File(file.getPath().replace("\\", "/"));
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

    protected record ResolvedSource(@NotNull AfmaSourceSequence sequence, @NotNull List<String> warnings) {
        public static @NotNull ResolvedSource empty() {
            return new ResolvedSource(AfmaSourceSequence.empty(), List.of());
        }
    }

}
