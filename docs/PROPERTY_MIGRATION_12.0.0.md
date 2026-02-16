# Property Migration Guide - Version 12.0.0

This document lists all configuration properties that have been renamed or changed in version 12.0.0.

## Kernel Properties

### Run-As User
- `kadai.adapter.run-as.user` => `kadai-adapter.kernel.run-as-user`

### Scheduler Intervals
- `kadai.adapter.scheduler.run.interval.for.start.kadai.tasks.in.milliseconds` => `kadai-adapter.kernel.scheduler.start-kadai-tasks-interval`
- `kadai.adapter.scheduler.run.interval.for.complete.referenced.tasks.in.milliseconds` => `kadai-adapter.kernel.scheduler.complete-referenced-tasks-interval`
- `kadai.adapter.scheduler.run.interval.for.claim.referenced.tasks.in.milliseconds` => `kadai-adapter.kernel.scheduler.claim-referenced-tasks-interval`
- `kadai.adapter.scheduler.run.interval.for.cancel.claim.referenced.tasks.in.milliseconds` => `kadai-adapter.kernel.scheduler.cancel-claim-referenced-tasks-interval`
- `kadai.adapter.scheduler.run.interval.for.check.finished.referenced.tasks.in.milliseconds` => `kadai-adapter.kernel.scheduler.check-finished-referenced-tasks-interval`
- `kadai.adapter.scheduler.run.interval.for.retries.and.blocking.taskevents.in.milliseconds` => `kadai-adapter.kernel.scheduler.retries-and-blocking-task-events-interval`

## Kadai Connector Properties

### Batch Size
- `kadai.adapter.sync.kadai.batchSize` => `kadai-adapter.kernel.kadai-connector.batch-size`

### Task Mapping - Object Reference
- `kadai.adapter.mapping.default.objectreference.company` => `kadai-adapter.kernel.kadai-connector.task-mapping.object-reference.company`
- `kadai.adapter.mapping.default.objectreference.system` => `kadai-adapter.kernel.kadai-connector.task-mapping.object-reference.system`
- `kadai.adapter.mapping.default.objectreference.system.instance` => `kadai-adapter.kernel.kadai-connector.task-mapping.object-reference.system-instance`
- `kadai.adapter.mapping.default.objectreference.type` => `kadai-adapter.kernel.kadai-connector.task-mapping.object-reference.type`
- `kadai.adapter.mapping.default.objectreference.value` => `kadai-adapter.kernel.kadai-connector.task-mapping.object-reference.value`

## Camunda 7 Plugin Properties

### Lock Duration
- `kadai.adapter.events.lockDuration` => `kadai-adapter.plugin.camunda7.lock-duration`

### Claiming
- `kadai.adapter.camunda.claiming.enabled` => `kadai-adapter.plugin.camunda7.claiming.enabled`

### XSRF Token
- `kadai.adapter.xsrf.token` => `kadai-adapter.plugin.camunda7.xsrf-token`

## Notes

- All properties now use consistent kebab-case naming (e.g., `start-kadai-tasks-interval` instead of `start.kadai.tasks.in.milliseconds`)
- Properties are now organized under clear prefixes:
  - `kadai-adapter.kernel.*` - Core adapter functionality
  - `kadai-adapter.kernel.kadai-connector.*` - Kadai-specific configuration
  - `kadai-adapter.plugin.camunda7.*` - Camunda 7 plugin configuration
  - `kadai-adapter.plugin.camunda8.*` - Camunda 8 plugin configuration
- Interval properties no longer include `.in.milliseconds` suffix - the unit is still milliseconds
