SELECT c.id, c.name, c.jmx, c.creation_mode, f.id as file_id, f.name as file_name, f.path, f.url 
FROM `case` c 
LEFT JOIN `file` f ON f.id = CAST(c.jmx AS UNSIGNED)
WHERE c.id = 6 AND c.delete_time IS NULL;

