-- ============================================================
-- Add oauth_provider column for OAuth2 authentication
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_users_oauth_provider ON users(oauth_provider);
