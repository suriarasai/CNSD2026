// backend/logger.js
const winston = require('winston');

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.json()
  ),
  transports: [
    //
    // - Write all logs with importance level of `info` or less to the console
    //
    new winston.transports.Console(),
  ],
});

module.exports = logger;