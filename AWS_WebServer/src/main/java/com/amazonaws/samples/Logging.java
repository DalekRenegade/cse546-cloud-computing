package com.amazonaws.samples;

import java.util.logging.FileHandler;
import java.util.logging.Logger;

enum LogType { Info, Warning, Error }

public class Logging {
	public static volatile Logger logger;

	public static void initializeLogging(FileHandler fileHandler) throws SecurityException {
		logger = Logger.getLogger("cse546project1");
		logger.setUseParentHandlers(false);
		logger.addHandler(fileHandler);
	}

	public synchronized static void addLog(LogType logType, String logMessage) {
		if(logType == LogType.Info)
			logger.info(logMessage);
		else if(logType == LogType.Warning)
			logger.warning(logMessage);
		else if(logType == LogType.Error)
			logger.severe(logMessage);
	}
}
