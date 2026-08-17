#!/usr/bin/env python3
"""Tests for the KadaiAdapter 12.0.0 properties migration."""

from __future__ import annotations

import contextlib
import io
import tempfile
import unittest
from pathlib import Path

import migrate_properties


class MigratePropertiesTest(unittest.TestCase):
    def test_migrates_all_renamed_properties(self) -> None:
        for index, (old_property, new_property) in enumerate(
            migrate_properties.RENAMED_PROPERTIES.items(), start=1
        ):
            with self.subTest(old_property=old_property):
                result = migrate_properties.migrate_text(f"{old_property}={index}\n")

                self.assertTrue(result.changed)
                self.assertEqual([], result.warnings)
                self.assertNotIn(f"{old_property}=", result.content)
                self.assertEqual(f"{new_property}={index}\n", result.content)

    def test_expands_legacy_camunda7_system_urls(self) -> None:
        original = (
            "kadai-system-connector-camundaSystemURLs=https://camunda-1/engine-rest | "
            "https://camunda-1/outbox | engine-1, "
            "https://camunda-2/engine-rest|https://camunda-2/outbox\n"
        )
        result = migrate_properties.migrate_text(original)

        self.assertEqual([], result.warnings)
        self.assertEqual(
            "kadai-adapter.plugin.camunda7.systems[0].system-rest-url=https://camunda-1/engine-rest\n"
            "kadai-adapter.plugin.camunda7.systems[0].system-task-event-url=https://camunda-1/outbox\n"
            "kadai-adapter.plugin.camunda7.systems[0].camunda7-engine-identifier=engine-1\n"
            "kadai-adapter.plugin.camunda7.systems[1].system-rest-url=https://camunda-2/engine-rest\n"
            "kadai-adapter.plugin.camunda7.systems[1].system-task-event-url=https://camunda-2/outbox\n",
            result.content,
        )

    def test_preserves_camunda8_claiming_default_when_camunda8_is_configured(self) -> None:
        result = migrate_properties.migrate_text("camunda.client.rest-address=https://camunda.example\n")

        self.assertEqual([], result.warnings)
        self.assertEqual(
            "camunda.client.rest-address=https://camunda.example\n"
            "# Added by the KadaiAdapter 12.0.0 migration to preserve the previous default.\n"
            "kadai-adapter.plugin.camunda8.claiming.enabled=true\n",
            result.content,
        )

    def test_does_not_change_camunda8_claiming_when_it_is_explicit(self) -> None:
        original = (
            "camunda.client.rest-address=https://camunda.example\n"
            "kadai-adapter.plugin.camunda8.claiming.enabled=false\n"
        )

        result = migrate_properties.migrate_text(original)

        self.assertFalse(result.changed)
        self.assertEqual(original, result.content)

    def test_leaves_unmigratable_jndi_settings_in_place_and_warns(self) -> None:
        original = "kadai.datasource.jndi-name=java:comp/env/jdbc/kadai\n"

        result = migrate_properties.migrate_text(original)

        self.assertFalse(result.changed)
        self.assertEqual(original, result.content)
        self.assertEqual(1, len(result.warnings))
        self.assertIn("JNDI support was removed", result.warnings[0])

    def test_does_not_overwrite_an_existing_new_property(self) -> None:
        original = (
            "kadai.adapter.run-as.user=legacy-user\n"
            "kadai-adapter.kernel.run-as-user=current-user\n"
        )

        result = migrate_properties.migrate_text(original)

        self.assertFalse(result.changed)
        self.assertEqual(original, result.content)
        self.assertEqual(1, len(result.warnings))
        self.assertIn("already configured", result.warnings[0])

    def test_apply_creates_backup_and_preserves_file_mode(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            properties_file = Path(temporary_directory) / "application.properties"
            properties_file.write_text("kadai.adapter.run-as.user=taskadmin\n", encoding="utf-8")
            properties_file.chmod(0o640)

            with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
                exit_code = migrate_properties.main(["--apply", str(properties_file)])

            self.assertEqual(0, exit_code)
            self.assertEqual(
                "kadai-adapter.kernel.run-as-user=taskadmin\n",
                properties_file.read_text(encoding="utf-8"),
            )
            self.assertEqual(
                "kadai.adapter.run-as.user=taskadmin\n",
                (Path(str(properties_file) + ".pre-12.0.0")).read_text(encoding="utf-8"),
            )
            self.assertEqual(0o640, properties_file.stat().st_mode & 0o777)


if __name__ == "__main__":
    unittest.main()
