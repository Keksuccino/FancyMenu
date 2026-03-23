package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import org.jetbrains.annotations.NotNull;

public class AfmaEncodeAnalyzer {

    @NotNull
    public Summary summarize(@NotNull AfmaEncodePlan plan) {
        return new Summary(
                plan.getFrameIndex().getFrames().size(),
                plan.getFrameIndex().getIntroFrames().size(),
                plan.countFrames(AfmaFrameOperationType.FULL),
                plan.countFrames(AfmaFrameOperationType.DELTA_RECT) + plan.countFrames(AfmaFrameOperationType.SPARSE_DELTA_RECT),
                plan.countFrames(AfmaFrameOperationType.SAME),
                plan.countFrames(AfmaFrameOperationType.COPY_RECT_PATCH),
                plan.getTotalPayloadBytes()
        );
    }

    public record Summary(int mainFrameCount, int introFrameCount, int fullFrames, int deltaRectFrames, int sameFrames, int copyRectPatchFrames, long payloadBytes) {
    }

}
