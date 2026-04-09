# AFMA Planner Optimization Analysis

This document summarizes the current AFMA encoder/planner performance bottlenecks after phases 1 to 6, with a strong focus on why frame planning has become painfully slow and what should be optimized first.

This is based on static code analysis only. No profiling, build, or runtime benchmarking was performed.

## Executive Summary

The planner is currently paying for some of the most expensive phase 1 to 6 improvements far too often inside the beam-search loop.

The single biggest issue is that archive scoring is effectively global and expensive, but it is being recomputed repeatedly during local candidate filtering and state expansion. That turns what should be a relatively cheap search over candidate transitions into a repeated "simulate a large part of the final archive layout again" workload.

The second major issue is repeated candidate synthesis. The planner rebuilds many of the same frame-local and frame-pair-local candidates for each beam state, even when those candidates do not actually depend on the full beam history.

The third major issue is repeated pixel scanning. Duplicate detection, dirty-bound detection, copy analysis, multi-copy analysis, block-inter analysis, and drift checks each rescan the same frames or regions.

If we optimize only one thing first, it should be the archive-scoring path.

## Main Hotspots

### 1. Archive scoring is far too expensive inside the beam loop

The planner expands every beam state for every frame in:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:321`

For each state, it collects candidates in:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:375`

Then it evaluates transitions in:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:506`

The expensive part is that transition evaluation calls:

- `ArchivePlanningState.appendCandidate(...)`

at:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:515`

That method can trigger a full recomputation path ending in:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:3265`

which calls:

- `AfmaChunkedPayloadHelper.simulateArchiveLayout(...)`

The archive simulation itself rebuilds packing stats, chunk grouping, compression estimates, and chunk-cache simulation in:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaChunkedPayloadHelper.java:334`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaChunkedPayloadHelper.java:397`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaChunkedPayloadHelper.java:565`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaChunkedPayloadHelper.java:610`

This is almost certainly the dominant regression.

It is even worse because candidate filtering also calls `archiveState.appendCandidate(...)` just to decide whether a candidate should stay alive:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:2267`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:2303`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:2336`

So the planner is often doing expensive archive simulation before a candidate even reaches the transition stage.

### 2. Candidate synthesis is repeated for each beam state

`collectWindowCandidates(...)` rebuilds candidate objects for the current frame and frame pair:

- full
- exact full
- delta
- residual delta
- sparse delta
- copy patch
- copy residual
- copy sparse
- multi-copy patch
- multi-copy residual
- multi-copy sparse
- block inter

The logic is all in:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:375`

Many of these candidates depend only on:

- the current source frame
- the working frame
- the previous reference frame
- encoder options

They do not depend on the full beam history. But they are still rebuilt for each beam state.

That means identical BIN_INTRA, residual, sparse, copy, and block-inter payloads may be re-encoded multiple times in the same planning window.

### 3. Payload metric caches are too identity-based to save planner work

`AfmaPayloadMetricsHelper` caches archive byte estimates and fingerprints using `WeakHashMap<byte[], ...>`:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaPayloadMetricsHelper.java:16`

That means the cache only helps when the exact same `byte[]` object instance is reused.

If the planner regenerates the same payload bytes into a fresh array, it pays again for:

- deflate-based size estimation
- SHA-256 fingerprinting

This is especially harmful because the planner currently recreates many candidate payloads instead of reusing them.

### 4. The same frame pair is rescanned many times

The planner repeatedly performs whole-frame or large-region scans for the same pair of frames:

- duplicate detection in `AfmaPixelFrameHelper.isIdentical(...)`
- dirty-bounds detection in `AfmaPixelFrameHelper.findDifferenceBounds(...)`
- dirty-after-copy detection in `AfmaPixelFrameHelper.findDirtyBoundsAfterCopy(...)`
- perceptual drift measurement in `measurePerceptualDrift(...)`
- copy and multi-copy analysis in `AfmaRectCopyDetector`

Relevant locations:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaPixelFrameHelper.java:22`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaPixelFrameHelper.java:27`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaPixelFrameHelper.java:66`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:580`

