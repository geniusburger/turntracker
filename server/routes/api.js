var express = require('express');
var router = express.Router();
var log = require('debug')('turntracker:api');
var Promise = require('bluebird');
var using = Promise.using;
var db = require('../controllers/db');
var index = require('../controllers/index');
var ApiError = require('./ApiError');

router.get('/tasks-turns-status', function(req, res, next){
	using(db.getConnection(), function(conn) {
		return index.getAll(conn, req.query.userid, req.query.taskid);
	}).then(function(results){
		res.json(results);
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get tasks/turns/status'));
	});
});

router.get('/subscriptions', function(req, res, next){
	using(db.getConnection(), function(conn){
		return index.getSubscriptions(conn, req.query.user_id);
	}).then(function(results){
		res.json(results);
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get subscriptions'));
	});
});

router.get('/tasks', function(req, res, next){
	using(db.getConnection(), function(conn) {
		return index.getTasks(conn, req.query.userid);
	}).then(function(tasks){
		res.json({tasks: tasks});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get tasks'));
	});
});

router.get('/turns', function(req, res, next) {
	using(db.getConnection(), function(conn) {
		var userPromise = index.getUser(conn, req.ip);
		var turnsPromise = userPromise.then(function(){
			return index.getTurns(conn, req.query.id);
		});
		return Promise.all([userPromise, turnsPromise]);
	}).then(function(results){
		res.json({user: results[0], turns: results[1]});
	}).catch(function(err) {
		next(new ApiError(err, 'Failed to get turns', {user: {id: -1, name: ''}}));
	});
});

router.get('/users', function(req, res, next){
	using(db.getConnection(), function(conn) {
		return index.getUsers(conn);
	}).then(function(rows){
		res.json({users: rows});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get users'));
	});
});

router.get('/user', function(req, res, next) {
	using(db.getConnection(), function(conn){
		return index.getUser(conn, req.query.username);
	}).then(function(rows){
		res.json({user: rows[0]});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get users'));
	});
});

router.get('/status', function(req, res, next) {
	using(db.getConnection(), function(conn) {
		return index.getStatus(conn, req.query.id);
	}).then(function(rows){
		res.json({users: rows});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get status'));
	});
});

router.put('/android', function(req, res, next){
	using(db.getConnection(), function(conn) {
		return index.setAndroidToken(conn, req.body.user_id, req.body.token);
	}).then(function(){
		res.json({success: true});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to set android key'));
	});
});

router.delete('/android', function(req, res, next){
	using(db.getConnection(), function(conn) {
		return index.setAndroidToken(conn, req.query.user_id, null)
		.then(function onDeleteSuccess(){
			return index.deleteAndroidSubscriptions(conn, req.query.user_id);
		}, function onDeleteError(err){
			throw new ApiError(err, 'Failaed to clear android token');
		});
	}).then(function(){
		res.json({success: true});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to clear android notifications after deleting android key'));
	});
});

router.delete('/turn', function(req, res, next){
	using(db.getConnection(), function(conn) {
		return index.deleteTurn(conn, req.query.turn_id).then(function onDeleteSuccess(){
			return index.getAll(conn, req.query.user_id, req.query.task_id);
		}, function onDeleteError(err){
			throw new ApiError(err, 'Failed to delete turn');
		}).catch(function onGetAllError(err){
			if(err instanceof ApiError) {
				throw err;
			}
			throw new ApiError(err, 'Failed to get all after deleting turn');
		});
	}).then(function(results){
		res.json(results);
	}).catch(function(err){
		next(err);
	});
});

router.put('/subscription', function(req, res, next) {
	using(db.getConnection(), function(conn) {
		return index.updateSubscription(conn, req.body.userId, req.body.taskId, req.body.note.reason_id, req.body.note.method_id, req.body.note.reminder);
	}).then(function(results){
		res.json({success: true});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to insert/update subscription'));
	});
});

router.delete('/subscription', function(req, res, next) {
	using(db.getConnection(), function(conn){
		return index.deleteSubscription(conn, req.query.userId, req.query.taskId);
	}).then(function(results){
		res.json({success: true});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to delete subscription'));
	});
});

router.get('/turns-status', function(req, res, next) {
	using(db.getConnection(), function(conn) {
		return Promise.all([index.getTurns(conn, req.query.task_id), index.getStatus(conn, req.query.task_id)]);
	}).spread(function(turns, users){
		res.json({turns: turns, users: users, taskid: parseInt(req.query.task_id)});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get turns/status'));
	});
});

router.post('/user', function(req, res, next){
	using(db.getConnection(), function(conn){
		return index.createUser(conn, req.body.username, req.body.displayname);
	}).then(function(userId){
		res.json({success: true, user_id: userId});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to create user'));
	});
});

