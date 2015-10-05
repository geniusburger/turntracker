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
		if(req.query.user_id) {
			// todo, no need to lookup id, just need to get user details?
		}
		var userPromise =index.getUser(conn, req.ip);
		var listPromise = userPromise.then(function(){
			return index.getTurns(conn, req.query.task_id);
		});
		return Promise.all([userPromise, listPromise, index.getStatus(conn, req.query.task_id)]);
	}).then(function(results){
		res.json({user: results[0], turns: results[1], users: results[2], taskid: parseInt(req.query.task_id)});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get turns/status'));
	});
});

router.post('/turn', function(req, res, next) {
	using(db.getConnection(), function(conn) {
		var turnTakerUserId;
		return (req.body.user_id ? index.saveAddress(conn, req.body.user_id, req.ip) : index.getUser(conn, req.ip)).then(function(user){
			turnTakerUserId = typeof user === 'object' ? user.id : user;
			return index.takeTurn(conn, req.body.task_id, turnTakerUserId);
		}).then(function(){
			return Promise.all([index.getTurns(conn, req.body.task_id), index.getStatus(conn, req.body.task_id)]);
		}, function(){
			// todo this could also be an error from getting the user or saving the address
			log('ERROR failed to take turn', err);
			return Promise.all([index.getTurns(conn, req.body.task_id), index.getStatus(conn, req.body.task_id)]);
		}).then(function(results){
			// send notifications
			var users = results[1];
			var newTurnUser = users[0];
			var turnTakerUserName = users.reduce(function(prev, user){
				return user.id === turnTakerUserId ? user.name : prev;
			}, '?');

			var notesPromise = index.getAndroidSubscriptions(conn, req.body.task_id, newTurnUser.id)
			.then(function(notes){
				if(notes.length) {
					var newTurnNote;
					var tokens = notes.filter(function(note){
						if(note.user_id === newTurnUser.id) {
							newTurnNote = note;
							return false; // send a different message to the person whose turn it now is
						}
						return note.user_id !== turnTakerUserId; // don't send a notification to the person that just took a turn
					}).map(function(note){
						return note.androidtoken;
					});
					if(tokens.length) {
						var othersPromise = index.sendAndroidMessage({
							message: turnTakerUserName + ' just took a turn for ' + notes[0].task + ', next is ' + newTurnUser.name
						}, tokens).then(function(results){
							console.log('others results', results);
							// todo - check results for cononical IDs, success/failure, and message ID
							// table 5 at https://developers.google.com/cloud-messaging/http-server-ref
						});
					}
					if(newTurnNote) {
						var newTurnPromise = index.sendAndroidMessage({
							message: turnTakerUserName + ' just took a turn for ' + notes[0].task + ", it's your turn next"
						}, newTurnNote.androidtoken).then(function(results){
							console.log('next results', results);
							// todo - check results for cononical IDs, success/failure, and message ID
							// table 5 at https://developers.google.com/cloud-messaging/http-server-ref
						});
					}
					return Promise.all([othersPromise || Promise.resolve(), newTurnPromise || Promise.resolve()]);
				}
				return Promise.resolve();
				// todo - update last token in DB and in GCM
			});
			return results.concat(notesPromise);
		});
	}).spread(function(turns, users){
		res.json({turns: turns, users: users});
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
	}).then(function(jsonString){
		return JSON.parse(jsonString);
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
