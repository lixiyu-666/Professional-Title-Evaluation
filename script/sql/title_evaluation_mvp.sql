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

INSERT INTO title_policy_document
  (id,name,version,publisher,effective_at,expires_at,information_level,source_url,status)
VALUES
  (9201,'2026年度工程系列副高级职称申报演示指南','MVP-1.0','人事职称管理部门','2026-01-01',NULL,'公开',NULL,'EFFECTIVE')
ON DUPLICATE KEY UPDATE
  name=VALUES(name), version=VALUES(version), publisher=VALUES(publisher), effective_at=VALUES(effective_at),
  information_level=VALUES(information_level), status=VALUES(status);

INSERT INTO title_policy_chunk
  (id,document_id,locator,content,applicable_year,title_series,title_level)
VALUES
  (920101,9201,'第一章 申报范围','2026年度工程系列副高级职称正常晋升申报，仅面向本批次适用范围内人员。',2026,'工程系列','副高级'),
  (920102,9201,'第二章 材料要求','申报人应按五个步骤完整填写基本信息、学历资历与经历、业绩成果、推荐与条件对照，并上传要求的证明材料。',2026,'工程系列','副高级'),
  (920103,9201,'第三章 审核流程','申报提交后依次经过部门人事初审、专业技术审核、部门领导审核和人事职称管理员终审。',2026,'工程系列','副高级'),
  (920104,9201,'第四章 补正要求','审核退回后，申报人应在补正截止时间前完成修改并重新提交；重新提交生成新版本并从部门人事初审开始。',2026,'工程系列','副高级')
ON DUPLICATE KEY UPDATE
  document_id=VALUES(document_id), locator=VALUES(locator), content=VALUES(content), applicable_year=VALUES(applicable_year),
  title_series=VALUES(title_series), title_level=VALUES(title_level);

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
  (9101,'000000',108,'title_applicant','申报人张某','sys_user','','','0',NULL,'$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1',NULL,108,1,SYSDATE(),NULL,NULL,'职称评审MVP脱敏账号'),
  (9102,'000000',108,'title_dept_reviewer','部门人事李某','sys_user','','','0',NULL,'$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1',NULL,108,1,SYSDATE(),NULL,NULL,'职称评审MVP脱敏账号'),
  (9103,'000000',103,'title_technical_reviewer','技术审核王某','sys_user','','','0',NULL,'$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1',NULL,103,1,SYSDATE(),NULL,NULL,'职称评审MVP脱敏账号'),
  (9104,'000000',108,'title_dept_leader','部门领导赵某','sys_user','','','0',NULL,'$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1',NULL,108,1,SYSDATE(),NULL,NULL,'职称评审MVP脱敏账号'),
  (9105,'000000',103,'title_hr_admin','职称管理员周某','sys_user','','','0',NULL,'$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1',NULL,103,1,SYSDATE(),NULL,NULL,'职称评审MVP脱敏账号')
ON DUPLICATE KEY UPDATE
  dept_id=VALUES(dept_id), user_name=VALUES(user_name), nick_name=VALUES(nick_name), password=VALUES(password), status='0', del_flag='0';

INSERT IGNORE INTO sys_user_role (user_id,role_id) VALUES
  (9101,9101),(9102,9102),(9103,9103),(9104,9104),(9105,9105);

-- Repair the departments referenced by MVP demo users when an older seed was imported without SET NAMES.
UPDATE sys_dept SET dept_name='研发部门' WHERE dept_id=103;
UPDATE sys_dept SET dept_name='市场部门' WHERE dept_id IN (104,108);

