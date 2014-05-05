---------------------------------------------------------------
-- Copyright (C) 2012-2014 Luleå University of Technology
-- All rights reserved.
---------------------------------------------------------------

---------------------------------------------------------------
-- Database history
--
-- Administration operations, or actions, on the database are
-- logged to a history table.
--
-- Actions are:
--
--   0 - the initial creation of the database
--   1 - an upgrade of the database
--
--
-- Write initial entry in history table containing data
-- model version.
--
-- ============================================================
-- = OBSERVE: When modifying the schema, increase either the  =
-- =          major or minor version. This version does not   =
-- =          necessarily have to be in harmony with any      =
-- =          application using the database.                 =
-- ============================================================
--
CREATE TABLE ppe_db_history (
  occasion    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  action      INTEGER       NOT NULL, -- 0=init, 1=upgrade
  description VARCHAR(255)  NOT NULL,
  major       INTEGER       NOT NULL, -- major version
  minor       INTEGER       NOT NULL  -- minor version
);

INSERT INTO ppe_db_history (
  action,
  description,
  major,
  minor
) VALUES (
  0,                  -- 'init'
  'Initiating database with schema version 1.0', -- description
  1,                  -- major version
  0                   -- minor version
);


---------------------------------------------------------------
-- Known purposes
--
CREATE TABLE ppe_purposes (
  purpose VARCHAR(255) NOT NULL, -- purpose ("evidence", "diagnostic", "research", "historical", ...)

  CONSTRAINT ppe_purposes_pk
    PRIMARY KEY (purpose),

  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


---------------------------------------------------------------
-- Known events
--
-- ON_START, ON_INGEST, ON_ERROR, ON_SEARCH, ON_ACCESS, ON_END, ON_TIME
--
CREATE TABLE ppe_events (
  event VARCHAR(255) NOT NULL, -- event ("ON_INGEST", "ON_ACCESS", ...)

  CONSTRAINT ppe_events_pk
    PRIMARY KEY (event),

  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


---------------------------------------------------------------
-- Assessments, currently defined in TDR
--
CREATE TABLE ppe_assessments (
  assessmentid VARCHAR(255) NOT NULL, -- id of assessment ("3.1.2.1" or the like)

  CONSTRAINT ppe_assessments_pk
    PRIMARY KEY (assessmentid),

  source VARCHAR(255) NOT NULL, -- name of source ("TDR" or the like)
  mnemonic VARCHAR(255), -- symbolic name of assessment

  condition INTEGER NOT NULL DEFAULT 0, -- 0=shall, 1=may, ... (adjust!)
  description VARCHAR(255), -- short description of assessment
  motivation VARCHAR(255), -- motivation of claim

  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-----------------------------------------------------------------
-- Plugin-data, i.e. opinions about plugins and their conformance
-- to TDR (or similarly appropriate standards). This is part of
-- the auxiliary data of the configurator.
--
CREATE TABLE ppe_opinions ( 
  providerid   VARCHAR(255) NOT NULL, -- id of service provider ("Amazon")
  systemid     VARCHAR(255) NOT NULL, -- id of system ("S3", "EC3", ...)
  assessmentid VARCHAR(255) NOT NULL, -- id of assessment ("3.1.2.1" or the like)

  CONSTRAINT ppe_opinions_pk
    PRIMARY KEY (providerid, systemid, assessmentid),

  CONSTRAINT ppe_opinions_assessment_exists
    FOREIGN KEY (assessmentid) REFERENCES ppe_assessments(assessmentid),

  locality     VARCHAR(255), -- locality of service&system ("US", "EU", ...)
  assessment   INTEGER, -- the actual assessment ('1'=true, '0'=false or null)
  probability  DOUBLE DEFAULT 100.0,  -- the probability of an effect taking place
  modified     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


---------------------------------------------------------------
-- Weighting-data which is used to identify assessments that
-- are particularly important for a given 'purpose'.
--
CREATE TABLE ppe_weights (
  assessmentid VARCHAR(255) NOT NULL, -- id of assessment ("3.1.2.1" or the like),
  --event VARCHAR(255) NOT NULL, -- event ("ON_INGEST", "ON_STORAGE", "ON_ACCESS", "ON_SEARCH", "ON_TIME", ...)
  purpose VARCHAR(255) NOT NULL, -- purpose ("evidence", "diagnostics", "research", ...)

  CONSTRAINT ppe_weights_pk
    PRIMARY KEY (assessmentid, purpose),

  CONSTRAINT ppe_weights_assessment_exists
    FOREIGN KEY (assessmentid) REFERENCES ppe_assessments(assessmentid),

  --CONSTRAINT ppe_weights_event_exists
  --  FOREIGN KEY (event) REFERENCES ppe_events(event),

  CONSTRAINT ppe_weights_purpose_exists
    FOREIGN KEY (purpose) REFERENCES ppe_purposes(purpose),

  weight INTEGER NOT NULL, -- A weight in the range [1, N] (where N is not necessarily bound)
  suitability DOUBLE DEFAULT 100.0,
  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


---------------------------------------------------------------
-- Source that may be used when allocating IDs
-- (instead of having to depend on synthetic keys)
--
CREATE TABLE ppe_unique_keystore (
	lastkey INTEGER NOT NULL DEFAULT 1000
);

--
-- Allocator-procedure
--
CREATE PROCEDURE ppe_allocate_id(IN value_count INTEGER,
OUT first_id INTEGER, OUT last_id INTEGER)
PARAMETER STYLE JAVA MODIFIES SQL DATA LANGUAGE JAVA
EXTERNAL NAME 'eu.ensure.ltu.ppe.derby.StoredProcedure.allocateId';


--
--CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.language.logQueryPlan', 'false');
--