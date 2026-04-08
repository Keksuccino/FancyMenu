# AFMA Compression and Codec Improvement TO-DO

## Phase 0: Baseline, Safety Nets, and Decision Framework

Purpose:
- Create the measurement and validation foundation before changing codec behavior.
- Make later compression decisions based on real AFMA data instead of intuition.

### 0.1 Create a fixed AFMA benchmark corpus
- Gather representative source sequences for:
  - Mostly static UI animations.
  - Scroll and pan motion.
  - Fade and alpha-heavy content.
  - Particle-like sparse motion.
  - Full-screen scene changes.
  - Repeating loops with long steady periods.
  - Intro + looping main sequence combinations.
- Store the corpus outside distributable assets if needed, but make its location stable and documented.
- Include both small and large resolutions.
- Include cases that are known to favor:
  - full BIN_INTRA,
  - delta rect,
  - sparse residual,
  - copy rect,
  - block inter.

### 0.2 Add a repeatable AFMA analysis report tool
- Implement an offline report generator that can print, export, or save:
  - Final archive bytes.
  - Total uncompressed payload bytes.
  - Bytes by frame opcode.
  - Bytes by payload type.
  - Chunk count and chunk fill ratios.
  - Payload dedup hit rate.
  - Keyframe interval distribution.
  - Estimated decode cost by frame and by sequence.
  - Intro and main sequence stats separately.
- Make the report work on:
  - source PNG folders before encoding,
  - generated AFMA archives after encoding.

### 0.3 Add correctness fixtures and golden archives
- Create a set of small golden AFMA archives with known decoded output.
- Add decode validation checks for:
  - exact reconstructed pixels for lossless modes,
  - allowed drift windows for near-lossless modes,
  - correct intro-to-main transitions,
  - custom frame time handling,
  - loop behavior,
  - payload dedup correctness,
  - payload chunk index correctness.
- Add backward compatibility fixtures for current AFMA version 5.

### 0.4 Add decode-side performance and allocation instrumentation
- Add optional debug counters for:
  - payload reads,
  - chunk reads,
  - chunk cache hits and misses,
  - per-frame decode time,
  - per-frame upload time,
  - block inter tile counts and mode counts,
  - allocation hot spots during playback.
- Track:
  - streaming thread decode time,
  - client-thread apply time,
  - memory churn caused by temporary arrays.

### 0.5 Define format evolution rules before changing the bitstream
- Decide how AFMA version 6+ should be introduced.
- Define:
  - feature flags in metadata,
  - version compatibility behavior,
  - fallback behavior for unsupported archives,
  - whether multiple payload codecs can coexist in one archive,
  - whether ZIP remains part of the format or becomes a legacy container.
- Write the versioning rules down before implementing new opcodes.

Acceptance criteria for Phase 0:
- You can compare old and new encoder changes against the same corpus.
- You can prove whether a change improves size, hurts speed, or changes memory.
- You have stable fixtures for backward compatibility.

---

## Phase 1: Decoder Infrastructure Refactor Without Changing Compression Semantics

Purpose:
- Make the decoder cheaper and more extensible before adding stronger codecs.
- Remove known allocation and hot-path inefficiencies that would magnify under richer compression.

### 1.1 Make block inter playback allocation-free
- Remove per-tile temporary byte-array creation during block inter decoding.
- Avoid building temporary `int[]` buffers for raw block-inter tiles on every frame.
- Add reusable scratch buffers or direct-write decode paths where safe.
- Keep memory bounded and deterministic.

### 1.2 Add reusable decode scratch storage
- Introduce reusable scratch state for:
  - filtered row decode,
  - raw tile unpacking,
  - sparse mask traversal helpers,
  - temporary color/reconstruction buffers where needed.
- Scope scratch storage to the decoder or texture instance so it does not leak globally.

### 1.3 Add direct-to-canvas or direct-to-native decode paths for BIN_INTRA
- Reduce avoidable conversions:
  - raw bytes -> intermediate arrays -> canvas writes.
