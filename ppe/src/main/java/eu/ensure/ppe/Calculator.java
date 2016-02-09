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


import eu.ensure.vopn.lang.Number;
import eu.ensure.vopn.statistics.MovingAverage;
import eu.ensure.ppe.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.util.*;

/**
 * Calculates score for Global Preservation Plans.
 * <p>
 * Written at an altitude of 11.277 meters, just above Le Mans, France.
 * The outside temperature is -55°C :)
 * </p>
 * Created by Frode Randers at 2012-10-16 17:57
 */
public class Calculator {
    private static final Logger log = LogManager.getLogger(Calculator.class);

    private final LocalizedPluginCalculator localizedPluginCalculator;
    private final FixityPluginCalculator fixityPluginCalculator;
    private final EncryptionPluginCalculator encryptionPluginCalculator;
    private final GeneralPluginCalculator generalPluginCalculator;


    public Calculator(DataSource dataSource) {
        this.localizedPluginCalculator = new LocalizedPluginCalculator(dataSource);
        this.fixityPluginCalculator = new FixityPluginCalculator(dataSource);
        this.encryptionPluginCalculator = new EncryptionPluginCalculator(dataSource);
        this.generalPluginCalculator = new GeneralPluginCalculator(dataSource);
    }

    private Double calculateScore(
            Aggregation aggregation,
            String event,
            Plugin plugin,
            Collection<String> storyLine,
            Collection<Consequence> consequences
    ) throws EvaluationException {

        if (log.isDebugEnabled()) {
            log.debug("--- Plugin: " + plugin.getDescription());
        }

        if (plugin instanceof StoragePlugin || plugin instanceof ComputePlugin) {
            Double score = localizedPluginCalculator.calculateScore(
                    aggregation, event, (LocalizedPlugin) plugin, storyLine, consequences
            );

            // Get a second opinion in debug
            if (log.isDebugEnabled()) {
                Double v = localizedPluginCalculator.calculateScoreVerification(
                        aggregation, event, (LocalizedPlugin) plugin
                );
                if ((null != score && !score.equals(v)) || (null != v && !v.equals(score))) {
                    String _score = null != score ? Double.toString(score) : "none";
                    String _verification = null != v ? Double.toString(v) : "none";
                    String info = "Calculations differ: score=" + _score + " verification=" + _verification;
                    log.debug(info);
                }
            }
            return score;
        }
        else if (plugin instanceof FixityPlugin) {
            return fixityPluginCalculator.calculateScore(
                    aggregation, event, (FixityPlugin) plugin, storyLine, consequences
            );
        }
        else if (plugin instanceof EncryptionPlugin) {
            return encryptionPluginCalculator.calculateScore(
                    aggregation, event, (EncryptionPlugin) plugin, storyLine, consequences
            );
        }
        else {
            Double score = generalPluginCalculator.calculateScore(
                    aggregation, event, plugin, storyLine, consequences
            );

            // Get a second opinion in debug
            if (log.isDebugEnabled()) {
                Double v = generalPluginCalculator.calculateScoreVerification(
                        aggregation, event, plugin
                );
                if ((null != score && !score.equals(v)) || (null != v && !v.equals(score))) {
                    String _score = null != score ? Double.toString(score) : "none";
                    String _verification = null != v ? Double.toString(v) : "none";
                    String info = "Calculations differ: score=" + _score + " verification=" + _verification;
                    log.debug(info);
                }
            }
            return score;
        }
    }

