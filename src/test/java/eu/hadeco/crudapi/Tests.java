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


import android.util.Base64;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;

@RunWith(OrderedTestRunner.class)
public abstract class Tests extends TestBase {
    private static int capabilities;
    private static boolean canContinue = true;

    @Before
    public void setUp() {
        Assume.assumeTrue(canContinue);
    }


    @Test
    public void seedDatabase() throws SQLException {
        try (Connection link = getApiConfig().getConnection()) {
            if (!checkVersion(link)) {
                canContinue = false;
                Assert.fail("This server version is not supported!");
            }
            capabilities = getCapabilities(link);
            try {
                seedDatabase(link, capabilities);
            } catch (SQLException ex) {
                ex.printStackTrace();
                canContinue = false;
            }
        }
    }

    @Test
    public void testMissingEntity() {
        TestApi test = new TestApi(this);
        test.get("/Posts");
        test.expect(false, "Not found (entity)");
    }

    @Test
    public void testListPosts() {
        TestApi test = new TestApi(this);
        test.get("/posts");
        test.expect("{\"posts\":{\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[[1,1,1,\"blog started\"],[2,1,2,\"It works!\"]]}}");
    }

    @Test
    public void testListPostColumns() {
        TestApi test = new TestApi(this);
        test.get("/posts?columns=id,content");
        test.expect("{\"posts\":{\"columns\":[\"id\",\"content\"],\"records\":[[1,\"blog started\"],[2,\"It works!\"]]}}");
    }

    @Test
    public void testListPostsWithTransform() {
        TestApi test = new TestApi(this);
        test.get("/posts?transform=1");
        test.expect("{\"posts\":[{\"id\":1,\"user_id\":1,\"category_id\":1,\"content\":\"blog started\"},{\"id\":2,\"user_id\":1,\"category_id\":2,\"content\":\"It works!\"}]}");
    }


    @Test
    public void testReadPost() {
        TestApi test = new TestApi(this);
        test.get("/posts/2");
        test.expect("{\"id\":2,\"user_id\":1,\"category_id\":2,\"content\":\"It works!\"}");
    }


    @Test
    public void testReadPosts() {
        TestApi test = new TestApi(this);
        test.get("/posts/1,2");
        test.expect("[{\"id\":1,\"user_id\":1,\"category_id\":1,\"content\":\"blog started\"},{\"id\":2,\"user_id\":1,\"category_id\":2,\"content\":\"It works!\"}]");
    }

    @Test
    public void testReadPostColumns() {
        TestApi test = new TestApi(this);
        test.get("/posts/2?columns=id,content");
        test.expect("{\"id\":2,\"content\":\"It works!\"}");
    }

    @Test
    public void testAddPost() {
        TestApi test = new TestApi(this);
        test.post("/posts", "{\"user_id\":1,\"category_id\":1,\"content\":\"test\"}");
        test.expect("3");
    }


    @Test
    public void testEditPost() {
        TestApi test = new TestApi(this);
        test.put("/posts/3", "{\"user_id\":1,\"category_id\":1,\"content\":\"test (edited)\"}");
        test.expect("1");
        test.get("/posts/3");
        test.expect("{\"id\":3,\"user_id\":1,\"category_id\":1,\"content\":\"test (edited)\"}");
    }

    @Test
    public void testEditPostColumnsMissingField() {
        TestApi test = new TestApi(this);
        test.put("/posts/3?columns=id,content", "{\"content\":\"test (edited 2)\"}");
        test.expect("1");
        test.get("/posts/3");
        test.expect("{\"id\":3,\"user_id\":1,\"category_id\":1,\"content\":\"test (edited 2)\"}");
    }

    @Test
    public void testEditPostColumnsExtraField() {
        TestApi test = new TestApi(this);
        test.put("/posts/3?columns=id,content", "{\"user_id\":2,\"content\":\"test (edited 3)\"}");
        test.expect("1");
        test.get("/posts/3");
        test.expect("{\"id\":3,\"user_id\":2,\"category_id\":1,\"content\":\"test (edited 3)\"}"); //expect error?
    }

    @Test
    public void testEditPostWithUtf8Content() {
        String utfString = "Hello world, Καλημέρα κόσμε, コンニチハ";
        TestApi test = new TestApi(this);
        test.put("/posts/2", "{\"content\":\"" + utfString + "\"}");
        test.expect("1");
        test.get("/posts/2");
        test.expect("{\"id\":2,\"user_id\":1,\"category_id\":2,\"content\":\"" + utfString + "\"}");
    }

    @Test
    public void testEditPostWithUtf8ContentWithPost() throws UnsupportedEncodingException {
        String utfString = "€ Hello world, Καλημέρα κόσμε, コンニチハ";
        final String encoded = URLEncoder.encode(utfString, "utf8");
        TestApi test = new TestApi(this);
        test.put("/posts/2", "content=" + encoded);
        test.expect("1");
        test.get("/posts/2");
        test.expect("{\"id\":2,\"user_id\":1,\"category_id\":2,\"content\":\"" + utfString + "\"}");
    }


