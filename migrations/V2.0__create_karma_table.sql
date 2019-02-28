/* Karma Table
 * Reference to how many arbitrary internet points something has been given
 */
CREATE TABLE karma(
  name            TEXT PRIMARY KEY, /* Unique, the acutal reference name for the karma */
  value           INT NOT NULL DEFAULT 0, /* Value, positive or negative, the entity is worth in internet points */
  linked_user     INT REFERENCES gambit_users(id), /* User the karma is assigned to if any */
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(), /* Time the row was created */
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() /* Last time the row was changed */
);

/* Update updated_at any time we update a slack_users */
CREATE TRIGGER set_timestamp
BEFORE UPDATE ON karma
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();
