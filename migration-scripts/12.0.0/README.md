# Migrate to KadaiAdapter 12.0.0

_We can neither guarantee that the migration script will work with all KadaiAdapter versions nor
that it covers all changes. Please consult the release notes for each version for more information.
With 12.0.0 all KadaiAdapter properties are strongly typed and therefore supported by IDE inlays for
`application.properties`._

---

`migrate_properties.py` updates active assignments in UTF-8 Java `.properties` files. It targets
KadaiAdapter 11.x configuration directly, so a separate migration through intermediate releases is
not required.

Preview the changes first. The default mode only prints a unified diff and does not modify files.

```bash
python3 migration-scripts/12.0.0/migrate_properties.py path/to/application.properties
```

After reviewing the diff, apply the migration. The script writes a sibling backup with the suffix
`.pre-12.0.0` before changing each file.

```bash
python3 migration-scripts/12.0.0/migrate_properties.py --apply \
  path/to/application.properties path/to/application-prod.properties
```

## Automated changes

The script covers all property renames
from [#401](https://github.com/kadai-io/KadaiAdapter/pull/401):

- Kernel: run-as user, all scheduler intervals, Kadai connector batch size, and default object
  reference fields.
- Camunda 7: claiming, lock duration, XSRF token, and Camunda/Outbox basic-auth credentials.

It also migrates the comma-separated legacy `kadai-system-connector-camundaSystemURLs` value to the
indexed `kadai-adapter.plugin.camunda7.systems[i]` Spring properties introduced by
[#353](https://github.com/kadai-io/KadaiAdapter/pull/353). Multiple systems and each system's
optional third engine identifier are supported.

Camunda 8 claiming changed from enabled to disabled by default
in [#495](https://github.com/kadai-io/KadaiAdapter/pull/495). When the file contains a
`camunda.client.*` or `kadai-adapter.plugin.camunda8.*` setting but does not explicitly set
claiming, the script adds
`kadai-adapter.plugin.camunda8.claiming.enabled=true` to preserve pre-12.0.0 behavior. Pass
`--no-preserve-camunda8-claiming` to adopt the 12.0.0 default instead.

The script preserves comments and line endings, and leaves a legacy key untouched when its target
key already exists. It reports such conflicts and exits with status `2` so they can be resolved
manually. Exit status `1` indicates a command or file error.

## Manual migration required

The following changes cannot be safely inferred and are reported as warnings:

- JNDI datasource properties `kadai.datasource.jndi-name` and
  `kadai.adapter.outbox.datasource.jndi` were removed
  in [#428](https://github.com/kadai-io/KadaiAdapter/pull/428). Replace them with the appropriate
  JDBC driver, URL, username, and password for the target datasource.
- Spring Boot 4 requires Java 21 and changes public Spring APIs. Review the
  [Spring Boot 4 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
  for application code and dependency updates.
- Public Camunda 7 classes and beans were renamed to include `Camunda7`
  in [#353](https://github.com/kadai-io/KadaiAdapter/pull/353). Update Java imports and any explicit
  bean references separately.