    @Test
    public void testDeletePost() {
        TestApi test = new TestApi(this);
        test.delete("/posts/3");
        test.expect("1");
        test.get("/posts/3");
        test.expect(false, "Not found (object)");
    }

    @Test
    public void testAddPostWithPost() {
        TestApi test = new TestApi(this);
        test.post("/posts", "user_id=1&category_id=1&content=test");
        test.expect("4");
    }

    @Test
    public void testEditPostWithPost() {
        TestApi test = new TestApi(this);
        test.put("/posts/4", "user_id=1&category_id=1&content=test (edited)");
        test.expect("1");
        test.get("/posts/4");
        test.expect("{\"id\":4,\"user_id\":1,\"category_id\":1,\"content\":\"test (edited)\"}");
    }

    @Test
    public void testDeletePostWithPost() {
        TestApi test = new TestApi(this);
        test.delete("/posts/4");
        test.expect("1");
        test.get("/posts/4");
        test.expect(false, "Not found (object)");
    }

    @Test
    public void testListWithPaginate() {
        TestApi test = new TestApi(this);
        for (int i = 1; i <= 10; i++) {
            test.post("/posts", "{\"user_id\":1,\"category_id\":1,\"content\":\"#" + i + "\"}");
            test.expect(String.format("%d", 4 + i));
        }
        test.get("/posts?page=2,2&order=id");
        test.expect("{\"posts\":{\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[[5,1,1,\"#1\"],[6,1,1,\"#2\"]],\"results\":11}}");
    }

    @Test
    public void testListWithPaginateInMultipleOrder() {
        TestApi test = new TestApi(this);
        test.get("/posts?page=1,2&order[]=category_id,asc&order[]=id,desc");
        test.expect("{\"posts\":{\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[[14,1,1,\"#10\"],[12,1,1,\"#8\"]],\"results\":11}}");
    }

    @Test
    public void testListWithPaginateInDescendingOrder() {
        TestApi test = new TestApi(this);
        test.get("/posts?page=2,2&order=id,desc");
        test.expect("{\"posts\":{\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[[11,1,1,\"#7\"],[10,1,1,\"#6\"]],\"results\":11}}");
    }

    @Test
    public void testListWithPaginateLastPage() {
        TestApi test = new TestApi(this);
        test.get("/posts?page=3,5&order=id");
        test.expect("{\"posts\":{\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[[14,1,1,\"#10\"]],\"results\":11}}");
    }

    @Test
    public void testListExampleFromReadmeFullRecord() {
        TestApi test = new TestApi(this);
        test.get("/posts?filter=id,eq,1");
        test.expect("{\"posts\":{\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[[1,1,1,\"blog started\"]]}}");
    }

    @Test
    public void testListExampleFromReadmeWithExclude() {
        TestApi test = new TestApi(this);
        test.get("/posts?exclude=id&filter=id,eq,1");
        test.expect("{\"posts\":{\"columns\":[\"user_id\",\"category_id\",\"content\"],\"records\":[[1,1,\"blog started\"]]}}");
    }

    @Test
    public void testListExampleFromReadme() {
        TestApi test = new TestApi(this);
        test.get("/posts?include=categories,tags,comments&filter=id,eq,1");
        test.expect("{\"posts\":{\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[[1,1,1,\"blog started\"]]},\"post_tags\":{\"relations\":{\"post_id\":\"posts.id\"},\"columns\":[\"id\",\"post_id\",\"tag_id\"],\"records\":[[1,1,1],[2,1,2]]},\"categories\":{\"relations\":{\"id\":\"posts.category_id\"},\"columns\":[\"id\",\"name\",\"icon\"],\"records\":[[1,\"announcement\",null]]},\"tags\":{\"relations\":{\"id\":\"post_tags.tag_id\"},\"columns\":[\"id\",\"name\"],\"records\":[[1,\"funny\"],[2,\"important\"]]},\"comments\":{\"relations\":{\"post_id\":\"posts.id\"},\"columns\":[\"id\",\"post_id\",\"message\"],\"records\":[[1,1,\"great\"],[2,1,\"fantastic\"]]}}");
    }

    @Test
    public void testListExampleFromReadmeWithTransform() {
        TestApi test = new TestApi(this);
        test.get("/posts?include=categories,tags,comments&filter=id,eq,1&transform=1");
        test.expect("{\"posts\":[{\"id\":1,\"post_tags\":[{\"id\":1,\"post_id\":1,\"tag_id\":1,\"tags\":[{\"id\":1,\"name\":\"funny\"}]},{\"id\":2,\"post_id\":1,\"tag_id\":2,\"tags\":[{\"id\":2,\"name\":\"important\"}]}],\"comments\":[{\"id\":1,\"post_id\":1,\"message\":\"great\"},{\"id\":2,\"post_id\":1,\"message\":\"fantastic\"}],\"user_id\":1,\"category_id\":1,\"categories\":[{\"id\":1,\"name\":\"announcement\",\"icon\":null}],\"content\":\"blog started\"}]}");
    }

