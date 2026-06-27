#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Sequence, Tuple
from urllib import error, request


SCRIPT_VERSION = "1.0.0"
CONFIG_FILE_NAME = "mod_upload_config.json"
USER_AGENT = f"minecraft-mod-upload-script/{SCRIPT_VERSION} (local project upload tool)"

MODRINTH_API_BASE = "https://api.modrinth.com/v2"
CURSEFORGE_MINECRAFT_API_BASE = "https://minecraft.curseforge.com/api"

KEYCHAIN_SERVICES = {
    "modrinth": "modrinth.token",
    "curseforge": "curseforge.token",
}

REQUIRED_GRADLE_PROPERTIES = ("mod_id", "mod_version", "minecraft_version")
SUPPORTED_SIDES = ("client", "server")
IGNORED_JAR_SUFFIXES = ("-source.jar", "-sources.jar", "-unshaded.jar", "-plain.jar")
VERSIONED_JAR_PATTERN = re.compile(r"-\d+(?:\.\d+)+\.jar$")
MAIN_UPLOAD_TARGET = "main"
EARLYWINDOW_MODULE = "earlywindow"


@dataclass(frozen=True)
class LoaderInfo:
    module: str
    fancy_name: str
    modrinth_name: str
    curseforge_name: str


LOADER_INFOS: Dict[str, LoaderInfo] = {
    "fabric": LoaderInfo("fabric", "Fabric", "fabric", "Fabric"),
    "forge": LoaderInfo("forge", "Forge", "forge", "Forge"),
    "neoforge": LoaderInfo("neoforge", "NeoForge", "neoforge", "NeoForge"),
    "earlywindow": LoaderInfo("earlywindow", "NeoForge", "neoforge", "NeoForge"),
    "quilt": LoaderInfo("quilt", "Quilt", "quilt", "Quilt"),
}


@dataclass(frozen=True)
class SelectedJar:
    source_path: Path
    reason: str


@dataclass(frozen=True)
class StagedArtifact:
    loader: str
    upload_target: str
    source_path: Path
    staged_path: Path
    file_name: str
    display_name: str
    modrinth_version_number: str
    dependencies: Tuple[str, ...]


@dataclass(frozen=True)
class ModrinthTargets:
    project: Dict[str, Any]
    project_id: str
    dependency_ids_by_slug: Dict[str, str]


@dataclass(frozen=True)
class CurseForgeTags:
    minecraft_version_id: int
    minecraft_version_name: str
    loader_ids_by_module: Dict[str, int]
    environment_ids_by_side: Dict[str, int]


@dataclass(frozen=True)
class JavaRuntime:
    version: str
    source: str
    java_home: Optional[Path] = None


class UploadError(Exception):
    pass


class ApiError(UploadError):
    pass


class Console:
    STYLES = {
        "reset": "0",
        "bold": "1",
        "dim": "2",
        "red": "31",
        "green": "32",
        "yellow": "33",
        "blue": "34",
        "magenta": "35",
        "cyan": "36",
        "white": "37",
        "muted": "90",
        "bright_red": "91",
        "bright_green": "92",
        "bright_yellow": "93",
        "bright_blue": "94",
        "bright_magenta": "95",
        "bright_cyan": "96",
    }

    THEME = {
        "section": ("bold", "bright_cyan"),
        "subsection": ("bold", "cyan"),
        "key": ("muted",),
        "value": ("bold", "white"),
        "muted": ("muted",),
        "path": ("muted",),
        "file": ("bold", "bright_blue"),
        "id": ("bold", "bright_magenta"),
        "version": ("bold", "bright_green"),
        "loader": ("bold", "bright_cyan"),
        "platform": ("bold", "bright_blue"),
        "prompt": ("bold", "bright_cyan"),
        "success": ("bold", "bright_green"),
        "warning": ("bold", "bright_yellow"),
        "error": ("bold", "bright_red"),
        "command": ("muted",),
        "upload": ("bold", "bright_yellow"),
        "dry_run": ("bold", "bright_green"),
    }

    def __init__(self, color_mode: str = "auto") -> None:
        self.color_mode = color_mode
        self.stdout_color = self._should_color(sys.stdout)
        self.stderr_color = self._should_color(sys.stderr)

    def _should_color(self, stream: Any) -> bool:
        if self.color_mode == "always":
            return True
        if self.color_mode == "never":
            return False
        if os.environ.get("NO_COLOR"):
            return False
        if os.environ.get("CLICOLOR_FORCE"):
            return True
        return bool(getattr(stream, "isatty", lambda: False)()) and os.environ.get("TERM") != "dumb"

    def style(self, text: Any, *style_names: str, stderr: bool = False) -> str:
        value = str(text)
        if not (self.stderr_color if stderr else self.stdout_color):
            return value

        codes: List[str] = []
        for style_name in style_names:
            for token in self.THEME.get(style_name, (style_name,)):
                code = self.STYLES.get(token)
                if code:
                    codes.append(code)
        if not codes:
            return value
        return f"\033[{';'.join(codes)}m{value}\033[0m"

    def section(self, title: str) -> None:
        width = min(max(shutil.get_terminal_size((88, 20)).columns, 64), 110)
        label = f"== {title} "
        suffix = "=" * max(2, width - len(label))
        print("\n" + self.style(label + suffix, "section"), flush=True)

    def subsection(self, title: str) -> None:
        print()
        print(self.style(title, "subsection"))
        print(self.style("-" * len(title), "muted"))

    def key_value(
        self,
        label: str,
        value: Any,
        *,
        value_style: str = "value",
        indent: int = 0,
    ) -> None:
        prefix = " " * indent
        print(
            f"{prefix}{self.style(label + ':', 'key')} "
            f"{self.style(value, value_style)}"
        )

    def bullet(
        self,
        text: str,
        *,
        marker: str = "-",
        style_name: str = "value",
        indent: int = 0,
    ) -> None:
        prefix = " " * indent
        print(
            f"{prefix}{self.style(marker, 'muted')} "
            f"{self.style(text, style_name)}"
        )

    def status(self, label: str, message: str, style_name: str = "value") -> None:
        print(
            f"{self.style('[' + label + ']', style_name)} "
            f"{message}"
        )

    def success(self, message: str) -> None:
        self.status("OK", message, "success")

    def warning(self, message: str) -> None:
        self.status("WARN", message, "warning")

    def error(self, message: str) -> None:
        print(
            f"{self.style('[ERROR]', 'error', stderr=True)} {message}",
            file=sys.stderr,
        )

    def prompt(self, prompt: str) -> str:
        return self.style(prompt, "prompt")

    def command(self, command: Sequence[str]) -> None:
        print(self.style("+ " + shlex.join(command), "command"), flush=True)

    def blank(self) -> None:
        print()


CONSOLE = Console()


def configure_console(color_mode: str) -> None:
    global CONSOLE
    CONSOLE = Console(color_mode)


def eprint(message: str = "") -> None:
    if message:
        CONSOLE.error(message)
    else:
        print(file=sys.stderr)


def section(title: str) -> None:
    CONSOLE.section(title)


def ordered_sides(sides: Iterable[str]) -> List[str]:
    side_set = set(sides)
    return [side for side in SUPPORTED_SIDES if side in side_set]


def parse_side_choice(value: str) -> List[str]:
    normalized = value.strip().lower()
    if normalized == "both":
        return list(SUPPORTED_SIDES)
    if normalized == "none":
        return []
    if normalized in SUPPORTED_SIDES:
        return [normalized]
    raise ValueError(value)


