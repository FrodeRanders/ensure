---------------------------------------------------------------
-- Copyright (C) 2012-2014 Luleå University of Technology
-- All rights reserved.
---------------------------------------------------------------

-- Completeness: The extent to which data are of sufficient 
-- breadth, depth, and scope for the task at hand [Wang&Strong]
--
-- Reputation: The extent to which data are trusted or highly
-- regarded in terms of their source or content [Wang&Strong]
--
-- Believability: The extent to which data are accepted or 
-- regarded as true, real, and credible [Wang&Strong]. 
-- The extent to which information appears to be plausible [Kelton]
--
-- Accuracy: The extent to which data are correct, reliable,
-- and certified free of error. [Wang&Strong]
--
-- Interpretability: The extent to which data are in appropriate 
-- language and units and the data definitions are clear [Wang&Strong]
--
-- Availability: The extent to which information is understandable 
-- to its users, say, by having a format that supports some use.
--
-- Accessibility: The extent to which data are available or easily 
-- and quickly retrievable [Wang&Strong] e.g. existence of finding 
-- aids, semantic search facilities, etc. Also having structural 
-- support for finding or support for searching [Liew&Schubert]. 
-- Related to reconstructability, when emulation or virtualization 
-- tools are needed in order to render some meaning. 
-- Related to predictability/stability of information over time, i.e. 
-- being up-to-date with respect to external factors (e.g. currencies 
-- etc). 
-- Further related to representational consistency where data uses
-- consistent formats and is compatible with previous data. This is
-- overall related to understandability, i.e. where the level of 
-- pre-knowledge among users matches the information.
--
-- Compatibility: Compatibility of data with user's needs, i.e. 
-- adaptability of data to user's needs such as anonymizing data 
-- (if privacy-protected). Related to format and it's appropriateness
-- with the purpose of use. Also related to ease of operation, 
-- where information is easily manageable and manipulateable 
-- [Kahn, Pipino].
--
-- Flexibility: The extent to which information is expandable, 
-- adaptable, and easily applied to other needs.
--
-- Validity: Information is verifiable as true and satisfies 
-- appropriate standards. 
--
-- Relevancy: Applicable and helpful for task at hand [Kahn et al; 
-- Pipino et al; Wang&Strong], applicable for resolving user's 
-- needs [Pierce]. Highly subjective and related to purpose. 
-- Deals with fitness for use ~ quality.


---------------------------------------------------------------
-- Known purposes
--
INSERT INTO ppe_purposes (purpose) VALUES ('evidence');
INSERT INTO ppe_purposes (purpose) VALUES ('diagnostic');
INSERT INTO ppe_purposes (purpose) VALUES ('research');
INSERT INTO ppe_purposes (purpose) VALUES ('historic');
INSERT INTO ppe_purposes (purpose) VALUES ('runtime');


---------------------------------------------------------------
-- Known events
--
INSERT INTO ppe_events (event) VALUES ('ON_START');
INSERT INTO ppe_events (event) VALUES ('ON_INGEST');
INSERT INTO ppe_events (event) VALUES ('ON_ERROR');
INSERT INTO ppe_events (event) VALUES ('ON_SEARCH');
INSERT INTO ppe_events (event) VALUES ('ON_ACCESS');
INSERT INTO ppe_events (event) VALUES ('ON_END');
INSERT INTO ppe_events (event) VALUES ('ON_TIME');
INSERT INTO ppe_events (event) VALUES ('ON_TRANSFORM');


---------------------------------------------------------------
-- 3. ORGANIZATIONAL INFRASTRUCTURE (TDR)
-- Dimension:            nr          max 
-- *integrity             1          (10)
-- *responsibility        1           (6)
-- *effectiveness         1           (5)
-- *understandability     1           (4)
-- *sustainability        2           (2)
-- *preparedness          1           (2)
-- *responsiveness        1           (2)
-- *accountability        1           (1)
-- *viability             1           (1)
-- *suitability           1           (1)

