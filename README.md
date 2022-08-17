## UPDATED version 1.1
Several security vulerabilities have been closed:
 - Bump mysql-connector-java from 5.1.49 to > 8.0.28
 - org.postgresql:postgresql > 42.2.26
 - Bump gson from 2.8.7 to 2.8.9
Because of these updates, this version is no longer compatible with Java 7

This is a full Java port of the [php-crud-api](https://github.com/mevdschee/php-crud-api) project (single file REST API).
It implements almost full functionality (with the exception of Swagger tools), and uses JDBC (should be platform and SQL-dialect independent).
It differs from the original project by some optimizations:
 - Result is transformed by default (if transform parameter is not specified, it is assumed to be 1)
 - Metadata cache (reading metadata is expensive, so it's cached for 1 minute, providing huge performance boost)
 - Streaming API in almost all cases (with the exception of relations: 'include' command).
 - Connection pooling through Hikari pool

These features makes it a lot more responsive under heavy load than php version.

## Installation

Install dependencies using:

    sudo apt-get install maven openjdk-8-jdk

Then build the server.

    mvn compile

## Configuring

Configuration has done through the class ApiConfig. Have a look at the constructor or tests for configuration examples

## Running

To run the api (during development) type:

    mvn compile exec:java

In production I recommend deploying the JAR file as described below.

## Usage

You can do all CRUD (Create, Read, Update, Delete) operations and one extra List operation. Here is how:

## List

List all records of a database table.

```
GET http://localhost:8080/categories?transform=0
```

Output:

```
{"categories":{"columns":["id","name"],"records":[[1,"Internet"],[3,"Web development"]]}}
```

## List + Transform

List all records of a database table and transform them to objects. In java CRUD API, transform is on by default!

```
GET http://localhost:8080/categories?transform=1
```

Output:

```
{"categories":[{"id":1,"name":"Internet"},{"id":3,"name":"Web development"}]}
```

NB: This transform is not as CPU and memory intensive as in php-crud-api (java API streams json objects very effectively), but it can also be executed client-side (see: [lib](https://github.com/mevdschee/php-crud-api/tree/master/lib)).

## List + Filter

Search is implemented with the "filter" parameter. You need to specify the column name, a comma, the match type, another commma and the value you want to filter on. These are supported match types:

  - cs: contain string (string contains value)
  - sw: start with (string starts with value)
  - ew: end with (string end with value)
  - eq: equal (string or number matches exactly)
  - lt: lower than (number is lower than value)
  - le: lower or equal (number is lower than or equal to value)
  - ge: greater or equal (number is higher than or equal to value)
  - gt: greater than (number is higher than value)
  - bt: between (number is between two comma separated values)
  - in: in (number is in comma separated list of values)
  - is: is null (field contains "NULL" value)

You can negate all filters by prepending a 'n' character, so that 'eq' becomes 'neq'.

```
GET http://localhost:8080/categories?filter=name,eq,Internet&transform=0
GET http://localhost:8080/categories?filter=name,sw,Inter&transform=0
GET http://localhost:8080/categories?filter=id,le,1&transform=0
GET http://localhost:8080/categories?filter=id,ngt,2&transform=0
GET http://localhost:8080/categories?filter=id,bt,1,1&transform=0
GET http://localhost:8080/categories?filter=categories.id,eq,1&transform=0
```

Output:

```
{"categories":{"columns":["id","name"],"records":[[1,"Internet"]]}}
```

NB: You may specify table name before the field name, seperated with a dot.

## List + Filter + Satisfy

Multiple filters can be applied by using "filter[]" instead of "filter" as a parameter name. Then the parameter "satisfy" is used to indicate whether "all" (default) or "any" filter should be satisfied to lead to a match:

```
GET http://localhost:8080/categories?filter[]=id,eq,1&filter[]=id,eq,3&satisfy=any&transform=0
GET http://localhost:8080/categories?filter[]=id,ge,1&filter[]=id,le,3&satisfy=all&transform=0
GET http://localhost:8080/categories?filter[]=id,ge,1&filter[]=id,le,3&satisfy=categories.all&transform=0
GET http://localhost:8080/categories?filter[]=id,ge,1&filter[]=id,le,3&transform=0
```

Output:

```
{"categories":{"columns":["id","name"],"records":[[1,"Internet"],[3,"Web development"]]}}
```

NB: You may specify "satisfy=categories.all,posts.any" if you want to mix "and" and "or" for different tables.

## List + Column selection

By default all columns are selected. With the "columns" parameter you can select specific columns. Multiple columns should be comma separated.
An asterisk ("*") may be used as a wildcard to indicate "all columns". Similar to "columns" you may use the "exclude" parameter to remove certain columns:

```
GET http://localhost:8080/categories?columns=name&transform=0
GET http://localhost:8080/categories?columns=categories.name&transform=0
GET http://localhost:8080/categories?exclude=categories.id&transform=0
```

Output:

```
{"categories":{"columns":["name"],"records":[["Web development"],["Internet"]]}}
```

NB: Columns that are used to include related entities are automatically added and cannot be left out of the output.

## List + Order

With the "order" parameter you can sort. By default the sort is in ascending order, but by specifying "desc" this can be reversed:

```
GET http://localhost:8080/categories?order=name,desc&transform=0
GET http://localhost:8080/posts?order[]=icon,desc&order[]=name&transform=0
```

Output:

```
{"categories":{"columns":["id","name"],"records":[[3,"Web development"],[1,"Internet"]]}}
```

NB: You may sort on multiple fields by using "order[]" instead of "order" as a parameter name.

## List + Order + Pagination

The "page" parameter holds the requested page. The default page size is 20, but can be adjusted (e.g. to 50):

```
GET http://localhost:8080/categories?order=id&page=1&transform=0
GET http://localhost:8080/categories?order=id&page=1,50&transform=0
```

Output:

```
{"categories":{"columns":["id","name"],"records":[[1,"Internet"],[3,"Web development"]],"results":2}}
```

NB: Pages that are not ordered cannot be paginated.

## Create

You can easily add a record using the POST method (x-www-form-urlencoded, see rfc1738). The call returns the "last insert id".

```
POST http://localhost:8080/categories
id=1&name=Internet
```

Output:

```
1
```

Note that the fields that are not specified in the request get the default value as specified in the database.

## Create (with JSON object)

Alternatively you can send a JSON object in the body. The call returns the "last insert id".

```
POST http://localhost:8080/categories
{"id":1,"name":"Internet"}
```

Output:

```
1
```

Note that the fields that are not specified in the request get the default value as specified in the database.

## Create (with JSON array)

Alternatively you can send a JSON array containing multiple JSON objects in the body. The call returns an array of "last insert id" values.

```
POST http://localhost:8080/categories
[{"name":"Internet"},{"name":"Programming"},{"name":"Web development"}]
```

Output:

```
[1,2,3]
```

This call uses a transaction and will either insert all or no records. If the transaction fails it will return 'null'.

## Read

If you want to read a single object you can use:

```
GET http://localhost:8080/categories/1
```

Output:

```
{"id":1,"name":"Internet"}
```

## Read (multiple)

If you want to read multiple objects you can use:

```
GET http://localhost:8080/categories/1,2
```

Output:

```
[{"id":1,"name":"Internet"},{"id":2,"name":"Programming"}]
```

## Update

Editing a record is done with the PUT method. The call returns the number of rows affected.

```
PUT http://localhost:8080/categories/2
name=Internet+networking
```

Output:

```
1
```

Note that only fields that are specified in the request will be updated.

## Update (with JSON object)

Alternatively you can send a JSON object in the body. The call returns the number of rows affected.

```
PUT http://localhost:8080/categories/2
{"name":"Internet networking"}
```

Output:

```
1
```

Note that only fields that are specified in the request will be updated.

## Update (with JSON array)

Alternatively you can send a JSON array containing multiple JSON objects in the body. The call returns an array of the rows affected.

```
PUT http://localhost:8080/categories/1,2
[{"name":"Internet"},{"name":"Programming"}]
```

Output:

```
[1,1]
```

The number of primary key values in the URL should match the number of elements in the JSON array (and be in the same order).

This call uses a transaction and will either update all or no records. If the transaction fails it will return 'null'.

## Delete

The DELETE verb is used to delete a record. The call returns the number of rows affected.

```
DELETE http://localhost:8080/categories/2
```

Output:

```
1
```

## Delete (multiple)

The DELETE verb can also be used to delete multiple records. The call returns the number of rows affected for each primary key value specified in the URL.

```
DELETE http://localhost:8080/categories/1,2
```

Output:

```
[1,1]
```

This call uses a transaction and will either delete all or no records. If the transaction fails it will return 'null'.

## Relations

The explanation of this feature is based on the data structure from the ```blog.sql``` database file. This database is a very simple blog data structure with corresponding foreign key relations between the tables. These foreign key constraints are required as the relationship detection is based on them, not on column naming.

You can get the "post" that has "id" equal to "1" with it's corresponding "categories", "tags" and "comments" using:

```
GET http://localhost:8080/posts?include=categories,tags,comments&filter=id,eq,1&transform=0
```

Output:

```
{
    "posts": {
        "columns": [
            "id",
            "user_id",
            "category_id",
            "content"
        ],
        "records": [
            [
                1,
                1,
                1,
                "blog started"
            ]
        ]
    },
    "post_tags": {
        "relations": {
            "post_id": "posts.id"
        },
        "columns": [
            "id",
            "post_id",
            "tag_id"
        ],
        "records": [
            [
                1,
                1,
                1
            ],
            [
                2,
                1,
                2
            ]
        ]
    },
    "categories": {
        "relations": {
            "id": "posts.category_id"
        },
        "columns": [
            "id",
            "name"
        ],
        "records": [
            [
                1,
                "anouncement"
            ]
        ]
    },
    "tags": {
        "relations": {
            "id": "post_tags.tag_id"
        },
        "columns": [
            "id",
            "name"
        ],
        "records": [
            [
                1,
                "funny"
            ],
            [
                2,
                "important"
            ]
        ]
    },
    "comments": {
        "relations": {
            "post_id": "posts.id"
        },
        "columns": [
            "id",
            "post_id",
            "message"
        ],
        "records": [
            [
                1,
                1,
                "great"
            ],
            [
                2,
                1,
                "fantastic"
            ]
        ]
    }
}
```

If you omit 'transform=0' parameter, the to structure the data hierarchical like this:

```
{
    "posts": [
        {
            "id": 1,
            "post_tags": [
                {
                    "id": 1,
                    "post_id": 1,
                    "tag_id": 1,
                    "tags": [
                        {
                            "id": 1,
                            "name": "funny"
                        }
                    ]
                },
                {
                    "id": 2,
                    "post_id": 1,
                    "tag_id": 2,
                    "tags": [
                        {
                            "id": 2,
                            "name": "important"
                        }
                    ]
                }
            ],
            "comments": [
                {
                    "id": 1,
                    "post_id": 1,
                    "message": "great"
                },
                {
                    "id": 2,
                    "post_id": 1,
                    "message": "fantastic"
                }
            ],
            "user_id": 1,
            "category_id": 1,
            "categories": [
                {
                    "id": 1,
                    "name": "anouncement"
                }
            ],
            "content": "blog started"
        }
    ]
}
```

## Permissions

By default a single database is exposed with all it's tables and columns in read-write mode. You can change the permissions by specifying
a 'tableAuthorizer()' and/or a 'columnAuthorizer()' function that returns a boolean indicating whether or not the table or column is allowed
for a specific CRUD action.

## Record filter

By defining a 'recordFilter()' function you can apply a forced filter, for instance to implement roles in a database system.
The rule "you cannot view unpublished blog posts unless you have the admin role" can be implemented with this filter.
Return null if record filter doesn't apply to this column

```
    @Override
    protected String[] recordFilter(RequestHandler.Actions action, String database, String table) {
        return "posts".equals(table) ? new String[]{"id,neq,13"} : null;
    }
```

## Multi-tenancy

The 'tenancyFunction()' allows you to expose an API for a multi-tenant database schema. In the simplest model all tables have a column
named 'customer_id' and the 'tenancyFunction()' is defined as:

```
    @Override
    protected Object tenancyFunction(RequestHandler.Actions action, String database, String table, String column) {
        return "users".equals(table) && "id".equals(column) ? 1 : null;
    }
```

## Sanitizing input

By default all input is accepted and sent to the database. If you want to strip (certain) HTML tags before storing you may specify a
'inputSanitizer()' function that returns the adjusted value.

## Validating input

By default all input is accepted. If you want to validate the input, you may specify a 'inputValidator()' function that returns a boolean
indicating whether or not the value is valid.

## Multi-Database

The code also supports multi-database API's. These have URLs where the first segment in the path is the database and not the table name.
This can be enabled by NOT specifying a database in the configuration. Also the permissions in the configuration should contain a dot
character to seperate the database from the table name. The databases 'mysql', 'information_schema' and 'sys' are automatically blocked.

## Atomic increment (for counters)

Incrementing a numeric field of a record is done with the PATCH method (non-numeric fields are ignored).
Decrementing can be done using a negative increment value.
To add '2' to the field 'visitors' in the 'events' table for record with primary key '1', execute:

```
PATCH http://localhost:8080/events/1
{"visitors":2}
```

Output:

```
1
```

The call returns the number of rows affected. Note that multiple fields can be incremented and batch operations are supported (see: update/PUT).

## Binary data

Binary fields are automatically detected and data in those fields is returned using base64 encoding.

```
GET http://localhost:8080/categories/2
```

Output:

```
{"id":2,"name":"funny","icon":"ZGF0YQ=="}
```

When sending a record that contains a binary field you will also have to send base64 encoded data.

```
PUT http://localhost:8080/categories/2
icon=ZGF0YQ
```

In the above example you see how binary data is sent. Both "base64url" and standard "base64" are allowed (see rfc4648).


## Spatial/GIS support

There is also support for spatial filters:

  - sco: spatial contains (geometry contains another)
  - scr: spatial crosses (geometry crosses another)
  - sdi: spatial disjoint (geometry is disjoint from another)
  - seq: spatial equal (geometry is equal to another)
  - sin: spatial intersects (geometry intersects another)
  - sov: spatial overlaps (geometry overlaps another)
  - sto: spatial touches (geometry touches another)
  - swi: spatial within (geometry is within another)
  - sic: spatial is closed (geometry is closed and simple)
  - sis: spatial is simple (geometry is simple)
  - siv: spatial is valid (geometry is valid)

You can negate these filters as well by prepending a 'n' character, so that 'sco' becomes 'nsco'.

Example:

```
GET http://localhost:8080/countries?columns=name,shape&filter[]=shape,sco,POINT(30 20)
```

Output:

```
{"countries":{"columns":["name","shape"],"records":[["Italy","POLYGON((30 10,40 40,20 40,10 20,30 10))"]]}}
```

When sending a record that contains a geometry (spatial) field you will also have to send a WKT string.

```
PUT http://localhost:8080/users/1
{"location":"POINT(30 20)"}
```

In the above example you see how a [WKT string](https://en.wikipedia.org/wiki/Well-known_text) is sent.
Note: Oracle XE doesn't support WKT string conversion.
"Oracle Express does not have a Javavm in the database and the WKT conversion routines need this as the feature is implemented as Java stored procedures. So these WKT routines are not supported on the Express edition."
## Unstructured data support

You may store JSON documents in JSON (MySQL), JSONB (PostgreSQL) or XML (SQL Server) field types in the database.
These documents have no schema. Whitespace in the structure is not maintained.

## Sending NULL

When using the POST method (x-www-form-urlencoded, see rfc1738) a database NULL value can be set using a parameter with the "__is_null" suffix:

```
PUT http://localhost:8080/categories/2
name=Internet&icon__is_null
```

When sending JSON data, then sending a NULL value for a nullable database field is easier as you can use the JSON "null" value (without quotes).

```
PUT http://localhost:8080/categories/2
{"name":"Internet","icon":null}
```

## Automatic fields

Before any operation the 'before' function is called that allows you to do set some automatic fields.
Note that the 'input' parameter is writable and is an object (or 'false' when it is missing or invalid).

## Soft delete

The 'before' function allows modification of the request parameters and can (for instance) be used to implement soft delete behavior.

```
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
        @Override
        protected boolean columnAuthorizer(RequestHandler.Actions action, String database, String table, String column) {
            return !("password".equals(column) && RequestHandler.Actions.LIST.equals(action));
        }

        @Override
        protected String[] recordFilter(RequestHandler.Actions action, String database, String table) {
            return "posts".equals(table) ? new String[]{"id,neq,13"} : null;
        }
```

## Multi-domain CORS (not supported in java-crud-api)

By specifying `allow_origin` in the configuration you can control the `Access-Control-Allow-Origin` response header that is being sent.

If you set `allow_origin` to `*` the `Access-Control-Allow-Origin` response header will be set to `*`.
In all other cases the `Access-Control-Allow-Origin` response header is set to the value of the request header `Origin` when a match is found.

You may also specify `allow_origin` to `https://*.yourdomain.com` matching any host that starts with `https://` and ends on `.yourdomain.com`.

Multiple hosts may be specified using a comma, allowing you to set `allow_origin` to `https://yourdomain.com, https://*.yourdomain.com`.

## 64 bit integers in JavaScript

JavaScript does not support 64 bit integers. All numbers are stored as 64 bit floating point values. The mantissa of a 64 bit floating point
number is only 53 bit and that is why all integer numbers bigger than 53 bit may cause problems in JavaScript.

## Errors

The following types of 404 'Not found' errors may be reported:

  - entity (could not find entity)
  - object (instance not found on read)
  - input (instance not found on create)
  - subject (instance not found on update)
  - 1pk (primary key not found or composite)

## Tests
By default, only Sqlite test is executed. To activate other tests configure test parameters at the top of each SQL server test file.
Tests are configured in the corresponding child BaseClass classes:
- MysqlTest
```
 Setup steps:
  Create user (eg. crudtest) with password,
  Create database 'crudtest', assign full rights to it for user above
  Edit MysqlTest.java and configure user, database and host (line 33)
```
- SqliteTest - configured to run by default
- SqlServerTest - follow mysql steps
- PostgresqlTest
- Oracle / Oracle XE: enter SQL console and create test user:
```sql
 SQL> connect system
  Enter password: <your setup password here>
  Connected.
 SQL> create user crudtest identified by crudtest;
  User created.
 SQL> grant dba to crudtest;
  Grant succeeded.

Running tests via docker is recommended (especially with PostgreSQL, which needs 'postgis' extension):
PostgreSQL docker image with postgis:
```
docker run --name postgres -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD='MySecretPasswordHere' -d mdillon/postgis
```
SQLServer 2017 docker image:
```
sudo docker run -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD='MySecretPasswordHere' -p 1433:1433 --name sql1 --hostname sql1 -d mcr.microsoft.com/mssql/server:2017-latest
```
Note: prior running tests, connect to each test server and create an empty database (for example `crudtest`)
Edit OracleTest.java and configure user, database/SID (Oracle Express can only use 'xe') and host (line 34)
```

in each test class, you should provide credentials, server and database name.
Database have to exist in order to perform the tests (it should be created by hand).
Table contents will be overwritten on each test run, so make sure you haven't provided a used DB name.
Run configured tests with

    mvn test

Tests have been performed in windows & Ubuntu 14.04 with following setups:

  - Ubuntu 14.04 Server with MySQL 5.5 and PostgreSQL 9.3
  - Ubuntu 14.04 Server with MySQL 5.6
  - Ubuntu 14.04 Server with MySQL 5.7
  - Windows 10x64 and SQL Server 2012 (build 2100)
  - Windows 10x64 and Oracle Expresss edition 11.2


## Building a executable JAR file
Make sure you've added proper credentials and SQL server type in CrudApiHandler class.
Also edit pom.xml and look below **SQL drivers** line, change the scope line that matches your driver:

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>${mysql.version}</version>
        <scope>test</scope>    <- remove or comment this line
    </dependency>

To compile everything in a single executable JAR file, run:

    mvn compile assembly:single

You can execute the JAR using:

    java -jar target/server.jar

You can see the api at work at http://localhost:8080/posts/1.

## Other dependencies

The project uses internally sources from 'fluentsql' project and 'Base64' class from AOSP,

 https://github.com/ivanceras/fluentsql/

 https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/util/Base64.java

Both of them licensed under Apache Public License v2.0.
## Binary files for Oracle JDBC connector
The project uses ojdbc6 drivers, which are compatible with Java 7. Xdb-1.0.jar is needed only for SDO extensions, you may skip it if GIS functionality is not needed.
Oracle JDBC files are not available in Maven. You should download them from Oracle site and install them into your local maven repository using the following commands:
```bash
 mvn install:install-file -Dfile=ojdbc6-12.1.0.1.jar -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=12.1.0.1 -Dpackaging=jar -DgeneratePom=true
 mvn install:install-file -Dfile=xdb-1.0.jar -DgroupId=oracle -DartifactId=xdb -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
```
