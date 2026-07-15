----------------------------------------------------------------------
-- The following variable needs to be changed for customization and
-- before running this script.
-- %schemaName% = Schema name for the outbox tables
-- %camundaSchemaName% = name of camunda's schema
----------------------------------------------------------------------
-- Example: sqlplus scott/tiger@mydb @outbox-schema-oracle-create-user.sql
-- or, at the sqlplus prompt, enter
-- SQL> @outbox-schema-oracle.sql

ALTER SESSION SET CURRENT_SCHEMA = %schemaName% ;

CREATE TABLE %schemaName%.EVENT_STORE
(
    ID NUMBER(32) GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    TYPE VARCHAR(32) NOT NULL,
    CREATED TIMESTAMP,
    PAYLOAD CLOB,
    REMAINING_RETRIES NUMBER(32) NOT NULL,
    BLOCKED_UNTIL TIMESTAMP NOT NULL,
    ERROR VARCHAR2(1000),
    CAMUNDA_TASK_ID VARCHAR(40),
    SYSTEM_ENGINE_IDENTIFIER VARCHAR(128),
    LOCK_EXPIRE TIMESTAMP NULL
    );

ALTER TABLE %schemaName%.EVENT_STORE ADD(
    CONSTRAINT EVENT_STORE_PKEY PRIMARY KEY (ID)
);

CREATE TABLE %schemaName%.OUTBOX_SCHEMA_VERSION(
        ID NUMBER(32) GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
        VERSION VARCHAR(255) NOT NULL,
        CREATED TIMESTAMP NOT NULL
);


ALTER TABLE %schemaName%.OUTBOX_SCHEMA_VERSION ADD(
    CONSTRAINT SCHEMA_VERSION_PKEY PRIMARY KEY (ID)
);


INSERT INTO %schemaName%.OUTBOX_SCHEMA_VERSION (VERSION, CREATED) VALUES ('1.12.0', CURRENT_TIMESTAMP);

GRANT INSERT,UPDATE,SELECT,DELETE ON %schemaName%.OUTBOX_SCHEMA_VERSION TO %camundaSchemaName%;
GRANT INSERT,UPDATE,SELECT,DELETE ON %schemaName%.EVENT_STORE TO %camundaSchemaName%;