-- 3.1 GOVERNANCE AND ORGANIZATIONAL VIABILITY


INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.1.1', 'TDR', 'The repository shall have a mission statement that reflects a commitment to the preservation of, long term retention of, management of, and access to digital information');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.1.2', 'TDR', 'The repository shall have a Preservation Strategic Plan that defines the approach the repository will take in the long-term support of its mission');
--Dimension: responsibility, sustainability
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-3.1.2.1', 'TDR', 'hasContingencyPlan', 'Assesses responsibility and sustainability of organization', 'The repository shall have an appropriate succession plan, contingency plans, and/or escrow arrangements in place in case the repository ceases to operate or the governing or funding institution substantially changes its scope');

--Dimension: preparedness, viability
INSERT INTO ppe_assessments (assessmentid, source, description, motivation)
  VALUES ('TDR-3.1.2.2', 'TDR', 'Assesses preparedness and viability of organization', 'The repository shall monitor its organizational environment to determine when to execute its succession plan, contingency plans, and/or escrow arrangements');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.1.3', 'TDR', 'The repository shall have a Collection Policy or other document that specifies the type of information it will preserve, retain, manage, and provide access to');


---------------------------------------------------------------
-- 3.2 ORGANIZATIONAL STRUCTURE AND STAFFING

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-3.2.1', 'TDR', 'staffSkillsRating', 'The repository shall have identified and established the duties that it needs to perform and shall have appointed staff with adequate skills and experience to fulfill these duties');
--Dimension: sustainability
INSERT INTO ppe_assessments (assessmentid, source, description, motivation)
  VALUES ('TDR-3.2.1.1', 'TDR', 'Assesses sustainability', 'The repository shall have identified and established the duties that it needs to perform');

--Dimension: effectiveness, suitability
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-3.2.1.2', 'TDR', 'hasAppropriateNrOfStaff', 'Assesses effectiveness and suitability', 'The repository shall have the appropriate number of staff to support all functions and services');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.2.1.3', 'TDR', 'The repository shall have in place an active professional development program that provides staff with skills and expertise development opportunities');


---------------------------------------------------------------
-- 3.3 PROCEDURAL ACCOUNTABILITY AND PRESERVATION POLICY FRAMEWORK

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-3.3.1', 'TDR', 'definedDesignatedCommunity', 'The repository shall have defined its Designated Community and associated knowledge base(s) and shall have these definitions appropriately accessible');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-3.3.2', 'TDR', 'hasDevelopmentReviewPolicies', 'Assesses understandability', 'The repository shall have Preservation Policies in place to ensure its Preservation Strategic Plan will be met');

--Dimension: responsiveness
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-3.3.2.1', 'TDR', 'hasMechanismsForReviewPreservationPolicies', 'Assesses responsiveness', 'The repository shall have mechanisms for review, update, and ongoing development of its Preservation Policies as the repository grows and as technology and community practice evolve');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-3.3.3', 'TDR', 'trackHistoryOfChanges', 'The repository shall have a documented history of the changes to its operations, procedures, software, and hardware');

--Dimension: accountability
INSERT INTO ppe_assessments (assessmentid, source, description, motivation)
  VALUES ('TDR-3.3.4', 'TDR', 'Assesses accountability', 'The repository shall commit to transparency and accountability in all actions supporting the operation and management of the repository that affect the preservation of digital content over time');

--Dimension: integrity
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-3.3.5', 'TDR', 'hasIntegrityMeasurement', 'Assesses integrity', 'The repository shall define, collect, track, and appropriately provide its information integrity measurements');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-3.3.6', 'TDR', 'hasRegularSelfAssessmentAndExternalCertification', 'Assesses trustworthiness', 'The repository shall commit to a regular schedule of self-assessment and external certification');


