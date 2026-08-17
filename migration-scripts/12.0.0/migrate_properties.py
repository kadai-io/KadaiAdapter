#!/usr/bin/env python3
"""Migrate KadaiAdapter 11.x .properties files to the 12.0.0 configuration."""

from __future__ import annotations

import argparse
import difflib
import os
import re
import shutil
import sys
import tempfile
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Optional


RENAMED_PROPERTIES = {
    "kadai.adapter.run-as.user": "kadai-adapter.kernel.run-as-user",
    "kadai.adapter.scheduler.run.interval.for.start.kadai.tasks.in.milliseconds": (
        "kadai-adapter.kernel.scheduler.start-kadai-tasks-interval"
    ),
    "kadai.adapter.scheduler.run.interval.for.complete.referenced.tasks.in.milliseconds": (
        "kadai-adapter.kernel.scheduler.complete-referenced-tasks-interval"
    ),
    "kadai.adapter.scheduler.run.interval.for.claim.referenced.tasks.in.milliseconds": (
        "kadai-adapter.kernel.scheduler.claim-referenced-tasks-interval"
    ),
    "kadai.adapter.scheduler.run.interval.for.cancel.claim.referenced.tasks.in.milliseconds": (
        "kadai-adapter.kernel.scheduler.cancel-claim-referenced-tasks-interval"
    ),
    "kadai.adapter.scheduler.run.interval.for.check.finished.referenced.tasks.in.milliseconds": (
        "kadai-adapter.kernel.scheduler.check-finished-referenced-tasks-interval"
    ),
    "kadai.adapter.scheduler.run.interval.for.retries.and.blocking.taskevents.in.milliseconds": (
        "kadai-adapter.kernel.scheduler.retries-and-blocking-task-events-interval"
    ),
    "kadai.adapter.sync.kadai.batchSize": "kadai-adapter.kernel.kadai-connector.batch-size",
    "kadai.adapter.mapping.default.objectreference.company": (
        "kadai-adapter.kernel.kadai-connector.task-mapping.object-reference.company"
    ),
    "kadai.adapter.mapping.default.objectreference.system": (
        "kadai-adapter.kernel.kadai-connector.task-mapping.object-reference.system"
    ),
    "kadai.adapter.mapping.default.objectreference.system.instance": (
        "kadai-adapter.kernel.kadai-connector.task-mapping.object-reference.system-instance"
    ),
    "kadai.adapter.mapping.default.objectreference.type": (
        "kadai-adapter.kernel.kadai-connector.task-mapping.object-reference.type"
    ),
    "kadai.adapter.mapping.default.objectreference.value": (
        "kadai-adapter.kernel.kadai-connector.task-mapping.object-reference.value"
    ),
    "kadai.adapter.events.lockDuration": "kadai-adapter.plugin.camunda7.lock-duration",
    "kadai.adapter.camunda.claiming.enabled": "kadai-adapter.plugin.camunda7.claiming.enabled",
    "kadai.adapter.xsrf.token": "kadai-adapter.plugin.camunda7.xsrf-token",
    # The unnumbered variants occur in 11.3.x configuration files. The Camunda-7 variants
    # occurred on the short-lived intermediate branch before 12.0.0.
    "kadai-system-connector-camunda-rest-api-user-name": (
        "kadai-adapter.plugin.camunda7.client.username"
    ),
    "kadai-system-connector-camunda-rest-api-user-password": (
        "kadai-adapter.plugin.camunda7.client.password"
    ),
    "kadai-system-connector-camunda7-rest-api-user-name": (
        "kadai-adapter.plugin.camunda7.client.username"
    ),
    "kadai-system-connector-camunda7-rest-api-user-password": (
        "kadai-adapter.plugin.camunda7.client.password"
    ),
    "kadai-system-connector-outbox-rest-api-user-name": (
        "kadai-adapter.plugin.camunda7.outbox.client.username"
    ),
    "kadai-system-connector-outbox-rest-api-user-password": (
        "kadai-adapter.plugin.camunda7.outbox.client.password"
    ),
}

