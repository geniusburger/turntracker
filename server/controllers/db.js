var extend = require('util')._extend;
var mysql = require('mysql');
var debug = require('debug')('turntracker:db');
var config = require('../config');

var db = mysql.createConnection( extend( config.database.test, {} ) );

db.connect(function(err){
	if(!err) {
		console.log("Database is connected ... \n\n");
	} else {
		console.error("Error connecting database ... \n\n");  
	}
});

module.exports = db;