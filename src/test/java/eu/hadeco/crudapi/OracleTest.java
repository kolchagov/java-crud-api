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
        USER = "crudtest";
        PASS = "crudtest";
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
        boolean isSupported = true;
/*        final String databaseProductVersion = link.getMetaData().getDatabaseProductVersion();
        System.out.println(databaseProductVersion);
        final String[] versionInfo = databaseProductVersion.split("\\.");
        if (versionInfo.length > 2) {
            int majorVersion = Integer.valueOf(versionInfo[0]);
            isSupported = majorVersion >= 5;
        }*/
        return isSupported;
    }

    @Override
    public int getCapabilities(Connection link) {
        int capabilities = 0;
//        capabilities = capabilities | GIS;
//        capabilities = capabilities | JSON;
        /*
        try {
            final String databaseProductVersion = link.getMetaData().getDatabaseProductVersion();
            final String[] versionInfo = databaseProductVersion.split("\\.");
            if (versionInfo.length > 2) {
                int minorVersion = Integer.valueOf(versionInfo[1]);
                if (minorVersion >= 6) {
                    capabilities = capabilities | GIS;
                }
                if (minorVersion >= 7) {
                    capabilities = capabilities | JSON;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }*/
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
            if(line.startsWith("/")) break;
            if(!line.isEmpty())
                sb.append(line).append(' ');
        }
    }

    private void executeSQLScript(int capabilities, Connection conn, BufferedReader reader) throws IOException, SQLException {
        StringBuilder sb = new StringBuilder();
        conn.setAutoCommit(false);
        Statement stmt = conn.createStatement();
        while (reader.ready()) {
            readSqlStatement(reader, sb);
            if (sb.length() > 0) {
                String line = sb.toString().trim();
                //System.out.println(line);
                /*
                if ((capabilities & JSON) == 0) {
                    line = line.replaceAll("JSON NOT NULL", "text NOT NULL");
                }
                if ((capabilities & GIS) == 0) {
                    line = line.replaceAll("(POINT|POLYGON)( NOT)? NULL", "text\u0002 NULL");
                    line = line.replaceAll("ST_GeomFromText", "concat");
                }
                */
                try {

                    if( isPLSQLBlock(line) ){
                        CallableStatement cstmt = conn.prepareCall(line);
                        cstmt.execute();
                    }else {
                        stmt.addBatch(line);
                    }

                } catch (SQLException ex) {
                    System.out.println("error line: " + line);
                    throw ex;
                }
            }
        }
        executeBatch(conn, stmt);
        conn.setAutoCommit(true);
    }

    private boolean isPLSQLBlock(String psql){
        psql = psql.toUpperCase();
        return psql.startsWith("DECLARE") || psql.startsWith("BEGIN");
    }

}
