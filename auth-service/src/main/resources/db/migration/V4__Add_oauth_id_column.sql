-- ============================================================
-- Add oauth_id column for OAuth2 authentication
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_users_oauth_id ON users(oauth_id);
