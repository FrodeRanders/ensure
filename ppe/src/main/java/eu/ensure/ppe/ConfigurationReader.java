/*
 * Copyright (C) 2012-2014 Frode Randers
 * All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The research leading to the implementation of this software package
 * has received funding from the European Community´s Seventh Framework
 * Programme (FP7/2007-2013) under grant agreement n° 270000.
 *
 * Frode Randers was at the time of creation of this software module
 * employed as a doctoral student by Luleå University of Technology
 * and remains the copyright holder of this material due to the
 * Teachers Exemption expressed in Swedish law (LAU 1949:345)
 */
package eu.ensure.ppe;

import eu.ensure.vopn.xml.Attribute;
import eu.ensure.vopn.xml.Namespaces;
import eu.ensure.vopn.xml.XPath;
import eu.ensure.vopn.xml.XmlException;
import eu.ensure.ppe.model.*;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.NumberFormat;
import java.util.*;

/**
 * Reads Global Preservation Plans.
 * <p>
 * Created by Frode Randers at 2012-09-13 14:34
 */
public class ConfigurationReader {
    private static final Logger log = LogManager.getLogger(ConfigurationReader.class);

    private static final String ENSURE = "ENSURE";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String DELETION = "DELETION";
    private static final String STORAGE = "STORAGE";
    private static final String COMPUTE = "COMPUTE";
    private static final String USES = "USES";


    private static NumberFormat dec2Format = NumberFormat.getNumberInstance();

    private ConfigurationReader() {}

    public static ConfigurationReader getInstance() {
        return new ConfigurationReader();
    }

    // ==================================================================================

    public Map<String, Plugin> readPluginConfiguration(
            ReadableByteChannel inputChannel, Namespaces namespaces
    ) throws XMLStreamException, XmlException, EvaluationException {
        // XML document to operate on
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(Channels.newReader(inputChannel, "utf-8"));

        StAXOMBuilder builder = new StAXOMBuilder(reader);
        OMElement pluginsConfig = builder.getDocumentElement();

        // Study configuration
        Map<String, Plugin> pluginMap = readPluginConfiguration(pluginsConfig, namespaces);
        return pluginMap;
    }

    public PreservationPlan readAggregations(
            ReadableByteChannel inputChannel, Namespaces namespaces, Map<String, Plugin> pluginMap
    ) throws XMLStreamException, XmlException, EvaluationException {
        // XML document to operate on
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(Channels.newReader(inputChannel, "utf-8"));

        StAXOMBuilder builder = new StAXOMBuilder(reader);
        OMElement aggregations = builder.getDocumentElement();

        PreservationPlan customer = readAggregations(aggregations, namespaces, pluginMap);
        return customer;
    }

    public void readRequirements(
            ReadableByteChannel inputChannel, Namespaces namespaces, PreservationPlan preservationPlan
    ) throws XMLStreamException, XmlException, EvaluationException {
        // XML document to operate on
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(Channels.newReader(inputChannel, "utf-8"));

        StAXOMBuilder builder = new StAXOMBuilder(reader);
        OMElement requirementSet = builder.getDocumentElement();

        readRequirements(requirementSet, namespaces, preservationPlan);
    }


    /*
     *  ---------------------------------------------------------------------------------
     *  Amazon Web Services (AWS) components:
     *
     *  Elastic Compute Cloud (EC2)  *
     *  E-Commerce Service (AWS)
     *  Simple Storage Service (S3)  *
     *  Simple Queue Service (SQS)
     *  SimpleDB
     *  Simple Email Service (SES)
     *
     *  ---------------------------------------------------------------------------------
     *  Rackspace components:
     *
     *  Cloud Files  *
     *  Cloud Servers  *
     *  Cloud Sites
     *
     *  ---------------------------------------------------------------------------------
     *  OpenStack components:  OBS - These may be provided by different organizations!
     *
     *  OpenStack Compute (code-name Nova)   *
     *  OpenStack Object Storage (code-name Swift)  *
     *  OpenStack Image Service (code-name Glance)
     *  OpenStack Identity (code-name Keystone)
     *  OpenStack Dashboard (code-name Horizon)
     *  OpenStack Networking (code-name Quantum)
     *  OpenStack Block Storage (code-name Cinder)
     *
     *  Metering (Ceilometer) - https://launchpad.net/ceilometer
     *  Basic Cloud Orchestration & Service Definition (Heat) - http://wiki.openstack.org/Heat
     *  ---------------------------------------------------------------------------------
     */


    // ==================================================================================

    private Calendar iso8601String2Calendar(String iso8601String) {
        return javax.xml.bind.DatatypeConverter.parseDateTime(iso8601String);
    }