LEGACY_CAMUNDA7_SYSTEM_PROPERTIES = {
    "kadai-system-connector-camundaSystemURLs",
    "kadai-system-connector-camunda7SystemURLs",
}

MANUAL_MIGRATION_PROPERTIES = {
    "kadai.datasource.jndi-name": (
        "JNDI support was removed. Configure kadai.datasource.jdbcUrl, "
        "kadai.datasource.username, and kadai.datasource.password instead."
    ),
    "kadai.adapter.outbox.datasource.jndi": (
        "JNDI support was removed. Configure the outbox datasource driver, URL, username, "
        "and password instead."
    ),
    "kadai.adapter.camunda.system.enabled": "This property has no 12.0.0 equivalent and can be removed.",
    "kadai.adapter.camunda.system.camunda.enabled": (
        "This property has no 12.0.0 equivalent and can be removed."
    ),
    "kadai.adapter.camunda.system.outbox.enabled": (
        "This property has no 12.0.0 equivalent and can be removed."
    ),
}

CAMUNDA8_CLAIMING_PROPERTY = "kadai-adapter.plugin.camunda8.claiming.enabled"

# This deliberately matches only active assignments. Comments are documentation, not runtime
# configuration, and should not be rewritten by a migration tool.
PROPERTY_LINE = re.compile(
    r"^(?P<indent>[ \t]*)(?P<key>[^#!\s:=]+)(?P<separator>[ \t]*(?:=|:)[ \t]*|[ \t]+)"
    r"(?P<value>.*?)(?P<newline>\r?\n)?$"
)


@dataclass(frozen=True)
class PropertyAssignment:
    line_number: int
    line: str
    key: str
    indent: str
    separator: str
    value: str
    newline: str


@dataclass
class MigrationResult:
    content: str
    changed: bool
    warnings: list[str]


@dataclass(frozen=True)
class LegacyCamunda7System:
    system_rest_url: str
    system_task_event_url: str
    camunda7_engine_identifier: Optional[str]


def parse_assignment(line_number: int, line: str) -> Optional[PropertyAssignment]:
    match = PROPERTY_LINE.match(line)
    if match is None:
        return None

    return PropertyAssignment(
        line_number=line_number,
        line=line,
        key=match.group("key"),
        indent=match.group("indent"),
        separator=match.group("separator"),
        value=match.group("value"),
        newline=match.group("newline") or "",
    )


def preferred_newline(content: str) -> str:
    return "\r\n" if "\r\n" in content else "\n"


def assignment_line(
    assignment: PropertyAssignment,
    key: str,
    value: Optional[str] = None,
    newline: Optional[str] = None,
) -> str:
    return (
        f"{assignment.indent}{key}{assignment.separator}"
        f"{assignment.value if value is None else value}{assignment.newline if newline is None else newline}"
    )


def is_camunda8_configuration(keys: Iterable[str]) -> bool:
    return any(
        key.startswith("camunda.client.") or key.startswith("kadai-adapter.plugin.camunda8.")
        for key in keys
    )


def parse_legacy_camunda7_systems(
    assignment: PropertyAssignment,
) -> tuple[list[LegacyCamunda7System], Optional[str]]:
    systems: list[LegacyCamunda7System] = []
    for system in assignment.value.split(","):
        urls = [url.strip() for url in system.split("|")]
        if len(urls) == 3 and not urls[2]:
            urls.pop()
        if len(urls) not in (2, 3) or not all(urls):
            return [], (
                f"line {assignment.line_number}: could not migrate {assignment.key}; expected "
                "one or more '<Camunda REST URL> | <Outbox REST URL> [| <engine identifier>]' "
                "pairs separated by commas."
            )
        systems.append(
            LegacyCamunda7System(
                system_rest_url=urls[0],
                system_task_event_url=urls[1],
                camunda7_engine_identifier=urls[2] if len(urls) == 3 else None,
            )
        )
    return systems, None