INSERT INTO sys_menu VALUES
  (9100,'职称评审','0','6','title',NULL,'',1,0,'M','0','0','','form',103,1,SYSDATE(),NULL,NULL,'职称评审MVP权限目录'),
  (9101,'职称首页','9100','1','dashboard','title/dashboard/index','',1,0,'C','0','0','title:dashboard:view','dashboard',103,1,SYSDATE(),NULL,NULL,'职称评审角色首页'),
  (9102,'我的申报','9100','2','application','title/application/index','',1,0,'C','0','0','title:application:query','form',103,1,SYSDATE(),NULL,NULL,'申请人申报入口'),
  (9103,'审核工作台','9100','3','review','title/review/index','',1,0,'C','0','0','title:application:review','todo',103,1,SYSDATE(),NULL,NULL,'四级统一审核工作台'),
  (9104,'政策助手','9100','4','policy','title/policy/index','',1,0,'C','0','0','title:policy:query','chat-dot-round',103,1,SYSDATE(),NULL,NULL,'政策检索与出处'),
  (9105,'批次查询','9101','1','','','',1,0,'F','0','0','title:batch:list','#',103,1,SYSDATE(),NULL,NULL,''),
  (9106,'申报创建','9102','1','','','',1,0,'F','0','0','title:application:create','#',103,1,SYSDATE(),NULL,NULL,''),
  (9107,'草稿保存','9102','2','','','',1,0,'F','0','0','title:application:update','#',103,1,SYSDATE(),NULL,NULL,''),
  (9108,'申报提交','9102','3','','','',1,0,'F','0','0','title:application:submit','#',103,1,SYSDATE(),NULL,NULL,''),
  (9109,'审计查询','9100','9','','','',1,0,'F','0','0','title:audit:list','#',103,1,SYSDATE(),NULL,NULL,''),
  (9110,'政策维护','9104','1','','','',1,0,'F','0','0','title:policy:manage','#',103,1,SYSDATE(),NULL,NULL,''),
  (9111,'审核查看申报','9103','1','','','',1,0,'F','0','0','title:application:query','#',103,1,SYSDATE(),NULL,NULL,''),
  (9112,'上传申报材料','9102','4','','','',1,0,'F','0','0','system:oss:upload','#',103,1,SYSDATE(),NULL,NULL,''),
  (9113,'查询申报材料','9100','10','','','',1,0,'F','0','0','system:oss:query','#',103,1,SYSDATE(),NULL,NULL,''),
  (9114,'下载申报材料','9100','11','','','',1,0,'F','0','0','system:oss:download','#',103,1,SYSDATE(),NULL,NULL,''),
  (9115,'删除草稿材料','9102','5','','','',1,0,'F','0','0','system:oss:remove','#',103,1,SYSDATE(),NULL,NULL,'')
ON DUPLICATE KEY UPDATE
  menu_name=VALUES(menu_name), parent_id=VALUES(parent_id), order_num=VALUES(order_num), path=VALUES(path),
  component=VALUES(component), menu_type=VALUES(menu_type), perms=VALUES(perms), icon=VALUES(icon), status='0';

DELETE FROM sys_role_menu
WHERE role_id BETWEEN 9101 AND 9105 AND menu_id BETWEEN 9100 AND 9199;

INSERT IGNORE INTO sys_role_menu (role_id,menu_id) VALUES
  (9101,9100),(9101,9101),(9101,9102),(9101,9104),(9101,9105),(9101,9106),(9101,9107),(9101,9108),(9101,9109),(9101,9112),(9101,9113),(9101,9114),(9101,9115),
  (9102,9100),(9102,9101),(9102,9103),(9102,9104),(9102,9105),(9102,9109),(9102,9111),(9102,9113),(9102,9114),
  (9103,9100),(9103,9101),(9103,9103),(9103,9104),(9103,9105),(9103,9109),(9103,9111),(9103,9113),(9103,9114),
  (9104,9100),(9104,9101),(9104,9103),(9104,9104),(9104,9105),(9104,9109),(9104,9111),(9104,9113),(9104,9114),
  (9105,9100),(9105,9101),(9105,9103),(9105,9104),(9105,9105),(9105,9109),(9105,9110),(9105,9111),(9105,9113),(9105,9114);
