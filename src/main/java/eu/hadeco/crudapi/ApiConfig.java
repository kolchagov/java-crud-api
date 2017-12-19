/*
 *  Copyright (c) 2017. I.Kolchagov, All rights reserved.
 *  Contact: I.Kolchagov (kolchagov (at) gmail.com)
 *
 *  The contents of this file is licensed under the terms of LGPLv3 license.
 *  You may read the the included file 'lgpl-3.0.txt'
 *  or https://www.gnu.org/licenses/lgpl-3.0.txt
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  The project uses 'fluentsql' internally, licensed under Apache Public License v2.0.
 *  https://github.com/ivanceras/fluentsql/blob/master/LICENSE.txt
 *
 */

package eu.hadeco.crudapi;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.hadeco.crudapi.RequestHandler.Actions;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * Extend this class to provide customization
 */
public class ApiConfig {
    public static final String DERBY = "org.apache.derby.jdbc.ClientDataSource";
    public static final String JAYBIRD = "org.firebirdsql.pool.FBSimpleDataSource";
    public static final String H2 = "org.h2.jdbcx.JdbcDataSource";
    public static final String HSQLDB = "org.hsqldb.jdbc.JDBCDataSource";
    public static final String IBM_JCC = "com.ibm.db2.jcc.DB2SimpleDataSource";
    public static final String IBM_INFORMIX = "com.informix.jdbcx.IfxDataSource";
    public static final String MICROSOFT = "com.microsoft.sqlserver.jdbc.SQLServerDataSource";
    public static final String CONNECTOR_J = "com.mysql.jdbc.jdbc2.optional.MysqlDataSource";
    public static final String MYSQL = "com.mysql.jdbc.jdbc2.optional.MysqlDataSource";
    public static final String MARIADB = "org.mariadb.jdbc.MariaDbDataSource";
    public static final String ORACLE = "oracle.jdbc.pool.OracleDataSource";
    public static final String ORIENTDB = "com.orientechnologies.orient.jdbc.OrientDataSource";
    public static final String PGJDBC_NG = "com.impossibl.postgres.jdbc.PGDataSource";
    public static final String POSTGRESQL = "org.postgresql.ds.PGSimpleDataSource";
    public static final String SAP = "com.sap.dbtech.jdbc.DriverSapDB";
    public static final String XERIAL = "org.sqlite.SQLiteDataSource";
    public static final String JCONNECT = "com.sybase.jdbc4.jdbc.SybDataSource";
    private static final int CACHE_TO = 1*60 * 1000; //1min
    //contains table metadata map to gain some performance, refreshed every 30s
    private static Map<String, TableMeta> cachedTableMeta = null;
    private static long cacheTimestamp = 0;
    private final HikariDataSource dataSource;
    private final Properties properties;

    /**
     * Default constructor with Hikari properties
     *
     * @param hikariDatasourceProperties
     */
    ApiConfig(Properties hikariDatasourceProperties) {
        properties = hikariDatasourceProperties;
        dataSource = new HikariDataSource(new HikariConfig(properties));
    }

    ApiConfig(HikariDataSource dataSource) {
        properties = null;
        this.dataSource = dataSource;
    }

    /**
     * Verbose configuration
     *
     * @param user
     * @param password
     * @param databaseName
     * @param portNumber
     * @param serverHostName
     * @param datasourceClassName
     */
    ApiConfig(String user, String password, String databaseName, String portNumber, String serverHostName, String datasourceClassName) {
        properties = new Properties();
        properties.put("dataSource.user", user);
        properties.put("dataSource.password", password);
        properties.put("dataSource.databaseName", databaseName);
        properties.put("dataSource.serverName", serverHostName);
        properties.put("dataSource.portNumber", portNumber);
        properties.put("dataSourceClassName", datasourceClassName);
        properties.put("dataSource.useUnicode", "true");
        properties.put("dataSource.characterEncoding", "utf8");
        dataSource = new HikariDataSource(new HikariConfig(properties));
    }

