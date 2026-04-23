#!/usr/bin/env python3
from __future__ import annotations

import json
import http.client
import os
import re
import shutil
import socket
import ssl
import subprocess
import sys
import tempfile
import threading
import time
import urllib.error
import urllib.request
from collections import OrderedDict
from dataclasses import dataclass, field
from pathlib import Path


LANG_DIRECTORY = Path("common/src/main/resources/assets/fancymenu/lang")
SOURCE_LANGUAGE_CODE = "en_us"
TARGET_LANGUAGE_CODES = [
    "de_de",
    "el_gr",
    "es_mx",
    "fr_fr",
    "ja_jp",
    "ru_ru",
    "zh_cn",
    "ko_kr",
]
OPENROUTER_API_KEY = "<openrouter_api_key_here>"
OPENROUTER_MODEL = "google/gemini-3.1-pro-preview"
BATCH_SIZE = 150

OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
REQUEST_TIMEOUT_SECONDS = 2400
REQUEST_RETRY_COUNT = 30
BATCH_SPLIT_AFTER_FAILED_ATTEMPTS = 5
REQUEST_RETRY_DELAY_SECONDS = 2
REQUEST_RETRY_DELAY_MAX_SECONDS = 60
NETWORK_RETRY_WAIT_SECONDS = 5
TEMP_DIFF_PREFIX = "fancymenu_translation_diff_"
PROGRESS_BAR_WIDTH = 28
UI_REFRESH_INTERVAL_SECONDS = 1.0
STREAM_RENDER_THROTTLE_SECONDS = 0.15
DASHBOARD_MAX_WIDTH = 250

ANSI_ESCAPE_RE = re.compile(r"\x1b\[[0-9;]*m")
ANSI_RESET = "\x1b[0m"
ANSI_BOLD = "\x1b[1m"
ANSI_RED = "\x1b[31m"
ANSI_GREEN = "\x1b[32m"
ANSI_YELLOW = "\x1b[33m"
ANSI_BLUE = "\x1b[34m"
ANSI_MAGENTA = "\x1b[35m"
ANSI_CYAN = "\x1b[36m"
ANSI_WHITE = "\x1b[37m"
ANSI_BRIGHT_BLACK = "\x1b[90m"

SYSTEM_PROMPT = """You are a professional Minecraft mod localization translator. You translate Minecraft-style localization JSONs from English to the target language. You only translate the value, never the translation keys. You never remove or add lines. You translate every line of the JSON to the target language and return back the translated version of the received JSON. Make sure you return ONLY THE TRANSLATED JSON as valid JSON, no other text. You translate mod localizations to natural sounding localizations in the target language. You use proper gaming and Minecraft slang when translating, which means that when the target language commonly uses terms for specific words that are not the perfect direct translation, but would work best, then you will use this term, to make the translation sound more natural and high-quality. Use a chill (non-formal) tone for translations, like using 'Du' in German, for example. The translations should fit the typical wording and style for Vanilla Minecraft localizations, meaning a friendly and chill tone, but still professional."""
USER_PROMPT_TEMPLATE = """Please translate the following localization to {target_language_code}. Return ONLY THE TRANSLATED JSON, no other text! Here is the JSON:

{json_localization_line_batch}"""

MODE_UPDATE_EXISTING = "1"
MODE_REGENERATE = "2"
LANGUAGE_SCOPE_ALL = "1"
LANGUAGE_SCOPE_SELECTED = "2"
OPENROUTER_TOKENS_ENTRY_NAME = "openrouter_mod_localization_key"

SCRIPT_DIR = Path(__file__).resolve().parent
TOKENS_FILE_PATH = (SCRIPT_DIR / "../../.TOKENS").resolve()
RESOLVED_LANG_DIRECTORY = (SCRIPT_DIR / LANG_DIRECTORY).resolve()
SOURCE_FILE_PATH = RESOLVED_LANG_DIRECTORY / f"{SOURCE_LANGUAGE_CODE}.json"
SOURCE_FILE_REPO_PATH = SOURCE_FILE_PATH.relative_to(SCRIPT_DIR).as_posix()


class ScriptError(RuntimeError):
    pass


class RetryableRequestError(ScriptError):
    pass


class NetworkUnavailableError(RetryableRequestError):
    pass


class BatchSplitRequest(ScriptError):
    def __init__(
        self,
        split_batches: list[OrderedDict[str, str]],
        message: str,
    ) -> None:
        super().__init__(message)
        self.split_batches = split_batches


@dataclass
class DiffResult:
    added_entries: OrderedDict[str, str]
    edited_entries: OrderedDict[str, str]
    removed_keys: list[str]
    changed_entries: OrderedDict[str, str]


@dataclass
class FilePlan:
    file_number: int
    language_code: str
    path: Path
    action_label: str
    result_label: str
    translation_source_entries: OrderedDict[str, str] = field(default_factory=OrderedDict)
    existing_entries: OrderedDict[str, str] | None = None
    removed_keys: list[str] = field(default_factory=list)
    entries_to_translate: int = 0
    missing_keys_count: int = 0
    overwrite_keys_count: int = 0
    obsolete_keys_count: int = 0
    added_keys_count: int = 0
    edited_keys_count: int = 0
    skipped: bool = False
    skip_reason: str = ""
    target_existed_before_run: bool = False
    force_full_rewrite: bool = False


class TerminalRenderer:
    def __init__(self) -> None:
        self.dynamic = sys.stdout.isatty()
        self.supports_color = self.dynamic and os.environ.get("TERM") != "dumb" and "NO_COLOR" not in os.environ
        self.last_line_count = 0

    def render(self, lines: list[str]) -> None:
        if not self.dynamic:
            for line in lines:
                print(line)
            print()
            return

        padded_lines = list(lines)
        if self.last_line_count > len(padded_lines):
            padded_lines.extend([""] * (self.last_line_count - len(padded_lines)))

        if self.last_line_count:
            sys.stdout.write(f"\x1b[{self.last_line_count}F")

        for line in padded_lines:
            sys.stdout.write("\x1b[2K")
            sys.stdout.write(line)
            sys.stdout.write("\n")

        sys.stdout.flush()
        self.last_line_count = len(lines)


