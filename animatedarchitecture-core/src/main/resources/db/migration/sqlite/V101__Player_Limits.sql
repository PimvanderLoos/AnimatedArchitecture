ALTER TABLE Player ADD COLUMN limitStructureSize INTEGER DEFAULT NULL;

ALTER TABLE Player ADD COLUMN limitStructureCount INTEGER DEFAULT NULL;

ALTER TABLE Player ADD COLUMN limitPowerBlockDistance INTEGER DEFAULT NULL;

ALTER TABLE Player ADD COLUMN limitBlocksToMove INTEGER DEFAULT NULL;

UPDATE Player
SET limitStructureSize =
    CASE WHEN sizeLimit = -1
        THEN NULL
        ELSE sizeLimit
    END;

UPDATE Player
SET limitStructureCount =
    CASE WHEN countLimit = -1
        THEN NULL
        ELSE countLimit
    END;

ALTER TABLE Player DROP COLUMN sizeLimit;

ALTER TABLE Player DROP COLUMN countLimit;
