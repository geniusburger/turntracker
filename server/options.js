var minimist = require('minimist');

module.exports = minimist(process.argv.slice(2), {
  boolean: 'live',
  default: { live: false }
});
