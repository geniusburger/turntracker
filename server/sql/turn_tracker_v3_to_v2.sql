-- phpMyAdmin SQL Dump
-- version 4.4.12
-- http://www.phpmyadmin.net
--
-- Host: 127.0.0.1
-- Generation Time: Nov 07, 2015 at 10:34 PM
-- Server version: 5.6.25
-- PHP Version: 5.6.11

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
SET time_zone = "+00:00";

CREATE DATABASE IF NOT EXISTS `turn_tracker` DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci;
USE `turn_tracker`;

DELIMITER $$

# Define the procedure
DROP PROCEDURE IF EXISTS upgradeTurnTrackerV3to2 $$
CREATE PROCEDURE upgradeTurnTrackerV3to2()
BEGIN

START TRANSACTION;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `turn_tracker`
--

-- --------------------------------------------------------

INSERT INTO `version_history` (`version`) VALUES (2);

--
-- Remove reason 3
--

UPDATE `notifications` SET `reason_id` = 2 WHERE `reason_id` = 3;
DELETE FROM `reasons` WHERE `id` = 3;

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

END $$
DELIMITER ;
