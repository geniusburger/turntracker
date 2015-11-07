var extend = require('util')._extend;
var mysql = require('mysql');
var Promise = require('bluebird');
var using = Promise.using;
var debug = require('debug')('turntracker:db');
var config = require('../config');

var pool = mysql.createPool( extend( config.database.test, {} ) );

var getConnection = function() {
	return new Promise(function(resolve, reject){
		pool.getConnection(function(err, conn) {
			if(err) {
				reject(err);
			} else {
				resolve(conn);
			}
		});
	}).disposer(function(connection){
		connection.release();
	});
};
exports.getConnection = getConnection;

var getVersion = function() {
	return using(getConnection(), function(conn) {
		return new Promise(function(resolve, reject){
			conn.query('SELECT version FROM version_history ORDER BY date DESC LIMIT 1', function(err, rows, fields){
				if(err) {
					log("ERROR failed to get database version", err);
					reject();
				} else {
					resolve((rows && rows[0] && rows[0].version) || 1);
				}
			});
		});
	});
};
exports.getVersion = getVersion;