def migrate_text(content: str, preserve_camunda8_claiming: bool = True) -> MigrationResult:
    """Return migrated content and warnings without writing to the filesystem."""

    lines = content.splitlines(keepends=True)
    assignments = [
        assignment
        for line_number, line in enumerate(lines, start=1)
        if (assignment := parse_assignment(line_number, line)) is not None
    ]
    keys = {assignment.key for assignment in assignments}
    warnings: list[str] = []
    candidate_destinations: dict[str, list[int]] = defaultdict(list)
    parsed_systems: dict[int, list[LegacyCamunda7System]] = {}

    for assignment in assignments:
        renamed_key = RENAMED_PROPERTIES.get(assignment.key)
        if renamed_key is not None:
            candidate_destinations[renamed_key].append(assignment.line_number)
        elif assignment.key in LEGACY_CAMUNDA7_SYSTEM_PROPERTIES:
            systems, warning = parse_legacy_camunda7_systems(assignment)
            if warning is not None:
                warnings.append(warning)
                continue
            parsed_systems[assignment.line_number] = systems
            for index, system in enumerate(systems):
                prefix = f"kadai-adapter.plugin.camunda7.systems[{index}]"
                candidate_destinations[f"{prefix}.system-rest-url"].append(assignment.line_number)
                candidate_destinations[f"{prefix}.system-task-event-url"].append(assignment.line_number)
                if system.camunda7_engine_identifier is not None:
                    candidate_destinations[f"{prefix}.camunda7-engine-identifier"].append(
                        assignment.line_number
                    )

    conflicting_destinations = {
        destination
        for destination, source_lines in candidate_destinations.items()
        if destination in keys or len(source_lines) > 1
    }
    for destination in sorted(conflicting_destinations):
        source_lines = candidate_destinations[destination]
        if destination in keys:
            warnings.append(
                f"could not set {destination}; it is already configured. "
                f"Resolve the legacy property on line(s) {', '.join(map(str, source_lines))} manually."
            )
        else:
            warnings.append(
                f"could not set {destination}; multiple legacy properties map to it on line(s) "
                f"{', '.join(map(str, source_lines))}. Resolve them manually."
            )

    line_ending = preferred_newline(content)
    migrated_lines: list[str] = []
    changed = False

    for line_number, line in enumerate(lines, start=1):
        assignment = parse_assignment(line_number, line)
        if assignment is None:
            migrated_lines.append(line)
            continue

        manual_message = MANUAL_MIGRATION_PROPERTIES.get(assignment.key)
        if manual_message is not None:
            warnings.append(f"line {line_number}: {assignment.key}: {manual_message}")
            migrated_lines.append(line)
            continue

        renamed_key = RENAMED_PROPERTIES.get(assignment.key)
        if renamed_key is not None:
            if renamed_key in conflicting_destinations:
                migrated_lines.append(line)
            else:
                migrated_lines.append(assignment_line(assignment, renamed_key))
                changed = True
            continue

        systems = parsed_systems.get(line_number)
        if systems is not None:
            destinations = []
            for index, system in enumerate(systems):
                prefix = f"kadai-adapter.plugin.camunda7.systems[{index}]"
                destinations.extend(
                    (f"{prefix}.system-rest-url", f"{prefix}.system-task-event-url")
                )
                if system.camunda7_engine_identifier is not None:
                    destinations.append(f"{prefix}.camunda7-engine-identifier")
            if any(destination in conflicting_destinations for destination in destinations):
                migrated_lines.append(line)
                continue

            generated_properties = []
            for index, system in enumerate(systems):
                prefix = f"kadai-adapter.plugin.camunda7.systems[{index}]"
                generated_properties.extend(
                    (
                        (f"{prefix}.system-rest-url", system.system_rest_url),
                        (f"{prefix}.system-task-event-url", system.system_task_event_url),
                    )
                )
                if system.camunda7_engine_identifier is not None:
                    generated_properties.append(
                        (
                            f"{prefix}.camunda7-engine-identifier",
                            system.camunda7_engine_identifier,
                        )
                    )
            for property_index, (key, value) in enumerate(generated_properties):
                migrated_lines.append(
                    assignment_line(
                        assignment,
                        key,
                        value,
                        assignment.newline
                        if property_index == len(generated_properties) - 1
                        else line_ending,
                    )
                )
            changed = True
            continue

        migrated_lines.append(line)

    migrated_content = "".join(migrated_lines)
    if (
        preserve_camunda8_claiming
        and is_camunda8_configuration(keys)
        and CAMUNDA8_CLAIMING_PROPERTY not in keys
    ):
        if migrated_content and not migrated_content.endswith(("\n", "\r")):
            migrated_content += line_ending
        migrated_content += (
            "# Added by the KadaiAdapter 12.0.0 migration to preserve the previous default."
            f"{line_ending}{CAMUNDA8_CLAIMING_PROPERTY}=true{line_ending}"
        )
        changed = True

    return MigrationResult(content=migrated_content, changed=changed, warnings=warnings)


