-- For each structure, iterate through all key/value pairs in the 'properties' column,
-- removing any pairs where the value is an empty JSON object.
UPDATE Structure
SET properties = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN json('{}')
        ELSE json_group_object(key, value)
    END
    FROM (
        SELECT
            json_each.key,
            json_each.value
        FROM json_each(properties)
        WHERE json_each.value != json('{}')
    )
);
