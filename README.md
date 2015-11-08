# Turn Tracker
A nodejs web app to track whose turn it is to do some task.

## NodeJS Server

### Setup

0. Install MySQL
0. Install Turn Tracker
    0. `npm install`
0. Setup database
    0. Run the SQL script _server/sql/turn_tracker_setup.sql_ in a MySQL terminal to create the database and tables.
0. Setup MySQL access
    Run the following commands in a MySQL terminal:
    0. `CREATE USER 'node'@'localhost' IDENTIFIED BY '<your password here>';`
    0. `GRANT SELECT, INSERT, DELETE, UPDATE ON turn_tracker.* TO 'node'@'localhost';`
    0. Copy _server/config.template.js_ to _server/config.js_
    0. Enter your credentials in _server/config.js_

### Run
Ensure MySQL is running and you've already followed the setup steps.
`npm start`

### Windows Service
This makes use of [qckwinsvc](https://www.npmjs.com/package/qckwinsvc) which is a wrapper for [node-windows](https://www.npmjs.com/package/node-windows).

#### Install
`npm run-script install-win-svc`

#### Uninstall
`npm run-script uninstall-win-svc`

### Logging
The [debug](https://www.npmjs.com/package/debug) npm package is used for logging. All logs are named _turntracker:<name>_ so the simple way of viewing it would be to set `DEBUG=turntracker:*` in your environment variables.

## Andnroid App

