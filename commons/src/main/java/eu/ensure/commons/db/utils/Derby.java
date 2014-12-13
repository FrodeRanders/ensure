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

import javax.sql.DataSource;
import java.util.Properties;

// For shutdown() purposes
import java.sql.DriverManager; 
import java.sql.SQLException;
import eu.ensure.commons.db.Database;

/**
 * Description of Derby:
 * <p>
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class Derby extends Manager {

    public Derby(DataSource dataSource, Options options) {
        super(dataSource, options, ";", /* ignore case? */ false, /* alone on line? */ false);

        // Always print database name to stdout.
        System.out.println("Target database: Derby");
    }

    public Derby(Properties properties, Options options, PrepareDataSource preparer) throws Exception {
        super(preparer, properties, options, ";", /* ignore case? */ false, /* alone on line? */ false);

        // Always print database name to stdout.
        System.out.println("Target database: Derby");
    }

    public void shutdown() {
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
}
