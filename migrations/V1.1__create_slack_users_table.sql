/* Slack User Table
 * User accounts detected by the slack client. Slack users may or may not have
 * an internal mapping to the gambit users table
 */
CREATE TABLE slack_users(
  slack_id        TEXT PRIMARY KEY, /* Unique, Slack-specific user ID */
  gambit_user_id  INT REFERENCES gambit_users(id), /* Mapping to a specific gambit user. Not unique because someone will be a smart ass */
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(), /* Time the row was created */
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() /* Last time the row was changed */
);

/* Update updated_at any time we update a slack_users */
CREATE TRIGGER set_timestamp
BEFORE UPDATE ON slack_users
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();
