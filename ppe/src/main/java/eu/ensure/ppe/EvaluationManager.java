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


import eu.ensure.vopn.db.Database;
import eu.ensure.vopn.db.DatabaseException;
import eu.ensure.vopn.db.utils.Derby;
import eu.ensure.vopn.db.utils.Manager;
import eu.ensure.vopn.db.utils.Options;
import eu.ensure.vopn.lang.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the local database.
 * <p>
 * Created by Frode Randers at 2012-12-03 10:47
 */
public class EvaluationManager {
    private static final Logger log = LogManager.getLogger(EvaluationManager.class);

    public static final String JNDI_ENVIRONMENT = "java:comp/env";

    // Set to true at least once after having made extensive changes to SQL files
    public static final boolean DEBUG_STMTS = false;

    private static DataSource dataSource = null;
    private static boolean databaseWasInitialized = false;
    private static final Object lock = new Object();

    public interface Configuration extends Database.Configuration {
        //@Configurable(property = "internal-database-path", value = "./quality-engine-db")
        @Configurable(property = "internal-database-path", value = "/Users/froran/Projects/ltu/ensure-internal/Code/A4-PreservationRuntime/A42-QualityTool/trunk/quality-engine-db")
        String database();

        @Configurable(property = "openejb/Resource/ppe-db-unmanaged")
        DataSource datasource();
    }

    public Configuration configuration;

    private EvaluationManager() {
        Collection<ConfigurationTool.ConfigurationResolver> resolvers =
                new ArrayList<ConfigurationTool.ConfigurationResolver>();

        // <<<"Check in the JNDI tree">>>-resolver
        resolvers.add(new JndiConfigurationResolver());

        // <<<"Check among system properties">>>-resolver
        resolvers.add(new SystemPropertiesConfigurationResolver());

        Map<String, Object> defaults = new HashMap<String, Object>();
        configuration = ConfigurationTool.bind(Configuration.class, defaults, resolvers);
    }

    public static EvaluationManager getInstance() {
        return new EvaluationManager();
    }

    /**
     * This method is used to retrieve a DataSource when communicating with the local database.
     *
     * @return
     * @throws DatabaseException
     */
    public DataSource getDataSource() throws DatabaseException {
        synchronized (lock) {
            if (null != dataSource)
                return dataSource;

            DataSource _dataSource = configuration.datasource();
            if (null == _dataSource) {
                if (log.isInfoEnabled()) {
                    log.info("Establishing datasource from configuration");
                }

                _dataSource = Database.getDataSource(configuration);
                String appName = "PPE";

                if (configuration.driver().equals("org.apache.derby.jdbc.EmbeddedDataSource")) {
                    org.apache.derby.jdbc.EmbeddedDataSource ds = (org.apache.derby.jdbc.EmbeddedDataSource) _dataSource;
                    ds.setDescription(appName); // std
                    ds.setDatabaseName(configuration.database()); // std
                    ds.setUser(configuration.user()); // std
                    ds.setPassword(configuration.password()); // std

                    ds.setCreateDatabase("create");  // derby specific
                } else {
                    String info = "Unsupported database driver requested for local database: ";
                    info += "Currently depends on Apache Derby for maintaining plugin opinions.";
                    throw new DatabaseException(info);
                }
            }
            dataSource = _dataSource;
            return dataSource;
        }
    }

    /**
     * This method sets up a database manager, that is used to initiate the database.
     * It knows how to read DDL from text files and parse them...
     *
     * @return
     * @throws Exception
     */
    public Manager getDatabaseManager() throws Exception {

        Options options = new Options();
        options.debug = DEBUG_STMTS;

        Manager dbms = new Derby(getDataSource(), options);
        return dbms;
    }

    public void setup() throws EvaluationException {
        InputStream is = null;
        try {
            synchronized (lock) {
                if (!databaseWasInitialized) {

                    Manager dbms = getDatabaseManager();

                    //
                    is = getClass().getResourceAsStream("initiate.sql");
                    dbms.execute("initiate.sql", new InputStreamReader(is));
                    is.close();

                    //
                    is = getClass().getResourceAsStream("populate.sql");
                    dbms.execute("populate.sql", new InputStreamReader(is));
                    is.close();

                    //
                    is = getClass().getResourceAsStream("opinions_amazon_s3.sql");
                    dbms.execute("opinions_amazon_s3.sql", new InputStreamReader(is));
                    is.close();

                    //
                    is = getClass().getResourceAsStream("opinions_amazon_ec2.sql");
                    dbms.execute("opinions_amazon_ec2.sql", new InputStreamReader(is));
                    is.close();

                    //
                    is = getClass().getResourceAsStream("opinions_amazon_glacier.sql");
                    dbms.execute("opinions_amazon_glacier.sql", new InputStreamReader(is));
                    is.close();

                    //
                    is = getClass().getResourceAsStream("opinions_rackspace_cloudfiles.sql");
                    dbms.execute("opinions_rackspace_cloudfiles.sql", new InputStreamReader(is));
                    is.close();

                    //
                    is = getClass().getResourceAsStream("opinions_rackspace_cloudservers.sql");
                    dbms.execute("opinions_rackspace_cloudservers.sql", new InputStreamReader(is));
                    is.close();

                    //
                    is = getClass().getResourceAsStream("opinions_openstack_objectstorage.sql");
                    dbms.execute("opinions_openstack_objectstorage.sql", new InputStreamReader(is));
                    is.close();

                    //
                    is = getClass().getResourceAsStream("opinions_openstack_compute.sql");
                    dbms.execute("opinions_openstack_compute.sql", new InputStreamReader(is));
                    is.close();

                    /*
                     * This is internal to the ENSURE solution and does not produce any delta
                     * between different evaluations
                     *
                    is = getClass().getResourceAsStream("opinions_base_ingest_service.sql");
                    dbms.execute("opinions_base_ingest_service.sql", new InputStreamReader(is));
                    is.close();
                    */

                    /*
                     * This is internal to the ENSURE solution and does not produce any delta
                     * between different evaluations
                     *
                    is = getClass().getResourceAsStream("opinions_base_access_service.sql");
                    dbms.execute("opinions_base_access_service.sql", new InputStreamReader(is));
                    is.close();
                    */

                    databaseWasInitialized = true;
                }
            }
        } catch (Exception e) {
            String info = "Failed to setup datamodel for PPE: ";
            Throwable baseCause = Stacktrace.getBaseCause(e);
            info += baseCause.getMessage();

            throw new EvaluationException(info, baseCause);

        } finally {
            // Force Derby shutdown, since we will load database later using JPA
            //LocalDatabase.shutdown();

            try {
                if (null != is) is.close();
            } catch (IOException ignore) {
            }
        }
    }

    public void shutdown() throws EvaluationException {
        // Does nothing
    }


    public Calculator getCalculator() throws DatabaseException {
        Calculator calculator = new Calculator(getDataSource());
        return calculator;
    }

    public ConfigurationReader getReader() {
        ConfigurationReader reader = ConfigurationReader.getInstance();
        return reader;
    }
}
