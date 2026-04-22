#!/usr/bin/env python3
from __future__ import annotations

import json
import shutil
import subprocess
import sys
import tempfile
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
BATCH_SIZE = 500

OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
REQUEST_TIMEOUT_SECONDS = 240
REQUEST_RETRY_COUNT = 3
TEMP_DIFF_PREFIX = "fancymenu_translation_diff_"
PROGRESS_BAR_WIDTH = 28

SYSTEM_PROMPT = """You are a professional Minecraft mod localization translator. You translate Minecraft-style localization JSONs from English to the target language. You only translate the value, never the translation keys. You never remove or add lines. You translate every line of the JSON to the target language and return back the translated version of the received JSON. Make sure you return ONLY THE TRANSLATED JSON as valid JSON, no other text. You translate mod localizations to natural sounding text in the target language, which means you sometimes swap words for better fitting ones in the target language, instead of translating directly. You also make sure to use proper gaming and Minecraft slang when translating, which means that when the target language commonly uses terms for specific words that are not the perfect direct translation, but would work best, then you will use this term, to make the translation sound more natural and high-quality. Use an informal tone for translations, like the 'Du' tone in German."""
USER_PROMPT_TEMPLATE = """Please translate the following localization to {target_language_code}. Return ONLY THE TRANSLATED JSON, no other text! Here is the JSON:

{json_localization_line_batch}"""

MODE_UPDATE_EXISTING = "1"
MODE_REGENERATE = "2"
OPENROUTER_TOKENS_ENTRY_NAME = "openrouter_mod_localization_key"

SCRIPT_DIR = Path(__file__).resolve().parent
TOKENS_FILE_PATH = (SCRIPT_DIR / "../../../.TOKENS").resolve()
RESOLVED_LANG_DIRECTORY = (SCRIPT_DIR / LANG_DIRECTORY).resolve()
SOURCE_FILE_PATH = RESOLVED_LANG_DIRECTORY / f"{SOURCE_LANGUAGE_CODE}.json"
SOURCE_FILE_REPO_PATH = SOURCE_FILE_PATH.relative_to(SCRIPT_DIR).as_posix()


class ScriptError(RuntimeError):
    pass


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


