package com.lollipopmedia.shipflow.inflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lollipopmedia.shipflow.Order;

/**
 * Checks the statement that {@link InflowOrderDao#updateCostLogDetail} sends to Inflow.
 * The statement is run against a recording {@link JdbcTemplate}, so the test needs no
 * database and checks the SQL text that the integration builds.
 */
public class InflowCostLogDetailSqlTest {

	private static final int LAST_SALES_ORDER_ID = 41;
	private static final int PROD_ID = 7;
	private static final String TABLE = "BASE_InventoryCostLogDetail";
	private static final String ROW_KEY = "InventoryCostLogDetailId";

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
	public void eachCostLogDetailInsertNamesOneValueForEveryColumn() {
		inflowOrdersDao.updateInflow(orders(1));

		List<List<String>> columns = columnsOfEveryInsert();
		List<List<String>> values = valuesOfEveryInsert();
		assertEquals("number of cost log detail rows", 1, columns.size());
		assertEquals("column count in " + costLogDetailStatements(),
				columns.get(0).size(), values.get(0).size());
	}

	@Test
	public void costLogDetailInsertLeavesTheRowKeyToTheServer() {
		inflowOrdersDao.updateInflow(orders(1));

		assertFalse("the insert names the row key: " + costLogDetailStatements(),
				columnsOfEveryInsert().get(0).contains(ROW_KEY));
	}

	/**
	 * Two shipped items in one run write two cost log detail rows. Neither may name the
	 * row key, because a named literal fails on an identity column and repeats on any
	 * other key column once the first row exists.
	 */
	@Test
	public void twoCostLogDetailRowsInOneRunDoNotShareAKey() {
		inflowOrdersDao.updateInflow(orders(2));

		List<List<String>> columns = columnsOfEveryInsert();
		List<List<String>> values = valuesOfEveryInsert();
		assertEquals("number of cost log detail rows", 2, columns.size());
		assertEquals("both rows are written the same way", columns.get(0), columns.get(1));
		assertFalse("the first row names the key: " + costLogDetailStatements(),
				columns.get(0).contains(ROW_KEY));
		assertFalse("the second row names the key: " + costLogDetailStatements(),
				columns.get(1).contains(ROW_KEY));
		assertEquals(columns.get(0).size(), values.get(0).size());
		assertEquals(columns.get(1).size(), values.get(1).size());
	}

	/** The cost log detail statements that the run sent, for an assertion message. */
	private String costLogDetailStatements() {
		String prefix = "INSERT INTO " + TABLE + " (";
		StringBuilder text = new StringBuilder();
		List<String> statements = jdbcTemplate.statements();
		for (int i = 0; i < statements.size(); i++) {
			if (statements.get(i).startsWith(prefix)) {
				text.append(statements.get(i)).append(' ');
			}
		}
		return text.toString();
	}

	private List<List<String>> columnsOfEveryInsert() {
		return partOfEveryInsert(0);
	}

	private List<List<String>> valuesOfEveryInsert() {
		return partOfEveryInsert(1);
	}

	/** Splits every insert into the target table into its column list and its value list. */
	private List<List<String>> partOfEveryInsert(int part) {
		String prefix = "INSERT INTO " + TABLE + " (";
		List<List<String>> parts = new ArrayList<List<String>>();
		List<String> statements = jdbcTemplate.statements();
		for (int i = 0; i < statements.size(); i++) {
			String statement = statements.get(i);
			if (!statement.startsWith(prefix)) {
				continue;
			}
			int columnsOpen = prefix.length() - 1;
			int columnsClose = statement.indexOf(')', columnsOpen);
			int valuesOpen = statement.indexOf('(', columnsClose);
			int valuesClose = statement.indexOf(')', valuesOpen);
			assertNotNull("no value list in " + statement, Integer.valueOf(valuesOpen));
			String text = part == 0
					? statement.substring(columnsOpen + 1, columnsClose)
					: statement.substring(valuesOpen + 1, valuesClose);
			parts.add(split(text));
		}
		return parts;
	}

	private List<String> split(String list) {
		List<String> items = new ArrayList<String>();
		String[] pieces = list.split(",");
		for (int i = 0; i < pieces.length; i++) {
			items.add(pieces[i].trim());
		}
		return items;
	}

	private List<Order> orders(int count) {
		List<Order> orders = new ArrayList<Order>();
		for (int i = 0; i < count; i++) {
			Order order = new Order();
			order.setSku("BLUETOOTH HEADSET: BN708 BLACK");
			order.setQuantity(1);
			orders.add(order);
		}
		return orders;
	}

	private static class RecordingJdbcTemplate extends JdbcTemplate {

		private final List<String> statements = new ArrayList<String>();

		List<String> statements() {
			return statements;
		}

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
	}
}
