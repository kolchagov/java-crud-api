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

import org.junit.AfterClass;
import org.junit.Assume;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Logger;

import static eu.hadeco.crudapi.CrudApiHandler.TAG_FILTER;

public abstract class TestBase {

    //configure test DB parameters in the corresponding test classes!!!
    protected static String USER = null;
    protected static String PASS = null;
    protected static String DB_NAME = null;
    protected static String SERVER_NAME = null;
    protected static String SERVER_CLASS = null;

    public static final int GIS = 1;
    public static final int JSON = 2;
    public static final Logger LOG = Logger.getLogger(TestBase.class.getName());

    protected static ApiConfig apiConfig;

    public static ApiConfig getApiConfig() {
        return apiConfig;
    }

    @AfterClass
    public static void tearDown() {
        ApiConfig.clearCachedTableMeta();
        apiConfig = null; //prepare for next Test from the suite
    }

    /**
     * Initialize ApiConfig here
     */
    public static void setupClass() {
        if (apiConfig == null) {
            if(hasEmptyConfigurationParameters(USER, PASS, DB_NAME, SERVER_NAME, SERVER_CLASS)) {
                LOG.severe("Server class is not configured!");
                Assume.assumeTrue(false);
            }
            apiConfig = new ApiConfig(USER, PASS, DB_NAME, SERVER_NAME, SERVER_CLASS) {
                @Override
                protected boolean columnAuthorizer(RequestHandler.Actions action, String database, String table, String column) {
                    return !("password".equals(column) && RequestHandler.Actions.LIST.equals(action));
                }

                @Override
                protected String[] recordFilter(RequestHandler.Actions action, String database, String table) {
                    return "posts".equals(table) ? new String[]{"id,neq,13"} : null;
                }

                @Override
                protected Object tenancyFunction(RequestHandler.Actions action, String database, String table, String column) {
                    return "users".equals(table) && "id".equals(column) ? 1 : null;
                }

                @Override
                protected Object inputSanitizer(RequestHandler.Actions action, String database, String table, String column, String type, Object value, HttpServletRequest context) {
                    return value instanceof String ? TAG_FILTER.matcher(((String) value)).replaceAll("") : value;
                }

                @Override
                protected Object inputValidator(RequestHandler.Actions action, String database, String table, String column, String type, Object value, HttpServletRequest context) {
//                    ($column=='category_id' && !is_numeric($value))?'must be numeric':true;
                    return "category_id".equals(column) && !(value instanceof Long) ? "must be numeric" : true;
                }

                @Override
                protected RequestHandler.Actions before(RequestHandler.Actions action, String database, String table, String[] ids, Map<String, Object> input) {
                    if ("products".equals(table)) {
                        if (action == RequestHandler.Actions.CREATE) {
                            input.put("created_at", "2013-12-11 10:09:08");
                        } else if (action == RequestHandler.Actions.DELETE) {
                            action = RequestHandler.Actions.UPDATE;
                            input.put("deleted_at", "2013-12-11 11:10:09");
                        }
                    }
                    return action;
                }
            };
        }
    }

    private static boolean hasEmptyConfigurationParameters(String... parameters) {
        for (String parameter : parameters) {
            if(parameter == null || parameter.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public abstract String getEngineName();

    public Connection connect() throws SQLException {
        return apiConfig.getConnection();
    }

    public abstract boolean checkVersion(Connection link) throws SQLException;

    public abstract int getCapabilities(Connection link);

    public abstract void seedDatabase(Connection connection, int capabilities) throws SQLException;

    protected void readSqlStatement(BufferedReader reader, StringBuilder sb) throws IOException {
        sb.setLength(0);
        //read until ; reached
        while (true) {
            String line = reader.readLine().trim();
            //skip comments
            if(line.equals("GO")) break;
            if (line.startsWith("--")) { //NOI18N
                break;
            }
            if(!line.isEmpty())
            sb.append(line).append(' ');
            if (line.endsWith(";")) { //NOI18N
                break;
            }
        }
    }

    protected void executeBatch(Connection conn, Statement stmt) throws SQLException {
        try {
            stmt.executeBatch();
            conn.commit();
        } catch (SQLException ex) {
            LOG.severe(ex.getMessage());
            throw ex;
        }
    }
}