def prompt_line(prompt: str, *, allow_empty: bool = False) -> str:
    if not sys.stdin.isatty():
        raise UploadError(
            "Missing configuration requires interactive input, but stdin is not a terminal."
        )

    while True:
        value = input(CONSOLE.prompt(prompt)).strip()
        if value or allow_empty:
            return value
        CONSOLE.warning("Please enter a value.")


def prompt_choice(prompt: str, choices: Sequence[str]) -> str:
    choices_text = "/".join(choices)
    while True:
        value = prompt_line(f"{prompt} [{choices_text}]: ").strip().lower()
        if value in choices:
            return value
        CONSOLE.warning(f"Please choose one of: {choices_text}")


def prompt_http_url(prompt: str) -> str:
    while True:
        value = prompt_line(prompt)
        if value.startswith("http://") or value.startswith("https://"):
            return value
        CONSOLE.warning("Please enter a full http:// or https:// URL.")


def prompt_numeric_id(prompt: str) -> str:
    while True:
        value = prompt_line(prompt)
        if value.isdigit():
            return value
        CONSOLE.warning("Please enter the numeric CurseForge project ID.")


def parse_gradle_properties(path: Path) -> Dict[str, str]:
    if not path.is_file():
        raise UploadError(f"Missing required file: {path}")

    properties: Dict[str, str] = {}
    with path.open("r", encoding="utf-8-sig") as handle:
        for line_number, raw_line in enumerate(handle, start=1):
            line = raw_line.strip()
            if not line or line.startswith("#") or line.startswith("!"):
                continue
            if "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip()
            if not key:
                raise UploadError(f"Invalid empty key in {path} at line {line_number}")
            properties[key] = value
    return properties


def require_gradle_properties(properties: Dict[str, str]) -> None:
    missing = [key for key in REQUIRED_GRADLE_PROPERTIES if not properties.get(key)]
    if missing:
        raise UploadError(
            "Missing required gradle.properties values: " + ", ".join(missing)
        )


def parse_included_modules(project_root: Path) -> List[str]:
    settings_paths = (
        project_root / "settings.gradle",
        project_root / "settings.gradle.kts",
    )
    settings_path = next((path for path in settings_paths if path.is_file()), None)
    if settings_path is None:
        return []

    text = settings_path.read_text(encoding="utf-8-sig")
    modules: List[str] = []
    seen = set()
    for match in re.finditer(r"include\s*\(?([^\n)]*)\)?", text):
        include_body = match.group(1)
        for quoted in re.findall(r"['\"]:?(.*?)['\"]", include_body):
            module = quoted.split(":")[-1].strip()
            if module and module not in seen:
                seen.add(module)
                modules.append(module)
    return modules


def discover_loader_modules(project_root: Path) -> List[str]:
    if not (project_root / "common").is_dir():
        raise UploadError(f"Expected a common module folder at {project_root / 'common'}")

    folder_modules = [
        module
        for module in LOADER_INFOS
        if (project_root / module).is_dir()
    ]
    included_modules = parse_included_modules(project_root)
    if included_modules:
        included_set = set(included_modules)
        modules = [module for module in folder_modules if module in included_set]
    else:
        modules = folder_modules

    if not modules:
        raise UploadError(
            "No supported loader modules found. Expected at least one of: "
            + ", ".join(LOADER_INFOS)
        )
    return modules


def load_config(config_path: Path) -> Dict[str, Any]:
    if not config_path.exists():
        return {"config_version": 1, "projects": {}}

    try:
        with config_path.open("r", encoding="utf-8") as handle:
            config = json.load(handle)
    except json.JSONDecodeError as exc:
        raise UploadError(f"Invalid JSON in {config_path}: {exc}") from exc

    if not isinstance(config, dict):
        raise UploadError(f"Config file must contain a JSON object: {config_path}")
    if "projects" not in config:
        config["projects"] = {}
    if not isinstance(config["projects"], dict):
        raise UploadError(f"Config field 'projects' must be an object: {config_path}")
    if "config_version" not in config:
        config["config_version"] = 1
    return config


def save_config(config_path: Path, config: Dict[str, Any]) -> None:
    config_path.parent.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(config, indent=2, sort_keys=True) + "\n"
    with tempfile.NamedTemporaryFile(
        "w",
        encoding="utf-8",
        dir=str(config_path.parent),
        prefix=f".{config_path.name}.",
        delete=False,
    ) as handle:
        temporary_path = Path(handle.name)
        handle.write(serialized)

    os.chmod(temporary_path, 0o600)
    os.replace(temporary_path, config_path)


def normalize_slug_list(raw: Any) -> List[str]:
    if raw is None:
        return []
    if isinstance(raw, str):
        pieces = raw.split(",")
    elif isinstance(raw, list):
        pieces = [str(item) for item in raw]
    else:
        raise UploadError("Dependency configuration must be a list or comma-separated string.")

    normalized: List[str] = []
    seen = set()
    for piece in pieces:
        slug = piece.strip().lower()
        if not slug or slug in seen:
            continue
        seen.add(slug)
        normalized.append(slug)
    return normalized


def normalize_dependencies_for_loader(loader: str, raw_dependencies: Any) -> List[str]:
    dependencies = normalize_slug_list(raw_dependencies)
    if loader == "fabric" and "fabric-api" not in dependencies:
        dependencies.insert(0, "fabric-api")
    return dependencies


def normalize_java_version(raw: Any, source: str) -> str:
    if isinstance(raw, (int, float)):
        value = str(raw)
    elif isinstance(raw, str):
        value = raw
    else:
        raise UploadError(f"{source} java_version must be a string or number.")

    version = value.strip()
    if not version:
        raise UploadError(f"{source} java_version must not be empty.")
    if any(character.isspace() for character in version):
        raise UploadError(f"{source} java_version must not contain whitespace.")
    return version


def ensure_java_version(
    properties: Dict[str, str],
    project_config: Dict[str, Any],
) -> Tuple[JavaRuntime, bool]:
    if properties.get("java_version"):
        return (
            JavaRuntime(
                version=normalize_java_version(
                    properties["java_version"],
                    "gradle.properties",
                ),
                source="gradle.properties",
            ),
            False,
        )

    if project_config.get("java_version"):
        return (
            JavaRuntime(
                version=normalize_java_version(
                    project_config["java_version"],
                    "config",
                ),
                source="saved config",
            ),
            False,
        )

    section("Java Runtime")
    java_version = normalize_java_version(
        prompt_line("Java version for Gradle builds (for example 17, 21, 25): "),
        "prompted",
    )
    project_config["java_version"] = java_version
    return JavaRuntime(version=java_version, source="saved config"), True


def upload_target_key_for_loader(loader: str) -> str:
    if loader == EARLYWINDOW_MODULE:
        return EARLYWINDOW_MODULE
    return MAIN_UPLOAD_TARGET


def upload_target_keys_for_loaders(loaders: Iterable[str]) -> List[str]:
    keys: List[str] = []
    seen = set()
    for loader in loaders:
        key = upload_target_key_for_loader(loader)
        if key not in seen:
            seen.add(key)
            keys.append(key)
    return keys


def upload_target_label(target_key: str) -> str:
    if target_key == MAIN_UPLOAD_TARGET:
        return "Main Mod"
    if target_key == EARLYWINDOW_MODULE:
        return "EarlyWindow"
    return target_key[:1].upper() + target_key[1:]