    private void ON_INGEST_analysis(
            Aggregation aggregation,
            Map<String, Collection<Plugin>> processMap,
            Collection<String> storyLine,
            Collection<Consequence> consequences,
            MovingAverage interAggregation
    ) {
        boolean noStrongFixityCheck = true;
        boolean noEncryption = true;

        Collection<Plugin> ingest = processMap.get(Event.ON_INGEST);
        if (null == ingest)
            return;

        for (Plugin plugin : ingest) {
            if (plugin instanceof FixityPlugin) {
                FixityPlugin fixityPlugin = (FixityPlugin) plugin;
                double rank = FixityAssessment.rank(fixityPlugin.getFixityAlgorithm());

                if (rank >= FixityAssessment.COMPETENT_ALGORITHM_SCORE) {
                    noStrongFixityCheck = false;
                }
            }
            else if (plugin instanceof EncryptionPlugin) {
                EncryptionPlugin encryptionPlugin = (EncryptionPlugin) plugin;

                String algorithm = encryptionPlugin.getEncryptionAlgorithm();

                // TODO Currently assuming that any type of encryption (except NONE) is OK
                noEncryption = "NONE".equalsIgnoreCase(algorithm);
            }
        }
        for (Purpose purpose : aggregation.getPrimaryPurposes()) {
            String purposeName = purpose.toString();
            if (Purpose.EVIDENCE.equalsIgnoreCase(purposeName)) {
                if (noStrongFixityCheck) {
                    // TODO Update after Year 2 review
                    //interAggregation.update(AggregationLevelScore.FAILURE_SCORE);

                    // ---- Consequences ----
                    double probability = 55.0; // TODO
                    double suitability = 25.0; // TODO

                    //
                    StringBuilder _description = new StringBuilder();
                    _description.append("During ingest no fixity check is prepared or used. ");

                    _description.append("Hence the suitability as ")
                       .append(purposeName.toLowerCase()).append(" is assessed to be ")
                       .append(Double.toString(suitability)).append(" % with probability ")
                       .append(Double.toString(probability)).append(" % of this happening. ");

                    Consequence consequence = new Consequence(probability, suitability, _description.toString());
                    consequences.add(consequence);

                    if (log.isInfoEnabled()) {
                        log.info(_description.toString());
                    }

                    // ---- Story line ----
                    StringBuilder story = new StringBuilder();
                    story.append("purpose=\"").append(purpose).append("\"");
                    story.append(" event=\"").append(Event.ON_INGEST).append("\"");
                    story.append(": \"No strong or competent fixity algorithm used during ingest. It will not guarantee integrity!").append("\"");
                    storyLine.add(story.toString());
                }
            }
        }

        if (noEncryption && aggregation.getNeedsEncryption()) {
            // TODO Update after Year 2 review
            //interAggregation.update(AggregationLevelScore.FAILURE_SCORE);

            // ---- Consequences ----
            double probability = 55.0; // TODO
            double suitability = 25.0; // TODO

            //
            StringBuilder _description = new StringBuilder();
            _description.append("During ingest no encryption is done whereas the requirements state this demand. ");

            _description.append("Hence the suitability as is assessed to be ")
               .append(Double.toString(suitability)).append(" % with probability ")
               .append(Double.toString(probability)).append(" % of this happening. ");

            Consequence consequence = new Consequence(probability, suitability, _description.toString());
            consequences.add(consequence);

            if (log.isInfoEnabled()) {
                log.info(_description.toString());
            }

            // ---- Story line ----
            StringBuilder story = new StringBuilder();
            story.append("event=\"").append(Event.ON_INGEST).append("\"");
            story.append(": \"No encryption scheduled for ingest while the aggregation assumes encryption. It may not guarantee confidentiality!").append("\"");
            storyLine.add(story.toString());
        }
    }

