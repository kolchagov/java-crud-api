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
import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.ivanceras.fluent.sql.Breakdown;
import com.ivanceras.fluent.sql.SQL;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import net.sf.json.xml.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static com.ivanceras.fluent.sql.SQL.Statics.*;
import static eu.hadeco.crudapi.RequestHandler.Actions.*;

public class RequestHandler {

    private static final java.util.logging.Logger LOGR = java.util.logging.Logger.getLogger(RequestHandler.class.getName());
    private static final String ID_KEY = "!_id_!";
    private static final Gson gson;
    //Change this to true during development - turns on sql exceptions logging
    private static final boolean DEBUG_SQL = false;

    static {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
            @Override
            public JsonElement serialize(Double originalValue, Type typeOf, JsonSerializationContext context) {
                //fix Gson's numeric format output
                if (originalValue == originalValue.longValue()) {
                    return new JsonPrimitive(originalValue.longValue());
                }
                return new JsonPrimitive(originalValue);
            }

        });
        gson = gsonBuilder.serializeNulls().disableHtmlEscaping().create();
    }

    private final Connection link;
    private final Map<String, String[]> parameters;
    private final Map<String, String> satisfyAny;
    private final ApiConfig config;
    private final boolean isJsonContent;
    private final HttpServletRequest req;
    private final String table;
    private final List<String> includeTables;
    //typeMap contains SQL column types for all selected includeTables with fully-qualified names eg: posts.id->INT ...
    private final Map<String, String> typeMap;
    //inputMap contains SQL column types with short names for selected table only eg: id->INT, name:VARCHAR(45) ...
    private final Map<String, String> inputTypeMap;
    //used when include parameter's given
    //contains fully-qualified idColumns with collected id values eg: posts.id->[1,2,3], tags.post_id->[1,2] ...
    private final Map<String, Set<String>> columns;
    private final String idColumn, databaseName;
    private final Map<String, List<String>> orderMap;
    private final Map<String, TableMeta> tableMetaMap;
    private Actions action;
    private JsonElement root;
    private boolean withTransform;

    private RequestHandler(Connection link, HttpServletRequest req, ApiConfig apiConfig) throws SQLException, IOException, ClassNotFoundException {
        this.link = link;
        this.req = req;
        this.config = apiConfig;
        this.databaseName = link.getCatalog();
        final String pathInfo = req.getPathInfo();
        String[] request = pathInfo == null ? new String[]{""} : pathInfo.replaceAll("/$|^/", "").split("/");
        JsonParser jsonParser = new JsonParser();
        this.root = null;
        // retrieve the table and key from the path
        String tableName = request[0].replaceAll("[^a-zA-Z0-9_]+", "");
        if (tableName == null || tableName.isEmpty()) {
            throw new ClassNotFoundException("entity");
        }
        isJsonContent = "application/json".equals(req.getContentType());
        if (isJsonContent) {
            try {
                this.root = jsonParser.parse(req.getReader());
            } catch (JsonParseException e) {
                throw new ClassNotFoundException("input");
            }
        }
        //or form-urlencoded: this parse the fields
        parameters = new LinkedHashMap<>(req.getParameterMap());
        if (request.length > 1) {
            parameters.put(ID_KEY, request[1].split(","));
        }
        satisfyAny = parseSatisfy(tableName);
        orderMap = parseOrder(tableName);
        final String transform = req.getParameter("transform");
        withTransform = transform == null || "1".equals(transform);
        this.table = tableName;
        this.tableMetaMap = getTableMetaMap();
        if (!tableMetaMap.containsKey(tableName)) {
            throw new ClassNotFoundException("entity");
        }
        this.action = getAction(req.getMethod(), parameters.containsKey(ID_KEY));
        if (!config.tableAuthorizer(action, databaseName, tableName)) {
            throw new ClassNotFoundException("entity");
        }
        this.typeMap = getColumnTypesMap(tableName);
        this.includeTables = applyInclude();
        //add non-prefixed columns to process input
        this.inputTypeMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : typeMap.entrySet()) {
            if (entry.getKey().startsWith(String.format("%s.", tableName))) {
                inputTypeMap.put(entry.getKey().substring(tableName.length() + 1), entry.getValue());
            }
        }
        this.idColumn = getPrimaryKey(tableName);
        columns = parseTableColumns(req);
    }

    /**
     * Default servlet handler. Use it to handle servlet requests
     *
     * @param req servlet request
     * @param resp servlet response to handle
     * @param apiConfig preconfigured ApiConfig class
     * @throws IOException
     */
    public static void handle(HttpServletRequest req, HttpServletResponse resp, ApiConfig apiConfig) throws IOException {
        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");
        try (PrintWriter writer = resp.getWriter()) {
            try {
                try (Connection link = apiConfig.getConnection()) {
                    if (link == null) {
                        throw new IllegalStateException("Cannot establish DB connection. Try again later.");
                    }
                    RequestHandler handler = new RequestHandler(link, req, apiConfig);
                    resp.setContentType("application/json; charset=utf-8");
                    resp.setStatus(HttpServletResponse.SC_OK);
                    handler.handleRequest(writer);
                }
            } catch (NumberFormatException ex) {
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                writer.write(ex.getMessage());
            } catch (ClassNotFoundException ex) {
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writer.write(String.format("Not found (%s)", ex.getMessage()));
            } catch (IllegalArgumentException ex) {
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                final String message = ex.getMessage() == null ? "null" : ex.getMessage();
                writer.write(message);
            } catch (Exception e) {
                LOGR.log(Level.SEVERE, e.getMessage(), e);
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writer.println(String.format("Internal exception: '%s'", e.getMessage()));
                e.printStackTrace(writer);
            }
        }
    }

    private static Object fixNumeric(Object o) {
        Object result = o;
        if (o instanceof Double) {
            Double number = (Double) o;
            if (number.longValue() == number) {
                result = number.longValue();
            }
        }
        return result;
    }

    private Map<String, String> parseSatisfy(String table) {
        Map<String, String> satisfyAny = new HashMap<>();
        String satisfyParams = req.getParameter("satisfy");
        if (satisfyParams != null) {
            for (String satisfyParam : satisfyParams.split(",")) {
                if (!satisfyParam.contains(".")) {
                    if (satisfyParam.equals("any")) {
                        satisfyAny.put(table, "any");
                    }
                } else {
                    final String tableName = getTableName(satisfyParam);
                    if ("any".equals(satisfyParam.substring(tableName.length() + 1))) {
                        satisfyAny.put(tableName, "any");
                    }
                }
            }
        }
        return satisfyAny;
    }

    private Map<String, List<String>> parseOrder(String table) {
        if (table == null) {
            throw new IllegalStateException("Invalid table name");
        }
        Map<String, List<String>> orderMap = new HashMap<>();
        String[] orderParam;
        orderParam = req.getParameterValues("order[]");
        if (orderParam == null) {
            String orderString = req.getParameter("order");
            if (orderString != null) {
                orderParam = new String[]{orderString};
            }
        }
        if (orderParam != null) {
            for (String order : orderParam) {
                order = order.replace(",", " ");
                String tableName;
                if (!order.contains(".")) {
                    tableName = table;
                } else {
                    tableName = getTableName(order);
                }
                if (!orderMap.containsKey(tableName)) {
                    orderMap.put(tableName, new ArrayList<String>());
                }
                orderMap.get(tableName).add(order);
            }
        }
        return orderMap;
    }

    private Actions getAction(String method, boolean hasId) throws ClassNotFoundException {
        Actions action;
        switch (method) {
            case "GET":
                action = hasId ? READ : LIST;
                break;
            case "OPTIONS":
                action = HEADERS;
                break;
            case "POST":
                action = CREATE;
                break;
            case "PUT":
                action = UPDATE;
                break;
            case "DELETE":
                action = DELETE;
                break;
            case "PATCH":
                action = INCREMENT;
                break;
            default:
                throw new ClassNotFoundException("method");

        }
        return action;
    }

    private Map<String, String> getColumnTypesMap(String table) throws ClassNotFoundException {
        ResultSetMetaData rsMeta;
        Map<String, String> typeMap;
        try (ResultSet rs = link.createStatement().executeQuery(String.format("SELECT * from %s where 0=1", table))) {
            rsMeta = rs.getMetaData();
            typeMap = new LinkedTreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (int i = 0; i < rsMeta.getColumnCount(); i++) {
                String columnTypeName = rsMeta.getColumnTypeName(i + 1);
                if ("NULL".equals(columnTypeName)) {
                    //special case: sqlite view's count() columns
                    columnTypeName = "INT";
                }
                final String columnName = rsMeta.getColumnName(i + 1);
                typeMap.put(getFullColumnName(table, columnName), columnTypeName);
            }
        } catch (SQLException ex) {
            throw new ClassNotFoundException("entity");
        }
        return typeMap;
    }

    private List<String> applyInclude() throws ClassNotFoundException {
        final String included = req.getParameter("include");
        List<String> includedList = new ArrayList<>();
        if (included != null) {
            for (String includedTable : included.split(",")) {
                //uncomment the following line to enable case-insensitive match of included tables
                //includedTable = getRealTableName(includedTable);
                if (!tableMetaMap.containsKey(includedTable)) {
                    throw new ClassNotFoundException(String.format("entity: %s", includedTable));
                }
                if (!includedList.contains(includedTable)) {
                    includedList.add(includedTable);
                    final Map<String, String> columnTypesMap = getColumnTypesMap(includedTable);
                    typeMap.putAll(columnTypesMap);
                }
            }
        }
        return includedList;
    }

    /**
     * Retrieves real table name from DB (uncomment to allow case-insensitive
     * match of included tables)
     */