---------------------------------------------------------------
-- 3.4 FINANCIAL SUSTAINABILITY

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.4.1', 'TDR', 'The repository shall have short- and long-term business planning processes in place to sustain the repository over time');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.4.2', 'TDR', 'The repository shall have financial practices and procedures which are transparent, compliant with relevant accounting standards and practices, and audited by third parties in accordance with territorial legal requirements');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.4.3', 'TDR', 'The repository shall have an ongoing commitment to analyze and report on financial risk, benefit, investment, and expenditure (including assets, licenses, and liabilities)');


---------------------------------------------------------------
-- 3.5 CONTRACTS, LICENSES, AND LIABILITIES

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.5.1', 'TDR', 'The repository shall have and maintain appropriate contracts or deposit agreements for digital materials that it manages, preserves, and/or to which it provides access');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.5.1.1', 'TDR', 'The repository shall have contracts or deposit agreements which specify and transfer all necessary preservation rights, and those rights transferred shall be documented');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.5.1.2', 'TDR', 'The repository shall have specified all appropriate aspects of acquisition, maintenance, access, and withdrawal in written agreements with depositors and other relevant parties');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.5.1.3', 'TDR', 'The repository shall have written policies that indicate when it accepts preservation responsibility for contents of each set of submitted data objects');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.5.1.4', 'TDR', 'The repository shall have policies in place to address liability and challenges to ownership/rights');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-3.5.2', 'TDR', 'The repository shall track and manage intellectual property rights and restrictions on use of repository content as required by deposit agreement, contract, or license');


---------------------------------------------------------------
-- 4. DIGITAL OBJECT MANAGEMENT (TDR)
-- Dimension:            nr         max  
-- *integrity             4         (10)
-- *responsibility        5          (6)
-- *effectiveness         3          (5)
-- *understandability     3          (4)
-- *correctness           4          (4)
-- *completeness          4          (4)
-- *authenticity          3          (3)
-- *traceability          1          (3)
-- *responsiveness        1          (2)
-- *visibility            1          (1)
-- *usability             1          (1)

-- 4.1 INGEST: ACQUISITION OF CONTENT

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.1.1', 'TDR', 'hasMissionStatementForSignificantProperties', 'The repository shall identify the Content Information and the Information Properties that the repository will preserve');

--Dimension: responsibility, authenticity
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.1.1.1', 'TDR', 'hasIdentifyingProceduresForSignificantProperties', 'Assesses authenticity and responsibility during ingest', 'The repository shall have a procedure(s) for identifying those Information Properties that it will preserve');

--Dimension: effectiveness, responsibility
INSERT INTO ppe_assessments (assessmentid, source, description, motivation)
  VALUES ('TDR-4.1.1.2', 'TDR', 'Assesses effectiveness and responsibility during ingest', 'The repository shall have a record of the Content Information and the Information Properties that it will preserve');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.1.2', 'TDR', 'hasSipAgreement', 'The repository shall clearly specify the information that needs to be associated with specific Content Information at the time of its deposit');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-4.1.3', 'TDR', 'The repository shall have adequate specifications enabling recognition and parsing of the SIPs');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.1.4', 'TDR', 'hasMechanismForAuthentication', 'The repository shall have mechanisms to appropriately verify the identity of the Producer of all materials');

--Dimension: correctness, completeness 
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.1.5', 'TDR', 'hasMechanismForSipVerification', 'Assesses completeness and correctness during ingest', 'The repository shall have an ingest process which verifies each SIP for completeness and correctness');

--Dimension: integrity
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.1.6', 'TDR', 'hasMechanismForSipControll', 'Assesses integrity during ingest', 'The repository shall obtain sufficient control over the Digital Objects to preserve them');

--Dimension: correctness, completeness
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.1.7', 'TDR', 'progressReportToProducer', 'Assesses completeness and correctness during ingest', 'The repository shall provide the producer/depositor with appropriate responses at agreed points during the ingest processes');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.1.8', 'TDR', 'sipCreation', 'The repository shall have contemporaneous records of actions and administration processes that are relevant to content acquisition');