- Add a decode path that can reconstruct directly into:
  - a reusable ABGR buffer, or
  - the destination canvas region.
- Preserve the existing path until the new one is verified.

### 1.4 Improve chunk read locality infrastructure
- Replace the single cached payload chunk with a tiny bounded LRU cache.
- Keep the cache size small enough that memory does not meaningfully increase.
- Add chunk reuse metrics so future chunk-layout optimization has feedback.

### 1.5 Prepare pluggable payload codec dispatch
- Introduce internal dispatch for primary payload codecs without changing current archives yet.
- Make the decoder capable of cleanly routing:
  - BIN_INTRA,
  - raw residual payloads,
  - future sparse codecs,
  - future inter codecs,
  - future tile-intra codecs.

Acceptance criteria for Phase 1:
- Version 5 AFMA archives still decode identically.
- Playback allocations drop in the hot path.
- New decode infrastructure is ready for additional codecs without major refactors.

---

## Phase 2: Encoder Cost Model Overhaul

Purpose:
- Fix the biggest structural weakness first: the encoder currently makes mostly local decisions.
- Make later codec additions useful instead of being wasted by a greedy planner.

### 2.1 Replace local greedy planning with horizon-based planning
- Implement a beam search, trellis search, or bounded dynamic-programming planner over short GOP windows.
- Keep the search window configurable.
- Optimize for:
  - final archive bytes,
  - decode cost caps,
  - memory-neutral runtime behavior.
- Track reference drift explicitly so near-lossless decisions are globally controlled.

### 2.2 Replace per-payload byte estimates with richer archive cost estimates
- Current candidate evaluation mainly estimates archive bytes on isolated payloads.
- Add a cost model that includes:
  - chunk grouping effects,
  - payload dedup reuse likelihood,
  - frame index overhead,
  - opcode-specific metadata overhead,
  - future chunk compression interaction,
  - local GOP structure effects.

### 2.3 Add explicit rate-distortion style scoring for near-lossless paths
- Score candidates using:
  - archive bytes,
  - exact/lossy status,
  - measured perceptual drift,
  - decode cost ceiling,
  - reference drift accumulation limits.
- Ensure lossy continuation cannot silently poison long inter chains.

### 2.4 Add reference-state simulation in the planner
- Make the planner simulate the exact reconstructed frame chain chosen so far.
- Do not estimate relative modes against an oversimplified previous state.
- Carry forward:
  - exact reconstructed pixels,
  - drift flags,
  - keyframe distance,
  - future candidate eligibility.

Acceptance criteria for Phase 2:
- The planner can choose a slightly larger frame now if it leads to a smaller and safer next GOP.
- The planner can globally limit quality drift.
- Candidate selection is no longer purely frame-local.

---

## Phase 3: Low-Risk Compression Wins Inside the Existing AFMA Model

Purpose:
- Harvest the strong improvements that fit current AFMA concepts before adding entirely new opcodes.

### 3.1 Improve BIN_INTRA mode selection
- Add more candidate shaping before filtering:
  - channel-plane separated variants,
  - alternative alpha layouts,
  - tile-local palette candidates,
  - shared-palette candidates for repeated UI sequences.
- Evaluate whether RGB and alpha should be filtered independently more often.
- Add better heuristics for palette reuse in low-color animations.

### 3.2 Improve residual byte shaping
- Test and add transformations such as:
  - zigzag residual coding,
  - channel-plane separation,
  - row-wise residual decorrelation,
  - zero-run coding for residual streams,
  - compact alpha-residual handling when alpha changes are sparse.
- Make residual payloads more compressible before chunk compression.

### 3.3 Add additional sparse payload encodings
- Keep the current mask + residual format as one sparse mode.
- Add new sparse forms such as:
  - row-span sparse payloads,
  - tile-mask sparse payloads,
  - coordinate-list sparse payloads,
  - hybrid sparse mode chosen by changed-pixel density and clustering.
