var express = require('express');
var path = require('path');
var favicon = require('serve-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var log = require('debug')('turntracker:app');

var ApiError = require('./routes/ApiError');
var db = require('./controllers/db');
var options = require('./options');
var api = require('./routes/api');

var REQUIRED_DB_VERSION = 2;

var app = express();

if(options.live)
{
    console.log('connecting live reload');
    app.use(require('connect-livereload')({
        port: 35729
    }));
}

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');

app.use(favicon(path.join(__dirname, '..', 'build', 'images', 'favicon.ico')));
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cookieParser());

app.use('/css', express.static(path.join(__dirname, '..', 'build/css')));

db.getVersion().then(function(version){
    // check version
    if(version !== REQUIRED_DB_VERSION) {
        var msg = 'Database version should be ' + REQUIRED_DB_VERSION + ' instead of ' + version;
        log(msg);
        app.use(function(req, res, next){
            next(new Error(msg));
        });
    } else {
        // hook up api
        app.use(express.static(path.join(__dirname, '..', 'build')));
        app.use('/api', api);

        // catch 404 and forward to error handler
        app.use(function(req, res, next) {
            var err = new Error('Not Found');
            err.status = 404;
            next(err);
        });
    }
}).catch(function(err){
    // cant get version
    log("ERROR get DB version", err);
    app.use(function(req, res, next){
        next(new Error("Failed to get database version"));
    });
}).finally(function(){

    // error handlers

    // development error handler
    // will print stacktrace
    if (app.get('env') === 'development') {
        app.use(function(err, req, res, next) {
            log("ERROR", err);
            res.status(err.status || 500);
            if(err instanceof ApiError) {
                res.json(err.getDevResponse());
            } else {
                res.render('error', {
                    error: err
                });
            }
        });
    }

    // production error handler
    // no stacktraces leaked to user
    app.use(function(err, req, res, next) {
        log("ERROR", err);
        res.status(err.status || 500);
        if(err instanceof ApiError) {
            res.json(err.getResponse());
        } else {
            res.render('error', {
                error: {
                    message: err.message,
                    status: err.status
                }
            });
        }
    });

});

module.exports = app;