    @Test
    public void testListExampleFromReadmeWithTransformWithExclude() {
        TestApi test = new TestApi(this);
        test.get("/posts?include=categories,tags,comments&exclude=comments.message&filter=id,eq,1&transform=1");
        test.expect("{\"posts\":[{\"id\":1,\"post_tags\":[{\"id\":1,\"post_id\":1,\"tag_id\":1,\"tags\":[{\"id\":1,\"name\":\"funny\"}]},{\"id\":2,\"post_id\":1,\"tag_id\":2,\"tags\":[{\"id\":2,\"name\":\"important\"}]}],\"comments\":[{\"id\":1,\"post_id\":1},{\"id\":2,\"post_id\":1}],\"user_id\":1,\"category_id\":1,\"categories\":[{\"id\":1,\"name\":\"announcement\",\"icon\":null}],\"content\":\"blog started\"}]}");
    }

    @Test
    public void testEditCategoryWithBinaryContent() {
        String encoded = Base64.encodeToString("\0abc\0\n\r\b\0".getBytes(), Base64.DEFAULT).trim();
        String urlEncoded = encoded.replace("+/", "-_").replace("==", "");
        TestApi test = new TestApi(this);
        test.put("/categories/2", "{\"icon\":\"" + urlEncoded + "\"}");
        test.expect("1");
        test.get("/categories/2");
        test.expect("{\"id\":2,\"name\":\"article\",\"icon\":\"" + encoded + "\"}");
    }

    @Test
    public void testEditCategoryWithNull() {
        TestApi test = new TestApi(this);
        test.put("/categories/2", "{\"icon\":null}");
        test.expect("1");
        test.get("/categories/2");
        test.expect("{\"id\":2,\"name\":\"article\",\"icon\":null}");
    }

    @Test
    public void testEditCategoryWithBinaryContentWithPost() throws UnsupportedEncodingException {
        String encoded = "4oKsIABhYmMACg1cYgA="; //php string "€ \0abc\0\n\r\b\0"
        String urlEncoded = encoded.replace("=", "");
        TestApi test = new TestApi(this);
        test.put("/categories/2", "icon=" + urlEncoded);
        test.expect("1");
        test.get("/categories/2");
        test.expect("{\"id\":2,\"name\":\"article\",\"icon\":\"" + encoded + "\"}");
    }

    @Test
    public void testListCategoriesWithBinaryContent() {
        TestApi test = new TestApi(this);
        test.get("/categories");
        test.expect("{\"categories\":{\"columns\":[\"id\",\"name\",\"icon\"],\"records\":[[1,\"announcement\",null],[2,\"article\",\"4oKsIABhYmMACg1cYgA=\"]]}}");
    }

    @Test
    public void testEditCategoryWithNullWithPost() {
        TestApi test = new TestApi(this);
        test.put("/categories/2", "icon__is_null");
        test.expect("1");
        test.get("/categories/2");
        test.expect("{\"id\":2,\"name\":\"article\",\"icon\":null}");
    }

    @Test
    public void testAddPostFailure() {
        TestApi test = new TestApi(this);
        test.post("/posts", "{\"user_id\":\"a\",\"category_id\":1,\"content\":\"tests\"}");
        test.expect(null);
    }

    @Test
    public void testOptionsRequest() {
        TestApi test = new TestApi(this);
        test.options("/posts/2");
        test.expect("[\"Access-Control-Allow-Headers: Content-Type, X-XSRF-TOKEN\",\"Access-Control-Allow-Methods: OPTIONS, GET, PUT, POST, DELETE, PATCH\",\"Access-Control-Allow-Credentials: true\",\"Access-Control-Max-Age: 1728000\"]");
    }

    @Test
    public void testHidingPasswordColumn() {
        TestApi test = new TestApi(this);
        test.get("/users?filter=id,eq,1&transform=1");
        test.expect("{\"users\":[{\"id\":1,\"username\":\"user1\",\"location\":null}]}");
    }

    @Test
    public void testValidatorErrorMessage() {
        TestApi test = new TestApi(this);
        test.put("/posts/1", "{\"category_id\":\"a\"}");
        test.expect(false, "{\"category_id\":\"must be numeric or boolean\"}");
    }

    @Test
    public void testSanitizerToStripTags() {
        TestApi test = new TestApi(this);
        test.put("/categories/2", "{\"name\":\"<script>alert();</script>\"}");
        test.expect("1");
        test.get("/categories/2");
        test.expect("{\"id\":2,\"name\":\"alert();\",\"icon\":null}");
    }