class ProgressTracker:
    def __init__(self, mode_label: str, plans: list[FilePlan], overview_lines: list[str]) -> None:
        self.mode_label = mode_label
        self.plans = plans
        self.overview_lines = overview_lines
        self.renderer = TerminalRenderer()
        self.lock = threading.RLock()
        self.refresh_stop_event = threading.Event()
        self.refresh_thread: threading.Thread | None = None
        self.last_render_time = 0.0
        self.start_time = time.time()
        self.total_entries = sum(plan.entries_to_translate for plan in plans if not plan.skipped)
        self.total_files = len(plans)
        self.active_files = sum(1 for plan in plans if not plan.skipped)
        self.completed_entries = 0
        self.completed_files = 0
        self.current_plan: FilePlan | None = None
        self.current_action = "Preparing translation run"
        self.current_note = "Waiting to start"
        self.current_file_entries_done = 0
        self.current_batch_index = 0
        self.current_batch_total = 0
        self.current_batch_size = 0
        self.current_attempt = 0
        self.current_batch_started_at = 0.0
        self.current_batch_stream_line_total = 0
        self.current_batch_stream_line_count = 0
        self.current_batch_stream_chunk_count = 0
        self.current_batch_stream_character_count = 0
        self.current_batch_stream_comment = ""
        self.last_stream_line = "Waiting for model output"
        self.last_event = "No file processed yet"
        if self.renderer.dynamic:
            self.refresh_thread = threading.Thread(
                target=self._refresh_loop,
                name="fancymenu-translation-progress",
                daemon=True,
            )
            self.refresh_thread.start()
        self.render(force=True)

    def shutdown(self) -> None:
        self.refresh_stop_event.set()
        thread = self.refresh_thread
        if thread is not None and thread.is_alive():
            thread.join(timeout=UI_REFRESH_INTERVAL_SECONDS + 0.5)
        self.refresh_thread = None

    def _refresh_loop(self) -> None:
        while not self.refresh_stop_event.wait(UI_REFRESH_INTERVAL_SECONDS):
            self.render(force=True)

    def set_action(self, action: str, note: str | None = None) -> None:
        with self.lock:
            self.current_action = action
            if note is not None:
                self.current_note = note
        self.render(force=True)

    def start_file(self, plan: FilePlan) -> None:
        with self.lock:
            self.current_plan = plan
            self.current_action = plan.action_label
            self.current_note = f"Preparing {plan.language_code}.json"
            self.current_file_entries_done = 0
            self.current_batch_index = 0
            self.current_batch_total = 0
            self.current_batch_size = 0
            self.current_attempt = 0
            self.current_batch_started_at = 0.0
            self.current_batch_stream_line_total = 0
            self.current_batch_stream_line_count = 0
            self.current_batch_stream_chunk_count = 0
            self.current_batch_stream_character_count = 0
            self.current_batch_stream_comment = ""
            self.last_stream_line = "Waiting for model output"
        self.render(force=True)

    def skip_file(self, plan: FilePlan) -> None:
        with self.lock:
            self.current_plan = plan
            self.current_action = plan.action_label
            self.current_note = plan.skip_reason
            self.current_file_entries_done = 0
            self.current_batch_index = 0
            self.current_batch_total = 0
            self.current_batch_size = 0
            self.current_attempt = 0
            self.current_batch_started_at = 0.0
            self.current_batch_stream_line_total = 0
            self.current_batch_stream_line_count = 0
            self.current_batch_stream_chunk_count = 0
            self.current_batch_stream_character_count = 0
            self.current_batch_stream_comment = ""
            self.last_stream_line = "Waiting for model output"
            self.last_event = f"{plan.language_code}.json skipped: {plan.skip_reason}"
            self.completed_files += 1
        self.render(force=True)

    def start_batch(
        self,
        batch_index: int,
        total_batches: int,
        batch_size: int,
        attempt: int,
        expected_output_lines: int,
    ) -> None:
        with self.lock:
            if self.current_plan is None:
                return

            self.current_batch_index = batch_index
            self.current_batch_total = total_batches
            self.current_batch_size = batch_size
            self.current_attempt = attempt
            self.current_batch_started_at = time.time()
            self.current_batch_stream_line_total = expected_output_lines
            self.current_batch_stream_line_count = 0
            self.current_batch_stream_chunk_count = 0
            self.current_batch_stream_character_count = 0
            self.current_batch_stream_comment = ""
            self.last_stream_line = "Waiting for model output"
            self.current_action = "Waiting for OpenRouter response"
            self.current_note = (
                f"{self.current_plan.language_code}.json batch {batch_index}/{total_batches} "
                f"({batch_size:,} keys)"
            )
        self.render(force=True)

    def set_batch_stage(self, action: str, note: str) -> None:
        with self.lock:
            self.current_action = action
            self.current_note = note
        self.render(force=True)

    def note_stream_comment(self, comment: str) -> None:
        with self.lock:
            self.current_batch_stream_comment = comment or "OPENROUTER PROCESSING"
        if self.renderer.dynamic:
            self.render(force=False)

    def update_stream_output(self, streamed_text: str) -> None:
        with self.lock:
            self.current_batch_stream_chunk_count += 1
            self.current_batch_stream_character_count = len(streamed_text)
            self.current_batch_stream_line_count = count_text_lines(streamed_text)
            self.last_stream_line = extract_last_stream_line(streamed_text)
            self.current_action = "Streaming model response"
        if self.renderer.dynamic:
            self.render(force=False)

    def finish_batch(self, processed_entries: int) -> None:
        with self.lock:
            self.current_file_entries_done += processed_entries
            self.completed_entries += processed_entries
            if self.current_batch_stream_line_total > self.current_batch_stream_line_count:
                self.current_batch_stream_line_count = self.current_batch_stream_line_total
            if self.current_plan is not None:
                self.last_event = (
                    f"{self.current_plan.language_code}.json finished batch "
                    f"{self.current_batch_index}/{self.current_batch_total}"
                )
            self.current_action = "Batch translated"
            self.current_note = f"Processed {processed_entries:,} keys in the latest batch"
        self.render(force=True)

    def finish_file(self, plan: FilePlan, result_note: str) -> None:
        with self.lock:
            self.current_plan = plan
            self.current_action = "File finished"
            self.current_note = result_note
            self.current_file_entries_done = plan.entries_to_translate
            self.current_batch_index = self.current_batch_total
            self.current_attempt = 0
            self.last_event = f"{plan.language_code}.json completed"
            self.completed_files += 1
        self.render(force=True)
        with self.lock:
            self.current_plan = None

    def finish_run(self, final_note: str) -> None:
        with self.lock:
            self.current_action = "Run complete"
            self.current_note = final_note
            self.current_plan = None
            self.current_file_entries_done = 0
            self.current_batch_index = 0
            self.current_batch_total = 0
            self.current_batch_size = 0
            self.current_attempt = 0
            self.current_batch_started_at = 0.0
            self.current_batch_stream_line_total = 0
            self.current_batch_stream_line_count = 0
            self.current_batch_stream_chunk_count = 0
            self.current_batch_stream_character_count = 0
            self.current_batch_stream_comment = ""
            self.last_stream_line = "Waiting for model output"
        self.render(force=True)

    def render(self, force: bool = False) -> None:
        with self.lock:
            if self.renderer.dynamic and not force:
                now = time.monotonic()
                if now - self.last_render_time < STREAM_RENDER_THROTTLE_SECONDS:
                    return
                self.last_render_time = now
            elif self.renderer.dynamic:
                self.last_render_time = time.monotonic()

            lines = self.build_lines()
            self.renderer.render(lines)

    def build_lines(self) -> list[str]:
        elapsed = format_duration(time.time() - self.start_time)
        width = get_dashboard_width()
        lines = [
            make_box_top(
                "FancyMenu Localization Translator",
                width,
                self.renderer.supports_color,
            ),
            make_box_content(
                format_status_summary(
                    self.mode_label,
                    self.current_action,
                    elapsed,
                    self.renderer.supports_color,
                ),
                width,
            ),
        ]

        lines.append(make_box_separator("Overview", width, self.renderer.supports_color))
        for overview_line in self.overview_lines:
            lines.append(make_box_content(overview_line, width))

        if self.current_plan is None:
            current_file_label = "Current: none"
            target_path_line = "Target : none"
            remaining_languages = "Remaining: none"
            file_stats_line = "Stats  : waiting"
        else:
            current_file_label = (
                f"Current: {self.current_plan.language_code}.json "
                f"({self.current_plan.file_number}/{self.total_files})"
            )
            target_path_line = f"Target : {self.current_plan.path.name}"
            remaining_languages = format_remaining_languages(
                self.plans[self.current_plan.file_number :]
            )
            file_stats_line = describe_file_stats(self.mode_label, self.current_plan)

        files_summary = (
            f"Files   : {self.completed_files}/{self.total_files} done  •  "
            f"{self.active_files} active  •  {remaining_languages}"
        )

        total_progress = format_progress_line(
            "Total",
            self.completed_entries,
            self.total_entries,
            self.renderer.supports_color,
            ANSI_CYAN,
        )
        current_total = 0 if self.current_plan is None else self.current_plan.entries_to_translate
        file_progress = format_progress_line(
            "File",
            self.current_file_entries_done,
            current_total,
            self.renderer.supports_color,
            ANSI_GREEN,
        )

        if self.current_plan is None:
            batch_line = "Batch  : none"
            batch_stream_line = format_progress_line(
                "Batch",
                0,
                0,
                self.renderer.supports_color,
                ANSI_MAGENTA,
            )
            batch_stream_meta_line = "Stream : waiting"
        else:
            if self.current_batch_total > 0:
                batch_elapsed = format_duration(time.time() - self.current_batch_started_at)
                batch_line = (
                    f"Batch  : {self.current_batch_index}/{self.current_batch_total}  •  "
                    f"{self.current_batch_size:,} keys  •  "
                    f"attempt {self.current_attempt}/{REQUEST_RETRY_COUNT}  •  "
                    f"{batch_elapsed}"
                )
                batch_stream_line = format_progress_line(
                    "Batch",
                    self.current_batch_stream_line_count,
                    self.current_batch_stream_line_total,
                    self.renderer.supports_color,
                    ANSI_MAGENTA,
                )
                batch_stream_meta_line = format_stream_meta_line(
                    self.current_batch_stream_chunk_count,
                    self.current_batch_stream_character_count,
                    self.current_batch_stream_comment,
                    self.renderer.supports_color,
                )
            else:
                batch_line = "Batch  : not started"
                batch_stream_line = format_progress_line(
                    "Batch",
                    0,
                    0,
                    self.renderer.supports_color,
                    ANSI_MAGENTA,
                )
                batch_stream_meta_line = "Stream : waiting"

        lines.extend(
            [
                make_box_separator("Queue", width, self.renderer.supports_color),
                make_box_content(current_file_label, width),
                make_box_content(target_path_line, width),
                make_box_content(files_summary, width),
                make_box_separator("Progress", width, self.renderer.supports_color),
                make_box_content(total_progress, width),
                make_box_content(file_progress, width),
                make_box_content(batch_stream_line, width),
                make_box_separator("Details", width, self.renderer.supports_color),
                make_box_content(file_stats_line, width),
                make_box_content(batch_line, width),
                make_box_content(batch_stream_meta_line, width),
                make_box_separator("Activity", width, self.renderer.supports_color),
                make_box_content(format_labeled_value("Status", self.current_note), width),
                make_box_content(format_labeled_value("Event", self.last_event), width),
                make_box_bottom(
                    format_labeled_value(
                        "Last stream line",
                        self.last_stream_line,
                    ),
                    width,
                    self.renderer.supports_color,
                ),
            ]
        )
        return lines


