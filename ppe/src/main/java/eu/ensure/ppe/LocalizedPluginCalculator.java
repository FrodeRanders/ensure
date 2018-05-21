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


import org.gautelis.vopn.db.Database;
import eu.ensure.ppe.model.Aggregation;
import eu.ensure.ppe.model.Consequence;
import eu.ensure.ppe.model.LocalizedPlugin;
import eu.ensure.ppe.model.Purpose;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Calculates score for localized plugins (such as StoragePlugin:s or ComputePlugin:s).
 * <p>
 * Created by Frode Randers at 2012-10-16 17:57
 */
public class LocalizedPluginCalculator {
    private static final Logger log = LogManager.getLogger(LocalizedPluginCalculator.class);

    static final String PLUGIN_ASSESSMENT_STMT =
                "SELECT DISTINCT " +
                "        w.assessmentid, w.purpose, w.weight, w.suitability, " +
                "        a.source, " +
                "        o.providerid, o.systemid, o.assessment, o.probability " +
                "FROM ppe_weights w " +
                "INNER JOIN ppe_assessments a ON " +
                "        w.assessmentid = a.assessmentid " +
                "LEFT OUTER JOIN ppe_opinions o ON ( " +
                "        w.assessmentid = o.assessmentid " +
                "   AND  LOWER(o.providerid) = ? " +
                "   AND  LOWER(o.systemid) = ? " +
                "   AND  (o.locality IS NULL OR o.locality = ?) " +
                ") " +
                "WHERE LOWER(w.purpose) = ?";

    private AssessmentInfoRetriever infoRetriever;
    private DataSource dataSource;

    public LocalizedPluginCalculator(DataSource dataSource) {
        this.dataSource = dataSource;
        this.infoRetriever = new AssessmentInfoRetriever(dataSource);
    }

    public Double calculateScore(
            Aggregation aggregation,
            String event,
            LocalizedPlugin plugin,
            Collection<String> storyLine,
            Collection<Consequence> consequences
    ) throws EvaluationException {

        String pluginId = plugin.getPluginId();
        String providerId = plugin.getServiceProvider();
        String systemId = plugin.getSystemId();
        String locality = plugin.getLocality();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(PLUGIN_ASSESSMENT_STMT, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            int count;

            long sum = 0L, max = 0L;

            for (Purpose purpose : aggregation.getPrimaryPurposes()) {
                String purposeName = purpose.getPurposeClass().name();
                stmt.clearParameters();
                stmt.setString(count = 1, providerId.toLowerCase());
                stmt.setString(++count, systemId.toLowerCase());
                stmt.setString(++count, locality.toLowerCase());
                stmt.setString(++count, purposeName.toLowerCase());
                rs = Database.executeQuery(stmt);

                while (rs.next()) {
                    String assessmentId = rs.getString("assessmentid");

                    Integer weight = rs.getInt("weight");
                    if (rs.wasNull()) {
                        weight = 1; // the lowest
                    }

                    int outcome;
                    Integer assessment = rs.getInt("assessment");
                    if (!rs.wasNull()) {
                        // Only consider assessment when we actually have a value
                        outcome = assessment * weight;
                        sum += outcome;
                        max += weight;

                        if (weight > 1 && outcome < weight) {

                            // ---- Consequences ----
                            String source = rs.getString("source");
                            double probability = rs.getDouble("probability");
                            double suitability = rs.getDouble("suitability");

                            //
                            StringBuilder description = new StringBuilder();
                            description.append("The plugin \"")
                               .append(providerId).append("/").append(systemId).append("-").append(locality)
                               .append("\" (").append(pluginId).append(")")
                               .append(" does not match the ").append(source)
                               .append(" requirement \"").append(assessmentId).append("\". ");

                            description.append("The plugin scored \"")
                               .append(outcome).append("\" points with weight \"")
                               .append(weight).append("\". ");

                            description.append("Hence the suitability is assessed to be ")
                               .append(Double.toString(suitability)).append(" % with probability ")
                               .append(Double.toString(probability)).append(" % of this happening. ");

                            Consequence consequence = new Consequence(probability, suitability, description.toString());
                            consequences.add(consequence);

                            if (log.isInfoEnabled()) {
                                log.info(description.toString());
                            }

                            // ---- Story line ----
                            StringBuilder story = new StringBuilder();
                            story.append("purpose=\"").append(purpose).append("\"")
                                 .append(" assessmentid=\"").append(assessmentId).append("\"")
                                 .append(" weight=\"").append(weight).append("\"")
                                 .append(" outcome=\"").append(outcome).append("\"")
                                 .append(" plugin=\"").append(providerId).append(":").append(systemId).append("-").append(locality).append("\"")
                                 .append(" (").append(plugin.getType()).append(")")
                                    .append(": \"This plugin is not appropriate");

                               AssessmentInfoRetriever.Details details = infoRetriever.getMotivation(assessmentId);
                               if (null != details && details.hasMotivation()) {
                                   story.append(" because: ").append(details.getMotivation())
                                        .append(" (").append(details.getSource()).append(")");
                               }
                               story.append("\"");

                               storyLine.add(story.toString());
                        }
                    }
                }
            }
            // Score [0.0 - 100.0]
            if (max > 0L) {
                double score = ((double) sum) / ((double) max);
                score *= 100;
                return score;
            }
            else {
                return null; // No score was calculated
            }
        }
        catch (SQLException sqle) {
            String info = "Failed to calculate score: ";
            info += Database.squeeze(sqle);
            throw new EvaluationException(info, sqle);
        }
        finally {
            try {
                if (null != rs) rs.close();
                if (null != stmt) stmt.close();
                if (null != conn) conn.close();
            }
            catch (SQLException ignore) {
            }
        }
    }

