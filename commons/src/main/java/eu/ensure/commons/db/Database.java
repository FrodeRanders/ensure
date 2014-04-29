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
import java.util.Properties;
import javax.sql.DataSource;

import eu.ensure.commons.lang.Configurable;
import eu.ensure.commons.lang.ConfigurationTool;
import eu.ensure.commons.lang.DynamicLoader;
import org.apache.log4j.Logger;

/**
 * Description of Database:
 * <p/>
 * <p/>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class Database {
    private static final Logger log = Logger.getLogger(Database.class);

    private static int DEADLOCK_MAX_RETRIES = 100;
    private static int DEADLOCK_SLEEP_TIME = 200; // milliseconds

    //
    public interface Configuration {
        @Configurable(value = "derby")
        String manager();

        @Configurable(value = "org.apache.derby.jdbc.EmbeddedDataSource")
        String driver();

        @Configurable(value = "jdbc:derby:temporary-db;create=true")
        String url();

        @Configurable
        String user();

        @Configurable
        String password();

        @Configurable(value = "temporary-db")
        String database();
    }

    // 
    private static DynamicLoader<DataSource> loader = new DynamicLoader<DataSource>("datasource");
    
    //
    private Database() {
    }


    /**
     * Deduces configuration from properties, handling default values where appropriate...
     * @param properties
     * @return
     */
    public static Configuration getConfiguration(Properties properties) {
        Configuration config = ConfigurationTool.bindProperties(Configuration.class, properties);
        return config;
    }

    /**
     * Gets a datasource for the database.
     * <p>
     * Now, these are the naked facts regarding data sources:        
     *
     * There is no uniform way to configure a data source - it is
     * highly proprietary and depends on the JDBC driver.
     *
     * Depending on what data source you have configured, you will
     * have to use a construction along the lines of these examples
     * on the returned DataSource.
     * <pre>
     *     String appName = "MyApplication"; 
     *
     *     Properties properties = ...;
     *     DataSource dataSource = getDataSource(properties);
     *     Database.Configuration config = Database.getConfiguration(properties);
     *
     *     if (driver.equals("net.sourceforge.jtds.jdbcx.JtdsDataSource")) {
     *         net.sourceforge.jtds.jdbcx.JtdsDataSource ds = (net.sourceforge.jtds.jdbcx.JtdsDataSource)dataSource;
     *         ds.setAppName(appName); // std
     *         ds.setDatabaseName(config.database()); // std
     *         ds.setUser(config.user()); // std
     *         ds.setPassword(config.password()); // std
     *
     *         ds.setServerName(config.server());  // jtds specific
     *         ds.setPortNumber(Integer.parseInt(config.port())); // jtds specific
     *     }
     *     else if (driver.equals("org.apache.derby.jdbc.EmbeddedDataSource")) {
     *         org.apache.derby.jdbc.EmbeddedDataSource ds = (org.apache.derby.jdbc.EmbeddedDataSource)dataSource;
     *         ds.setDescription(appName); // std
     *         ds.setDatabaseName(config.database()); // std
     *         ds.setUser(config.user()); // std
     *         ds.setPassword(config.password()); // std
     * 
     *         ds.setCreateDatabase("create");  // derby specific
     *     }
     *     else if (driver.equals("sun.jdbc.odbc.ee.DataSource")) {
     *         sun.jdbc.odbc.ee.DataSource ds = (sun.jdbc.odbc.ee.DataSource)dataSource;
     *         ds.setDescription(appName); // std
     *         ds.setDatabaseName(config.database()); // std
     *         ds.setUser(config.user()); // std
     *         ds.setPassword(config.password()); // std
     * }
     * </pre>
     * <p>
     * @return
     * @throws Exception
     */
    public static DataSource getDataSource(Configuration config) throws DatabaseException {

        // Class implementing the DataSource
        String driver = config.driver();
        if (null == driver) {
            throw new DatabaseException("Could not determine JDBC driver name (driver)");
        }
        driver = driver.trim();

        // Now instantiate a DataSource
        try {
            return createDataSource(driver, loadDataSource(driver));
        } catch (ClassNotFoundException cnfe) {
            String info = "Could not instantiate DataSource: ";
            info += cnfe.getMessage();
            throw new DatabaseException(info, cnfe);
        }
    }

    /**
     * Dynamically loads the named class (fully qualified classname).
     */
    public static Class loadDataSource(String className) throws ClassNotFoundException {
        return loader.createClass(className);
    }

    /**
     * Creates a DataSource object instance from a DataSource class.
     * <p/>
     */
    public static DataSource createDataSource(String className, Class clazz) throws ClassNotFoundException {
        return loader.createObject(className, clazz);
    }
    
    /**
     * Support for explicit logging of SQL exceptions to error log.
     */
    public static String squeeze(SQLException sqle) {
        SQLException e = sqle;
        StringBuilder buf = new StringBuilder();
        while (e != null) {
            buf.append(sqle.getClass().getSimpleName() + " [");
            buf.append(e.getMessage());
            buf.append("], SQLstate(");
            buf.append(e.getSQLState());
            buf.append("), Vendor code(");
            buf.append(e.getErrorCode());
            buf.append(")\n");
            e = e.getNextException();
        }
        return buf.toString();
    }

    /**
     * Support for explicit logging of SQL warnings to warning log.
     */
    public static String squeeze(SQLWarning sqlw) {
        SQLWarning w = sqlw;
        StringBuilder buf = new StringBuilder();
        while (w != null) {
            buf.append(sqlw.getClass().getSimpleName() + " [");
            buf.append(w.getMessage());
            buf.append("], SQLstate(");
            buf.append(w.getSQLState());
            buf.append("), Vendor code(");
            buf.append(w.getErrorCode());
            buf.append(")\n");
            w = w.getNextWarning();
        }
        return buf.toString();
    }

    //
    private interface ExecutableCall {
        boolean execute() throws SQLException;
    }

    private interface QueryCall {
        ResultSet query() throws SQLException;
    }

    private interface UpdateCall {
        int update() throws SQLException;
    }



    /**
     * Wraps an execute in deadlock detection
     */
    private static boolean executeWithDD(ExecutableCall call) throws SQLException {
        SQLException sqle = null;
        int i = DEADLOCK_MAX_RETRIES;
        do {
            try {
                return call.execute();

            } catch (SQLException se) {
                sqle = se;
                // Is SQLException a deadlock? (40001)
                if (se.getSQLState() != null && se.getSQLState().startsWith("40")) {
                    log.info("Database deadlock has occurred during executeWithDD, trying again");
                    try {
                        Thread.sleep(DEADLOCK_SLEEP_TIME);
                    } catch (Exception ignore) {
                    }
                } else /* other SQLException */ {
                    throw se;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Execute, retries=" + i);
            }
        } while (--i > 0);
        log.error("Giving up deadlock retry");
        throw sqle;
    }

    /**
     * Wraps a query in deadlock detection
     */
    private static ResultSet queryWithDD(QueryCall call) throws SQLException {
        SQLException sqle = null;
        int i = DEADLOCK_MAX_RETRIES;
        do {
            try {
                return call.query();

            } catch (SQLException se) {
                sqle = se;
                // Is SQLException a deadlock? (40001)
                if (se.getSQLState() != null && se.getSQLState().startsWith("40")) {
                    log.info("Database deadlock has occurred during executeQuery, trying again");
                    try {
                        Thread.sleep(DEADLOCK_SLEEP_TIME);
                    } catch (Exception ignore) {
                    }
                } else /* other SQLException */ {
                    throw se;
                }
            }
        } while (--i > 0);
        log.error("Giving up deadlock retry");
        throw sqle;
    }

    /**
     * Wraps an update in deadlock detection
     */
    private static int updateWithDD(UpdateCall call) throws SQLException {
        SQLException sqle = null;
        int i = DEADLOCK_MAX_RETRIES;
        do {
            try {
                return call.update();

            } catch (SQLException se) {
                sqle = se;
                // Is SQLException a deadlock? (40001)
                if (se.getSQLState() != null && se.getSQLState().startsWith("40")) {
                    log.info("Database deadlock has occurred during executeUpdate, trying again");
                    try {
                        Thread.sleep(DEADLOCK_SLEEP_TIME);
                    } catch (Exception ignore) {
                    }
                } else /* other SQLException */ {
                    throw se;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Update, retries=" + i);
            }
        } while (--i > 0);
        log.error("Giving up deadlock retry");
        throw sqle;

    }
    /**
     * Manages call to Statement.executeBatch(), providing support for deadlock
     * detection and statement reruns.
     */
    public static void executeBatch(final Statement stmt) throws SQLException {
        executeWithDD(new ExecutableCall() {
            @Override
            public boolean execute() throws SQLException {
                int[] results = stmt.executeBatch();

                // Handle warning, if applicable
                SQLWarning warning = stmt.getWarnings();
                if (null != warning) {
                    log.info(squeeze(warning));
                }

                return true; // any value will do
            }
        });
    }

    /**
     * Manages call to Statement.executeWithDD(), providing support for deadlock
     * detection and statement reruns.
     */
    public static boolean execute(final Statement stmt, final String sql) throws SQLException {
        return executeWithDD(new ExecutableCall() {
            @Override
            public boolean execute() throws SQLException {
                boolean results = stmt.execute(sql);

                // Handle warning, if applicable
                SQLWarning warning = stmt.getWarnings();
                if (null != warning) {
                    log.info(squeeze(warning));
                }

                return results;
            }
        });
    }

    /**
     * Manages call to PreparedStatement.executeWithDD(), providing support for deadlock
     * detection and statement reruns.
     */
    public static boolean execute(final PreparedStatement pStmt) throws SQLException {
        return executeWithDD(new ExecutableCall() {
            @Override
            public boolean execute() throws SQLException {
                boolean result = pStmt.execute();

                // Handle warning, if applicable
                SQLWarning warning = pStmt.getWarnings();
                if (null != warning) {
                    log.info(squeeze(warning));
                }

                return result;
            }
        });
    }

    /**
     * Manages call to CallableStatement.executeWithDD(), providing support for deadlock
     * detection and statement reruns.
     */
    public static boolean execute(final CallableStatement cStmt) throws SQLException {
        return executeWithDD(new ExecutableCall() {
            @Override
            public boolean execute() throws SQLException {
                boolean result = cStmt.execute();

                // Handle warning, if applicable
                SQLWarning warning = cStmt.getWarnings();
                if (null != warning) {
                    log.info(squeeze(warning));
                }

                return result;
            }
        });
    }

    /**
     * Manages call to PreparedStatement.executeQuery(), providing support for deadlock
     * detection and statement reruns.
     */
    public static ResultSet executeQuery(final PreparedStatement pStmt) throws SQLException {
        return queryWithDD(new QueryCall() {
            @Override
            public ResultSet query() throws SQLException {
                ResultSet rs = pStmt.executeQuery();

                // Handle warning, if applicable
                SQLWarning stmtWarning = pStmt.getWarnings();
                if (null != stmtWarning) {
                    log.info(squeeze(stmtWarning));
                }

                SQLWarning rsWarning = rs.getWarnings();
                if (null != rsWarning) {
                    log.info(squeeze(rsWarning));
                }

                return rs;
            }
        });
    }

    /**
     * Manages call to Statement.executeQuery(), providing support for deadlock
     * detection and statement reruns.
     */
    public static ResultSet executeQuery(final Statement stmt, final String sql) throws SQLException {
        return queryWithDD(new QueryCall() {
            @Override
            public ResultSet query() throws SQLException {
                ResultSet rs = stmt.executeQuery(sql);

                // Handle warning, if applicable
                SQLWarning stmtWarning = stmt.getWarnings();
                if (null != stmtWarning) {
                    log.warn(squeeze(stmtWarning));
                }

                SQLWarning rsWarning = rs.getWarnings();
                if (null != rsWarning) {
                    log.warn(squeeze(rsWarning));
                }

                return rs;
            }
        });
    }

    /**
     * Manages call to Statement.executeUpdate(), providing support for deadlock
     * detection and statement reruns.
     */
    public static int executeUpdate(final Statement stmt, final String sql) throws SQLException {
        return updateWithDD(new UpdateCall() {
            @Override
            public int update() throws SQLException {
                int rows = stmt.executeUpdate(sql);

                // Handle warning, if applicable
                SQLWarning warning = stmt.getWarnings();
                if (null != warning) {
                    log.info(squeeze(warning));
                }

                return rows;
            }
        });
    }

    public static int executeUpdate(final PreparedStatement pStmt) throws SQLException {
        return updateWithDD(new UpdateCall() {
            @Override
            public int update() throws SQLException {
                int rows = pStmt.executeUpdate();

                // Handle warning, if applicable
                SQLWarning warning = pStmt.getWarnings();
                if (null != warning) {
                    log.info(squeeze(warning));
                }

                return rows;
            }
        });
    }
}
