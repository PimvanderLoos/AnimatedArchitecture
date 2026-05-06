CREATE TABLE PluginSession (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid             TEXT    NOT NULL UNIQUE,
    startedAt        INTEGER NOT NULL,
    endedAt          INTEGER,
    status           TEXT    NOT NULL,
    endReason        TEXT,
    pluginVersion    TEXT    NOT NULL,
    serverVersion    TEXT    NOT NULL,
    minecraftVersion TEXT    NOT NULL,
    serverSoftware   TEXT    NOT NULL
);

CREATE TABLE AnimationRun (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid                TEXT    NOT NULL UNIQUE,
    sessionId           INTEGER NOT NULL REFERENCES PluginSession(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    structureUid        INTEGER NOT NULL,
    actionType          TEXT    NOT NULL,
    animationType       TEXT    NOT NULL,
    startedAt           INTEGER NOT NULL,
    endedAt             INTEGER,
    status              TEXT    NOT NULL,
    expectedAnimatedBlockCount INTEGER,
    recoveredBlockCount INTEGER NOT NULL DEFAULT 0,
    lastRecoveredAt     INTEGER,
    recoveryCompletedAt INTEGER,
    diagnosticMessage   TEXT
);

CREATE INDEX idx_pluginsession_status_endedat ON PluginSession(status, endedAt);
CREATE INDEX idx_animationrun_sessionid ON AnimationRun(sessionId);
CREATE INDEX idx_animationrun_status_endedat ON AnimationRun(status, endedAt);