---------------------------------------------------------------
-- 4.2 INGEST: CREATION OF THE AIP

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.1', 'TDR', 'hasAipClassStructureSpecification', 'The repository shall have for each AIP or class of AIPs preserved by the repository an associated definition that is adequate for parsing the AIP and fit for long- term preservation needs');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.1.1', 'TDR', 'ableIndentifyDefinitionToAip', 'The repository shall be able to identify which definition applies to which AIP');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.1.2', 'TDR', 'hasDefinitionForEachAip', 'The repository shall have a definition of each AIP that is adequate for long- term preservation, enabling the identification and parsing of all the required components within that AIP');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.2', 'TDR', 'hasDescFromSipToAip', 'The repository shall have a description of how AIPs are constructed from SIPs');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.3', 'TDR', 'hasDocumentFinalDispositionAllSip', 'The repository shall document the final disposition of all SIPs');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.3.1', 'TDR', 'hasDocProceduresForNotIncorporatedSipToAip', 'The repository shall follow documented procedures if a SIP is not incorporated into an AIP or discarded and shall indicate why the SIP was not incorporated or discarded');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.4', 'TDR', 'generateUniqueAipIdentifier', 'The repository shall have and use a convention that generates persistent, unique identifiers for all AIPs');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.4.1', 'TDR', 'shallUniquelyIdentifyEachAip', 'The repository shall uniquely identify each AIP within the repository');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.4.1.1', 'TDR', 'hasUniqueIdentifier', 'The repository shall have unique identifiers');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.4.1.2', 'TDR', 'shallAssignAndMaintainPersistentIdentifier', 'The repository shall assign and maintain persistent identifiers of the AIP and its components so as to be unique within the context of the repository');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.4.1.3', 'TDR', 'hasDocumentForChangesIdentifier', 'Documentation shall describe any processes used for changes to such identifiers');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.4.1.4', 'TDR', 'hasComleteListOfIdentifier', 'The repository shall be able to provide a complete list of all such identifiers and do spot checks for duplications');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.4.1.5', 'TDR', 'hasAdequateIdentifierForTheSystem', 'The system of identifiers shall be adequate to fit the repository’s current and foreseeable future requirements such as numbers of objects');

--Dimension: visibility 
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.2.4.2', 'TDR', 'hasReliableResolutionServiceForFindIdentifiedObject', 'Assesses visibility and traceability of AIP creation during ingest', 'The repository shall have a system of reliable linking/resolution services in order to find the uniquely identified object, regardless of its physical location');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.5', 'TDR', 'generateUniqueAipIdentifier', 'The repository shall have access to necessary tools and resources to provide authoritative Representation Information for all of the digital objects it contains');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.5.1', 'TDR', 'hasToolsMethodsForIdentifyFileInDataObject', 'The repository shall have tools or methods to identify the file type of all submitted Data Objects');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.5.2', 'TDR', 'hasToolsMethodsToDetermineRiForDataObject', 'The repository shall have tools or methods to determine what Representation Information is necessary to make each Data Object understandable to the Designated Community');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.5.3', 'TDR', 'hasAccessToRequisiteRepresentationInformation', 'The repository shall have access to the requisite Representation Information');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.5.4', 'TDR', 'hasToolsMethodsToEnsureRiIsAssociatedToDO', 'The repository shall have tools or methods to ensure that the requisite Representation Information is persistently associated with the relevant Data Objects');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.6', 'TDR', 'hasMechanismForAcquiringPreservationMetadata', 'The repository shall have documented processes for acquiring Preservation Description Information (PDI) for its associated Content Information and acquire PDI in accordance with the documented processes');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.6.1', 'TDR', 'hasDocumentedProcessForAcquiringPDI', 'The repository shall have documented processes for acquiring PDI');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.6.2', 'TDR', 'shallExecuteProcessessForAcquiringPDI', 'The repository shall execute its documented processes for acquiring PDI');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.6.3', 'TDR', 'shallEnsurePDIAssociatedWithContentInformation', 'The repository shall ensure that the PDI is persistently associated with the relevant Content Information');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.7', 'TDR', 'repositoryEnsureContentInformationofTheAip', 'The repository shall ensure that the Content Information of the AIPs is understandable for their Designated Community at the time of creation of the AIP');