def read_text(path: Path) -> str:
    with path.open("r", encoding="utf-8", newline="") as file:
        return file.read()


def write_text_atomically(path: Path, content: str) -> None:
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    temporary_path = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="") as file:
            file.write(content)
        shutil.copymode(path, temporary_path)
        temporary_path.replace(path)
    except BaseException:
        temporary_path.unlink(missing_ok=True)
        raise


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Migrate KadaiAdapter 11.x .properties files to the 12.0.0 configuration."
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="write the migration after creating a backup; otherwise print a unified diff",
    )
    parser.add_argument(
        "--backup-suffix",
        default=".pre-12.0.0",
        help="suffix for backups created with --apply (default: %(default)s)",
    )
    parser.add_argument(
        "--no-preserve-camunda8-claiming",
        action="store_true",
        help="do not add the Camunda 8 claiming setting that preserves the pre-12.0.0 default",
    )
    parser.add_argument("files", nargs="+", type=Path, help=".properties files to migrate")
    return parser


def main(argv: Optional[list[str]] = None) -> int:
    arguments = build_parser().parse_args(argv)
    files: list[tuple[Path, str, MigrationResult]] = []
    exit_code = 0

    for path in arguments.files:
        if not path.is_file():
            print(f"[error] {path}: not a file", file=sys.stderr)
            exit_code = 1
            continue
        try:
            original_content = read_text(path)
        except UnicodeDecodeError:
            print(f"[error] {path}: expected a UTF-8 encoded .properties file", file=sys.stderr)
            exit_code = 1
            continue
        result = migrate_text(
            original_content,
            preserve_camunda8_claiming=not arguments.no_preserve_camunda8_claiming,
        )
        files.append((path, original_content, result))

    if exit_code != 0:
        return exit_code

    if arguments.apply:
        for path, _, result in files:
            if not result.changed:
                continue
            backup = path.with_name(path.name + arguments.backup_suffix)
            if backup.exists():
                print(f"[error] {path}: backup already exists at {backup}", file=sys.stderr)
                return 1

        for path, original_content, result in files:
            if result.changed:
                backup = path.with_name(path.name + arguments.backup_suffix)
                shutil.copyfile(path, backup)
                shutil.copymode(path, backup)
                write_text_atomically(path, result.content)
                print(f"[migrated] {path} (backup: {backup})")
            else:
                print(f"[unchanged] {path}")
    else:
        for path, original_content, result in files:
            if result.changed:
                sys.stdout.writelines(
                    difflib.unified_diff(
                        original_content.splitlines(keepends=True),
                        result.content.splitlines(keepends=True),
                        fromfile=str(path),
                        tofile=f"{path} (12.0.0 migration)",
                    )
                )
            else:
                print(f"[unchanged] {path}")

    has_warnings = False
    for path, _, result in files:
        for warning in result.warnings:
            print(f"[warning] {path}: {warning}", file=sys.stderr)
            has_warnings = True
    return 2 if has_warnings else 0


if __name__ == "__main__":
    raise SystemExit(main())