    @Test
    public void testErrorOnInvalidJson() {
        TestApi test = new TestApi(this);
        test.post("/posts", "{\"}");
        test.expect(false, "Not found (input)");
    }

    @Test
    public void testErrorOnDuplicatePrimaryKey() {
        TestApi test = new TestApi(this);
        test.post("/posts", "{\"id\":1,\"user_id\":1,\"category_id\":1,\"content\":\"blog started (duplicate)\"}");
        test.expect("null");
    }

    @Test
    public void testErrorOnFailingForeignKeyConstraint() {
        TestApi test = new TestApi(this);
        test.post("/posts", "{\"user_id\":3,\"category_id\":1,\"content\":\"fk constraint\"}");
        if (!"SQLite".equals(getEngineName())) {
            test.expect("null");
        } else { //SQLite fk constraint is off by default
            test.expectAny();
            test.delete("/posts/15");
            test.expect("1");
        }
    }

    @Test
    public void testMissingIntermediateTable() {
        TestApi test = new TestApi(this);
        test.get("/users?include=posts,tags&order=posts.id");
        test.expect("{\"users\":{\"columns\":[\"id\",\"username\",\"location\"],\"records\":[[1,\"user1\",null]]},\"posts\":{\"relations\":{\"user_id\":\"users.id\"},\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[[1,1,1,\"blog started\"],[2,1,2,\"€ Hello world, Καλημέρα κόσμε, コンニチハ\"],[5,1,1,\"#1\"],[6,1,1,\"#2\"],[7,1,1,\"#3\"],[8,1,1,\"#4\"],[9,1,1,\"#5\"],[10,1,1,\"#6\"],[11,1,1,\"#7\"],[12,1,1,\"#8\"],[14,1,1,\"#10\"]]},\"post_tags\":{\"relations\":{\"post_id\":\"posts.id\"},\"columns\":[\"id\",\"post_id\",\"tag_id\"],\"records\":[[1,1,1],[2,1,2],[3,2,1],[4,2,2]]},\"tags\":{\"relations\":{\"id\":\"post_tags.tag_id\"},\"columns\":[\"id\",\"name\"],\"records\":[[1,\"funny\"],[2,\"important\"]]}}");
    }

    @Test
    public void testEditUserPassword() {
        TestApi test = new TestApi(this);
        test.put("/users/1", "{\"password\":\"testtest\"}");
        test.expect("1");
    }

    @Test
    public void testEditUserLocation() {
        TestApi test = new TestApi(this);
        test.put("/users/1", "{\"location\":\"POINT(30 20)\"}");
        test.expect("1");
        test.get("/users/1?columns=id,location");
        test.expect("{\"id\":1,\"location\":\"POINT(30 20)\"}");
    }

    @Test
    public void testListUserLocations() {
        TestApi test = new TestApi(this);
        test.get("/users?columns=id,location");
        test.expect("{\"users\":{\"columns\":[\"id\",\"location\"],\"records\":[[1,\"POINT(30 20)\"]]}}");
    }

    @Test
    public void testEditUserWithId() {
        if (!"SQLServer".equals(this.getEngineName())) {
            TestApi test = new TestApi(this);
            //this test always fails on MSSQL as it can't update id
            test.put("/users/1", "{\"id\":2,\"password\":\"testtest2\"}");
            test.expect("1");
            test.get("/users/1?columns=id,username,password");
            test.expect("{\"id\":1,\"username\":\"user1\",\"password\":\"testtest2\"}");
        }
    }

    @Test
    public void testReadOtherUser() {
        TestApi test = new TestApi(this);
        test.get("/users/2");
        test.expect(false, "Not found (object)");
    }

    @Test
    public void testEditOtherUser() {
        TestApi test = new TestApi(this);
        test.put("/users/2", "{\"password\":\"testtest\"}");
        test.expect("0");
    }

    @Test
    public void testFilterCategoryOnNullIcon() {
        TestApi test = new TestApi(this);
        test.get("/categories?filter[]=icon,is,null&transform=1");
        test.expect("{\"categories\":[{\"id\":1,\"name\":\"announcement\",\"icon\":null},{\"id\":2,\"name\":\"alert();\",\"icon\":null}]}");
    }

    @Test
    public void testFilterCategoryOnNotNullIcon() {
        TestApi test = new TestApi(this);
        test.get("/categories?filter[]=icon,nis,null&transform=1");
        test.expect("{\"categories\":[]}");
    }

    @Test
    public void testFilterPostsNotIn() {
        TestApi test = new TestApi(this);
        test.get("/posts?filter[]=id,nin,1,2,3,4,7,8,9,10,11,12,13,14&transform=1");
        test.expect("{\"posts\":[{\"id\":5,\"user_id\":1,\"category_id\":1,\"content\":\"#1\"},{\"id\":6,\"user_id\":1,\"category_id\":1,\"content\":\"#2\"}]}");
    }