class TerminalRenderer:
    def __init__(self) -> None:
        self.dynamic = sys.stdout.isatty()
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
        self.last_event = "No file processed yet"
        self.render()

    def set_action(self, action: str, note: str | None = None) -> None:
        self.current_action = action
        if note is not None:
            self.current_note = note
        self.render()

    def start_file(self, plan: FilePlan) -> None:
        self.current_plan = plan
        self.current_action = plan.action_label
        self.current_note = f"Preparing {plan.language_code}.json"
        self.current_file_entries_done = 0
        self.current_batch_index = 0
        self.current_batch_total = 0
        self.current_batch_size = 0
        self.current_attempt = 0
        self.render()

    def skip_file(self, plan: FilePlan) -> None:
        self.current_plan = plan
        self.current_action = plan.action_label
        self.current_note = plan.skip_reason
        self.current_file_entries_done = 0
        self.current_batch_index = 0
        self.current_batch_total = 0
        self.current_batch_size = 0
        self.current_attempt = 0
        self.last_event = f"{plan.language_code}.json skipped: {plan.skip_reason}"
        self.completed_files += 1
        self.render()

    def start_batch(self, batch_index: int, total_batches: int, batch_size: int, attempt: int) -> None:
        if self.current_plan is None:
            return

        self.current_batch_index = batch_index
        self.current_batch_total = total_batches
        self.current_batch_size = batch_size
        self.current_attempt = attempt
        self.current_action = "Waiting for OpenRouter response"
        self.current_note = (
            f"{self.current_plan.language_code}.json batch {batch_index}/{total_batches} "
            f"({batch_size:,} keys)"
        )
        self.render()

    def set_batch_stage(self, action: str, note: str) -> None:
        self.current_action = action
        self.current_note = note
        self.render()

    def finish_batch(self, processed_entries: int) -> None:
        self.current_file_entries_done += processed_entries
        self.completed_entries += processed_entries
        if self.current_plan is not None:
            self.last_event = (
                f"{self.current_plan.language_code}.json finished batch "
                f"{self.current_batch_index}/{self.current_batch_total}"
            )
        self.current_action = "Batch translated"
        self.current_note = f"Processed {processed_entries:,} keys in the latest batch"
        self.render()

    def finish_file(self, plan: FilePlan, result_note: str) -> None:
        self.current_plan = plan
        self.current_action = "File finished"
        self.current_note = result_note
        self.current_file_entries_done = plan.entries_to_translate
        self.current_batch_index = self.current_batch_total
        self.current_attempt = 0
        self.last_event = f"{plan.language_code}.json completed"
        self.completed_files += 1
        self.render()
        self.current_plan = None

    def finish_run(self, final_note: str) -> None:
        self.current_action = "Run complete"
        self.current_note = final_note
        self.current_plan = None
        self.current_file_entries_done = 0
        self.current_batch_index = 0
        self.current_batch_total = 0
        self.current_batch_size = 0
        self.current_attempt = 0
        self.render()

    def render(self) -> None:
        lines = self.build_lines()
        self.renderer.render(lines)

    def build_lines(self) -> list[str]:
        elapsed = format_duration(time.time() - self.start_time)
        lines = [
            fit_text("FancyMenu localization translator"),
            fit_text(f"Mode: {self.mode_label} | Action: {self.current_action} | Elapsed: {elapsed}"),
        ]

        for overview_line in self.overview_lines:
            lines.append(fit_text(overview_line))

        if self.current_plan is None:
            current_file_label = "Current file: none"
            remaining_languages = ", ".join(plan.language_code for plan in self.plans[self.completed_files :]) or "none"
            file_stats_line = "File stats: waiting"
        else:
            current_file_label = (
                f"Current file: {self.current_plan.language_code}.json "
                f"({self.current_plan.file_number}/{self.total_files})"
            )
            remaining_languages = ", ".join(
                plan.language_code
                for plan in self.plans[self.current_plan.file_number :]
            ) or "none"
            file_stats_line = describe_file_stats(self.mode_label, self.current_plan)

        files_summary = (
            f"Files done: {self.completed_files}/{self.total_files} | "
            f"Active files: {self.active_files} | "
            f"Remaining after current: {remaining_languages}"
        )

        total_progress = format_progress_line("Total", self.completed_entries, self.total_entries)
        current_total = 0 if self.current_plan is None else self.current_plan.entries_to_translate
        file_progress = format_progress_line("File ", self.current_file_entries_done, current_total)

        if self.current_plan is None:
            batch_line = "Batch: none"
            target_path_line = "Target path: none"
        else:
            if self.current_batch_total > 0:
                batch_line = (
                    f"Batch: {self.current_batch_index}/{self.current_batch_total} | "
                    f"Size: {self.current_batch_size:,} | "
                    f"Attempt: {self.current_attempt}/{REQUEST_RETRY_COUNT}"
                )
            else:
                batch_line = "Batch: not started"
            target_path_line = f"Target path: {self.current_plan.path.name}"

        lines.extend(
            [
                fit_text(current_file_label),
                fit_text(files_summary),
                fit_text(total_progress),
                fit_text(file_progress),
                fit_text(file_stats_line),
                fit_text(target_path_line),
                fit_text(batch_line),
                fit_text(f"Status note: {self.current_note}"),
                fit_text(f"Last event: {self.last_event}"),
            ]
        )
        return lines


def prompt_mode() -> str:
    print("FancyMenu localization translator")
    print()
    print("Choose how translations should be processed:")
    print("  1) Only update languages that already have a localization file")
    print("     Existing files are refreshed against the current en_us.json.")
    print("     Missing language files are skipped.")
    print("  2) Completely (re-)generate translations for all configured languages")
    print("     Existing files only patch English keys changed since a git revision.")
    print("     Missing language files are generated from the full current en_us.json.")
    print()

    while True:
        choice = input("Select 1 or 2: ").strip()
        if choice in {MODE_UPDATE_EXISTING, MODE_REGENERATE}:
            return choice
        print("Please enter 1 or 2.")


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
    batch_json = json.dumps(batch_entries, ensure_ascii=False, indent=2)
    user_prompt = USER_PROMPT_TEMPLATE.format(
        target_language_code=language_code,
        json_localization_line_batch=batch_json,
    )
    return {
        "model": OPENROUTER_MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        "response_format": {"type": "json_object"},
    }