This is a strong case for a shared frame-pair analysis structure.

### 5. Motion search is being repeated and overscored

Copy and block-inter both depend on motion search from `AfmaRectCopyDetector`.

Axis candidate collection scores every offset over the full search distance here:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaRectCopyDetector.java:166`

Then motion vectors are sorted using a comparator that recomputes scores repeatedly:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaRectCopyDetector.java:157`

That means the same motion score may be recomputed many times during sort.

This becomes especially expensive with the larger preset search ranges:

- `SMALLEST_FILE` uses `maxCopySearchDistance = 2048`
- `SMALLEST_FILE` uses `maxCandidateAxisOffsets = 12`

from:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaOptimizationPreset.java:12`

### 6. Multi-copy is likely a large hotspot on its own

`detectMulti(...)` does a lot of work:

- computes initial dirty bounds
- copies the full previous frame
- repeatedly searches for the best next rect
- reapplies copy rects into the prediction buffer
- rescans final difference bounds

Relevant code:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaRectCopyDetector.java:84`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaRectCopyDetector.java:277`

This likely pays off on some inputs, but it should not run eagerly for every beam state unless cheaper signals suggest it has a real chance to win.

### 7. Block-inter work scales very badly

Block-inter candidate generation does the following:

- aligns the region to the tile grid
- gathers motion vectors for the whole region
- evaluates many vectors per tile
- may build dense residual payloads
- may build sparse residual payloads
- may refine motion locally over multiple passes

Relevant code:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:1695`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:1758`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:1797`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:1845`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:2000`

This is a very expensive candidate type. It should only be constructed after much cheaper upper-bound tests indicate it can beat the simpler candidates.

### 8. BIN_INTRA mode search is costly and gets multiplied by planner repetition

BIN_INTRA encoding is powerful but expensive:

- materializes dense pixels
- may build several perceptual pixel candidates
- builds many mode candidates
- may spawn parallel async work

Relevant code:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaBinIntraPayloadHelper.java:48`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaBinIntraPayloadHelper.java:742`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaBinIntraPayloadHelper.java:760`
- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaBinIntraPayloadHelper.java:996`

This is fine when done once. It is not fine when the planner keeps rebuilding similar full and patch payloads across beam states.

### 9. Residual and sparse codec/layout selection are still brute-force

Residual encoding tries all codecs:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaResidualPayloadHelper.java:214`

Sparse layout selection tries all four layouts:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:1514`

These are good compression decisions, but they should be entered later in the search, with more pruning up front.

### 10. Current planner presets are aggressive enough to amplify all of the above

Base defaults:

- `plannerSearchWindowFrames = 12`
- `plannerBeamWidth = 8`

from:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodeOptions.java:40`

Creator presets make this even larger:

- `SMALLEST_FILE`: window 24, beam 14
- `BALANCED`: window 16, beam 10
- `FASTEST_DECODE`: window 8, beam 4

from:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaCreatorState.java:413`

When combined with the expensive per-state candidate evaluation and archive simulation, the total work rises very fast.

## Most Important Improvements

## 1. Remove full archive simulation from the inner planner loop

This is the highest-value optimization by far.

### Problem

The planner currently asks "what would the whole archive layout look like now?" too often.

### Better approach

Split scoring into two levels:

1. Cheap incremental planning score used inside beam expansion.
2. Expensive archive-layout simulation used only for:
   - end-of-window winner selection
   - periodic rescoring of top-K survivors
   - final plan consolidation

### Recommended design

Introduce a lightweight planner archive model that tracks only:

- unique payload count
- sum of individually estimated payload archive bytes
- descriptor bytes
- approximate deflate-tail continuity
- simple access-locality heuristics

Then keep the full `AfmaChunkedPayloadHelper.simulateArchiveLayout(...)` path only as a refinement stage.

### Expected impact

This should produce the single largest planning speedup.

## 2. Memoize archive append results

Even if full simulation remains for a transitional period, `appendCandidate(...)` results should be cached.