    @Test
    public void testFilterCommentsStringIn() {
        TestApi test = new TestApi(this);
        test.get("/comments?filter=message,in,fantastic,thank you&transform=1");
        test.expect("{\"comments\":[{\"id\":2,\"post_id\":1,\"message\":\"fantastic\"},{\"id\":3,\"post_id\":2,\"message\":\"thank you\"}]}");
    }

    @Test
    public void testFilterPostsBetween() {
        TestApi test = new TestApi(this);
        test.get("/posts?filter[]=id,bt,5,6&transform=1");
        test.expect("{\"posts\":[{\"id\":5,\"user_id\":1,\"category_id\":1,\"content\":\"#1\"},{\"id\":6,\"user_id\":1,\"category_id\":1,\"content\":\"#2\"}]}");
    }

    @Test
    public void testFilterPostsNotBetween() {
        TestApi test = new TestApi(this);
        test.get("/posts?filter[]=id,nbt,2,13&transform=1");
        test.expect("{\"posts\":[{\"id\":1,\"user_id\":1,\"category_id\":1,\"content\":\"blog started\"},{\"id\":14,\"user_id\":1,\"category_id\":1,\"content\":\"#10\"}]}");
    }

    @Test
    public void testColumnsWithTable() {
        TestApi test = new TestApi(this);
        test.get("/posts?columns=posts.content&filter=id,eq,1&transform=1");
        test.expect("{\"posts\":[{\"content\":\"blog started\"}]}");
    }

    @Test
    public void testColumnsWithTableWildcard() {
        TestApi test = new TestApi(this);
        test.get("/posts?columns=posts.*&filter=id,eq,1&transform=1");
        test.expect("{\"posts\":[{\"id\":1,\"user_id\":1,\"category_id\":1,\"content\":\"blog started\"}]}");
    }

    @Test
    public void testColumnsOnInclude() {
        TestApi test = new TestApi(this);
        test.get("/posts?include=categories&columns=categories.name&filter=id,eq,1&transform=1");
        test.expect("{\"posts\":[{\"id\":1,\"category_id\":1,\"categories\":[{\"id\":1,\"name\":\"announcement\"}]}]}");
    }

    @Test
    public void testFilterOnRelationAnd() {
        TestApi test = new TestApi(this);
        test.get("/categories?include=posts&filter[]=id,ge,1&filter[]=id,le,1&filter[]=id,le,2&filter[]=posts.id,lt,8&filter[]=posts.id,gt,4&&order=posts.id");
        test.expect("{\"categories\":{\"columns\":[\"id\",\"name\",\"icon\"],\"records\":[[1,\"announcement\",null]]},\"posts\":{\"relations\":{\"category_id\":\"categories.id\"},\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[[5,1,1,\"#1\"],[6,1,1,\"#2\"],[7,1,1,\"#3\"]]}}");
    }

    @Test
    public void testFilterOnRelationOr() {
        TestApi test = new TestApi(this);
        test.get("/categories?include=posts&filter[]=id,ge,1&filter[]=id,le,1&filter[]=posts.id,eq,5&filter[]=posts.id,eq,6&filter[]=posts.id,eq,7&satisfy=all,posts.any&order=posts.id");
        test.expect("{\"categories\":{\"columns\":[\"id\",\"name\",\"icon\"],\"records\":[[1,\"announcement\",null]]},\"posts\":{\"relations\":{\"category_id\":\"categories.id\"},\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[[1,1,1,\"blog started\"],[5,1,1,\"#1\"],[6,1,1,\"#2\"],[7,1,1,\"#3\"],[8,1,1,\"#4\"],[9,1,1,\"#5\"],[10,1,1,\"#6\"],[11,1,1,\"#7\"],[12,1,1,\"#8\"],[14,1,1,\"#10\"]]}}");
    }

    @Test
    public void testColumnsOnWrongInclude() {
        TestApi test = new TestApi(this);
        test.get("/posts?include=categories&columns=categories&filter=id,eq,1&transform=1");
        test.expect(false, "Not found (column)");
    }

    @Test
    public void testColumnsOnImplicitJoin() {
        TestApi test = new TestApi(this);
        test.get("/posts?include=tags&columns=posts.id,tags.name&filter=id,eq,1&transform=1");
        test.expect("{\"posts\":[{\"id\":1,\"post_tags\":[{\"id\":1,\"post_id\":1,\"tag_id\":1,\"tags\":[{\"id\":1,\"name\":\"funny\"}]},{\"id\":2,\"post_id\":1,\"tag_id\":2,\"tags\":[{\"id\":2,\"name\":\"important\"}]}]}]}");
    }

    @Test
    public void testSpatialFilterWithin() {
        if ((capabilities & GIS) > 0) {
            TestApi test = new TestApi(this);
            test.get("/users?columns=id,username&filter=location,swi,POINT(30 20)");
            test.expect("{\"users\":{\"columns\":[\"id\",\"username\"],\"records\":[[1,\"user1\"]]}}");
        }
    }