def send_openrouter_request(
    request_body: dict[str, object],
    openrouter_api_key: str,
) -> dict[str, object]:
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
            response_text = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        error_text = exc.read().decode("utf-8", errors="replace")
        if request_body.get("response_format") and should_retry_without_json_mode(exc.code, error_text):
            fallback_body = dict(request_body)
            fallback_body.pop("response_format", None)
            return send_openrouter_request(fallback_body, openrouter_api_key)
        raise ScriptError(
            f"OpenRouter request failed with HTTP {exc.code}.\n{error_text}"
        ) from exc
    except urllib.error.URLError as exc:
        raise ScriptError(f"OpenRouter request failed: {exc}") from exc

    try:
        parsed = json.loads(response_text)
    except json.JSONDecodeError as exc:
        raise ScriptError(f"OpenRouter returned invalid JSON.\n{response_text}") from exc

    if not isinstance(parsed, dict):
        raise ScriptError("OpenRouter returned a non-object response.")
    return parsed


def should_retry_without_json_mode(status_code: int, error_text: str) -> bool:
    if status_code < 400 or status_code >= 500:
        return False

    lowered = error_text.lower()
    return "response_format" in lowered or "json_object" in lowered or "structured output" in lowered


def translate_batch(
    language_code: str,
    batch_entries: OrderedDict[str, str],
    batch_index: int,
    total_batches: int,
    tracker: ProgressTracker,
    openrouter_api_key: str,
) -> OrderedDict[str, str]:
    expected_keys = list(batch_entries.keys())
    last_error: Exception | None = None

    for attempt in range(1, REQUEST_RETRY_COUNT + 1):
        tracker.start_batch(batch_index, total_batches, len(batch_entries), attempt)

        try:
            request_body = build_translation_request(language_code, batch_entries)
            tracker.set_batch_stage(
                "Waiting for OpenRouter response",
                f"{language_code}.json batch {batch_index}/{total_batches} request sent",
            )
            response_json = send_openrouter_request(request_body, openrouter_api_key)

            tracker.set_batch_stage(
                "Validating translated JSON response",
                f"{language_code}.json batch {batch_index}/{total_batches} response received",
            )
            response_text = extract_response_text(response_json)
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
        except Exception as exc:
            last_error = exc
            if attempt < REQUEST_RETRY_COUNT:
                tracker.set_batch_stage(
                    "Retrying failed batch",
                    f"{language_code}.json batch {batch_index}/{total_batches} failed: {exc}",
                )
                time.sleep(attempt * 2)

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

    batches = chunk_entries(entries, BATCH_SIZE)
    translated_entries: OrderedDict[str, str] = OrderedDict()

    for batch_index, batch_entries in enumerate(batches, start=1):
        translated_batch = translate_batch(
            language_code=language_code,
            batch_entries=batch_entries,
            batch_index=batch_index,
            total_batches=len(batches),
            tracker=tracker,
            openrouter_api_key=openrouter_api_key,
        )
        translated_entries.update(translated_batch)
        tracker.finish_batch(len(batch_entries))

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

    if not RESOLVED_LANG_DIRECTORY.is_dir():
        raise ScriptError(f"Language directory does not exist: {RESOLVED_LANG_DIRECTORY}")

    if not SOURCE_FILE_PATH.is_file():
        raise ScriptError(f"Main localization file does not exist: {SOURCE_FILE_PATH}")


def build_update_file_plans(
    current_source: OrderedDict[str, str],
    target_paths: dict[str, Path],
) -> list[FilePlan]:
    plans: list[FilePlan] = []

    for file_number, (language_code, target_path) in enumerate(target_paths.items(), start=1):
        if not target_path.exists():
            plans.append(
                FilePlan(
                    file_number=file_number,
                    language_code=language_code,
                    path=target_path,
                    action_label="Skipping missing localization file",
                    result_label="skipped (missing file)",
                    skipped=True,
                    skip_reason="Target localization file does not exist in update mode",
                )
            )
            continue

        existing_entries = load_json_file(target_path)
        missing_keys_count = sum(1 for key in current_source if key not in existing_entries)
        overwrite_keys_count = len(current_source) - missing_keys_count
        obsolete_keys = [key for key in existing_entries if key not in current_source]

        plans.append(
            FilePlan(
                file_number=file_number,
                language_code=language_code,
                path=target_path,
                action_label="Updating existing localization file",
                result_label="updated",
                translation_source_entries=current_source,
                existing_entries=existing_entries,
                removed_keys=obsolete_keys,
                entries_to_translate=len(current_source),
                missing_keys_count=missing_keys_count,
                overwrite_keys_count=overwrite_keys_count,
                obsolete_keys_count=len(obsolete_keys),
            )
        )

    return plans


