var express = require('express');
var router = express.Router();
var debug = require('debug')('turntracker:api');
var db = require('../controllers/index');

router.get('/tasks-turns-status', function(req,res){
	var results = {};
	db.getTasks(req.query.userid).then(function(tasks){
		results.tasks = tasks;
		if(req.query.taskid) {
			for(var i = 0; i < tasks.length; i++) {
				if(tasks[i].id === req.query.taskid) {
					results.taskid = parseInt(req.query.taskid);
					return Promise.all([db.getTurns(req.query.taskid), db.getStatus(req.query.taskid)]);
				}
			}
			log.warn('tasks-turns-status invalid task id', req.query.taskid);
		}
		if(tasks.length) {
			results.taskid = tasks[0].id;
			return Promise.all([db.getTurns(tasks[0].id),db.getStatus(tasks[0].id)]);
		} else {
			results.taskId = 0;
			return [[],[]];
		}
	}).then(function(data){
		results.turns = data[0];
		results.users = data[1];
		res.json(results);
	}).catch(function(err){
		console.error('tasks-turns-status error', err, err.stack);
		res.json({error: 'query error'});
	});
});

router.get('/tasks', function(req,res){
	db.getTasks(req.query.userid).then(function(tasks){
		res.json({tasks: tasks});
	}).catch(function(err){
		console.error('tasks error', err);
		res.json({error: 'tasks error'});
	});
});

router.get('/turns', function(req,res) {
	var userPromise = db.getUser(req.ip);
	var turnsPromise = userPromise.then(function(){
		return db.getTurns(req.query.id);
	});

	Promise.all([userPromise, turnsPromise]).then(function(results){
		res.json({user: results[0], turns: results[1]});
	}).catch(function(err) {
		console.error('turns error', err);
		res.json({user: {id: -1, name: ''}, error: 'query error'});
	});
});

router.get('/users', function(req,res){
	db.getUsers().then(function(rows){
		res.json({users: rows});
	}).catch(function(err){
		res.json({error: err});
	});
});

router.get('/status', function(req,res) {
	db.getStatus(req.query.id).then(function(rows){
		res.json({users: rows});
	}).catch(function(err){
		console.error('Error while performing status query', err);
		res.json({error: 'Query Error'});
	});
});

router.put('/android', function(req,res){
	db.setAndroidToken(req.body.user_id, req.body.token).then(function(){
		res.json({success: true});
	}).catch(function(err){
		res.json({error: err});
	});
});

router.delete('/android', function(req,res){
	/// @todo Should also delete subscriptions once they exist
	db.setAndroidToken(userid, null).then(function(){
		res.json({success: true});
	}).catch(function(err){
		res.json({error: err});
	});
});

router.get('/turns-status', function(req,res) {
	var userPromise = db.getUser(req.ip);
	var listPromise = userPromise.then(function(){
		return db.getTurns(req.query.id);
	});
	Promise.all([userPromise, listPromise, db.getStatus(req.query.id)]).then(function(results){
		res.json({user: results[0], turns: results[1], users: results[2], taskid: parseInt(req.query.id)});
	}).catch(function(err){
		console.error('turns-status error', err);
		res.json({error: 'query error'});
	});
});

router.post('/turn', function(req,res) {
	(req.body.user_id ? db.saveAddress(req.body.user_id, req.ip) : db.getUser(req.ip)).then(function(user){
		return db.takeTurn(req.body.task_id, typeof user === 'object' ? user.id : user);
	}).then(function(){
		return Promise.all([db.getTurns(req.body.task_id), db.getStatus(req.body.task_id)]);
	}, function(){
		console.error('failed to take turn', err);
		return Promise.all([db.getTurns(req.body.task_id), db.getStatus(req.body.task_id)]);
	}).then(function(results){
		res.json({turns: results[0], users: results[1]});
	}).catch(function(err){
		console.error('turn error', err);
		res.json({error: 'query error'});
	});
});

router.post('/notify', function(req,res) {
	db.getAndroidUsers().then(function(rows){
		console.log('users', rows);
		if(Array.isArray(rows) && rows.length) {
			return rows.map(function(row){
				return row.token;
			});
		}
		return null;
	}).catch(function(err){
		return null;
	}).then(function(token){
		return db.sendAndroidMessage({
			message: 'Android message from TT'
		}, token);
	}).then(function(jsonString){
		return JSON.parse(jsonString);
	}).then(function(jsonResults){
		res.json(jsonResults);
	}).catch(function(err){
		console.error(err,err.stack);
		res.json({error: err});
	});
});

module.exports = router;
