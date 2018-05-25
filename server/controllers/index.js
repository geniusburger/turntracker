var https = require('https');
var Promise = require('bluebird');
var log = require('debug')('turntracker:index');
var mysql = require('mysql');

var db = require('./db');
var config = require('../config');

var getTurns = function(conn, taskId, limit) {
	return new Promise(function(resolve, reject){
		conn.query(
	    	'SELECT users.displayname AS name, turns.taken AS date, turns.inserted, users.id as userid, turns.id as turnid ' +
	    	'FROM tasks INNER JOIN turns on turns.task_id = tasks.id INNER JOIN USERS ON turns.user_id = users.id ' +
			'WHERE tasks.id = ? ORDER BY turns.taken DESC ' +
			(typeof limit == 'number' && limit > 0 ? ('LIMIT ' + limit) : ''),
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

var getAndroidSubscriptions = function(conn, taskId, /* the user whose turn it is */ userId) {
	return new Promise(function(resolve, reject){
		conn.query(
			'SELECT notifications.user_id, users.androidtoken, last_android_id, reasons.label, tasks.name AS task ' +
			'FROM notifications JOIN reasons on reasons.id = notifications.reason_id JOIN users on users.id = notifications.user_id JOIN tasks on tasks.id = notifications.task_id ' +
			'WHERE task_id = ? AND method_id = 1 AND users.androidtoken IS NOT NULL AND ( reason_id = 1 OR (reason_id = 2 and user_id = ?) )',
			[taskId, userId], function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};
exports.getAndroidSubscriptions = getAndroidSubscriptions;

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

var getTasks = function(conn, userId, taskId) {
	return new Promise(function(resolve, reject){
		conn.query(
			'SELECT tasks.id AS taskId, tasks.name AS taskName, tasks.periodic_hours, tasks.creator_user_id, ' +
				'tasks.modified as taskModified, tasks.inserted as taskInserted, notifications.reason_id, ' +
    			'notifications.method_id, notifications.reminder, notifications.last_android_id, notifications.modified AS notificationModified, ' +
    			'(notifications.method_id IS NOT NULL) as notification ' +
			'FROM participants ' +
				'JOIN tasks ON participants.task_id = tasks.id ' +
    			'LEFT JOIN notifications ON tasks.id = notifications.task_id and notifications.user_id = ?' +
    			'LEFT JOIN methods ON notifications.method_id = methods.id ' +
    			'LEFT JOIN reasons ON notifications.reason_id = reasons.id ' +
			'WHERE  participants.user_id = ?' + (taskId ? ' and tasks.id = ?' : ''), 
			[userId, userId, taskId], function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};
exports.getTasks = getTasks;

var getAllReminders = function(conn) {
	return new Promise(function(resolve, reject){
		conn.query(
			'SELECT notifications.user_id, notifications.task_id, tasks.periodic_hours, tasks.name, users.androidtoken ' +
			'FROM notifications JOIN users ON users.id = notifications.user_id JOIN tasks ON tasks.id = notifications.task_id ' +
			'WHERE notifications.reminder = 1 and tasks.periodic_hours > 0 ' +
			'ORDER BY notifications.task_id, notifications.user_id',
			[], function(err, rows){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};

var sendAllPendingReminders = function(conn) {
	return getAllReminders(conn).then(function(reminders){
		log(reminders);
		// group all reminders by task
		return (reminders || []).reduce(function(groups, reminder){
			if(groups.length && groups[groups.length-1][0].task_id === reminder.task_id) {
				groups[groups.length-1].push(reminder);
			} else {
				groups.push([reminder]);
			}
			return groups;
		}, []);
	}).then(function(groups){
		log(groups);
		// attach status of the next user to each reminder group/array
		return Promise.all(groups.map(function(group){
			return getStatus(conn, group[0].task_id).then(function(status){
				group.statusOfNext = status[0];
				return group;
			});
		}));
	}).then(function(reminderGroups){
		log(reminderGroups);
		// 
		return reminderGroups.map(function(reminderGroup){
			// filter out reminders that aren't for the next user
			return reminderGroup.filter(function(reminder){
				return reminder.user_id == reminderGroup.statusOfNext.id;
			});
		}).filter(function(reminderGroup){
			// filter out empty reminder groups
			return reminderGroup.length;
		}).map(function(reminderGroup){
			// convert from array to single reminder
			var reminder = reminderGroup[0];
			reminder.lastTurn = reminderGroup.statusOfNext
			return reminderGroup[0];
		});
	}).then(function(reminders){
		// get most recent turn and attach it to the reminder
		return Promise.all(reminders.map(function(reminder){
			return getTurns(conn, reminder.task_id, 1).then(function(turns){
				reminder.lastTurn = turns.length ? turns[0] : null;
				return reminder;
			});
		})).then(function(reminders){
			return reminders.filter(function(reminder){
				// filter out reminders that are not overdue
				// TODO need to store date/time in a consistent way so calculations can be done across time zones. For now, assume the same time zone
				return reminder.lastTurn && (reminder.lastTurn.date.getTime() + (reminder.periodic_hours * 3600000) <= Date.now());
			});
		});
	}).then(function(reminders){
		log(reminders);
		// send a message for each reminder
		return reminders.map(function(reminder){
			return sendAndroidMessage({
				message: 'Reminder: Take a turn for ' + reminder.name,
				taskId: reminder.task_id,
				userId: reminder.user_id
			}, reminder.androidtoken).then(function(fcmResponse){
				log('sent ' + fcmResponse.success + ' reminders');
				return fcmResponse.results.map(function(result){
					return { userId: reminder.user_id, token: result.registration_id};
				}).filter(function(update){
					return update.token;
				});
			});
		});
	}).then(function(sentMessages){
		log(sentMessages);
		return sentMessages.map(function(update){
			log('updating token');
			return setAndroidTokens(conn, [update]);
		});
	});
};
exports.sendAllPendingReminders = sendAllPendingReminders;

var getStatus = function(conn, taskId) {
	return new Promise(function(resolve, reject){
		conn.query(
			'SELECT users.id AS id, users.displayname AS name, IFNULL(counts.turns, 0) AS turns, (users.androidtoken IS NOT NULL) AS mobile ' + 
			'FROM participants JOIN users on participants.user_id = users.id LEFT JOIN ( ' +
				'SELECT turns.user_id, count(*) as turns, turns.taken ' +
				'FROM turns WHERE turns.task_id = ? ' +
				'GROUP BY turns.user_id ORDER BY turns.taken ASC ' +
			') counts on participants.user_id = counts.user_id ' +
			'WHERE  participants.task_id = ? ' +
			'ORDER by turns ASC, counts.taken ASC',
			[taskId,taskId], function(err, rows){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};
exports.getStatus = getStatus;

var getTaskUsers = function(conn, taskId) {
	return new Promise(function(resolve, reject){
		conn.query(
			'SELECT users.id AS id, users.displayname AS name, (participants.task_id IS NOT NULL) AS selected  ' +
			'FROM users LEFT JOIN participants on participants.user_id = users.id AND participants.task_id = ?',
			[taskId], function(err, rows){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};
exports.getTaskUsers = getTaskUsers;

var getAll = function(conn, userId, taskId) {
	// TODO, need to include task info
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

var simpleDeletePromise = function(conn, query, params) {
	return new Promisefunction(resolve, reject) {
		conn.query(query, params, function(err, rows, fields){
			if(err) {
				reject(err);
			} else {
				resolve();
			}
		});
	};
};

var deleteTask = function(conn, taskId) {
	// delete from participants where task_id = ?;
	// delete from turns where task_id = ?;
	// delete from notifications where task_id = ?;
	// delete from tasks where id = ?;

	return new Promise(function(resolve, reject){
		conn.beginTransaction(function(transactionError){
			if(transactionError) throw transactionError;
			return simpleDeletePromise(conn, 'delete from participants where task_id = ?', [taskid])
			.then(function(){
				return simpleDeletePromise(conn, 'delete from turns where task_id = ?', [taskId]);
			}).then(function(){
				return simpleDeletePromise(conn, 'delete from notifications where task_id = ?', [taskId]);
			}).then(function(){
				return simpleDeletePromise(conn,'delete from tasks where id = ?', [taskId]);
			}).catch(function(err){
				log('ERROR rolling back delete task transaction')
				return conn.rollback(function() {
				  throw err;
			  });
			});
		});
	});

};
exports.deleteTask = deleteTask;

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

var takeTurn = function(conn, taskId, userid, dateTaken) {
	return new Promise(function(resolve, reject){

		var fields = {user_id: userid, task_id: taskId};
		if(typeof dateTaken === 'number') {
			fields.taken = new Date(dateTaken);
		}
	    conn.query( 'INSERT INTO turns SET ?', fields, function(err, rows, fields) {
			if (err) {
				log('ERROR while performing turn query', err);
				reject(err);
			} else {
				resolve(rows.insertId);
			}
	    });
	});
};
exports.takeTurn = takeTurn;

var minNameLength = 5;
var maxNameLength = 50;
var createUser = function(conn, username, displayname) {
	return new Promise(function(resolve, reject){
		username = username.trim();
		displayname = displayname.trim();
		if(username.length < minNameLength || username.length > maxNameLength) {
			reject(new Error('username must be ' + minNameLength + ' to ' + maxNameLength + ' letters long'));
		} else if(username.match(/\s/)) {
			reject(new Error('username cannot contain white space'));
		} else if(displayname.length < minNameLength || displayname.length > maxNameLength) {
			reject(new Error('displayname must be ' + minNameLength + ' to ' + maxNameLength + ' letters long'));
		} else {
			conn.query('INSERT INTO users SET ?', {username: username, displayname: displayname}, function(err, rows, fields){
				if(err) {
					log('ERROR while performing create user query', err);
					reject(err);
				} else {
					resolve(rows.insertId);
				}
			});
		}
	});
};
exports.createUser = createUser;

var createOrEditTask = function(conn, name, hours, creator, id) {
	return new Promise(function(resolve, reject){
		name = name.trim();
		if(name.length < 1) {
			reject(new Error("name can't be empty"));
		} else if(hours < 0) {
			reject(new Error("hours can't be negative"));
		} else if(id){
			conn.query('UPDATE tasks SET ? WHERE id = ?', [{name: name, periodic_hours: hours, creator_user_id: creator}, id], function(err, rows, fields){
				if(err) {
					log('ERROR while performing update task query', err);
					reject(err);
				} else {
					resolve(id);
				}
			});
		} else {
			conn.query('INSERT INTO tasks SET ?', {name: name, periodic_hours: hours, creator_user_id: creator}, function(err, rows, fields){
				if(err) {
					log('ERROR while performing create task query', err, fields);
					reject(err);
				} else {
					resolve(rows.insertId);
				}
			});
		}
	});
};
exports.createOrEditTask = createOrEditTask;

var deleteParticipants = function(conn, taskId, userIds) {
	return new Promise(function(resolve, reject){
		if(userIds.length === 0) {
			resolve();
		} else {
			var participants = userIds.map(function(user){
				return [taskId, user];
			});
			//DELETE FROM table WHERE (col1,col2) IN ((1,2),(3,4),(5,6))
			conn.query('DELETE FROM participants WHERE (task_id, user_id) IN ?', [[participants]], function(err, rows){
				if(err) {
					log('ERROR while clearing participants', err);
					reject(err);
				} else {
					log("cleared participants", rows);
					resolve();
				}
			});
		}
	});
};

var getParticipants = function(conn, taskId) {
	return new Promise(function(resolve, reject){
		conn.query('SELECT user_id FROM participants WHERE task_id = ?', [taskId], function(err, rows){
			if(err) {
				log('ERROR while performing get participants query', err);
				reject(err);
			} else {
				resolve(rows.map(function(user){ return user.user_id; }));
			}
		});
	});
};

var addParticipants = function(conn, taskId, userIds) {
	return new Promise(function(resolve, reject){
		if(userIds.length === 0) {
			resolve();
		} else {
			var participants = userIds.map(function(user){
				return [taskId, user];
			});
			conn.query('INSERT INTO participants (task_id, user_id) VALUES ?', [participants], function(err, rows){
				if(err) {
					log('ERROR while performing add participants query', err);
					reject(err);
				} else {
					resolve();
				}
			});
		}
	});
};

var setParticipants = function(conn, taskId, userIds) {
	return getParticipants(conn, taskId).then(function(existingUserIds){
		var toDelete = existingUserIds.filter(function(id){ return userIds.indexOf(id) < 0; });
		var toAdd = userIds.filter(function(id){ return existingUserIds.indexOf(id) < 0; });
		// log("userIds: ", userIds);
		// log("existing: ", existingUserIds);
		log("participants to delete: ", toDelete);
		log("participants to add: ", toAdd);
		return deleteParticipants(conn, taskId, toDelete).then(function(){
			return addParticipants(conn, taskId, toAdd);
		});
	});
};
exports.setParticipants = setParticipants;

var getAndroidUsers = function(conn, userIds) {
	return new Promise(function(resolve, reject){
		var fields = userIds || [];
		if(!Array.isArray(fields)) {
			fields = [fields];
		}
		var query = 'SELECT id, displayname AS name, androidtoken as token FROM users WHERE androidtoken IS NOT NULL';
		if(fields.length) {
			query += ' AND users.id IN (';
			query += fields.map(function(){return '?';}).join(',');
			query += ')';
		}
		conn.query(query, fields, function(err, rows, fields) {
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

var getUser = function(conn, username){
	return new Promise(function(resolve, reject){
		conn.query('SELECT id, username, displayname, androidtoken FROM users WHERE username = ?', [username], function(err, rows, fields){
			if(err) {
				log("ERROR failed to get user '" + username + "'", err);
				reject(err);
			} else {
				resolve(rows);
			}
		});
	});
};
exports.getUser = getUser;

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

var setAndroidTokens = function(conn, updates) {
	// { userId: nextTurnUser.id, token: result.registration_id};
	// UPDATE table_users
 //    SET cod_user = (case when user_role = 'student' then '622057'
 //                         when user_role = 'assistant' then '2913659'
 //                         when user_role = 'admin' then '6160230'
 //                    end),
 //        date = '12082014'
 //    WHERE user_role in ('student', 'assistant', 'admin') AND
 //          cod_office = '17389551';
 	var query = 'UPDATE users SET androidtoken = ( CASE ';
 	var fields = [];
 	log('updates', updates);
 	var ids = updates.map(function(up){
 		query += 'WHEN id = ? then ? ';
 		fields.push(up.userId, up.token);
 		return up.userId;
 	});
 	query += 'END ) WHERE id IN (' + ids.map(function(){return '?'}).join(', ') + ')';
	fields = fields.concat(ids);
	log('update token query', query, 'values', fields);
	return new Promise(function(resolve, reject){
		conn.query(query, fields, function(err, rows, fields){
			if(err) {
				log('ERROR failed to update android tokens');
				reject(err);
			} else {
				log('updated android tokens');
				resolve();
			}
		});
	});
};

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
			hostname: 'fcm.googleapis.com',
			port: 443,
			path: '/fcm/send',
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'Content-Length': dataString.length,
				"Authorization": "key=" + config.fcm.apiKey
			}
		}, function(res) {
			res.setEncoding('utf8');
			log('response code: ' + res.statusCode);
			log('response headers', res.headers);
			res.on('data', function (chunk) {
				resolve(chunk);
			});
		});

		// post the data
		req.write(dataString);
		req.on('error', function(err){
			log('ERROR failed to send android message', err);
			reject(err);
		});
		req.end();
	}).then(function(jsonString){
		try{		
			return JSON.parse(jsonString);
		} catch(e) {
			log('ERROR failed to parse json response: ' + jsonString);
			throw e;
		}
	});
};
exports.sendAndroidMessage = sendAndroidMessage;