def build_regenerate_file_plans(
    current_source: OrderedDict[str, str],
    changed_entries: OrderedDict[str, str],
    diff: DiffResult,
    target_paths: dict[str, Path],
) -> list[FilePlan]:
    plans: list[FilePlan] = []

    for file_number, (language_code, target_path) in enumerate(target_paths.items(), start=1):
        if target_path.exists():
            plans.append(
                FilePlan(
                    file_number=file_number,
                    language_code=language_code,
                    path=target_path,
                    action_label="Patching existing localization file",
                    result_label="patched existing file",
                    translation_source_entries=changed_entries,
                    existing_entries=load_json_file(target_path),
                    removed_keys=list(diff.removed_keys),
                    entries_to_translate=len(changed_entries),
                    added_keys_count=len(diff.added_entries),
                    edited_keys_count=len(diff.edited_entries),
                    obsolete_keys_count=len(diff.removed_keys),
                )
            )
            continue

        plans.append(
            FilePlan(
                file_number=file_number,
                language_code=language_code,
                path=target_path,
                action_label="Creating new localization file",
                result_label="created new file",
                translation_source_entries=current_source,
                entries_to_translate=len(current_source),
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

        tracker.set_action("Merging translated keys into localization file", f"{plan.language_code}.json")
        merged_entries = merge_existing_translation(plan.existing_entries, translated_entries, plan.removed_keys)

        tracker.set_action("Writing localization file to disk", f"{plan.path.name}")
        write_json_file(plan.path, merged_entries)

        if plan.existing_entries is None:
            result_note = f"Created {plan.path.name} with {len(merged_entries):,} keys"
        else:
            result_note = (
                f"Wrote {plan.path.name} | translated {len(translated_entries):,} keys | "
                f"removed {len(plan.removed_keys):,} keys"
            )

        tracker.finish_file(plan, result_note)
        results[plan.language_code] = plan.result_label

    return results


def print_summary(results: dict[str, str], tracker: ProgressTracker) -> None:
    print()
    print("Summary:")
    print(f"  Mode: {tracker.mode_label}")
    print(f"  Elapsed: {format_duration(time.time() - tracker.start_time)}")
    print(f"  Total translated keys: {tracker.completed_entries:,}/{tracker.total_entries:,}")
    for language_code in TARGET_LANGUAGE_CODES:
        result = results.get(language_code)
        if result is None:
            continue
        print(f"  {language_code}: {result}")


def describe_file_stats(mode_label: str, plan: FilePlan) -> str:
    if plan.skipped:
        return f"File stats: skipped | {plan.skip_reason}"

    if mode_label == "update-existing":
        return (
            "File stats: "
            f"translate {plan.entries_to_translate:,} | "
            f"overwrite {plan.overwrite_keys_count:,} | "
            f"missing add {plan.missing_keys_count:,} | "
            f"obsolete remove {plan.obsolete_keys_count:,}"
        )

    if plan.existing_entries is not None:
        return (
            "File stats: "
            f"patch {plan.entries_to_translate:,} | "
            f"english added {plan.added_keys_count:,} | "
            f"english edited {plan.edited_keys_count:,} | "
            f"english removed {plan.obsolete_keys_count:,}"
        )

    return f"File stats: new file | full translate {plan.entries_to_translate:,} keys"


def format_duration(seconds: float) -> str:
    total_seconds = max(0, int(seconds))
    hours, remainder = divmod(total_seconds, 3600)
    minutes, secs = divmod(remainder, 60)
    return f"{hours:02}:{minutes:02}:{secs:02}"


def format_progress_line(label: str, current: int, total: int) -> str:
    if total <= 0:
        ratio = 1.0
        filled = PROGRESS_BAR_WIDTH
    else:
        ratio = max(0.0, min(1.0, current / total))
        filled = int(ratio * PROGRESS_BAR_WIDTH)
        if current > 0 and filled == 0:
            filled = 1

    empty = PROGRESS_BAR_WIDTH - filled
    percent = ratio * 100.0
    bar = f"[{'#' * filled}{'-' * empty}]"
    return f"{label} progress: {bar} {current:,}/{total:,} ({percent:5.1f}%)"


def fit_text(text: str) -> str:
    width = shutil.get_terminal_size((140, 24)).columns
    if width < 20:
        return text

    if len(text) <= width - 1:
        return text

    return text[: width - 4] + "..."


def main() -> int:
    try:
        openrouter_api_key, api_key_source = resolve_openrouter_api_key()
        validate_configuration(openrouter_api_key)
        if api_key_source == ".TOKENS file":
            print(
                f"Using OpenRouter API key from {TOKENS_FILE_PATH} "
                f"({OPENROUTER_TOKENS_ENTRY_NAME})."
            )

        current_source = load_json_file(SOURCE_FILE_PATH)
        target_paths = {
            language_code: RESOLVED_LANG_DIRECTORY / f"{language_code}.json"
            for language_code in TARGET_LANGUAGE_CODES
            if language_code != SOURCE_LANGUAGE_CODE
        }

        mode = prompt_mode()
        if mode == MODE_UPDATE_EXISTING:
            plans = build_update_file_plans(current_source, target_paths)
            existing_file_count = sum(1 for plan in plans if not plan.skipped)
            skipped_count = sum(1 for plan in plans if plan.skipped)
            overview_lines = [
                f"Source: {SOURCE_FILE_PATH.name} | Source keys: {len(current_source):,}",
                f"Model: {OPENROUTER_MODEL} | Batch size: {BATCH_SIZE:,} | API key source: {api_key_source}",
                f"Run stats: existing files {existing_file_count} | skipped missing files {skipped_count}",
            ]
            tracker = ProgressTracker("update-existing", plans, overview_lines)
            tracker.set_action("Refreshing all keys for existing localization files", "Starting translation work")
            results = process_file_plans(plans, tracker, openrouter_api_key)
            tracker.finish_run("All update-mode files finished")
            print_summary(results, tracker)
            return 0

        revision = prompt_commit_reference()
        old_source = load_source_json_from_git(revision)
        diff = compute_diff(old_source, current_source)

        temp_diff_path = create_temp_diff_file(diff.changed_entries)
        changed_entries = load_json_file(temp_diff_path)

        try:
            plans = build_regenerate_file_plans(current_source, changed_entries, diff, target_paths)
            existing_file_count = sum(1 for plan in plans if plan.existing_entries is not None)
            new_file_count = sum(1 for plan in plans if not plan.skipped and plan.existing_entries is None)
            overview_lines = [
                f"Source: {SOURCE_FILE_PATH.name} | Revision diff vs {revision}",
                (
                    f"Model: {OPENROUTER_MODEL} | Batch size: {BATCH_SIZE:,} | "
                    f"Temp diff: {temp_diff_path.name} | API key source: {api_key_source}"
                ),
                (
                    "Run stats: "
                    f"english added {len(diff.added_entries):,} | "
                    f"english edited {len(diff.edited_entries):,} | "
                    f"english removed {len(diff.removed_keys):,} | "
                    f"existing files {existing_file_count} | new files {new_file_count}"
                ),
            ]
            tracker = ProgressTracker("regenerate", plans, overview_lines)
            tracker.set_action("Preparing regenerate-mode translation run", f"Temp diff file: {temp_diff_path.name}")
            results = process_file_plans(plans, tracker, openrouter_api_key)
        finally:
            if temp_diff_path.exists():
                temp_diff_path.unlink()

        tracker.finish_run("All regenerate-mode files finished")
        print_summary(results, tracker)
        return 0
    except KeyboardInterrupt:
        print("\nOperation cancelled.")
        return 1
    except ScriptError as exc:
        print(f"Error: {exc}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