def prompt_mode() -> str:
    print("FancyMenu localization translator")
    print()
    print("Choose how translations should be processed:")
    print("  1) Update existing languages and create missing localization files")
    print("     Existing files only patch English keys changed since a git revision.")
    print("     Missing language files are created afterward from the full current en_us.json.")
    print("  2) Completely (re-)generate translations for all configured languages")
    print("     Every configured language file is regenerated from the full current en_us.json.")
    print("     Existing translation files are fully replaced, not patched.")
    print()

    while True:
        choice = input("Select 1 or 2: ").strip()
        if choice in {MODE_UPDATE_EXISTING, MODE_REGENERATE}:
            return choice
        print("Please enter 1 or 2.")


def prompt_target_languages() -> list[str]:
    available_languages = [
        language_code
        for language_code in TARGET_LANGUAGE_CODES
        if language_code != SOURCE_LANGUAGE_CODE
    ]

    print()
    print("Choose which target languages should be processed:")
    print("  1) All configured target languages")
    print("  2) Only a selected subset")
    print(f"Configured target languages: {', '.join(available_languages)}")
    print()

    while True:
        choice = input("Select 1 or 2: ").strip()
        if choice == LANGUAGE_SCOPE_ALL:
            return available_languages
        if choice == LANGUAGE_SCOPE_SELECTED:
            break
        print("Please enter 1 or 2.")

    print()
    print("Enter a comma-separated list of target language codes to process.")
    print(f"Available target languages: {', '.join(available_languages)}")

    while True:
        raw_value = input("Target languages: ").strip()
        raw_codes = [part.strip().lower() for part in raw_value.split(",") if part.strip()]
        if not raw_codes:
            print("Enter at least one target language code.")
            continue

        invalid_codes = sorted({code for code in raw_codes if code not in available_languages})
        if invalid_codes:
            print(f"Unknown target language codes: {', '.join(invalid_codes)}")
            continue

        selected_codes = {code for code in raw_codes}
        return [code for code in available_languages if code in selected_codes]


def prompt_commit_reference() -> str:
    print()
    print("Enter the git commit hash or revision to compare against the current en_us.json.")
    print("Examples: a1b2c3d4 or HEAD~1")

    while True:
        revision = input("Git revision: ").strip()
        if not revision:
            print("A git revision is required.")
            continue

        result = run_git_command(["rev-parse", "--verify", f"{revision}^{{commit}}"], check=False)
        if result.returncode == 0:
            return revision
        print("That git revision could not be resolved. Please try again.")


def run_git_command(arguments: list[str], check: bool) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", *arguments],
        cwd=SCRIPT_DIR,
        text=True,
        capture_output=True,
        check=check,
    )


