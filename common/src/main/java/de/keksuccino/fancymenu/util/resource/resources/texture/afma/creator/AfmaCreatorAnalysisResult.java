package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AfmaCreatorAnalysisResult(
        @NotNull AfmaEncodePlan plan,
        @NotNull AfmaSourceSequence mainSequence,
        @NotNull AfmaSourceSequence introSequence,
        @NotNull AfmaEncodeAnalyzer.Summary summary,
        boolean alphaUsed,
        long estimatedArchiveBytes,
        @NotNull List<String> warnings
) {
}