--Dimension: understandability
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.2.7.1', 'TDR', 'hasMechanismForTestingUnderstandabilityOfContentInfo', 'Assesses understandability of AIP creation during ingest', 'Repository shall have a documented process for testing understandability for their Designated Communities of the Content Information of the AIPs at their creation');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.2.7.2', 'TDR', 'hasMechanismForExecuteTheTestingOfContentInfoAip', 'The repository shall execute the testing process for each class of Content Information of the AIPs');

--Dimension: understandability
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.2.7.3', 'TDR', 'hasMechanismForBringContentInfoOfAipToRequiredLevel', 'Assesses understandability and intelligibility of AIP creation during ingest', 'The repository shall bring the Content Information of the AIP up to the required level of understandability if it fails the understandability testing');

--Dimension: correctness, completeness
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.2.8', 'TDR', 'hasMechanismsForAipVerification', 'Assesses completeness and correctness of AIP creation during ingest', 'The repository shall verify each AIP for completeness and correctness at the point it is created');

--Dimension: integrity, correctness, completeness
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.2.9', 'TDR', 'hasMechanismForVerifyingIntegrityOfTheRepositoryCollection', 'Assesses integrity, responsibility, completeness and correctness of AIP creation during ingest', 'The repository shall provide an independent mechanism for verifying the integrity of the repository collection/content');

--Dimension: responsibility
INSERT INTO ppe_assessments (assessmentid, source, description, motivation)
  VALUES ('TDR-4.2.10', 'TDR', 'Assesses responsibility of AIP creation during ingest', 'The repository shall have contemporaneous records of actions and administration processes that are relevant to AIP creation');

---------------------------------------------------------------
-- 4.3 PRESERVATION PLANNING

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.3.1', 'TDR', 'hasPreservationStrategies', 'The repository shall have documented preservation strategies relevant to its holdings');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.3.2', 'TDR', 'hasMechanismForMonitorPreservationEnvironment', 'The repository shall have mechanisms in place for monitoring its preservation environment');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.3.2.1', 'TDR', 'hasMechanismForMonitorRI', 'The repository shall have mechanisms in place for monitoring and notification when Representation Information is inadequate for the Designated Community to understand the data holdings');

--Dimension: effectiveness
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.3.3', 'TDR', 'hasMechanismForChangePreservationPlan', 'Assesses effectiveness of preservation planning', 'The repository shall have mechanisms to change its preservation plans as a result of its monitoring activities');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.3.3.1', 'TDR', 'hasMechanismForCreatingIdentifyingExtraRI', 'The repository shall have mechanisms for creating, identifying or gathering any extra Representation Information required');

--Dimension: effectiveness, understandability, usability
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.3.4', 'TDR', 'hasEvidenceOfEffectivenessOfItsPreservationActivities', 'Assesses usability, effectiveness and understandability of preservation planning', 'The repository shall provide evidence of the effectiveness of its preservation activities');

---------------------------------------------------------------
-- 4.4 AIP PRESERVATION

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.4.1', 'TDR', 'hasSpecificationHowAipStored', 'The repository shall have specifications for how the AIPs are stored down to the bit level');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.4.1.1', 'TDR', 'preserveContentInformationOfAip', 'The repository shall preserve the Content Information of AIPs');

--Dimension: integrity
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.4.1.2', 'TDR', 'monitorAipIntegrity', 'Assesses integrity of AIP preservation', 'The repository shall actively monitor the integrity of AIPs');

