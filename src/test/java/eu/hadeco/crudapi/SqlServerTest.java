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
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

public class SqlServerTest extends Tests {
    static {
        //configure test parameters here
        USER = "";
        PASS = "";
        DB_NAME = "";
        SERVER_NAME = "";
        SERVER_CLASS = ApiConfig.MICROSOFT;
    }

    @BeforeClass
    public static void setupTestClass() {
        setupClass();
    }


    @Override
    public String getEngineName() {
        return "SQLServer";
    }

    @Override
    public boolean checkVersion(Connection link) throws SQLException {
        boolean isSupported = false;
        final String databaseProductVersion = link.getMetaData().getDatabaseProductVersion();
        System.out.println(databaseProductVersion);
        final String[] versionInfo = databaseProductVersion.split("\\.");
        if (versionInfo.length > 2) {
            int majorVersion = Integer.valueOf(versionInfo[0]);
            int minorVersion = Integer.valueOf(versionInfo[1]);
//            int buildVersion = Integer.valueOf(versionInfo[2]);
            isSupported = majorVersion >= 11 && minorVersion >= 0 ;
        }
        return isSupported;
    }

    @Override
    public int getCapabilities(Connection link) {
//        return 0;
        return GIS | JSON ;
    }

    @Override
    public void seedDatabase(Connection con, int capabilities) {
        try (InputStream stream = SqlServerTest.class.getClassLoader().getResourceAsStream("blog_sqlserver.sql")) {
            final EncodedResource res = new EncodedResource(new InputStreamResource(stream), "utf8");
            ScriptUtils.executeSqlScript(con, res, true, true,
                    "--", "GO", "/*", "*/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
