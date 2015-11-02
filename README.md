# Turn Tracker
A nodejs web app to track whose turn it is to do some task.

## Setup

### mysql

0. Install MySQL
0. Setup user account and database
    0. CREATE USER 'node'@'localhost' IDENTIFIED BY 'password';
    0. CREATE DATABASE turn_tracker;
    0. GRANT SELECT, INSERT, DELETE, UPDATE ON turn_tracker.* TO 'node'@'localhost';
    0. FLUSH PRIVILEGES;
0. Setup tables