    private Map<String, Plugin> readPluginConfiguration(
            OMElement gpp, Namespaces namespaces
    ) throws XmlException, EvaluationException {

        if (log.isInfoEnabled()) {
            log.info("\n<<Reading plugin configuration...>>");
        }

        Map</* plugin id */ String, Plugin> _pluginMap = new HashMap<String, Plugin>();

        namespaces.defineNamespace("http://www.w3.org/2001/XMLSchema-instance", "xsi");

        // Query objects
        XPath xpath = new XPath(namespaces);
        Attribute attribute = new Attribute(namespaces);

        //String expression = "//pluginServiceInstanceList/pluginServiceInstancesList";
        String expression = "//pluginServiceInstanceList/*";
        List<OMElement> pluginServicesInstances = xpath.getElementsFrom(gpp, expression);
        if (pluginServicesInstances.size() > 0) {
            if (log.isDebugEnabled()) {
                String info = "Found " + pluginServicesInstances.size() + " plugin service instances.";
                log.debug(info);
            }

            for (OMElement pluginServicesInstance : pluginServicesInstances) {
                //-----------------------------------------
                // 1. Read plugin service instance information
                //-----------------------------------------
                String pluginServicesInstanceId = xpath.getTextFrom(pluginServicesInstance, "id", /* accept failure? */ true);
                String serviceReference = xpath.getTextFrom(pluginServicesInstance, "serviceReference", /* accept failure? */ true);
                if (log.isInfoEnabled()) {
                    if (null == pluginServicesInstanceId || pluginServicesInstanceId.length() == 0) {
                        pluginServicesInstanceId = "";
                    }
                    String info = "> Found plugin service instance \"" + pluginServicesInstanceId + "\"";
                    if (log.isDebugEnabled() && null != serviceReference && serviceReference.length() > 0) {
                        info += " referring to \"" + serviceReference + "\"";
                    }
                    log.info(info);
                }

                String endpoint = xpath.getTextFrom(pluginServicesInstance, "endpoint", /* accept failure? */ true);
                if (log.isDebugEnabled() && null != endpoint && endpoint.length() > 0) {
                    String info = "> Endpoint for service instance is: " + endpoint;
                    log.debug(info);
                }

                //-----------------------------------------
                // 2. Read plugin service information
                //-----------------------------------------
                OMElement pluginService = xpath.getElementFrom(pluginServicesInstance, "pluginService");
                {
                    String pluginServiceId = xpath.getTextFrom(pluginService, "id");
                    String pluginServiceDescription = xpath.getTextFrom(pluginService, "description", /* accept failure? */ true);
                    if (null == pluginServiceDescription || pluginServiceDescription.length() == 0) {
                        pluginServiceDescription = UNKNOWN;
                    }
                    if (log.isInfoEnabled()) {
                        String info = ">> Found plugin service \"" + pluginServiceDescription + "\" (" + pluginServiceId + ")";
                        log.info(info);
                    }

                    //-----------------------------------------
                    // 3. Read plugin information
                    //-----------------------------------------
                    OMElement plugin = xpath.getElementFrom(pluginService, "plugin");
                    {
                        String _providerId = UNKNOWN;
                        String _systemId = UNKNOWN;
                        String pluginId = xpath.getTextFrom(plugin, "id", /* accept failure? */ true);
                        if (null == pluginId || pluginId.length() == 0) {
                            String info = "Plugin configuration element does not have a (mandatory) \"id\": " + plugin;
                            log.warn(info);
                            throw new EvaluationException(info);
                        }

                        String code = xpath.getTextFrom(plugin, "code", /* accept failure? */ true);
                        if (null == code || code.length() == 0) {
                            code = UNKNOWN;
                        }

                        String pluginDescription = xpath.getTextFrom(plugin, "description", /* accept failure? */ true);

                        // Determine plugin type, since this indicates which internal plugins we may ignore...
                        String pluginType = attribute.getValueFrom(plugin, "xsi", "type", /* accept failure? */ true); // Year 1 compatibility
                        if (null == pluginType) {
                            log.warn("Plugin with id=\"" + pluginId + "\" has unknown plugin type attribute [skipping]: " + plugin);
                            continue;
                        }

                        if (null == pluginDescription || pluginDescription.length() == 0) {
                            pluginDescription = pluginType;
                        }

                        if (log.isInfoEnabled()) {
                            String info = ">>> Found plugin \"" + pluginDescription + "\" (" + pluginId + ") of type \"" + pluginType + "\"";
                            log.info(info);
                        }

                        // Can we leave early? Is this internal plugins (belonging to ENSURE) that we don't have to calculate
                        // quality for?
                        if ("baseStorageServices".equalsIgnoreCase(pluginType)
                         || "baseIngestPlugin".equalsIgnoreCase(pluginType)
                         || "baseAccessPlugin".equalsIgnoreCase(pluginType)
                         || "pdalmPlugin".equalsIgnoreCase(pluginType)
                         || "ingestXMLTemplatePlugin".equalsIgnoreCase(pluginType)
                         || "preservationOntologyPlugin".equalsIgnoreCase(pluginType)
                         || "searchIndexStoragePlugin".equalsIgnoreCase(pluginType)) {
                            log.info(">>> Will not assess quality of plugin with type \"" + pluginType+ "\"");

                            _providerId = ENSURE;
                            _systemId = pluginType;

                            Plugin _plugin = new Plugin(pluginId, _providerId, _systemId, pluginType, pluginDescription);
                            _pluginMap.put(pluginId, _plugin);

                            continue;
                        }

                        // ------------ Determining service provider ------------
                        // First shot at determining service provider from the <provider/> element
                        /*
                        OMElement provider = xpath.getElementFrom(plugin, "provider", /* accept failure? * / true);
                        if (null == provider) {
                            String info = ">>> Plugin \"" + pluginDescription + "\" (" + pluginId + ") ";
                            info += "does not specify <provider/>";
                            log.info(info);
                        } else {
                            if (null == _providerId || _providerId.length() == 0 || UNKNOWN.equalsIgnoreCase(_providerId)) {
                                String pluginServiceProvider = provider.getText().trim();
                                log.debug(">>> Plugin service-provider is \"" + pluginServiceProvider + "\"");
                                _providerId = pluginServiceProvider;
                            }
                        }
                        */

                        // Second shot at determining service provider from the <serviceProvider/> element
                        OMElement provider = xpath.getElementFrom(plugin, "serviceProvider", /* accept failure? */ true);
                        if (null == provider) {
                            String info = ">>> Plugin \"" + pluginDescription + "\" (" + pluginId + ") ";
                            info += "does not specify <serviceProvider/>";
                            log.info(info);
                        } else {
                            if (null == _providerId || _providerId.length() == 0 || UNKNOWN.equalsIgnoreCase(_providerId)) {
                                String pluginServiceProvider = xpath.getTextFrom(provider, "name");
                                log.debug(">>> Plugin service provider is \"" + pluginServiceProvider + "\"");
                                _providerId = pluginServiceProvider;
                            }

                            OMElement country = xpath.getElementFrom(provider, "country", /* accept failure? */ true);
                            if (null == country) {
                                String info = "WARN: Plugin \"" + pluginDescription + "\" (" + pluginId + ") ";
                                info += "has no specified service provider location";
                                log.warn(info);
                            }
                            else {
                                String serviceProviderCountryCode = xpath.getTextFrom(country, "code");
                                String serviceProviderCountryName = xpath.getTextFrom(country, "name");
                                log.debug(">>> Plugin service provider located in \"" + serviceProviderCountryName + "\" (" + serviceProviderCountryCode + ")");
                            }
                        }

                        // Third shot at determining service provider from <pluginCreators/> element
                        OMElement creator = xpath.getElementFrom(plugin, "pluginCreators", /* accept failure? */ true);
                        if (null == creator) {
                            String info = ">>> Plugin \"" + pluginDescription + "\" (" + pluginId + ") ";
                            info += "does not specify <pluginCreators/>";
                            log.info(info);
                        } else {
                            String pluginCreator = xpath.getTextFrom(creator, "name");
                            log.debug(">>> Plugin creator is \"" + pluginCreator + "\"");

                            // If no service provider was specified, may we guess at service provider from plugin creator?
                            if (null == _providerId || _providerId.length() == 0 || UNKNOWN.equalsIgnoreCase(_providerId)) {
                                log.info(">>> Guessing at service provider based on plugin creator: " + pluginCreator);
                                _providerId = pluginCreator;
                            }

                            OMElement country = xpath.getElementFrom(creator, "country", /* accept failure? */ true);
                            if (null == country) {
                                String info = "WARN: Plugin \"" + pluginDescription + "\" (" + pluginId + ") ";
                                info += "has no specified plugin creator location";
                                log.warn(info);
                            }
                            else {
                                String serviceProviderCountryCode = xpath.getTextFrom(country, "code");
                                String serviceProviderCountryName = xpath.getTextFrom(country, "name");
                                log.debug(">>> Plugin creator located in \"" + serviceProviderCountryName + "\" (" + serviceProviderCountryCode + ")");
                            }
                        }

                        // Fourth shot at determining service provider from the <code/> element
                        if (null == _providerId || _providerId.length() == 0 || UNKNOWN.equalsIgnoreCase(_providerId)) {
                            /*
                             *   <code>Public_Rackspace</code>
                             *   <code>Public_Amazon</code>
                             *   <code>Private_OpenStack</code>
                             */
                            if (null != code && code.length() > 0 && !UNKNOWN.equalsIgnoreCase(code)) {
                                code = code.toLowerCase();
                                if (code.contains("amazon")) {
                                    _providerId = "Amazon";
                                }
                                else if (code.contains("rackspace")) {
                                    _providerId = "Rackspace";
                                }
                                else if (code.contains("openstack")) {
                                    _providerId = "Lulea Tekniska Universitet";
                                }
                            }
                        }

                        // Last shot at determining service provider from the plugin description
                        if (null == _providerId || _providerId.length() == 0 || UNKNOWN.equalsIgnoreCase(_providerId)) {
                            String indice = pluginDescription.toLowerCase();
                            if (indice.contains("amazon")) {
                                _providerId = "Amazon";
                            }
                            else if (indice.contains("rackspace")) {
                                _providerId = "Rackspace";
                            }
                            else if (indice.contains("openstack")) {
                                _providerId = "Lulea Tekniska Universitet";
                            }
                        }

                        // ------------ Determining system ------------
                        // First shot at determining system from the <platform/> element
                        /*
                        OMElement platform = xpath.getElementFrom(plugin, "platform", /* accept failure? * / true);
                        if (null == platform) {
                            String info = ">>> Plugin \"" + pluginDescription + "\" (" + pluginId + ") ";
                            info += "does not specify <platform/>";
                            log.info(info);
                        } else {
                            if (null == _systemId || _systemId.length() == 0 || UNKNOWN.equalsIgnoreCase(_systemId)) {
                                String pluginServiceSystem = platform.getText().trim();
                                log.debug(">>> Plugin system is \"" + pluginServiceSystem + "\"");
                                _systemId = pluginServiceSystem;
                            }
                        }
                        */

                        // ------------ Determining visibility ------------
                        // Determine visibility (for those plugins who have any defined)
                        // Explicitly assume PUBLIC if not marked as PRIVATE.
                        LocalizedPlugin.Visibility visibility = LocalizedPlugin.Visibility.PUBLIC;
                        if (null != code && code.length() > 0 && !UNKNOWN.equalsIgnoreCase(code)) {
                            if (code.toLowerCase().contains("private")) {
                                visibility = LocalizedPlugin.Visibility.PRIVATE;
                            }
                        }



                        Plugin _plugin = null;

                        // Based on knowledge about the individual plugin types, extract plugin-specific information
                        // but also make some qualified judgements about service provider and specifically system id.
                        if ("storagePlugin".equalsIgnoreCase(pluginType)) {
                            // Determine location of storage
                            String _locality = UNKNOWN;

                            OMElement storesAt = xpath.getElementFrom(plugin, "storesAt", /* accept failure? */ true);
                            if (null == storesAt) {
                                String info = "WARN: Storage plugin \"" + pluginDescription + "\" (" + pluginId + ") ";
                                info += "has no specified storage location";
                                log.warn(info);
                            } else {
                                String pluginLocationName = xpath.getTextFrom(storesAt, "name");
                                String pluginLocationCode = xpath.getTextFrom(storesAt, "code");
                                log.debug(">>> Storage located in \"" + pluginLocationName + "\" (" + pluginLocationCode + ")");

                                _locality = pluginLocationCode;
                            }

                            if ("Amazon".equalsIgnoreCase(_providerId) &&
                                (null == _systemId || _systemId.length() == 0 || UNKNOWN.equalsIgnoreCase(_systemId))) {
                                log.info(">>> Guessing at system id \"S3\" based on provider id: " + _providerId);
                                _systemId = "S3";
                            }
                            else if ("Rackspace".equalsIgnoreCase(_providerId) &&
                                    (null == _systemId || _systemId.length() == 0 || UNKNOWN.equalsIgnoreCase(_systemId))) {
                                log.info(">>> Guessing at system id \"CloudFiles\" based on provider id: " + _providerId);
                                _systemId = "CloudFiles";
                            }
                            else if ("Lulea Tekniska Universitet".equalsIgnoreCase(_providerId) &&
                                    (null == _systemId || _systemId.length() == 0 || UNKNOWN.equalsIgnoreCase(_systemId))) {
                                log.info(">>> Guessing at system id \"ObjectStorage\" (Swift) based on provider id: " + _providerId);
                                _systemId = "OpenStack ObjectStorage";
                            }
                            _plugin = new StoragePlugin(pluginId, _providerId, _systemId, _locality, pluginType, pluginDescription, visibility);
                        }
                        else if ("computePlugin".equalsIgnoreCase(pluginType)) {
                            // Determine location of computation
                            String _locality = UNKNOWN;

                            OMElement storesAt = xpath.getElementFrom(plugin, "computesAt", /* accept failure? */ true);
                            if (null == storesAt) {
                                String info = "WARN: Compute plugin \"" + pluginDescription + "\" (" + pluginId + ") ";
                                info += "has no specified computation location";
                                log.warn(info);
                            } else {
                                String pluginLocationName = xpath.getTextFrom(storesAt, "name");
                                String pluginLocationCode = xpath.getTextFrom(storesAt, "code");
                                log.debug(">>> Computations performed in \"" + pluginLocationName + "\" (" + pluginLocationCode + ")");

                                _locality = pluginLocationCode;
                            }

                            if ("Amazon".equalsIgnoreCase(_providerId) &&
                                (null == _systemId || _systemId.length() == 0 || UNKNOWN.equalsIgnoreCase(_systemId))) {
                                log.info(">>> Guessing at system id \"EC2\" based on provider id: " + _providerId);
                                _systemId = "EC2";
                            }
                            else if ("Rackspace".equalsIgnoreCase(_providerId) &&
                                    (null == _systemId || _systemId.length() == 0 || UNKNOWN.equalsIgnoreCase(_systemId))) {
                                log.info(">>> Guessing at system id \"CloudServers\" based on provider id: " + _providerId);
                                _systemId = "CloudServers";
                            }
                            else if ("Lulea Tekniska Universitet".equalsIgnoreCase(_providerId) &&
                                    (null == _systemId || _systemId.length() == 0 || UNKNOWN.equalsIgnoreCase(_systemId))) {
                                log.info(">>> Guessing at system id \"Compute\" (Nova) based on provider id: " + _providerId);
                                _systemId = "OpenStack Compute";
                            }
                            _plugin = new ComputePlugin(pluginId, _providerId, _systemId, _locality, pluginType, pluginDescription, visibility);
                        }
                        else if ("fixityPlugin".equals(pluginType)) {
                            // Determine fixity check algorithm of fixity plugin
                            String fixityAlgorithm = xpath.getTextFrom(plugin, "algorithm");
                            log.debug(">>> Fixity check algorithm is \"" + fixityAlgorithm + "\"");

                            _plugin = new FixityPlugin(pluginId, fixityAlgorithm, pluginType, pluginDescription);
                        }
                        else if ("encryptionPlugin".equalsIgnoreCase(pluginType)) {
                            // Determine encryption specific information
                            String encryptionAlgorithm = xpath.getTextFrom(plugin, "algorithm");
                            log.debug(">>> Encryption algorithm is \"" + encryptionAlgorithm + "\"");

                            String algorithmStrengthRating = xpath.getTextFrom(plugin, "algorithmStrengthRating");
                            String attackStepsPowerOf2 = xpath.getTextFrom(plugin, "attackStepsPowerOf2");
                            String blockSize = xpath.getTextFrom(plugin, "blockSize");
                            String exposureToPeerReview = xpath.getTextFrom(plugin, "exposureToPeerReview");
                            String keyLengthBits = xpath.getTextFrom(plugin, "keyLengthBits");
                            String numberOfKnownVulnerabilities = xpath.getTextFrom(plugin, "numberOfKnownVulnerabilities");
                            String rounds = xpath.getTextFrom(plugin, "rounds");

                            _plugin = new EncryptionPlugin(pluginId, encryptionAlgorithm,
                                    algorithmStrengthRating, attackStepsPowerOf2, blockSize,
                                    exposureToPeerReview, keyLengthBits, numberOfKnownVulnerabilities,
                                    rounds, pluginType, pluginDescription);
                        }
                        else if ("transformationPlugin".equalsIgnoreCase(pluginType)) {
                            if (UNKNOWN.equalsIgnoreCase(_providerId) && pluginDescription.toLowerCase().contains("dicom")) {
                                log.info(">>> Guessing at provider id \"Philips\" based on description containing the word: DICOM");
                                _providerId = "Philips";

                                log.info(">>> Guessing at system id \"PickRegionsOfInterest\" based on provider id: Philips");
                                _systemId = "PickRegionsOfInterest";
                            }
                            _plugin = new TransformationPlugin(pluginId, _providerId, _systemId, pluginType, pluginDescription);
                        }
                        else {
                            _plugin = new Plugin(pluginId, _providerId, _systemId, pluginType, pluginDescription);
                        }

                        if (log.isInfoEnabled()) {
                            String info = ">>> Instantiated plugin \"" + pluginDescription + "\" (" + pluginId + ") of general type \"" + pluginType + "\"";
                            log.info(info);
                        }

                        // Capture additional quality metrics for this plugin
                        String meantimeBetweenFailuresUnit = xpath.getTextFrom(plugin, "meantimeBetweenFailuresUnit", /* accept failure? */ true);
                        String _meantimeBetweenFailures = xpath.getTextFrom(plugin, "meantimeBetweenFailures", /* accept failure? */ true);
                        if (null != meantimeBetweenFailuresUnit && meantimeBetweenFailuresUnit.length() > 0
                         && null != _meantimeBetweenFailures && _meantimeBetweenFailures.length() > 0) {

                            double mtbf = Double.parseDouble(_meantimeBetweenFailures);
                            if (mtbf > 0.0) {
                                //
                                _plugin.setMtbf(mtbf);
                                _plugin.setMtbfUnit(meantimeBetweenFailuresUnit);
                            }
                        }

                        //-----------------------------------------
                        // Read plugin dependencies information
                        //-----------------------------------------
                        OMElement dependencies = xpath.getElementFrom(plugin, "dependences", /* accept failure? */ true);
                        if (null != dependencies) {
                            String dependenciesTypeAttribute = attribute.getValueFrom(dependencies, "xsi", "type", /* accept failure? */ true);
                            if (null == dependenciesTypeAttribute)
                                dependenciesTypeAttribute = UNKNOWN;

                            String dependenciesId = xpath.getTextFrom(dependencies, "id");
                            String dependenciesDescription = xpath.getTextFrom(dependencies, "description");
                            //String dependenciesLocation = xpath.getTextFrom(dependencies, "location"); // OSGi related
                            //String dependenciesEfficiencyRating = xpath.getTextFrom(plugin, "efficiencyRating");
                            if (log.isDebugEnabled()) {
                                String info = "    depending on plugin \"" + dependenciesDescription + "\" (" + dependenciesId + ") of general type \"" + dependenciesTypeAttribute + "\"";
                                log.debug(info);
                            }

                            Plugin _parentPlugin = _pluginMap.get(dependenciesId);
                            if (null != _parentPlugin) {
                                _plugin.setParentPlugin(_parentPlugin);
                            }
                        }

                        if (_pluginMap.containsKey(pluginId)) {
                            String info = "Mismatch among service plugin IDs: id = \"" + pluginId + "\" is already defined";
                            //info += " - ignoring plugin " + _plugin.getDescription();
                            log.warn(info);
                            //throw new EvaluationException(info);
                        }
                        _pluginMap.put(pluginId, _plugin);
                    }
                }
            }
        }
        return _pluginMap;
    }