def upload_target_message_label(target_key: str) -> str:
    if target_key == MAIN_UPLOAD_TARGET:
        return "Main project"
    if target_key == EARLYWINDOW_MODULE:
        return "EarlyWindow project"
    return f"{upload_target_label(target_key)} project"


def get_upload_target_config(
    project_config: Dict[str, Any],
    target_key: str,
) -> Dict[str, Any]:
    if target_key == MAIN_UPLOAD_TARGET:
        return project_config

    project_targets = project_config.get("project_targets", {})
    if not isinstance(project_targets, dict):
        raise UploadError("Config field 'project_targets' must be an object.")

    target_config = project_targets.get(target_key)
    if not isinstance(target_config, dict):
        raise UploadError(
            f"Missing config for upload target '{target_key}'. "
            "Run the script interactively to create it."
        )
    return target_config


def ensure_upload_target_config(
    project_config: Dict[str, Any],
    target_key: str,
    *,
    show_target_label: bool,
) -> bool:
    changed = False
    if target_key == MAIN_UPLOAD_TARGET:
        target_config = project_config
    else:
        project_targets = project_config.setdefault("project_targets", {})
        if not isinstance(project_targets, dict):
            raise UploadError("Config field 'project_targets' must be an object.")
        target_config = project_targets.setdefault(target_key, {})
        if not isinstance(target_config, dict):
            raise UploadError(
                f"Config for upload target '{target_key}' must be an object."
            )

    def target_section(platform: str) -> str:
        if show_target_label or target_key != MAIN_UPLOAD_TARGET:
            return f"{upload_target_label(target_key)} {platform} Target"
        return f"{platform} Target"

    if not target_config.get("modrinth_project_id"):
        section(target_section("Modrinth"))
        target_config["modrinth_project_id"] = prompt_line(
            "Modrinth project ID or slug: "
        )
        changed = True

    if not target_config.get("curseforge_project_id"):
        section(target_section("CurseForge"))
        target_config["curseforge_project_id"] = prompt_numeric_id(
            "CurseForge numeric project ID: "
        )
        changed = True

    return changed


def ensure_project_config(
    config: Dict[str, Any],
    project_key: str,
    loaders: Sequence[str],
    *,
    reset_project_config: bool,
) -> Tuple[Dict[str, Any], bool]:
    projects = config.setdefault("projects", {})
    if not isinstance(projects, dict):
        raise UploadError("Config field 'projects' must be an object.")

    if reset_project_config and project_key in projects:
        del projects[project_key]

    project_config = projects.setdefault(project_key, {})
    if not isinstance(project_config, dict):
        raise UploadError(f"Config for project key '{project_key}' must be an object.")

    changed = False

    upload_target_keys = upload_target_keys_for_loaders(loaders)
    show_target_label = len(upload_target_keys) > 1
    for target_key in upload_target_keys:
        changed = (
            ensure_upload_target_config(
                project_config,
                target_key,
                show_target_label=show_target_label,
            )
            or changed
        )

    if not project_config.get("supported_environments"):
        section("Supported Environments")
        choice = prompt_choice(
            "Where does this mod work?",
            ("client", "server", "both"),
        )
        project_config["supported_environments"] = ordered_sides(parse_side_choice(choice))
        changed = True

    supported = set(project_config.get("supported_environments", []))
    if not supported or not supported.issubset(set(SUPPORTED_SIDES)):
        raise UploadError(
            "Config field 'supported_environments' must contain client, server, or both."
        )

    if "mandatory_environments" not in project_config:
        section("Mandatory Environments")
        while True:
            choice = prompt_choice(
                "Where is this mod mandatory?",
                ("none", "client", "server", "both"),
            )
            mandatory = set(parse_side_choice(choice))
            if mandatory.issubset(supported):
                project_config["mandatory_environments"] = ordered_sides(mandatory)
                changed = True
                break
            supported_text = "/".join(ordered_sides(supported))
            CONSOLE.warning(
                f"Mandatory environments must be within supported environments: {supported_text}"
            )

    mandatory = set(project_config.get("mandatory_environments", []))
    if not mandatory.issubset(supported):
        raise UploadError(
            "Config field 'mandatory_environments' must be a subset of supported_environments."
        )

    if not project_config.get("changelog_url"):
        section("Changelog")
        project_config["changelog_url"] = prompt_http_url("Stable changelog URL: ")
        changed = True

    dependencies_by_loader = project_config.setdefault("dependencies", {})
    if not isinstance(dependencies_by_loader, dict):
        raise UploadError("Config field 'dependencies' must be an object.")

    for loader in loaders:
        if loader not in dependencies_by_loader:
            section(f"{module_display_name(loader)} Dependencies")
            raw = prompt_line(
                "Mandatory dependency slugs, comma-separated. Leave empty for none: ",
                allow_empty=True,
            )
            dependencies_by_loader[loader] = normalize_dependencies_for_loader(loader, raw)
            changed = True
        else:
            normalized = normalize_dependencies_for_loader(loader, dependencies_by_loader[loader])
            if normalized != dependencies_by_loader[loader]:
                dependencies_by_loader[loader] = normalized
                changed = True

    return project_config, changed


def loader_fancy_name(loader: str) -> str:
    info = LOADER_INFOS.get(loader)
    if info:
        return info.fancy_name
    return loader[:1].upper() + loader[1:]


def module_display_name(loader: str) -> str:
    if loader == EARLYWINDOW_MODULE:
        return "EarlyWindow (NeoForge)"
    return loader_fancy_name(loader)


def upload_loader_name(loader: str) -> str:
    info = LOADER_INFOS.get(loader)
    if info:
        return info.modrinth_name
    return loader


def staged_file_name(
    loader: str,
    *,
    mod_id: str,
    mod_version: str,
    minecraft_version: str,
) -> str:
    safe_mod_id = safe_file_component(mod_id)
    loader_name = safe_file_component(upload_loader_name(loader))
    if loader == EARLYWINDOW_MODULE:
        safe_mod_id = f"{safe_mod_id}-earlywindow"

    return (
        f"{safe_mod_id}_"
        f"{loader_name}_"
        f"{safe_file_component(mod_version)}_MC_"
        f"{safe_file_component(minecraft_version)}.jar"
    )