    @Test
    public void testAddPostsWithNonExistingCategory() {
        TestApi test = new TestApi(this);
        test.post("/posts", "[{\"user_id\":1,\"category_id\":1,\"content\":\"tests\"},{\"user_id\":1,\"category_id\":15,\"content\":\"tests\"}]");
        if (!"SQLite".equals(getEngineName())) {
            test.expect("null");
        } else {
            test.expectAny();
            test.delete("/posts/16,17");
            test.expectAny();
        }
        test.get("/posts?columns=content&filter=content,eq,tests");
        test.expect("{\"posts\":{\"columns\":[\"content\"],\"records\":[]}}");
    }

    @Test
    public void testAddPosts() {
        TestApi test = new TestApi(this);
        test.post("/posts", "[{\"user_id\":1,\"category_id\":1,\"content\":\"tests\"},{\"user_id\":1,\"category_id\":1,\"content\":\"tests\"}]");
        test.expectAny();
        test.get("/posts?columns=content&filter=content,eq,tests");
        test.expect("{\"posts\":{\"columns\":[\"content\"],\"records\":[[\"tests\"],[\"tests\"]]}}");
    }

    @Test
    public void testListEvents() {
        TestApi test = new TestApi(this);
        test.get("/events?columns=datetime");
        test.expect("{\"events\":{\"columns\":[\"datetime\"],\"records\":[[\"2016-01-01 13:01:01\"]]}}");
    }

    @Test
    public void testIncrementEventVisitors() {
        TestApi test = new TestApi(this);
        test.patch("/events/1", "{\"visitors\":11}");
        test.expect("1");
        test.get("/events/1");
        test.expect("{\"id\":1,\"name\":\"Launch\",\"datetime\":\"2016-01-01 13:01:01\",\"visitors\":11}");
    }

    @Test
    public void testIncrementEventVisitorsWithZero() {
        TestApi test = new TestApi(this);
        test.patch("/events/1", "{\"visitors\":0}");
        test.expect("1");
        test.get("/events/1");
        test.expect("{\"id\":1,\"name\":\"Launch\",\"datetime\":\"2016-01-01 13:01:01\",\"visitors\":11}");
    }

    @Test
    public void testDecrementEventVisitors() {
        TestApi test = new TestApi(this);
        test.patch("/events/1", "{\"visitors\":-5}");
        test.expect("1");
        test.get("/events/1");
        test.expect("{\"id\":1,\"name\":\"Launch\",\"datetime\":\"2016-01-01 13:01:01\",\"visitors\":6}");
    }

    @Test
    public void testListTagUsage() {
        TestApi test = new TestApi(this);
        test.get("/tag_usage");
        test.expect("{\"tag_usage\":{\"columns\":[\"name\",\"count\"],\"records\":[[\"funny\",2],[\"important\",2]]}}");
    }

    @Test
    public void testUpdateMultipleTags() {
        TestApi test = new TestApi(this);
        test.get("/tags?transform=1");
        test.expect("{\"tags\":[{\"id\":1,\"name\":\"funny\"},{\"id\":2,\"name\":\"important\"}]}");
        test.put("/tags/1,2", "[{\"name\":\"funny\"},{\"name\":\"important\"}]");
        test.expect("[1,1]");
    }

    @Test
    public void testUpdateMultipleTagsTooManyIds() {
        TestApi test = new TestApi(this);
        test.put("/tags/1,2,3", "[{\"name\":\"funny!!!\"},{\"name\":\"important\"}]");
        test.expect(false, "Not found (subject)");
        test.get("/tags?transform=1");
        test.expect("{\"tags\":[{\"id\":1,\"name\":\"funny\"},{\"id\":2,\"name\":\"important\"}]}");
    }

    @Test
    public void testUpdateMultipleTagsWithoutFields() {
        TestApi test = new TestApi(this);
        test.put("/tags/1,2", "[{\"name\":\"funny!!!\"},{}]");
        test.expect(false, "Not found (input)");
        test.get("/tags?transform=1");
        test.expect("{\"tags\":[{\"id\":1,\"name\":\"funny\"},{\"id\":2,\"name\":\"important\"}]}");
    }

    @Test
    public void testDeleteMultipleTags() {
        TestApi test = new TestApi(this);
        test.post("/tags", "[{\"name\":\"extra\"},{\"name\":\"more\"}]");
        test.expect("[3,4]");
        test.delete("/tags/3,4");
        test.expect("[1,1]");
        test.get("/tags?transform=1");
        test.expect("{\"tags\":[{\"id\":1,\"name\":\"funny\"},{\"id\":2,\"name\":\"important\"}]}");
    }

