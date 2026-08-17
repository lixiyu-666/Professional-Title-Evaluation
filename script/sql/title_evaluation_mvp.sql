-- Title evaluation MVP domain tables. Import after ry_vue_5.X.sql and ry_workflow.sql.
SET NAMES utf8mb4;

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
VALUES (9001,'2026 工程系列副高级正常晋升演示批次',2026,'2026-08-01 09:00:00','2026-12-31 18:00:00','MVP-1.0',1)
ON DUPLICATE KEY UPDATE
  name=VALUES(name), evaluation_year=VALUES(evaluation_year), open_at=VALUES(open_at),
  first_submit_deadline=VALUES(first_submit_deadline), rule_version=VALUES(rule_version), published=VALUES(published);

-- MVP demo roles. All demo users use password 666666.
INSERT INTO sys_role VALUES
  (9101,'000000','职称申请人','title_applicant',10,5,1,1,'0','0',103,1,SYSDATE(),NULL,NULL,'职称评审MVP演示角色'),
  (9102,'000000','部门人事审核人','title_dept_reviewer',20,3,1,1,'0','0',103,1,SYSDATE(),NULL,NULL,'职称评审MVP演示角色'),
  (9103,'000000','专业技术审核人','title_technical_reviewer',30,1,1,1,'0','0',103,1,SYSDATE(),NULL,NULL,'职称评审MVP演示角色'),
  (9104,'000000','部门领导审核人','title_dept_leader',40,3,1,1,'0','0',103,1,SYSDATE(),NULL,NULL,'职称评审MVP演示角色'),
  (9105,'000000','人事职称管理员','title_hr_admin',50,1,1,1,'0','0',103,1,SYSDATE(),NULL,NULL,'职称评审MVP演示角色')
ON DUPLICATE KEY UPDATE
  role_name=VALUES(role_name), role_key=VALUES(role_key), data_scope=VALUES(data_scope), status='0', del_flag='0';

INSERT INTO sys_user VALUES
  (9101,'000000',108,'title_applicant','申报人张某','sys_user','','','0',NULL,'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne','0','0','127.0.0.1',NULL,108,1,SYSDATE(),NULL,NULL,'职称评审MVP脱敏账号'),
  (9102,'000000',108,'title_dept_reviewer','部门人事李某','sys_user','','','0',NULL,'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne','0','0','127.0.0.1',NULL,108,1,SYSDATE(),NULL,NULL,'职称评审MVP脱敏账号'),
  (9103,'000000',103,'title_technical_reviewer','技术审核王某','sys_user','','','0',NULL,'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne','0','0','127.0.0.1',NULL,103,1,SYSDATE(),NULL,NULL,'职称评审MVP脱敏账号'),
  (9104,'000000',108,'title_dept_leader','部门领导赵某','sys_user','','','0',NULL,'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne','0','0','127.0.0.1',NULL,108,1,SYSDATE(),NULL,NULL,'职称评审MVP脱敏账号'),
  (9105,'000000',103,'title_hr_admin','职称管理员周某','sys_user','','','0',NULL,'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne','0','0','127.0.0.1',NULL,103,1,SYSDATE(),NULL,NULL,'职称评审MVP脱敏账号')
ON DUPLICATE KEY UPDATE
  dept_id=VALUES(dept_id), user_name=VALUES(user_name), nick_name=VALUES(nick_name), password=VALUES(password), status='0', del_flag='0';

INSERT IGNORE INTO sys_user_role (user_id,role_id) VALUES
  (9101,9101),(9102,9102),(9103,9103),(9104,9104),(9105,9105);

-- Repair the departments referenced by MVP demo users when an older seed was imported without SET NAMES.
UPDATE sys_dept SET dept_name='研发部门' WHERE dept_id=103;
UPDATE sys_dept SET dept_name='市场部门' WHERE dept_id IN (104,108);

INSERT INTO sys_menu VALUES
  (9100,'职称评审','0','6','title',NULL,'',1,0,'M','0','0','','form',103,1,SYSDATE(),NULL,NULL,'职称评审MVP权限目录'),
  (9101,'批次查询','9100','1','','','',1,0,'F','0','0','title:batch:list','#',103,1,SYSDATE(),NULL,NULL,''),
  (9102,'申报查询','9100','2','','','',1,0,'F','0','0','title:application:query','#',103,1,SYSDATE(),NULL,NULL,''),
  (9103,'申报创建','9100','3','','','',1,0,'F','0','0','title:application:create','#',103,1,SYSDATE(),NULL,NULL,''),
  (9104,'草稿保存','9100','4','','','',1,0,'F','0','0','title:application:update','#',103,1,SYSDATE(),NULL,NULL,''),
  (9105,'申报提交','9100','5','','','',1,0,'F','0','0','title:application:submit','#',103,1,SYSDATE(),NULL,NULL,''),
  (9106,'申报审核','9100','6','','','',1,0,'F','0','0','title:application:review','#',103,1,SYSDATE(),NULL,NULL,''),
  (9107,'审计查询','9100','7','','','',1,0,'F','0','0','title:audit:list','#',103,1,SYSDATE(),NULL,NULL,''),
  (9108,'政策查询','9100','8','','','',1,0,'F','0','0','title:policy:query','#',103,1,SYSDATE(),NULL,NULL,''),
  (9109,'政策维护','9100','9','','','',1,0,'F','0','0','title:policy:manage','#',103,1,SYSDATE(),NULL,NULL,'')
ON DUPLICATE KEY UPDATE
  menu_name=VALUES(menu_name), parent_id=VALUES(parent_id), order_num=VALUES(order_num), perms=VALUES(perms), status='0';

INSERT IGNORE INTO sys_role_menu (role_id,menu_id) VALUES
  (9101,9100),(9101,9101),(9101,9102),(9101,9103),(9101,9104),(9101,9105),(9101,9107),(9101,9108),
  (9102,9100),(9102,9101),(9102,9102),(9102,9106),(9102,9107),(9102,9108),
  (9103,9100),(9103,9101),(9103,9102),(9103,9106),(9103,9107),(9103,9108),
  (9104,9100),(9104,9101),(9104,9102),(9104,9106),(9104,9107),(9104,9108),
  (9105,9100),(9105,9101),(9105,9102),(9105,9106),(9105,9107),(9105,9108),(9105,9109);
