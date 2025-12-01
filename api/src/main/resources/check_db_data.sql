-- 检查数据库中的实际数据

-- 1. 检查 case 表的 status 字段类型和数据
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'case'
  AND COLUMN_NAME = 'status';

-- 2. 查看 case 表中 status 字段的实际值
SELECT 
    id, 
    name, 
    status,
    TYPEOF(status) as status_type,
    CASE 
        WHEN status = 0 THEN 'IDLE(0)'
        WHEN status = 1 THEN 'CONFIGURE(1)'
        WHEN status = 2 THEN 'RUN(2)'
        WHEN status = 3 THEN 'COLLECT(3)'
        WHEN status = 4 THEN 'CLEAN(4)'
        WHEN status = 5 THEN 'INTERRUPT(5)'
        ELSE CONCAT('异常值: ', status)
    END AS status_desc
FROM `case`
WHERE delete_time IS NULL
ORDER BY id;

-- 3. 检查是否有字符串类型的值（如果字段类型是 VARCHAR）
SELECT 
    id, 
    name, 
    status,
    CASE 
        WHEN status REGEXP '^[0-9]+$' THEN '数字'
        ELSE CONCAT('字符串: ', status)
    END AS value_type
FROM `case`
WHERE delete_time IS NULL
  AND status NOT REGEXP '^[0-9]+$'
ORDER BY id;

-- 4. 检查 lin_group 表的 level 字段
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'lin_group'
  AND COLUMN_NAME = 'level';

-- 5. 查看 lin_group 表中 level 字段的实际值
SELECT 
    id, 
    name, 
    level,
    CASE 
        WHEN level = 1 THEN 'ROOT(1)'
        WHEN level = 2 THEN 'GUEST(2)'
        WHEN level = 3 THEN 'USER(3)'
        ELSE CONCAT('异常值: ', level)
    END AS level_desc
FROM lin_group
WHERE delete_time IS NULL
ORDER BY id;

-- 6. 检查 task 表的 result 字段
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'task'
  AND COLUMN_NAME = 'result';

-- 7. 查看 task 表中 result 字段的实际值（最近20条）
SELECT 
    id, 
    task_id, 
    result,
    CASE 
        WHEN result = 0 THEN 'IN_PROGRESS(0)'
        WHEN result = 1 THEN 'SUCCESS(1)'
        WHEN result = 2 THEN 'EXCEPTION(2)'
        WHEN result = 3 THEN 'MANUAL(3)'
        ELSE CONCAT('异常值: ', result)
    END AS result_desc
FROM task
WHERE delete_time IS NULL
ORDER BY id DESC
LIMIT 20;

