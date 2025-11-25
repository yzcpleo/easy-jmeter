@echo off
echo Running JMX Structure Migration...
mysql -u root -proot -P 3307 -h localhost easy-jmeter < add_jmx_structure_and_creation_mode.sql
if %ERRORLEVEL% EQU 0 (
    echo Migration completed successfully!
) else (
    echo Migration failed with error code %ERRORLEVEL%
)
pause

