#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import re
import shutil
import subprocess
import sys
from pathlib import Path


OUTPUT_DIR = Path("run_client/config/fancymenu/assets")


def resolve_yt_dlp_command() -> list[str] | None:
    yt_dlp_binary = shutil.which("yt-dlp")
    if yt_dlp_binary:
        return [yt_dlp_binary]

    if importlib.util.find_spec("yt_dlp") is not None:
        return [sys.executable, "-m", "yt_dlp"]

    return None


def sanitize_filename(filename: str) -> str:
    sanitized = filename.strip()
    if sanitized.lower().endswith(".mp4"):
        sanitized = sanitized[:-4]

    sanitized = re.sub(r'[<>:"/\\|?*\x00-\x1F]', "_", sanitized)
    sanitized = re.sub(r"\s+", " ", sanitized).strip(" .")
    return sanitized


def print_ffmpeg_install_help() -> None:
    print("Install ffmpeg and make sure it is available in PATH.")
    print("Examples:")
    print("  Windows (winget): winget install Gyan.FFmpeg")
    print("  Linux (apt):      sudo apt install ffmpeg")
    print("  macOS (brew):     brew install ffmpeg")


def main() -> int:
    print("YouTube MP4 downloader for FancyMenu")
    print(f"Output folder: {OUTPUT_DIR}")
    print()

    url = input("Paste YouTube video URL: ").strip()
    if not url:
        print("No URL entered. Exiting.")
        return 1

    requested_name = input("Enter output file name (without .mp4): ").strip()
    safe_name = sanitize_filename(requested_name)
    if not safe_name:
        print("Invalid file name. Exiting.")
        return 1

    yt_dlp_command = resolve_yt_dlp_command()
    if yt_dlp_command is None:
        print("Could not find yt-dlp.")
        print("Install it with one of these commands:")
        print("  pip install yt-dlp")
        print("  python -m pip install yt-dlp")
        return 1

    ffmpeg_available = shutil.which("ffmpeg") is not None

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    output_template = OUTPUT_DIR / f"{safe_name}.%(ext)s"

    # Ensure audio is present and output stays MP4.
    # With ffmpeg, yt-dlp can merge the best video+audio streams.
    # Without ffmpeg, only progressive MP4 files can be downloaded with audio.
    if ffmpeg_available:
        format_selector = "bestvideo*+bestaudio/best[ext=mp4][acodec!=none]/best"
    else:
        format_selector = "best[ext=mp4][vcodec!=none][acodec!=none]"
        print("ffmpeg was not found. Using progressive MP4 only.")
        print("If this fails for a video, install ffmpeg and run again.")
        print()

    command = [
        *yt_dlp_command,
        "--ignore-config",
        "--no-playlist",
        "-f",
        format_selector,
        "-o",
        str(output_template),
        url,
    ]
    if ffmpeg_available:
        command.extend(["--merge-output-format", "mp4"])

    result = subprocess.run(command, text=True, capture_output=True)
    if result.returncode != 0:
        output = f"{result.stdout}\n{result.stderr}".lower()
        if "ffmpeg is not installed" in output or "ffprobe and ffmpeg not found" in output:
            print("Download failed because ffmpeg is missing.")
            print_ffmpeg_install_help()
            return result.returncode or 1

        if (
            not ffmpeg_available
            and "requested format is not available" in output
        ):
            print("This video has no direct MP4 stream with audio without ffmpeg.")
            print_ffmpeg_install_help()
            return result.returncode or 1

        print("Download failed. yt-dlp output:")
        error_text = (result.stderr or result.stdout).strip()
        if error_text:
            print(error_text)
        else:
            print(f"Exit code {result.returncode}")
        return result.returncode or 1

    print()
    print(f"Done. Video saved to: {OUTPUT_DIR / (safe_name + '.mp4')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
