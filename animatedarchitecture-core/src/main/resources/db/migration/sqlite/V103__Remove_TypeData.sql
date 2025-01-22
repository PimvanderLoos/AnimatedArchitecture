
-- Move the following fields from the 'typeData' column to the 'properties' column:
-- 1) quarterCircles
-- 2) blocksToMove
-- Skip the following fields, since they are no longer used:
-- 1) hourArmSide
-- 2) northSouthAnimated
UPDATE Structure
SET properties = (
    WITH base_properties AS (
        SELECT properties as p
    ),
    with_quarter_circles AS (
        SELECT CASE
            WHEN json_extract(typeData, '$.quarterCircles') IS NOT NULL
            THEN json_set(p, '$.animatedarchitecture:quarter_circles.value',
                json_extract(typeData, '$.quarterCircles'))
            ELSE p
        END as p
        FROM base_properties
    ),
    with_blocks_to_move AS (
        SELECT CASE
            WHEN json_extract(typeData, '$.blocksToMove') IS NOT NULL
            THEN json_set(p, '$.animatedarchitecture:blocks_to_move.value',
                json_extract(typeData, '$.blocksToMove'))
            ELSE p
        END as p
        FROM with_quarter_circles
    )
    SELECT p FROM with_blocks_to_move
);


-- Remove the typeData column
ALTER TABLE Structure DROP COLUMN typeData;


-- Bump the version of the structure types to 10, since we have manually updated them.
UPDATE Structure
SET typeVersion = 10
WHERE type IN (
    'animatedarchitecture:bigdoor',
    'animatedarchitecture:clock',
    'animatedarchitecture:drawbridge',
    'animatedarchitecture:flag',
    'animatedarchitecture:garagedoor',
    'animatedarchitecture:portcullis',
    'animatedarchitecture:revolvingdoor',
    'animatedarchitecture:slidingdoor',
    'animatedarchitecture:windmill'
);
