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
package eu.ensure.commons.db.utils;

import eu.ensure.commons.db.Database;
import eu.ensure.commons.db.DatabaseException;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.List;


/**
 * Description of Manager:
 * <p>
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public abstract class Manager {
    public interface PrepareDataSource {
        void prepare(DataSource ds, Database.Configuration configuration);
    }

    protected static final boolean ACCEPT_FAILURE = true;
    protected static final boolean BREAK_ON_FAILURE = false;

    //
    private Properties properties = null;
    private Options options = null;

    //
    protected DataSource dataSource;

    // Information on how to read batch file
    protected Characteristics characteristics = null;

    private Manager(PrepareDataSource preparer, Properties properties, Options options) throws DatabaseException {
        this.properties = properties;
        this.options = options;

        Database.Configuration configuration = Database.getConfiguration(properties);
        dataSource = Database.getDataSource(configuration);
        preparer.prepare(dataSource, configuration);
    }

    /**
     * Constructs a Manager object that will parse the DDL, demanding
     * batch separator alone on line (ignoring case).
     */
    protected Manager(PrepareDataSource preparer, Properties properties, Options options, String batchSeparator) throws DatabaseException {
        this(preparer, properties, options);
        characteristics = new Characteristics(batchSeparator);
    }

    /**
     * Constructs a Manager object that will parse the DDL,
     * as specified by the parameters.
     *
     * @see Characteristics
     */
    protected Manager(
            PrepareDataSource preparer, Properties properties,
            Options options, String batchSeparator, boolean ignoreCase, boolean separatorAloneOnLine
    ) throws DatabaseException {

        this(preparer, properties, options);
        characteristics = new Characteristics(batchSeparator, ignoreCase, separatorAloneOnLine);
    }

    /**
     * Constructs a Manager object that will parse DDL.
     */
    protected Manager(DataSource dataSource, Options options,
                    String batchSeparator, boolean ignoreCase, boolean separatorAloneOnLine) {
        this.dataSource = dataSource;
        this.options = options;
        this.properties = new Properties(); // empty and will not be used

        characteristics = new Characteristics(batchSeparator, ignoreCase, separatorAloneOnLine);
    }

    /**
     * Constructs a Manager object that will parse DDL, demanding
     * batch separator alone on line (ignoring case).
     */
    protected Manager(DataSource dataSource, Options options,
                    String batchSeparator) {
        this.dataSource = dataSource;
        this.options = options;
        this.properties = new Properties(); // empty and will not be used

        characteristics = new Characteristics(batchSeparator);
    }

    /**
     * Returns the internal DataSource, in case operations should continue after Manager ceases its work.
     * @return
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Executes any SQL script file
     */
    public void execute(String name, Reader sqlCode) throws Exception {
        runScript(name, sqlCode);
    }

    /**
     * Check if file exists
     */
    protected boolean fileExist(String filename) {
        File file = new File(filename);
        return file.exists();
    }


    /**
     */
    protected void runScript(String name, Reader sqlCode) throws Exception {
        //Exception here = new Exception("synthetic");
        //here.printStackTrace();
        
        try {
            BatchReader batch = new BatchReader(options, characteristics);
            List<String> sqls = batch.readFile(sqlCode);

            System.out.println("Running script \"" + name + "\"...");
            for (String sql : sqls) {
                execute(sql, ACCEPT_FAILURE); // or BREAK_ON_FAILURE?
            }
        } catch (SQLException sqle) {
            System.out.println(" - Script failed");

            if (options.debug) {
                System.out.println(Database.squeeze(sqle));
            }
            throw new Exception("Caught exception when running script\n" + sqle.toString());

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Caught exception when running script\n" + e.toString());
        }
    }


    /**
     * Executes an SQL statement.
     * <p>
     *
     * @param sqlStatement
     * @throws Exception
     */
    protected void execute(String sqlStatement, boolean acceptFailure) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            boolean success = execute(stmt, sqlStatement, acceptFailure);

            // Loop over all kinds of results.
            while (success) { /* 'success' is not modified below this line */
                int rowCount = stmt.getUpdateCount();

                if (rowCount > 0) {
                    // --------------------------------------------------
                    // Result of successful INSERT or UPDATE or the like
                    // --------------------------------------------------
                    if (options.debug)
                        System.out.println("Rows affected: " + rowCount);

                    if (stmt.getMoreResults()) {
                        continue;
                    }

                } else if (rowCount == 0) {
                    // --------------------------------------------------
                    // Either a DDL command or 0 updates
                    // --------------------------------------------------
                    if (options.debug)
                        System.out.println("No rows affected or statement was DDL command");

                    boolean moreResults = false;
                    try {
                        moreResults = stmt.getMoreResults();
                    } catch (SQLException sqle) {

                        // State: 24000 - Invalid cursor state
                        //  [Teradata: Continue request submitted but no response to return]
                        if (sqle.getSQLState().startsWith("24")) {
                            break;

                        } else {
                            throw sqle;
                        }
                    }
                    if (moreResults) {
                        continue;
                    }

                } else { // rowCount < 0
                    // --------------------------------------------------
                    // Either we have a result set or no more results...
                    // --------------------------------------------------
                    ResultSet rs = stmt.getResultSet();
                    if (null != rs) {
                        // Ignore resultset
                        rs.close();

                        if (stmt.getMoreResults()) {
                            continue;
                        }
                    }
                }

                // No more results
                break;
            }
        } catch (SQLException sqle) {
            System.out.println("Failed to execute statement: \n" + sqlStatement);
            System.out.println("\n\nDescription of failure: \n" + Database.squeeze(sqle));
            throw sqle;

        } catch (Exception e) {
            System.out.println("Failed to execute statement: \n" + sqlStatement);
            System.out.println("\n\nDescription of failure: \n" + e.getMessage());
            throw e;

        } finally {
            // Cleanup
            if (null != stmt) stmt.close();
            if (null != conn) conn.close();
        }
    }

    protected boolean execute(Statement stmt, String sqlStatement, boolean acceptFailure) throws SQLException {
        int i = 20; // number of retries (see variable info below)

        try {
            SQLException last = null;
            do {

                try {
                    stmt.execute(sqlStatement);
                    return true; // success

                } catch (SQLException sqle) {
                    last = sqle;

                    // State: 40001 - Deadlock
                    if (sqle.getSQLState().startsWith("40")) {
                        if (options.debug) {
                            System.out.println("Deadlock detected in database, retrying " + (i - 1) + " times...");
                        }
                        try {
                            Thread.sleep(200 /* ms to sleep */);
                        } catch (Exception e) { /* ignore */ }
                    } else {
                        throw sqle;
                    }
                }
            } while (--i > 0);
            String info = "Giving up on deadlock after 20 retries";
            System.out.println(info);

            throw last;

        } catch (SQLException sqle) {
            if (!acceptFailure) {
                System.err.println("-------------------------------------------------------------------------");
                System.err.println(Database.squeeze(sqle));
                System.err.println("Incorrect statement was: ");
                System.err.println(sqlStatement);
                System.err.println("-------------------------------------------------------------------------");
            }
            
            if (acceptFailure) {
                // State: 01000 - Generic warning
                //  [SQL Server: Object already exists]
                if (sqle.getSQLState().equals("01000")) {
                    if (options.debug) {
                        System.out.println("Statement: " + sqlStatement);
                        System.out.println(Database.squeeze(sqle));
                        System.out.println("\n[OK]: Object already exists - ACCEPTED!");
                    }
                    return false; // no success - but acceptable
                }
                // State: 23000 - Integrity constraint/key violation
                //  [SQL Server: Data already exists]
                //  [Oracle:     Data already exists]
                else if (sqle.getSQLState().startsWith("23")) {
                    if (options.debug) {
                        System.out.println("Statement: " + sqlStatement);
                        System.out.println(Database.squeeze(sqle));
                        System.out.println("\n[OK]: Data already exists - ACCEPTED!");
                    }
                    return false; // no success - but acceptable
                }
                // State: 42000 - Syntax error or access violation
                //  [Oracle: Object already exists]
                //  [Oracle: Table or view does not exist]
                else if (sqle.getSQLState().startsWith("42")) {
                    if (options.debug) {
                        System.out.println("Statement: " + sqlStatement);
                        System.out.println(Database.squeeze(sqle));
                        System.out.println("\n[OK]: Either object already exists or it does not exist - ACCEPTED!");
                    }
                    return false; // no success - but acceptable
                }
                // State: 72000 - SQL execute phase errors
                // [Oracle: such column list already indexed]
                else if (sqle.getSQLState().startsWith("72")) {
                    if (options.debug) {
                        System.out.println("Statement: " + sqlStatement);
                        System.out.println(Database.squeeze(sqle));
                        System.out.println("\n[OK]: An index covering these columns already exists - ACCEPTED!");
                    }
                    return false; // no success - but acceptable
                }
                // State: S0002 - Base table not found
                //  [Teradata: Macro 'xyz' does not exist] - when dropping objects
                else if (sqle.getSQLState().equalsIgnoreCase("S0002")) {
                    if (options.debug) {
                        System.out.println("Statement: " + sqlStatement);
                        System.out.println(Database.squeeze(sqle));
                        System.out.println("\n[OK]: Object does not exist - ACCEPTED!");
                    }
                    return false; // no success - but acceptable
                }
                // State: X0Y32 - <value> '<value>' already exists in <value> '<value>'.
                // State: X0Y68 - <value> '<value>' already exists.
                //  [Derby: Table/View 'XYZ' already exists in Schema 'ZYX'.]
                //  [Derby: PROCEDURE 'XYZ' already exists.]
                else if (sqle.getSQLState().startsWith("X0Y")) {
                    if (options.debug) {
                        System.out.println("Statement: " + sqlStatement);
                        System.out.println(Database.squeeze(sqle));
                        System.out.println("\n[OK]: Object already exists - ACCEPTED!");
                    }
                    return false; // no success - but acceptable
                }
            }
            //
            // This is something we do not accept
            //
            throw sqle;
        }
    }

    public void shutdown() {}
}

