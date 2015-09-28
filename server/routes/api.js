var express = require('express');
var router = express.Router();
var log = require('debug')('turntracker:api');
var index = require('../controllers/index');
var ApiError = require('./ApiError');

router.get('/tasks-turns-status', function(req, res, next){
	index.getAll(req.query.userid, req.query.taskid).then(function(results){
		res.json(results);
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get tasks/turns/status'));
	});
});

router.get('/tasks', function(req, res, next){
	index.getTasks(req.query.userid).then(function(tasks){
		res.json({tasks: tasks});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get tasks'));
	});
});

router.get('/turns', function(req, res, next) {
	var userPromise = index.getUser(req.ip);
	var turnsPromise = userPromise.then(function(){
		return index.getTurns(req.query.id);
	});

	Promise.all([userPromise, turnsPromise]).then(function(results){
		res.json({user: results[0], turns: results[1]});
	}).catch(function(err) {
		next(new ApiError(err, 'Failed to get turns', {user: {id: -1, name: ''}}));
	});
});

router.get('/users', function(req, res, next){
	index.getUsers().then(function(rows){
		res.json({users: rows});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get users'));
	});
});

router.get('/status', function(req, res, next) {
	index.getStatus(req.query.id).then(function(rows){
		res.json({users: rows});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get status'));
	});
});

router.put('/android', function(req, res, next){
	index.setAndroidToken(req.body.user_id, req.body.token).then(function(){
		res.json({success: true});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to set android key'));
	});
});

router.delete('/android', function(req, res, next){
	/// @todo Should also delete subscriptions once they exist
	index.setAndroidToken(req.query.user_id, null).then(function(){
		res.json({success: true});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to delete android key'));
	});
});

router.delete('/turn', function(req, res, next){
	index.deleteTurn(req.query.turn_id).then(function(){
		return index.getAll(req.query.user_id, req.query.task_id);
	}, function(err){
		throw new ApiError(err, 'Failed to delete turn');
	}).then(function(results){
		res.json(results);
	}, function(err){
		throw new ApiError(err, 'Failed to get all after deleting turn');
	}).catch(function(err){
		next(err);
	});
});

router.get('/turns-status', function(req, res, next) {
	if(req.query.user_id) {
		// todo, no need to lookup id, just need to get user details?
	}
	var userPromise =index.getUser(req.ip);
	var listPromise = userPromise.then(function(){
		return index.getTurns(req.query.task_id);
	});
	Promise.all([userPromise, listPromise, index.getStatus(req.query.task_id)]).then(function(results){
		res.json({user: results[0], turns: results[1], users: results[2], taskid: parseInt(req.query.task_id)});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to get turns/status'));
	});
});

router.post('/turn', function(req, res, next) {
	(req.body.user_id ? index.saveAddress(req.body.user_id, req.ip) : index.getUser(req.ip)).then(function(user){
		return index.takeTurn(req.body.task_id, typeof user === 'object' ? user.id : user);
	}).then(function(){
		return Promise.all([index.getTurns(req.body.task_id), index.getStatus(req.body.task_id)]);
	}, function(){
		log('ERROR failed to take turn', err);
		return Promise.all([index.getTurns(req.body.task_id), index.getStatus(req.body.task_id)]);
	}).then(function(results){
		res.json({turns: results[0], users: results[1]});
	}).catch(function(err){
		next(new ApiError(err, 'Failed to take turn'));
	});
});

router.post('/notify', function(req, res, next) {
	index.getAndroidUsers().then(function(rows){
		log('users', rows);
		if(Array.isArray(rows) && rows.length) {
			return rows.map(function(row){
				return row.token;
			});
		}
		return null;
	}).catch(function(err){
		return null;
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