    /**
     * Reads the preservation plan part of the GPP. It contains several aggregations with individual
     * actions (using plugins), but also some "system wide" actions (system actions) that apply equally
     * to all aggregations.
     * <p>
     * @param gpp
     * @param namespaces
     * @param _pluginMap
     * @return
     * @throws XmlException
     * @throws EvaluationException
     */
    private PreservationPlan readAggregations(
            OMElement gpp, Namespaces namespaces, Map<String, Plugin> _pluginMap
    ) throws XmlException, EvaluationException {

        if (log.isInfoEnabled()) {
            log.info("\n<<Reading aggregations...>>");
        }

        PreservationPlan _customer = new PreservationPlan();
        readAggregationActions(_customer, gpp, namespaces, _pluginMap);
        readSystemActions(_customer, gpp, namespaces, _pluginMap);
        return _customer;
    }

    /**
     * Reads the aggregation specific part of the preservation plans for individual aggregations.
     * <p>
     * @param _customer
     * @param gpp
     * @param namespaces
     * @param _pluginMap
     * @return
     * @throws XmlException
     * @throws EvaluationException
     */
    private PreservationPlan readAggregationActions(
            PreservationPlan _customer, OMElement gpp, Namespaces namespaces, Map<String, Plugin> _pluginMap
    ) throws XmlException, EvaluationException {

        // Query objects
        XPath xpath = new XPath(namespaces);
        Attribute attribute = new Attribute(namespaces);

        String expression = "/globalPreservationPlan/ID";
        String id = xpath.getTextFrom(gpp, expression, /* accept failure */ false);
        _customer.setId(id);

        expression = "//preservationPlan/aggregationActions";
        List<OMElement> aggregationActions = xpath.getElementsFrom(gpp, expression);
        if (aggregationActions.size() > 0) {
            if (log.isDebugEnabled()) {
                String info = "Found " + aggregationActions.size() + " aggregation actions.";
                log.debug(info);
            }

            for (OMElement aggregationAction : aggregationActions) {
                //-----------------------------------------
                // Read aggregation information
                //-----------------------------------------
                OMElement aggregation = xpath.getElementFrom(aggregationAction, "aggregation");
                String aggregationId = xpath.getTextFrom(aggregation, "id");
                String aggregationName = xpath.getTextFrom(aggregation, "name");
                String dataCategory = xpath.getTextFrom(aggregation, "dataCategory", /* accept failure? */ true);

                if (log.isDebugEnabled()) {
                    dataCategory = null == dataCategory ? UNKNOWN : dataCategory;
                    String info = "> Found aggregation \"" + aggregationName + "\" (" + aggregationId + ") for category \"" + dataCategory + "\"";
                    log.debug(info);
                }

                // Collect format information
                Collection<String> mimeTypes = new LinkedList<String>();

                List<OMElement> formats = xpath.getElementsFrom(aggregation, "format", /* accept failure? */ true);
                for (OMElement format : formats) {
                    String formatName = xpath.getTextFrom(format, "name");
                    String formatDescription = xpath.getTextFrom(format, "description");
                    String formatMimeType = xpath.getTextFrom(format, "mimeType");
                    String formatVersion = xpath.getTextFrom(format, "version");

                    if (log.isDebugEnabled()) {
                        String info = "> Found format " + formatName + " (" + formatDescription + ") version \"" + formatVersion + "\"";
                        log.debug(info);
                    }
                    mimeTypes.add(formatMimeType);
                }

                String aggregationXml = aggregation.toString();
                Aggregation _aggregation = new Aggregation(
                        aggregationId, aggregationName, mimeTypes, aggregationXml
                );
                Map</* event name */ String, Event> _events = _aggregation.getEvents();
                readAggregationActions(_aggregation, aggregationAction, xpath, _pluginMap, _events);

                _customer.getAggregations().add(_aggregation);
            }
        }
        return _customer;
    }

