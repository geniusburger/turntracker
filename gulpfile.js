var gulp = require('gulp');
var uglify = require('gulp-uglify');
var sass = require('gulp-sass');
var gulpif = require('gulp-if');
var gls = require('gulp-live-server');
var sourcemaps = require('gulp-sourcemaps');
var concat = require('gulp-concat');

var merge = require('merge-stream');
var minimist = require('minimist');

var options = minimist(process.argv.slice(2), {
  boolean: 'release',
  default: { release: false }
});
options.debug = !options.release;

var src = {
	css: 'public/scss/**/*.{scss,sass}',
	js: 'public/js/**/*.js',
	html: 'public/**/*.html',
	images: 'public/**/images/**'
}

gulp.task('html', function() {
	return gulp.src(src.html).pipe(gulp.dest('build'));
});

gulp.task('images', function() {
	return gulp.src(src.images).pipe(gulp.dest('build'));
});

gulp.task('libs', function() {
	return merge(
		gulp.src('bower_components/jquery/dist/*').pipe(gulp.dest('build/js')),
		gulp.src('bower_components/bootstrap/dist/**/{css/*,fonts/*,js/bootstrap*}').pipe(gulp.dest('build')),
		gulp.src('bower_components/angular/angular*.{js,map}').pipe(gulp.dest('build/js'))
	);	
});

gulp.task('css', function() {
	return gulp.src(src.css)
		.pipe(sourcemaps.init())
		.pipe(sass({
			outputStyle: options.release ? 'compressed' : 'nested'
		}))
		.pipe(sourcemaps.write())
		.pipe(gulp.dest('build/css'));
});

gulp.task('js', function() {
	return gulp.src(src.js)
		.pipe(sourcemaps.init())
		.pipe(concat('tt.js'))
		.pipe(gulpif(options.release, uglify()))
		.pipe(sourcemaps.write())
		.pipe(gulp.dest('build/js'));
});

gulp.task('serve', ['build'], function() {
	var server = gls.new(['server/server.js', '--live']);
	var promise = server.start();
    // handle the server process exiting
    promise.then(function(result) {
       server.start.bind(server);
    });

	//use gulp.watch to trigger server actions(notify, start or stop)
	gulp.watch(src.html, ['html']);
	gulp.watch(src.images, ['images']);
	gulp.watch(src.css, ['css']);
	gulp.watch(src.js, ['js']);
	gulp.watch(['build/**/*'], function (file) {
		server.notify.apply(server, [file]);
	});
	gulp.watch('server/**/*', function() {
		console.log('restarting server');
	    server.start.apply(server); //restart my server
	});
});

gulp.task('build', ['libs', 'html', 'images', 'css', 'js'], function(){});

gulp.task('default', ['build'], function(){});
