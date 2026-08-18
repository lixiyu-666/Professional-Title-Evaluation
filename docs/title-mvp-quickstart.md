# 职称评审系统 MVP 本地启动

## 环境

- Docker Desktop（WSL 2 后端）
- Java 17 或 21
- Maven 3.9+
- Node.js 20.19+
- pnpm 10+

## 1. 启动基础服务

在仓库根目录执行：

```powershell
docker compose -f docker-compose.title.yml up -d
docker compose -f docker-compose.title.yml ps
```

首次创建 MySQL 数据卷时会自动依次导入框架、工作流和职称 MVP SQL。

如果数据库卷已经存在，更新业务菜单和演示数据：

```powershell
Get-Content -Raw -Encoding UTF8 script\sql\title_evaluation_mvp.sql |
  docker compose -f docker-compose.title.yml exec -T mysql mysql --default-character-set=utf8mb4 -uroot -proot ry-vue
Get-Content -Raw -Encoding UTF8 script\sql\title_encoding_repair.sql |
  docker compose -f docker-compose.title.yml exec -T mysql mysql --default-character-set=utf8mb4 -uroot -proot ry-vue
```

## 2. 启动后端

```powershell
mvn -pl ruoyi-admin -am -DskipTests package
java -jar ruoyi-admin\target\ruoyi-admin.jar --spring.profiles.active=title-dev
```

后端地址为 `http://localhost:8080`，接口文档入口为 `http://localhost:8080/swagger-ui.html`。

## 3. 启动前端

另开一个终端：

```powershell
cd plus-ui
pnpm install
pnpm dev
```

浏览器打开 `http://localhost:81`。

## 演示账号

演示环境的统一密码为 `admin123`：

- 申请人：`title_applicant`
- 部门人事：`title_dept_reviewer`
- 技术审核人：`title_technical_reviewer`
- 部门领导：`title_dept_leader`
- 人事职称管理员：`title_hr_admin`

申请人只显示职称首页、我的申报和政策助手；其余四个角色显示职称首页、审核工作台和政策助手。

## 验证命令

```powershell
mvn -pl ruoyi-modules/ruoyi-title -am -DskipTests=false test
cd plus-ui
pnpm lint:eslint src/api/title src/views/title
pnpm build:prod
```

五步表单字段由 `script/tools/generate_title_fields.py` 从 PRD 附录 C 至 G 提取生成。生成器会强制校验字段编码唯一且总数为 149。