    /**
     * Reads actions that are specific to an aggregation.
     * <p>
     * @param aggregation
     * @param aggregationAction
     * @param xpath
     * @param pluginMap
     * @param events
     * @throws XmlException
     * @throws EvaluationException
     */
    private void readAggregationActions(
            Aggregation aggregation, OMElement aggregationAction, XPath xpath,
            Map<String, Plugin> pluginMap, Map</* event name */ String, Event> events
    ) throws XmlException, EvaluationException {

        // Read the aggregation actions...
        List<OMElement> actions = xpath.getElementsFrom(aggregationAction, "actions");
        if (actions.size() > 0) {
            if (log.isDebugEnabled()) {
                String info = "> Found " + actions.size() + " actions associated with aggregation \"" + aggregation.getName() + "\"";
                log.debug(info);
            }

            for (OMElement action : actions) {
                readAggregationAction(aggregation, action, xpath, pluginMap, events);
            }
        }

        // Read the aggregation copy actions
        List<OMElement> copies = xpath.getElementsFrom(aggregationAction, "copyActions");
        if (copies.size() > 1) {
            log.info("> Aggregation \"" + aggregation.getName() + "\" uses diversity (1 + " + (copies.size() - 1) + " copies)");
        }

        for (OMElement copy : copies) {
            String order = xpath.getTextFrom(copy, "order");
            String info = "> Found actions for copy number " + order;
            log.info(info);
            int copyNumber = Integer.parseInt(order);

            List<OMElement> copyActions = xpath.getElementsFrom(copy, "actions");
            for (OMElement copyAction : copyActions) {
                readAggregationCopyAction(copyNumber, aggregation, copyAction, xpath, pluginMap, events);
            }
        }
    }