### Suggested cache key

- archive state identity or version id
- primary payload fingerprint id
- patch payload fingerprint id
- frame descriptor type and descriptor metadata
- intro/main sequence flag

### Why this helps

The same candidate is often compared against the same archive state multiple times:

- once during keep/drop filtering
- again during transition expansion
- possibly again during sorting and comparison

Memoizing `CandidateArchiveCost` would remove a lot of duplicate work.

## 3. Add a planning-window candidate cache

Candidates should be cached at the granularity where they are actually reusable.

### Cache categories

- full candidate by working-frame fingerprint
- exact full candidate by source-frame fingerprint
- delta candidate by `(previous fingerprint, working fingerprint, delta bounds, options profile)`
- residual candidate by `(previous fingerprint, working fingerprint, bounds)`
- sparse candidate by `(previous fingerprint, working fingerprint, bounds)`
- copy detection by `(previous fingerprint, working fingerprint, copy search options)`
- multi-copy detection by `(previous fingerprint, working fingerprint, copy search options)`
- block-inter candidate by `(previous fingerprint, working fingerprint, aligned region bounds, motion search options)`

### Important note

This should cache the expensive candidate construction, not just the final keep/drop answer.

## 4. Build a shared `FramePairAnalysis`

The planner should analyze a frame pair once and reuse the results.

### Suggested contents

- identical flag
- dirty bounds
- changed pixel count
- maybe changed alpha flag
- maybe a coarse difference density map
- maybe row/column dirty histograms

### Reuse points

- duplicate-frame elision
- delta bounds
- candidate gating
- sparse decision shortcuts
- copy / multi-copy gating
- drift shortcut decisions

This will cut repeated full-frame scans significantly.

## 5. Separate cheap gating from expensive candidate construction

Currently the planner often builds a candidate payload before it really knows that candidate still has a chance.

That should be inverted.

### New flow

1. Compute cheap structural analysis.
2. Estimate best-case / optimistic byte bounds.
3. Drop obviously losing candidate families early.
4. Only encode payloads for the surviving families.

### Best candidates for this treatment

- multi-copy
- block-inter
- sparse residuals
- perceptual full / patch BIN_INTRA variants

## 6. Rework motion search to be coarse-to-fine and shared

### Current issues

- axis scoring scans many offsets
- vector scoring is recomputed during sort
- copy, multi-copy, and block-inter all redo related motion work

### Recommended changes

- precompute motion scores once per vector before sorting
- cache axis score tables per frame pair
- use a coarse pass first, then refine top candidates only
- cap block-inter to the top N global vectors plus local predictors
- share one motion-analysis result across single-copy, multi-copy, and block-inter

### Extra win

Stop passing huge motion-vector lists into every tile when only a small top subset is likely to matter.

## 7. Gate multi-copy much harder

`detectMulti(...)` should not run eagerly for every eligible frame pair.

### Good gating signals

- single-copy usefulness is already very high
- dirty area is too small
- dirty distribution is too compact
- motion candidate diversity is low
- cheap row-run analysis suggests a single dominant motion

### Recommendation

Only run multi-copy when cheap heuristics say:

- there are likely at least two meaningful motion regions
- single-copy left a large fragmented residual

## 8. Gate block-inter much harder

Block-inter should be treated as a high-cost fallback, not a default candidate family.

### Recommended entry criteria

- aligned dirty region exceeds a tile-count threshold
- simple delta / copy / sparse candidates are not already clearly winning
- motion analysis suggests real blockwise movement
- region fragmentation is high enough that single rect modes are unlikely to compress well

### Additional optimization

Use optimistic lower bounds earlier.

The current `scanMotionTile(...)` already has some early abort logic, but that still happens after block-inter candidate construction has already been selected. More should happen before reaching this stage at all.

## 9. Add fast-path heuristics for residual codec and sparse layout selection

Residual encoding and sparse layout selection should not always do exhaustive search.

### Residual ideas

