// backend/logger.js  —  LAB 5: structured logs carrying trace context
const winston = require('winston');
const { trace, context } = require('@opentelemetry/api');

// Attach the ACTIVE span's ids to every log line. This is what turns a wall of
// logs into something you can pivot from directly into a Jaeger trace.
const withTraceContext = winston.format((info) => {
  const span = trace.getSpan(context.active());
  if (span) {
    const spanContext = span.spanContext();
    info.trace_id = spanContext.traceId;
    info.span_id = spanContext.spanId;
  }
  info['service.name'] = process.env.OTEL_SERVICE_NAME || 'backend';
  info['service.instance.id'] = process.env.HOSTNAME;
  return info;
});

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    withTraceContext(),
    winston.format.json()
  ),
  // stdout ONLY. Log files inside a container are state you cannot collect.
  transports: [new winston.transports.Console()],
});

module.exports = logger;
