-- Repair PostgreSQL SERIAL / IDENTITY sequences after importing rows with explicit IDs.
--
-- Usage:
--   psql -h <host> -U <user> -d <database> -f repair-owned-sequences.sql
--
-- Run this once for each migrated database. The script scans the current database for
-- sequences owned by table columns, compares each sequence's next value with MAX(column)+1,
-- and only advances sequences that are behind. It never moves a sequence backwards.
--
-- Requirements:
--   - PostgreSQL
--   - Run as the table/sequence owner or a superuser

DO $$
DECLARE
    rec record;
    max_column_value numeric;
    seq_last_value bigint;
    seq_is_called boolean;
    current_next_value bigint;
    target_next_value bigint;
    repaired_count integer := 0;
BEGIN
    FOR rec IN
        SELECT
            table_ns.nspname AS table_schema,
            table_class.relname AS table_name,
            column_attr.attname AS column_name,
            sequence_ns.nspname AS sequence_schema,
            sequence_class.relname AS sequence_name,
            format('%I.%I', sequence_ns.nspname, sequence_class.relname) AS sequence_regclass,
            sequence_meta.seqmin AS sequence_min
        FROM pg_class sequence_class
        JOIN pg_namespace sequence_ns
            ON sequence_ns.oid = sequence_class.relnamespace
        JOIN pg_sequence sequence_meta
            ON sequence_meta.seqrelid = sequence_class.oid
        JOIN pg_depend dependency
            ON dependency.objid = sequence_class.oid
        JOIN pg_class table_class
            ON table_class.oid = dependency.refobjid
        JOIN pg_namespace table_ns
            ON table_ns.oid = table_class.relnamespace
        JOIN pg_attribute column_attr
            ON column_attr.attrelid = table_class.oid
            AND column_attr.attnum = dependency.refobjsubid
        WHERE sequence_class.relkind = 'S'
          AND sequence_meta.seqincrement > 0
          AND dependency.deptype IN ('a', 'i')
          AND table_class.relkind IN ('r', 'p')
          AND table_ns.nspname NOT IN ('pg_catalog', 'information_schema')
          AND column_attr.attnum > 0
          AND NOT column_attr.attisdropped
        ORDER BY table_ns.nspname, table_class.relname, column_attr.attname
    LOOP
        EXECUTE format(
            'SELECT COALESCE(MAX(%I), 0) FROM %I.%I',
            rec.column_name,
            rec.table_schema,
            rec.table_name
        )
        INTO max_column_value;

        EXECUTE format(
            'SELECT last_value, is_called FROM %I.%I',
            rec.sequence_schema,
            rec.sequence_name
        )
        INTO seq_last_value, seq_is_called;

        current_next_value := CASE
            WHEN seq_is_called THEN seq_last_value + 1
            ELSE seq_last_value
        END;
        target_next_value := GREATEST(
            COALESCE(max_column_value, 0)::bigint + 1,
            current_next_value,
            rec.sequence_min
        );

        IF target_next_value > current_next_value THEN
            EXECUTE format(
                'SELECT setval(%L::regclass, %s, false)',
                rec.sequence_regclass,
                target_next_value
            );
            repaired_count := repaired_count + 1;
            RAISE NOTICE
                'REPAIRED %.% column %, sequence %, current next %, target next %, table max %',
                rec.table_schema,
                rec.table_name,
                rec.column_name,
                rec.sequence_regclass,
                current_next_value,
                target_next_value,
                max_column_value;
        ELSE
            RAISE NOTICE
                'OK %.% column %, sequence %, current next %, table max %',
                rec.table_schema,
                rec.table_name,
                rec.column_name,
                rec.sequence_regclass,
                current_next_value,
                max_column_value;
        END IF;
    END LOOP;

    RAISE NOTICE 'Sequence repair finished for database %, repaired_count=%',
        current_database(),
        repaired_count;
END $$;