    private void ON_ACCESS_analysis(
            Aggregation aggregation,
            Map<String, Collection<Plugin>> processMap,
            Collection<String> storyLine,
            Collection<Consequence> consequences,
            MovingAverage interAggregation
    ) {
        boolean noStrongFixityCheck = true;
        boolean noEncryption = true;

        Collection<Plugin> access = processMap.get(Event.ON_ACCESS);
        if (null == access)
            return;

        for(Plugin plugin : access) {
            if (plugin instanceof FixityPlugin) {
                FixityPlugin fixityPlugin = (FixityPlugin) plugin;
                double rank = FixityAssessment.rank(fixityPlugin.getFixityAlgorithm());

                if (rank >= FixityAssessment.COMPETENT_ALGORITHM_SCORE) {
                    noStrongFixityCheck = false;
                }
            }
            else if (plugin instanceof EncryptionPlugin) {
                EncryptionPlugin encryptionPlugin = (EncryptionPlugin) plugin;

                String algorithm = encryptionPlugin.getEncryptionAlgorithm();

                // TODO Currently assuming that any type of encryption (except NONE) is OK
                noEncryption = "NONE".equalsIgnoreCase(algorithm);
            }
        }

        for (Purpose purpose : aggregation.getPrimaryPurposes()) {
            String purposeName = purpose.toString();
            if (Purpose.EVIDENCE.equalsIgnoreCase(purposeName)) {
                if (noStrongFixityCheck) {
                    // TODO Update after Year 2 review
                    // interAggregation.update(AggregationLevelScore.FAILURE_SCORE);

                    // ---- Consequences ----
                    double probability = 55.0; // TODO
                    double suitability = 25.0; // TODO

                    //
                    StringBuilder _description = new StringBuilder();
                    _description.append("During access no fixity check is done. ");

                    _description.append("Hence the suitability as ")
                       .append(purposeName.toLowerCase()).append(" is assessed to be ")
                       .append(Double.toString(suitability)).append(" % with probability ")
                       .append(Double.toString(probability)).append(" % of this happening. ");

                    Consequence consequence = new Consequence(probability, suitability, _description.toString());
                    consequences.add(consequence);

                    if (log.isInfoEnabled()) {
                        log.info(_description.toString());
                    }

                    // ---- Story line ----
                    StringBuilder story = new StringBuilder();
                    story.append("purpose=\"").append(purpose).append("\"");
                    story.append(" event=\"").append(Event.ON_ACCESS).append("\"");
                    story.append(": \"No strong or competent fixity algorithm used during access. It will not guarantee integrity!").append("\"");
                    storyLine.add(story.toString());
                }
            }
        }

        if (noEncryption && aggregation.getNeedsEncryption()) {
            // TODO Update after Year 2 review
            //interAggregation.update(AggregationLevelScore.FAILURE_SCORE);

            // ---- Consequences ----
            double probability = 100.0; // TODO
            double suitability = 25.0; // TODO

            //
            StringBuilder _description = new StringBuilder();
            _description.append("During access no decryption is done whereas the requirements state this demand. ");

            _description.append("Hence the suitability as is assessed to be ")
               .append(Double.toString(suitability)).append(" % with probability ")
               .append(Double.toString(probability)).append(" % of this happening. ");

            Consequence consequence = new Consequence(probability, suitability, _description.toString());
            consequences.add(consequence);

            if (log.isInfoEnabled()) {
                log.info(_description.toString());
            }

            // ---- Story line ----
            StringBuilder story = new StringBuilder();
            story.append("event=\"").append(Event.ON_ACCESS).append("\"");
            story.append(": \"No decryption scheduled for access while the aggregation assumes encryption. It may not guarantee confidentiality!").append("\"");
            storyLine.add(story.toString());
        }
    }

    private Double mtbfAnalysis(Aggregation aggregation, Plugin plugin) {
        Double score = null;

        Double mtbf = plugin.getMtbf();
        String unit = plugin.getMtbfUnit();
        if (null != mtbf && null != unit && unit.length() > 0) {
            if ("HOUR".equalsIgnoreCase(unit)) {
                Double mtbfYear = (((mtbf / 24.0) / 7.0) / 52.0);
                int retentionYears = aggregation.getRetentionPeriod();
                if (mtbfYear >= retentionYears) {
                    return 100.0;
                }
                else {
                    score = mtbfYear / retentionYears;
                    score *= 100;
                }
            }
            else {
                String info = "Unknown MTBF unit: " + unit;
                log.warn(info);
            }
        }
        return score;
    }

