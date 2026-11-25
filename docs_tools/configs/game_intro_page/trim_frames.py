"""Copies recorded frames from the frame with a mark event on, renumbered, for compose.py.

Usage: python3 trim_frames.py <frames dir> <out dir> [mark name, default intro] [frame count, default all]

The play GIF starts on the Title screen for a moment before the intro opens. This cuts those frames off, so the GIF starts with the intro."""
import json, os, shutil, sys

src, out = sys.argv[1], sys.argv[2]
mark = sys.argv[3] if len(sys.argv) > 3 else "intro"
count = int(sys.argv[4]) if len(sys.argv) > 4 else None
os.makedirs(out, exist_ok=True)
with open(os.path.join(src, "frames.jsonl")) as f:
    lines = [json.loads(line) for line in f if line.strip()]
start = next(i for i, line in enumerate(lines) if any(e.get("type") == "mark" and e.get("name") == mark for e in line.get("events", [])))
with open(os.path.join(out, "frames.jsonl"), "w") as f:
    kept = lines[start:start + count] if count else lines[start:]
    for n, line in enumerate(kept):
        shutil.copyfile(os.path.join(src, "f_%05d.png" % line["frame"]), os.path.join(out, "f_%05d.png" % n))
        line["frame"] = n
        f.write(json.dumps(line) + "\n")
print(f"{len(kept)} frames from frame {start}")
