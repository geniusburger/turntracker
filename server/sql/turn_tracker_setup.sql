
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
SET time_zone = "+00:00";

DELIMITER $$

# Define the procedure
DROP PROCEDURE IF EXISTS  test.upgradeTurnTrackerDB $$
CREATE PROCEDURE test.upgradeTurnTrackerDB()
BEGIN

SET @version := 0;
SET @count := 0;
SET @error := '';

# Check for database
SET @count := (SELECT COUNT(*) FROM information_schema.schemata WHERE SCHEMA_NAME = 'turn_tracker' LIMIT 1);

IF @count = 0 THEN
	# no database, version 0
	SET @version := 0;
ELSE
	# found database, try to get version number
    SET @count := (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'turn_tracker' AND table_name = 'version_history' LIMIT 1);
    IF @count = 0 THEN
		# not version_history table, version 1
		SET @version := 1;
	ELSE
		SET @version := (SELECT version FROM turn_tracker.version_history ORDER BY date DESC LIMIT 1);
	END IF;
END IF;

upgrade_loop: LOOP
	CASE @version
		WHEN 0 THEN
			SELECT 'upgrading version from 0 to 1';
			SOURCE turn_tracker_v1.sql;
			SET @version := 1;
		WHEN 1 THEN
			SELECT 'upgrading version from 1 to 2';
			SOURCE turn_tracker_v2.sql;
			SET @version := 2;
		WHEN 2 THEN
			SELECT 'done version 2';
			LEAVE upgrade_loop;
		ELSE 
			SET @error := CONCAT('unhandled upgrade version ',IFNULL(CONCAT("'",@version,"'"),'NULL'));
			SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @error;
			LEAVE upgrade_loop;
	END CASE;
END LOOP upgrade_loop;

END $$

DELIMITER ;

# Execute procedure
CALL test.upgradeTurnTrackerDB();

# Remove the procedure
DROP PROCEDURE test.upgradeTurnTrackerDB;