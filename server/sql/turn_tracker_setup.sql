
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
SET time_zone = "+00:00";

CREATE DATABASE IF NOT EXISTS `turn_tracker` DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci;
USE `turn_tracker`;

DELIMITER $$

# Define the procedure
DROP PROCEDURE IF EXISTS upgradeTurnTrackerDB $$
CREATE PROCEDURE upgradeTurnTrackerDB()
BEGIN

SET @version := 0;
SET @count := 0;
SET @error := '';

# Check for version 0
SET @count := (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'turn_tracker' AND table_name = 'users' LIMIT 1);

IF @count = 0 THEN
	# no users, version 0
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
			SET @version := 1;
			CALL upgradeTurnTrackerV0to1();
		WHEN 1 THEN
			SELECT 'upgrading version from 1 to 2';
			SET @version := 2;
			CALL upgradeTurnTrackerV1to2();
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
CALL upgradeTurnTrackerDB();

# Remove the procedure
DROP PROCEDURE IF EXISTS upgradeTurnTrackerDB;