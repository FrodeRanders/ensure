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
 */package eu.ensure.ppe.derby;

import org.apache.log4j.Logger;

import java.sql.*;

import eu.ensure.commons.db.Database;

/**
 * Description of StoredProcedure:
 * <p/>
 * <p/>
 * Created by Frode Randers at 2012-09-08 14:14
 */
public class StoredProcedure {

    protected static Logger log = Logger.getLogger(StoredProcedure.class);

    public static final int INITIAL_ID = 1000;

    /*
     * Example of declaration of stored procedure:
     *
     * CREATE PROCEDURE ppe_allocate_id(IN value_count INTEGER,
     *   OUT first_id INTEGER, OUT last_id INTEGER)
     *   PARAMETER STYLE JAVA MODIFIES SQL DATA LANGUAGE JAVA
     *   EXTERNAL NAME 'eu.ensure.ppe.derby.StoredProcedure.allocateId';
     *
     * which has functionality somewhat like this:
     *
     * CREATE PROCEDURE ppe_allocate_id
     *   @value_count INTEGER = 1,
     *   @first_id INTEGER OUT,
     *   @last_id INTEGER OUT
     * AS
     *   BEGIN TRANSACTION
     *     SELECT @first_id=lastkey+1 FROM ppe_unique_keystore
     *     UPDATE ppe_unique_keystore set lastkey=lastkey+@value_count
     *     IF @@rowcount=1
     *     BEGIN
     *       SELECT @last_id=lastkey FROM ppe_unique_keystore
     *     END
     *   COMMIT TRANSACTION
     */

    private static boolean getCurrentId(Connection conn, String tableName, int[] currentId) throws SQLException {
        int id = 1000; // default id
        boolean successfullyReadValue = false;

        Statement stmt = null;
        ResultSet rs = null;
        try  {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT lastkey FROM " + tableName);
            if (rs.next()) {
                id = rs.getInt(1);
                successfullyReadValue =  true;
            }
        } finally {
            if (null != rs) rs.close();
            if (null != stmt) stmt.close();
        }
        currentId[0] = id;
        return successfullyReadValue;
    }

    private static int setNextId(Connection conn, String tableName, int nextId, boolean anIdExists) throws SQLException {
        Statement stmt = null;
        int rowCount = 0;
        try {
            stmt = conn.createStatement();
            if (anIdExists) {
                rowCount = stmt.executeUpdate("UPDATE " + tableName + " SET lastkey=" + nextId);
            } else {
                rowCount = stmt.executeUpdate("INSERT INTO " + tableName + " (lastkey) VALUES (" + nextId + ")");
            }
        } finally {
            if (null != stmt) stmt.close();
        }
        return rowCount;
    }

