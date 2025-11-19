-- 修复 jmeter_status 字段中的错误数据
-- 问题：数据库中存储了 'IN_PROGRESS' 字符串，但 JmeterStatusEnum 中没有这个值

-- 1. 查看当前有问题的数据
SELECT
    id,
    name,
    address,
    jmeter_status,
    CASE
        WHEN jmeter_status NOT IN (0,1,2,3,4,5) THEN 'INVALID'
        ELSE 'VALID'
    END as status_check
FROM machine
WHERE delete_time IS NULL;

-- 2. 修复错误数据 - 将所有非有效枚举值重置为 IDLE(0)
UPDATE machine
SET jmeter_status = 0
WHERE delete_time IS NULL
  AND jmeter_status NOT IN (0,1,2,3,4,5);

-- 3. 验证修复结果
SELECT
    id,
    name,
    address,
    jmeter_status,
    CASE
        WHEN jmeter_status = 0 THEN 'IDLE'
        WHEN jmeter_status = 1 THEN 'CONFIGURE'
        WHEN jmeter_status = 2 THEN 'RUN'
        WHEN jmeter_status = 3 THEN 'COLLECT'
        WHEN jmeter_status = 4 THEN 'CLEAN'
        WHEN jmeter_status = 5 THEN 'INTERRUPT'
        ELSE 'UNKNOWN'
    END as status_desc
FROM machine
WHERE delete_time IS NULL;