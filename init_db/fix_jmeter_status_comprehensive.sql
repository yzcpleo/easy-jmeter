-- 检查并修复 jmeter_status 数据的综合脚本

-- 查看当前所有数据
SELECT '=== 当前 machine 表中的所有 jmeter_status 值 ===' as info;
SELECT id, name, jmeter_status FROM machine WHERE delete_time IS NULL;

-- 查找所有问题数据（包括非数字和 IN_PROGRESS 值）
SELECT '=== 查找所有问题数据 ===' as info;
SELECT id, name, jmeter_status FROM machine
WHERE delete_time IS NULL
  AND (jmeter_status NOT IN ('0','1','2','3','4','5') OR jmeter_status IS NULL);

-- 强制修复所有问题数据，包括 'IN_PROGRESS'
SELECT '=== 开始修复所有问题数据 ===' as info;
UPDATE machine
SET jmeter_status = '0'
WHERE delete_time IS NULL
  AND (jmeter_status NOT IN ('0','1','2','3','4','5') OR jmeter_status IS NULL);

-- 显示修复结果
SELECT '=== 修复完成后的结果 ===' as info;
SELECT ROW_COUNT() as fixed_rows_count;

-- 验证修复结果
SELECT '=== 验证修复结果 ===' as info;
SELECT COUNT(*) as remaining_problems
FROM machine
WHERE delete_time IS NULL
  AND (jmeter_status NOT IN ('0','1','2','3','4','5') OR jmeter_status IS NULL);

-- 显示最终的所有数据
SELECT '=== 最终的 jmeter_status 数据 ===' as info;
SELECT id, name, jmeter_status FROM machine WHERE delete_time IS NULL;