    /**
     * Reads individual actions for an aggregation.
     * <p>
     * @param aggregation
     * @param action
     * @param xpath
     * @param pluginMap
     * @param events
     * @throws XmlException
     * @throws EvaluationException
     */
    private void readAggregationAction(
            Aggregation aggregation, OMElement action, XPath xpath,
            Map<String, Plugin> pluginMap, Map</* event name */ String, Event> events
    ) throws XmlException, EvaluationException {

        String actionType = xpath.getTextFrom(action, "actionType");
        String description = xpath.getTextFrom(action, "description");
        String trigger = xpath.getTextFrom(action, "trigger");
        String pluginServiceInstanceId = xpath.getTextFrom(action, "pluginServiceInstanceId", /* accept failure? */ true);
        if (null == pluginServiceInstanceId) {
            pluginServiceInstanceId = UNKNOWN;
        }

        // Extract information about retention period.
        if (DELETION.equalsIgnoreCase(actionType)) {
            // Extract retention period information for this aggregation
            if (!Event.ON_INGEST.equalsIgnoreCase(trigger)) {
                String info = "Unexpected action DELETION at other event than ON_INGEST for aggregation: ";
                info += aggregation.getName() + " (" + aggregation.getId() + ")";
                log.warn(info);
            }
            String _maximumNumberOfExecutions = xpath.getTextFrom(action, "maximumNumberOfExecutions");
            int maximumNumberOfExecutions = Integer.parseInt(_maximumNumberOfExecutions);
            if (1 != maximumNumberOfExecutions) {
                String info = "DELETION maximum number of executions given as: " + maximumNumberOfExecutions;
                log.warn(info);
            }

            String executionIntervalUnit = xpath.getTextFrom(action, "executionIntervalUnit");
            if (!"YEAR".equalsIgnoreCase(executionIntervalUnit)) {
                String info = ">> DELETION execution interval unit given as: " + executionIntervalUnit;
                log.info(info);
            }

            String _executionInterval = xpath.getTextFrom(action, "executionInterval");
            Float executionInterval = Float.parseFloat(_executionInterval);

            // Retention period will be an even number of years, even though it is provided as months or days
            if ("YEAR".equalsIgnoreCase(executionIntervalUnit)) {
                int years = (int)Math.ceil(executionInterval);
                if (log.isDebugEnabled()) {
                    log.debug(">> Retention period: " + years + " years (" + dec2Format.format(executionInterval) + " years)");
                }
                aggregation.setRetentionPeriod(years);
            }
            else if ("MONTH".equalsIgnoreCase(executionIntervalUnit)) {
                int years = (int)Math.ceil(executionInterval / 12.0);
                if (log.isDebugEnabled()) {
                    log.debug(">> Retention period: " + years + " years (" + dec2Format.format(executionInterval) + " months)");
                }
                aggregation.setRetentionPeriod(years);
            }
            else if ("DAY".equalsIgnoreCase(executionIntervalUnit)) {
                int years = (int)Math.ceil(executionInterval / 365.0);
                if (log.isDebugEnabled()) {
                    log.debug(">> Retention period: " + years + " years (" + dec2Format.format(executionInterval) + " days)");
                }
                aggregation.setRetentionPeriod(years);
            }
            return; // Will not model DELETION as a plugin!
        }

        if (STORAGE.equalsIgnoreCase(actionType)) {
            String info = "Storage plugins listed among general aggregation actions are ignored: ";
            info += description + " - [ignoring]";
            log.warn(info);
            return;
        }

        if (COMPUTE.equalsIgnoreCase(actionType)) {
            String info = "Compute plugins listed among general aggregation actions are ignored: ";
            info += description + " - [ignoring]";
            log.warn(info);
            return;
        }

        Plugin _plugin = pluginMap.get(pluginServiceInstanceId);
        if (null == _plugin) {
            String info = "Could not locate a plugin service instance matching " + pluginServiceInstanceId;
            info += " for action \"" + description + "\"";
            info += " in aggregation \"" + aggregation.getName() + "\"";
            info += " - [ignoring]";
            log.warn(info);
            return;
        }

        if (log.isDebugEnabled()) {
            String info = ">> Found \"" + actionType + "\" action type with trigger \"" + trigger;
            info += "\" for \"" + description + "\" handled by plugin \"" + _plugin.getDescription() + "\" " +
                    "(" + pluginServiceInstanceId + ")";
            log.debug(info);
        }

        Event _event = Event.findEvent(events, trigger);

        List<Plugin> registeredPlugins = _event.getPlugins();
        if (registeredPlugins.contains(_plugin)) {
            String info = "Plugin \"" + _plugin.getDescription();
            info += "\" (" + pluginServiceInstanceId + ") is already associated with event ";
            info += _event.getType().name();
            info += " in aggregation \"" + aggregation.getName() + "\"";
            info += " - [ignoring]";
            log.warn(info);
            return;
        }
        registeredPlugins.add(_plugin);
    }

