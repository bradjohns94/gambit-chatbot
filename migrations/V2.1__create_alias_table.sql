/* Aliases Table
 * Mappings of karma values that may have other names mapped to them
 */
CREATE TABLE aliases(
  primary_name    TEXT PRIMARY KEY REFERENCES karma(name), /* Original name of the aliased value */
  aliased_name    INT UNIQUE NOT NULL, /* The name that maps to the primary value */
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(), /* Time the row was created */
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() /* Last time the row was changed */
);

/* Update updated_at any time we update a slack_users */
CREATE TRIGGER set_timestamp
BEFORE UPDATE ON aliases
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();
