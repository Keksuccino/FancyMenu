#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from collections import OrderedDict
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

SYSTEM_PROMPT = """You are a professional Minecraft mod localization translator. You translate Minecraft-style localization JSONs from English to the target language. You only translate the value, never the translation keys. You never remove or add lines. You translate every line of the JSON to the target language and return back the translated version of the received JSON. Make sure you return ONLY THE TRANSLATED JSON as valid JSON, no other text. You translate mod localizations to natural sounding text in the target language, which means you sometimes swap words for better fitting ones in the target language, instead of translating directly. You also make sure to use proper gaming and Minecraft slang when translating, which means that when the target language commonly uses terms for specific words that are not the perfect direct translation, but would work best, then you will use this term, to make the translation sound more natural and high-quality."""
USER_PROMPT_TEMPLATE = """Please translate the following localization to {target_language_code}. Return ONLY THE TRANSLATED JSON, no other text! Here is the JSON:

{json_localization_line_batch}"""

MODE_UPDATE_EXISTING = "1"
MODE_REGENERATE = "2"

SCRIPT_DIR = Path(__file__).resolve().parent
RESOLVED_LANG_DIRECTORY = (SCRIPT_DIR / LANG_DIRECTORY).resolve()
SOURCE_FILE_PATH = RESOLVED_LANG_DIRECTORY / f"{SOURCE_LANGUAGE_CODE}.json"
SOURCE_FILE_REPO_PATH = SOURCE_FILE_PATH.relative_to(SCRIPT_DIR).as_posix()


class ScriptError(RuntimeError):
    pass


class DiffResult:
    def __init__(
        self,
        added_entries: OrderedDict[str, str],
        edited_entries: OrderedDict[str, str],
        removed_keys: list[str],
        changed_entries: OrderedDict[str, str],
    ) -> None:
        self.added_entries = added_entries
        self.edited_entries = edited_entries
        self.removed_keys = removed_keys
        self.changed_entries = changed_entries


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