def load_json_file(path: Path) -> OrderedDict[str, str]:
    try:
        with path.open("r", encoding="utf-8") as handle:
            data = json.load(handle, object_pairs_hook=OrderedDict)
    except FileNotFoundError as exc:
        raise ScriptError(f"Localization file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise ScriptError(f"Failed to parse JSON file: {path}\n{exc}") from exc

    return validate_localization_mapping(data, str(path))


def load_json_text(text: str, context: str) -> OrderedDict[str, str]:
    try:
        data = json.loads(text, object_pairs_hook=OrderedDict)
    except json.JSONDecodeError as exc:
        raise ScriptError(f"Failed to parse JSON for {context}.\n{exc}") from exc

    return validate_localization_mapping(data, context)


def validate_localization_mapping(data: object, context: str) -> OrderedDict[str, str]:
    if not isinstance(data, OrderedDict):
        raise ScriptError(f"{context} must contain a JSON object at the top level.")

    validated = OrderedDict()
    for key, value in data.items():
        if not isinstance(key, str):
            raise ScriptError(f"{context} contains a non-string localization key.")
        if not isinstance(value, str):
            raise ScriptError(f"{context} contains a non-string value for key '{key}'.")
        validated[key] = value
    return validated


def write_json_file(path: Path, data: OrderedDict[str, str]) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def serialize_localization_json(entries: OrderedDict[str, str]) -> str:
    return json.dumps(entries, ensure_ascii=False, indent=2)


def count_text_lines(text: str) -> int:
    if not text:
        return 0

    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    return normalized.count("\n") + 1


def extract_last_stream_line(text: str) -> str:
    if not text:
        return "Waiting for model output"

    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    for line in reversed(normalized.split("\n")):
        if line != "":
            return line
    return "Waiting for model output"


def resolve_openrouter_api_key() -> tuple[str, str]:
    if TOKENS_FILE_PATH.is_file():
        try:
            with TOKENS_FILE_PATH.open("r", encoding="utf-8") as handle:
                for raw_line in handle:
                    line = raw_line.strip()
                    if not line or line.startswith("#"):
                        continue

                    entry_name, separator, entry_value = line.partition(":")
                    if separator and entry_name.strip() == OPENROUTER_TOKENS_ENTRY_NAME:
                        token = entry_value.strip()
                        if not token:
                            raise ScriptError(
                                f"{TOKENS_FILE_PATH} contains an empty {OPENROUTER_TOKENS_ENTRY_NAME} entry."
                            )
                        return token, ".TOKENS file"
        except OSError as exc:
            raise ScriptError(f"Failed to read tokens file: {TOKENS_FILE_PATH}\n{exc}") from exc

    return OPENROUTER_API_KEY.strip(), "script variable"


def load_source_json_from_git(revision: str) -> OrderedDict[str, str]:
    result = run_git_command(["show", f"{revision}:{SOURCE_FILE_REPO_PATH}"], check=False)
    if result.returncode != 0:
        stderr = result.stderr.strip() or "git show failed."
        raise ScriptError(
            f"Could not read {SOURCE_FILE_REPO_PATH} from git revision '{revision}'.\n{stderr}"
        )
    return load_json_text(result.stdout, f"{SOURCE_FILE_REPO_PATH} at {revision}")


def compute_diff(old_source: OrderedDict[str, str], current_source: OrderedDict[str, str]) -> DiffResult:
    added_entries: OrderedDict[str, str] = OrderedDict()
    edited_entries: OrderedDict[str, str] = OrderedDict()
    changed_entries: OrderedDict[str, str] = OrderedDict()

    for key, value in current_source.items():
        if key not in old_source:
            added_entries[key] = value
            changed_entries[key] = value
            continue

        if old_source[key] != value:
            edited_entries[key] = value
            changed_entries[key] = value

    removed_keys = [key for key in old_source if key not in current_source]
    return DiffResult(added_entries, edited_entries, removed_keys, changed_entries)


def create_temp_diff_file(changed_entries: OrderedDict[str, str]) -> Path:
    with tempfile.NamedTemporaryFile(
        mode="w",
        encoding="utf-8",
        suffix=".json",
        prefix=TEMP_DIFF_PREFIX,
        dir=RESOLVED_LANG_DIRECTORY,
        delete=False,
    ) as handle:
        temp_path = Path(handle.name)

    write_json_file(temp_path, changed_entries)
    return temp_path


def chunk_entries(entries: OrderedDict[str, str], batch_size: int) -> list[OrderedDict[str, str]]:
    items = list(entries.items())
    return [
        OrderedDict(items[index : index + batch_size])
        for index in range(0, len(items), batch_size)
    ]


def split_batch_entries(entries: OrderedDict[str, str]) -> list[OrderedDict[str, str]] | None:
    items = list(entries.items())
    if len(items) <= 1:
        return None

    midpoint = len(items) // 2
    if midpoint <= 0 or midpoint >= len(items):
        return None

    return [
        OrderedDict(items[:midpoint]),
        OrderedDict(items[midpoint:]),
    ]


def extract_response_text(response_json: dict[str, object]) -> str:
    choices = response_json.get("choices")
    if not isinstance(choices, list) or not choices:
        raise ScriptError("OpenRouter response did not contain any choices.")

    first_choice = choices[0]
    if not isinstance(first_choice, dict):
        raise ScriptError("OpenRouter response choice had an unexpected format.")

    message = first_choice.get("message")
    if not isinstance(message, dict):
        raise ScriptError("OpenRouter response choice did not contain a message.")

    content = message.get("content")
    if isinstance(content, str):
        return content

    if isinstance(content, list):
        text_parts: list[str] = []
        for part in content:
            if not isinstance(part, dict):
                continue
            text = part.get("text")
            if isinstance(text, str):
                text_parts.append(text)

        if text_parts:
            return "".join(text_parts)

    raise ScriptError("OpenRouter response message did not contain text content.")


def parse_json_object_from_text(text: str, context: str) -> OrderedDict[str, str]:
    decoder = json.JSONDecoder(object_pairs_hook=OrderedDict)
    stripped = text.strip()

    if not stripped:
        raise ScriptError(f"The model returned an empty response for {context}.")

    candidate_texts = [stripped]
    if stripped.startswith("```"):
        lines = stripped.splitlines()
        if len(lines) >= 3 and lines[-1].strip().startswith("```"):
            candidate_texts.append("\n".join(lines[1:-1]).strip())

    for candidate in candidate_texts:
        for index, character in enumerate(candidate):
            if character != "{":
                continue
            try:
                parsed, _ = decoder.raw_decode(candidate[index:])
            except json.JSONDecodeError:
                continue
            return validate_localization_mapping(parsed, context)

    raise ScriptError(f"Could not extract a JSON object from the model response for {context}.")


def build_translation_request(language_code: str, batch_entries: OrderedDict[str, str]) -> dict[str, object]:
    batch_json = serialize_localization_json(batch_entries)
    user_prompt = USER_PROMPT_TEMPLATE.format(
        target_language_code=language_code,
        json_localization_line_batch=batch_json,
    )
    return {
        "model": OPENROUTER_MODEL,
        "temperature": 1,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        "response_format": {"type": "json_object"},
        "stream": True,
    }


def is_network_related_exception(exc: BaseException) -> bool:
    if isinstance(
        exc,
        (
            NetworkUnavailableError,
            TimeoutError,
            socket.timeout,
            ConnectionError,
            ConnectionResetError,
            ConnectionAbortedError,
            ConnectionRefusedError,
            BrokenPipeError,
            http.client.IncompleteRead,
            ssl.SSLError,
        ),
    ):
        return True

    if isinstance(exc, urllib.error.URLError):
        reason = exc.reason
        if isinstance(reason, BaseException):
            return is_network_related_exception(reason)
        return True

    if isinstance(exc, OSError):
        return True

    return False


def wrap_request_exception(prefix: str, exc: BaseException) -> RetryableRequestError:
    message = f"{prefix}: {exc}"
    if is_network_related_exception(exc):
        return NetworkUnavailableError(message)
    return RetryableRequestError(message)


def send_openrouter_request(
    request_body: dict[str, object],
    openrouter_api_key: str,
    tracker: ProgressTracker,
    stream_label: str,
) -> str:
    encoded_body = json.dumps(request_body, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        OPENROUTER_API_URL,
        data=encoded_body,
        headers={
            "Authorization": f"Bearer {openrouter_api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
            content_type = response.info().get("Content-Type", "").lower()
            try:
                if "text/event-stream" in content_type:
                    tracker.set_batch_stage(
                        "Streaming model response",
                        f"{stream_label} stream opened",
                    )
                    return read_openrouter_stream_response(response, tracker)

                response_text = response.read().decode("utf-8")
            except ScriptError as exc:
                raise RetryableRequestError(str(exc)) from exc
            except (
                TimeoutError,
                socket.timeout,
                ConnectionError,
                OSError,
                http.client.IncompleteRead,
                ssl.SSLError,
            ) as exc:
                raise wrap_request_exception("OpenRouter response read failed", exc) from exc
    except urllib.error.HTTPError as exc:
        error_text = exc.read().decode("utf-8", errors="replace")
        if request_body.get("response_format") and should_retry_without_json_mode(exc.code, error_text):
            fallback_body = dict(request_body)
            fallback_body.pop("response_format", None)
            return send_openrouter_request(
                fallback_body,
                openrouter_api_key,
                tracker,
                f"{stream_label} fallback without JSON mode",
            )
        raise RetryableRequestError(
            f"OpenRouter request failed with HTTP {exc.code}.\n{error_text}"
        ) from exc
    except (
        urllib.error.URLError,
        TimeoutError,
        socket.timeout,
        ConnectionError,
        OSError,
        http.client.IncompleteRead,
        ssl.SSLError,
    ) as exc:
        raise wrap_request_exception("OpenRouter request failed", exc) from exc

    try:
        parsed = json.loads(response_text)
    except json.JSONDecodeError as exc:
        raise RetryableRequestError(f"OpenRouter returned invalid JSON.\n{response_text}") from exc

    if not isinstance(parsed, dict):
        raise RetryableRequestError("OpenRouter returned a non-object response.")

    extracted_text = extract_response_text(parsed)
    tracker.update_stream_output(extracted_text)
    return extracted_text


def should_retry_without_json_mode(status_code: int, error_text: str) -> bool:
    if status_code < 400 or status_code >= 500:
        return False

    lowered = error_text.lower()
    return "response_format" in lowered or "json_object" in lowered or "structured output" in lowered


def extract_stream_delta_text(chunk_json: dict[str, object]) -> str:
    choices = chunk_json.get("choices")
    if not isinstance(choices, list) or not choices:
        return ""

    first_choice = choices[0]
    if not isinstance(first_choice, dict):
        return ""

    delta = first_choice.get("delta")
    if not isinstance(delta, dict):
        return ""

    content = delta.get("content")
    if isinstance(content, str):
        return content

    if isinstance(content, list):
        text_parts: list[str] = []
        for part in content:
            if isinstance(part, dict):
                text = part.get("text")
                if isinstance(text, str):
                    text_parts.append(text)
        return "".join(text_parts)

    return ""


def extract_stream_finish_reason(chunk_json: dict[str, object]) -> str | None:
    choices = chunk_json.get("choices")
    if not isinstance(choices, list) or not choices:
        return None

    first_choice = choices[0]
    if not isinstance(first_choice, dict):
        return None

    finish_reason = first_choice.get("finish_reason")
    if isinstance(finish_reason, str):
        return finish_reason
    return None


def format_stream_error_message(chunk_json: dict[str, object]) -> str:
    error_payload = chunk_json.get("error")
    if isinstance(error_payload, dict):
        message = error_payload.get("message")
        code = error_payload.get("code")
        if isinstance(message, str) and isinstance(code, str):
            return f"{code}: {message}"
        if isinstance(message, str):
            return message
    return "OpenRouter stream terminated with an error."


def process_stream_event(
    payload_lines: list[str],
    accumulated_text_parts: list[str],
    tracker: ProgressTracker,
) -> bool:
    if not payload_lines:
        return False

    payload = "\n".join(payload_lines).strip()
    if not payload:
        return False

    if payload == "[DONE]":
        return True

    try:
        chunk_json = json.loads(payload)
    except json.JSONDecodeError as exc:
        raise ScriptError(f"OpenRouter stream returned invalid JSON chunk.\n{payload}") from exc

    if not isinstance(chunk_json, dict):
        raise ScriptError(f"OpenRouter stream returned a non-object chunk.\n{payload}")

    if "error" in chunk_json:
        raise ScriptError(f"OpenRouter stream error: {format_stream_error_message(chunk_json)}")

    delta_text = extract_stream_delta_text(chunk_json)
    if delta_text:
        accumulated_text_parts.append(delta_text)
        tracker.update_stream_output("".join(accumulated_text_parts))

    finish_reason = extract_stream_finish_reason(chunk_json)
    if finish_reason == "error":
        raise ScriptError(f"OpenRouter stream error: {format_stream_error_message(chunk_json)}")

    return False


def read_openrouter_stream_response(response, tracker: ProgressTracker) -> str:
    accumulated_text_parts: list[str] = []
    payload_lines: list[str] = []
    stream_finished = False

    while True:
        raw_line = response.readline()
        if not raw_line:
            break

        line = raw_line.decode("utf-8", errors="replace").rstrip("\r\n")
        if not line:
            stream_finished = process_stream_event(payload_lines, accumulated_text_parts, tracker)
            payload_lines = []
            if stream_finished:
                break
            continue

        if line.startswith(":"):
            tracker.note_stream_comment(line[1:].strip())
            continue

        if line.startswith("data:"):
            payload_lines.append(line[5:].lstrip())

    if payload_lines and not stream_finished:
        process_stream_event(payload_lines, accumulated_text_parts, tracker)

    return "".join(accumulated_text_parts)


def translate_batch(
    language_code: str,
    batch_entries: OrderedDict[str, str],
    batch_index: int,
    total_batches: int,
    tracker: ProgressTracker,
    openrouter_api_key: str,
) -> OrderedDict[str, str]:
    expected_keys = list(batch_entries.keys())
    expected_output_lines = count_text_lines(serialize_localization_json(batch_entries))
    batch_label = f"{language_code}.json batch {batch_index}/{total_batches}"
    last_error: Exception | None = None

    attempt = 1
    while attempt <= REQUEST_RETRY_COUNT:
        tracker.start_batch(
            batch_index,
            total_batches,
            len(batch_entries),
            attempt,
            expected_output_lines,
        )

        try:
            request_body = build_translation_request(language_code, batch_entries)
            tracker.set_batch_stage(
                "Waiting for OpenRouter response",
                f"{batch_label} request sent",
            )
            response_text = send_openrouter_request(
                request_body,
                openrouter_api_key,
                tracker,
                batch_label,
            )

            tracker.set_batch_stage(
                "Validating translated JSON response",
                f"{batch_label} response received",
            )
            translated_entries = parse_json_object_from_text(
                response_text,
                f"{language_code} batch {batch_index}",
            )

            translated_keys = list(translated_entries.keys())
            missing_keys = [key for key in expected_keys if key not in translated_entries]
            extra_keys = [key for key in translated_keys if key not in batch_entries]
            if missing_keys or extra_keys:
                raise ScriptError(
                    "Translated JSON keys do not match the requested batch.\n"
                    f"Missing keys: {missing_keys}\n"
                    f"Extra keys: {extra_keys}"
                )

            return OrderedDict((key, translated_entries[key]) for key in expected_keys)
        except NetworkUnavailableError as exc:
            last_error = exc
            tracker.set_batch_stage(
                "Waiting for network connection to recover",
                f"{batch_label} paused: {exc} | retrying in {NETWORK_RETRY_WAIT_SECONDS}s",
            )
            time.sleep(NETWORK_RETRY_WAIT_SECONDS)
            continue
        except Exception as exc:
            last_error = exc
            if (
                attempt >= BATCH_SPLIT_AFTER_FAILED_ATTEMPTS
                and attempt < REQUEST_RETRY_COUNT
            ):
                split_batches = split_batch_entries(batch_entries)
                if split_batches is not None:
                    split_sizes = ", ".join(f"{len(batch):,}" for batch in split_batches)
                    tracker.set_batch_stage(
                        "Splitting failed batch into smaller requests",
                        (
                            f"{batch_label} failed {attempt} times; "
                            f"splitting {len(batch_entries):,} keys into {split_sizes}"
                        ),
                    )
                    raise BatchSplitRequest(
                        split_batches,
                        (
                            f"{batch_label} failed {attempt} times and was split into "
                            f"{split_sizes} keys"
                        ),
                    ) from exc
            if attempt < REQUEST_RETRY_COUNT:
                retry_delay = min(
                    REQUEST_RETRY_DELAY_MAX_SECONDS,
                    max(1, attempt * REQUEST_RETRY_DELAY_SECONDS),
                )
                tracker.set_batch_stage(
                    "Retrying failed batch",
                    f"{batch_label} failed: {exc} | retrying in {retry_delay}s",
                )
                time.sleep(retry_delay)
                attempt += 1
                continue
            break

    assert last_error is not None
    raise ScriptError(
        f"Failed to translate {language_code} batch {batch_index}/{total_batches}.\n{last_error}"
    )


def translate_entries(
    language_code: str,
    entries: OrderedDict[str, str],
    tracker: ProgressTracker,
    openrouter_api_key: str,
) -> OrderedDict[str, str]:
    if not entries:
        tracker.set_action("No translation request needed", f"{language_code}.json has 0 keys to translate")
        return OrderedDict()

    pending_batches = chunk_entries(entries, BATCH_SIZE)
    translated_entries: OrderedDict[str, str] = OrderedDict()
    completed_batches = 0

    while pending_batches:
        batch_entries = pending_batches.pop(0)
        batch_index = completed_batches + 1
        total_batches = completed_batches + 1 + len(pending_batches)

        try:
            translated_batch = translate_batch(
                language_code=language_code,
                batch_entries=batch_entries,
                batch_index=batch_index,
                total_batches=total_batches,
                tracker=tracker,
                openrouter_api_key=openrouter_api_key,
            )
        except BatchSplitRequest as exc:
            pending_batches = [*exc.split_batches, *pending_batches]
            continue

        translated_entries.update(translated_batch)
        tracker.finish_batch(len(batch_entries))
        completed_batches += 1

    return translated_entries


def merge_existing_translation(
    existing_entries: OrderedDict[str, str] | None,
    translated_entries: OrderedDict[str, str],
    removed_keys: list[str],
) -> OrderedDict[str, str]:
    merged_entries = OrderedDict(existing_entries or ())

    for key in removed_keys:
        merged_entries.pop(key, None)

    for key, value in translated_entries.items():
        merged_entries[key] = value

    return merged_entries


def validate_configuration(openrouter_api_key: str) -> None:
    if not openrouter_api_key or openrouter_api_key == "<openrouter_api_key_here>":
        raise ScriptError(
            "Set OPENROUTER_API_KEY at the top of the script before running it, or provide "
            f"{OPENROUTER_TOKENS_ENTRY_NAME} in {TOKENS_FILE_PATH}."
        )

    if BATCH_SIZE <= 0:
        raise ScriptError("BATCH_SIZE must be greater than 0.")

    if BATCH_SPLIT_AFTER_FAILED_ATTEMPTS <= 0:
        raise ScriptError("BATCH_SPLIT_AFTER_FAILED_ATTEMPTS must be greater than 0.")

    if not RESOLVED_LANG_DIRECTORY.is_dir():
        raise ScriptError(f"Language directory does not exist: {RESOLVED_LANG_DIRECTORY}")

    if not SOURCE_FILE_PATH.is_file():
        raise ScriptError(f"Main localization file does not exist: {SOURCE_FILE_PATH}")


def build_update_file_plans(
    current_source: OrderedDict[str, str],
    changed_entries: OrderedDict[str, str],
    diff: DiffResult,
    target_paths: dict[str, Path],
) -> list[FilePlan]:
    existing_file_plans: list[FilePlan] = []
    missing_file_plans: list[FilePlan] = []

    for language_code, target_path in target_paths.items():
        if not target_path.exists():
            missing_file_plans.append(
                FilePlan(
                    file_number=0,
                    language_code=language_code,
                    path=target_path,
                    action_label="Creating missing localization file",
                    result_label="created new file",
                    translation_source_entries=current_source,
                    entries_to_translate=len(current_source),
                )
            )
            continue

        existing_entries = load_json_file(target_path)
        if not changed_entries and not diff.removed_keys:
            existing_file_plans.append(
                FilePlan(
                    file_number=0,
                    language_code=language_code,
                    path=target_path,
                    action_label="Skipping unchanged localization file",
                    result_label="skipped (no English changes)",
                    skipped=True,
                    skip_reason="No English key changes to apply for this update run",
                )
            )
            continue

        existing_file_plans.append(
            FilePlan(
                file_number=0,
                language_code=language_code,
                path=target_path,
                action_label="Patching existing localization file",
                result_label="patched existing file",
                translation_source_entries=changed_entries,
                existing_entries=existing_entries,
                removed_keys=list(diff.removed_keys),
                entries_to_translate=len(changed_entries),
                added_keys_count=len(diff.added_entries),
                edited_keys_count=len(diff.edited_entries),
                obsolete_keys_count=len(diff.removed_keys),
            )
        )

    plans = [*existing_file_plans, *missing_file_plans]
    for file_number, plan in enumerate(plans, start=1):
        plan.file_number = file_number

    return plans


def build_regenerate_file_plans(
    current_source: OrderedDict[str, str],
    target_paths: dict[str, Path],
) -> list[FilePlan]:
    plans: list[FilePlan] = []

    for file_number, (language_code, target_path) in enumerate(target_paths.items(), start=1):
        plans.append(
            FilePlan(
                file_number=file_number,
                language_code=language_code,
                path=target_path,
                action_label="Regenerating localization file" if target_path.exists() else "Creating regenerated localization file",
                result_label="regenerated file" if target_path.exists() else "created new file",
                translation_source_entries=current_source,
                entries_to_translate=len(current_source),
                target_existed_before_run=target_path.exists(),
                force_full_rewrite=True,
            )
        )

    return plans


def process_file_plans(
    plans: list[FilePlan],
    tracker: ProgressTracker,
    openrouter_api_key: str,
) -> dict[str, str]:
    results: dict[str, str] = {}

    for plan in plans:
        if plan.skipped:
            tracker.skip_file(plan)
            results[plan.language_code] = plan.result_label
            continue

        tracker.start_file(plan)
        translated_entries = translate_entries(
            plan.language_code,
            plan.translation_source_entries,
            tracker,
            openrouter_api_key,
        )

        if plan.force_full_rewrite:
            tracker.set_action("Replacing localization file with regenerated translation", f"{plan.language_code}.json")
            merged_entries = OrderedDict(translated_entries)
        else:
            tracker.set_action("Merging translated keys into localization file", f"{plan.language_code}.json")
            merged_entries = merge_existing_translation(plan.existing_entries, translated_entries, plan.removed_keys)

        tracker.set_action("Writing localization file to disk", f"{plan.path.name}")
        write_json_file(plan.path, merged_entries)

        if plan.force_full_rewrite:
            if plan.target_existed_before_run:
                result_note = f"Regenerated {plan.path.name} with {len(merged_entries):,} keys"
            else:
                result_note = f"Created {plan.path.name} with {len(merged_entries):,} keys"
        elif plan.existing_entries is None:
            result_note = f"Created {plan.path.name} with {len(merged_entries):,} keys"
        else:
            result_note = (
                f"Wrote {plan.path.name} | translated {len(translated_entries):,} keys | "
                f"removed {len(plan.removed_keys):,} keys"
            )

        tracker.finish_file(plan, result_note)
        results[plan.language_code] = plan.result_label

    return results


def print_summary(
    results: dict[str, str],
    tracker: ProgressTracker,
    selected_language_codes: list[str],
) -> None:
    print()
    print("Summary:")
    print(f"  Mode: {tracker.mode_label}")
    print(f"  Elapsed: {format_duration(time.time() - tracker.start_time)}")
    print(f"  Total translated keys: {tracker.completed_entries:,}/{tracker.total_entries:,}")
    print(f"  Target scope: {', '.join(selected_language_codes)}")
    for language_code in selected_language_codes:
        result = results.get(language_code)
        if result is None:
            continue
        print(f"  {language_code}: {result}")


def describe_file_stats(mode_label: str, plan: FilePlan) -> str:
    if plan.skipped:
        return f"Stats  : skipped  •  {plan.skip_reason}"

    if plan.force_full_rewrite:
        if plan.target_existed_before_run:
            return f"Stats  : replace existing file  •  full translate {plan.entries_to_translate:,} keys"
        return f"Stats  : new file  •  full translate {plan.entries_to_translate:,} keys"

    if plan.existing_entries is None:
        return f"Stats  : new file  •  full translate {plan.entries_to_translate:,} keys"

    if mode_label == "update-existing":
        return (
            "Stats  : "
            f"patch {plan.entries_to_translate:,}  •  "
            f"english added {plan.added_keys_count:,}  •  "
            f"english edited {plan.edited_keys_count:,}  •  "
            f"english removed {plan.obsolete_keys_count:,}"
        )

    return f"Stats  : new file  •  full translate {plan.entries_to_translate:,} keys"


def format_duration(seconds: float) -> str:
    total_seconds = max(0, int(seconds))
    hours, remainder = divmod(total_seconds, 3600)
    minutes, secs = divmod(remainder, 60)
    return f"{hours:02}:{minutes:02}:{secs:02}"


def format_progress_line(
    label: str,
    current: int,
    total: int,
    supports_color: bool,
    accent_color: str,
) -> str:
    if total <= 0:
        ratio = 0.0
        filled = 0
    else:
        ratio = max(0.0, min(1.0, current / total))
        filled = int(ratio * PROGRESS_BAR_WIDTH)
        if current > 0 and filled == 0:
            filled = 1

    empty = PROGRESS_BAR_WIDTH - filled
    percent = ratio * 100.0
    filled_bar = "█" * filled
    empty_bar = "░" * empty

    if supports_color and filled_bar:
        filled_bar = colorize(filled_bar, accent_color, bold=True)
    if supports_color and empty_bar:
        empty_bar = colorize(empty_bar, ANSI_BRIGHT_BLACK)

    bar = f"{filled_bar}{empty_bar}"
    label_text = colorize(f"{label:<5}", ANSI_WHITE, bold=True) if supports_color else f"{label:<5}"
    return f"{label_text} {bar}  {current:,}/{total:,}  {percent:5.1f}%"


def get_dashboard_width() -> int:
    width = shutil.get_terminal_size((140, 24)).columns
    return max(20, min(width, DASHBOARD_MAX_WIDTH))


def strip_ansi(text: str) -> str:
    return ANSI_ESCAPE_RE.sub("", text)


def visible_len(text: str) -> int:
    return len(strip_ansi(text))


def colorize(text: str, *codes: str, bold: bool = False) -> str:
    if not codes and not bold:
        return text
    prefix = ""
    if bold:
        prefix += ANSI_BOLD
    prefix += "".join(codes)
    return f"{prefix}{text}{ANSI_RESET}"


def fit_text(text: str, width: int | None = None) -> str:
    width = width or get_dashboard_width()
    if width < 20:
        return strip_ansi(text)

    if visible_len(text) <= width:
        return text

    plain_text = strip_ansi(text)
    if width <= 3:
        return plain_text[:width]
    return plain_text[: width - 3] + "..."


def pad_visible(text: str, width: int) -> str:
    text = fit_text(text, width)
    padding = max(0, width - visible_len(text))
    return f"{text}{' ' * padding}"


def sanitize_inline_text(text: str) -> str:
    cleaned = " ".join(text.split())
    return cleaned if cleaned else "-"


def format_labeled_value(label: str, value: str) -> str:
    return f"{label:<16}: {sanitize_inline_text(value)}"


def format_remaining_languages(plans: list[FilePlan]) -> str:
    if not plans:
        return "Remaining: none"
    languages = ", ".join(plan.language_code for plan in plans)
    return f"Remaining: {len(plans)} file(s)  •  {languages}"


def format_status_summary(
    mode_label: str,
    current_action: str,
    elapsed: str,
    supports_color: bool,
) -> str:
    mode_text = colorize(mode_label, ANSI_CYAN, bold=True) if supports_color else mode_label
    action_text = style_action(current_action, supports_color)
    elapsed_text = colorize(elapsed, ANSI_YELLOW, bold=True) if supports_color else elapsed
    return f"Mode {mode_text}  •  Action {action_text}  •  Elapsed {elapsed_text}"


def style_action(action: str, supports_color: bool) -> str:
    if not supports_color:
        return action

    lower_action = action.lower()
    if "complete" in lower_action or "finished" in lower_action:
        return colorize(action, ANSI_GREEN, bold=True)
    if "stream" in lower_action:
        return colorize(action, ANSI_BLUE, bold=True)
    if "wait" in lower_action or "prepar" in lower_action:
        return colorize(action, ANSI_YELLOW, bold=True)
    if "error" in lower_action or "cancel" in lower_action:
        return colorize(action, ANSI_RED, bold=True)
    return colorize(action, ANSI_MAGENTA, bold=True)


def format_stream_meta_line(
    chunk_count: int,
    character_count: int,
    stream_comment: str,
    supports_color: bool,
) -> str:
    base = f"Stream : {chunk_count:,} chunks  •  {character_count:,} chars"
    if stream_comment:
        comment = sanitize_inline_text(stream_comment)
        if supports_color:
            comment = colorize(comment, ANSI_BLUE, bold=True)
        return f"{base}  •  event {comment}"
    return base


def make_box_top(title: str, width: int, supports_color: bool) -> str:
    title_text = f" {title} "
    if supports_color:
        title_text = colorize(title_text, ANSI_CYAN, bold=True)
    return make_border_line("┌", "┐", title_text, width)


def make_box_separator(title: str, width: int, supports_color: bool) -> str:
    title_text = f" {title} "
    if supports_color:
        title_text = colorize(title_text, ANSI_WHITE, bold=True)
    return make_border_line("├", "┤", title_text, width)


def make_box_bottom(text: str, width: int, supports_color: bool) -> str:
    footer_text = f" {text} "
    if supports_color:
        footer_text = colorize(footer_text, ANSI_GREEN, bold=True)
    return make_border_line("└", "┘", footer_text, width)


def make_border_line(left: str, right: str, content: str, width: int) -> str:
    inner_width = max(10, width - 2)
    plain_content = fit_text(content, inner_width)
    fill_count = max(0, inner_width - visible_len(plain_content))
    return f"{left}{plain_content}{'─' * fill_count}{right}"


def make_box_content(text: str, width: int) -> str:
    inner_width = max(10, width - 4)
    return f"│ {pad_visible(text, inner_width)} │"


def main() -> int:
    tracker: ProgressTracker | None = None
    try:
        openrouter_api_key, api_key_source = resolve_openrouter_api_key()
        validate_configuration(openrouter_api_key)
        if api_key_source == ".TOKENS file":
            print(
                f"Using OpenRouter API key from {TOKENS_FILE_PATH} "
                f"({OPENROUTER_TOKENS_ENTRY_NAME})."
            )

        current_source = load_json_file(SOURCE_FILE_PATH)
        mode = prompt_mode()
        selected_language_codes = prompt_target_languages()
        target_paths = {
            language_code: RESOLVED_LANG_DIRECTORY / f"{language_code}.json"
            for language_code in selected_language_codes
        }
        available_target_language_count = len(
            [language_code for language_code in TARGET_LANGUAGE_CODES if language_code != SOURCE_LANGUAGE_CODE]
        )
        scope_label = (
            f"Target scope: all configured languages ({len(selected_language_codes)})"
            if len(selected_language_codes) == available_target_language_count
            else f"Target scope: selected languages ({len(selected_language_codes)})"
        )
        scope_languages_line = f"Targets: {', '.join(selected_language_codes)}"

        if mode == MODE_UPDATE_EXISTING:
            revision = prompt_commit_reference()
            old_source = load_source_json_from_git(revision)
            diff = compute_diff(old_source, current_source)

            temp_diff_path: Path | None = None
            try:
                temp_diff_path = create_temp_diff_file(diff.changed_entries)
                changed_entries = load_json_file(temp_diff_path)
                plans = build_update_file_plans(current_source, changed_entries, diff, target_paths)
                existing_file_count = sum(
                    1 for plan in plans if plan.existing_entries is not None and not plan.skipped
                )
                new_file_count = sum(1 for plan in plans if not plan.skipped and plan.existing_entries is None)
                skipped_count = sum(1 for plan in plans if plan.skipped)
                overview_lines = [
                    f"Source: {SOURCE_FILE_PATH.name} | Revision diff vs {revision}",
                    scope_label,
                    scope_languages_line,
                    (
                        f"Model: {OPENROUTER_MODEL} | Batch size: {BATCH_SIZE:,} | "
                        f"Temp diff: {temp_diff_path.name} | API key source: {api_key_source}"
                    ),
                    (
                        "Run stats: "
                        f"english added {len(diff.added_entries):,} | "
                        f"english edited {len(diff.edited_entries):,} | "
                        f"english removed {len(diff.removed_keys):,} | "
                        f"existing files to patch {existing_file_count} | "
                        f"missing files to create {new_file_count} | "
                        f"skipped unchanged files {skipped_count}"
                    ),
                ]
                tracker = ProgressTracker("update-existing", plans, overview_lines)
                tracker.set_action(
                    "Preparing incremental update run",
                    f"Temp diff file: {temp_diff_path.name}",
                )
                results = process_file_plans(plans, tracker, openrouter_api_key)
            finally:
                if temp_diff_path is not None and temp_diff_path.exists():
                    temp_diff_path.unlink()

            tracker.finish_run("All update-mode files finished")
            tracker.shutdown()
            print_summary(results, tracker, selected_language_codes)
            return 0

        plans = build_regenerate_file_plans(current_source, target_paths)
        existing_file_count = sum(1 for plan in plans if plan.target_existed_before_run)
        new_file_count = sum(1 for plan in plans if not plan.target_existed_before_run)
        overview_lines = [
            f"Source: {SOURCE_FILE_PATH.name} | Full regeneration from current English source",
            scope_label,
            scope_languages_line,
            f"Model: {OPENROUTER_MODEL} | Batch size: {BATCH_SIZE:,} | API key source: {api_key_source}",
            (
                "Run stats: "
                f"files to replace {existing_file_count} | "
                f"missing files to create {new_file_count} | "
                f"keys per file {len(current_source):,}"
            ),
        ]
        tracker = ProgressTracker("regenerate", plans, overview_lines)
        tracker.set_action("Preparing full regeneration run", "Starting translation work")
        results = process_file_plans(plans, tracker, openrouter_api_key)

        tracker.finish_run("All regenerate-mode files finished")
        tracker.shutdown()
        print_summary(results, tracker, selected_language_codes)
        return 0
    except KeyboardInterrupt:
        if tracker is not None:
            tracker.shutdown()
        print("\nOperation cancelled.")
        return 1
    except ScriptError as exc:
        if tracker is not None:
            tracker.shutdown()
        print(f"Error: {exc}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