    /**
     * Reads individual copy actions for an aggregation.
     * <p>
     * @param aggregation
     * @param action
     * @param xpath
     * @param pluginMap
     * @param events
     * @throws XmlException
     * @throws EvaluationException
     */
    private void readAggregationCopyAction(
            int copyNumber, Aggregation aggregation, OMElement action, XPath xpath,
            Map<String, Plugin> pluginMap, Map</* event name */ String, Event> events
    ) throws XmlException, EvaluationException {

        String actionType = xpath.getTextFrom(action, "actionType");
        String description = xpath.getTextFrom(action, "description");
        String trigger = xpath.getTextFrom(action, "trigger");
        String pluginServiceInstanceId = xpath.getTextFrom(action, "pluginServiceInstanceId", /* accept failure? */ true);
        if (null == pluginServiceInstanceId) {
            pluginServiceInstanceId = UNKNOWN;
        }

        if (!STORAGE.equalsIgnoreCase(actionType) && !COMPUTE.equalsIgnoreCase(actionType)) {
            String info = "Expecting storage- and compute plugins among copy actions: ";
            info += description + " - [ignoring]";
            log.warn(info);
            return;
        }

        Plugin _plugin = pluginMap.get(pluginServiceInstanceId);
        if (null == _plugin) {
            String info = "Could not locate a plugin service instance matching " + pluginServiceInstanceId;
            info += " for action \"" + description + "\"";
            info += " in aggregation \"" + aggregation.getName() + "\"";
            info += " - [ignoring]";
            log.warn(info);
            return;
        }

        if (log.isDebugEnabled()) {
            String info = ">> Found \"" + actionType + "\" action type (with trigger \"" + trigger;
            info += "\") for \"" + description + "\" handled by plugin \"" + _plugin.getDescription() + "\" " +
                    "(" + pluginServiceInstanceId + ")";
            log.debug(info);
        }

        Map<Integer, Copy> copies = aggregation.getCopies();
        if (STORAGE.equalsIgnoreCase(actionType)) {

            if (copies.containsKey(copyNumber)) {
                // This copy already exists - may already have the compute plugin
                Copy existingCopy = copies.get(copyNumber);

                if (null != existingCopy.getStoragePlugin()) {
                    String info = "A storage plugin is already associated with this copy: " + copyNumber + ": ";
                    info += existingCopy.getStoragePlugin();
                    log.warn(info);
                }
                else {
                    existingCopy.setStoragePlugin((StoragePlugin)_plugin);
                }
            }
            else {
                Copy copy = new Copy(copyNumber);
                copy.setStoragePlugin((StoragePlugin) _plugin);
                copies.put(copyNumber, copy);
            }
        }
        else if (COMPUTE.equalsIgnoreCase(actionType)) {

            if (copies.containsKey(copyNumber)) {
                // This copy already exists - may already have the storage plugin
                Copy existingCopy = copies.get(copyNumber);

                if (null != existingCopy.getComputePlugin()) {
                    String info = "A compute plugin is already associated with this copy: " + copyNumber + ": ";
                    info += existingCopy.getComputePlugin();
                    log.warn(info);
                }
                else {
                    existingCopy.setComputePlugin((ComputePlugin)_plugin);
                }
            }
            else {
                Copy copy = new Copy(copyNumber);
                copy.setComputePlugin((ComputePlugin) _plugin);
                copies.put(copyNumber, copy);
            }
        }
    }

    /**
     * Reads the system actions (which applies to all aggregations).
     * <p>
     * @param _customer
     * @param gpp
     * @param namespaces
     * @param _pluginMap
     * @return
     * @throws XmlException
     * @throws EvaluationException
     */
    private PreservationPlan readSystemActions(PreservationPlan _customer, OMElement gpp, Namespaces namespaces, Map<String, Plugin> _pluginMap)
            throws XmlException, EvaluationException {

        // Query objects
        XPath xpath = new XPath(namespaces);

        String expression = "//preservationPlan/systemActions";

        List<OMElement> systemActions = xpath.getElementsFrom(gpp, expression);
        if (systemActions.size() > 0) {
            if (log.isDebugEnabled()) {
                String info = "Found " + systemActions.size() + " system actions.";
                log.debug(info);
            }

            for (OMElement action : systemActions) {
                Map</* event name */ String, Event> _events = _customer.getSystemWideEvents();
                readSystemAction(action, xpath, _pluginMap, _events);
            }
        }
        return _customer;
    }

