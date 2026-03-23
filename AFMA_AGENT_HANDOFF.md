# AFMA Agent Handoff

## Current Context
This repository is on branch `v3-1.21.1`.

Recent AFMA-related fixes already landed:

- `1dd174816` Fix AFMA payload stream close handling
- `9ffef061d` Fix AFMA NativeImage mixin and decoder teardown race
- `5cfb1bf55` Fix AFMA payload channel order

Do not run or compile the project. This repository explicitly forbids that.

The current active task is:

1. improve AFMA encode output so AFMA actually becomes smaller than equivalent FMA where it should
2. integrate `ffmpeg` where it is genuinely useful during encoding, but not as a replacement for AFMA-specific logic
3. specifically address why `animation_2.afma` is much larger than `cave_animation_fancymenu_v3.fma` even though both are based on the same frame set

## Important Recent Runtime Fixes
Before continuing encoder work, be aware of these runtime pitfalls that were just fixed:

- `IMixinNativeImage` existed for a long time but was never registered in `fancymenu.mixins.json`
- AFMA runtime started depending on that accessor in commit `3b0606b21`, which turned the old latent issue into a hard runtime crash
- `IMixinNativeImage` is now registered in:
  - [fancymenu.mixins.json](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/resources/fancymenu.mixins.json)
- AFMA stream failure cleanup previously closed the decoder/archive while the stream thread was still inside payload decode, producing `ClosedChannelException`
- decoder teardown is now deferred until the stream thread actually exits:
  - [AfmaTexture.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaTexture.java)
- AFMA playback colors were blue-shifted because Java `BufferedImage` ARGB pixels were written directly into `NativeImage` memory without channel conversion
- that is now fixed by converting ARGB to ABGR in:
  - [AfmaNativeImageHelper.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/AfmaNativeImageHelper.java)

## The File-Size Problem That Must Be Fixed
The user compared:

- `run_client/config/fancymenu/assets/animation_2.afma`
- `run_client/config/fancymenu/assets/cave_animation_fancymenu_v3.fma`

Observed sizes:

- `animation_2.afma`: about `315 MB`
- `cave_animation_fancymenu_v3.fma`: about `229 MB`

The AFMA file is about `86-100 MB` larger, depending on exact filesystem reporting.

## Exact Archive Comparison Findings
I already inspected the actual archives locally. The result was very clear: the size gap is almost entirely in the stored PNG payloads, not metadata.

### Compressed payload totals
AFMA:

- `frames/`: `184,520,006` bytes
- `intro_frames/`: `145,360,600` bytes
- total payload bytes: about `329.9 MB`

FMA:

- `frames/`: `134,525,360` bytes
- `intro_frames/`: `104,660,193` bytes
- total payload bytes: about `239.2 MB`

This means the AFMA overhead is basically payload PNG size bloat, not JSON/container overhead.

### AFMA operation distribution for `animation_2.afma`
From `frame_index.json`:

- main frames: `114`
  - `100` `full`
  - `14` `delta_rect`
- intro frames: `96`
  - `95` `full`
  - `1` `delta_rect`
- `0` `same`
- `0` `copy_rect_patch`

This is the first major issue: AFMA is barely optimizing this animation structurally.

### The chosen `delta_rect` frames are nearly full-frame
The AFMA `delta_rect` candidates that were actually chosen are mostly huge:

- examples: `1911x1080`, `1912x1080`, `1904x1080`, `1880x1080`
- area ratio is roughly `0.979` to `0.997` of the full 1920x1080 canvas

Examples from the actual archive:

- `frames/e.png`: `1911x1080`, area ratio `0.9953`, PNG bytes `1,698,066`
- `frames/q.png`: `1912x1080`, area ratio `0.9958`, PNG bytes `1,651,542`
- `intro_frames/2n.png`: `1912x1080`, area ratio `0.9958`, PNG bytes `1,682,437`

This is effectively still full-frame storage.

### AFMA payload PNGs are much larger than the original FMA PNGs
Average compressed payload size:

AFMA:

- main: about `1,618,597` bytes
- intro: about `1,514,173` bytes

FMA:

- main: about `1,180,047` bytes
- intro: about `1,090,210` bytes

So even where AFMA stores a full frame, its rewritten PNG payloads are substantially larger than the original FMA PNGs for the same visual data.

### This is not mainly an RGBA-vs-RGB issue for this file
I checked sample PNG headers from both archives.

Sampled AFMA payloads:

- `intro_frames/0.png`: bit depth `8`, PNG color type `2` (RGB)
- `frames/0.png`: bit depth `8`, PNG color type `2` (RGB)
- `frames/e.png`: bit depth `8`, PNG color type `2` (RGB)

Sampled FMA payloads:

- `intro_frames/11.png`: bit depth `8`, color type `2`
- sampled main FMA frame: bit depth `8`, color type `2`

So the large size increase here is not explained by AFMA unnecessarily writing alpha channels for these samples. The bigger issues are:

1. almost no meaningful structural optimization was achieved
2. the PNG re-encode path produces significantly larger PNGs than the original source PNGs used by the FMA archive

## Files You Need To Start With
Encoder/planner path:

- [AfmaEncodePlanner.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java)
- [AfmaPixelFrame.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaPixelFrame.java)
- [AfmaRectCopyDetector.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaRectCopyDetector.java)
- [AfmaFrameNormalizer.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaFrameNormalizer.java)
- [AfmaArchiveWriter.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaArchiveWriter.java)
- [AfmaCreatorState.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaCreatorState.java)
- [AfmaEncodeOptions.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodeOptions.java)
- [AfmaOptimizationPreset.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaOptimizationPreset.java)

