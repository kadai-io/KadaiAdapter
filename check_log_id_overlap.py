#!/usr/bin/env python3
import argparse
import re
import sys
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Optional


ARRAY_PATTERN = re.compile(r"\[(\d+(?:\s*,\s*\d+)*)\]")
ID_PATTERN = re.compile(r"\d+")
TIMESTAMP_PATTERN = re.compile(
    r"^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}[+-]\d{2}:\d{2})"
)


@dataclass(frozen=True)
class LoggedIdOccurrence:
    line_number: int
    timestamp: Optional[datetime]


def parse_logged_ids(log_path):
    ids_by_line = defaultdict(list)

    with log_path.open(encoding="utf-8", errors="replace") as log_file:
        for line_number, line in enumerate(log_file, start=1):
            timestamp = parse_timestamp(line)
            for array_match in ARRAY_PATTERN.finditer(line):
                for id_match in ID_PATTERN.finditer(array_match.group(1)):
                    ids_by_line[int(id_match.group(0))].append(
                        LoggedIdOccurrence(line_number, timestamp)
                    )

    return ids_by_line


def parse_timestamp(line):
    timestamp_match = TIMESTAMP_PATTERN.match(line)
    if not timestamp_match:
        return None
    return datetime.fromisoformat(timestamp_match.group(1))


def format_lines(occurrences):
    unique_lines = sorted(
        {occurrence.line_number for occurrence in occurrences}
    )
    return ",".join(str(line_number) for line_number in unique_lines)


def format_time_differences(log1_occurrences, log2_occurrences):
    differences = []
    for log1_occurrence in log1_occurrences:
        for log2_occurrence in log2_occurrences:
            if log1_occurrence.timestamp is None or log2_occurrence.timestamp is None:
                differences.append(
                    f"{log1_occurrence.line_number}<->{log2_occurrence.line_number}:n/a"
                )
                continue

            diff_ms = round(
                abs(
                    (log2_occurrence.timestamp - log1_occurrence.timestamp).total_seconds()
                    * 1000
                )
            )
            differences.append(
                f"{log1_occurrence.line_number}<->{log2_occurrence.line_number}:{diff_ms}ms"
            )
    return ", ".join(differences)


def main():
    parser = argparse.ArgumentParser(
        description="Find IDs that appear in both numeric arrays logged by two files."
    )
    parser.add_argument("log1", nargs="?", default="logs1.log", type=Path)
    parser.add_argument("log2", nargs="?", default="logs2.log", type=Path)
    args = parser.parse_args()

    missing_logs = [str(path) for path in (args.log1, args.log2) if not path.is_file()]
    if missing_logs:
        print(f"Missing log file(s): {', '.join(missing_logs)}", file=sys.stderr)
        return 2

    log1_ids = parse_logged_ids(args.log1)
    log2_ids = parse_logged_ids(args.log2)
    overlapping_ids = sorted(set(log1_ids) & set(log2_ids))

    print(f"{args.log1}: {len(log1_ids)} unique IDs parsed")
    print(f"{args.log2}: {len(log2_ids)} unique IDs parsed")

    if not overlapping_ids:
        print("OK: no ID appears in both logs.")
        return 0

    print(f"Violation: {len(overlapping_ids)} ID(s) appear in both logs:")
    for task_id in overlapping_ids:
        print(
            f"{task_id} "
            f"({args.log1}:{format_lines(log1_ids[task_id])}; "
            f"{args.log2}:{format_lines(log2_ids[task_id])}; "
            f"diff_ms:{format_time_differences(log1_ids[task_id], log2_ids[task_id])})"
        )

    return 1


if __name__ == "__main__":
    raise SystemExit(main())