- if changed sample count is tiny, skip some codecs
- if alpha is unchanged, stay entirely on RGB fast path
- if residual entropy looks very low/high, skip weak codecs
- add a quick histogram-based or zero-run-based heuristic to rank codecs before full transforms

### Sparse ideas

- use row density to decide whether `ROW_SPANS` is worth trying
- use tile occupancy to decide whether `TILE_MASK` is worth trying
- use changed-pixel count threshold to decide whether `COORD_LIST` is worth trying

This reduces exhaustive work without meaningfully hurting final quality if the heuristics are reasonable.

## 10. Reduce planner-triggered BIN_INTRA duplication

The planner currently reuses none of the expensive mode-search work unless the same payload object is literally reused.

### Recommended changes

- cache encoded patch/full BIN_INTRA results by region fingerprint
- keep lossless and perceptual variants separately
- reuse cached `EncodedPayloadResult` objects across beam states

### Important detail

The biggest gain is not in making BIN_INTRA itself faster, but in not rerunning it for the same region again and again.

## 11. Avoid nested unbounded parallelism

`AfmaBinIntraPayloadHelper` uses `CompletableFuture.supplyAsync(...)` with the common pool:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaBinIntraPayloadHelper.java:996`

At the same time, block-inter may parallelize tile evaluation via parallel streams:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java:1712`

This risks oversubscription and poor scheduling behavior.

### Recommendation

Use a dedicated bounded planning executor strategy instead of mixing:

- common-pool `CompletableFuture`
- parallel streams

This should improve throughput consistency even if it does not reduce total work by itself.

## 12. Rework `ArchivePackingHints.append(...)`

Appending packing hints currently copies the whole access-frame list every time:

- `common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaChunkedPayloadHelper.java:747`

That is another hidden cost in the archive-state path.

### Better approach

Use either:

- a persistent linked structure
- a parent pointer plus appended frame
- a small immutable node chain flattened only when needed

The same idea also applies to planner steps and other append-heavy structures.

## Recommended Implementation Order

This is the order that should give the best speedup per unit of implementation effort.

1. Replace full archive simulation in the beam inner loop with a cheap incremental score.
2. Add append-result memoization for `(archive state, candidate)`.
3. Add a planning-window candidate cache.
4. Introduce a shared `FramePairAnalysis`.
5. Share and cache motion analysis.
6. Gate multi-copy and block-inter much more aggressively.
7. Add heuristic fast paths for residual codecs and sparse layouts.
8. Rework BIN_INTRA reuse and planner-side encoded payload caching.
9. Replace nested common-pool / parallel-stream behavior with a bounded planner executor model.
10. Clean up persistent append-heavy structures like packing hints and planner step chains.

## Concrete Refactor Direction

If the goal is to keep output quality nearly unchanged while massively improving planning speed, the cleanest architectural change is:

### Stage A: Fast planning

Use:

- frame-local analysis
- frame-pair analysis
- cheap incremental archive estimates
- cached candidate families

to search broadly and cheaply.

### Stage B: Selective refinement

Only for the top candidates or top beam survivors:

- build expensive payloads that were deferred
- run expensive archive-layout simulation
- re-rank the finalists

### Stage C: Final archive packing

Run the full archive packing logic once on the final selected plan.

This keeps the smarter codec/container decisions, but stops paying their full cost on nearly every local planning decision.

## Most Likely Top 3 Real Bottlenecks

If I had to rank the current bottlenecks without profiling, I would rank them like this:

1. Repeated `simulateArchiveLayout(...)` during candidate keep/drop and transition scoring.
2. Repeated candidate payload construction across beam states, especially BIN_INTRA and copy-family variants.
3. Repeated motion analysis and block-inter work, especially under large search-distance presets.

## Bottom Line

The planner became slow mostly because the new compression intelligence is being executed too eagerly and too repeatedly inside the search itself.

The core fix is not "make one codec helper a bit faster". The core fix is:

- do much less expensive work inside the beam loop
- cache far more aggressively
- split cheap candidate ranking from expensive final-quality scoring

That should preserve the compression gains from phases 1 to 6 while making planning much faster again.