--Dimension: authenticity
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.4.2', 'TDR', 'hasRecordOfActionAndAdministrationProcessToStorageOfAip', 'Assesses authenticity of AIP preservation', 'The repository shall have contemporaneous records of actions and administration processes that are relevant to storage and preservation of the AIPs');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.4.2.1', 'TDR', 'hasMechanismForActionTakenOnAip', 'The repository shall have procedures for all actions taken on AIPs');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-4.4.2.2', 'TDR', 'The repository shall be able to demonstrate that any actions taken on AIPs were compliant with the specification of those actions');

---------------------------------------------------------------
-- 4.5 INFORMATION MANAGEMENT

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-4.5.1', 'TDR', 'The repository shall specify minimum information requirements to enable the Designated Community to discover and identify material of interest');

--Dimension: responsibility
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.5.2', 'TDR', 'shallCaptureDescInfoAssociatedWithTheAip', 'Assesses responsibility of information management', 'The repository shall capture or create minimum descriptive information and ensure that it is associated with the AIP');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.5.3', 'TDR', 'shallMaintainLinkageBetweenAipAndDescInfo', 'The repository shall maintain bi-directional linkage between each AIP and its descriptive information');

--Dimension: integrity
INSERT INTO ppe_assessments (assessmentid, source, description, motivation)
  VALUES ('TDR-4.5.3.1', 'TDR', 'Assesses integrity', 'The repository shall maintain the associations between its AIPs and their descriptive information over time');

---------------------------------------------------------------
-- 4.6 ACCESS MANAGEMENT

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.6.1', 'TDR', 'hasAccessPolicies', 'Assesses trustworthiness of access management', 'The repository shall comply with Access Policies');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-4.6.1.1', 'TDR', 'hasUnusualDenialReviewMechanism', 'The repository shall log and review all access management failures and anomalies');

--Dimension: authenticity
INSERT INTO ppe_assessments (assessmentid, source, description, motivation)
  VALUES ('TDR-4.6.2', 'TDR', 'Assesses authenticity', 'The repository shall follow policies and procedures that enable the dissemination of digital objects that are traceable to the originals, with evidence supporting their authenticity');

--Dimension: responsiveness
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-4.6.2.1', 'TDR', 'hasRequestResponseMechanism', 'Assesses responsiveness and trustworthiness of access management', 'The repository shall record and act upon problem reports about errors in data or responses from users');


---------------------------------------------------------------
-- 5. INFRASTRUCTURE AND SECURITY RISK MANAGEMENT (TDR)
-- Dimension:            nr               max
-- *integrity             5               (10)
-- *effectiveness         1                (5)
-- *traceability          2                (3)
-- *preparedness          1                (2)
-- *availability          1                (1)

-- 5.1 TECHNICAL INFRASTRUCTURE RISK MANAGEMENT

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-5.1.1', 'TDR', 'The repository shall identify and manage the risks to its preservation operations and goals associated with system infrastructure');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-5.1.1.1', 'TDR', 'The repository shall employ technology watches or other technology monitoring notification systems');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.1.1.1.1', 'TDR', 'designatedHardwareToCommunityService', 'The repository shall have hardware technologies appropriate to the services it provides to its designated communities');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.1.1.1.2', 'TDR', 'userNeedsEvaluation', 'The repository shall have procedures in place to monitor and receive notifications when hardware technology changes are needed');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.1.1.1.3', 'TDR', 'hasHardwareInventory', 'The repository shall have procedures in place to evaluate when changes are needed to current hardware');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.1.1.1.4', 'TDR', 'hasProceduresForReplaceHardware', 'The repository shall have procedures, commitment and funding to replace hardware when evaluation indicates the need to do so');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.1.1.1.5', 'TDR', 'hasAppropriateSoftwareService', 'The repository shall have software technologies appropriate to the services it provides to its designated communities');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.1.1.1.6', 'TDR', 'hasMechanismToMonitorSoftwareChanges', 'The repository shall have procedures in place to monitor and receive notifications when software changes are needed');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.1.1.1.7', 'TDR', 'hasProceduresToEvaluateChangesToCurrentSoftware', 'The repository shall have procedures in place to evaluate when changes are needed to current software');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.1.1.1.8', 'TDR', 'hasProceduresToReplaceSoftware', 'The repository shall have procedures, commitment, and funding to replace software when evaluation indicates the need to do so');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.1.1.2', 'TDR', 'hasHardwareAndSoftwareBackup', 'The repository shall have adequate hardware and software support for backup functionality sufficient for preserving the repository content and tracking repository functions');

