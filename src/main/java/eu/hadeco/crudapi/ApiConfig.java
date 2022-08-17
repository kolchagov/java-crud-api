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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;

/**
 * Extend this class to provide customization
 *
 * @author ivankol
 * @version $Id: $Id
 */
public class ApiConfig implements AutoCloseable {

    /** Constant <code>DERBY="org.apache.derby.jdbc.ClientDataSource"</code> */
    public static final String DERBY = "org.apache.derby.jdbc.ClientDataSource";
    /** Constant <code>JAYBIRD="org.firebirdsql.pool.FBSimpleDataSource"</code> */
    public static final String JAYBIRD = "org.firebirdsql.pool.FBSimpleDataSource";
    /** Constant <code>H2="org.h2.jdbcx.JdbcDataSource"</code> */
    public static final String H2 = "org.h2.jdbcx.JdbcDataSource";
    /** Constant <code>HSQLDB="org.hsqldb.jdbc.JDBCDataSource"</code> */
    public static final String HSQLDB = "org.hsqldb.jdbc.JDBCDataSource";
    /** Constant <code>IBM_JCC="com.ibm.db2.jcc.DB2SimpleDataSource"</code> */
    public static final String IBM_JCC = "com.ibm.db2.jcc.DB2SimpleDataSource";
    /** Constant <code>IBM_INFORMIX="com.informix.jdbcx.IfxDataSource"</code> */
    public static final String IBM_INFORMIX = "com.informix.jdbcx.IfxDataSource";
    /** Constant <code>MICROSOFT="com.microsoft.sqlserver.jdbc.SQLServerD"{trunked}</code> */
    public static final String MICROSOFT = "com.microsoft.sqlserver.jdbc.SQLServerDataSource";
    /** Constant <code>CONNECTOR_J="com.mysql.jdbc.jdbc2.optional.MysqlData"{trunked}</code> */
    public static final String CONNECTOR_J = "com.mysql.jdbc.jdbc2.optional.MysqlDataSource";
    /** Constant <code>MYSQL="com.mysql.jdbc.jdbc2.optional.MysqlData"{trunked}</code> */
    public static final String MYSQL = "com.mysql.cj.jdbc.MysqlDataSource";
    /** Constant <code>MARIADB="org.mariadb.jdbc.MariaDbDataSource"</code> */
    public static final String MARIADB = "org.mariadb.jdbc.MariaDbDataSource";
    /** Constant <code>ORACLE="oracle.jdbc.pool.OracleDataSource"</code> */
    public static final String ORACLE = "oracle.jdbc.pool.OracleDataSource";
    /** Constant <code>ORIENTDB="com.orientechnologies.orient.jdbc.Orien"{trunked}</code> */
    public static final String ORIENTDB = "com.orientechnologies.orient.jdbc.OrientDataSource";
    /** Constant <code>PGJDBC_NG="com.impossibl.postgres.jdbc.PGDataSourc"{trunked}</code> */
    public static final String PGJDBC_NG = "com.impossibl.postgres.jdbc.PGDataSource";
    /** Constant <code>POSTGRESQL="org.postgresql.ds.PGSimpleDataSource"</code> */
    public static final String POSTGRESQL = "org.postgresql.ds.PGSimpleDataSource";
    /** Constant <code>SAP="com.sap.dbtech.jdbc.DriverSapDB"</code> */
    public static final String SAP = "com.sap.dbtech.jdbc.DriverSapDB";
    /** Constant <code>XERIAL="org.sqlite.SQLiteDataSource"</code> */
    public static final String XERIAL = "org.sqlite.SQLiteDataSource";
    /** Constant <code>JCONNECT="com.sybase.jdbc4.jdbc.SybDataSource"</code> */
    public static final String JCONNECT = "com.sybase.jdbc4.jdbc.SybDataSource";

    private static final int CACHE_TO = 1 * 60 * 1000; //1min
    //contains table metadata map to gain some performance, refreshed every 30s
    private static Map<String, TableMeta> cachedTableMeta = null;
    private static long cacheTimestamp = 0;
    private final HikariDataSource dataSource;
    private final Properties properties;

