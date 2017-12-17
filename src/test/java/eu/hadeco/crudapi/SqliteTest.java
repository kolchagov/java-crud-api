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

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static eu.hadeco.crudapi.ApiConfig.XERIAL;

public class SqliteTest extends Tests {

    static {
        //configure test parameters here
        USER = "root";
        PASS = "root";
        DB_NAME = "crudtest.db";
        SERVER_NAME = "localhost";
        SERVER_CLASS = XERIAL;
    }

    @BeforeClass
    public static void setupTestClass() {
        setupClass();
    }

    @Override
    public String getEngineName() {
        return "SQLite";
    }

    @Override
    public boolean checkVersion(Connection link) throws SQLException {
        boolean isSupported = false;
            final String databaseProductVersion = link.getMetaData().getDatabaseProductVersion();
            System.out.println(databaseProductVersion);
            final String[] versionInfo = databaseProductVersion.split("\\.");
            if (versionInfo.length > 1) {
                int majorVersion = Integer.valueOf(versionInfo[0]);
                int minorVersion = Integer.valueOf(versionInfo[1]);
                isSupported = majorVersion >= 3 && minorVersion >= 0;
            }
        return isSupported;
    }

    @Override
    public int getCapabilities(Connection link) {
        return JSON;
    }

    @Override
    public void seedDatabase(Connection con, int capabilities) throws SQLException {
        try (InputStream stream = SqliteTest.class.getClassLoader().getResourceAsStream("blog_sqlite.sql")) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf8"))) {
                new File(DB_NAME).createNewFile(); //create db file if not exists
                executeSQLScript(con, reader);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeSQLScript(Connection conn, BufferedReader reader) throws IOException, SQLException {
        StringBuilder sb = new StringBuilder();
        Statement stmt = conn.createStatement();
//        final String catalog = conn.getCatalog();
//        final String schema = conn.getSchema();
        while (reader.ready()) {
            readSqlStatement(reader, sb);
            if (sb.length() > 0) {
                String line = sb.toString();
                line = line.replace("geometry", "TEXT");
                try {
//                    System.out.println(line);  //debug
                    stmt.execute(line);
                } catch (SQLException ex) {
                    System.out.println("error line: " + line);
                    throw ex;
                }
            }
        }
    }

}
