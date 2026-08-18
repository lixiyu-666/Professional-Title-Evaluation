-- Repairs Chinese text that was imported as UTF-8 bytes through a latin1 client.
-- Safe to run repeatedly: only values matching the double-encoding byte pattern are changed.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE flow_spel SET remark = CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4) WHERE HEX(remark) LIKE '%C3%C2%';
UPDATE sys_config SET config_name = CONVERT(BINARY CONVERT(config_name USING latin1) USING utf8mb4) WHERE HEX(config_name) LIKE '%C3%C2%';
UPDATE sys_config SET remark = CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4) WHERE HEX(remark) LIKE '%C3%C2%';
UPDATE sys_dept SET dept_name = CONVERT(BINARY CONVERT(dept_name USING latin1) USING utf8mb4) WHERE HEX(dept_name) LIKE '%C3%C2%';
UPDATE sys_dict_data SET dict_label = CONVERT(BINARY CONVERT(dict_label USING latin1) USING utf8mb4) WHERE HEX(dict_label) LIKE '%C3%C2%';
UPDATE sys_dict_data SET remark = CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4) WHERE HEX(remark) LIKE '%C3%C2%';
UPDATE sys_dict_type SET dict_name = CONVERT(BINARY CONVERT(dict_name USING latin1) USING utf8mb4) WHERE HEX(dict_name) LIKE '%C3%C2%';
UPDATE sys_dict_type SET remark = CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4) WHERE HEX(remark) LIKE '%C3%C2%';
UPDATE sys_menu SET menu_name = CONVERT(BINARY CONVERT(menu_name USING latin1) USING utf8mb4) WHERE HEX(menu_name) LIKE '%C3%C2%';
UPDATE sys_menu SET remark = CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4) WHERE HEX(remark) LIKE '%C3%C2%';
UPDATE sys_notice SET notice_title = CONVERT(BINARY CONVERT(notice_title USING latin1) USING utf8mb4) WHERE HEX(notice_title) LIKE '%C3%C2%';
UPDATE sys_notice SET remark = CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4) WHERE HEX(remark) LIKE '%C3%C2%';
UPDATE sys_post SET post_name = CONVERT(BINARY CONVERT(post_name USING latin1) USING utf8mb4) WHERE HEX(post_name) LIKE '%C3%C2%';
UPDATE sys_role SET role_name = CONVERT(BINARY CONVERT(role_name USING latin1) USING utf8mb4) WHERE HEX(role_name) LIKE '%C3%C2%';
UPDATE sys_role SET remark = CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4) WHERE HEX(remark) LIKE '%C3%C2%';
UPDATE sys_tenant SET contact_user_name = CONVERT(BINARY CONVERT(contact_user_name USING latin1) USING utf8mb4) WHERE HEX(contact_user_name) LIKE '%C3%C2%';
UPDATE sys_tenant SET company_name = CONVERT(BINARY CONVERT(company_name USING latin1) USING utf8mb4) WHERE HEX(company_name) LIKE '%C3%C2%';
UPDATE sys_tenant SET intro = CONVERT(BINARY CONVERT(intro USING latin1) USING utf8mb4) WHERE HEX(intro) LIKE '%C3%C2%';
UPDATE sys_user SET nick_name = CONVERT(BINARY CONVERT(nick_name USING latin1) USING utf8mb4) WHERE HEX(nick_name) LIKE '%C3%C2%';
UPDATE sys_user SET remark = CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4) WHERE HEX(remark) LIKE '%C3%C2%';
UPDATE test_demo SET test_key = CONVERT(BINARY CONVERT(test_key USING latin1) USING utf8mb4) WHERE HEX(test_key) LIKE '%C3%C2%';
UPDATE test_demo SET value = CONVERT(BINARY CONVERT(value USING latin1) USING utf8mb4) WHERE HEX(value) LIKE '%C3%C2%';
UPDATE test_tree SET tree_name = CONVERT(BINARY CONVERT(tree_name USING latin1) USING utf8mb4) WHERE HEX(tree_name) LIKE '%C3%C2%';
UPDATE title_batch SET title_series = CONVERT(BINARY CONVERT(title_series USING latin1) USING utf8mb4) WHERE HEX(title_series) LIKE '%C3%C2%';
UPDATE title_batch SET title_level = CONVERT(BINARY CONVERT(title_level USING latin1) USING utf8mb4) WHERE HEX(title_level) LIKE '%C3%C2%';
UPDATE title_batch SET application_type = CONVERT(BINARY CONVERT(application_type USING latin1) USING utf8mb4) WHERE HEX(application_type) LIKE '%C3%C2%';
UPDATE flow_category SET category_name = CONVERT(BINARY CONVERT(category_name USING latin1) USING utf8mb4) WHERE HEX(category_name) LIKE '%C3%C2%';

COMMIT;
