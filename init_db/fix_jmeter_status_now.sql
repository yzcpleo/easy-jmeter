-- 修复 jmeter_status 字段中的错误数据
UPDATE machine 
SET jmeter_status = 0 
WHERE delete_time IS NULL 
  AND (jmeter_status NOT IN ('0','1','2','3','4','5') OR jmeter_status IS NULL);

-- 查看修复后的数据
SELECT id, name, jmeter_status FROM machine WHERE delete_time IS NULL;
