package com.lollipopmedia.shipflow.shipworks;	

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.lollipopmedia.shipflow.Order;
import com.lollipopmedia.shipflow.ShipflowIntegrator;

@Repository
public class ShipworksOrderDao {

	@Autowired
	@Qualifier("jdbcTemplate")
	private JdbcTemplate jdbcTemplate;
	
	Logger logger = Logger.getLogger(ShipworksOrderDao.class);

	
	/**
	 * gets all items with a must send item status
	 * 
	 * @return list of sku and quantity pairs
	 */
	@Transactional(readOnly = true)
	public List<Order> findAllSKUAndQtyPairs() {
		String query = "SELECT OrderItem.SKU, OrderItem.quantity " +
				"FROM OrderItem " +
				"LEFT OUTER JOIN  \"Order\" ON \"Order\".OrderID=OrderItem.OrderId " +
				"WHERE \"Order\".localstatus='Must Send Item'";
		List<Order> orders = jdbcTemplate.query(query,
				new BeanPropertyRowMapper(Order.class));
		logger.info("Retrieved  "+orders.size()+" orders from Shipflow");
		return orders;
		
	}
	
}
