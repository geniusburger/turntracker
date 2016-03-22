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
DROP PROCEDURE IF EXISTS upgradeTurnTrackerV2to3 $$
CREATE PROCEDURE upgradeTurnTrackerV2to3()
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

INSERT INTO `reasons` (`id`, `label`, `description`) VALUES (3, 'only reminders', 'None, only reminders');
INSERT INTO `version_history` (`version`) VALUES (3);

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

END $$
DELIMITER ;