    /**
     * Reads individual actions for an aggregation.
     * <p>
     * @param action
     * @param xpath
     * @param pluginMap
     * @param events
     * @throws XmlException
     * @throws EvaluationException
     */
    private void readSystemAction(
            OMElement action, XPath xpath,
            Map<String, Plugin> pluginMap, Map</* event name */ String, Event> events
    ) throws XmlException, EvaluationException {

        String actionType = xpath.getTextFrom(action, "actionType");
        String description = xpath.getTextFrom(action, "description");
        String trigger = xpath.getTextFrom(action, "trigger");
        String pluginServiceInstanceId = xpath.getTextFrom(action, "pluginServiceInstanceId", /* accept failure? */ true);
        if (null == pluginServiceInstanceId) {
            pluginServiceInstanceId = UNKNOWN;
        }

        if (!USES.equalsIgnoreCase(actionType)) {
            String info = "Only USES actions are expected among system actions: action-type=\"";
            info += "\"" + actionType + "\" (for \"" + description + "\") - [ignoring]";
            log.warn(info);
            return;

        }

        Plugin _plugin = pluginMap.get(pluginServiceInstanceId);
        if (null == _plugin) {
            String info = "Could not locate a plugin service instance matching " + pluginServiceInstanceId;
            info += " for action \"" + description + "\"";
            info += " - [ignoring]";
            log.warn(info);
            return;
        }

        /* Relaxing this constraint...
        if (!_plugin.getType().startsWith("base")) {
            String info = "Only 'base'-plugins are expected among system actions: ";
            info += description + " of type \"" + _plugin.getType() + "\" - [ignoring]";
            log.warn(info);
            return;
        }
        */

        if (log.isDebugEnabled()) {
            String info = "> Found \"" + actionType + "\" system action type with trigger \"" + trigger;
            info += "\" for \"" + description + "\" handled by plugin \"" + _plugin.getDescription() + "\" " +
                    "(" + pluginServiceInstanceId + ") of type \"" + _plugin.getType() + "\"";
            log.debug(info);
        }

        Event _event = Event.findEvent(events, trigger);

        List<Plugin> registeredPlugins = _event.getPlugins();
        if (registeredPlugins.contains(_plugin)) {
            String info = "Plugin \"" + _plugin.getDescription();
            info += "\" (" + pluginServiceInstanceId + ") is already associated with event ";
            info += _event.getType().name();
            info += " - [ignoring]";
            log.warn(info);
            return;
        }
        registeredPlugins.add(_plugin);
    }

    /*
    <requirementSet>
        <configurationHorizonMonths>25</configurationHorizonMonths>

        <requirementsPerAggregation>
            <category>SILVER</category>
            <dataRetentionPeriod>
                <id>0</id>
                <lowerBoundIncluded>true</lowerBoundIncluded>
                <unit/>
                <upperBoundIncluded>true</upperBoundIncluded>
                <lowerBound>0</lowerBound>
                <upperBound>1</upperBound>
            </dataRetentionPeriod>
            <encrypted>true</encrypted>
            <id>3</id>
            <needAnonymization>false</needAnonymization>
            <relatedAggregation>
                <id>Information_Pages</id>
                <name>Information Pages</name>
                <description>Maccabi Information Pages.</description>
                <dataCategory>DOCUMENT</dataCategory>
                <format>
                    <creationTime>2013-02-18T17:43:34.320+01:00</creationTime>
                    <defaultExtension>pdf</defaultExtension>
                    <deprecatedTime>2013-11-01T17:43:34.320+01:00</deprecatedTime>
                    <description>PDF version 1.5</description>
                    <id>5</id>
                    <mimeType>application/pdf</mimeType>
                    <name>PDF</name>
                    <obsolete>false</obsolete>
                    <version>1.5</version>
                </format>
            </relatedAggregation>
            <storageType>UNSPECIFIED</storageType>
        </requirementsPerAggregation>

        <requirementsPerAggregation>
            <category>SILVER</category>
            <dataRetentionPeriod>
                <id>0</id>
                <lowerBoundIncluded>true</lowerBoundIncluded>
                <unit/>
                <upperBoundIncluded>true</upperBoundIncluded>
                <lowerBound>0</lowerBound>
                <upperBound>1</upperBound>
            </dataRetentionPeriod>
            <encrypted>true</encrypted>
            <id>2</id>
            <needAnonymization>false</needAnonymization>
            <relatedAggregation>
                <id>Medical_Images</id>
                <name>Medical Images</name>
                <description>Within Maccabi medical record, individual radiographies.</description>
                <dataCategory>RADIOGRAPHY</dataCategory>
                <format>
                    <creationTime>2013-02-18T17:43:34.320+01:00</creationTime>
                    <deprecatedTime>2013-11-01T17:43:34.320+01:00</deprecatedTime>
                    <description>Digital Imaging and Communication in Medicine</description>
                    <id>2</id>
                    <mimeType>application/dicom</mimeType>
                    <name>DICOM</name>
                    <obsolete>false</obsolete>
                    <version>2008</version>
                </format>
                <format>
                    <creationTime>2013-02-18T17:43:34.320+01:00</creationTime>
                    <deprecatedTime>2013-11-01T17:43:34.320+01:00</deprecatedTime>
                    <description>Subset of DICOM 2008 + supplement 122 (for specimen) + private Philips attribute</description>
                    <id>3</id>
                    <mimeType>application/dicompl</mimeType>
                    <name>DICOM_PHILIPS</name>
                    <obsolete>false</obsolete>
                    <version>2008</version>
                </format>
            </relatedAggregation>
            <storageType>UNSPECIFIED</storageType>
        </requirementsPerAggregation>

        <userPrefences xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="specificUserPreference">
            <id>0</id>
            <kind>SPECIFIC</kind>
            <preference>VERY_HIGH</preference>
            <aggregation>
                <id>Medical_Encounters</id>
                <name>Medical Encounters</name>
                <description>Within Maccabi medical record, individual medical encounters are marked by discrete summations of a patient's medical history by a physician, nurse practitioner, or physician assistant and can take several forms.</description>
                <dataCategory>CLINIC_HISTORY</dataCategory>
                <format>
                    <creationTime>2007-08-29T18:43:34.320+02:00</creationTime>
                    <defaultExtension>.xml</defaultExtension>
                    <deprecatedTime>2013-05-29T18:43:34.320+02:00</deprecatedTime>
                    <description>Extensible Markup Language</description>
                    <id>1</id>
                    <mimeType>text/xml</mimeType>
                    <name>XML</name>
                    <obsolete>false</obsolete>
                    <version>1.1</version>
                </format>
            </aggregation>
            <applicationLevel>PRESERVATION_PLAN</applicationLevel>
            <hasContraints>
                <id>0</id>
                <limit>5.0</limit>
                <type>GREATER_OR_EQUAL</type>
            </hasContraints>
            <hasContraints>
                <id>0</id>
                <limit>50000.0</limit>
                <type>LOWER_THAN</type>
            </hasContraints>
            <type>TOTAL_COSTS_CONFIGURATION_HORIZON</type>
        </userPrefences>

        <userPrefences xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="specificUserPreference">
            <id>0</id>
            <kind>SPECIFIC</kind>
            <preference>VERY_HIGH</preference>
            <aggregation>
                <id>Medical_Encounters</id>
                <name>Medical Encounters</name>
                <description>Within Maccabi medical record, individual medical encounters are marked by discrete summations of a patient's medical history by a physician, nurse practitioner, or physician assistant and can take several forms.</description>
                <dataCategory>CLINIC_HISTORY</dataCategory>
                <format>
                    <creationTime>2007-08-29T18:43:34.320+02:00</creationTime>
                    <defaultExtension>.xml</defaultExtension>
                    <deprecatedTime>2013-05-29T18:43:34.320+02:00</deprecatedTime>
                    <description>Extensible Markup Language</description>
                    <id>1</id>
                    <mimeType>text/xml</mimeType>
                    <name>XML</name>
                    <obsolete>false</obsolete>
                    <version>1.1</version>
                </format>
            </aggregation>
            <applicationLevel>AGGREGATION</applicationLevel>
            <hasContraints>
                <id>0</id>
                <limit>5.0</limit>
                <type>GREATER_OR_EQUAL</type>
            </hasContraints>
            <hasContraints>
                <id>0</id>
                <limit>50000.0</limit>
                <type>LOWER_THAN</type>
            </hasContraints>
            <type>SCORE</type>
        </userPrefences>

        <userPrefences xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="highLevelUserPreference">
            <id>0</id>
            <kind>HIGH_LEVEL</kind>
            <preference>HIGH</preference>
            <type>COST</type>
        </userPrefences>
    </requirementSet>

     */