def send_openrouter_request(request_body: dict[str, object]) -> dict[str, object]:
    encoded_body = json.dumps(request_body, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        OPENROUTER_API_URL,
        data=encoded_body,
        headers={
            "Authorization": f"Bearer {OPENROUTER_API_KEY}",
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
            return send_openrouter_request(fallback_body)
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
) -> OrderedDict[str, str]:
    expected_keys = list(batch_entries.keys())
    last_error: Exception | None = None

    for attempt in range(1, REQUEST_RETRY_COUNT + 1):
        print(
            f"  [{language_code}] Translating batch {batch_index}/{total_batches} "
            f"({len(batch_entries)} entries, attempt {attempt}/{REQUEST_RETRY_COUNT})..."
        )

        try:
            request_body = build_translation_request(language_code, batch_entries)
            response_json = send_openrouter_request(request_body)
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
                print(f"  [{language_code}] Batch {batch_index} failed: {exc}")
                print("  Retrying...")
                time.sleep(attempt * 2)

    assert last_error is not None
    raise ScriptError(
        f"Failed to translate {language_code} batch {batch_index}/{total_batches}.\n{last_error}"
    )


def translate_entries(language_code: str, entries: OrderedDict[str, str]) -> OrderedDict[str, str]:
    if not entries:
        return OrderedDict()

    batches = chunk_entries(entries, BATCH_SIZE)
    translated_entries: OrderedDict[str, str] = OrderedDict()

    for batch_index, batch_entries in enumerate(batches, start=1):
        translated_entries.update(
            translate_batch(language_code, batch_entries, batch_index, len(batches))
        )
    return translated_entries


def merge_existing_translation(
    existing_entries: OrderedDict[str, str],
    translated_entries: OrderedDict[str, str],
    removed_keys: list[str],
) -> OrderedDict[str, str]:
    merged_entries = OrderedDict(existing_entries)

    for key in removed_keys:
        merged_entries.pop(key, None)

    for key, value in translated_entries.items():
        merged_entries[key] = value

    return merged_entries


def validate_configuration() -> None:
    if OPENROUTER_API_KEY == "<openrouter_api_key_here>":
        raise ScriptError(
            "Set OPENROUTER_API_KEY at the top of the script before running it."
        )

    if BATCH_SIZE <= 0:
        raise ScriptError("BATCH_SIZE must be greater than 0.")

    if not RESOLVED_LANG_DIRECTORY.is_dir():
        raise ScriptError(f"Language directory does not exist: {RESOLVED_LANG_DIRECTORY}")

    if not SOURCE_FILE_PATH.is_file():
        raise ScriptError(f"Main localization file does not exist: {SOURCE_FILE_PATH}")


def process_existing_only(
    current_source: OrderedDict[str, str],
    target_paths: dict[str, Path],
) -> dict[str, str]:
    results: dict[str, str] = {}

    for language_code, target_path in target_paths.items():
        if not target_path.exists():
            print(f"[{language_code}] Skipping because the localization file does not exist.")
            results[language_code] = "skipped (missing file)"
            continue

        print(f"[{language_code}] Loading existing localization file...")
        existing_entries = load_json_file(target_path)
        removed_keys = [key for key in existing_entries if key not in current_source]
        translated_entries = translate_entries(language_code, current_source)
        merged_entries = merge_existing_translation(existing_entries, translated_entries, removed_keys)
        write_json_file(target_path, merged_entries)

        print(
            f"[{language_code}] Updated {len(translated_entries)} translated keys"
            f" and removed {len(removed_keys)} obsolete keys."
        )
        results[language_code] = "updated"

    return results


def process_regenerate(
    current_source: OrderedDict[str, str],
    target_paths: dict[str, Path],
    changed_entries: OrderedDict[str, str],
    removed_keys: list[str],
) -> dict[str, str]:
    results: dict[str, str] = {}

    for language_code, target_path in target_paths.items():
        if target_path.exists():
            print(f"[{language_code}] Patching existing localization file...")
            existing_entries = load_json_file(target_path)
            translated_entries = translate_entries(language_code, changed_entries)
            merged_entries = merge_existing_translation(existing_entries, translated_entries, removed_keys)
            write_json_file(target_path, merged_entries)

            print(
                f"[{language_code}] Patched {len(translated_entries)} changed keys"
                f" and removed {len(removed_keys)} obsolete keys."
            )
            results[language_code] = "patched existing file"
            continue

        print(f"[{language_code}] Creating a new localization file from the full source...")
        translated_entries = translate_entries(language_code, current_source)
        write_json_file(target_path, translated_entries)

        print(f"[{language_code}] Created new file with {len(translated_entries)} translated keys.")
        results[language_code] = "created new file"

    return results


def print_summary(results: dict[str, str]) -> None:
    print()
    print("Summary:")
    for language_code in TARGET_LANGUAGE_CODES:
        result = results.get(language_code)
        if result is None:
            continue
        print(f"  {language_code}: {result}")


def main() -> int:
    try:
        validate_configuration()
        current_source = load_json_file(SOURCE_FILE_PATH)
        target_paths = {
            language_code: RESOLVED_LANG_DIRECTORY / f"{language_code}.json"
            for language_code in TARGET_LANGUAGE_CODES
            if language_code != SOURCE_LANGUAGE_CODE
        }

        mode = prompt_mode()
        if mode == MODE_UPDATE_EXISTING:
            print()
            print("Refreshing all keys for existing localization files...")
            results = process_existing_only(current_source, target_paths)
            print_summary(results)
            return 0

        revision = prompt_commit_reference()
        old_source = load_source_json_from_git(revision)
        diff = compute_diff(old_source, current_source)

        print()
        print(
            f"English diff summary compared to {revision}: "
            f"{len(diff.added_entries)} added, "
            f"{len(diff.edited_entries)} edited, "
            f"{len(diff.removed_keys)} removed."
        )

        temp_diff_path = create_temp_diff_file(diff.changed_entries)
        print(f"Temporary diff localization file: {temp_diff_path}")

        try:
            results = process_regenerate(
                current_source=current_source,
                target_paths=target_paths,
                changed_entries=load_json_file(temp_diff_path),
                removed_keys=diff.removed_keys,
            )
        finally:
            if temp_diff_path.exists():
                temp_diff_path.unlink()
                print(f"Deleted temporary diff localization file: {temp_diff_path}")

        print_summary(results)
        return 0
    except KeyboardInterrupt:
        print("\nOperation cancelled.")
        return 1
    except ScriptError as exc:
        print(f"Error: {exc}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
