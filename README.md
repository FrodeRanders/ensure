ENSURE Open Source Offering
===========================

Context
-------
These are Open Source software components developed during the course of the ENSURE EU
project.

The ENSURE project set out to solve a set of problems related to long-term preservation of digital information. Among the problems to address were decision support to commercial customers for selecting a suitable preservation plan. Suitability then, is defined as a balance in cost, economic performance (such as Return On Investment) and quality. Overall, the ENSURE project defined an architecture for building commercial long-term digital preservation services and developed software to this end.

Several partners were cooperating in this effort: 
 - IBM (IBM Israel - Science and Technology LTD) developed an abstraction layer over cloud services that made it possible to utilize cloud storage and compute facilities from different providers - even in the same setup as defined by a preservation plan, with functionality for moving data and running software close to the data (storelets).
 - Tessella (Tessella plc) developed the preservation runtime infrastructure, that dynamically instantiates a workflow for individual information packages as they enter the system. They also developed the monitoring function of the preservation service by implementing an augmented version of PRONOM based on linked data and collaboration.
 - Custodix developed an authentication and authorization component that implemented the security policy of the preservation service. This included anonymization services according to policy.
 - Fraunhofer (Fraunhofer-Institut Biomedizinische Technik) developed the ingest and access functions of the preservation service, particularly implementing search functionality on top of an ontology store. In light of the problem domain, even these ontologies have to be maintained over time as the ontologies themselves evolve.
 - Philips (Royal Philips Electronics) implemented the optimizing software, using genetic algorithms, to balance cost, economic performance and quality. Philips also developed various storelets, running in the designated cloud and close to the information package, that (among other things) allowed "zooming" in digital pathology DICOM data with the effect of drastically minimizing the overhead communication time and cost of accessing archived DICOM data.
 - Atos (Atos Origin SAE) developed the preservation plan configurator, that provided a graphical user interface to the system for configuring and selecting preservation plans and also coordinated the invokation of the calculation engines (cost, economic performance, and quality).
 - STFC (Science and Technologies Facility Council) developed the rule set for creating the initial preservation plans.
 - Cranfield University developed the cost and economic performance engines, responsible for calculating cost and performance metrics for the optimizer to peruse.
 - Luleå University of Technology developed the quality engine, responsible for assessing the applicability (i.e. the quality) of individual preservation plans to the ends indicated by the customer.
 - University of Porto (Universidade do Porto) were supporting our efforts.
 - Maccabi (Maccabi Health Services) provided healthcare journal and digital pathology use cases.
 - JRC (JRC Capital Management Consultancy & Research GmbH) provided financial use cases.
 - FISABIO (Fundación para el Fomento de la Investigación Sanitaria y Biomédica de la Comunitat Valenciana) provided clinical studies use cases.
   
Contents of Open Source offering
--------------------------------
This GitHub project contains the software developed by Luleå University of Technology in the course of the ENSURE project and mainly addresses these two functions.

1. Software for general processing of packaged files - which may have the most applicability for general re-use outside of ENSURE.
   
2. Software for assessing quality of preservation plans.

The latter component assumes a structure of a preservation plan as laid out 
[here](./doc/QualityEnginePlanModel.png)

The former component does have implicit support for processing DICOM images packaged using the [XFDU](http://www.dcc.ac.uk/resources/external/xml-formatted-data-unit-xfdu) format, but it is in fact much more general. Among the examples are processing files in a file system (where the file system may be considered being the package) ([code](./packproc/packproc-fs/src/test/java/eu/ensure/packproc/ProcessingTest.java) and [configuration](./packproc/packproc-fs/src/test/resources/eu/ensure/packproc/filesystem-processing-configuration.xml)), processing files in TAR (which often is used as the wrapping file format for information packages in a preservation context) or ZIP files.

Maven is used for managing external dependencies and building (and testing) the software, which facilitates building the software stand alone as well as importing the software into your favourite IDE.  


   