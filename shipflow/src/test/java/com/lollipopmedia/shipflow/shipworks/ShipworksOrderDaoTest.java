package com.lollipopmedia.shipflow.shipworks;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.lollipopmedia.shipflow.Order;


/**
 * test class for the shipworksorderdao
 * 
 * @author kduggan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/shipflow-context.xml"})
public class ShipworksOrderDaoTest {
	
	@Autowired
	ShipworksOrderDao ordersDao;
	
	@Test
	public void testFindAllSKUAndQtyPairs() {
		List<Order> orders = ordersDao.findAllSKUAndQtyPairs();
		assertFalse(orders.size()==0);
	}

}
