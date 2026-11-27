package com.lollipopmedia.shipflow.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lollipopmedia.shipflow.Order;

/**
 * Checks the statements that {@link InflowOrderDao#addSalesOrderLine} sends to Inflow.
 * The statements are run against a recording {@link JdbcTemplate}, so the test needs no
 * database and checks the SQL text that the integration builds.
 */
public class InflowSalesOrderLineSqlTest {

	private static final int LAST_SALES_ORDER_ID = 41;
	private static final int NEW_SALES_ORDER_ID = LAST_SALES_ORDER_ID + 1;
	private static final int PROD_ID = 7;

	private RecordingJdbcTemplate jdbcTemplate;
	private InflowOrderDao inflowOrdersDao;

	@Before
	public void setUp() throws Exception {
		jdbcTemplate = new RecordingJdbcTemplate();
		inflowOrdersDao = new InflowOrderDao();
		Field field = InflowOrderDao.class.getDeclaredField("jdbcTemplate");
		field.setAccessible(true);
		field.set(inflowOrdersDao, jdbcTemplate);
	}

	@Test
	public void eachLineInsertNamesOneValueForEveryColumn() {
		inflowOrdersDao.updateInflow(orders());

		String[] lineTables = { "SO_SalesOrderInvoice_Line", "SO_SalesOrderPack_Line",
				"SO_SalesOrderPick_Line", "SO_SalesOrderShip_Line", "SO_SalesOrderShipContainer" };
		for (int i = 0; i < lineTables.length; i++) {
			Insert insert = jdbcTemplate.insertInto(lineTables[i]);
			assertNotNull("no insert for " + lineTables[i], insert);
			assertEquals("column count of " + lineTables[i] + " in " + insert.statement,
					insert.columns.size(), insert.values.size());
		}
	}

	@Test
	public void eachLineInsertUsesTheNewSalesOrderId() {
		inflowOrdersDao.updateInflow(orders());

		String[] lineTables = { "SO_SalesOrderInvoice_Line", "SO_SalesOrderPack_Line",
				"SO_SalesOrderPick_Line", "SO_SalesOrderShip_Line" };
		for (int i = 0; i < lineTables.length; i++) {
			Insert insert = jdbcTemplate.insertInto(lineTables[i]);
			assertEquals("SalesOrderId of " + lineTables[i], String.valueOf(NEW_SALES_ORDER_ID),
					insert.row().get("SalesOrderId"));
			assertEquals("Version of " + lineTables[i], "1", insert.row().get("Version"));
			assertEquals("LineNum of " + lineTables[i], "0", insert.row().get("LineNum"));
		}
	}

	@Test
	public void shipLineWritesTheShipDate() {
		inflowOrdersDao.updateInflow(orders());

		Insert insert = jdbcTemplate.insertInto("SO_SalesOrderShip_Line");
		String shippedDate = insert.row().get("ShippedDate").replace("'", "");
		assertNotNull(shippedDate);
		assertTrue("ShippedDate is not a date: " + shippedDate,
				shippedDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
	}

	private List<Order> orders() {
		Order order = new Order();
		order.setSku("BLUETOOTH HEADSET: BN708 BLACK");
		order.setQuantity(3);
		List<Order> orders = new ArrayList<Order>();
		orders.add(order);
		return orders;
	}

	private static class RecordingJdbcTemplate extends JdbcTemplate {

		private final List<String> statements = new ArrayList<String>();

		@Override
		public Map queryForMap(String sql) {
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("SalesOrderId", Integer.valueOf(LAST_SALES_ORDER_ID));
			row.put("OrderNumber", "SO-000041");
			return row;
		}

		@Override
		public int queryForInt(String sql) {
			if (sql.indexOf("MAX(InventoryLogBatchId)") >= 0) {
				return 900;
			}
			if (sql.indexOf("MAX(InventoryCostLogBatchId)") >= 0) {
				return 800;
			}
			if (sql.indexOf("MAX(") >= 0 && sql.indexOf("SO_SalesOrderShip_Line") >= 0) {
				return 700;
			}
			if (sql.indexOf("from base_product") >= 0) {
				return PROD_ID;
			}
			if (sql.indexOf("from base_inventory") >= 0) {
				return 10;
			}
			throw new IllegalStateException("unexpected query: " + sql);
		}

		@Override
		public Object queryForObject(String sql, Class requiredType) {
			return Double.valueOf(2.5);
		}

		@Override
		public void execute(String sql) {
			statements.add(sql);
		}

		@Override
		public int update(String sql) {
			statements.add(sql);
			return 1;
		}

		Insert insertInto(String table) {
			for (int i = 0; i < statements.size(); i++) {
				Insert insert = Insert.parse(statements.get(i));
				if (insert != null && table.equals(insert.table)) {
					return insert;
				}
			}
			return null;
		}
	}

	/** One INSERT statement split into its column list and its value list. */
	private static class Insert {

		private final String statement;
		private final String table;
		private final List<String> columns;
		private final List<String> values;

		private Insert(String statement, String table, List<String> columns, List<String> values) {
			this.statement = statement;
			this.table = table;
			this.columns = columns;
			this.values = values;
		}

		static Insert parse(String statement) {
			String upper = statement.toUpperCase();
			if (!upper.startsWith("INSERT INTO")) {
				return null;
			}
			int columnsOpen = statement.indexOf('(');
			int columnsClose = matchingParen(statement, columnsOpen);
			int valuesAt = upper.indexOf("VALUES", columnsClose);
			int valuesOpen = statement.indexOf('(', valuesAt);
			int valuesClose = matchingParen(statement, valuesOpen);
			String table = statement.substring("INSERT INTO".length(), columnsOpen).trim();
			return new Insert(statement, table,
					splitTopLevel(statement.substring(columnsOpen + 1, columnsClose)),
					splitTopLevel(statement.substring(valuesOpen + 1, valuesClose)));
		}

		/** The values of this statement by column name, for the columns that have one. */
		Map<String, String> row() {
			Map<String, String> row = new LinkedHashMap<String, String>();
			for (int i = 0; i < columns.size() && i < values.size(); i++) {
				row.put(columns.get(i).trim(), values.get(i).trim());
			}
			return row;
		}

		private static int matchingParen(String text, int open) {
			int depth = 0;
			boolean quoted = false;
			for (int i = open; i < text.length(); i++) {
				char c = text.charAt(i);
				if (c == '\'') {
					quoted = !quoted;
				} else if (!quoted && c == '(') {
					depth++;
				} else if (!quoted && c == ')') {
					depth--;
					if (depth == 0) {
						return i;
					}
				}
			}
			throw new IllegalStateException("unbalanced parentheses in: " + text);
		}

		private static List<String> splitTopLevel(String list) {
			List<String> items = new ArrayList<String>();
			int depth = 0;
			boolean quoted = false;
			int start = 0;
			for (int i = 0; i < list.length(); i++) {
				char c = list.charAt(i);
				if (c == '\'') {
					quoted = !quoted;
				} else if (!quoted && c == '(') {
					depth++;
				} else if (!quoted && c == ')') {
					depth--;
				} else if (!quoted && depth == 0 && c == ',') {
					items.add(list.substring(start, i));
					start = i + 1;
				}
			}
			items.add(list.substring(start));
			return items;
		}
	}
}
