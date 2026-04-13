I analyzed the AFMA encoder with four parallel explorer agents. The main verdict is that AFMA is slow mostly because the encoder keeps redoing
expensive work: it rescans frame data, explores too many candidate modes, rematerializes payloads, and repeatedly re-simulates archive packing
instead of carrying state forward.

For a professional, maximum-optimized encoder, I would change it in this order:

1. Rebuild the planner around incremental archive scoring. The biggest win is to stop doing near-full archive refinement inside the beam search
   for every promising transition. planWindow() keeps expanding candidates and then calls exact archive refinement again and again in common/
   src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:385, common/src/main/java/de/
   keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:458, and common/src/main/java/de/keksuccino/
   fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:5594. The right end-state is a persistent PackedArchiveState
   with delta updates, plus cheap lower/upper bounds so most candidates never reach full exact scoring.
2. Replace the temp-file payload model with a spillable blob store. AfmaStoredPayload writes unique payloads to temp files and later reopens/
   streams them again in common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaStoredPayload.java:50 and common/
   src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaStoredPayload.java:168. That is a major architectural tax. The
   optimized design is a PayloadSource abstraction backed by byte[], pooled direct ByteBuffer, or spill-to-disk only above a threshold, with
   payload bytes materialized once and reused everywhere.
3. Build a shared frame-pair analysis index and make every strategy consume it. Right now full-frame drift scans, diff scans, dirty-after-copy
   scans, and region rescans happen repeatedly in common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/
   AfmaFramePairAnalysis.java:117, common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/
   AfmaFramePairAnalysis.java:161, and common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/
   AfmaFramePairAnalysis.java:201. AFMA needs a codec-style preanalysis pass that produces dirty masks, ROI bounds, per-tile hashes, per-tile
   error metrics, summed-area tables, and perceptual summaries once per frame pair.
4. Aggressively prune candidate fan-out before any real encode. BIN_INTRA, rect-copy, multi-copy, sparse layouts, and block-inter are all
   exploring too much search space up front in common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/
   AfmaBinIntraPayloadHelper.java:774, common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/
   AfmaRectCopyDetector.java:43, and common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/
   AfmaEncodePlanner.java:1843. The professional approach is a multi-stage sieve: cheap heuristic estimate, then medium-cost estimate, then full
   encode only for the top few candidates.
5. Replace brute-force motion search with hierarchical search. Rect-copy and block-inter still spend too much time doing dense dx/dy evaluation
   and tile SAD refinement in common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/
   AfmaRectCopyDetector.java:203 and common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/
   AfmaEncodePlanner.java:2333. Move to coarse-to-fine pyramids, neighbor-predictive motion vectors, early termination, and hash-based candidate
   ranking before any pixel-accurate refinement.
6. Stop double-running writer-backed payload work. Some block-inter payload paths still summarize or replay writer output later in common/src/
   main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:2203 and common/src/main/java/de/
   keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:4463. Those payloads should be produced once, hashed
   once, and handed around by reference.
7. Simplify chunk-packing search and reuse final packing. The chunk helper tries multiple chunk sizes and repeatedly simulates packing/
   compression/cache behavior in common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/
   AfmaChunkedPayloadHelper.java:35 and common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/
   AfmaChunkedPayloadHelper.java:598. Use data-driven chunk-size selection from payload statistics, and carry the winning pack layout straight
   into final archive writing instead of rebuilding it again in common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/
   afma/creator/AfmaEncodePlan.java:35.
8. Make full decode validation optional. After writing, the export path reopens and revalidates the archive in common/src/main/java/de/
   keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaCreatorState.java:331 and common/src/main/java/de/keksuccino/fancymenu/
   util/resource/resources/texture/afma/AfmaDecoder.java:332. Keep that for strict/debug mode, but not for normal production encoding.

The slowest parts today are therefore not “compression” in isolation. They are “search explosion + repeated scans + repeated materialization +
repeated archive simulation.” If you want the biggest possible improvement, the first three items above are the critical rewrite: incremental
planner state, shared frame-pair analysis, and one-shot payload materialization. That is the difference between a heavy experimental encoder and
a professional offline codec pipeline.

I did not build or run the project, per repo instructions. If you want, I can turn this into a concrete implementation blueprint next, with
proposed new classes, data flow, and the safest rewrite order.