FFmpeg integration points that currently exist but are underused:

- [AfmaCreatorEntryGate.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaCreatorEntryGate.java)
- [AfmaFfmpegBridge.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaFfmpegBridge.java)
- existing FancyMenu FFmpeg downloader classes mentioned in the AFMA plan

Comparison target:

- `run_client/config/fancymenu/assets/animation_2.afma`
- `run_client/config/fancymenu/assets/cave_animation_fancymenu_v3.fma`

## What Needs To Be Implemented Next
There are three main workstreams.

### 1. Stop needlessly rewriting full-frame PNG payloads when source bytes are already good
This is likely the most important immediate fix for this exact animation.

Current behavior:

- AFMA normalizes frames into `AfmaPixelFrame`
- for a `full` candidate it writes a new PNG through `AfmaPixelFrame.asByteArray()`
- this discards the original source PNG encoding and replaces it with a larger PNG

For a source set that already consists of good 8-bit PNGs, this loses badly to FMA.

What you should implement:

- during frame normalization, keep enough source metadata to know whether the input file is already a compliant AFMA payload candidate
- if a source frame is already:
  - PNG
  - 8-bit
  - straight-compatible output
  - no problematic metadata that AFMA must strip for determinism/correctness
  - same visible pixel result as the normalized working buffer
- then allow `full` candidates to reuse the original source PNG bytes directly instead of re-encoding

Important:

- do not let this break the AFMA rule that created payloads must be normalized and deterministic where needed
- but for a file like this, if the exact source PNG bytes are already compliant and visually identical to the normalized pixel buffer, reusing them is probably the biggest size win

The same idea may also apply to some patch payloads when the patch region exactly matches an already compliant source crop path, but full-frame reuse should be tackled first

### 2. Make candidate selection much stricter so AFMA does not emit near-full `delta_rect` frames
Current behavior is clearly allowing pathological deltas.

Needed changes:

- aggressively reject `delta_rect` candidates when changed area is near full-frame
- do not just compare byte size in a way that allows a `99.5%` patch to survive as a “delta”
- add a hard ceiling for delta area ratio unless there is a genuinely strong compressed-byte win
- likely also add an absolute-byte margin requirement so the more complex representation only wins when it is meaningfully smaller

For this animation, a `1912x1080` patch should almost certainly become either:

- `full`, if the archive will still store a full frame anyway
- or `copy_rect_patch`, if the content is mostly translation/panning

but not a nominal delta that is basically the full frame

### 3. Improve `copy_rect_patch` detection so AFMA can actually win on scrolling/panning motion
`animation_2` appears to be the kind of content where AFMA should benefit from copy-based motion handling, but the archive contains zero `copy_rect_patch` frames.

That means rect-copy detection is currently too weak or too conservative.

Inspect:

- [AfmaRectCopyDetector.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaRectCopyDetector.java)

Likely needed:

- widen or improve translation detection heuristics
- better support dominant horizontal/vertical shift detection
- better shortlist/ranking logic for candidate offsets
- allow `copy_rect_patch` to beat bad near-full delta candidates more often

The AFMA plan explicitly intended rect-copy to help with:

- scrolling
- panning
- credits movement

This asset is a strong candidate for that kind of optimization and the current encoder is failing to capture it.

## Where FFmpeg Should Actually Be Used
The user explicitly raised that AFMA still does not use FFmpeg at all.

The correct answer is:

- FFmpeg should not replace AFMA-specific planning logic
- but FFmpeg can and should be used where it genuinely improves the encoding pipeline

Useful FFmpeg roles:

- source ingestion from video or unsupported formats
- frame extraction
- preprocessing/transcoding into a stable working sequence
- possibly normalization into a known temporary PNG sequence when needed
- metadata/probe support

Less convincing role:

- AFMA core decision-making (`full` vs `delta_rect` vs `copy_rect_patch`) must stay in FancyMenu code

Potentially useful role for current size issue:

- compare current Java/ImageIO PNG output against FFmpeg PNG output as an optional payload writer backend
- but do not assume FFmpeg PNG writing alone solves the problem; the major current failure is still bad operation selection and needless full-frame re-encoding

Recommended order:

1. fix full-frame payload reuse
2. fix bad delta candidate acceptance
3. improve rect-copy detection
4. then benchmark whether FFmpeg-backed PNG writing is worth adding as an optional encode backend for payload emission

## Suggested First Steps For The Next Agent
Do these first, in this order:

1. read:
   - [AfmaEncodePlanner.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaEncodePlanner.java)
   - [AfmaPixelFrame.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaPixelFrame.java)
   - [AfmaRectCopyDetector.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaRectCopyDetector.java)
   - [AfmaFrameNormalizer.java](/mnt/e/coding/workspaces/intellij/minecraft%20mods/fancymenu-1.21.1/common/src/main/java/de/keksuccino/fancymenu/util/resource/resources/texture/afma/creator/AfmaFrameNormalizer.java)

2. inspect how source files are currently loaded and whether original PNG bytes can be preserved when the source is already compliant

3. patch the planner so:
   - near-full deltas are rejected much earlier
   - `copy_rect_patch` gets a fairer chance
   - `full` candidates can reuse original source PNG bytes if safe

4. only after that, evaluate whether FFmpeg should be introduced for payload writing or preprocessing

## One More Important Constraint
The user explicitly wants FFmpeg used only where it is actually useful in the encoding process.

Do not turn AFMA into:

- a generic FFmpeg-driven video encoder
- or an AFMA system whose core decisions depend on FFmpeg

FFmpeg should be a helper, not the core AFMA brain.
