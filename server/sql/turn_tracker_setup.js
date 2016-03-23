
var config = require('../config');
var mysql = config.database.mysql;
var Promise = require('bluebird');

var scripts = [
	'turn_tracker_v0_to_v1.sql',
	'turn_tracker_v1_to_v2.sql',
	'turn_tracker_v2_to_v1.sql',
	'turn_tracker_v2_to_v3.sql',
	'turn_tracker_v3_to_v2.sql',
	'turn_tracker_setup.sql'
];

var baseCmd = '"' + mysql.exe + '"';
if(mysql.ini && mysql.ini.length > 0) {
	baseCmd += ' --defaults-file="' + mysql.ini + '"';
}
if(mysql.user && mysql.user.length > 0) {
	baseCmd += ' -u' + mysql.user;
}
baseCmd += ' -p';
if(mysql.pass && mysql.pass.length > 0) {
	baseCmd += ' -p' + mysql.pass;
}
baseCmd += ' -e"source ' + __dirname.replace(/\\/g,'/') + '/';

var chain = Promise.resolve();

scripts.forEach(function(script){
	chain = chain.then(function(){
		return new Promise(function(resolve, reject) {
			var cmd = baseCmd + script + '"';
			console.log('running cmd: ' + cmd);
			var exec = require('child_process').exec;
			exec(cmd, function(error, stdout, stderr) {
			    console.log('stdout: ', stdout);
			    console.log('stderr: ', stderr);
			    if (error !== null) {
			        console.log('exec error: ', error);
			        reject(error);
			    } else {
			    	resolve();
			    }
			});
		});
	});
});

chain.then(function(){
	console.log('done running db migration scripts');
}).catch(function(err){
	console.log('failed somewhere', err);
});