router.post('/task', function(req, res, next){
	using(db.getConnection(), function(conn){

		return new Promise(function(resolve, reject){
			conn.beginTransaction(function(err) {
	  			if (err) {
	  				throw err;
	  			}
	  			return index.createOrEditTask(conn, req.body.name, req.body.hours, req.body.creator, req.body.id)
	  			.then(function(taskId){
	  				return Promise.all([taskId, index.setParticipants(conn, taskId, req.body.users)]);
	  			}).spread(function(taskId){
					return conn.commit(function(err) {
						if (err) {
							return conn.rollback(function() {
								throw err;
							});
						}
						resolve(taskId);
					});
	  			}).catch(function(err){
	  				return conn.rollback(function() {
						throw err;
					});
	  			});
			});
		});
	}).then(function(taskId){
		res.json({success: true, task_id: taskId});
	}).catch(function(err){
		next(new ApiError(err, req.body.id ? 'Failed to edit task' : 'Failed to create task'));
	});
});

router.post('/turn', function(req, res, next) {
	using(db.getConnection(), function(conn) {
		var turnTakerUserId;
		return (req.body.user_id ? index.saveAddress(conn, req.body.user_id, req.ip) : index.getUser(conn, req.ip)).then(function(user){
			turnTakerUserId = typeof user === 'object' ? user.id : user;
			return index.takeTurn(conn, req.body.task_id, turnTakerUserId, req.body.date);
		}).then(function(turnId){
			return Promise.all([turnId, index.getTurns(conn, req.body.task_id), index.getStatus(conn, req.body.task_id)]);
		}, function(err){
			// todo this could also be an error from getting the user or saving the address
			log('ERROR failed to take turn', err);
			return Promise.all([0, index.getTurns(conn, req.body.task_id), index.getStatus(conn, req.body.task_id)]);
		}).spread(function(turnId, turns, users){
			// send notifications
			var nextTurnUser = users[0];
			var turnTakerUserName = users.reduce(function(prev, user){
				return user.id === turnTakerUserId ? user.name : prev;
			}, '?');

			var notesPromise = index.getAndroidSubscriptions(conn, req.body.task_id, nextTurnUser.id)
			.then(function(allNotes){
				if(allNotes.length) {
					var nextTurnNote;
					var otherNotes = allNotes.filter(function(note){
						if(note.user_id === nextTurnUser.id) {
							nextTurnNote = note;
							return false; // send a different message to the person whose turn it now is
						}
						return note.user_id !== turnTakerUserId; // don't send a notification to the person that just took a turn
					});
					var otherTokens = otherNotes.map(function(note){
						return note.androidtoken;
					});
					var extractRegistrationIds = function(gcmResults, registrationIds) {
						gcmResults.forEach(function(gcmRes){
							if(gcmRes.registration_id) {
								registrationIds.push()
							}
						});
					};
					if(otherTokens.length) {
						var othersPromise = index.sendAndroidMessage({
							message: turnTakerUserName + ' just took a turn for ' + otherNotes[0].task + ', ' + nextTurnUser.name + ' is next'
						}, otherTokens).then(function(gcmResponse){
							log('sent ' + gcmResponse.success + ' out of ' + otherTokens.length + ' other notes', typeof gcmResponse, gcmResponse);
							return gcmResponse.results.map(function(result, i){
								return { userId: otherNotes[i].user_id, token: result.registration_id};
							}).filter(function(update){
								return update.token;
							});
							// todo - check results for cononical IDs, success/failure, and message ID
							// table 5 at https://developers.google.com/cloud-messaging/http-server-ref
							// what is the point of saving the msg IDs?
							// should check for errors
						});
					}
					if(nextTurnNote) {
						var nextTurnPromise = index.sendAndroidMessage({
							message: turnTakerUserName + ' just took a turn for ' + nextTurnNote.task + ', you are next'
						}, nextTurnNote.androidtoken).then(function(gcmResponse){
							log('sent ' + gcmResponse.success + ' out of 1 next notes');
							return gcmResponse.results.map(function(result){
								return { userId: nextTurnUser.id, token: result.registration_id};
							}).filter(function(update){
								return update.token;
							});
						});
					}
					return Promise.all([othersPromise || [], nextTurnPromise || []])
					.spread(function(otherUpdates, nextUpdates){
						var updates = otherUpdates.concat(nextUpdates);
						if(updates.length) {
							log('updating ' + updates.length + ' tokens');
							return index.setAndroidTokens(conn, updates);
						}
					});
				}
			});
			return Promise.all([turnId, turns, users, notesPromise]);
		});
	}).spread(function(turnId, turns, users){
		res.json({turnId: turnId, turns: turns, users: users});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to take turn'));
	});
});

router.post('/notify', function(req, res, next) {
	using(db.getConnection(), function(conn) {
		return index.getAndroidUsers(conn).then(function(rows){
			log('users', rows);
			if(Array.isArray(rows) && rows.length) {
				return rows.map(function(row){
					return row.token;
				});
			}
			return null;
		}).catch(function(err){
			return null;
		});
	}).then(function(token){
		return index.sendAndroidMessage({
			message: 'Android message from TT'
		}, token);
	}).then(function(jsonResults){
		res.json(jsonResults);
	}).catch(function(err){
		next(new ApiError(err, 'Failed to notify'));
	});
});

router.use(function(req, res, next) {
    next(new ApiError({status: 404}, 'Not Found'));
});

module.exports = router;
