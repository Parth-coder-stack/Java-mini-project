-- ============================================================
--  UCMS - create the MySQL user the Java app logs in with.
--  Run AFTER schema.sql, on the machine that runs MySQL, as root.
--  The user/password here must match db.properties.
-- ============================================================

-- For the app running on the SAME computer as MySQL
CREATE USER IF NOT EXISTS 'ucms_user'@'localhost' IDENTIFIED BY 'ucms_pass';
ALTER USER 'ucms_user'@'localhost' IDENTIFIED BY 'ucms_pass';   -- resets an older password
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON ucms_db.* TO 'ucms_user'@'localhost';

-- For the app running on ANOTHER computer on the same network
CREATE USER IF NOT EXISTS 'ucms_user'@'%' IDENTIFIED BY 'ucms_pass';
ALTER USER 'ucms_user'@'%' IDENTIFIED BY 'ucms_pass';
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON ucms_db.* TO 'ucms_user'@'%';   -- EXECUTE = call fn_cgpa / fn_sgpa

FLUSH PRIVILEGES;

-- ------------------------------------------------------------
-- Extra steps ONLY if the app runs on a different computer:
--
-- 1. Let MySQL accept network connections:
--    open C:\ProgramData\MySQL\MySQL Server 8.0\my.ini, set
--        bind-address = 0.0.0.0
--    then restart the "MySQL80" service (services.msc).
--
-- 2. Open port 3306 in the firewall (PowerShell as Administrator):
--    netsh advfirewall firewall add rule name="MySQL UCMS" protocol=TCP dir=in localport=3306 action=allow
--
-- 3. Find this computer's IP:  ipconfig  ->  "IPv4 Address", e.g. 192.168.1.50
--    Put it in db.properties (db.host=...) on the other computer,
--    or type it into "Database settings" on the login screen.
-- ------------------------------------------------------------
