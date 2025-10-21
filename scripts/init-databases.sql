-- 创建所有微服务需要的数据库 (PostgreSQL 语法)
-- 注意: PostgreSQL 的 docker-entrypoint-initdb.d 脚本在首次启动时自动执行
-- 如果数据库已存在,CREATE DATABASE 会报错但不影响后续操作

-- 为了避免重复创建错误,使用 \gexec 和条件查询(需要 psql)
-- 或者简单地尝试创建,如果失败则忽略

-- 创建数据库(如果已存在会报错,但容器初始化时只运行一次,所以问题不大)
CREATE DATABASE admin_db;
CREATE DATABASE cafeteria_db;
CREATE DATABASE media_db;
CREATE DATABASE preference_db;