- Let the encoder choose the sparse representation per candidate.

### 3.4 Improve duplicate-frame and timing collapsing
- Extend current duplicate elision to detect:
  - repeating micro-cycles,
  - short still ranges,
  - intro/main transition redundancy,
  - recurring payload identity patterns.
- Collapse timing more aggressively when playback semantics remain identical.

### 3.5 Strengthen adaptive keyframe placement
- Base keyframe forcing on:
  - measured future savings,
  - decode complexity accumulation,
  - chunk-locality penalties,
  - reference drift risk,
  - seek/reset friendliness.
- Make keyframe placement depend on actual reconstructed inter history, not only interval thresholds.

### 3.6 Expose the already existing advanced options in the creator UI
- Surface planner options that already exist internally but are hidden behind presets.
- Add advanced controls for:
  - adaptive keyframe placement,
  - adaptive max keyframe interval,
  - continuation savings thresholds,
  - perceptual BIN_INTRA thresholds,
  - copy search parameters.
- Keep sensible presets, but allow power users to override them.

Acceptance criteria for Phase 3:
- AFMA v5-style archives get noticeably smaller on the corpus without major format upheaval.
- Decode speed stays flat or improves.
- Users can access the stronger existing encoder behaviors.

---

## Phase 4: Chunk Layout and Archive Locality Optimization

Purpose:
- Improve both archive size and decode locality without changing steady-state playback memory.

### 4.1 Replace naive sequential payload packing with locality-aware packing
- Pack payloads by predicted playback access order, not only insertion order.
- Keep frequently adjacent frame payloads near each other.
- Try to keep short decode runs inside as few chunks as possible.

### 4.2 Add chunk packing heuristics aware of dedup and GOPs
- Group payloads by:
  - sequence order,
  - keyframe regions,
  - likely repeated loops,
  - shared payload references.
- Avoid scattering the payloads of one inter chain across many chunks.

### 4.3 Tune target chunk size using corpus data
- Evaluate multiple target chunk sizes for:
  - archive ratio,
  - read amplification,
  - cache reuse,
  - web/local/resource-location behavior.
- Pick one or more profile-specific defaults if needed.

### 4.4 Add chunk packing simulation to the candidate cost model
- Make the planner aware that payload layout affects real archive size.
- If needed, perform a late re-packing pass and re-score borderline candidates.

Acceptance criteria for Phase 4:
- Chunk cache hit rates improve.
- Average payload reads per displayed frame do not worsen.
- Archive size improves through better cross-payload compression.

---

## Phase 5: Multi-Region Motion Reuse

Purpose:
- Fill the gap between one global copy rect and full tile-based inter.
- This is likely one of the highest-payoff new AFMA opcodes.

### 5.1 Add a `MULTI_COPY_PATCH` family of frame operations
- Design new opcodes that can express:
  - multiple copy rects plus optional BIN_INTRA patches,
  - multiple copy rects plus dense residual patches,
  - multiple copy rects plus sparse patches.
- Keep decoding simple:
  - apply copy operations in a stable order,
  - then apply patch payloads.

### 5.2 Build a multi-rect copy detector
- Extend copy detection from one dominant motion to multiple reusable regions.
- Detect:
  - independent UI panels moving separately,
  - layered sliding elements,
  - partial scrolling plus static chrome,
  - repeated icon strips and list movement.

### 5.3 Add planner support for multi-copy candidates
- Let the planner compare:
  - single copy rect,
  - multi-copy rect,
  - sparse delta,
  - block inter.
- Penalize over-fragmentation so the encoder does not explode metadata for tiny wins.

### 5.4 Add efficient descriptor encoding for multi-copy
- Compress copy-rect lists using:
  - varints,
  - delta-coded coordinates,
  - small-count special forms,
  - optional shared dimensions where profitable.

Acceptance criteria for Phase 5:
- UI-heavy sequences with several moving regions get smaller than both single-copy and block-inter in common cases.
- Decode remains simpler than a full motion-compensated tile codec.