//    private String getRealTableName(String includedTable) {
//        if(config.isOracle()) {
//            includedTable = getNativeCaseName(includedTable);
//        } else {
//            try (Connection conn = config.getConnection()) {
//                final Statement stmt = conn.createStatement();
//                final ResultSet resultSet = stmt.executeQuery(String.format("SELECT * from %s where 0=1", includedTable));
//                final ResultSetMetaData metaData = resultSet.getMetaData();
//                if (metaData.getColumnCount() > 0) {
//                   final String realName = metaData.getTableName(1);
//                   if(!includedTable.equals(realName) && includedTable.equalsIgnoreCase(realName))
//                       includedTable = realName;
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//        return includedTable;
//    }
    private String getPrimaryKey(String table) throws SQLException {
        DatabaseMetaData metaData = link.getMetaData();
        String schemaPattern = getCurrentSchema();
        String primaryKey;
        try (ResultSet rs = metaData.getPrimaryKeys(null, schemaPattern, config.isOracle() ? table.toUpperCase() : table)) {
            primaryKey = null;
            while (rs.next()) {
                if (table.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
                    primaryKey = getFullColumnName(table, rs.getString("COLUMN_NAME"));
                    break;
                }
            }
        }
        return primaryKey;
    }

    private Map<String, Set<String>> parseTableColumns(HttpServletRequest request) throws ClassNotFoundException {
        final String columnsParam = request.getParameter("columns");
        Map<String, Set<String>> columnsMap = new HashMap<>();
        if (columnsParam != null) {
            final String[] columnsArray = columnsParam.split(",");
            for (String column : columnsArray) {
                if (column.contains(".")) {
                    final String tableName = getTableName(column);
                    String colName = column.substring(tableName.length() + 1);
                    if ("*".equals(colName)) {
                        putColumns(columnsMap, tableName, getColumnTypesMap(tableName).keySet());
                    } else {
                        if (!typeMap.containsKey(column)) {
                            throw new ClassNotFoundException("column");
                        }
                        putColumns(columnsMap, tableName, column);
                    }
                } else {
                    final String fqColumnName = getFullColumnName(this.table, column);
                    if (!typeMap.containsKey(fqColumnName)) {
                        throw new ClassNotFoundException("column");
                    }
                    putColumns(columnsMap, this.table, fqColumnName);
                }
            }
        } else {
            for (String column : typeMap.keySet()) {
                putColumns(columnsMap, getTableName(column), column);
            }
        }
        if (request.getParameter("exclude") != null) {
            String[] excludedColumns = request.getParameter("exclude").split(",");
            for (String excludedColumn : excludedColumns) {
                if (excludedColumn.contains(".")) {
                    final String tableName = getTableName(excludedColumn);
                    columnsMap.get(tableName).remove(excludedColumn);
                } else {
                    columnsMap.get(this.table).remove(getFullColumnName(this.table, excludedColumn));
                }
            }
        }
        //apply columnAuthorizer
        for (String table : columnsMap.keySet()) {
            for (Iterator<String> iterator = columnsMap.get(table).iterator(); iterator.hasNext();) {
                String column = iterator.next();
                final boolean isAuthorized
                        = config.columnAuthorizer(action, databaseName, table, column.substring(table.length() + 1));
                if (!isAuthorized) {
                    iterator.remove();
                }
            }
        }
        return columnsMap;
    }

    private String getTableName(String fqColumnName) {
        final int idx = fqColumnName.lastIndexOf(".");
        if (idx < 0) {
            throw new IllegalArgumentException("Invalid column name");
        }
        return fqColumnName.substring(0, idx);
    }

    private void putColumns(Map<String, Set<String>> columnsMap, String tableName, Collection<String> fqColumnList) {
        if (!columnsMap.containsKey(tableName)) {
            columnsMap.put(tableName, new LinkedHashSet<String>());
        }
        columnsMap.get(tableName).addAll(fqColumnList);
    }

    private void putColumns(Map<String, Set<String>> columnsMap, String tableName, String... fqColumnNames) {
        putColumns(columnsMap, tableName, Arrays.asList(fqColumnNames));
    }

    private String getNativeCaseName(String column) {
        if (config.isOracle()) {
            //Oracle metadata is upper-case by default, convert it to lowercase
            column = column.toLowerCase();
        }
        return column;
    }

    private String getFullColumnName(String table, String column) {
        return String.format("%s.%s", table, getNativeCaseName(column));
    }

    private boolean isCreateAction() {
        return action == CREATE;
    }

    /**
     * Filtered columns list
     *
     * @param table selected table
     * @return filtered list
     */
    private List<String> getColumnsList(String table) {
        List<String> columns = new ArrayList<>();
        final Set<String> columnSet = this.columns.get(table);
        if (columnSet != null) {
            columns.addAll(columnSet);
        }
        final String prefix = String.format("%s.", table);
        if (columns.isEmpty()) {
            for (String column : typeMap.keySet()) {
                if (column.startsWith(prefix)) {
                    columns.add(column);
                }
            }
        }
        return columns;
    }

    private boolean applyFilters(SQL sql, String table, String... ids) {
        if (isCreateAction()) {
            return false;
        }
        String[] filters = getFilters(table);
        boolean hasFilters = parseFilters(sql, filters, satisfyAny.containsKey(table));
        if (ids != null && ids.length > 0) {
            if (idColumn == null) {
                throw new IllegalStateException(String.format("Primary key not found for table: %s", table));
            }
            if (hasFilters) {
                sql.AND(idColumn).IN(ids);
            } else {
                sql.WHERE(idColumn).IN(ids);
                hasFilters = true;
            }
        }

        return hasFilters;
    }

    private String[] getFilters(String table) {
        List<String> result = new ArrayList<>();
        final String prefix = String.format("%s.", table);
        String[] filters = parameters.get("filter");
        if (filters == null) {
            filters = parameters.get("filter[]");
        }
        if (filters != null) {
            for (String filter : filters) {
                if (filter.contains(".")) {
                    if (filter.toLowerCase().startsWith(prefix.toLowerCase())) {
                        result.add(filter);
                    }
                } else if (this.table.equalsIgnoreCase(table)) {
                    result.add(String.format("%s.%s", table, filter));
                }
            }
        }
        //apply record filters
        final String[] extraFilters = config.recordFilter(action, databaseName, table);
        if (extraFilters != null) {
            for (String filter : extraFilters) {
                if (filter.toLowerCase().startsWith(prefix.toLowerCase())) {
                    result.add(filter);
                } else if (!filter.contains(".")) {
                    result.add(String.format("%s.%s", table, filter));
                }
            }
        }
        return result.isEmpty() ? null : result.toArray(new String[result.size()]);
    }

    private boolean isReadOnly() {
        return action == LIST || action == READ;
    }

    private Map applyPaging(SQL sql) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("sql", sql);
        String pageParam = req.getParameter("page");
        if (pageParam != null) {

            final List<String> orders = applyOrder(sql, table);
            if (orders == null) {
                throw new IllegalStateException("'page' without 'order' is not possible!");
            }

            Integer resultCount = getResultCount(link, orders.get(0), table);
            map.put("resultCount", resultCount);

            String[] split = pageParam.split(",");
            int pages = Integer.valueOf(split[0]);
            pages = pages > 0 ? pages - 1 : 0;

            int limit = split.length == 2 ? Integer.valueOf(split[1]) : 20;
            int offset = pages * limit;
            final boolean msSQL = config.isMsSQL();
            final boolean oracle = config.isOracle();
            if (!msSQL && !oracle) {
                sql.LIMIT(limit);
            }
            if (!oracle) {
                sql.OFFSET(offset);
            }
            if (msSQL) {
                sql.keyword("ROWS");
                sql.keyword(String.format("FETCH NEXT %d ROWS ONLY", limit));
            }

            if (oracle) {
                SQL sqlRowNum = SELECT("rownum as rownumid", "t.*")
                        .FROM(sql).keyword("t");

                SQL sqlFilter = SELECT("*").FROM(sqlRowNum);
                int begin = offset + 1;
                int end = limit + offset;
                sqlFilter.keyword(String.format("WHERE rownumid BETWEEN %d AND %d", begin, end));
                map.put("sql", sqlFilter);
            }
        }
        return map;
    }

    private List<String> applyOrder(SQL sql, String table) {
        final List<String> orders = orderMap.get(table);
        if (orders != null) {
            sql.ORDER_BY(orders.toArray(new String[orders.size()]));
        }
        return orders;
    }

    private List<String> filterTableColumns(String table, List<String> columns) {
        List<String> result = new ArrayList<>();
        for (String column : columns) {
            if (column.startsWith(table)) {
                result.add(column.substring(table.length() + 1));
            }
        }
        return result;
    }

    private LinkedList<Map<String, Object>> processJsonObjectResults(
            ResultSet rs, PrintWriter writer, Collection<String> columns, TableMeta tableMeta,
            Map<String, Set<Object>> collectIds, Map.Entry<String, String> relation) throws SQLException {
        for (String key : tableMeta.getRelatedTableKeys()) {
            if (!collectIds.containsKey(key)) {
                collectIds.put(key, new HashSet<>());
            }
        }
        final LinkedList<Map<String, Object>> records = new LinkedList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String colName : columns) {
                Object value = parseValue(colName, rs);
                row.put(colName.substring(tableMeta.getName().length() + 1), value);
                if (collectIds.containsKey(colName)) {
                    collectIds.get(colName).add(value);
                }
            }
            records.add(row);
        }

        for (TableMeta subTableMeta : tableMeta.getReferencedTables().values()) {
            String table = subTableMeta.getName();
            List<String> columnsList = getColumnsList(table);
            Map.Entry<String, String> subRelation = subTableMeta.getRelation();
            final Set<Object> ids;
            if (subRelation != null && collectIds.containsKey(subRelation.getValue())) {
                ids = collectIds.get(subRelation.getValue());
                final String key = subRelation.getKey();
                if (!columnsList.contains(key)) {
                    columnsList.add(key);
                }
            } else {
                ids = new HashSet<>();
            }
            final SQL sql = SELECT(columnsList.toArray(new String[columnsList.size()])).FROM(table);
            if (!ids.isEmpty() && subRelation != null) {
                sql.WHERE(subRelation.getKey()).IN(ids.toArray());
            }
            applyOrder(sql, table);
            try (ResultSet resultSet = prepareStatement(sql).executeQuery()) {
                LinkedList<Map<String, Object>> relatedRecords = processJsonObjectResults(
                        resultSet, writer, columnsList, subTableMeta, collectIds, subRelation);
                if (subRelation == null) {
                    writer.write(","); //list non-related table objects sequentially
                } else {
                    for (Map<String, Object> record : records) {
                        LinkedList<Map<String, Object>> filtered = new LinkedList<>();
                        for (Map<String, Object> relatedRecord : relatedRecords) {
                            String left = getShortId(subRelation, tableMeta.getName());
                            final String right = getShortId(subRelation, table);
                            if (Objects.equals(record.get(left), relatedRecord.get(right))) {
                                filtered.add(relatedRecord);
                            }
                        }
                        record.put(table, filtered);
                    }
                }
            }
        }
        if (relation == null) {
            writer.write(String.format("\"%s\":", tableMeta.getName()));
            writer.write(gson.toJson(records));
        }
        return records;
    }

    private String getShortId(Map.Entry<String, String> relation, String tableName) {
        String id = null;
        tableName = String.format("%s.", tableName);
        if (relation.getKey().startsWith(tableName)) {
            id = relation.getKey().substring(tableName.length());
        } else if (relation.getValue().startsWith(tableName)) {
            id = relation.getValue().substring(tableName.length());
        }
        return id;
    }

    private void streamJsonObjectResults(ResultSet rs, PrintWriter writer, TableMeta tableMeta)
            throws SQLException, ClassNotFoundException {
        String separator = "";
        Collection<String> columns = getColumnsList(tableMeta.getName());
        while (rs.next()) {
            writer.write(separator);
            Map<String, Object> values = new LinkedHashMap<>();
            for (String colName : columns) {
                Object value = parseValue(colName, rs);
                values.put(colName.substring(tableMeta.getName().length() + 1), value);
            }
            final String json = gson.toJson(values);
            writer.write(json);
            separator = ",";
        }
        final String[] ids = parameters.get(ID_KEY);
        if (separator.isEmpty() && ids != null && ids.length > 0) {
            throw new ClassNotFoundException("object");
        }
    }

    /**
     * Check for unsigned types
     *
     * @param type SQL type
     * @return true if unsigned
     */
    private boolean isUnsignedType(String type) {
        return type.toUpperCase().endsWith(" UNSIGNED");
    }

    private Object parseValue(String fullName, ResultSet rs) throws SQLException {
        Object value;
        final String BINARY = "_binary_", TIME = "_time_";
        String colName = fullName.substring(fullName.lastIndexOf(".") + 1);
        try {
            rs.findColumn(colName);
        } catch (SQLException ignored) {
            colName = fullName;
        }
        //POSTGRES JDBC driver returns lower-cased types
        String type = typeMap.get(fullName);
        type = type.toUpperCase();
        if (isBinaryColumn(fullName, typeMap)) {
            type = BINARY;
        } else if (isTimeColumn(fullName, typeMap)) {
            type = TIME;
        } else if (isUnsignedType(type)) {
            type = type.split(" ")[0];
        }

        switch (type) {
            case "GEOMETRY":         //this type is included in tests as ST_AsText(value)
                value = rs.getString(colName.replace(".", "_"));
                break;
            case "MDSYS.SDO_GEOMETRY":
                //todo implement Oracle spatial object conversion to text
                //NOTE: not possible for Oracle XE - https://stackoverflow.com/questions/44832223/oracle-converting-sdo-geometry-to-wkt
                value = rs.getString(colName);
                break;
            case "JSON":
            case "CLOB":
                final String strValue = rs.getString(colName);
                try {
                    value = gson.fromJson(strValue, JsonElement.class);
                } catch (JsonParseException ignored) {
                    value = strValue;       //non-JSON content returned as String
                }
                break;
            case "XML":
                Document doc;
                final String xmlContents = rs.getNString(colName);
                try {
                    doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xmlContents.getBytes()));
                    value = parseXmlObject(doc.getDocumentElement());
                } catch (SAXException | IOException | ParserConfigurationException e) {
                    value = gson.fromJson(xmlContents, JsonElement.class);
                }
                break;
            case "CHARACTER":
            case "TEXT":
            case "VARCHAR":
            case "VARCHAR2":
            case "NVARCHAR":
            case "LONGVARCHAR":
            case "NUMBER":          //Oracle's mixed type
            case "DECIMAL":         //this type is included as string in tests
            case "NUMERIC":         //this type is included as string in tests
                value = rs.getString(colName);
                if (config.isOracle() && "NUMBER".equals(type) && value != null && ((String) value).indexOf('.') < 0) {
                    //special case: Oracle's integer values
                    value = rs.getLong(colName);
                }
                break;
