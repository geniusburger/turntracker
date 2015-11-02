# Turn Tracker
A nodejs web app to track whose turn it is to do some task.

## Setup

### mysql

0. Install MySQL
0. Install Turn Tracker
    0. npm install
0. Setup database
    0. Run SQL script server/turn_tracker.sql to create the database and tables.
0. Setup access
    0. CREATE USER 'node'@'localhost' IDENTIFIED BY 'password';
    0. GRANT SELECT, INSERT, DELETE, UPDATE ON turn_tracker.* TO 'node'@'localhost';
    0. FLUSH PRIVILEGES;
0. Install windows service
    * npm install -g qckwinsvc
    * qckwinsvc --name "Turn Tracker Server" --description "Runs the Turn Tracker NodeJS server." --script "<path to server.js>" --startImmediately
0. Uninstall windows service
    * qckwinsvc --uninstall --name "Turn Tracker Server" --script "<path to server.js>"