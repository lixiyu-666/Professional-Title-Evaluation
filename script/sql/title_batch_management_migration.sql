-- Apply to an existing Docker database after title_evaluation_mvp.sql.
-- Uses information_schema rather than ADD COLUMN IF NOT EXISTS for compatibility
-- with the MySQL image used by the local Docker Compose environment.
SET NAMES utf8mb4;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'title_batch' AND column_name = 'config_json'
);
SET @migration_sql = IF(@column_exists = 0,
  'ALTER TABLE title_batch ADD COLUMN config_json JSON NULL AFTER rule_version',
  'SELECT 1');
PREPARE title_batch_migration FROM @migration_sql;
EXECUTE title_batch_migration;
DEALLOCATE PREPARE title_batch_migration;

INSERT INTO sys_menu VALUES
  (9116,'批次管理','9100','5','batch-manage','title/batch/index','',1,0,'C','0','0','title:batch:manage','calendar',103,1,SYSDATE(),NULL,NULL,'人事职称管理员维护批次'),
  (9117,'发布批次','9116','1','','','',1,0,'F','0','0','title:batch:publish','#',103,1,SYSDATE(),NULL,NULL,'')
ON DUPLICATE KEY UPDATE
  menu_name=VALUES(menu_name), parent_id=VALUES(parent_id), order_num=VALUES(order_num), path=VALUES(path),
  component=VALUES(component), menu_type=VALUES(menu_type), perms=VALUES(perms), icon=VALUES(icon), status='0';

INSERT IGNORE INTO sys_role_menu (role_id,menu_id) VALUES (9105,9116),(9105,9117);
