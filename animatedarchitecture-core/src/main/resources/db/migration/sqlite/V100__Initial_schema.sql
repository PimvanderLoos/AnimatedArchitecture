CREATE TABLE Player (
    id          INTEGER    PRIMARY KEY AUTOINCREMENT,
    playerUUID  TEXT       NOT NULL,
    playerName  TEXT       NOT NULL,
    sizeLimit   INTEGER    NOT NULL,
    countLimit  INTEGER    NOT NULL,
    permissions INTEGER    NOT NULL,
    unique(playerUUID)
);

CREATE TABLE Structure (
    id                    INTEGER    PRIMARY KEY AUTOINCREMENT,
    name                  TEXT       NOT NULL,
    world                 TEXT       NOT NULL,
    xMin                  INTEGER    NOT NULL,
    yMin                  INTEGER    NOT NULL,
    zMin                  INTEGER    NOT NULL,
    xMax                  INTEGER    NOT NULL,
    yMax                  INTEGER    NOT NULL,
    zMax                  INTEGER    NOT NULL,
    rotationPointX        INTEGER    NOT NULL,
    rotationPointY        INTEGER    NOT NULL,
    rotationPointZ        INTEGER    NOT NULL,
    rotationPointChunkId  INTEGER    NOT NULL,
    powerBlockX           INTEGER    NOT NULL,
    powerBlockY           INTEGER    NOT NULL,
    powerBlockZ           INTEGER    NOT NULL,
    powerBlockChunkId     INTEGER    NOT NULL,
    openDirection         INTEGER    NOT NULL,
    type                  TEXT       NOT NULL,
    typeVersion           INTEGER    NOT NULL,
    typeData              TEXT       NOT NULL,
    bitflag               INTEGER    NOT NULL
);

CREATE TABLE StructureOwnerPlayer (
    id           INTEGER    PRIMARY KEY AUTOINCREMENT,
    permission   INTEGER    NOT NULL,
    playerID     REFERENCES Player(id)    ON UPDATE CASCADE ON DELETE CASCADE,
    structureUID REFERENCES Structure(id) ON UPDATE CASCADE ON DELETE CASCADE,
    unique (playerID, structureUID)
);

INSERT OR IGNORE INTO SQLITE_SEQUENCE (name, seq) VALUES ('Player', 10);

INSERT OR IGNORE INTO SQLITE_SEQUENCE (name, seq) VALUES ('StructureOwnerPlayer', 10);
