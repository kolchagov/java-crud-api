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

import java.util.*;

class TableMeta {
    private final String table;
    private final Map<String, TableMeta> referencedTables;
    private final Set<String> foreignKeys;
    private final Map<String, String> foreignToPrimaryKeys;
    private final Map<String, String> referencedTablePrimaryKeys;
    private final Map<String, String> primaryToForeignKeys;
    private String primaryKey, referedFromKey, referedToKey;

    public TableMeta(String table) {
        this.table = table;
        foreignKeys = new HashSet<>();
        referencedTablePrimaryKeys = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        foreignToPrimaryKeys = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        primaryToForeignKeys = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        referencedTables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    public String getName() {
        return table;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = String.format("%s.%s", table, primaryKey);
    }

    public String getReferedToKey() {
        return referedToKey;
    }

    public Set<String> getRelatedTableKeys() {
        HashSet<String> result = new HashSet<>();
        for (TableMeta tableMeta : referencedTables.values()) {
            result.add(tableMeta.getReferedToKey());
        }
        return result;
    }

    public Set<String> getForeignKeys() {
        return foreignKeys;
    }

    public void clearReferencedTables() {
        referencedTables.clear();
        referedFromKey = null;
    }

    public void addReferencedTable(TableMeta otherTable) {
        otherTable.setReferencesFrom(this);
        referencedTables.put(otherTable.table, otherTable);
    }

    private void setReferencesFrom(TableMeta otherTable) {
        if (hasReferenceTo(otherTable.table)) {
            //other table has primary key, referenced by this table's foreign key
            referedToKey = getReferencedPrimaryKey(otherTable.table);
            referedFromKey = primaryToForeignKeys.get(referedToKey);
        } else {
            referedFromKey = otherTable.getReferencedPrimaryKey(table);
            referedToKey = otherTable.primaryToForeignKeys.get(referedFromKey);
        }
    }

    public Map<String, TableMeta> getReferencedTables() {
        return referencedTables;
    }

    public void addForeignKeys(String pkTable, String pk, String fkTable, String fk) {
        String primaryKey = String.format("%s.%s", pkTable, pk);
        String foreignKey = String.format("%s.%s", fkTable, fk);
        foreignToPrimaryKeys.put(foreignKey, primaryKey);
        primaryToForeignKeys.put(primaryKey, foreignKey);
        foreignKeys.add(foreignKey);
        referencedTablePrimaryKeys.put(pkTable, primaryKey);
    }

    private String getReferencedPrimaryKey(String table) {
        return referencedTablePrimaryKeys.get(table);
    }

    public boolean hasReferenceTo(String table) {
        return referencedTablePrimaryKeys.containsKey(table);
    }

    public boolean isIntermediateFor(String leftTable, String rightTable) {
        return referencedTablePrimaryKeys.containsKey(leftTable) && referencedTablePrimaryKeys.containsKey(rightTable);
    }

    @Override
    public String toString() {
        final String tableName = String.format("%s: %s", table, foreignToPrimaryKeys);
        return String.format("%s, relation: %s", tableName, getRelation());
    }

    /**
     * Rturns an entry with key pair: thisTable.foreignKey, referringTable.primaryKey
     *
     * @return key pair Entry or null
     */
    public Map.Entry<String, String> getRelation() {
        return referedFromKey == null ? null : new AbstractMap.SimpleEntry<>(referedFromKey, referedToKey);
    }

    public String getRelationsJson() {
        Map.Entry<String, String> relation = getRelation();
        if (relation == null) {
            return "";
        } else {
            final String fromColumn = relation.getKey().substring(table.length() + 1);
            return String.format("\"relations\":{\"%s\":\"%s\"},", fromColumn, relation.getValue());
        }
    }

}
