-- phpMyAdmin SQL Dump
-- version 4.4.12
-- http://www.phpmyadmin.net
--
-- Host: 127.0.0.1
-- Generation Time: Aug 27, 2015 at 07:22 AM
-- Server version: 5.6.25
-- PHP Version: 5.6.11

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `turn_tracker`
--
CREATE DATABASE IF NOT EXISTS `turn_tracker` DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci;
USE `turn_tracker`;

-- --------------------------------------------------------

--
-- Table structure for table `addresses`
--

CREATE TABLE IF NOT EXISTS `addresses` (
  `user_id` int(10) unsigned NOT NULL,
  `ip` varchar(40) COLLATE utf8_unicode_ci NOT NULL,
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `inserted` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `addresses`
--

INSERT INTO `addresses` (`user_id`, `ip`, `modified`, `inserted`) VALUES
(1, '::1', '2015-08-22 13:43:10', '2015-08-22 12:48:28'),
(1, '::ffff:192.168.2.11', '2015-08-23 04:36:54', '2015-08-23 04:36:54'),
(3, '::ffff:192.168.2.3', '2015-08-23 04:14:51', '2015-08-23 04:14:38');

-- --------------------------------------------------------

--
-- Table structure for table `participants`
--

CREATE TABLE IF NOT EXISTS `participants` (
  `task_id` int(10) unsigned NOT NULL,
  `user_id` int(10) unsigned NOT NULL,
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `inserted` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `participants`
--

INSERT INTO `participants` (`task_id`, `user_id`, `modified`, `inserted`) VALUES
(1, 1, '2015-08-17 04:53:30', '2015-08-17 04:53:30'),
(1, 2, '2015-08-17 04:53:30', '2015-08-17 04:53:30'),
(1, 3, '2015-08-17 07:09:03', '2015-08-17 07:09:03');

-- --------------------------------------------------------

--
-- Table structure for table `tasks`
--

CREATE TABLE IF NOT EXISTS `tasks` (
  `id` int(10) unsigned NOT NULL,
  `name` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `periodic_hours` int(10) unsigned NOT NULL DEFAULT '0',
  `creator_user_id` int(10) unsigned NOT NULL,
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `inserted` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `tasks`
--

INSERT INTO `tasks` (`id`, `name`, `periodic_hours`, `creator_user_id`, `modified`, `inserted`) VALUES
(1, 'Litter Box', 48, 1, '2015-08-17 04:52:06', '2015-08-17 04:52:06');

-- --------------------------------------------------------

--
-- Table structure for table `turns`
--

CREATE TABLE IF NOT EXISTS `turns` (
  `id` int(10) unsigned NOT NULL,
  `user_id` int(10) unsigned NOT NULL,
  `task_id` int(10) unsigned NOT NULL,
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `inserted` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB AUTO_INCREMENT=98 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `turns`
--

INSERT INTO `turns` (`id`, `user_id`, `task_id`, `modified`, `inserted`) VALUES
(1, 1, 1, '2015-08-17 04:51:30', '2015-08-17 04:27:12'),
(2, 2, 1, '2015-08-17 04:51:39', '2015-08-17 04:27:24'),
(3, 1, 1, '2015-08-17 04:51:45', '2015-08-17 04:27:29'),
(4, 1, 1, '2015-08-17 09:04:34', '2015-08-17 09:04:34'),
(5, 1, 1, '2015-08-17 09:09:55', '2015-08-17 09:09:55'),
(6, 1, 1, '2015-08-17 09:15:18', '2015-08-17 09:15:18'),
(7, 2, 1, '2015-08-17 09:15:50', '2015-08-17 09:15:50'),
(8, 3, 1, '2015-08-17 09:15:52', '2015-08-17 09:15:52'),
(9, 2, 1, '2015-08-17 09:15:54', '2015-08-17 09:15:54'),
(10, 1, 1, '2015-08-17 09:15:59', '2015-08-17 09:15:59'),
(11, 3, 1, '2015-08-17 09:16:02', '2015-08-17 09:16:02'),
(12, 3, 1, '2015-08-17 09:16:04', '2015-08-17 09:16:04'),
(13, 2, 1, '2015-08-17 09:16:07', '2015-08-17 09:16:07'),
(14, 2, 1, '2015-08-17 09:16:08', '2015-08-17 09:16:08'),
(15, 3, 1, '2015-08-17 09:16:11', '2015-08-17 09:16:11'),
(16, 3, 1, '2015-08-17 09:16:12', '2015-08-17 09:16:12'),
(17, 2, 1, '2015-08-17 09:16:16', '2015-08-17 09:16:16'),
(18, 2, 1, '2015-08-17 09:22:22', '2015-08-17 09:22:22'),
(19, 3, 1, '2015-08-17 09:23:49', '2015-08-17 09:23:49'),
(20, 1, 1, '2015-08-17 09:23:51', '2015-08-17 09:23:51'),
(21, 3, 1, '2015-08-17 09:23:54', '2015-08-17 09:23:54'),
(22, 2, 1, '2015-08-17 09:23:56', '2015-08-17 09:23:56'),
(23, 1, 1, '2015-08-17 09:45:31', '2015-08-17 09:45:31'),
(24, 3, 1, '2015-08-17 09:45:33', '2015-08-17 09:45:33'),
(25, 2, 1, '2015-08-17 09:49:42', '2015-08-17 09:49:42'),
(26, 3, 1, '2015-08-17 09:49:54', '2015-08-17 09:49:54'),
(27, 1, 1, '2015-08-17 09:49:54', '2015-08-17 09:49:54'),
(28, 2, 1, '2015-08-17 09:50:24', '2015-08-17 09:50:24'),
(29, 1, 1, '2015-08-17 09:51:39', '2015-08-17 09:51:39'),
(30, 3, 1, '2015-08-17 09:51:45', '2015-08-17 09:51:45'),
(31, 1, 1, '2015-08-17 09:51:47', '2015-08-17 09:51:47'),
(32, 3, 1, '2015-08-17 09:51:49', '2015-08-17 09:51:49'),
(33, 1, 1, '2015-08-17 09:51:52', '2015-08-17 09:51:52'),
(34, 3, 1, '2015-08-17 09:51:52', '2015-08-17 09:51:52'),
(35, 3, 1, '2015-08-17 09:51:58', '2015-08-17 09:51:58'),
(36, 1, 1, '2015-08-17 09:51:59', '2015-08-17 09:51:59'),
(37, 3, 1, '2015-08-17 09:52:00', '2015-08-17 09:52:00'),
(38, 1, 1, '2015-08-17 09:52:01', '2015-08-17 09:52:01'),
(39, 3, 1, '2015-08-17 09:52:01', '2015-08-17 09:52:01'),
(40, 2, 1, '2015-08-17 09:52:03', '2015-08-17 09:52:03'),
(41, 2, 1, '2015-08-17 09:52:04', '2015-08-17 09:52:04'),
(42, 2, 1, '2015-08-17 09:52:05', '2015-08-17 09:52:05'),
(43, 1, 1, '2015-08-17 09:52:05', '2015-08-17 09:52:05'),
(44, 2, 1, '2015-08-17 09:52:06', '2015-08-17 09:52:06'),
(45, 2, 1, '2015-08-17 09:52:07', '2015-08-17 09:52:07'),
(46, 3, 1, '2015-08-17 09:52:09', '2015-08-17 09:52:09'),
(47, 3, 1, '2015-08-17 09:52:13', '2015-08-17 09:52:13'),
(48, 2, 1, '2015-08-17 09:52:52', '2015-08-17 09:52:52'),
(49, 2, 1, '2015-08-17 09:52:58', '2015-08-17 09:52:58'),
(50, 3, 1, '2015-08-17 09:52:59', '2015-08-17 09:52:59'),
(51, 2, 1, '2015-08-17 09:52:59', '2015-08-17 09:52:59'),
(52, 3, 1, '2015-08-18 07:26:48', '2015-08-18 07:26:48'),
(53, 1, 1, '2015-08-18 08:23:22', '2015-08-18 08:23:22'),
(54, 1, 1, '2015-08-18 08:23:23', '2015-08-18 08:23:23'),
(55, 1, 1, '2015-08-18 08:23:24', '2015-08-18 08:23:24'),
(56, 2, 1, '2015-08-18 08:23:27', '2015-08-18 08:23:27'),
(57, 1, 1, '2015-08-18 08:23:28', '2015-08-18 08:23:28'),
(58, 3, 1, '2015-08-18 08:29:29', '2015-08-18 08:29:29'),
(59, 2, 1, '2015-08-18 08:29:30', '2015-08-18 08:29:30'),
(60, 1, 1, '2015-08-18 08:29:32', '2015-08-18 08:29:32'),
(61, 3, 1, '2015-08-21 06:19:00', '2015-08-21 06:19:00'),
(62, 3, 1, '2015-08-21 06:19:02', '2015-08-21 06:19:02'),
(63, 1, 1, '2015-08-21 06:19:05', '2015-08-21 06:19:05'),
(64, 2, 1, '2015-08-21 06:19:08', '2015-08-21 06:19:08'),
(65, 1, 1, '2015-08-21 06:19:10', '2015-08-21 06:19:10'),
(66, 2, 1, '2015-08-21 06:19:11', '2015-08-21 06:19:11'),
(67, 1, 1, '2015-08-22 08:20:03', '2015-08-22 08:20:03'),
(68, 3, 1, '2015-08-22 08:44:43', '2015-08-22 08:44:43'),
(69, 2, 1, '2015-08-22 08:45:03', '2015-08-22 08:45:03'),
(70, 1, 1, '2015-08-22 08:45:59', '2015-08-22 08:45:59'),
(71, 2, 1, '2015-08-22 08:59:06', '2015-08-22 08:59:06'),
(72, 3, 1, '2015-08-22 09:01:09', '2015-08-22 09:01:09'),
(73, 1, 1, '2015-08-22 11:28:57', '2015-08-22 11:28:57'),
(74, 1, 1, '2015-08-22 12:36:47', '2015-08-22 12:36:47'),
(75, 1, 1, '2015-08-22 12:37:58', '2015-08-22 12:37:58'),
(76, 3, 1, '2015-08-22 12:39:29', '2015-08-22 12:39:29'),
(77, 2, 1, '2015-08-22 12:45:18', '2015-08-22 12:45:18'),
(78, 3, 1, '2015-08-22 12:46:02', '2015-08-22 12:46:02'),
(79, 2, 1, '2015-08-22 12:46:25', '2015-08-22 12:46:25'),
(80, 3, 1, '2015-08-22 12:46:48', '2015-08-22 12:46:48'),
(81, 1, 1, '2015-08-22 12:48:02', '2015-08-22 12:48:02'),
(82, 1, 1, '2015-08-22 12:48:28', '2015-08-22 12:48:28'),
(83, 2, 1, '2015-08-22 12:48:49', '2015-08-22 12:48:49'),
(84, 2, 1, '2015-08-22 12:51:16', '2015-08-22 12:51:16'),
(85, 2, 1, '2015-08-22 13:07:54', '2015-08-22 13:07:54'),
(86, 2, 1, '2015-08-22 13:10:45', '2015-08-22 13:10:45'),
(87, 3, 1, '2015-08-22 13:11:07', '2015-08-22 13:11:07'),
(88, 3, 1, '2015-08-22 13:11:09', '2015-08-22 13:11:09'),
(89, 3, 1, '2015-08-22 13:32:53', '2015-08-22 13:32:53'),
(90, 3, 1, '2015-08-22 13:33:00', '2015-08-22 13:33:00'),
(91, 1, 1, '2015-08-22 13:33:39', '2015-08-22 13:33:39'),
(92, 2, 1, '2015-08-22 13:43:05', '2015-08-22 13:43:05'),
(93, 1, 1, '2015-08-22 13:43:10', '2015-08-22 13:43:10'),
(94, 1, 1, '2015-08-23 04:14:38', '2015-08-23 04:14:38'),
(95, 1, 1, '2015-08-23 04:14:47', '2015-08-23 04:14:47'),
(96, 3, 1, '2015-08-23 04:14:51', '2015-08-23 04:14:51'),
(97, 1, 1, '2015-08-23 04:36:54', '2015-08-23 04:36:54');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE IF NOT EXISTS `users` (
  `id` int(10) unsigned NOT NULL,
  `username` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  `displayname` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  `picture` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `inserted` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `displayname`, `picture`, `modified`, `inserted`) VALUES
(1, 'user1', 'Human', NULL, '2015-08-17 07:06:59', '2015-08-17 04:26:03'),
(2, 'user2', 'Robot', NULL, '2015-08-17 07:06:49', '2015-08-17 04:26:03'),
(3, 'user3', 'Monkey', NULL, '2015-08-17 07:06:36', '2015-08-17 07:06:36'),
(4, 'user4', 'Cat', NULL, '2015-08-17 07:08:46', '2015-08-17 07:08:46');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `addresses`
--
ALTER TABLE `addresses`
  ADD PRIMARY KEY (`ip`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `participants`
--
ALTER TABLE `participants`
  ADD PRIMARY KEY (`task_id`,`user_id`),
  ADD KEY `task_id` (`task_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `tasks`
--
ALTER TABLE `tasks`
  ADD PRIMARY KEY (`id`),
  ADD KEY `creator_user_id` (`creator_user_id`);

--
-- Indexes for table `turns`
--
ALTER TABLE `turns`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `task_id` (`task_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username_unique` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `tasks`
--
ALTER TABLE `tasks`
  MODIFY `id` int(10) unsigned NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=2;
--
-- AUTO_INCREMENT for table `turns`
--
ALTER TABLE `turns`
  MODIFY `id` int(10) unsigned NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=98;
--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(10) unsigned NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=5;
--
-- Constraints for dumped tables
--

--
-- Constraints for table `addresses`
--
ALTER TABLE `addresses`
  ADD CONSTRAINT `addresses_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `participants`
--
ALTER TABLE `participants`
  ADD CONSTRAINT `participants_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  ADD CONSTRAINT `participants_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `tasks`
--
ALTER TABLE `tasks`
  ADD CONSTRAINT `tasks_ibfk_1` FOREIGN KEY (`creator_user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `turns`
--
ALTER TABLE `turns`
  ADD CONSTRAINT `turns_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `turns_ibfk_2` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