--Dimension: integrity, effectiveness
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-5.1.1.3', 'TDR', 'hasBitErrorDetectionMechanisms', 'Assesses effectiveness and integrity of technical infrastructure', 'The repository shall have effective mechanisms to detect bit corruption or loss');

--Dimension: integrity
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-5.1.1.3.1', 'TDR', 'progressReportToProduce', 'Assesses integrity of technical infrastructure', 'The repository shall record and report to its administration all incidents of data corruption or loss, and steps shall be taken to repair/replace corrupt or lost data');

--Dimension: integrity, availability
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-5.1.1.4', 'TDR', 'hasRiskAnalysisForSecurityPatches', 'Assesses availability and integrity of technical infrastructure', 'The repository shall have a process to record and react to the availability of new security updates based on a risk-benefit assessment');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.1.1.5', 'TDR', 'triggeringEventMaintAction', 'The repository shall have defined processes for storage media and/or hardware change (e.g., refreshing, migration)');

--Dimension: traceability
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-5.1.1.6', 'TDR', 'hasDocumentedProcedureForCriticalProcesses', 'Assesses ability and traceability of technical infrastructure', 'The repository shall have identified and documented critical processes that affect its ability to comply with its mandatory responsibilities');

--Dimension: traceability
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-5.1.1.6.1', 'TDR', 'hasChangeMngmntForCriticalProcesses', 'Assesses traceability of technical infrastructure', 'The repository shall have a documented change management process that identifies changes to critical processes that potentially affect the repository’s ability to comply with its mandatory responsibilities');

--Dimension: integrity
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-5.1.1.6.2', 'TDR', 'hasCriticalChangesTestProcedures', 'Assesses integrity of technical infrastructure', 'The repository shall have a process for testing and evaluating the effect of changes to the repository’s critical processes');

--Dimension: integrity
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-5.1.2', 'TDR', 'shallManageNumberAndLocationOfCopiesOfAllDO', 'Assesses integrity of technical infrastructure', 'The repository shall manage the number and location of copies of all digital objects');

INSERT INTO ppe_assessments (assessmentid, source, motivation)
  VALUES ('TDR-5.1.2.1', 'TDR', 'The repository shall have mechanisms in place to ensure any/multiple copies of digital objects are synchronized');

---------------------------------------------------------------
-- 5.2 SECURITY RISK MANAGEMENT

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-5.2.1', 'TDR', 'securityCertification', 'Assesses liability of risk management', 'The repository shall maintain a systematic analysis of security risk factors associated with data, systems, personnel, and physical plant');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.2.2', 'TDR', 'hasRiskAssessmentControls', 'The repository shall have implemented controls to adequately address each of the defined security risks');

INSERT INTO ppe_assessments (assessmentid, source, mnemonic, motivation)
  VALUES ('TDR-5.2.3', 'TDR', 'hasDocumentedSystemAuthorization', 'The repository staff shall have delineated roles, responsibilities, and authorizations related to implementing changes within the system');

--Dimension: preparedness
INSERT INTO ppe_assessments (assessmentid, source, mnemonic, description, motivation)
  VALUES ('TDR-5.2.4', 'TDR', 'hasDisasterAndSystemRecoveryPlan', 'Assesses preparedness of risk management', 'The repository shall have suitable written disaster preparedness and recovery plan(s), including at least one off-site backup of all preserved information together with an offsite copy of the recovery plan(s)');


