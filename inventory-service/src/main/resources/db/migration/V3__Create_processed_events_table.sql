-- Create processed_events table for idempotency tracking
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);
COMMENT ON TABLE processed_events IS 'Tracks processed Kafka events for idempotency';
