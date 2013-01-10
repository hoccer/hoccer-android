/*******************************************************************************
 * Copyright (C) 2009, 2010, Hoccer GmbH Berlin, Germany <www.hoccer.com> These coded instructions,
 * statements, and computer programs contain proprietary information of Hoccer GmbH Berlin, and are
 * copy protected by law. They may be used, modified and redistributed under the terms of GNU
 * General Public License referenced below. Alternative licensing without the obligations of the GPL
 * is available upon request. GPL v3 Licensing: This file is part of the "Linccer Java-API". Linccer
 * Java-API is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. Linccer Java-API is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with Linccer Java-API. If
 * not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.hoccer.android;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import android.util.Log;

import com.hoccer.util.HoccerLoggers;

/**
 * Logging handler that forwards incoming log records to Android's logcat so
 * that log output from Android-agnostic code will appear in the Android log.
 * Call {@link engage} once your app starts to enable log forwarding and in your
 * Android-agnostic code get your logger instance from {@link HoccerLoggers} to
 * use this feature conveniently.
 * 
 * In Android-dependant code you should never use this, but use Android's Log
 * class instead.
 * 
 * @author Arne Handt, it@handtwerk.de
 * 
 */
public class AndroidLogHandler extends Handler {

	// Constants ---------------------------------------------------------

	private static final String LOG_TAG = AndroidLogHandler.class
			.getSimpleName();

	private static final int ALL = Level.ALL.intValue();
	private static final int FINEST = Level.FINEST.intValue();
	private static final int FINER = Level.FINER.intValue();
	private static final int FINE = Level.FINE.intValue();
	private static final int CONFIG = Level.CONFIG.intValue();
	private static final int WARNING = Level.WARNING.intValue();

	private static final AndroidLogHandler INSTANCE = new AndroidLogHandler();

	// not used:
	// private static final int INFO = Level.INFO.intValue();
	// private static final int SEVERE = Level.SEVERE.intValue();
	// private static final int OFF = Level.OFF.intValue();

	// Static Methods ----------------------------------------------------

	public static void engage() {

		Log.d(LOG_TAG, "engaging log forwarding");
		HoccerLoggers.addHandler(INSTANCE);
	}

	// Public Instance Methods -------------------------------------------

	@Override
	public void publish(LogRecord record) {

		final String msg = record.getMessage();
		final String tag = record.getLoggerName();
		final int level = record.getLevel().intValue();

		// switch doesn't work because log level int values can't be determined
		// at compile time
		if (ALL == level || FINEST == level || FINER == level) {

			Log.v(tag, msg);

		} else if (FINE == level) {

			Log.d(tag, msg);

		} else if (CONFIG == level) {

			Log.i(tag, msg);

		} else if (WARNING == level) {

			Log.w(tag, msg);
		}

		// INFO, SEVERE: handled by the system
		// OFF: ignored
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
	}
}