    /**
     * Default constructor with Hikari properties
     *
     * @param hikariDatasourceProperties a {@link java.util.Properties} object.
     */
    public ApiConfig(Properties hikariDatasourceProperties) {
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
     * @param user a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     * @param databaseName a {@link java.lang.String} object.
     * @param portNumber a {@link java.lang.String} object.
     * @param serverHostName a {@link java.lang.String} object.
     * @param datasourceClassName a {@link java.lang.String} object.
     */
    public ApiConfig(String user, String password, String databaseName, String portNumber, String serverHostName, String datasourceClassName) {
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
     * @param user a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     * @param databaseName a {@link java.lang.String} object.
     * @param serverHostName a {@link java.lang.String} object.
     * @param datasourceClassName a {@link java.lang.String} object.
     */
    public ApiConfig(String user, String password, String databaseName, String serverHostName, String datasourceClassName) {
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
            //this is no longer supported in Mysql Connector Java 8, but utf8 is now default
//            properties.put("dataSource.useUnicode", "true");
            //This removes warning messages by explicitly set SSL to false.
            //if you need SSL, set this to true and provide trust store as required by MySQL
            properties.put("dataSource.useSSL", "false");
            properties.put("dataSource.characterEncoding", "utf8");
        }
        if (isPSQL()) {
            //allows proper handling of timestamps like "2013-12-11 10:09:08"
            properties.put("dataSource.stringType", "unspecified");
        }
        if (ORACLE.equals(datasourceClassName)) {
            properties.remove("dataSourceClassName");
            properties.setProperty("DriverClassName", "oracle.jdbc.OracleDriver");
            String jdbcUrl = String.format("jdbc:oracle:thin:@%s:%d:%s", serverHostName, 1521, databaseName);
            properties.setProperty("jdbcUrl", jdbcUrl);
        }
        final HikariConfig hikariConfig = new HikariConfig(properties);
//        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setMaximumPoolSize(10); //tweak connections according to your needs
//        hikariConfig.setMaxLifetime(5*60*1000);
//        hikariConfig.setMinimumIdle(5);
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
     *
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
     * Caches tableMeta map - this provides huge performance boost, as reading
     * this data is expensive
     *
     * @param cachedTableMeta a {@link java.util.Map} object.
     */
    public static void setCachedTableMeta(Map<String, TableMeta> cachedTableMeta) {
        ApiConfig.cachedTableMeta = cachedTableMeta;
        cacheTimestamp = System.currentTimeMillis();
    }

    /**
     * Resets the tableMeta cache.
     */
    public static void clearCachedTableMeta() {
        cachedTableMeta = null;
        cacheTimestamp = 0;
    }

    /**
     * <p>getConnection.</p>
     *
     * @return a {@link java.sql.Connection} object.
     * @throws java.sql.SQLException if any.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * <p>tableAuthorizer.</p>
     *
     * @param action   root actions: "list" (GET), "create" (POST); ID actions:
     *                 "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database a {@link java.lang.String} object.
     * @param table a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean tableAuthorizer(Actions action, String database, String table) {
        return true;
    }

    /**
     * <p>recordFilter.</p>
     *
     * @param action   root actions: "list" (GET), "create" (POST); ID actions:
     *                 "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database a {@link java.lang.String} object.
     * @param table a {@link java.lang.String} object.
     * @return additional filters to be added (Map column-[filters]) or null
     */
    public String[] recordFilter(Actions action, String database, String table) {
        return null;
    }

    /**
     * <p>columnAuthorizer.</p>
     *
     * @param action   root actions: "list" (GET), "create" (POST); ID actions:
     *                 "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database a {@link java.lang.String} object.
     * @param table a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean columnAuthorizer(Actions action, String database, String table, String column) {
        return true;
    }

    /**
     * <p>tenancyFunction.</p>
     *
     * @param action   root actions: "list" (GET), "create" (POST); ID actions:
     *                 "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database a {@link java.lang.String} object.
     * @param table a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
     */
    public Object tenancyFunction(Actions action, String database, String table, String column) {
        return null;
    }

    /**
     * Process the input value and returns sanitized
     *
     * @param action   root actions: "list" (GET), "create" (POST); ID actions:
     *                 "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database a {@link java.lang.String} object.
     * @param table a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     * @param type     SQL type as read from JDBC metadata
     * @param value a {@link java.lang.Object} object.
     * @param context a {@link javax.servlet.http.HttpServletRequest} object.
     * @return sanitized value
     */
    public Object inputSanitizer(Actions action, String database, String table, String column, String type, Object value, HttpServletRequest context) {
        return value;
    }

    /**
     * Validates the input. Returns true if validation is ok or String REASON
     * for failed validation
     *
     * @param action   root actions: "list" (GET), "create" (POST); ID actions:
     *                 "read" (GET), "update" (PUT), "delete" (DELETE), "increment" (PATCH)
     * @param database a {@link java.lang.String} object.
     * @param table a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     * @param type     SQL type as read from JDBC metadata
     * @param value a {@link java.lang.Object} object.
     * @param context a {@link javax.servlet.http.HttpServletRequest} object.
     * @return Boolean.true if value is valid or String to be reported to the
     * client
     */
    public Object inputValidator(Actions action, String database, String table, String column, String type, Object value, HttpServletRequest context) {
        return true;
    }

    /**
     * Can be used to manipulate the action or input map, right before DB
     * operation. (e.g. soft delete operations)
     *
     * @param action a {@link eu.hadeco.crudapi.RequestHandler.Actions} object.
     * @param database a {@link java.lang.String} object.
     * @param table a {@link java.lang.String} object.
     * @param ids an array of {@link java.lang.String} objects.
     * @param input a {@link java.util.Map} object.
     * @return a {@link eu.hadeco.crudapi.RequestHandler.Actions} object.
     */
    public Actions before(Actions action, String database, String table, String[] ids, Map<String, Object> input) {
        return action;
    }

    /**
     * <p>isMsSQL.</p>
     *
     * @return a boolean.
     */
    public final boolean isMsSQL() {
        return MICROSOFT.equals(properties.get("dataSourceClassName"));
    }

    /**
     * <p>isOracle.</p>
     *
     * @return a boolean.
     */
    public final boolean isOracle() {
        return ORACLE.equals(properties.get("dataSourceClassName")) || properties.getProperty("jdbcUrl", "").startsWith("jdbc:oracle");
    }

    /**
     * <p>isPSQL.</p>
     *
     * @return a boolean.
     */
    public final boolean isPSQL() {
        return POSTGRESQL.equals(properties.get("dataSourceClassName"));
    }

    /**
     * <p>isXERIAL.</p>
     *
     * @return a boolean.
     */
    public final boolean isXERIAL() {
        return XERIAL.equals(properties.get("dataSourceClassName"));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return properties.toString();
    }

    /** {@inheritDoc} */
    @Override
    public final void close() {
        dataSource.close();
    }
}
