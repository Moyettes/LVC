package org.apache.logging.log4j;

// Stub matches real log4j-api: Logger is an INTERFACE (not a class). Shadowed
// into the Risugami drop-in so client-common's pre-compiled INVOKEINTERFACE
// bytecode resolves at runtime. Must include the specific positional overloads
// (1/2/3/4 args, Throwable) because call sites compile against the concrete
// log4j method signatures, not the vararg collapse.

public interface Logger {

	String getName();

	void trace(String msg);
	void trace(String fmt, Object arg);
	void trace(String fmt, Object arg1, Object arg2);
	void trace(String fmt, Object arg1, Object arg2, Object arg3);
	void trace(String fmt, Object arg1, Object arg2, Object arg3, Object arg4);
	void trace(String fmt, Object... args);
	void trace(String msg, Throwable t);

	void debug(String msg);
	void debug(String fmt, Object arg);
	void debug(String fmt, Object arg1, Object arg2);
	void debug(String fmt, Object arg1, Object arg2, Object arg3);
	void debug(String fmt, Object arg1, Object arg2, Object arg3, Object arg4);
	void debug(String fmt, Object... args);
	void debug(String msg, Throwable t);

	void info(String msg);
	void info(String fmt, Object arg);
	void info(String fmt, Object arg1, Object arg2);
	void info(String fmt, Object arg1, Object arg2, Object arg3);
	void info(String fmt, Object arg1, Object arg2, Object arg3, Object arg4);
	void info(String fmt, Object... args);
	void info(String msg, Throwable t);

	void warn(String msg);
	void warn(String fmt, Object arg);
	void warn(String fmt, Object arg1, Object arg2);
	void warn(String fmt, Object arg1, Object arg2, Object arg3);
	void warn(String fmt, Object arg1, Object arg2, Object arg3, Object arg4);
	void warn(String fmt, Object... args);
	void warn(String msg, Throwable t);

	void error(String msg);
	void error(String fmt, Object arg);
	void error(String fmt, Object arg1, Object arg2);
	void error(String fmt, Object arg1, Object arg2, Object arg3);
	void error(String fmt, Object arg1, Object arg2, Object arg3, Object arg4);
	void error(String fmt, Object... args);
	void error(String msg, Throwable t);

	void fatal(String msg);
	void fatal(String fmt, Object arg);
	void fatal(String fmt, Object arg1, Object arg2);
	void fatal(String fmt, Object arg1, Object arg2, Object arg3);
	void fatal(String fmt, Object arg1, Object arg2, Object arg3, Object arg4);
	void fatal(String fmt, Object... args);
	void fatal(String msg, Throwable t);

	default boolean isTraceEnabled() { return false; }
	default boolean isDebugEnabled() { return false; }
	default boolean isInfoEnabled() { return true; }
	default boolean isWarnEnabled() { return true; }
	default boolean isErrorEnabled() { return true; }
}
