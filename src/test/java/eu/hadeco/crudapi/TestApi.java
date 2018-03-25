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


import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.javacrumbs.jsonunit.JsonAssert;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestApi {
    private static final JsonParser parser = new JsonParser();
    private final TestBase test;
    private String baseUrl;
    private String method;
    private String data;

    TestApi(TestBase test) {
        this.test = test;
    }


    private void setBaseUrl(String relativeUrlWithParams) {
        baseUrl = String.format("http://localhost:8080%s", relativeUrlWithParams);
    }

    public void expect(String expected) {
        expect(true, expected);
    }

    public void expect(boolean isOkResponse, String expected) {
        try {
            final MockHttpServletRequest req = getMockHttpServletRequest();
            final MockHttpServletResponse resp = new MockHttpServletResponse();
            //todo check why some tests are failing without it
            try (Connection link = test.connect()) {
                final ApiConfig apiConfig = TestBase.getApiConfig();
                RequestHandler.handle(req, resp, apiConfig);
                String actual = resp.getContentAsString();
                assertEquals("expected ok response, got: " + actual, isOkResponse, resp.getStatus() < 400);
                if (expected != null) {
                    if (isOkResponse) {
                        JsonAssert.assertJsonEquals(expected.toLowerCase(), actual.toLowerCase() );
                    } else {
                        assertEquals( expected.toLowerCase(), actual.toLowerCase() );
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            this.data = null;
        }
    }

    public void expectAny() {
        expect(true, null);
    }

    private MockHttpServletRequest getMockHttpServletRequest() throws UnsupportedEncodingException {
        final MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServerName("localhost");
        req.setMethod(method);
        final UriComponents build = UriComponentsBuilder.fromUriString(this.baseUrl).build();
        req.setPathInfo(build.getPath());
        req.setQueryString(build.getQuery());
        setParamsFromBuild(req, build);
        if (data != null) {
            try {
                if (data.endsWith("__is_null")) throw new JsonParseException("");
                //invalid json test expects json content
                if (!"{\"}".equals(data)) {
                    parser.parse(data);
                }
                req.setContentType("application/json");
            } catch (JsonParseException ignored) {
                req.setContentType("application/x-www-form-urlencoded");
                final String url = "/?" + URLDecoder.decode(data, "utf8");
                setParamsFromBuild(req, UriComponentsBuilder.fromUriString(url).build());
            }
            req.setContent(data.getBytes("utf8"));
        }
        return req;
    }

    private void setParamsFromBuild(MockHttpServletRequest req, UriComponents build) {
        MultiValueMap<String, String> parameters = build.getQueryParams();
        for (String param : parameters.keySet()) {
            final List<String> values = parameters.get(param);
            req.setParameter(param, values.toArray(new String[values.size()]));
        }
        if(!parameters.containsKey("transform")) req.setParameter("transform", new String[] {"0"});
    }

    public void get(String relativeUrlWithParams) {
        setBaseUrl(relativeUrlWithParams);
        this.method = "GET";
    }

    public void post(String url, String data) {
        prepareRequestWithData("POST", url, data);
    }

    private void prepareRequestWithData(String method, String url, String data) {
        setBaseUrl(url);
        this.method = method;
        this.data = data;
    }

    public void put(String url, String data) {
        prepareRequestWithData("PUT", url, data);
    }

    public void delete(String url) {
        setBaseUrl(url);
        this.method = "DELETE";
    }

    public void options(String url) {
        setBaseUrl(url);
        this.method = "OPTIONS";
    }

    public void patch(String url, String data) {
        prepareRequestWithData("PATCH", url, data);
    }
}