def keychain_token(service_name: str) -> Optional[str]:
    security_binary = Path("/usr/bin/security")
    if not security_binary.exists():
        raise UploadError("macOS security command not found at /usr/bin/security.")

    result = subprocess.run(
        [str(security_binary), "find-generic-password", "-s", service_name, "-w"],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if result.returncode != 0:
        return None

    token = result.stdout.strip()
    return token or None


def load_tokens_from_keychain() -> Dict[str, str]:
    missing: List[str] = []
    tokens: Dict[str, str] = {}

    for platform, service_name in KEYCHAIN_SERVICES.items():
        token = keychain_token(service_name)
        if not token:
            missing.append(service_name)
        else:
            tokens[platform] = token

    if missing:
        raise UploadError(
            "Could not find required token(s) in the macOS Keychain: "
            + ", ".join(missing)
            + ". Add them to the keychain and run this script again."
        )
    return tokens


def gradle_command(project_root: Path) -> List[str]:
    wrapper = project_root / "gradlew"
    if wrapper.is_file():
        if os.access(wrapper, os.X_OK):
            return [str(wrapper)]
        return ["sh", str(wrapper)]
    return ["gradle"]


def resolve_java_home(java_version: str) -> Path:
    java_home_binary = Path("/usr/libexec/java_home")
    if not java_home_binary.is_file():
        raise UploadError(
            "Could not find /usr/libexec/java_home for resolving the requested JDK."
        )

    result = subprocess.run(
        [str(java_home_binary), "-v", java_version],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if result.returncode != 0:
        details = (result.stderr or result.stdout).strip()
        message = (
            f"Could not find an installed JDK for Java {java_version} "
            "via /usr/libexec/java_home."
        )
        if details:
            message += f" {details}"
        raise UploadError(message)

    java_home = Path(result.stdout.strip())
    java_binary = java_home / "bin" / "java"
    if not java_binary.is_file():
        raise UploadError(
            f"Resolved Java {java_version} to {java_home}, but bin/java was not found."
        )
    return java_home


def resolve_java_runtime(java_runtime: JavaRuntime) -> JavaRuntime:
    return JavaRuntime(
        version=java_runtime.version,
        source=java_runtime.source,
        java_home=resolve_java_home(java_runtime.version),
    )


def java_subprocess_env(java_runtime: JavaRuntime) -> Dict[str, str]:
    if java_runtime.java_home is None:
        raise UploadError("Internal error: Gradle build requested without resolved JAVA_HOME.")

    env = os.environ.copy()
    java_home = str(java_runtime.java_home)
    java_bin = str(java_runtime.java_home / "bin")
    env["JAVA_HOME"] = java_home
    env["PATH"] = java_bin + os.pathsep + env.get("PATH", "")
    return env


def build_loader_modules(
    project_root: Path,
    loaders: Sequence[str],
    java_runtime: JavaRuntime,
) -> None:
    section("Gradle Build")
    CONSOLE.key_value(
        "Java version",
        f"{java_runtime.version} ({java_runtime.source})",
        value_style="version",
    )
    CONSOLE.key_value("JAVA_HOME", java_runtime.java_home, value_style="path")
    command_prefix = gradle_command(project_root)
    env = java_subprocess_env(java_runtime)
    for loader in loaders:
        task = f":{loader}:build"
        command = command_prefix + [task]
        CONSOLE.command(command)
        result = subprocess.run(command, cwd=str(project_root), env=env)
        if result.returncode != 0:
            raise UploadError(
                f"Build failed for module '{loader}' with exit code {result.returncode}."
            )


def format_size(byte_count: int) -> str:
    units = ("B", "KiB", "MiB", "GiB")
    value = float(byte_count)
    for unit in units:
        if value < 1024.0 or unit == units[-1]:
            if unit == "B":
                return f"{int(value)} {unit}"
            return f"{value:.1f} {unit}"
        value /= 1024.0
    return f"{byte_count} B"


def choose_largest(paths: Sequence[Path]) -> Path:
    return max(paths, key=lambda path: (path.stat().st_size, path.name))


def confirm_ambiguous_jar(
    loader: str,
    candidates: Sequence[Path],
    selected: Path,
    *,
    assume_yes: bool,
) -> None:
    CONSOLE.blank()
    CONSOLE.warning(f"Could not confidently identify the {loader} upload JAR.")
    print(CONSOLE.style("Remaining candidates:", "key"))
    for candidate in candidates:
        size = format_size(candidate.stat().st_size)
        CONSOLE.bullet(
            f"{candidate.name} ({size})",
            style_name="file",
            indent=2,
        )
    CONSOLE.key_value(
        "Selected largest candidate",
        selected.name,
        value_style="file",
    )

    if assume_yes:
        CONSOLE.success("Accepted because --yes was provided.")
        return

    if not sys.stdin.isatty():
        raise UploadError(
            f"Ambiguous {loader} JAR selection requires confirmation, but stdin is not a terminal."
        )

    answer = input(
        CONSOLE.prompt("Is this the correct file to upload? [y/N]: ")
    ).strip().lower()
    if answer not in ("y", "yes"):
        raise UploadError(f"Aborted because the {loader} upload JAR was not confirmed.")


def select_mod_jar(
    libs_dir: Path,
    loader: str,
    mod_version: str,
    *,
    assume_yes: bool,
) -> SelectedJar:
    if not libs_dir.is_dir():
        raise UploadError(f"Missing build output folder for {loader}: {libs_dir}")

    jars = sorted(path for path in libs_dir.iterdir() if path.is_file() and path.suffix == ".jar")
    candidates = [
        path for path in jars
        if not any(path.name.endswith(suffix) for suffix in IGNORED_JAR_SUFFIXES)
    ]

    if not candidates:
        raise UploadError(
            f"No uploadable JAR candidates found in {libs_dir}. "
            f"Ignored suffixes: {', '.join(IGNORED_JAR_SUFFIXES)}"
        )

    all_jars = [path for path in candidates if path.name.endswith("-all.jar")]
    if all_jars:
        selected = choose_largest(all_jars)
        return SelectedJar(selected, "found -all.jar candidate")

    if len(candidates) == 1:
        return SelectedJar(candidates[0], "only one uploadable JAR candidate remained")

    exact_mod_version_jars = [
        path for path in candidates
        if path.name.endswith(f"-{mod_version}.jar") and VERSIONED_JAR_PATTERN.search(path.name)
    ]
    if len(exact_mod_version_jars) == 1:
        return SelectedJar(
            exact_mod_version_jars[0],
            "found a version-suffixed JAR matching mod_version",
        )

    versioned_jars = [
        path for path in candidates
        if VERSIONED_JAR_PATTERN.search(path.name)
    ]
    if len(versioned_jars) == 1:
        return SelectedJar(versioned_jars[0], "found one numeric version-suffixed JAR")

    selected = choose_largest(candidates)
    confirm_ambiguous_jar(loader, candidates, selected, assume_yes=assume_yes)
    return SelectedJar(selected, "largest candidate accepted after confirmation")


def safe_file_component(value: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9._-]+", "_", value.strip())
    cleaned = cleaned.strip("._-")
    if not cleaned:
        raise UploadError(f"Could not create a safe file-name component from: {value!r}")
    return cleaned


def stage_artifacts(
    project_root: Path,
    loaders: Sequence[str],
    properties: Dict[str, str],
    project_config: Dict[str, Any],
    *,
    assume_yes: bool,
) -> List[StagedArtifact]:
    mod_id = properties["mod_id"]
    mod_version = properties["mod_version"]
    minecraft_version = properties["minecraft_version"]
    output_dir = project_root / "build" / "mod-upload"
    output_dir.mkdir(parents=True, exist_ok=True)

    artifacts: List[StagedArtifact] = []
    section("Upload JAR Selection")
    for loader in loaders:
        selected = select_mod_jar(
            project_root / loader / "build" / "libs",
            loader,
            mod_version,
            assume_yes=assume_yes,
        )

        file_name = staged_file_name(
            loader,
            mod_id=mod_id,
            mod_version=mod_version,
            minecraft_version=minecraft_version,
        )
        staged_path = output_dir / file_name
        shutil.copy2(selected.source_path, staged_path)

        dependencies = tuple(
            normalize_dependencies_for_loader(
                loader,
                project_config.get("dependencies", {}).get(loader, []),
            )
        )

        display_name = (
            f"[{loader_fancy_name(loader)}] "
            f"v{mod_version} MC {minecraft_version}"
        )
        modrinth_version_number = (
            f"{mod_version}-{minecraft_version}-{upload_loader_name(loader)}"
        )

        print(
            f"{CONSOLE.style(module_display_name(loader), 'loader')}: "
            f"{CONSOLE.style(selected.source_path.name, 'file')} "
            f"{CONSOLE.style('->', 'muted')} "
            f"{CONSOLE.style(file_name, 'file')} "
            f"{CONSOLE.style('(' + selected.reason + ')', 'muted')}"
        )

        artifacts.append(
            StagedArtifact(
                loader=loader,
                upload_target=upload_target_key_for_loader(loader),
                source_path=selected.source_path,
                staged_path=staged_path,
                file_name=file_name,
                display_name=display_name,
                modrinth_version_number=modrinth_version_number,
                dependencies=dependencies,
            )
        )

    return artifacts


class HttpClient:
    def __init__(self, default_headers: Optional[Dict[str, str]] = None) -> None:
        self.default_headers = default_headers or {}

    def json_request(
        self,
        method: str,
        url: str,
        *,
        headers: Optional[Dict[str, str]] = None,
        body: Optional[Any] = None,
        expected_statuses: Sequence[int] = (200,),
        timeout: int = 60,
    ) -> Any:
        request_headers = dict(self.default_headers)
        if headers:
            request_headers.update(headers)

        data: Optional[bytes] = None
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            request_headers.setdefault("Content-Type", "application/json")

        response_body, status = self.raw_request(
            method,
            url,
            headers=request_headers,
            data=data,
            expected_statuses=expected_statuses,
            timeout=timeout,
        )
        if not response_body:
            return None
        try:
            return json.loads(response_body.decode("utf-8"))
        except json.JSONDecodeError as exc:
            raise ApiError(f"Expected JSON from {url}, got: {response_body[:500]!r}") from exc

    def raw_request(
        self,
        method: str,
        url: str,
        *,
        headers: Optional[Dict[str, str]] = None,
        data: Optional[bytes] = None,
        expected_statuses: Sequence[int] = (200,),
        timeout: int = 60,
    ) -> Tuple[bytes, int]:
        request_headers = dict(self.default_headers)
        if headers:
            request_headers.update(headers)

        http_request = request.Request(
            url,
            data=data,
            headers=request_headers,
            method=method,
        )

        try:
            with request.urlopen(http_request, timeout=timeout) as response:
                status = response.getcode()
                response_body = response.read()
        except error.HTTPError as exc:
            response_body = exc.read()
            message = response_body.decode("utf-8", errors="replace")
            raise ApiError(
                f"HTTP {exc.code} from {method} {url}: {message[:1000]}"
            ) from exc
        except error.URLError as exc:
            raise ApiError(f"Request failed for {method} {url}: {exc}") from exc

        if status not in expected_statuses:
            message = response_body.decode("utf-8", errors="replace")
            raise ApiError(f"Unexpected HTTP {status} from {method} {url}: {message[:1000]}")

        return response_body, status


def multipart_body(
    fields: Dict[str, str],
    files: Sequence[Tuple[str, Path, str, str]],
) -> Tuple[bytes, str]:
    boundary = f"----mod-upload-{uuid.uuid4().hex}"
    chunks: List[bytes] = []

    for name, value in fields.items():
        chunks.append(f"--{boundary}\r\n".encode("utf-8"))
        chunks.append(
            f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode("utf-8")
        )
        chunks.append(value.encode("utf-8"))
        chunks.append(b"\r\n")

    for field_name, path, filename, content_type in files:
        chunks.append(f"--{boundary}\r\n".encode("utf-8"))
        chunks.append(
            (
                f'Content-Disposition: form-data; name="{field_name}"; '
                f'filename="{filename}"\r\n'
            ).encode("utf-8")
        )
        chunks.append(f"Content-Type: {content_type}\r\n\r\n".encode("utf-8"))
        with path.open("rb") as handle:
            chunks.append(handle.read())
        chunks.append(b"\r\n")

    chunks.append(f"--{boundary}--\r\n".encode("utf-8"))
    return b"".join(chunks), boundary


class ModrinthClient:
    def __init__(self, token: str) -> None:
        self.http = HttpClient(
            {
                "Authorization": token,
                "User-Agent": USER_AGENT,
            }
        )

    def get_loaders(self) -> List[Dict[str, Any]]:
        return self.http.json_request("GET", f"{MODRINTH_API_BASE}/tag/loader")

    def get_game_versions(self) -> List[Dict[str, Any]]:
        return self.http.json_request("GET", f"{MODRINTH_API_BASE}/tag/game_version")

    def get_project(self, project_id_or_slug: str) -> Dict[str, Any]:
        return self.http.json_request(
            "GET",
            f"{MODRINTH_API_BASE}/project/{project_id_or_slug}",
        )

    def patch_project_environment(
        self,
        project_id_or_slug: str,
        *,
        client_side: str,
        server_side: str,
    ) -> None:
        self.http.json_request(
            "PATCH",
            f"{MODRINTH_API_BASE}/project/{project_id_or_slug}",
            body={
                "client_side": client_side,
                "server_side": server_side,
            },
            expected_statuses=(200, 204),
        )

    def create_version(self, metadata: Dict[str, Any], artifact: StagedArtifact) -> Any:
        body, boundary = multipart_body(
            {"data": json.dumps(metadata, separators=(",", ":"))},
            [("primary", artifact.staged_path, artifact.file_name, "application/java-archive")],
        )
        response_body, _ = self.http.raw_request(
            "POST",
            f"{MODRINTH_API_BASE}/version",
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
            data=body,
            expected_statuses=(200, 201),
            timeout=300,
        )
        return json.loads(response_body.decode("utf-8")) if response_body else None


class CurseForgeClient:
    def __init__(self, token: str) -> None:
        self.http = HttpClient(
            {
                "X-Api-Token": token,
                "User-Agent": USER_AGENT,
            }
        )
        self._version_types: Optional[List[Dict[str, Any]]] = None
        self._game_versions: Optional[List[Dict[str, Any]]] = None

    def get_version_types(self) -> List[Dict[str, Any]]:
        if self._version_types is None:
            self._version_types = self.http.json_request(
                "GET",
                f"{CURSEFORGE_MINECRAFT_API_BASE}/game/version-types",
            )
        return self._version_types

    def get_game_versions(self) -> List[Dict[str, Any]]:
        if self._game_versions is None:
            self._game_versions = self.http.json_request(
                "GET",
                f"{CURSEFORGE_MINECRAFT_API_BASE}/game/versions",
            )
        return self._game_versions

    def version_type_slugs_by_id(self) -> Dict[int, str]:
        return {
            int(version_type["id"]): str(version_type["slug"])
            for version_type in self.get_version_types()
        }

    def resolve_minecraft_version_id(self, minecraft_version: str) -> int:
        type_slugs = self.version_type_slugs_by_id()
        candidates = [
            item
            for item in self.get_game_versions()
            if str(item.get("name")) == minecraft_version
            and type_slugs.get(int(item.get("gameVersionTypeID", -1)), "").startswith("minecraft-")
        ]

        if not candidates:
            raise UploadError(
                f"CurseForge does not list Minecraft version '{minecraft_version}' "
                "as a Minecraft game-version tag."
            )

        candidates.sort(
            key=lambda item: (
                item.get("apiVersion") is not None,
                int(item.get("id", 0)),
            )
        )
        return int(candidates[0]["id"])

    def resolve_tag_id(self, name: str, expected_type_slug: str) -> int:
        type_slugs = self.version_type_slugs_by_id()
        candidates = [
            item
            for item in self.get_game_versions()
            if str(item.get("name")) == name
            and type_slugs.get(int(item.get("gameVersionTypeID", -1))) == expected_type_slug
        ]
        if not candidates:
            raise UploadError(
                f"CurseForge does not list required tag '{name}' "
                f"under version type '{expected_type_slug}'."
            )
        candidates.sort(key=lambda item: int(item.get("id", 0)))
        return int(candidates[0]["id"])

    def upload_file(
        self,
        project_id: str,
        metadata: Dict[str, Any],
        artifact: StagedArtifact,
    ) -> Any:
        body, boundary = multipart_body(
            {"metadata": json.dumps(metadata, separators=(",", ":"))},
            [("file", artifact.staged_path, artifact.file_name, "application/java-archive")],
        )
        response_body, _ = self.http.raw_request(
            "POST",
            f"{CURSEFORGE_MINECRAFT_API_BASE}/projects/{project_id}/upload-file",
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
            data=body,
            expected_statuses=(200, 201),
            timeout=300,
        )
        return json.loads(response_body.decode("utf-8")) if response_body else None


def modrinth_environment_values(project_config: Dict[str, Any]) -> Tuple[str, str]:
    supported = set(project_config["supported_environments"])
    mandatory = set(project_config.get("mandatory_environments", []))

    values = []
    for side in SUPPORTED_SIDES:
        if side not in supported:
            values.append("unsupported")
        elif side in mandatory:
            values.append("required")
        else:
            values.append("optional")
    return values[0], values[1]


def modrinth_version_environment(project_config: Dict[str, Any]) -> str:
    supported = set(project_config["supported_environments"])
    mandatory = set(project_config.get("mandatory_environments", []))

    client_supported = "client" in supported
    server_supported = "server" in supported
    client_required = "client" in mandatory
    server_required = "server" in mandatory

    if client_supported and server_supported:
        if client_required and server_required:
            return "client_and_server"
        if client_required:
            return "client_only_server_optional"
        if server_required:
            return "server_only_client_optional"
        return "client_or_server"
    if client_supported:
        return "client_only"
    if server_supported:
        return "server_only"
    return "unknown"


def require_modrinth_project_id(project: Dict[str, Any], lookup_name: str) -> str:
    project_id = project.get("id")
    if not isinstance(project_id, str) or not project_id:
        raise UploadError(
            f"Modrinth project lookup for '{lookup_name}' did not return a project ID."
        )
    return project_id


def validate_modrinth_targets(
    client: ModrinthClient,
    minecraft_version: str,
    project_config: Dict[str, Any],
    artifacts: Sequence[StagedArtifact],
) -> Dict[str, ModrinthTargets]:
    section("Modrinth Validation")

    loader_names = {
        str(loader.get("name"))
        for loader in client.get_loaders()
        if "mod" in (loader.get("supported_project_types") or [])
        or "project" in (loader.get("supported_project_types") or [])
    }
    game_versions = {
        str(version.get("version"))
        for version in client.get_game_versions()
    }
    if minecraft_version not in game_versions:
        raise UploadError(
            f"Modrinth does not list Minecraft version '{minecraft_version}'."
        )
    CONSOLE.key_value("Minecraft version", minecraft_version, value_style="version")

    target_keys = upload_target_keys_for_loaders(artifact.loader for artifact in artifacts)
    targets_by_key: Dict[str, ModrinthTargets] = {}
    dependency_project_cache: Dict[str, Tuple[str, str]] = {}

    for target_key in target_keys:
        target_artifacts = [
            artifact for artifact in artifacts
            if artifact.upload_target == target_key
        ]
        target_config = get_upload_target_config(project_config, target_key)
        project_id_or_slug = target_config["modrinth_project_id"]
        project = client.get_project(project_id_or_slug)
        project_id = require_modrinth_project_id(project, project_id_or_slug)
        project_title = project.get("title") or project.get("slug") or project_id_or_slug

        CONSOLE.subsection(f"{upload_target_label(target_key)} Project")
        CONSOLE.key_value("Project", project_title, value_style="platform", indent=2)
        CONSOLE.key_value("Project ID", project_id, value_style="id", indent=2)

        printed_loaders = set()
        for artifact in target_artifacts:
            modrinth_loader = LOADER_INFOS[artifact.loader].modrinth_name
            if modrinth_loader not in loader_names:
                raise UploadError(f"Modrinth does not list loader '{modrinth_loader}'.")
            loader_key = (artifact.loader, modrinth_loader)
            if loader_key in printed_loaders:
                continue
            printed_loaders.add(loader_key)
            CONSOLE.key_value(
                f"{module_display_name(artifact.loader)} loader",
                modrinth_loader,
                value_style="loader",
                indent=2,
            )

        dependency_ids_by_slug: Dict[str, str] = {}
        dependency_slugs = sorted(
            {
                dependency
                for artifact in target_artifacts
                for dependency in artifact.dependencies
            }
        )
        if dependency_slugs:
            print("  " + CONSOLE.style("Dependencies:", "key"))
        for dependency_slug in dependency_slugs:
            if dependency_slug in dependency_project_cache:
                dependency_id, dependency_title = dependency_project_cache[dependency_slug]
            else:
                try:
                    dependency_project = client.get_project(dependency_slug)
                except ApiError as exc:
                    raise UploadError(
                        "Could not resolve Modrinth dependency slug "
                        f"'{dependency_slug}' to a project ID."
                    ) from exc
                dependency_id = require_modrinth_project_id(
                    dependency_project,
                    dependency_slug,
                )
                dependency_title = (
                    dependency_project.get("title")
                    or dependency_project.get("slug")
                    or dependency_slug
                )
                dependency_project_cache[dependency_slug] = (
                    dependency_id,
                    str(dependency_title),
                )

            dependency_ids_by_slug[dependency_slug] = dependency_id
            print(
                "    "
                + CONSOLE.style(dependency_slug, "file")
                + CONSOLE.style(" -> ", "muted")
                + CONSOLE.style(dependency_title, "value")
                + CONSOLE.style(" (ID ", "muted")
                + CONSOLE.style(dependency_id, "id")
                + CONSOLE.style(")", "muted")
            )

        targets_by_key[target_key] = ModrinthTargets(
            project=project,
            project_id=project_id,
            dependency_ids_by_slug=dependency_ids_by_slug,
        )

    return targets_by_key


def resolve_curseforge_tags(
    client: CurseForgeClient,
    loaders: Sequence[str],
    minecraft_version: str,
    supported_sides: Sequence[str],
) -> CurseForgeTags:
    section("CurseForge Validation")
    minecraft_version_id = client.resolve_minecraft_version_id(minecraft_version)
    CONSOLE.key_value("Minecraft version", minecraft_version, value_style="version")
    CONSOLE.key_value("Minecraft version ID", minecraft_version_id, value_style="id", indent=2)

    loader_ids_by_module: Dict[str, int] = {}
    loader_ids_by_name: Dict[str, int] = {}
    for loader in loaders:
        curseforge_loader = LOADER_INFOS[loader].curseforge_name
        if curseforge_loader not in loader_ids_by_name:
            loader_ids_by_name[curseforge_loader] = client.resolve_tag_id(
                curseforge_loader,
                "modloader",
            )
        loader_id = loader_ids_by_name[curseforge_loader]
        loader_ids_by_module[loader] = loader_id
        print(
            "  "
            + CONSOLE.style(f"{module_display_name(loader)} loader:", "key")
            + " "
            + CONSOLE.style(curseforge_loader, "loader")
            + CONSOLE.style(" -> ID ", "muted")
            + CONSOLE.style(loader_id, "id")
        )

    environment_ids_by_side: Dict[str, int] = {}
    for side in supported_sides:
        tag_name = side[:1].upper() + side[1:]
        tag_id = client.resolve_tag_id(tag_name, "environment")
        environment_ids_by_side[side] = tag_id
        print(
            "  "
            + CONSOLE.style("Environment:", "key")
            + " "
            + CONSOLE.style(tag_name, "value")
            + CONSOLE.style(" -> ID ", "muted")
            + CONSOLE.style(tag_id, "id")
        )

    return CurseForgeTags(
        minecraft_version_id=minecraft_version_id,
        minecraft_version_name=minecraft_version,
        loader_ids_by_module=loader_ids_by_module,
        environment_ids_by_side=environment_ids_by_side,
    )


def modrinth_version_metadata(
    project_config: Dict[str, Any],
    properties: Dict[str, str],
    artifact: StagedArtifact,
    release_type: str,
    modrinth_project_id: str,
    modrinth_dependency_ids_by_slug: Dict[str, str],
) -> Dict[str, Any]:
    dependencies = []
    for dependency in artifact.dependencies:
        dependency_id = modrinth_dependency_ids_by_slug.get(dependency)
        if not dependency_id:
            raise UploadError(
                f"Missing resolved Modrinth project ID for dependency '{dependency}'."
            )
        dependencies.append(
            {
                "project_id": dependency_id,
                "version_id": None,
                "file_name": None,
                "dependency_type": "required",
            }
        )

    return {
        "project_id": modrinth_project_id,
        "name": artifact.display_name,
        "version_number": artifact.modrinth_version_number,
        "changelog": f"CHANGELOG: {project_config['changelog_url']}",
        "dependencies": dependencies,
        "game_versions": [properties["minecraft_version"]],
        "version_type": release_type,
        "loaders": [LOADER_INFOS[artifact.loader].modrinth_name],
        "featured": False,
        "status": "listed",
        "requested_status": None,
        "file_parts": ["primary"],
        "primary_file": "primary",
        "environment": modrinth_version_environment(project_config),
    }


def curseforge_file_metadata(
    project_config: Dict[str, Any],
    artifact: StagedArtifact,
    cf_tags: CurseForgeTags,
    release_type: str,
) -> Dict[str, Any]:
    game_version_ids = [
        cf_tags.minecraft_version_id,
        cf_tags.loader_ids_by_module[artifact.loader],
    ]
    game_version_ids.extend(cf_tags.environment_ids_by_side.values())

    metadata: Dict[str, Any] = {
        "changelog": f"CHANGELOG: {project_config['changelog_url']}",
        "changelogType": "markdown",
        "displayName": artifact.display_name,
        "gameVersions": game_version_ids,
        "releaseType": release_type,
        "isMarkedForManualRelease": False,
    }

    if artifact.dependencies:
        metadata["relations"] = {
            "projects": [
                {
                    "slug": dependency,
                    "type": "requiredDependency",
                }
                for dependency in artifact.dependencies
            ]
        }

    return metadata


def sync_modrinth_environment(
    client: ModrinthClient,
    project_config: Dict[str, Any],
    project_data: Dict[str, Any],
    *,
    modrinth_project_id: str,
    target_label: str,
    upload_enabled: bool,
) -> None:
    desired_client_side, desired_server_side = modrinth_environment_values(project_config)
    current_client_side = project_data.get("client_side")
    current_server_side = project_data.get("server_side")

    if (
        current_client_side == desired_client_side
        and current_server_side == desired_server_side
    ):
        CONSOLE.success(
            f"{target_label} Modrinth environment is already "
            f"client={desired_client_side}, server={desired_server_side}."
        )
        return

    message = (
        f"{target_label} Modrinth project environment "
        f"{current_client_side}/{current_server_side} -> "
        f"{desired_client_side}/{desired_server_side}"
    )
    if upload_enabled:
        client.patch_project_environment(
            modrinth_project_id,
            client_side=desired_client_side,
            server_side=desired_server_side,
        )
        CONSOLE.success(message + " (updated)")
    else:
        CONSOLE.warning(message + " (dry run, not updated)")


def print_plan_summary(
    *,
    project_root: Path,
    project_key: str,
    properties: Dict[str, str],
    project_config: Dict[str, Any],
    java_runtime: JavaRuntime,
    artifacts: Sequence[StagedArtifact],
    upload_enabled: bool,
    release_type: str,
) -> None:
    section("Upload Plan")
    CONSOLE.key_value("Project root", project_root, value_style="path")
    CONSOLE.key_value("Config key", project_key)
    CONSOLE.key_value("Mod ID", properties["mod_id"], value_style="file")
    CONSOLE.key_value("Mod version", properties["mod_version"], value_style="version")
    CONSOLE.key_value("Minecraft version", properties["minecraft_version"], value_style="version")
    CONSOLE.key_value(
        "Java version",
        f"{java_runtime.version} ({java_runtime.source})",
        value_style="version",
    )
    if java_runtime.java_home is not None:
        CONSOLE.key_value("JAVA_HOME", java_runtime.java_home, value_style="path")
    CONSOLE.key_value("Release type", release_type)
    mode_text = "upload after confirmation" if upload_enabled else "dry run"
    mode_style = "upload" if upload_enabled else "dry_run"
    CONSOLE.key_value("Mode", mode_text, value_style=mode_style)
    CONSOLE.key_value(
        "Supported environments",
        ", ".join(project_config["supported_environments"]),
    )
    mandatory = project_config.get("mandatory_environments", [])
    CONSOLE.key_value(
        "Mandatory environments",
        ", ".join(mandatory) if mandatory else "none",
    )
    CONSOLE.key_value(
        "Modrinth version environment",
        modrinth_version_environment(project_config),
    )

    upload_target_count = len(
        upload_target_keys_for_loaders(artifact.loader for artifact in artifacts)
    )
    for artifact in artifacts:
        target_config = get_upload_target_config(project_config, artifact.upload_target)
        dependencies = ", ".join(artifact.dependencies) if artifact.dependencies else "none"
        CONSOLE.subsection(module_display_name(artifact.loader))
        if upload_target_count > 1:
            CONSOLE.key_value(
                "Upload target",
                upload_target_label(artifact.upload_target),
                value_style="platform",
                indent=2,
            )
        CONSOLE.key_value(
            "Modrinth project",
            target_config["modrinth_project_id"],
            value_style="id",
            indent=2,
        )
        CONSOLE.key_value(
            "CurseForge project",
            target_config["curseforge_project_id"],
            value_style="id",
            indent=2,
        )
        CONSOLE.key_value("Source", artifact.source_path, value_style="path", indent=2)
        CONSOLE.key_value("Staged", artifact.staged_path, value_style="file", indent=2)
        CONSOLE.key_value("Display name", artifact.display_name, indent=2)
        CONSOLE.key_value(
            "Modrinth version",
            artifact.modrinth_version_number,
            value_style="version",
            indent=2,
        )
        CONSOLE.key_value("Required dependencies", dependencies, indent=2)
    sys.stdout.flush()


def confirm_real_upload(artifacts: Sequence[StagedArtifact], *, assume_yes: bool) -> None:
    if assume_yes:
        return
    if not sys.stdin.isatty():
        raise UploadError("Real upload requires confirmation, but stdin is not a terminal.")

    CONSOLE.blank()
    CONSOLE.warning("This will upload these files to Modrinth and CurseForge:")
    upload_target_count = len(
        upload_target_keys_for_loaders(artifact.loader for artifact in artifacts)
    )
    for artifact in artifacts:
        if upload_target_count > 1:
            target = upload_target_label(artifact.upload_target)
            CONSOLE.bullet(
                f"{artifact.file_name} -> {target}",
                style_name="file",
                indent=2,
            )
        else:
            CONSOLE.bullet(artifact.file_name, style_name="file", indent=2)
    answer = input(CONSOLE.prompt("Type 'upload' to continue: ")).strip()
    if answer != "upload":
        raise UploadError("Upload aborted before any files were sent.")


def upload_artifacts(
    *,
    modrinth_client: ModrinthClient,
    curseforge_client: CurseForgeClient,
    project_config: Dict[str, Any],
    properties: Dict[str, str],
    artifacts: Sequence[StagedArtifact],
    modrinth_targets_by_key: Dict[str, ModrinthTargets],
    cf_tags: CurseForgeTags,
    release_type: str,
) -> None:
    section("Uploading")
    for artifact in artifacts:
        CONSOLE.subsection(module_display_name(artifact.loader))
        target_config = get_upload_target_config(project_config, artifact.upload_target)
        modrinth_targets = modrinth_targets_by_key[artifact.upload_target]
        CONSOLE.key_value(
            "Upload target",
            upload_target_label(artifact.upload_target),
            value_style="platform",
            indent=2,
        )

        modrinth_metadata = modrinth_version_metadata(
            project_config,
            properties,
            artifact,
            release_type,
            modrinth_targets.project_id,
            modrinth_targets.dependency_ids_by_slug,
        )
        modrinth_result = modrinth_client.create_version(modrinth_metadata, artifact)
        modrinth_id = (
            modrinth_result.get("id")
            if isinstance(modrinth_result, dict)
            else modrinth_result
        )
        CONSOLE.success(
            "Modrinth version created: "
            + CONSOLE.style(modrinth_id, "id")
        )

        curseforge_metadata = curseforge_file_metadata(
            project_config,
            artifact,
            cf_tags,
            release_type,
        )
        curseforge_result = curseforge_client.upload_file(
            target_config["curseforge_project_id"],
            curseforge_metadata,
            artifact,
        )
        curseforge_id = (
            curseforge_result.get("id")
            if isinstance(curseforge_result, dict)
            else curseforge_result
        )
        CONSOLE.success(
            "CurseForge file uploaded: "
            + CONSOLE.style(curseforge_id, "id")
        )


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Build loader modules, identify upload JARs, stage clean filenames, "
            "and upload Minecraft mods to Modrinth and CurseForge."
        )
    )
    parser.add_argument(
        "--project-root",
        type=Path,
        default=Path.cwd(),
        help="Project root containing gradle.properties, common, and loader modules.",
    )
    parser.add_argument(
        "--config-project-key",
        help="Project key inside the script-local config file. Defaults to mod_id.",
    )
    parser.add_argument(
        "--reset-config",
        action="store_true",
        help="Forget this project's saved config and ask for it again.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Build, validate, and stage files without uploading or modifying platform metadata.",
    )
    parser.add_argument(
        "--skip-build",
        action="store_true",
        help="Skip Gradle builds. Useful only when build/libs outputs are already current.",
    )
    parser.add_argument(
        "--yes",
        action="store_true",
        help="Accept ambiguous JAR choices and real upload confirmation.",
    )
    parser.add_argument(
        "--release-type",
        choices=("release", "beta", "alpha"),
        default="release",
        help="Release type to send to Modrinth and CurseForge.",
    )
    parser.add_argument(
        "--config-path",
        type=Path,
        help=(
            "Override config path. Defaults to mod_upload_config.json next to this script."
        ),
    )
    parser.add_argument(
        "--color",
        choices=("auto", "always", "never"),
        default="auto",
        help="Control colored terminal output. Defaults to auto.",
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    configure_console(args.color)
    script_dir = Path(__file__).resolve().parent
    config_path = (args.config_path or (script_dir / CONFIG_FILE_NAME)).resolve()
    project_root = args.project_root.resolve()

    try:
        properties = parse_gradle_properties(project_root / "gradle.properties")
        require_gradle_properties(properties)

        project_key = args.config_project_key or properties["mod_id"]
        loaders = discover_loader_modules(project_root)

        config = load_config(config_path)
        project_config, config_changed = ensure_project_config(
            config,
            project_key,
            loaders,
            reset_project_config=args.reset_config,
        )
        java_runtime, java_config_changed = ensure_java_version(properties, project_config)
        config_changed = config_changed or java_config_changed

        if config_changed or not config_path.exists():
            save_config(config_path, config)
            CONSOLE.blank()
            CONSOLE.success(
                "Saved config: " + CONSOLE.style(config_path, "path")
            )

        if args.skip_build:
            section("Gradle Build")
            CONSOLE.warning("Skipped because --skip-build was provided.")
        else:
            java_runtime = resolve_java_runtime(java_runtime)
            build_loader_modules(project_root, loaders, java_runtime)

        artifacts = stage_artifacts(
            project_root,
            loaders,
            properties,
            project_config,
            assume_yes=args.yes,
        )

        tokens = load_tokens_from_keychain()

        modrinth_client = ModrinthClient(tokens["modrinth"])
        curseforge_client = CurseForgeClient(tokens["curseforge"])

        modrinth_targets_by_key = validate_modrinth_targets(
            modrinth_client,
            properties["minecraft_version"],
            project_config,
            artifacts,
        )
        cf_tags = resolve_curseforge_tags(
            curseforge_client,
            loaders,
            properties["minecraft_version"],
            project_config["supported_environments"],
        )

        print_plan_summary(
            project_root=project_root,
            project_key=project_key,
            properties=properties,
            project_config=project_config,
            java_runtime=java_runtime,
            artifacts=artifacts,
            upload_enabled=not args.dry_run,
            release_type=args.release_type,
        )

        if args.dry_run:
            for target_key, modrinth_targets in modrinth_targets_by_key.items():
                sync_modrinth_environment(
                    modrinth_client,
                    project_config,
                    modrinth_targets.project,
                    modrinth_project_id=modrinth_targets.project_id,
                    target_label=upload_target_message_label(target_key),
                    upload_enabled=False,
                )
            CONSOLE.blank()
            CONSOLE.success("Dry run complete.")
            CONSOLE.key_value(
                "Next step",
                "Re-run without --dry-run to upload.",
                value_style="command",
            )
        else:
            confirm_real_upload(artifacts, assume_yes=args.yes)
            for target_key, modrinth_targets in modrinth_targets_by_key.items():
                sync_modrinth_environment(
                    modrinth_client,
                    project_config,
                    modrinth_targets.project,
                    modrinth_project_id=modrinth_targets.project_id,
                    target_label=upload_target_message_label(target_key),
                    upload_enabled=True,
                )
            upload_artifacts(
                modrinth_client=modrinth_client,
                curseforge_client=curseforge_client,
                project_config=project_config,
                properties=properties,
                artifacts=artifacts,
                modrinth_targets_by_key=modrinth_targets_by_key,
                cf_tags=cf_tags,
                release_type=args.release_type,
            )
            CONSOLE.blank()
            CONSOLE.success("Done.")

        return 0

    except UploadError as exc:
        CONSOLE.blank()
        eprint(str(exc))
        return 1
    except KeyboardInterrupt:
        CONSOLE.blank()
        eprint("Aborted.")
        return 130


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