//            case "NUMERIC":         //this type is included as string in tests
//                value = new BigDecimal(rs.getString(colName));
//                break;
            case "BIT":
                value = rs.getBoolean(colName);
                break;
            case "TINYINT":
            case "SMALLINT":
            case "INTEGER":
            case "INT":
            case "INT4":
            case "INT2":
            case "SERIAL":
                value = rs.getInt(colName);
                break;
            case "BIGINT":
            case "INT8":
                value = rs.getLong(colName);
                break;
            case "REAL":
                if (rs.getString(colName).contains(".")) {
                    value = rs.getFloat(colName);
                } else {
                    value = rs.getInt(colName);
                }
                break;
            case "FLOAT":
            case "DOUBLE":
            case "DOUBLE PRECISION":
//            case "NUMBER":
                value = rs.getDouble(colName);
                break;
            case BINARY:
                final byte[] bytes = rs.getBytes(colName);
                value = bytes != null ? Base64.encodeToString(bytes, Base64.DEFAULT).trim() : null;
                break;
            case TIME:
                final String ts = rs.getString(colName);
                if (ts != null && ts.length() > 19) {
                    value = ts.substring(0, 19);      //treat all date types as String
                } else {
                    value = ts;
                }
                break;
            default:
                throw new SQLException("Type not implemented: " + type);
        }
        return value;
    }

    private JsonElement parseXmlObject(Node node) {
        JsonElement result = JsonNull.INSTANCE;
        Node type = node.getAttributes().getNamedItem("type");
        final NodeList childNodes = node.getChildNodes();
        switch (type.getNodeValue()) {
            case "object":
                final JsonObject jsonObject = new JsonObject();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    final Node item = childNodes.item(i);
                    final JsonElement jsonElement = parseXmlObject(item);
                    jsonObject.add(item.getNodeName(), jsonElement);
                }
                result = jsonObject;
                break;
            case "array":
                final JsonArray jsonElements = new JsonArray();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    final Node item = childNodes.item(i);
                    final JsonElement jsonElement = parseXmlObject(item);
                    jsonElements.add(jsonElement);
                }
                result = jsonElements;
                break;
            case "boolean":
                result = new JsonPrimitive("true".equalsIgnoreCase(node.getTextContent()));
                break;
            case "number":
                String nodeValue = node.getTextContent();
                if (nodeValue != null) {
                    final Double aDouble = Double.valueOf(nodeValue);
                    result = aDouble == aDouble.longValue()
                            ? new JsonPrimitive(aDouble.longValue()) : new JsonPrimitive(aDouble);
                }
                break;
            case "string":
                nodeValue = node.getTextContent();
                if (nodeValue != null) {
                    result = new JsonPrimitive(nodeValue);
                }
                break;
            case "null":
                break;
            default:
                throw new UnsupportedOperationException("Type not implemented: " + type);
        }
        return result;
    }

    private PreparedStatement prepareStatement(SQL sql) throws SQLException {
        Breakdown breakdown = sql.build();
        PreparedStatement statement;
        if (isReadOnly() && config.isPSQL()) {
            statement = link.prepareStatement(breakdown.getSql());
        } else if (config.isOracle()) {
            String[] pks = {};
            if (this.idColumn != null) {
                pks = new String[]{this.idColumn.split("\\.")[1]};
            }
            statement = link.prepareStatement(breakdown.getSql(), pks);
        } else {
            statement = link.prepareStatement(breakdown.getSql(), Statement.RETURN_GENERATED_KEYS);
        }
        List<Object> convertedList = new ArrayList<>();
        int i = 0;
        for (Object param : breakdown.getParameters()) {
            Object converted = convertToObject(param);
            if (converted != null) {
                if ((converted instanceof Map) || (converted instanceof List)) {
                    if (config.isMsSQL()) {
                        final SQLXML sqlxml = getSqlxmlObject(converted);
                        statement.setObject(++i, sqlxml);
                    } else {
                        statement.setObject(++i, gson.toJson(converted));
                    }
                } else {
                    statement.setObject(++i, converted);
                }
            } else {
                statement.setNull(++i, Types.BINARY); //ms sql caprice again...
            }
            convertedList.add(converted);
        }
        if (DEBUG_SQL) {
            System.out.println(String.format("%s with params: %s", breakdown.getSql(), gson.toJson(convertedList)));
        }
        return statement;
    }

    private SQLXML getSqlxmlObject(Object converted) throws SQLException {
        final SQLXML sqlxml = link.createSQLXML();
        final JSON json = JSONSerializer.toJSON(converted);
        String xml;
        if (json.isEmpty()) {
            xml = "<root type=\"object\"/>";
        } else {
            final XMLSerializer xmlSerializer = new XMLSerializer();
            xmlSerializer.setRootName("root");
            xml = xmlSerializer.write(json);
            //remove first line <?xml version="1.0" encoding="utf-8"?>\r\n<root>...</root>
            xml = xml.replaceFirst(".*\r\n<root>(.*)", "<root type=\"object\">$1").trim();
            xml = xml.replace("class=\"", "type=\"");
        }
        sqlxml.setString(xml);
        return sqlxml;
    }

    private Integer getResultCount(Connection link, String order, String table) throws SQLException {
        String id = order.contains(" ") ? order.substring(0, order.lastIndexOf(' ')) : order;

        SQL sql = SELECT(String.format("count(%s)", id)).FROM(table);
        applyFilters(sql, table, parameters.get(ID_KEY));
        final Breakdown breakdown = sql.build();
        final String query = breakdown.getSql();
        final PreparedStatement statement = link.prepareStatement(query);
        Integer count = null;
        for (int i = 0; i < breakdown.getParameters().length; i++) {
            Object o = convertToObject(breakdown.getParameters()[i]);
            statement.setObject(i + 1, o);
        }
        try (ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        }
        return count;
    }

    private Object convertToObject(Object param) {
        Object result = param;
        if (param != null && param instanceof String) {
            String value = (String) param;
            if (value.matches("[+-]?\\d\\d*")) {
                result = Long.parseLong(value);
            } else if (value.matches("(?:true|false)")) {
                result = Boolean.parseBoolean(value);
            }
        }
        return result;
    }

    private boolean parseFilters(SQL sql, String[] filters, boolean satisfyAny) {
        boolean isParsed = filters != null && filters.length > 0;
        if (isParsed) {
            boolean isNextCondition = false;
            for (String filter : filters) {
                String[] splitCommands = filter.split(",", 3);
                if (splitCommands.length < 3) {
                    throw new IllegalArgumentException("Invalid filter: " + filter);
                }
                String column = splitCommands[0];
                if (!typeMap.containsKey(column)) {
                    throw new IllegalArgumentException("Invalid column in filter: " + column);
                }
                String parameter = splitCommands[1]; // compare parameter
                final boolean isGeometryColumn = isGeometryObject(column);
                int stExpected = 1;
                if (!isGeometryColumn) {
                    addNextCondition(sql, column, satisfyAny, isNextCondition);
                } else if (parameter.startsWith("n")) {
                    stExpected = 0; //check negative condition
                    parameter = parameter.substring(1).toLowerCase();
                }
                String value = splitCommands[2];
                String spatialFilter = null;
                switch (parameter) {
                    case "eq":
                        sql.EQUAL_TO(value);
                        break;
                    case "neq":
                        sql.NOT_EQUAL_TO(value);
                        break;
                    case "sw":
                        sql.keyword("LIKE ").VALUE(String.format("%s%%", value));
                        break;
                    case "cs":
                        sql.keyword("LIKE ").VALUE(String.format("%%%s%%", value));
                        break;
                    case "ew":
                        sql.keyword("LIKE ").VALUE(String.format("%%%s", value));
                        break;
                    case "lt":
                    case "ngt":
                        sql.LESS_THAN(value);
                        break;
                    case "le":
                        sql.keyword("<=").VALUE(value);
                        break;
                    case "gt":
                        sql.GREATER_THAN(value);
                        break;
                    case "ge":
                    case "nlt":
                        sql.GREATER_THAN_OR_EQUAL(value);
                        break;
                    case "bt":
                        filterBetween(sql, filter, column, value, false);
                        break;
                    case "nbt":
                        filterBetween(sql, filter, column, value, true);
                        break;
                    case "in":
                        sql.IN(value.split(","));
                        break;
                    case "nin":
                        sql.NOT_IN(value.split(","));
                        break;
                    case "is":
                        if ("null".equalsIgnoreCase(value)) {
                            sql.IS_NULL();
                        }
                        break;
                    case "nis":
                        if ("null".equalsIgnoreCase(value)) {
                            sql.IS_NOT_NULL();
                        }
                        break;
                    case "swi":
                    case "sco":
                    case "scr":
                    case "sdi":
                    case "seq":
                    case "sin":
                    case "sov":
                    case "sto":
                        spatialFilter = getGeometryFromText(parameter, column, value);
                        break;
                    default:
                        throw new IllegalStateException("Filter not implemented: " + filter);
                }
                if (isGeometryColumn) {
                    addNextCondition(sql, spatialFilter, satisfyAny, isNextCondition);
                    sql.EQUAL(stExpected);
                }
                isNextCondition = true;
            }
        }
        return isParsed;
    }

    private String getGeometryFromText(String command, String column, String value) {
        final boolean msSQL = config.isMsSQL();
        final boolean oracle = config.isOracle();
        String filter;
        switch (command) {
            case "swi":
                filter = msSQL ? "%s.STWithin(%s)" : "ST_Within(%s, %s)";
                break;
            case "sco":
                filter = msSQL ? "%s.STContains(%s)" : "ST_Contains(%s, %s)";
                break;
            case "scr":
                filter = msSQL ? "%s.STCrosses(%s)" : "ST_Crosses(%s, %s)";
                break;
            case "sdi":
                filter = msSQL ? "%s.STDisjoint(%s)" : "ST_Disjoint(%s, %s)";
                break;
            case "seq":
                filter = msSQL ? "%s.STEquals(%s)" : "ST_Equals(%s, %s)";
                break;
            case "sin":
                filter = msSQL ? "%s.STIntersects(%s)" : "ST_Intersects(%s, %s)";
                break;
            case "sov":
                filter = msSQL ? "%s.STOverlaps(%s)" : "ST_Overlaps(%s, %s)";
                break;
            case "sto":
                filter = msSQL ? "%s.STTouches(%s)" : "ST_Touches(%s, %s)";
                break;
            default:
                throw new IllegalArgumentException("Command not implemented: " + command);
        }

        String valueFormat = "ST_GeomFromText('%s')";
        if (msSQL) {
            valueFormat = "geometry::STGeomFromText('%s',0)";
        }
        if (oracle) {
            valueFormat = "SDO_GEOMETRY('%s')";
        }

        final String valueCmd = String.format(valueFormat, value);
        return String.format(filter, column, valueCmd);
    }

    private void addNextCondition(SQL sql, String column, boolean satisfyAny, boolean isNextCondition) {
        if (isNextCondition) {
            if (satisfyAny) {
                sql.OR(column);
            } else {
                sql.AND(column);
            }
        } else {
            sql.WHERE(column);
        }
    }

    private void filterBetween(SQL sql, String filter, String column, String value, boolean isInverse) {
        if (!value.contains(",")) {
            throw new IllegalStateException(String.format("Exepecting two values for between: %s", filter));
        }
        String[] betweenValues = value.split(",", 2);
        if (isInverse) {
            sql.LESS_THAN(betweenValues[0]).OR(column).GREATER_THAN(betweenValues[1]);
        } else {
            sql.GREATER_THAN_OR_EQUAL(betweenValues[0]).AND(column).keyword("<=").VALUE(betweenValues[1]);
        }
    }

    private void handleRequest(PrintWriter writer) throws SQLException, ClassNotFoundException {
        final List<String> columnsList = getColumnsList(table);
        String[] ids = parameters.get(ID_KEY);
        Map<String, Object> input = new LinkedHashMap<>();
        List<SQL> batch = new ArrayList<>();
        SQL sql = null;
        if (!parameters.isEmpty() && !isJsonContent) {
//            process x-www-form-urlencoded data
            for (String key : parameters.keySet()) {
                if (!key.equals(ID_KEY) && (inputTypeMap.containsKey(key) || key.endsWith("__is_null"))) {
                    input.put(key, convertToObject(parameters.get(key)[0]));
                }
            }
        } else if (root != null && root.isJsonObject()) {
//            process json data - single object
            input = gson.fromJson(root, input.getClass());
        } else if (isJsonContent && !isReadOnly()) {
//           process multiple row data
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = gson.fromJson(root, LinkedList.class);
            final Iterator<Map<String, Object>> it = list.iterator();
            final Iterator<String> idsIterator = ids != null ? Arrays.asList(ids).iterator() : null;
            if (!it.hasNext()) {
                throw new ClassNotFoundException("input");
            }
            if (ids != null && ids.length != list.size()) {
                throw new ClassNotFoundException("subject");
            }
            while (it.hasNext()) {
                action = config.before(action, databaseName, table, ids, input);
                sql = prepareSql(columnsList);
                final Map<String, Object> next = it.next();
                if (next.isEmpty()) {
                    throw new ClassNotFoundException("input");
                }
                parseInput(sql, next, inputTypeMap);
                final String id = idsIterator != null && idsIterator.hasNext() ? idsIterator.next() : null;
                applyFilters(sql, table, id);
                batch.add(sql);
            }
        }
        action = config.before(action, databaseName, table, ids, input);
        if (isReadOnly()) {
            doReadonlyActions(writer, columnsList, ids);
        } else {
            if (batch.isEmpty()) {
                try {
                    if (ids == null) {
                        ids = new String[]{null};
                    }
                    for (String id : ids) {
                        sql = prepareSql(columnsList);
                        parseInput(sql, input, inputTypeMap);
                        final boolean hasFilters = applyFilters(sql, table, id);
                        applyTenancyFilter(hasFilters, sql);
                        batch.add(sql);
                    }
                } catch (NumberFormatException e) {
                    if (action != CREATE) {
                        throw e;
                    }
                }
            }
            if (action == HEADERS) {
                writer.write("[\"Access-Control-Allow-Headers: Content-Type, X-XSRF-TOKEN\",\"Access-Control-Allow-Methods: OPTIONS, GET, PUT, POST, DELETE, PATCH\",\"Access-Control-Allow-Credentials: true\",\"Access-Control-Max-Age: 1728000\"]");
            } else {
                if (batch.isEmpty()) {
                    batch.add(sql);
                }

                link.setAutoCommit(false);
                try {
                    List<Object> results = new ArrayList<>();
                    for (SQL nextSql : batch) {
                        try (PreparedStatement statement = prepareStatement(nextSql)) {
                            statement.execute();
                            if (isCreateAction()) {
                                results.addAll(getGeneratedKeys(statement));
                            } else {
                                int updateCount = statement.getUpdateCount();
                                results.add(updateCount);
                            }
                        }
                    }
                    link.commit();
                    writer.write(results.size() == 1 ? gson.toJson(results.get(0)) : gson.toJson(results));
                } catch (SQLException ex) {
                    if (DEBUG_SQL) {
                        LOGR.log(Level.INFO, ex.getMessage(), ex);
                    }
                    link.rollback();
                    if (isCreateAction()) {
                        LOGR.log(Level.INFO, "CREATE operation failed", ex);
                        writer.write("null");
                    } else {
                        throw ex;
                    }
                } finally {
                    link.setAutoCommit(true);
                }
            }
        }
    }

    private void doReadonlyActions(PrintWriter writer, List<String> columnsList, String... ids) throws SQLException, ClassNotFoundException {
        SQL sql = prepareSql(columnsList);
        Map<String, Set<Object>> collectIds = new HashMap<>();
        final boolean hasIncludedTables = !includeTables.isEmpty();
        findTableRelations(tableMetaMap, collectIds);
        //add primary + foreign keys from related tables
        for (String fqColumnName : collectIds.keySet()) {
            final String tableName = getTableName(fqColumnName);
            putColumns(columns, tableName, tableMetaMap.get(tableName).getPrimaryKey());
            putColumns(columns, tableName, fqColumnName);
        }
        for (String t : includeTables) {
            //add unrelated tables to the output
            final TableMeta mt = tableMetaMap.get(t);
            tableMetaMap.get(table).addReferencedTable(mt);
        }
        boolean hasFilters = applyFilters(sql, table, ids);
        Integer resultCount = null;
        if (action == LIST) {
            Map map = applyPaging(sql);
            resultCount = (Integer) map.get("resultCount");
            sql = (SQL) map.get("sql");
        }
        applyTenancyFilter(hasFilters, sql);
        final List<String> columnList = getColumnsList(table);
        if (parameters.containsKey(ID_KEY)) {
            String preamble = parameters.get(ID_KEY).length > 1 ? "[" : "";
            try (ResultSet rs = prepareStatement(sql).executeQuery()) {
                writer.write(preamble);
                streamJsonObjectResults(rs, writer, tableMetaMap.get(table));
            }
            if (!preamble.isEmpty()) {
                writer.write("]");
            }
        } else if (withTransform) {
            try (ResultSet rs = prepareStatement(sql).executeQuery()) {
                writer.write("{");
                if (hasIncludedTables) {
                    processJsonObjectResults(rs, writer, columnList, tableMetaMap.get(table), collectIds, null);
                } else {
                    writer.write(String.format("\"%s\":", table));
                    writer.write("[");
                    streamJsonObjectResults(rs, writer, tableMetaMap.get(table));
                    writer.write("]");
                }
            }
            writer.write("}");
        } else {
            TableMeta topTable = tableMetaMap.get(table);
            try (ResultSet rs = prepareStatement(sql).executeQuery()) {
                writer.write("{");
                streamRecords(writer, columnsList, collectIds, rs, resultCount, topTable);
            }
            streamRelatedTables(writer, collectIds, topTable);
            writer.write("}");
        }
    }

    /**
     * Searches for relations between tables and removes related from include
     * list. Feeds collectIds with exported primary keys
     *
     * @param tableMeta table columns metadata map
     * @param collectIds map with sets of collected ids
     * @throws SQLException
     */
    private void findTableRelations(Map<String, TableMeta> tableMeta, Map<String, Set<Object>> collectIds)
            throws ClassNotFoundException {
        if (!includeTables.isEmpty()) {
            for (Iterator<String> iterator = includeTables.iterator(); iterator.hasNext();) {
                String includeTable = iterator.next();
                TableMeta cTable = tableMeta.get(includeTable);
                TableMeta topTable = tableMeta.get(table);
                if (topTable.hasReferenceTo(includeTable) || cTable.hasReferenceTo(table)) {
                    topTable.addReferencedTable(cTable);
                    collectIds.put(cTable.getReferedToKey(), new HashSet<>());
                    iterator.remove();
                } else {
                    //find intermediate tables
                    List<String> allTables = new ArrayList<>();
                    allTables.add(table);
                    allTables.addAll(tableMeta.get(table).getReferencedTables().keySet());
                    allTables.addAll(includeTables);
                    for (int i = 0, allTablesSize = allTables.size(); i < allTablesSize; i++) {
                        String leftTable = allTables.get(i);
                        for (int j = i + 1; j < allTablesSize; j++) {
                            String rightTable = allTables.get(j);
                            for (TableMeta ft : tableMeta.values()) {
                                if (ft.isIntermediateFor(leftTable, rightTable)) {
                                    final String imTable = ft.getName();
                                    typeMap.putAll(getColumnTypesMap(imTable));
                                    tableMeta.get(leftTable).addReferencedTable(ft);
                                    if (leftTable.equals(includeTable) || rightTable.equals(includeTable)) {
                                        iterator.remove();
                                    }
                                    ft.addReferencedTable(tableMeta.get(includeTable));
                                    for (String idKey : ft.getForeignKeys()) {
                                        collectIds.put(idKey, new HashSet<>());
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void streamRelatedTables(PrintWriter writer, Map<String, Set<Object>> collectIds, TableMeta tableMeta) throws SQLException {
        for (TableMeta mt : tableMeta.getReferencedTables().values()) {
            String table = mt.getName();
            List<String> columnsList = getColumnsList(table);
            final SQL sql = SELECT(columnsList.toArray(new String[columnsList.size()])).FROM(table);
            boolean hasFilters = applyFilters(sql, table);
            Map.Entry<String, String> relation = mt.getRelation();
            if (relation != null && collectIds.containsKey(relation.getValue())) {
                final String collectedIds = relation.getValue();
                if (!collectedIds.isEmpty()) {
                    if (hasFilters) {
                        sql.AND(relation.getKey()).IN(collectIds.get(collectedIds).toArray());
                    } else {
                        sql.WHERE(relation.getKey()).IN(collectIds.get(collectedIds).toArray());
                    }
                }
            }
            applyOrder(sql, table);
            try (ResultSet resultSet = prepareStatement(sql).executeQuery()) {
                writer.write(",");
                streamRecords(writer, columnsList, collectIds, resultSet, null, mt);
            }
            streamRelatedTables(writer, collectIds, mt);
        }
    }

    private void streamRecords(PrintWriter writer, List<String> columnsList, Map<String, Set<Object>> collectIds,
            ResultSet rs, Integer resultCount, TableMeta tableMeta) throws SQLException {
        String relations = tableMeta.getRelationsJson();
        writer.write(String.format("\"%s\":{%s\"columns\":%s,\"records\":[", tableMeta.getName(),
                relations,
                gson.toJson(filterTableColumns(tableMeta.getName(), columnsList))));

        List<Object> row = new ArrayList<>();
        while (rs.next()) {
            if (!row.isEmpty()) {
                writer.write(",");
                row.clear();
            }
            for (String column : columnsList) {
                final Object value = parseValue(column, rs);
                row.add(value);
                if (collectIds.containsKey(column)) {
                    collectIds.get(column).add(value);
                }
            }
            writer.write(gson.toJson(row));
        }
        writer.write("]");
        if (resultCount != null) {
            writer.write(String.format(",\"results\":%d", resultCount));
        }
        writer.write("}");
    }

    private String getCurrentSchema() throws SQLException {
        String schemaPattern = null;
        if (config.isOracle()) {
            schemaPattern = this.link.getMetaData().getUserName();
        }
        if (config.isMsSQL()) {
            schemaPattern = "dbo";
        }
        return schemaPattern;
    }

    /**
     * Returns map that contains metadata about every table's primary and
     * foreign keys
     *
     * @return
     * @throws SQLException
     */
    private Map<String, TableMeta> getTableMetaMap() throws SQLException {
        Map<String, TableMeta> tablesMap = ApiConfig.getCachedTableMeta();
        if (tablesMap != null) {
            return tablesMap;
        }
        tablesMap = new LinkedHashMap<>();

        DatabaseMetaData md = link.getMetaData();
        String schemaPattern = getCurrentSchema();

        try (ResultSet tables = md.getTables(databaseName, schemaPattern,
                "%", new String[]{"TABLE", "VIEW"})) {
            while (tables.next()) {
                final String jdbcTableName = tables.getString("TABLE_NAME");
                final String table = getNativeCaseName(jdbcTableName);
                if (!tablesMap.containsKey(table)) {
                    tablesMap.put(table, new TableMeta(table));
                }
                try (ResultSet rs = md.getImportedKeys(null, schemaPattern, jdbcTableName)) {
                    while (rs.next()) {
                        final String pkTable = getNativeCaseName(rs.getString("PKTABLE_NAME"));
                        final String pkColumn = getNativeCaseName(rs.getString("PKCOLUMN_NAME"));
                        final String fkTable = getNativeCaseName(rs.getString("FKTABLE_NAME"));
                        final String fkColumn = getNativeCaseName(rs.getString("FKCOLUMN_NAME"));
                        tablesMap.get(table).addForeignKeys(pkTable, pkColumn, fkTable, fkColumn);
                    }
                }
                try (ResultSet rs = md.getPrimaryKeys(null, schemaPattern, jdbcTableName)) {
                    if (rs.next()) {
                        final String column = rs.getString("COLUMN_NAME");
                        tablesMap.get(table).setPrimaryKey(getNativeCaseName(column));
                    }
                }
            }
        }
        ApiConfig.setCachedTableMeta(tablesMap);
        return tablesMap;
    }

    private void applyTenancyFilter(boolean hasFilters, SQL sql) {
        //apply tenancy filter
        for (String column : inputTypeMap.keySet()) {
            final Object value = config.tenancyFunction(action, databaseName, table, column);
            if (value != null) {
                if (hasFilters) {
                    sql.AND(column).EQUAL(value);
                } else {
                    sql.WHERE(column).EQUAL(value);
                    hasFilters = true;
                }
            }
        }
    }

    private SQL prepareSql(List<String> columnsList) {
        SQL sql;
        switch (action) {
            case READ:
            case LIST:
                sql = SELECT(getColumnsArray(columnsList)).FROM(table);
                break;
            case UPDATE:
                sql = UPDATE(table);
                break;
            case CREATE:
                sql = INSERT().INTO(table);
                break;
            case DELETE:
                sql = DELETE().FROM(table);
                break;
            case HEADERS:
                sql = WITH();
                break;
            case INCREMENT:
                sql = UPDATE(table);
                break;
            default:
                throw new UnsupportedOperationException("Action not implemented: " + action);
        }
        return sql;
    }

    private String[] getColumnsArray(List<String> columnsList) {
        String[] result = new String[columnsList.size()];
        for (int i = 0; i < result.length; i++) {
            String column = columnsList.get(i);
            if (isGeometryObject(column)) {
                //
                if (config.isMsSQL()) {
                    result[i] = String.format("%s.STAsText() as %s", column, column.replace(".", "_"));
                } else if (config.isOracle()) {
                    result[i] = column;
                } else {
                    result[i] = String.format("ST_asText(%s) as %s", column, column.replace(".", "_"));
                }

            } else {
                result[i] = column;
            }
        }
        return result;
    }

    private List<Object> getGeneratedKeys(PreparedStatement statement) throws SQLException {
        List<Object> result = new ArrayList<>();
        try (ResultSet rs = statement.getGeneratedKeys()) {
            while (rs.next()) {
                result.add(rs.getInt(1));
            }
        }
        return result;
    }

    private void parseInput(SQL sql, Map<String, Object> input, Map<String, String> typeMap) {
        if (!(isCreateAction() || action == UPDATE || action == INCREMENT)) {
            return;
        }
        //apply tenancy function
        for (String column : input.keySet()) {
            final Object tenancyValue = config.tenancyFunction(action, databaseName, table, column);
            if (tenancyValue != null) {
                input.put(column, tenancyValue);
            }
        }
        final List<String> columns = new ArrayList<>();
        columns.addAll(input.keySet());
        if (isCreateAction()) {
            sql.openParen().FIELD(columns.toArray(new String[columns.size()])).closeParen();
        } else {
            sql.SET();
        }
        Map<String, Object> values = processRow(input, typeMap);
        if (isCreateAction()) {
            sql.VALUES(values.values().toArray(new Object[values.size()]));
        } else {
            final Iterator<Map.Entry<String, Object>> it = values.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, Object> entry = it.next();
                if (action == INCREMENT) {
                    sql.FIELD(entry.getKey()).EQUAL().FIELD(entry.getKey()).keyword("+").VALUE(entry.getValue());
                } else if (isGeometryObject(entry.getKey())) {
                    if (config.isMsSQL()) {
                        sql.FIELD(entry.getKey()).EQUAL().keyword("geometry::STGeomFromText")
                                .openParen().VALUE(entry.getValue()).comma().VALUE(0).closeParen();
                    } else if (config.isOracle()) {
                        sql.FIELD(entry.getKey()).EQUAL().keyword("SDO_GEOMETRY").openParen().VALUE(entry.getValue()).closeParen();
                    } else {
                        sql.FIELD(entry.getKey()).EQUAL().keyword("ST_GeomFromText").openParen().VALUE(entry.getValue()).closeParen();
                    }
                } else {
                    sql.FIELD(entry.getKey()).EQUAL(entry.getValue());
                }
                if (it.hasNext()) {
                    sql.comma();
                }
            }
        }
    }

    private boolean isTimeColumn(String key, Map<String, String> typeMap) {
        boolean isTime = false;
        switch (typeMap.get(key).toUpperCase()) {
            case "DATE":
            case "TIME":
            case "DATETIME":
            case "DATETIME2":
            case "TIMESTAMP":
                isTime = true;
                break;
        }
        return isTime;
    }

    private LinkedHashMap<String, Object> processRow(Map<String, Object> input, Map<String, String> typeMap) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (String key : input.keySet()) {
            Object value;
            //special case
            if (!typeMap.containsKey(key) && key.endsWith("__is_null")) {
                key = key.replace("__is_null", "");
                value = null;
            } else {
                value = validateInput(input, typeMap, key);
            }
            value = config.inputSanitizer(action, databaseName, table, key, typeMap.get(key),
                    value, req);
            final Object validatorResult = config.inputValidator(
                    action, databaseName, table, key, typeMap.get(key), value, req);
            if (!Boolean.TRUE.equals(validatorResult)) {
                throw new NumberFormatException((String) validatorResult);
            }
            //special case: Postgres
            if (isTimeColumn(key, typeMap) && (config.isPSQL() || config.isOracle())) {
                final String strValue = (String) value;
                try {
                    if (strValue.matches("\\d{4}-\\d\\d-\\d\\d")) {
                        value = Date.valueOf(strValue);
                    } else {
                        value = Timestamp.valueOf(strValue);
                    }
                } catch (IllegalArgumentException ignored) {
                    throw new IllegalArgumentException("date argument is invalid");
                }
            }
            values.put(key, value);
        }
        return values;
    }

    private Object validateInput(Map<String, Object> input, Map<String, String> typeMap, String key) {
        Object result = input.get(key);
        final boolean isString = result instanceof String;
        if (result != null) {
            if (isBinaryColumn(key, typeMap)) {
                result = Base64.decode(result.toString(), Base64.DEFAULT);
            } else if (isNumericColumn(key, typeMap)) {
                if (isString) {
                    if (!((String) result).matches("[+-]\\d\\d*[.]?\\d*")) {
                        throw new NumberFormatException(String.format("{\"%s\":\"must be numeric\"}", key));
                    }
                } else {
                    result = fixNumeric(result);
                }
            }
        }
        return result;
    }

    private boolean isBinaryColumn(String key, Map<String, String> typeMap) {
        boolean isBinary = false;
        final String dbColumn = typeMap.get(key);
        if (dbColumn == null) {
            throw new IllegalArgumentException(String.format("Invalid input column: %s", key));
        }
        final String valueString = dbColumn.toUpperCase();
        switch (valueString) {
            case "BINARY":
            case "DATA":
            case "BYTEA":
            case "VARBINARY":
            case "LONGVARBINARY":
            case "BLOB":
            case "LONGBLOB":
                isBinary = true;
        }
        return isBinary;
    }

    private boolean isNumericColumn(String key, Map<String, String> typeMap) {
        boolean isNumeric = false;
        final String valueString = typeMap.get(key).toUpperCase();
        switch (valueString) {
            case "NUMERIC":
            case "DECIMAL":
            case "BIT":
            case "TINYINT":
            case "SMALLINT":
            case "INTEGER":
            case "INT":
            case "INT2":
            case "INT4":
            case "SERIAL":
            case "INT8":
            case "BIGINT":
            case "REAL":
            case "FLOAT":
            case "DOUBLE PRECISION":
            case "NUMBER":
                isNumeric = true;
        }
        return isNumeric;
    }

    private boolean isGeometryObject(String key) {
        boolean isGeometry = false;
        key = key.contains(".") ? key : String.format("%s.%s", table, key);
        final String valueString = typeMap.get(key).toUpperCase();
        switch (valueString) {
            case "GEOMETRY":
            case "MDSYS.SDO_GEOMETRY":
                isGeometry = true;
        }
        return isGeometry;
    }

    public enum Actions {
        LIST, CREATE, READ, UPDATE, DELETE, INCREMENT, HEADERS
    }

}
