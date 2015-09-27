var https = require('https');

var db = require('./db');
var config = require('../config');

exports.getTurns = function(taskId) {
	return new Promise(function(resolve, reject){
		db.query(
	    	'SELECT users.displayname AS name, turns.inserted AS date, users.id ' +
	    	'FROM tasks INNER JOIN turns on turns.task_id = tasks.id INNER JOIN USERS ON turns.user_id = users.id ' +
			'WHERE tasks.id = ? ORDER BY turns.inserted DESC',
			taskId, function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};

exports.getTasks = function(userId) {
	return new Promise(function(resolve, reject){
		db.query(
			'SELECT tasks.id,  tasks.name, tasks.periodic_hours, tasks.creator_user_id ' +
			'FROM participants JOIN tasks ON participants.task_id = tasks.id ' +
			'WHERE participants.user_id = ?', 
			userId, function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};

exports.getStatus = function(taskId, callback) {
	return new Promise(function(resolve, reject){
		db.query(
			'SELECT users.id AS id, users.displayname AS name, IFNULL(counts.turns, 0) AS turns ' + 
			'FROM participants JOIN users on participants.user_id = users.id LEFT JOIN ( ' +
				'SELECT turns.user_id, count(*) as turns, turns.inserted ' +
				'FROM turns WHERE turns.task_id = ? ' +
				'GROUP BY turns.user_id ORDER BY turns.inserted ASC ' +
			') counts on participants.user_id = counts.user_id ' +
			'WHERE  participants.task_id = ? ' +
			'ORDER by turns ASC, counts.inserted ASC',
			[taskId,taskId], function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};

exports.saveAddress = function(userId, ip, callback) {
	return new Promise(function(resolve, reject){
	    db.query( 'INSERT INTO addresses SET ? ON DUPLICATE KEY UPDATE user_id = ?',
	    	[{user_id: userId, ip: ip}, userId], function(err, rows, fields) {
			if (err) {
				if(err.code === 'ER_DUP_ENTRY') {
					console.log('Already saved user %d address %s', userId, ip);
					resolve(userId);
				} else {
					console.error('Error while performing addresses query', err);
					reject(err);
				}
			} else {
				console.log('Saved user %d address %s', userId, ip);
				resolve(userId);
			}
		})
	});
};

exports.getUser = function(ip, callback) {
	return new Promise(function(resolve, reject){
		db.query('SELECT users.id, users.displayname as name FROM addresses JOIN users ON users.id = addresses.user_id WHERE ip = ?',
			ip, function(err, rows, fields) {
				if(err) {
					console.error('Error while getting address', err);
					reject(err);
				} else {
					if(rows[0]) {
						console.log('Got user from address %s', ip, rows[0]);
						resolve(rows[0]);
					} else {
						console.warn("Didn't find user for address %s", ip);
						reject();
					}
				}
		});
	});
};

exports.getUsers = function() {
	return new Promise(function(resolve, reject){
		db.query('SELECT id, displayname AS name FROM users ORDER BY displayname ASC', function(err, rows, fields) {
			if(err) {
				console.error('failed to get all users', err);
				reject(err);
			} else {
				resolve(rows);
			}
		});
	});
};

exports.takeTurn = function(taskId, userid) {
	return new Promise(function(resolve, reject){
	    db.query( 'INSERT INTO turns SET ?', {user_id: userid, task_id: taskId}, function(err, rows, fields) {
			if (err) {
				console.error('Error while performing turn query', err);
				reject(err);
			} else {
				resolve();
			}
	    });
	});
};

exports.getAndroidUsers = function() {
	return new Promise(function(resolve, reject){
		db.query('SELECT id, displayname AS name, androidtoken as token FROM users WHERE androidtoken IS NOT NULL', function(err, rows, fields) {
			if(err) {
				console.error('failed to get android users', err);
				reject(err);
			} else {
				resolve(rows);
			}
		});
	});
};

exports.setAndroidToken = function(userid, token) {
	return new Promise(function(resolve, reject){
		db.query('UPDATE users SET androidtoken = ? WHERE id = ?', [token, userid], function(err, rows, fields){
			if(err) {
				console.error('failed to set android token for user ' + userid, token);
				reject(err);
			} else {
				console.log('set android token for user ' + userid + ' to ' + token);
				resolve();
			}
		});
	});
};

exports.sendAndroidMessage = function(dataObject, token) {
	return new Promise(function(resolve, reject){
		// Build the post string from an object
		var data = {
			data: dataObject
		}
		if(token) {
			if(Array.isArray(token)) {
				data.registration_ids = token;
			} else if(typeof token === 'string') {
				data.to = token;
			} else {
				data.to = '/topics/global';	
			}
		} else {
			data.to = '/topics/global';
		}
		console.log('sending android message', data);
		var dataString = JSON.stringify(data);

		// Set up the request
		var req = https.request({
			host: 'android.googleapis.com',
			port: '443',
			path: '/gcm/send',
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'Content-Length': dataString.length,
				"Authorization": "key=" + config.gcm.apiKey
			}
		}, function(res) {
			res.setEncoding('utf8');
			res.on('data', function (chunk) {
				resolve(chunk);
			});
		});

		// post the data
		req.write(dataString);
		req.end();
		req.on('error', function(err){
			console.error('failed to send android message', err);
			reject(err);
		});
	});
};
