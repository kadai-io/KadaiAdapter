-- Outbox schema for the embedded Camunda 7 Outbox REST service.
-- Spring Boot executes this against spring.datasource (jdbc:h2:mem:outbox) on startup.
-- The OutboxDataSource connects to the same named H2 instance so both share the tables.

CREATE SCHEMA IF NOT EXISTS kadai_tables;

CREATE TABLE IF NOT EXISTS kadai_tables.event_store
(
    ID                      INT          NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
    TYPE                    VARCHAR(32)  NOT NULL,
    CREATED                 TIMESTAMP,
    PAYLOAD                 CLOB,
    REMAINING_RETRIES       INT          NOT NULL,
    BLOCKED_UNTIL           TIMESTAMP    NOT NULL,
    ERROR                   VARCHAR(1000),
    CAMUNDA_TASK_ID         VARCHAR(40),
    SYSTEM_ENGINE_IDENTIFIER VARCHAR(128),
    LOCK_EXPIRE             TIMESTAMP    NULL,
    CONSTRAINT event_store_pkey PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS kadai_tables.OUTBOX_SCHEMA_VERSION
(
    ID      INT          NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
    VERSION VARCHAR(255) NOT NULL,
    CREATED TIMESTAMP    NOT NULL,
    PRIMARY KEY (ID)
);

INSERT INTO kadai_tables.OUTBOX_SCHEMA_VERSION (VERSION, CREATED)
    SELECT '1.12.0', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM kadai_tables.OUTBOX_SCHEMA_VERSION);