    /**
     * Verbose configuration, with default port
     *
     * @param user
     * @param password
     * @param databaseName
     * @param serverHostName
     * @param datasourceClassName
     */
    ApiConfig(String user, String password, String databaseName, String serverHostName, String datasourceClassName) {
        properties = new Properties();
        if (XERIAL.equals(datasourceClassName)) {
            //dataSource props doesn't work with sqlite... why?
            properties.put("jdbcUrl", "jdbc:sqlite:" + databaseName);
        } else {
            properties.put("dataSource.user", user);
            properties.put("dataSource.password", password);
            properties.put("dataSource.serverName", serverHostName);
            properties.put("dataSourceClassName", datasourceClassName);
        }
        properties.put("dataSource.databaseName", databaseName);
        if (MYSQL.equals(datasourceClassName)) {
            properties.put("dataSource.useUnicode", "true");
            properties.put("dataSource.characterEncoding", "utf8");
        }
        if(isPSQL()) {
            //allows proper handling of timestamps like "2013-12-11 10:09:08"
            properties.put("dataSource.stringType", "unspecified");
        }
        final HikariConfig hikariConfig = new HikariConfig(properties);
//        hikariConfig.setConnectionTestQuery("SELECT 1");
        //        hikariConfig.setMaximumPoolSize(1); //debug
        dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * Default constructor for MySQL server
     *
     * @param user
     * @param password
     * @param databaseName
     * @param serverHostName
     */
    ApiConfig(String user, String password, String databaseName, String serverHostName) {
        this(user, password, databaseName, serverHostName, MYSQL);
    }

    /**
     * Returns cached tableMeta map but clears the references
     * @return null if cache is expired or cleared
     */
    public static Map<String, TableMeta> getCachedTableMeta() {
        if (System.currentTimeMillis() > cacheTimestamp + CACHE_TO) {
            cachedTableMeta = null;
        } else {
            for (TableMeta tableMeta : cachedTableMeta.values()) {
                tableMeta.clearReferencedTables();
            }
        }
        return cachedTableMeta;
    }

    /**
     * Caches tableMeta map - this provides huge performance boost, as reading this data is expensive
     * @param cachedTableMeta
     */
    public static void setCachedTableMeta(Map<String, TableMeta> cachedTableMeta) {
        ApiConfig.cachedTableMeta = cachedTableMeta;
        cacheTimestamp = System.currentTimeMillis();
    }

    /**
     * Resets the tableMeta cache.
     */
    public static void clearCachedTableMeta() {
        cachedTableMeta =null;
        cacheTimestamp = 0;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * @param action   root actions: "list" (GET), "create" (POST);
     *                 ID actions: "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database
     * @param table
     * @return
     */
    boolean tableAuthorizer(Actions action, String database, String table) {
        return true;
    }

    /**
     * @param action   root actions: "list" (GET), "create" (POST);
     *                 ID actions: "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database
     * @param table
     * @return additional filters to be added (Map column->[filters]) or null
     */
    String[] recordFilter(Actions action, String database, String table) {
        return null;
    }

    /**
     * @param action   root actions: "list" (GET), "create" (POST);
     *                 ID actions: "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database
     * @param table
     * @param column
     * @return
     */
    boolean columnAuthorizer(Actions action, String database, String table, String column) {
        return true;
    }

    /**
     * @param action   root actions: "list" (GET), "create" (POST);
     *                 ID actions: "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database
     * @param table
     * @param column
     * @return
     */
    Object tenancyFunction(Actions action, String database, String table, String column) {
        return null;
    }

    /**
     * Process the input value and returns sanitized
     *
     * @param action   root actions: "list" (GET), "create" (POST);
     *                 ID actions: "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database
     * @param table
     * @param column
     * @param type     SQL type as read from JDBC metadata
     * @param value
     * @param context
     * @return sanitized value
     */
    Object inputSanitizer(Actions action, String database, String table, String column, String type, Object value, HttpServletRequest context) {
        return value;
    }

    /**
     * Validates the input. Returns true if validation is ok or String REASON for failed validation
     *
     * @param action   root actions: "list" (GET), "create" (POST);
     *                 ID actions: "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database
     * @param table
     * @param column
     * @param type     SQL type as read from JDBC metadata
     * @param value
     * @param context
     * @return Boolean.true if value is valid or String to be reported to the client
     */
    Object inputValidator(Actions action, String database, String table, String column, String type, Object value, HttpServletRequest context) {
        return true;
    }


    /**
     * Can be used to manipulate the action or input map, right before DB operation.
     * (e.g. soft delete operations)
     *
     * @param action
     * @param database
     * @param table
     * @param ids
     * @param input
     */
    Actions before(Actions action, String database, String table, String[] ids, Map<String, Object> input) {
        return action;
    }

    public boolean isMsSQL() {
        return MICROSOFT.equals(properties.get("dataSourceClassName"));
    }

    public boolean isPSQL() {
        return POSTGRESQL.equals(properties.get("dataSourceClassName"));
    }
}