    /**
     * Generic object allocation method for allocating a RANGE of ids.
     * <p/>
     */
    private static void allocateIdRange(String tableName, int valueCount, int[] out1, int[] out2) {
        // In case table is empty, this is what we want
        int firstId = 0;
        int nextId = firstId + valueCount;

        //
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:default:connection");
            conn.setAutoCommit(/* no automatic commits */ false);

            // Get current id (will be 1000 if table was empty)
            int[] currentId = {0};
            boolean gotCurrentId = getCurrentId(conn, tableName, currentId);
            firstId = currentId[0];

            //
            nextId = firstId + valueCount; // incremented with requested count

            int rowCount = setNextId(conn, tableName, nextId, /* a current id exists? */ gotCurrentId);
            if (1 != rowCount) {
                String info = "Failed to save updated object count in " + tableName;
                info += ": rowcount from update not as expected";
                log.warn(info);
            }

            // Just to be sure...
            gotCurrentId = getCurrentId(conn, tableName, currentId);
            if (!gotCurrentId) {
                String info = "Failed to store id to " + tableName;
                log.warn(info);
            } else if (currentId[0] != nextId) {
                String info = "Mismatch object count in " + tableName;
                info += ": requeried lastkey (" + currentId[0] + ") does not match expected value (" + nextId + ")";
                log.warn(info);
            }

            // 
            conn.commit();
            
        } catch (SQLException sqle) {
            String info = "Failed to allocate object count from " + tableName;
            info += ": " + Database.squeeze(sqle);

            if (null != conn) {
                try {
                    conn.rollback();
                } catch (SQLException sqle2) {
                    info += ": Failed to rollback transaction!";
                    info += Database.squeeze(sqle2);
                }
            }
            log.warn(info);
        } finally {
            try {
                if (null != conn) conn.close();
            } catch (SQLException sqle) {
                /* ignore */
            }
        }
        //-------------------------------------------------------------------
        // OBSERVE:
        //   'nextId' is one above the requested highest id in range!
        //-------------------------------------------------------------------
        out1[0] = firstId;
        out2[0] = nextId - 1;
    }

    /**
     * Implementation of the SQL procedure 'ppe_allocate_id'.
     * <p/>
     * <pre>
     * CREATE PROCEDURE ppe_allocate_id(IN value_count INTEGER,
     *   OUT first_id INTEGER, OUT last_id INTEGER)
     *   PARAMETER STYLE JAVA MODIFIES SQL DATA LANGUAGE JAVA
     *   EXTERNAL NAME 'eu.ensure.ppe.derby.StoredProcedure.allocateId';
     * </pre>
     * <p/>
     */
    public static void allocateId(int valueCount, int[] out1, int[] out2) {
        allocateIdRange("ppe_unique_keystore", valueCount, out1, out2);
    }




    /**
     * Currently a simple recalculator, somewhat corresponding to these PL/SQL lines:
     * <pre>
     * DECLARE
     *     v_max_id NUMBER(38);
     * BEGIN
     *     SELECT MAX(id)+1 INTO v_max_id FROM ppe_template;
     *
     *     IF SQL%ROWCOUNT = 1 AND v_max_id > 999 THEN
     *         BEGIN
     *             DELETE FROM ppe_unique_keystore;
     *             INSERT INTO ppe_unique_keystore ( lastkey ) VALUES ( v_max_id );
     *         END;
     *     END IF;
     * END;
     * </pre>
     * <p/>
     */
    private static void recalculateByMaximizing(
            Connection conn, String srcTableName, String srcColumnName, int defaultValue)
            throws SQLException
    {
        String dstTableName = "ppe_" + srcColumnName + "_keystore";

        Statement stmt = null;
        ResultSet rs = null;
        try  {
            stmt = conn.createStatement();

            // SELECT MAX(id)+1 INTO v_max_id FROM ppe_template;
            rs = stmt.executeQuery("SELECT MAX(" + srcColumnName + ")+1 FROM " + srcTableName);

            int maxId = defaultValue;
            if (rs.next()) {
                maxId = rs.getInt(1);
            }
            rs.close();
            stmt.close();

            // DELETE FROM ppe_unique_keystore;
            stmt = conn.createStatement();
            stmt.executeUpdate("DELETE FROM " + dstTableName); // ignore rowcount
            stmt.close();

            // We will skip the initial segment, starting off with 1000 if
            // no entry is segment has been allocated.
            int nextId = Math.max(maxId, INITIAL_ID);

            // INSERT INTO ppe_unique_keystore ( lastkey ) VALUES ( v_max_id );
            stmt = conn.createStatement();
            int rowCount = stmt.executeUpdate("INSERT INTO " + dstTableName + " ( lastkey ) VALUES ( " + nextId + " )");

            if (rowCount == 1) {
                log.info("Successfully recalculated " + dstTableName + ", assigning next available id " + nextId);
            } else {
                log.warn("Failed to recalculate " + dstTableName + ", " + rowCount + " rows were affected");
            }
        } finally {
            if (null != rs) rs.close();
            if (null != stmt) stmt.close();
        }
    }

    /**
     * Recalculate counters for all keystores
     */
    public static void recalculateCounters() {
        Connection conn = null;
        try {
            log.info("Recalculating counters for id sources...");
            
            conn = DriverManager.getConnection("jdbc:default:connection");
            conn.setAutoCommit(/* no automatic commits */ false);

            //recalculateByMaximizing(conn, "ppe_template", "id", INITIAL_ID);

            //
            conn.commit();

        } catch (SQLException sqle) {
            String info = "Failed to recalculate counters: ";
            info += Database.squeeze(sqle);

            if (null != conn) {
                try {
                    conn.rollback();
                } catch (SQLException sqle2) {
                    info += ": Failed to rollback transaction!";
                    info += Database.squeeze(sqle2);
                }
            }
            log.warn(info);
        } finally {
            try {
                if (null != conn) conn.close();
            } catch (SQLException sqle) {
                /* ignore */
            }
        }
    }
}
