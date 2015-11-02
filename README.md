# Turn Tracker
A nodejs web app to track whose turn it is to do some task.

## Setup

0. Install MySQL
0. Install Turn Tracker
    0. `npm install`
0. Setup database
    0. Run the SQL script server/turn_tracker.sql in a MySQL terminal to create the database and tables.
0. Setup MySQL access
    Run the following commands in a MySQL terminal:
    0. `CREATE USER 'node'@'localhost' IDENTIFIED BY '<your password here>';`
    0. `GRANT SELECT, INSERT, DELETE, UPDATE ON turn_tracker.* TO 'node'@'localhost';`

## Windows Service

### Install
`npm install -g qckwinsvc`
`qckwinsvc --name "Turn Tracker Server" --description "Runs the Turn Tracker NodeJS server." --script "<path to server.js>" --startImmediately`

### Uninstall
`qckwinsvc --uninstall --name "Turn Tracker Server" --script "<path to server.js>"`

## Logging
The [debug](https://www.npmjs.com/package/debug) npm package is used for logging. All logs are named _turntracker:<name>_ so the simple way of viewing it would be to set `DEBUG=turntracker:*` in your environment variables.