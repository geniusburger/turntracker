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
DROP PROCEDURE IF EXISTS upgradeTurnTrackerV1to2 $$
CREATE PROCEDURE upgradeTurnTrackerV1to2()
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

--
-- Table structure for table `version_history`
--

CREATE TABLE IF NOT EXISTS `version_history` (
  `id` int(10) unsigned NOT NULL,
  `version` int(10) unsigned NOT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

ALTER TABLE `version_history`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `version_history`
  MODIFY `id` int(10) unsigned NOT NULL AUTO_INCREMENT;COMMIT;

INSERT INTO `version_history` (`version`) VALUES (2);

--
-- Add 'taken' column to 'turns' table. Use the 'inserted' date as its value.
--

ALTER TABLE `turns` ADD `taken` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `task_id`;
UPDATE `turns` SET taken = inserted;

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

END $$
DELIMITER ;
