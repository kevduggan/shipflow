package com.lollipopmedia.shipflow.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lollipopmedia.shipflow.Order;

/**
 * Checks the log records that tie a run to the inflow sales order it writes and
 * to the shipped skus that got no sales order line.
 */
public class InflowOrderDaoLoggingTest {

	private static final String MATCHED_SKU = "MATCHED-SKU";
	private static final String UNMATCHED_SKU = "UNMATCHED-SKU";

	private static final String[] FIELD_NAMES = { "sales_order", "sales_order_id", "line", "sku",
			"lines_written", "lines_skipped", "skus_skipped" };

	private final Logger daoLogger = Logger.getLogger(InflowOrderDao.class);
	private final RecordingAppender appender = new RecordingAppender();
	private InflowOrderDao inflowOrderDao;

	@Before
	public void attachRecorder() throws Exception {
		inflowOrderDao = new InflowOrderDao();
		Field jdbcTemplateField = InflowOrderDao.class.getDeclaredField("jdbcTemplate");
		jdbcTemplateField.setAccessible(true);
		jdbcTemplateField.set(inflowOrderDao, new StubJdbcTemplate());
		appender.setThreshold(Level.DEBUG);
		daoLogger.addAppender(appender);
	}

	@After
	public void detachRecorder() {
		daoLogger.removeAppender(appender);
		for (String field : FIELD_NAMES) {
			MDC.remove(field);
		}
	}

	@Test
	public void namesTheSalesOrderAndCountsTheSkippedLines() {
		inflowOrderDao.updateInflow(Arrays.asList(order(MATCHED_SKU, 2), order(UNMATCHED_SKU, 1)));

		RecordedEvent written = appender.eventAt(Level.INFO, "Inflow updated successfully");
		assertNotNull("the run summary is logged", written);
		assertEquals("SO-000129", written.field("sales_order"));
		assertEquals("4128", written.field("sales_order_id"));
		assertEquals("1", written.field("lines_written"));
		assertEquals("1", written.field("lines_skipped"));

		RecordedEvent skipped = appender.eventAt(Level.WARN, "Inflow sales order is missing lines for unmatched SKUs");
		assertNotNull("a run with a skipped sku is a warning", skipped);
		assertEquals("SO-000129", skipped.field("sales_order"));
		assertEquals(UNMATCHED_SKU, skipped.field("skus_skipped"));

		RecordedEvent noProduct = appender.eventContaining(Level.ERROR, "no matching product");
		assertNotNull("the skipped sku is logged", noProduct);
		assertEquals("SO-000129", noProduct.field("sales_order"));
		assertEquals("1", noProduct.field("line"));
		assertEquals(UNMATCHED_SKU, noProduct.field("sku"));
	}

	@Test
	public void doesNotWarnWhenEverySkuMatched() {
		inflowOrderDao.updateInflow(Arrays.asList(order(MATCHED_SKU, 1)));

		RecordedEvent written = appender.eventAt(Level.INFO, "Inflow updated successfully");
		assertNotNull(written);
		assertEquals("1", written.field("lines_written"));
		assertEquals("0", written.field("lines_skipped"));
		assertFalse(appender.hasEvent(Level.WARN, "Inflow sales order is missing lines for unmatched SKUs"));
	}

	private static Order order(String sku, int quantity) {
		Order order = new Order();
		order.setSku(sku);
		order.setQuantity(quantity);
		return order;
	}

	private static class RecordedEvent {

		private final Level level;
		private final String message;
		private final Map<String, String> fields;

		RecordedEvent(Level level, String message, Map<String, String> fields) {
			this.level = level;
			this.message = message;
			this.fields = fields;
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
			events.add(new RecordedEvent(event.getLevel(), event.getRenderedMessage(), fields));
		}

		RecordedEvent eventAt(Level level, String message) {
			for (RecordedEvent event : events) {
				if (event.level.toInt() == level.toInt() && message.equals(event.message)) {
					return event;
				}
			}
			return null;
		}

		RecordedEvent eventContaining(Level level, String text) {
			for (RecordedEvent event : events) {
				if (event.level.toInt() == level.toInt() && event.message.indexOf(text) >= 0) {
					return event;
				}
			}
			return null;
		}

		boolean hasEvent(Level level, String message) {
			return eventAt(level, message) != null;
		}

		public void close() {
		}

		public boolean requiresLayout() {
			return false;
		}
	}

	private static class StubJdbcTemplate extends JdbcTemplate {

		public Map queryForMap(String sql) {
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("SalesOrderId", Integer.valueOf(4127));
			row.put("OrderNumber", "SO-000128");
			return row;
		}

		public int queryForInt(String sql) {
			if (sql.contains("from base_product")) {
				if (sql.contains(UNMATCHED_SKU)) {
					throw new EmptyResultDataAccessException(1);
				}
				return 501;
			}
			if (sql.contains("InventoryCostLogBatchId")) {
				return 800;
			}
			if (sql.contains("InventoryLogBatchId")) {
				return 900;
			}
			if (sql.contains("SO_SalesOrderShip_Line")) {
				return 700;
			}
			if (sql.contains("from base_inventory")) {
				return 10;
			}
			throw new IllegalStateException("unexpected query: " + sql);
		}

		public Object queryForObject(String sql, Class requiredType) {
			return Double.valueOf(12.5d);
		}

		public void execute(String sql) {
		}

		public int update(String sql) {
			return 1;
		}
	}
}