---

## Phase 6: Sparse Codec Family Expansion

Purpose:
- Make AFMA much better at small, scattered, and clustered changes.
- This especially matters for menus, blinking highlights, cursor motion, and text effects.

### 6.1 Add row-span sparse patches
- Encode runs of changed pixels per row instead of full bitmasks when changes cluster horizontally.

### 6.2 Add tile-sparse patches
- Use a coarse tile mask first.
- Within changed tiles, use dense or local sparse residuals.
- Skip untouched tiles entirely.

### 6.3 Add coordinate-list sparse patches
- For very low changed-pixel counts, store changed coordinates directly.
- Add sorted coordinate coding and delta coding.

### 6.4 Add hybrid sparse selection
- Choose among:
  - raster bitmask sparse,
  - row-span sparse,
  - tile-sparse,
  - coordinate sparse,
  - dense residual.
- Base selection on changed-pixel count, clustering, row coherence, and tile coherence.

### 6.5 Add sparse codec reuse inside block-inter tiles
- Let block-inter use the same sparse family internally, not only one mask+residual form.

Acceptance criteria for Phase 6:
- Cursor-like and particle-like changes compress much better.
- Sparse modes activate in more real cases without hurting decode speed.

---

## Phase 7: Block Inter v2

Purpose:
- Upgrade AFMA's most video-like mode after the planner and sparse family are strong enough to support it.

### 7.1 Redesign block inter payload layout
- Stop storing each tile as a mostly self-contained inline blob.
- Split block-inter payload into compact streams such as:
  - tile mode stream,
  - motion-vector stream,
  - dense residual stream,
  - sparse metadata stream,
  - sparse residual stream,
  - raw tile stream.
- This should compress much better than per-tile inline records.

### 7.2 Add motion-vector prediction
- Predict tile motion from:
  - left tile,
  - top tile,
  - top-left tile,
  - dominant frame motion,
  - zero vector fallback.
- Store only motion residuals where possible.

### 7.3 Add variable block sizes
- Support a block hierarchy such as:
  - 8x8,
  - 16x16,
  - 32x32.
- Let the encoder split only where it buys enough compression.
- Keep decoder memory flat by decoding one block at a time.

### 7.4 Add tile-intra modes
- For blocks that do not predict well from previous frames, allow:
  - tiny BIN_INTRA block payloads,
  - small palette blocks,
  - raw block fallback.
- Prevent block-inter from degenerating into bad raw-tile storage.

### 7.5 Improve motion search quality
- Replace the current axis-sampled motion candidate generation with stronger search:
  - hierarchical motion search,
  - seeded local refinement from frame-global motion clusters,
  - optional exhaustive search in small windows,
  - motion reuse from previous frame decisions.

### 7.6 Add block-inter-specific planner heuristics
- Penalize block inter when:
  - region size is too small,
  - motion coherence is low,
  - metadata overhead dominates,
  - decode complexity would exceed the allowed budget.

Acceptance criteria for Phase 7:
- Motion-heavy content compresses materially better than AFMA v5 block inter.
- Decode allocations do not increase.
- Decode speed remains competitive with or better than current block inter.

---

## Phase 8: New Container and Streaming Format

Purpose:
- Remove ZIP-specific limitations after the codec structure has stabilized.
- This is the largest format change and should come late.

### 8.1 Design a native AFMA container
- Replace or supplement ZIP with a custom container that supports:
  - direct random access,
  - chunk tables,
  - optional uncompressed regions,
  - future per-stream codecs,
  - easier web streaming,
  - lower open-time overhead.

### 8.2 Remove full temp-file spooling for stream sources
- Support stream-friendly decoding for web and generic input sources.
- Allow:
  - page-based buffering,
  - seekable in-memory index windows,
  - chunk-demand fetch where feasible.

### 8.3 Support codec-aware paging
- Keep related data together:
  - GOP data pages,
  - frame index pages,
  - payload tables,
  - optional thumbnail pages,
  - optional seek map pages.

