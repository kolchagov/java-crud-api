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

import org.junit.BeforeClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class OracleTest extends Tests {

    static {
        //configure test parameters here
        USER = "";
        PASS = "";
        DB_NAME = "xe";
        SERVER_NAME = "localhost";
        SERVER_CLASS = ApiConfig.ORACLE;
    }

    @BeforeClass
    public static void setupTestClass() {
        setupClass();
    }

    @Override
    public String getEngineName() {
        return "Oracle";
    }

    @Override
    public boolean checkVersion(Connection link) throws SQLException {
        final String databaseProductString = link.getMetaData().getDatabaseProductVersion();
        final String releaseKeyword = "Release ";
        int releaseIdx = databaseProductString.lastIndexOf(releaseKeyword) + releaseKeyword.length();
        boolean isSupported = releaseIdx > 0;
        if (isSupported) {
            String[] version = databaseProductString.substring(releaseIdx).split("\\.");
            final Integer major = Integer.valueOf(version[0]);
            final Integer minor = Integer.valueOf(version[1]);
            isSupported = major == 11 ? minor >= 2 : major > 11; //minimum supported JDBC version is 11.2
        }
        return isSupported;
    }

    @Override
    public int getCapabilities(Connection link) {
        //Oracle Express does not have a Javavm in the database and the WKT conversion routines need this as the feature
        // is implemented as Java stored procedures. So these WKT routines are not supported on the Express edition.
        // https://stackoverflow.com/questions/44832223/oracle-converting-sdo-geometry-to-wkt
        int capabilities = 0;
        try {
            final String databaseProductVersion = link.getMetaData().getDatabaseProductVersion();
            if (databaseProductVersion.toLowerCase().contains("express edition")) {
                capabilities = JSON;
            } else {
                capabilities = JSON | GIS;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return capabilities;
    }

    @Override
    public void seedDatabase(Connection con, int capabilities) throws SQLException {
        try (InputStream stream = OracleTest.class.getClassLoader().getResourceAsStream("blog_oracle.sql")) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf8"))) {
                executeSQLScript(capabilities, con, reader);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void readSqlStatement(BufferedReader reader, StringBuilder sb) throws IOException {
        sb.setLength(0);
        //read until ; reached
        while (true) {
            String line = reader.readLine().trim();
            //skip comments
            if (line.startsWith("--")) { //NOI18N
                break;
            }
            if (line.startsWith("/")) break;
            if (!line.isEmpty())
                sb.append(line).append(' ');
        }
    }

    private void executeSQLScript(int capabilities, Connection conn, BufferedReader reader) throws IOException, SQLException {
        StringBuilder sb = new StringBuilder();
        dropAllDataObjects(conn);
        conn.setAutoCommit(false);
        Statement stmt = conn.createStatement();
        while (reader.ready()) {
            readSqlStatement(reader, sb);
            if (sb.length() > 0) {
                String line = sb.toString().trim();
                //System.out.println(line);
                if ((capabilities & GIS) == 0) {
                    line = line.replaceAll("(SDO_GEOMETRY)( NOT)? NULL", "varchar(255) NULL");
                    line = line.replaceAll("SDO_GEOMETRY\\('(.*)'\\)", "'$1'");
                }
                try {
                    stmt.addBatch(line);
                } catch (SQLException ex) {
                    System.out.println("error line: " + line);
                    throw ex;
                }
            }
        }
        executeBatch(conn, stmt);
        conn.setAutoCommit(true);
    }

    /**
     * Drops all DB objects for current schema (user)
     * @param conn DB connection
     */
    private void dropAllDataObjects(Connection conn) {
        try (CallableStatement stmt = conn.prepareCall("BEGIN FOR cur_rec IN (SELECT object_name, object_type FROM user_objects WHERE object_type IN ('TABLE', 'VIEW', 'PACKAGE', 'PROCEDURE', 'FUNCTION', 'SEQUENCE', 'SYNONYM', 'PACKAGE BODY' )) LOOP BEGIN IF cur_rec.object_type = 'TABLE' THEN EXECUTE IMMEDIATE    'DROP ' || cur_rec.object_type || ' \"' || cur_rec.object_name || '\" CASCADE CONSTRAINTS'; ELSE EXECUTE IMMEDIATE    'DROP ' || cur_rec.object_type || ' \"' || cur_rec.object_name || '\"'; END IF; EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.put_line (   'FAILED: DROP ' || cur_rec.object_type || ' \"' || cur_rec.object_name || '\"' ); END; END LOOP; END;")) {
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
