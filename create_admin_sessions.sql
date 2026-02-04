-- Admin multi-device sessions: allow admin to be logged in from up to 4 devices.
-- When 5th device logs in, the oldest session is removed.

CREATE TABLE IF NOT EXISTS admin_sessions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  device_key VARCHAR(64) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  last_used_at DATETIME(6) NOT NULL,
  INDEX idx_admin_sessions_user_id (user_id),
  INDEX idx_admin_sessions_last_used (last_used_at),
  CONSTRAINT fk_admin_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