    static final String PLUGIN_ASSESSMENT_VERIFICATION_STMT =
                // Oviktade värden, dvs vi har en åsikt, men inte något entry i ppe_weights
                "SELECT PROVIDERID, SYSTEMID, ASSESSMENTID, ASSESSMENT AS CalcValue, 1 AS WEIGHT FROM PPE_OPINIONS " +
                "WHERE LOWER(PPE_OPINIONS.PROVIDERID) = ? " +
                "AND LOWER(PPE_OPINIONS.SYSTEMID) = ? " +
              //"AND LOWER(PPE_OPINIONS.LOCALITY) = ? " +
                "AND PPE_OPINIONS.ASSESSMENT IS NOT NULL " +
                "AND ASSESSMENTID NOT IN (SELECT ASSESSMENTID FROM PPE_WEIGHTS WHERE LOWER(PURPOSE) = ?) " +
                "UNION " +
                // Viktade värden, dvs vi har både en åsikt och en vikt
                "SELECT PROVIDERID, SYSTEMID ,PPE_OPINIONS.ASSESSMENTID, " +
                "PPE_OPINIONS.ASSESSMENT * PPE_WEIGHTS.WEIGHT AS CalcValue, PPE_WEIGHTS.WEIGHT AS WEIGHT " +
                "FROM PPE_OPINIONS " +
                "INNER JOIN PPE_WEIGHTS " +
                "ON PPE_OPINIONS.ASSESSMENTID = PPE_WEIGHTS.ASSESSMENTID " +
                "WHERE LOWER(PPE_OPINIONS.PROVIDERID) = ? " +
                "AND LOWER(PPE_OPINIONS.SYSTEMID) = ? " +
              //"AND LOWER(PPE_OPINIONS.LOCALITY) = ? " +
                "AND LOWER(PPE_WEIGHTS.PURPOSE) = ?";

