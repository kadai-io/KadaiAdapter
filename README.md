# KadaiAdapter

[![Contributors](https://img.shields.io/github/contributors/kadai-io/KadaiAdapter.svg)](https://github.com/kadai-io/KadaiAdapter/graphs/contributors)
[![CI KADAI adapter](https://github.com/kadai-io/KadaiAdapter/actions/workflows/continuous-integration.yml/badge.svg)](https://github.com/kadai-io/KadaiAdapter/actions/workflows/continuous-integration.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kadai-io_KadaiAdapter&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=kadai-io_KadaiAdapter)
[![Maven Central](https://img.shields.io/maven-central/v/io.kadai/kadai-adapter.svg)](https://central.sonatype.com/artifact/io.kadai/kadai-adapter)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Adapter to sync tasks between [KADAI](https://github.com/kadai-io/kadai) and an external workflow
system, e.g. [Camunda BPM](https://camunda.com/).

## 🧫Components

The Kadai Adapter repository consists of the Kadai adapter, sample connectors to Camunda and Kadai
as well as
an outbox REST-Service, and it's SpringBoot-Starter and listeners for Camunda. In addition to that
there are various
example and test modules.

The sample implementation of the camunda-system-connector uses
the [Transactional-Outbox-Pattern](https://microservices.io/patterns/data/transactional-outbox.html).

| **Component**                                             | **Description**                                                                                                                                                                                                                                   |
|-----------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **kadai-adapter**                                         | The adapter. Defines the service provider SPIs and APIs for SystemConnector (connects to the external system) and KadaiConnector (connects to kadai). These connectors are plugged in at runtime via SPI mechanisms.                              |
| **kadai-adapter-camunda-system-connector**                | Sample implementation of SystemConnector SPI. Connects to camunda systems via camunda's REST API.                                                                                                                                                 |
| **kadai-adapter-kadai-connector**                         | Sample implementation of KadaiConnector SPI. Connects to one kadai system via kadai's Java API which accesses the database directly.                                                                                                              |
| **kadai-adapter-camunda-listener**                        | Contains a TaskListener, ParseListener, ParseListenerProcessEnginePlugin, and OutboxSchemaCreator as client-side components of camunda-system-connector.                                                                                          |
| **kadai-adapter-camunda-outbox-rest**                     | An outbox REST-Service which allows the adapter to query events from the outbox tables, implemented using JAX-RS. The concept of the outbox-pattern can be found under the "Notes for sample implementation of camunda-system-connector" section. |
| **kadai-adapter-camunda-outbox-rest-spring-boot-starter** | SpringBoot-Starter in case the REST-Service is used within a SpringBoot-Application.                                                                                                                                                              |
| **kadai-adapter-camunda-spring-boot-sample**              | SpringBoot-Application containing the adapter with the sample camunda-system-connector implementation.                                                                                                                                            |
| **kadai-adapter-camunda-spring-boot-test**                | SpringBoot-Application containing camunda, the adapter, and the outbox REST-Service to test a complete scenario.                                                                                                                                  |
| **kadai-adapter-camunda-listener-example**                | Example Process-Application that can be deployed to camunda.                                                                                                                                                                                      |

## 🔗The Adapter defines two SPIs

- **SystemConnector SPI** connects the adapter to an external system like e.g. Camunda.
- **KadaiConnector SPI** connects the adapter to kadai.

Both SPI implementations are loaded by the adapter at initialization time via the Java SPI
mechanism. They provide plug
points where custom code can be plugged in.\
Please note, that the term ‘referenced task’ is used in this document to refer to tasks in the
external system that is
accessed via the SystemConnector

## 🔎Overall Function

The adapter performs periodically the following tasks

* `retrieveNewReferencedTasksAndCreateCorrespondingKadaiTasks`
  * retrieve newly created referenced tasks via `SystemConnector::retrieveNewStartedReferencedTasks`
  * get the task’s variables via `SystemConnector::retrieveVariables`
  * map referenced task to KADAI task via `KadaiConnector::convertToKadaiTask`
  * create an associated KADAI task via `KadaiConnector::createKadaiTask`
  *	clean the corresponding create-event in the outbox via `SystemConnector::kadaiTasksHaveBeenCreatedForNewReferencedTasks`
* `retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks`
  * retrieve finished referenced tasks via `SystemConnector::retrieveFinishedTasks`
  * terminate corresponding KADAI tasks via `KadaiConnector::terminateKadaiTask`
  * clean the corresponding complete/delete-event in the outbox via `SystemConnector::kadaiTasksHaveBeenCompletedForTerminatedReferencedTasks`
* `retrieveFinishedKadaiTasksAndCompleteCorrespondingReferencedTasks`
  * retrieve finished KADAI tasks via `KadaiConnector::retrieveCompletedKadaiTasksAsReferencedTasks`
  * complete the corresponding referenced tasks in the external system via `SystemConnector::completeReferencedTask`
  * change the CallbackState of the corresponding task in KADAI to completed via `KadaiConnector::changeReferencedTaskCallbackState`
* `retrieveClaimedKadaiTasksAndClaimCorrespondingReferencedTasks`
  * retrieve claimed KADAI tasks via `KadaiConnector::retrieveClaimedKadaiTasksAsReferencedTasks`
  * claim the corresponding referenced tasks in the external system via ``SystemConnector::claimReferencedTask``
  * change the CallbackState of the corresponding task in KADAI to claimed via `KadaiConnector::changeReferencedTaskCallbackState`
* `retrieveCancelledClaimKadaiTasksAndCancelClaimCorrespondingReferencedTasks`
  * retrieve cancel claimed KADAI tasks via `KadaiConnector::retrieveCancelledClaimKadaiTasksAsReferencedTasks`
  * cancel the claim of the corresponding referenced tasks in the external system via `SystemConnector::cancelClaimReferencedTask`
  * change the CallbackState of the corresponding task in KADAI to processing required via `KadaiConnector::changeReferencedTaskCallbackState`         

## 📓Notes

1. **Variables** \
   When the adapter finds a referenced task for which a kadai task must be started, it checks the
   variables of the
   referenced task's process. If they are not already present due to retrieval from the outbox it
   will attempt to
   retrieve them from the referenced task's process.
   These variables are stored in the **custom attributes** of the corresponding kadai task in a
   HashMap with key **referenced_task_variables** and value of type String that contains the Json representation of
   the variables.
2. **Workbaskets** \
   The Adapter does not perform routing of tasks to workbaskets but instead relies on a SPI.

# 📨Contact

If you have any questions or ideas feel free to create an [issue](https://github.com/kadai-io/KadaiAdapter/issues) or contact us
via [GitHub Discussions](https://github.com/kadai-io/kadai/discussions).

We love listening to your feedback, and of course also discussing the project roadmap and possible use cases with you!

This open source project is being developed by [envite consulting GmbH](https://www.envite.de/)
with the support of the open source community.

---
[![envite consulting GmbH](docs/images/envite-black.png)](https://envite.de/)
---