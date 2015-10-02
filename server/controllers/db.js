var extend = require('util')._extend;
var mysql = require('mysql');
var Promise = require('bluebird');
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