    /**
     *
     * @param customer
     * @param scores
     * @param aggregationMap
     * @return
     * @throws EvaluationException
     */
    public CustomerLevelScore calculateScore(
            PreservationPlan customer, Collection<AggregationLevelScore> scores, Map<String, String> aggregationMap
    ) throws EvaluationException {

        Collection<String> gppStoryLine = new LinkedList<String>();

        Map<String, Collection<Plugin>> processMap = new HashMap<String, Collection<Plugin>>();

        MovingAverage intraAggregation = new MovingAverage();

        if (log.isDebugEnabled()) {
            log.debug("\n<<Calculating quality metrics...>>");
        }
        List<Aggregation> aggregations = customer.getAggregations();
        for (Aggregation aggregation : aggregations) {
            aggregationMap.put(aggregation.getId(), aggregation.getXml());

            if (log.isDebugEnabled()) {
                StringBuilder buf = new StringBuilder();
                buf.append("\n- Aggregation: ");
                buf.append(" name=\"").append(aggregation.getName());
                buf.append("\" purposes={");
                String separator = "";
                for (Purpose purpose : aggregation.getPrimaryPurposes()) {
                    buf.append(separator).append(purpose.getPurposeClass().name());
                    separator = ", ";
                }
                buf.append("}");
                log.debug(buf);
            }

            Collection<String> aggrStoryLine = new LinkedList<String>();
            Collection<Consequence> consequences = new LinkedList<Consequence>();

            MovingAverage interAggregation = new MovingAverage();

            //--------------------------------------------------------------------
            // Iterate through aggregation specific actions (on a per event basis)
            //--------------------------------------------------------------------
            Collection<Event> events = aggregation.getEvents().values();
            for (Event event : events) {
                Event.EventType type = event.getType();
                String trigger = type.name(); // ON_INGEST, ...

                Collection<Plugin> eventPlugins = processMap.get(trigger);
                if (null == eventPlugins) {
                    eventPlugins = new LinkedList<Plugin>();
                    processMap.put(trigger, eventPlugins);
                }

                if (log.isDebugEnabled()) {
                    StringBuilder buf = new StringBuilder();
                    buf.append("-- Event: ");
                    buf.append(" event=").append(trigger);
                    log.debug(buf);
                }

                // Aggregation specific plugins
                List<Plugin> plugins = event.getPlugins();
                for (Plugin plugin : plugins) {
                    eventPlugins.add(plugin);

                    Double score = calculateScore(aggregation, trigger, plugin, aggrStoryLine, consequences);
                    if (null != score) {
                        if (log.isDebugEnabled()) {
                            log.debug("    score=" + Number.roundTwoDecimals(score) + "%");
                        }
                        interAggregation.update(score);
                    }
                }
            }

            //--------------------------------------------------------------------
            // Iterate through aggregation specific actions (on a per copy basis)
            //--------------------------------------------------------------------
            Collection<Copy> copies = aggregation.getCopies().values();
            for (Copy copy : copies) {
                // Storage plugin
                Plugin plugin = copy.getStoragePlugin();
                Double score = calculateScore(aggregation, /* irrelevant */ null, plugin, aggrStoryLine, consequences);
                if (null != score) {
                    if (log.isDebugEnabled()) {
                        log.debug("    score=" + Number.roundTwoDecimals(score) + "%");
                    }
                    interAggregation.update(score);
                }

                // Compute plugin - if one was assigned
                plugin = copy.getComputePlugin();
                if (null != plugin) {
                    score = calculateScore(aggregation, /* irrelevant */ null, plugin, aggrStoryLine, consequences);
                    if (null != score) {
                        if (log.isDebugEnabled()) {
                            log.debug("    score=" + Number.roundTwoDecimals(score) + "%");
                        }
                        interAggregation.update(score);
                    }
                }
            }

            //--------------------------------------------------------------------
            // Iterate through system actions (on a per event basis). These are
            // actions that apply to all aggregations in addition to the actions
            // that are specific to individual aggregations.
            //--------------------------------------------------------------------
            events = customer.getSystemWideEvents().values();
            for (Event event : events) {
                Event.EventType type = event.getType();
                String trigger = type.name(); // ON_INGEST, ...

                Collection<Plugin> eventPlugins = processMap.get(trigger);
                if (null == eventPlugins) {
                    eventPlugins = new LinkedList<Plugin>();
                    processMap.put(trigger, eventPlugins);
                }

                if (log.isDebugEnabled()) {
                    StringBuilder buf = new StringBuilder();
                    buf.append("-- (System) Event: ");
                    buf.append(" event=").append(trigger);
                    log.debug(buf);
                }

                // These are plugins bound to system actions
                List<Plugin> plugins = event.getPlugins();
                for (Plugin plugin : plugins) {
                    eventPlugins.add(plugin);

                    Double score = calculateScore(aggregation, trigger, plugin, aggrStoryLine, consequences);
                    if (null != score) {
                        if (log.isDebugEnabled()) {
                            log.debug("    score=" + Number.roundTwoDecimals(score) + "%");
                        }
                        interAggregation.update(score);
                    }
                }
            }

            //-----------------------------------------------------------------------------------------------
            // Do analysis on process level ON_INGEST -> ... -> ON_ACCESS
            //   SHA-512 better than MD5 during ingest and access, but using MD5 during storage (ON_TIME) has
            //   no such implications. [OK - covered]
            //   Check whether fixity checks are done during ON_INGEST and ON_ACCESS!  [DONE!]
            //-----------------------------------------------------------------------------------------------
            ON_INGEST_analysis(aggregation, processMap, aggrStoryLine, consequences, interAggregation);
            ON_ACCESS_analysis(aggregation, processMap, aggrStoryLine, consequences, interAggregation);

            //-----------------------------------------------------------------------------------------------
            // If purpose is provided to choose weights, should we track
            // usage-profile i.e. how we want to use the information (frequency, time, ...);
            // "Glacier would be no-good since your usage-profile indicates that the latency is too high"
            // Popularity of material? Time of staying? Number of accesses?
            //-----------------------------------------------------------------------------------------------

            //-----------------------------------------------------------------------------------------------
            // "Quality of service"-aspects, such as uptime of service... MTBF (related to monitoring below)
            //-----------------------------------------------------------------------------------------------
            {
                if (log.isDebugEnabled()) {
                    log.debug("-- Calculations for MTBF");
                }

                // Iterate over all events and all plugins in each event
                /*  Don't assess quality based on MTBF for "normal" plugins
                Collection<Collection<Plugin>> eventCollections = processMap.values();
                for (Collection<Plugin> collection : eventCollections) {
                    for (Plugin plugin : collection) {
                        Double score = mtbfAnalysis(aggregation, plugin);
                        if (null != score) {
                            if (log.isDebugEnabled()) {
                                log.debug("    score (mtbf)=" + Number.roundTwoDecimals(score) + "%");
                            }
                            interAggregation.update(score);
                        }
                    }
                }
                */

                // Iterate over copy storage plugins
                for (Copy copy : aggregation.getCopies().values()) {
                    Plugin plugin = copy.getStoragePlugin();
                    if (null != plugin) {
                        Double score = mtbfAnalysis(aggregation, plugin);
                        if (null != score) {
                            if (log.isDebugEnabled()) {
                                log.debug("--- Plugin: " + plugin.getServiceProvider() + "/" + plugin.getSystemId());
                                log.debug("    score (mtbf)=" + Number.roundTwoDecimals(score) + "%");
                            }
                            interAggregation.update(score);
                        }
                    }
                    /* Skip compute plugin - only storage plugin considered for MTBF analysis.
                    plugin = copy.getComputePlugin();
                    if (null != plugin) {
                        Double score = mtbfAnalysis(aggregation, plugin);
                        if (null != score) {
                            if (log.isDebugEnabled()) {
                                log.debug("--- Plugin: " + plugin.getServiceProvider() + "/" + plugin.getSystemId());
                                log.debug("    score (mtbf)=" + Number.roundTwoDecimals(score) + "%");
                            }
                            interAggregation.update(score);
                        }
                    }
                    */
                }
            }

            //-----------------------------------------------------------------------------------------------
            // Could we do anything with information generated by PDSCloud/PDALM during use,
            // adjusting auxiliary data for given plugins. I.e. monitoring information fed back
            // to auxiliary data and used when determining quality of plugins. [Independent on
            // events etc]
            //-----------------------------------------------------------------------------------------------

            //-----------------------------------------------------------------------------------------------
            // Running a transformation plugin from a provider on a specific compute cloud component,
            // should the choice of platform be judged as part of judging transformation plugin. How does
            // using a specific version of the JRE influence the score? (since we are in part depending on
            // built-in functionality for, say, hash-value calculations)
            //-----------------------------------------------------------------------------------------------

            // Update GPP data
            intraAggregation.update(interAggregation.getAverage());

            // Aggregation data
            AggregationLevelScore score = new AggregationLevelScore(
                    aggregation.getId(), aggregation.getName(), interAggregation, aggrStoryLine, consequences
            );
            scores.add(score);

            if (log.isDebugEnabled()) {
                double aggrScore = Number.roundTwoDecimals(interAggregation.getAverage());
                double aggrCV = Number.roundTwoDecimals(interAggregation.getCV());
                long aggSetSize = interAggregation.getCount();

                String info = "Aggregation score=" + Number.roundTwoDecimals(aggrScore) + "% ";
                info += "cv=" + Number.roundTwoDecimals(aggrCV) + "% ";
                info += "data-set=" + aggSetSize + " pts";
                log.debug(info);
            }

            if (log.isDebugEnabled()) {
                log.debug("Aggregation story line:");
                for (String story : aggrStoryLine) {
                    log.debug(" * " + story);
                }
            }
        }

        //-----------------------------------------------------------------------------------------------
        // Analysis of risks.
        //
        //
        //-----------------------------------------------------------------------------------------------
        double customerImpact = 0.0;
        double providerImpact = 0.0;
        double reconfigurationImpact = 0.0;

        // GPP data
        if (log.isDebugEnabled()) {
            log.debug("GPP story line:");
            for (String story : gppStoryLine) {
                log.debug(" * " + story);
            }
        }
        return new CustomerLevelScore(customer.getId(), intraAggregation, customerImpact, providerImpact, reconfigurationImpact, gppStoryLine);
    }
}
