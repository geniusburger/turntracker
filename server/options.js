var minimist = require('minimist');

exports.options = minimist(process.argv.slice(2), {
  boolean: 'live',
  default: { live: false }
});
