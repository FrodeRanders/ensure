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

import eu.ensure.commons.db.Database;
import org.apache.log4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Retrieves 'motivation' from the assessment database, to annotate consequence descriptions.
 * <p>
 * Created by Frode Randers at 2013-02-21 10:24
 */
public class AssessmentInfoRetriever {
    private static final Logger log = Logger.getLogger(AssessmentInfoRetriever.class);

    static final String RETRIEVE_ASSESSMENT_MOTIVATION_STMT =
                "SELECT source, motivation, condition, description FROM ppe_assessments WHERE assessmentid = ?";

    public enum Condition {
        UNSPECIFIED,
        SHALL,
        MAY
    }

    public class Details {
        public String assessmentId;
        public String source;
        public String motivation;
        public Condition condition;
        public String description;

        Details(String assessmentId, String source, String motivation, Condition condition, String description) {
            this.assessmentId = assessmentId;
            this.source = source;
            this.motivation = motivation;
            this.condition = condition;
            this.description = description;
        }

        public String getAssessmentId() {
            return assessmentId;
        }

        public String getSource() {
            return source;
        }

        public boolean hasMotivation() {
            return null != motivation && motivation.length() > 0;
        }

        public String getMotivation() {
            return motivation;
        }

        public Condition getCondition() {
            return condition;
        }

        public boolean hasDescription() {
            return null != description && description.length() > 0;
        }

        public String getDescription() {
            return description;
        }
    }


    private DataSource dataSource;

    public AssessmentInfoRetriever(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Details getMotivation(String assessmentId) {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(RETRIEVE_ASSESSMENT_MOTIVATION_STMT, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            int count;

            stmt.setString(count = 1, assessmentId);
            rs = Database.executeQuery(stmt);

            if (rs.next()) {
                String source = rs.getString(count = 1);
                String motivation = rs.getString(++count); // may be null

                Condition condition;
                Integer _condition = rs.getInt(++count); // may be null
                if (rs.wasNull()) {
                    // No condition was provided in the database
                    condition = Condition.UNSPECIFIED;
                }
                else {
                    switch (_condition) {
                        case 0:
                            condition = Condition.SHALL;
                            break;

                        case 1:
                            condition = Condition.MAY;
                            break;

                        default:
                            condition = Condition.UNSPECIFIED;
                            break;
                    }
                }

                String description = rs.getString(++count); // may be null

                return new Details(assessmentId, source, motivation, condition, description);
            }
        }
        catch (SQLException sqle) {
            String info = "Failed to retrieve assessment details: ";
            info += Database.squeeze(sqle);
            log.warn(info);
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
        return null;
    }
}