    static final String MAX_PLUGIN_ASSESSMENT_VERIFICATION_STMT =
                "SELECT PROVIDERID, SYSTEMID, ASSESSMENTID, " +
                "CASE WHEN ASSESSMENT = 0 THEN 1 ELSE ASSESSMENT*1 END " +
                "AS CalcValue FROM PPE_OPINIONS " +
                "WHERE LOWER(PPE_OPINIONS.PROVIDERID) = ? " +
                "AND LOWER(PPE_OPINIONS.SYSTEMID) = ? " +
              //"AND LOWER(PPE_OPINIONS.LOCALITY) = ? " +
                "AND PPE_OPINIONS.ASSESSMENT IS NOT NULL " +
                "AND ASSESSMENTID NOT IN (SELECT ASSESSMENTID FROM PPE_WEIGHTS WHERE LOWER(PURPOSE) = ?) " +
                "UNION " +
                "SELECT PROVIDERID, SYSTEMID ,PPE_OPINIONS.ASSESSMENTID, " +
                "CASE WHEN ASSESSMENT = 0 THEN (ASSESSMENT+1)*PPE_WEIGHTS.WEIGHT ELSE PPE_OPINIONS.ASSESSMENT * PPE_WEIGHTS.WEIGHT END " +
                "AS CalcValue " +
                "FROM PPE_OPINIONS " +
                "INNER JOIN PPE_WEIGHTS " +
                "ON PPE_OPINIONS.ASSESSMENTID = PPE_WEIGHTS.ASSESSMENTID " +
                "WHERE LOWER(PPE_OPINIONS.PROVIDERID) = ? " +
                "AND LOWER(PPE_OPINIONS.SYSTEMID) = ? " +
              //"AND LOWER(PPE_OPINIONS.LOCALITY) = ? " +
                "AND LOWER(PPE_WEIGHTS.PURPOSE) = ?";

    public Double calculateScoreVerification(
            Aggregation aggregation,
            String event,
            LocalizedPlugin plugin
    ) throws EvaluationException {

        String providerId = plugin.getServiceProvider();
        String systemId = plugin.getSystemId();
        String locality = plugin.getLocality();

        Connection conn = null;
        PreparedStatement sumStmt = null;
        PreparedStatement maxStmt = null;
        ResultSet rs = null;
        try {
            long sum = 0L;
            long max = 0L;

            //
            conn = dataSource.getConnection();
            sumStmt = conn.prepareStatement(PLUGIN_ASSESSMENT_VERIFICATION_STMT, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            maxStmt = conn.prepareStatement(MAX_PLUGIN_ASSESSMENT_VERIFICATION_STMT, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            int count;

            for (Purpose purpose : aggregation.getPrimaryPurposes()) {
                String purposeName = purpose.toString();
                sumStmt.clearParameters();
                sumStmt.setString(count = 1, providerId.toLowerCase());
                sumStmt.setString(++count, systemId.toLowerCase());
                //sumStmt.setString(++count, locality.toLowerCase()); // TODO
                sumStmt.setString(++count, purposeName.toLowerCase());
                sumStmt.setString(++count, providerId.toLowerCase());
                sumStmt.setString(++count, systemId.toLowerCase());
                //sumStmt.setString(++count, locality.toLowerCase()); // TODO
                sumStmt.setString(++count, purposeName.toLowerCase());
                rs = Database.executeQuery(sumStmt);

                while (rs.next()) {
                    String assessmentId = rs.getString("assessmentid");
                    Integer outcome = rs.getInt("CalcValue");
                    if (!rs.wasNull()) {
                        // Only consider assessment when we actually have a value
                        sum += outcome;
                    }
                }
                rs.close();

                maxStmt.clearParameters();
                maxStmt.setString(count = 1, providerId.toLowerCase());
                maxStmt.setString(++count, systemId.toLowerCase());
                //maxStmt.setString(++count, locality.toLowerCase()); // TODO
                maxStmt.setString(++count, purposeName.toLowerCase());
                maxStmt.setString(++count, providerId.toLowerCase());
                maxStmt.setString(++count, systemId.toLowerCase());
                //maxStmt.setString(++count, locality.toLowerCase()); // TODO
                maxStmt.setString(++count, purposeName.toLowerCase());
                rs = Database.executeQuery(maxStmt);

                while (rs.next()) {
                    Integer outcome = rs.getInt("CalcValue");
                    if (!rs.wasNull()) {
                        // Only consider assessment when we actually have a value
                        max += outcome;
                    }
                }
                rs.close();
            }

            if (max > 0L) {
                // Score [0.0 - 100.0]
                double score = ((double) sum) / ((double) max);
                score *= 100;
                return score;
            }
            else {
                return null; // no score for this plugin
            }

        }
        catch (SQLException sqle) {
            String info = "Failed to calculate score: ";
            info += Database.squeeze(sqle);
            throw new EvaluationException(info, sqle);
        }
        finally {
            try {
                if (null != rs) rs.close();
                if (null != sumStmt) sumStmt.close();
                if (null != maxStmt) maxStmt.close();
                if (null != conn) conn.close();
            }
            catch (SQLException ignore) {
            }
        }
    }
}
