/*
 * Copyright (C) 2011-2014 Frode Randers
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
package eu.ensure.commons.db;

import java.sql.*;

/**
 * Description of LocalDatabase:
 * <p/>
 * Convenience code for handling a local Derby database
 * <p/>
 * Created by Frode Randers at 2011-11-04 14:14
 */
 public class LocalDatabase {
    private static final String JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    public static void startup() {
        // Load the Derby JDBC driver
        try {
            Class.forName(JDBC_DRIVER).newInstance();

        } catch (Exception e) {
            String info = "Could not load JDBC driver for Derby: ";
            info += e.getMessage();
            System.err.println(info);
        }
    }

    public static void shutdown() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");

        } catch (SQLException sqle) {
            // This connection request explicitly shuts down the database, which
            // ensures that database files are in a consistent state and there are
            // no outstanding records in the transaction log.
            // SQL state "08006" indicates no error per se -
            // it just confirms a successful shutdown.

            String info = "Database shutdown ";
            String state = sqle.getSQLState();
            if ("08006".equals(state) || "XJ015".equals(state)) {
                info += "was successful";
            } else {
                info += "reports error: ";
                info += Database.squeeze(sqle);
            }
            System.err.println(info);
        }
    }

    public static void createObject(Connection conn, String ddl) throws SQLException {
        PreparedStatement pStmt = null;
        try {
            pStmt = conn.prepareStatement(ddl);
            pStmt.executeUpdate();

        } catch (SQLException sqle) {
            // We explicitly choose to accept that an object, such as
            // a table, already exists.
            if (! "X0Y32".equals(sqle.getSQLState())) {
                throw sqle;
            }
        } finally {
            if (pStmt != null) pStmt.close();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:derby:localdb;create=true");
    }
}
