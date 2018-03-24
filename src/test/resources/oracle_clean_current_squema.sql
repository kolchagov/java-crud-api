purge recyclebin;

declare
    -- FK first, then unique, then PK
    cursor cursor_constraints is select table_name, constraint_name
                          from user_constraints
                         where constraint_type in ('P', 'R', 'U')
                        order by decode(constraint_type, 'R', 0, 'U', 1, 'P', 2, 3);
    cursor cursor_views is select view_name from user_views;
    cursor cursor_tables is select table_name from user_tables;
    cursor cursor_synonyms is select synonym_name from user_synonyms;
    cursor cursor_sequences is select sequence_name from user_sequences;
begin
    for current_val in cursor_constraints
    loop
        execute immediate 'alter table ' || current_val.table_name || ' drop constraint ' || current_val.constraint_name;
    end loop;
    for current_val in cursor_views
    loop
        execute immediate 'drop view ' || current_val.view_name;
    end loop;
    for current_val in cursor_tables
    loop
        execute immediate 'drop table ' || current_val.table_name || ' purge';
    end loop;
    for current_val in cursor_synonyms
    loop
        execute immediate 'drop synonym ' || current_val.synonym_name;
    end loop;
    for current_val in cursor_sequences
    loop
        execute immediate 'drop sequence ' || current_val.sequence_name;
    end loop;
end;