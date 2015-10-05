var https = require('https');
var Promise = require('bluebird');
var log = require('debug')('turntracker:index');

var db = require('./db');
var config = require('../config');

var getTurns = function(conn, taskId) {
	return new Promise(function(resolve, reject){
		conn.query(
	    	'SELECT users.displayname AS name, turns.inserted AS date, users.id as userid, turns.id as turnid ' +
	    	'FROM tasks INNER JOIN turns on turns.task_id = tasks.id INNER JOIN USERS ON turns.user_id = users.id ' +
			'WHERE tasks.id = ? ORDER BY turns.inserted DESC',
			[taskId], function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};
exports.getTurns = getTurns;

var getSubscriptions = function(conn, userId) {
	return new Promise(function(resolve, reject){
		conn.query(
			'SELECT notifications.user_id, notifications.task_id, tasks.name AS taskName, notifications.method_id, methods.label AS methodLabel, methods.description AS methodDescription, notifications.reason_id, reasons.label AS reasonLabel, reasons.description AS reasonDescription, notifications.reminder, notifications.last_android_id, notifications.modified ' +
			'FROM notifications INNER JOIN tasks on tasks.id = notifications.task_id INNER JOIN methods on methods.id = notifications.method_id INNER JOIN reasons ON reasons.id = notifications.reason_id ' +
			'WHERE notifications.user_id = ?',
			[userId], function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};
exports.getSubscriptions = getSubscriptions;

var updateSubscription = function(conn, userId, taskId, reasonId, methodId, reminder) {
	//INSERT INTO table (id, name, age) VALUES(1, "A", 19) ON DUPLICATE KEY UPDATE name=VALUES(name), age=VALUES(age)
	return new Promise(function(resolve, reject){
		conn.query(
			'INSERT INTO notifications (user_id, task_id, reason_id, method_id, reminder) ' +
			'VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE ' +
			'reason_id=VALUES(reason_id), method_id=VALUES(method_id), reminder=VALUES(reminder)',
			[userId, taskId, reasonId, methodId, reminder],
			function(err, rows, fields){
				if(err) {
					reject(err);
				} else if(rows.affectedRows === 0) {
					reject(new Error('no rows affected'));
				} else {
					resolve();
				}
			});
	});
};
exports.updateSubscription = updateSubscription;

var deleteSubscription = function(conn, userId, taskId) {
	return new Promise(function(resolve, reject){
		conn.query('DELETE FROM notifications WHERE user_id = ? AND task_id = ?', [userId, taskId], function(err, rows, fields){
			if(err) {
				reject(err);
			} else if(rows.affectedRows === 0) {
					reject(new Error('no rows affected'));
			} else {
				resolve();
			}
		});
	});
};
exports.deleteSubscription = deleteSubscription;

var deleteAndroidSubscriptions = function(conn, userId) {
	return new Promise(function(resolve, reject) {
		conn.query('DELETE FROM notifications WHERE user_id = ? AND method_id = 1', [userId], function(err, rows, fields){
			if(err) {
				reject(err);
			} else {
				resolve();
			}
		});
	});
};
exports.deleteAndroidSubscriptions = deleteAndroidSubscriptions;

var getTasks = function(conn, userId) {
	return new Promise(function(resolve, reject){
		conn.query(
			'SELECT tasks.id AS taskId, tasks.name AS taskName, tasks.periodic_hours, tasks.creator_user_id, notifications.reason_id, ' +
    			'notifications.method_id, notifications.reminder, notifications.last_android_id, notifications.modified AS notificationModified, ' +
    			'(notifications.method_id IS NOT NULL) as notification ' +
			'FROM participants ' +
				'JOIN tasks ON participants.task_id = tasks.id ' +
    			'LEFT JOIN notifications ON tasks.id = notifications.task_id ' +
    			'LEFT JOIN methods ON notifications.method_id = methods.id ' +
    			'LEFT JOIN reasons ON notifications.reason_id = reasons.id ' +
			'WHERE  participants.user_id = ?', 
			[userId], function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};
exports.getTasks = getTasks;

var getStatus = function(conn, taskId) {
	return new Promise(function(resolve, reject){
		conn.query(
			'SELECT users.id AS id, users.displayname AS name, IFNULL(counts.turns, 0) AS turns, (users.androidtoken IS NOT NULL) AS mobile ' + 
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
exports.getStatus = getStatus;

var getAll = function(conn, userId, taskId) {
	var results = {};
	return getTasks(conn, userId).then(function(tasks){
		results.tasks = tasks;
		if(taskId) {
			for(var i = 0; i < tasks.length; i++) {
				console.log(taskId, tasks[i].id);
				if(tasks[i].taskId == taskId) {
					results.taskid = parseInt(taskId);
					return Promise.all([getTurns(conn, taskId), getStatus(conn, taskId), getEnums(conn, 'methods'), getEnums(conn, 'reasons')]);
				}
			}
			log('tasks-turns-status invalid task id', taskId);
		}
		if(tasks.length) {
			results.taskid = tasks[0].taskId;
			return Promise.all([getTurns(conn, tasks[0].taskId), getStatus(conn, tasks[0].taskId), getEnums(conn, 'methods'), getEnums(conn, 'reasons')]);
		} else {
			results.taskId = 0;
			return [[],[]];
		}
	}).spread(function(turns, users, methods, reasons){
		results.turns = turns;
		results.users = users;
		results.methods = methods;
		results.reasons = reasons;
		return results;
	});
};
exports.getAll = getAll;

var getEnums = function(conn, table) {
	return new Promise(function(resolve, reject){
		conn.query('SELECT id, label, description FROM ' + table + ' ORDER BY id ASC', function(err, rows, fields){
			if(err) {
				reject(err);
			} else {
				resolve(rows);
			}
		});
	});
};
exports.getEnums = getEnums;

var deleteTurn = function(conn, turnId) {
	return new Promise(function(resolve, reject){
		conn.query('DELETE FROM turns WHERE id = ?', [turnId], function(err, rows, fields){
			if(err) {
				reject(err);
			} else {
				resolve();
			}
		});
	});
};
exports.deleteTurn = deleteTurn;

var saveAddress = function(conn, userId, ip, callback) {
	return new Promise(function(resolve, reject){
	    conn.query( 'INSERT INTO addresses SET ? ON DUPLICATE KEY UPDATE user_id = ?',
	    	[{user_id: userId, ip: ip}, userId], function(err, rows, fields) {
			if (err) {
				if(err.code === 'ER_DUP_ENTRY') {
					log('Already saved user %d address %s', userId, ip);
					resolve(userId);
				} else {
					log('ERROR while performing addresses query', err);
					reject(err);
				}
			} else {
				log('Saved user %d address %s', userId, ip);
				resolve(userId);
			}
		})
	});
};
exports.saveAddress = saveAddress;

var getUser = function(conn, ip, callback) {
	return new Promise(function(resolve, reject){
		conn.query('SELECT users.id, users.displayname as name FROM addresses JOIN users ON users.id = addresses.user_id WHERE ip = ?',
			[ip], function(err, rows, fields) {
				if(err) {
					log('ERROR while getting address', err);
					reject(err);
				} else {
					if(rows[0]) {
						log('Got user from address %s', ip, rows[0]);
						resolve(rows[0]);
					} else {
						log("ERROR Didn't find user for address %s", ip);
						reject();
					}
				}
		});
	});
};
exports.getUser = getUser;

var getUsers = function(conn) {
	return new Promise(function(resolve, reject){
		conn.query('SELECT id, displayname AS name FROM users ORDER BY displayname ASC', function(err, rows, fields) {
			if(err) {
				log('ERROR failed to get all users', err);
				reject(err);
			} else {
				resolve(rows);
			}
		});
	});
};
exports.getUsers = getUsers;

var takeTurn = function(conn, taskId, userid) {
	return new Promise(function(resolve, reject){
	    conn.query( 'INSERT INTO turns SET ?', {user_id: userid, task_id: taskId}, function(err, rows, fields) {
			if (err) {
				log('ERROR while performing turn query', err);
				reject(err);
			} else {
				resolve();
			}
	    });
	});
};
exports.takeTurn = takeTurn;

var getAndroidUsers = function(conn) {
	return new Promise(function(resolve, reject){
		conn.query('SELECT id, displayname AS name, androidtoken as token FROM users WHERE androidtoken IS NOT NULL', function(err, rows, fields) {
			if(err) {
				log('ERROR failed to get android users', err);
				reject(err);
			} else {
				resolve(rows);
			}
		});
	});
};
exports.getAndroidUsers = getAndroidUsers;

var setAndroidToken = function(conn, userid, token) {
	return new Promise(function(resolve, reject){
		conn.query('UPDATE users SET androidtoken = ? WHERE id = ?', [token, userid], function(err, rows, fields){
			if(err) {
				log('ERROR failed to set android token for user ' + userid, token);
				reject(err);
			} else {
				log('set android token for user ' + userid + ' to ' + token);
				resolve();
			}
		});
	});
};
exports.setAndroidToken = setAndroidToken;

var sendAndroidMessage = function(dataObject, token) {
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
		log('sending android message', data);
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
			log('ERROR failed to send android message', err);
			reject(err);
		});
	});
};
exports.sendAndroidMessage = sendAndroidMessage;