    @Test
    public void testListProducts() {
        TestApi test = new TestApi(this);
        test.get("/products?columns=id,name,price&transform=1");
        test.expect("{\"products\":[{\"id\":1,\"name\":\"Calculator\",\"price\":\"23.01\"}]}");
    }

    @Test
    public void testListProductsProperties() {
        TestApi test = new TestApi(this);
        test.get("/products?columns=id,properties&transform=1");
        if ((capabilities & JSON) > 0) {
            test.expect("{\"products\":[{\"id\":1,\"properties\":{\"depth\":false,\"model\":\"TRX-120\",\"width\":100,\"height\":null}}]}");
        } else {
            test.expect("{\"products\":[{\"id\":1,\"properties\":\"{\\\"depth\\\":false,\\\"model\\\":\\\"TRX-120\\\",\\\"width\\\":100,\\\"height\\\":null}\"}]}");
        }
    }

    @Test
    public void testReadProductProperties() {
        TestApi test = new TestApi(this);
        test.get("/products/1?columns=id,properties");
        if ((capabilities & JSON) > 0) {
            test.expect("{\"id\":1,\"properties\":{\"depth\":false,\"model\":\"TRX-120\",\"width\":100,\"height\":null}}");
        } else {
            test.expect("{\"id\":1,\"properties\":\"{\\\"depth\\\":false,\\\"model\\\":\\\"TRX-120\\\",\\\"width\\\":100,\\\"height\\\":null}\"}");
        }
    }

    @Test
    public void testWriteProductProperties() {
        TestApi test = new TestApi(this);
        if ((capabilities & JSON) > 0) {
            test.put("/products/1", "{\"properties\":{\"depth\":false,\"model\":\"TRX-120\",\"width\":100,\"height\":123}}");
        } else {
            test.put("/products/1", "{\"properties\":\"{\\\"depth\\\":false,\\\"model\\\":\\\"TRX-120\\\",\\\"width\\\":100,\\\"height\\\":123}\"}");
        }
        test.expect("1");
        test.get("/products/1?columns=id,properties");
        if ((capabilities & JSON) > 0) {
            test.expect("{\"id\":1,\"properties\":{\"depth\":false,\"model\":\"TRX-120\",\"width\":100,\"height\":123}}");
        } else {
            test.expect("{\"id\":1,\"properties\":\"{\\\"depth\\\":false,\\\"model\\\":\\\"TRX-120\\\",\\\"width\\\":100,\\\"height\\\":123}\"}");
        }
    }

    @Test
    public void testAddProducts() {
        TestApi test = new TestApi(this);
        if ((capabilities & JSON) > 0) {
            test.post("/products", "{\"name\":\"Laptop\",\"price\":1299.99,\"properties\":{}}");
        } else {
            test.post("/products", "{\"name\":\"Laptop\",\"price\":1299.99,\"properties\":\"{}\"}");
        }
        test.expect("2");
        test.get("/products/2?columns=id,created_at,deleted_at");
        test.expect("{\"id\":2,\"created_at\":\"2013-12-11 10:09:08\",\"deleted_at\":null}");
    }

    @Test
    public void testSoftDeleteProducts() {
        TestApi test = new TestApi(this);
        test.delete("/products/1,2");
        test.expect("[1,1]");
        test.get("/products?columns=id,deleted_at");
        test.expect("{\"products\":{\"columns\":[\"id\",\"deleted_at\"],\"records\":[[1,\"2013-12-11 11:10:09\"],[2,\"2013-12-11 11:10:09\"]]}}");
    }

    @Test
    public void testVarBinaryBarcodes() {
        TestApi test = new TestApi(this);
        test.get("/barcodes?transform=1");
        test.expect("{\"barcodes\":[{\"id\":1,\"product_id\":1,\"hex\":\"00ff01\",\"bin\":\"AP8B\"}]}");
    }

    @Test
    public void testEditPostWithApostrophe() {
        TestApi test = new TestApi(this);
        test.put("/posts/1", "[{\"user_id\":1,\"category_id\":1,\"content\":\"blog start'd\"}]");
        test.expect("1");
        test.get("/posts/1");
        test.expect("{\"id\":1,\"user_id\":1,\"category_id\":1,\"content\":\"blog start'd\"}");
    }

    @Test
    public void testIncludeNonRelatedTables() {
        TestApi test = new TestApi(this);
        test.get("/users?include=tags&columns=id,username,password&transform=1");
        test.expect("{\"users\":[{\"id\":1,\"username\":\"user1\"}],\"tags\":[{\"id\":1,\"name\":\"funny\"},{\"id\":2,\"name\":\"important\"}]}");
    }

    @Test
    public void testAddPostWithLeadingWhitespaceInJSON() {
        TestApi test = new TestApi(this);
        test.post("/posts", " \n {\"user_id\":1,\"category_id\":1,\"content\":\"test whitespace\"}   ");
        test.expect("20");
        test.get("/posts/20");
        test.expect("{\"id\":20,\"user_id\":1,\"category_id\":1,\"content\":\"test whitespace\"}");
    }