### 8.4 Add a seek map
- Store periodic reset points and lightweight navigation metadata.
- Improve:
  - reset behavior,
  - future seeking support,
  - validation and preview tools.

### 8.5 Keep version 5 ZIP decoding as legacy support
- Do not break existing archives.
- Add a clean migration path:
  - encode new archives in v6+,
  - decode both v5 and v6+,
  - optionally offer an offline converter.

Acceptance criteria for Phase 8:
- AFMA no longer depends on temporary ZIP spooling for streamed sources.
- Open and validation costs improve.
- New container pages support the evolved codec design cleanly.

---

## Phase 9: Advanced Quality and Drift Management

Purpose:
- Make the strongest compression modes safe and controllable over long animations.

### 9.1 Add long-range drift budgeting
- Track drift over time, not only frame by frame.
- Enforce:
  - per-frame error limits,
  - cumulative GOP drift limits,
  - forced refresh when drift budget is exhausted.

### 9.2 Add content-aware quality policy
- Treat content differently based on visible importance:
  - text and UI edges,
  - translucent shadows,
  - flat backgrounds,
  - fully hidden transparent pixels.
- Allow more aggressive compression where it is visually safe.

### 9.3 Add region importance weighting
- Weight perceptual error by:
  - alpha visibility,
  - edge strength,
  - local contrast,
  - possible future user-defined masks.

### 9.4 Add optional shared palette or theme-aware compression for UI-heavy content
- For recurring UI colors, icons, and flat areas:
  - build per-sequence or per-GOP palettes,
  - reuse palette references across frames and regions,
  - reduce repeated color payload cost.

Acceptance criteria for Phase 9:
- Lossy modes remain visually stable over long loops.
- Compression can be pushed harder without hidden quality failure.

---

## Phase 10: Tooling, UX, and Migration

Purpose:
- Make all the new codec power usable, debuggable, and maintainable.

### 10.1 Expand the AFMA creator advanced panel
- Add grouped advanced sections for:
  - keyframe strategy,
  - sparse mode family,
  - copy and motion search,
  - block-inter settings,
  - near-lossless and perceptual controls,
  - chunk packing strategy,
  - container format selection.

### 10.2 Add archive introspection UI or CLI output
- Show:
  - frame opcode counts,
  - payload codec counts,
  - chunk stats,
  - top payload contributors,
  - drift warnings,
  - dedup effectiveness.

### 10.3 Add comparison export mode
- Allow encoding the same source sequence with multiple presets or format versions.
- Export a side-by-side report:
  - final bytes,
  - decode cost estimate,
  - quality drift metrics,
  - opcode distribution.

### 10.4 Add AFMA upgrade and conversion tooling
- Add utilities to:
  - convert v5 archives to newer formats,
  - rebuild archives with new presets,
  - validate compatibility,
  - print warnings for deprecated features.

Acceptance criteria for Phase 10:
- Developers can understand why a given AFMA archive became smaller or larger.
- Advanced users can tune the codec without editing code.

---

## Implementation Order Summary

Implement in this order:

1. Phase 0: baseline corpus, validation, instrumentation, and versioning rules.
2. Phase 1: decoder refactor and allocation removal.
3. Phase 2: planner and cost-model overhaul.
4. Phase 3: low-risk compression wins within the current AFMA model.
5. Phase 4: chunk layout optimization.
6. Phase 5: multi-region motion reuse.
7. Phase 6: sparse codec family expansion.
8. Phase 7: block inter v2.
9. Phase 8: native AFMA container and stream-friendly paging.
10. Phase 9: advanced drift budgeting and content-aware quality policy.
11. Phase 10: creator UX, analysis UX, migration, and comparison tooling.

This order is intended to:
- avoid redesigning the decoder twice,
- avoid adding new opcodes before the planner can choose them well,
- avoid locking the container too early,
- keep the runtime memory model stable throughout the project.
