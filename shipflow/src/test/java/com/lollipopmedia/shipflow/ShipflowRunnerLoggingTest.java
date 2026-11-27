package com.lollipopmedia.shipflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Checks that a run has an identifier and that a run which cannot start leaves a
 * record with that identifier and the failure.
 */
public class ShipflowRunnerLoggingTest {

	private static final String[] FIELD_NAMES = { "run_id" };

	private final Logger runnerLogger = Logger.getLogger(ShipflowRunner.class);
	private final RecordingAppender appender = new RecordingAppender();

	@Before
	public void attachRecorder() {
		appender.setThreshold(Level.DEBUG);
		runnerLogger.addAppender(appender);
	}

	@After
	public void detachRecorder() {
		runnerLogger.removeAppender(appender);
		MDC.remove("run_id");
	}

	@Test
	public void givesEachRunItsOwnIdentifier() {
		String first = ShipflowRunner.beginRun();
		assertNotNull(first);
		assertEquals(first, MDC.get("run_id"));

		String second = ShipflowRunner.beginRun();
		assertFalse("a new run gets a new identifier", first.equals(second));
		assertEquals(second, MDC.get("run_id"));
	}

	@Test
	public void logsAContextThatCannotStartWithTheRunId() {
		String runId = ShipflowRunner.beginRun();
		try {
			ShipflowRunner.startContext("/no-such-context.xml");
			fail("a missing spring context must not start the run");
		} catch (RuntimeException expected) {
			assertNotNull(expected.getMessage());
		}

		RecordedEvent failed = appender.eventAt(Level.ERROR, "Shipflow run could not start");
		assertNotNull("a run that cannot start is logged", failed);
		assertEquals(runId, failed.field("run_id"));
		assertNotNull("the failure stays on the record", failed.throwable);
	}

	private static class RecordedEvent {

		private final Level level;
		private final String message;
		private final Map<String, String> fields;
		private final ThrowableInformation throwable;

		RecordedEvent(Level level, String message, Map<String, String> fields, ThrowableInformation throwable) {
			this.level = level;
			this.message = message;
			this.fields = fields;
			this.throwable = throwable;
		}

		String field(String name) {
			return fields.get(name);
		}
	}

	private static class RecordingAppender extends AppenderSkeleton {

		private final List<RecordedEvent> events = new ArrayList<RecordedEvent>();

		protected void append(LoggingEvent event) {
			Map<String, String> fields = new HashMap<String, String>();
			for (String field : FIELD_NAMES) {
				Object value = event.getMDC(field);
				if (value != null) {
					fields.put(field, String.valueOf(value));
				}
			}
			events.add(new RecordedEvent(event.getLevel(), event.getRenderedMessage(), fields,
					event.getThrowableInformation()));
		}

		RecordedEvent eventAt(Level level, String message) {
			for (RecordedEvent event : events) {
				if (event.level.toInt() == level.toInt() && message.equals(event.message)) {
					return event;
				}
			}
			return null;
		}

		public void close() {
		}

		public boolean requiresLayout() {
			return false;
		}
	}
}
