var extend = require('util')._extend;
var config = require('./config.js');
var express = require('express');
var mysql = require('mysql');
var minimist = require('minimist');
var bodyParser = require('body-parser');

var options = minimist(process.argv.slice(2), {
  boolean: 'live',
  default: { live: false }
});

const PORT = 3000;

var db = mysql.createConnection( extend( config.database.test, {} ) );

db.connect(function(err){
	if(!err) {
		console.log("Database is connected ... \n\n");  
	} else {
		console.error("Error connecting database ... \n\n");  
	}
});

var app = express();

if(options.live)
{
	app.use(require('connect-livereload')({
		port: 35729
	}));
}

app.use(express.static('build'));
app.use(bodyParser.json());       // to support JSON-encoded bodies
//app.use(bodyParser.urlencoded({ extended: true }));	// to support URL-encoded bodies

var getList = function(id, callback) {
	db.query(
    	'SELECT users.displayname AS name, turns.inserted AS date, users.id ' +
    	'FROM tasks INNER JOIN turns on turns.task_id = tasks.id INNER JOIN USERS ON turns.user_id = users.id ' +
		'WHERE tasks.id = ? ORDER BY turns.inserted DESC',
		id, callback );
};

var getStatus = function(id, callback) {
	db.query(
    	'SELECT users.id AS id, users.displayname AS name, COUNT(turns.user_id) AS turns ' +
		'FROM turns ' +
		'RIGHT JOIN participants ON participants.task_id = turns.task_id and participants.user_id = turns.user_id ' +
		'INNER JOIN users ON participants.user_id = users.id ' +
		'WHERE participants.task_id = ? GROUP BY turns.user_id',
		id, callback );
};

var saveAddress = function(user, ip, callback) {
    db.query( 'INSERT INTO addresses SET ? ON DUPLICATE KEY UPDATE user_id = ?', [{user_id: user, ip: ip}, user], function(err, rows, fields) {
		if (err) {
			if(err.code === 'ER_DUP_ENTRY') {
				console.log('Already saved user %d address %s', user, ip);
			} else {
				console.error('Error while performing addresses query', err);
			}
		} else {
			console.log('Saved user %d address %s', user, ip);
		}
		callback();
    });
};

var getUserId = function(ip, callback) {
	db.query('SELECT user_id FROM addresses WHERE ip = ?', ip, function(err, rows, fields) {
		if(err) {
			console.error('Error while getting address', err);
			callback();
		} else {
			console.log('Got user %d from address %s', rows[0].user_id, ip);
			callback(rows[0].user_id);
		}
	});
};

app.get('/api/list', function(req,res) {
    res.setHeader('Content-Type', 'application/json');
    getUserId(req.ip, function(user) {
	    getList(req.query.id, function(err, rows, fields) {
			if (err) {
				console.error('Error while performing list query', err, req.query);
				res.send({user: user, error: 'Query Error'});
			} else {
				res.send({user: user, list: rows});
			}
		});
	});
});

app.get('/api/status', function(req,res) {
    res.setHeader('Content-Type', 'application/json');
    getStatus( req.query.id, function(err, rows, fields) {
		if (err) {
			console.error('Error while performing status query', err);
			res.send({error: 'Query Error'});
		} else {
			res.send({ users: rows });
		}
    });
});

var takeTurn = function(task, user, res) {
    db.query( 'INSERT INTO turns SET ?', {user_id: user, task_id: task}, function(err, rows, fields) {
		if (err) {
			console.error('Error while performing turn query', err);
			res.send({error: 'Query Error'});
		} else {
			res.send({error: false});
		}
    });
};

app.post('/api/turn', function(req,res) {
    res.setHeader('Content-Type', 'application/json');
    if(!req.body.user_id) {
    	getUserId(req.ip, function(user) {
    		if(user) {
    			takeTurn(req.body.task_id, user, res);
    		} else {
    			res.send({error: 'No User ID'});
    		}
    	});
    } else {
    	saveAddress(req.body.user_id, req.ip, function() {
    		takeTurn(req.body.task_id, req.body.user_id, res);
		});
    }
});

var server = app.listen(PORT, function() {
	var host = server.address().address;
	var port = server.address().port;
	console.log('TurnTracker listening at http://%s:%s', host, port);
});