    //    @Test
    public void testListPostWithIncludeButNoRecords() {
        TestApi test = new TestApi(this);
        test.get("/posts?filter=id,eq,999&include=tags");
        test.expect("{\"posts\":{\"columns\":[\"id\",\"user_id\",\"category_id\",\"content\"],\"records\":[]},\"post_tags\":{\"relations\":{\"post_id\":\"posts.id\"},\"columns\":[\"id\",\"post_id\",\"tag_id\"],\"records\":[]},\"tags\":{\"relations\":{\"id\":\"post_tags.tag_id\"},\"columns\":[\"id\",\"name\"],\"records\":[]}}");
    }

    @Test
    public void testListUsersExcludeTenancyId() {
        TestApi test = new TestApi(this);
        test.get("/users?exclude=id");
        test.expect("{\"users\":{\"columns\":[\"username\",\"location\"],\"records\":[[\"user1\",\"POINT(30 20)\"]]}}");
    }

    @Test
    public void testListUsersColumnsWithoutTenancyId() {
        TestApi test = new TestApi(this);
        test.get("/users?columns=username,location");
        test.expect("{\"users\":{\"columns\":[\"username\",\"location\"],\"records\":[[\"user1\",\"POINT(30 20)\"]]}}");
    }

    @Test
    public void testTenancyCreateColumns() {
        // creation should fail, since due to tenancy function it will try to create with id=1, which is a PK and is already taken
        TestApi test = new TestApi(this);
        test.post("/users?columns=username,password,location", "{\"username\":\"user3\",\"password\":\"pass3\",\"location\":null}");
        test.expect("null");
    }

    @Test
    public void testTenancyCreateExclude() {
        // creation should fail, since due to tenancy function it will try to create with id=1, which is a PK and is already taken
        TestApi test = new TestApi(this);
        test.post("/users?exclude=id", "{\"username\":\"user3\",\"password\":\"pass3\",\"location\":null}");
        test.expect("null");
    }

    @Test
    public void testTenancyListColumns() {
        // should list only user with id=1 (exactly 1 record)
        TestApi test = new TestApi(this);
        test.get("/users?columns=username,location");
        test.expect("{\"users\":{\"columns\":[\"username\",\"location\"],\"records\":[[\"user1\",\"POINT(30 20)\"]]}}");
    }

    @Test
    public void testTenancyListExclude() {
        // should list only user with id=1 (exactly 1 record)
        TestApi test = new TestApi(this);
        test.get("/users?exclude=id");
        test.expect("{\"users\":{\"columns\":[\"username\",\"location\"],\"records\":[[\"user1\",\"POINT(30 20)\"]]}}");
    }

    @Test
    public void testTenancyReadColumns() {
        // should fail, since due to tenancy function user id=2 is unvailable to us
        TestApi test = new TestApi(this);
        test.get("/users/2?columns=username,location");
        test.expect(false, "Not found (object)");
    }

    @Test
    public void testTenancyReadExclude() {
        // should fail, since due to tenancy function user id=2 is unvailable to us
        TestApi test = new TestApi(this);
        test.get("/users/2?exclude=id");
        test.expect(false, "Not found (object)");
    }

    @Test
    public void testTenancyUpdateColumns() {
        // should fail, since due to tenancy function user id=2 is unavailable to us
        TestApi test = new TestApi(this);
        test.put("/users/2?columns=location", "{\"location\":\"POINT(0 0)\"}");
        test.expect("0");
    }

    @Test
    public void testTenancyUpdateExclude() {
        // should fail, since due to tenancy function user id=2 is unavailable to us
        TestApi test = new TestApi(this);
        test.put("/users/2?exclude=id", "{\"location\":\"POINT(0 0)\"}");
        test.expect("0");
    }

    @Test
    public void testTenancyDeleteColumns() {
        // should fail, since due to tenancy function user id=2 is unvailable to us
        TestApi test = new TestApi(this);
        test.delete("/users/2?columns=location");
        test.expect("0");
    }

    @Test
    public void testTenancyDeleteExclude() {
        // should fail, since due to tenancy function user id=2 is unvailable to us
        TestApi test = new TestApi(this);
        test.delete("/users/2?exclude=id");
        test.expect("0");
    }

    @Test
    public void testInsertReservedWord() {
        // test insert into column with reserved sql name
        TestApi test = new TestApi(this);
        test.post("/parameters", "{\"key\":\"key\",\"value\":\"testValue\"}");
        test.expectAny();
    }

    @Test
    public void testReadColumnReservedWord() {
        // read from column with reserved sql names
        TestApi test = new TestApi(this);
        test.get("/parameters/key");
        if(!"SQLServer".equals(getEngineName())) {
            //this test always fails on MsSQL only
            test.expect("{\"key\":\"key\",\"value\":\"testValue\"}");
        }
    }
}