    private void readRequirements(OMElement reqSet, Namespaces namespaces, PreservationPlan preservationPlan) {

        // Query objects
        XPath xpath = new XPath(namespaces);

        try {
            List<Aggregation> aggregations = preservationPlan.getAggregations();
            for (Aggregation aggregation : aggregations) {
                String id = aggregation.getId();
                String expression = String.format("//requirementsPerAggregation[(./relatedAggregation/id[text() = '%s'])]", id);
                List<OMElement> requirements = xpath.getElementsFrom(reqSet, expression);
                for (OMElement requirement : requirements) {
                    String category = xpath.getTextFrom(requirement, "category", /* accept failure? */ true);

                    String _encryption = xpath.getTextFrom(requirement, "encrypted", /* accept failure? */ true);
                    boolean needEncryption = "true".equalsIgnoreCase(_encryption);
                    aggregation.setNeedsEncryption(needEncryption);

                    String _needAnonymization = xpath.getTextFrom(requirement, "needAnonymization", /* accept failure? */ true);
                    boolean needAnonymization = "true".equalsIgnoreCase(_needAnonymization);
                    aggregation.setNeedsAnonymization(needAnonymization);

                    // Data retention period is selected from the DELETION action for the aggregation!!

                    List<OMElement> purposes = xpath.getElementsFrom(requirement, "purposes");
                    if (purposes.size() > 0) {
                        for (OMElement purpose : purposes) {
                            //
                            String priority = xpath.getTextFrom(purpose, "priority", /* accept failure? */ true);
                            if (null == priority || priority.length() == 0) {
                                priority = Purpose.PurposeType.PRIMARY.name(); // default
                            }

                            Purpose.PurposeType purposeType = Purpose.PurposeType.PRIMARY; // default
                            if (Purpose.PRIMARY.equalsIgnoreCase(priority)) {
                                purposeType = Purpose.PurposeType.PRIMARY;
                            }
                            else if (Purpose.SECONDARY.equalsIgnoreCase(priority)) {
                                purposeType = Purpose.PurposeType.SECONDARY;
                            }
                            else {
                                log.warn("Unknown purpose priority: " + priority + ": " + purpose);
                            }

                            //
                            String type = xpath.getTextFrom(purpose, "type", /* accept failure? */ true);
                            if (null == type || type.length() == 0) {
                                type = Purpose.PurposeClass.EVIDENCE.name(); // default
                                String info = "No purpose type ({evidence, historic, ...}) was provided for aggregation \"";
                                info += aggregation.getId();
                                info += "\" - assuming 'evidence' (the strictest)";
                                log.warn(info);
                            }

                            Purpose.PurposeClass purposeClass = Purpose.PurposeClass.EVIDENCE; // default
                            if (Purpose.EVIDENCE.equalsIgnoreCase(type)) {
                                purposeClass = Purpose.PurposeClass.EVIDENCE;
                            }
                            else if (Purpose.FISCAL_VALUE.equalsIgnoreCase(type)) {
                                purposeClass = Purpose.PurposeClass.FISCAL_VALUE;
                            }
                            else if (Purpose.RESEARCH.equalsIgnoreCase(type)) {
                                purposeClass = Purpose.PurposeClass.RESEARCH;
                            }
                            else if (Purpose.HISTORY.equalsIgnoreCase(type)) {
                                purposeClass = Purpose.PurposeClass.HISTORY;
                            }
                            else if (Purpose.ADMINISTRATIVE.equalsIgnoreCase(type)) {
                                purposeClass = Purpose.PurposeClass.ADMINISTRATIVE;
                            }
                            else if (Purpose.INFORMATIONAL.equalsIgnoreCase(type)) {
                                purposeClass = Purpose.PurposeClass.INFORMATIONAL;
                            }
                            else if (Purpose.DIAGNOSTIC.equalsIgnoreCase(type)) {
                                purposeClass = Purpose.PurposeClass.DIAGNOSTIC;
                            }
                            else if (Purpose.RUNTIME.equalsIgnoreCase(type)) {
                                purposeClass = Purpose.PurposeClass.RUNTIME;
                            }
                            else {
                                log.warn("Unknown purpose: " + purposeClass + ": " + purpose);
                            }

                            //
                            Purpose _purpose = new Purpose(purposeClass, purposeType);
                            aggregation.addPurpose(_purpose);
                        }
                    }
                    else {
                        // Strictest choice as default
                        Purpose evidence = new Purpose(Purpose.PurposeClass.EVIDENCE, Purpose.PurposeType.PRIMARY);
                        aggregation.addPurpose(evidence);
                    }
                }
            }
        }
        catch (XmlException xmle) {
            String info = "Could not read requirement set: " + xmle.getMessage();
            log.warn(info, xmle);
        }
    }
}
