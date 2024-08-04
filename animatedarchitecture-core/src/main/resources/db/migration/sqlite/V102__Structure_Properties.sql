-- Add the new properties column to the Structure table.
ALTER TABLE Structure ADD COLUMN properties TEXT NOT NULL DEFAULT '{}';

-- Convert the 3 rotation point columns to Property.ROTATION_POINT in the 'properties' column for the types that support this property.
UPDATE Structure
SET properties = CASE
    WHEN type NOT IN ('animatedarchitecture:portcullis', 'animatedarchitecture:slidingdoor')
    THEN json_insert(properties, '$.animatedarchitecture:rotation_point.value', json_object('x', rotationPointX, 'y', rotationPointY, 'z', rotationPointZ))
    ELSE properties
END;

-- Convert the 'open' bit in the 'bitflag' column to Property.OPEN_STATUS in the 'properties' column for the types that support this property.
UPDATE Structure
SET properties = CASE
    WHEN bitflag & 1 = 0
    THEN json_insert(properties, '$.animatedarchitecture:open_status.value', json('false'))
    WHEN bitflag & 1 = 1
    THEN json_insert(properties, '$.animatedarchitecture:open_status.value', json('true'))
    ELSE properties
END
WHERE type NOT IN ('animatedarchitecture:windmill', 'animatedarchitecture:flag', 'animatedarchitecture:revolvingdoor', 'animatedarchitecture:clock');

-- Unset the 'open' bit in the 'bitflag' column
UPDATE Structure
SET bitflag = bitflag & ~1;

-- Rename the 'openDirection' column to 'animationDirection', since some types no longer have an 'open' state.
ALTER TABLE Structure RENAME COLUMN openDirection TO animationDirection;

-- Rename the 'rotationPointChunkId' column to 'centerPointChunkId'
-- The value might not be correct, but it should be close enough.
-- It will be recalculated when the structure is updated.
ALTER TABLE Structure RENAME COLUMN rotationPointChunkId TO centerPointChunkId;

-- Remove the old rotation point columns
ALTER TABLE Structure DROP COLUMN rotationPointX;
ALTER TABLE Structure DROP COLUMN rotationPointY;
ALTER TABLE Structure DROP COLUMN rotationPointZ;
