package org.apache.logging.log4j;

// Runtime stub of log4j's LogManager for the Risugami drop-in. The returned
// Logger prints to stdout. Must match real log4j's shape: Logger is an
// interface with specific positional overloads (see Logger.java).

public final class LogManager {
	private LogManager() {}

	public static Logger getLogger() {
		return newLogger("LegacyVoiceChat");
	}

	public static Logger getLogger(String name) {
		return newLogger(name);
	}

	public static Logger getLogger(Class<?> clazz) {
		return newLogger(clazz.getName());
	}

	private static Logger newLogger(final String name) {
		return new Logger() {
			@Override public String getName() { return name; }

			@Override public void trace(String msg) { log("TRACE", msg, null); }
			@Override public void trace(String fmt, Object a) { log("TRACE", format(fmt, new Object[] { a }), null); }
			@Override public void trace(String fmt, Object a, Object b) { log("TRACE", format(fmt, new Object[] { a, b }), null); }
			@Override public void trace(String fmt, Object a, Object b, Object c) { log("TRACE", format(fmt, new Object[] { a, b, c }), null); }
			@Override public void trace(String fmt, Object a, Object b, Object c, Object d) { log("TRACE", format(fmt, new Object[] { a, b, c, d }), null); }
			@Override public void trace(String fmt, Object... args) { log("TRACE", format(fmt, args), null); }
			@Override public void trace(String msg, Throwable t) { log("TRACE", msg, t); }

			@Override public void debug(String msg) { log("DEBUG", msg, null); }
			@Override public void debug(String fmt, Object a) { log("DEBUG", format(fmt, new Object[] { a }), null); }
			@Override public void debug(String fmt, Object a, Object b) { log("DEBUG", format(fmt, new Object[] { a, b }), null); }
			@Override public void debug(String fmt, Object a, Object b, Object c) { log("DEBUG", format(fmt, new Object[] { a, b, c }), null); }
			@Override public void debug(String fmt, Object a, Object b, Object c, Object d) { log("DEBUG", format(fmt, new Object[] { a, b, c, d }), null); }
			@Override public void debug(String fmt, Object... args) { log("DEBUG", format(fmt, args), null); }
			@Override public void debug(String msg, Throwable t) { log("DEBUG", msg, t); }

			@Override public void info(String msg) { log("INFO", msg, null); }
			@Override public void info(String fmt, Object a) { log("INFO", format(fmt, new Object[] { a }), null); }
			@Override public void info(String fmt, Object a, Object b) { log("INFO", format(fmt, new Object[] { a, b }), null); }
			@Override public void info(String fmt, Object a, Object b, Object c) { log("INFO", format(fmt, new Object[] { a, b, c }), null); }
			@Override public void info(String fmt, Object a, Object b, Object c, Object d) { log("INFO", format(fmt, new Object[] { a, b, c, d }), null); }
			@Override public void info(String fmt, Object... args) { log("INFO", format(fmt, args), null); }
			@Override public void info(String msg, Throwable t) { log("INFO", msg, t); }

			@Override public void warn(String msg) { log("WARN", msg, null); }
			@Override public void warn(String fmt, Object a) { log("WARN", format(fmt, new Object[] { a }), null); }
			@Override public void warn(String fmt, Object a, Object b) { log("WARN", format(fmt, new Object[] { a, b }), null); }
			@Override public void warn(String fmt, Object a, Object b, Object c) { log("WARN", format(fmt, new Object[] { a, b, c }), null); }
			@Override public void warn(String fmt, Object a, Object b, Object c, Object d) { log("WARN", format(fmt, new Object[] { a, b, c, d }), null); }
			@Override public void warn(String fmt, Object... args) { log("WARN", format(fmt, args), null); }
			@Override public void warn(String msg, Throwable t) { log("WARN", msg, t); }

			@Override public void error(String msg) { log("ERROR", msg, null); }
			@Override public void error(String fmt, Object a) { log("ERROR", format(fmt, new Object[] { a }), null); }
			@Override public void error(String fmt, Object a, Object b) { log("ERROR", format(fmt, new Object[] { a, b }), null); }
			@Override public void error(String fmt, Object a, Object b, Object c) { log("ERROR", format(fmt, new Object[] { a, b, c }), null); }
			@Override public void error(String fmt, Object a, Object b, Object c, Object d) { log("ERROR", format(fmt, new Object[] { a, b, c, d }), null); }
			@Override public void error(String fmt, Object... args) { log("ERROR", format(fmt, args), null); }
			@Override public void error(String msg, Throwable t) { log("ERROR", msg, t); }

			@Override public void fatal(String msg) { log("FATAL", msg, null); }
			@Override public void fatal(String fmt, Object a) { log("FATAL", format(fmt, new Object[] { a }), null); }
			@Override public void fatal(String fmt, Object a, Object b) { log("FATAL", format(fmt, new Object[] { a, b }), null); }
			@Override public void fatal(String fmt, Object a, Object b, Object c) { log("FATAL", format(fmt, new Object[] { a, b, c }), null); }
			@Override public void fatal(String fmt, Object a, Object b, Object c, Object d) { log("FATAL", format(fmt, new Object[] { a, b, c, d }), null); }
			@Override public void fatal(String fmt, Object... args) { log("FATAL", format(fmt, args), null); }
			@Override public void fatal(String msg, Throwable t) { log("FATAL", msg, t); }

			private void log(String level, String msg, Throwable t) {
				System.out.println("[" + name + "/" + level + "] " + msg);
				if (t != null) t.printStackTrace(System.out);
			}
		};
	}

	private static String format(String pattern, Object[] args) {
		if (args == null || args.length == 0) return pattern;
		StringBuilder sb = new StringBuilder(pattern.length() + 16 * args.length);
		int argIdx = 0;
		int i = 0;
		while (i < pattern.length()) {
			int next = pattern.indexOf("{}", i);
			if (next < 0) {
				sb.append(pattern, i, pattern.length());
				break;
			}
			sb.append(pattern, i, next);
			if (argIdx < args.length) {
				Object a = args[argIdx++];
				sb.append(a == null ? "null" : a.toString());
			} else {
				sb.append("{}");
			}
			i = next + 2;
		}
		return sb.toString();
	}
}