---------------------------------------------------------------
-- Some weights
--
-- purpose: evidence
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.1.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2.1', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2.2', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.1.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.2.1', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.2.1.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.2.1.2', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.2.1.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.2.1', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.4', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.5', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.6', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.4', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.4', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.5', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.6', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.7', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.8', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.3.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.2.4.1.1', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.2.4.1.2', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.4', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.5', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.2.4.2', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.4', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.8', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.9', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.10', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.2.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.3.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.4', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.3.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.6.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.6.1.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.6.2', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.6.2.1', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.1', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.1.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.3', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.4', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.5', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.1.6', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.7', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.8', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.2', 'evidence', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.3.1', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.4', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.5', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6.1', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6.2', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.2', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.2.1', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.1', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.2', 'evidence', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.2.3', 'evidence', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.4', 'evidence', 5, 50.0);

-- purpose: diagnostic
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.1.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2.1', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2.2', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.1.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.2.1', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.2.1.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.2.1.2', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.2.1.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.2.1', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.4', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.5', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.6', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.4', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.4', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.5', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.6', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.7', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.8', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.3.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.2.4.1.1', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.2.4.1.2', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.4', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.5', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.2.4.2', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.4', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.8', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.9', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.10', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.2.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.3.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.4', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.3.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.6.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.6.1.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.6.2', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.6.2.1', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.1', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.1.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.3', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.4', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.5', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.1.6', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.7', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.8', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.2', 'diagnostic', 5, 50.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.3.1', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.4', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.5', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6.1', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6.2', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.2', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.2.1', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.1', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.2', 'diagnostic', 3, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.2.3', 'diagnostic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.4', 'diagnostic', 5, 50.0);

-- purpose: research
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.1.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2.1', 'research', 5, 60.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2.2', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.1.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.2.1', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.2.1.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.2.1.2', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.2.1.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.2.1', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.4', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.5', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.6', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.4', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.4', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.5', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.6', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.7', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.8', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.3.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.2.4.1.1', 'research', 5, 60.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.2.4.1.2', 'research', 5, 60.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.4', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.5', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.2.4.2', 'research', 5, 60.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.4', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.8', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.9', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.10', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.2.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.3.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.4', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.3.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.6.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.6.1.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.6.2', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.6.2.1', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1', 'research', 5, 60.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.1', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.1.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.3', 'research', 5, 60.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.4', 'research', 5, 60.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.5', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.1.6', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.7', 'research', 5, 60.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.8', 'research', 5, 60.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.2', 'research', 5, 60.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.3.1', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.4', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.5', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6.1', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6.2', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.2', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.2.1', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.1', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.2', 'research', 3, 80.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.2.3', 'research', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.4', 'research', 5, 60.0);

-- purpose: historic
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.1.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2.1', 'historic', 5, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.1.2.2', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.1.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.2.1', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.2.1.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.2.1.2', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.2.1.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.2.1', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.3.4', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.5', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-3.3.6', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.4.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.1.4', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-3.5.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.1.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.4', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.5', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.6', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.7', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.1.8', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.1.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.3.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.1', 'historic', 5);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.2', 'historic', 5);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.4', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.4.1.5', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.2.4.2', 'historic', 5, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.5.4', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.6.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.7.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.8', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.9', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.2.10', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.2.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.3.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.3.4', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.1.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.4.2.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.5.3.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.6.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-4.6.1.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.6.2', 'historic', 2, 95.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-4.6.2.1', 'historic', 2, 95.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1', 'historic', 5, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.1', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.1.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.3', 'historic', 5, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.4', 'historic', 5, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.5', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.1.6', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.7', 'historic', 5, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.1.8', 'historic', 5, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.2', 'historic', 5, 75.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.3.1', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.4', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.1.5', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6.1', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.1.1.6.2', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.2', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.1.2.1', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.1', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.2', 'historic', 3, 90.0);
INSERT INTO ppe_weights (assessmentid, purpose, weight) VALUES ('TDR-5.2.3', 'historic', 1);
INSERT INTO ppe_weights (assessmentid, purpose, weight, suitability) VALUES ('TDR-5.2.4', 'historic', 5, 75.0);

