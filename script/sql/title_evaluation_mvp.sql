-- Title evaluation MVP domain tables. Import after ry_vue_5.X.sql and ry_workflow.sql.
CREATE TABLE IF NOT EXISTS title_batch (
  id BIGINT PRIMARY KEY, name VARCHAR(128) NOT NULL, evaluation_year INT NOT NULL,
  title_series VARCHAR(64) NOT NULL DEFAULT '工程系列', title_level VARCHAR(32) NOT NULL DEFAULT '副高级',
  application_type VARCHAR(32) NOT NULL DEFAULT '正常晋升', open_at DATETIME NOT NULL, first_submit_deadline DATETIME NOT NULL,
  default_correction_hours INT NOT NULL DEFAULT 72, rule_version VARCHAR(64) NOT NULL, published TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS title_application (
  id BIGINT PRIMARY KEY, batch_id BIGINT NOT NULL, applicant_user_id BIGINT NOT NULL, status VARCHAR(48) NOT NULL,
  current_version INT NOT NULL DEFAULT 0, form_json JSON NOT NULL, correction_deadline DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_title_application_batch_user (batch_id, applicant_user_id)
);
CREATE TABLE IF NOT EXISTS title_application_version (
  id BIGINT PRIMARY KEY, application_id BIGINT NOT NULL, version_no INT NOT NULL, form_snapshot JSON NOT NULL,
  material_snapshot JSON NOT NULL, precheck_snapshot JSON NOT NULL, submitted_at DATETIME NOT NULL,
  UNIQUE KEY uk_title_application_version (application_id, version_no)
);
CREATE TABLE IF NOT EXISTS title_review_record (
  id BIGINT PRIMARY KEY, application_id BIGINT NOT NULL, version_no INT NOT NULL, reviewer_user_id BIGINT NOT NULL,
  review_node VARCHAR(64) NOT NULL, action VARCHAR(32) NOT NULL, reason TEXT NULL, policy_basis TEXT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS title_audit_event (
  id BIGINT PRIMARY KEY, application_id BIGINT NULL, actor_user_id BIGINT NULL, actor_role VARCHAR(64) NULL,
  event_type VARCHAR(64) NOT NULL, before_status VARCHAR(48) NULL, after_status VARCHAR(48) NULL, version_no INT NULL,
  reason VARCHAR(1000) NULL, occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS title_policy_document (
  id BIGINT PRIMARY KEY, name VARCHAR(255) NOT NULL, version VARCHAR(64) NOT NULL, publisher VARCHAR(255) NOT NULL,
  effective_at DATE NULL, expires_at DATE NULL, information_level VARCHAR(32) NOT NULL, source_url VARCHAR(500) NULL, status VARCHAR(32) NOT NULL
);
CREATE TABLE IF NOT EXISTS title_policy_chunk (
  id BIGINT PRIMARY KEY, document_id BIGINT NOT NULL, locator VARCHAR(255) NOT NULL, content TEXT NOT NULL,
  applicable_year INT NULL, title_series VARCHAR(64) NULL, title_level VARCHAR(32) NULL
);
INSERT INTO title_batch (id,name,evaluation_year,open_at,first_submit_deadline,rule_version,published)
VALUES (9001,'2026 工程系列副高级正常晋升演示批次',2026,'2026-08-01 09:00:00','2026-12-31 18:00:00','MVP-1.0',1);
