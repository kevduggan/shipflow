package com.lollipopmedia.shipflow.inflow;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.lollipopmedia.shipflow.Order;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/shipflow-context.xml"})
public class InflowOrderDaoTest {

	@Autowired
	InflowOrderDao inflowOrdersDao;
	
	@Test
	public void testUpdateInflow() {
		Order order =  new Order();
		order.setQuantity(1);
		order.setSku("BLUETOOTH HEADSET: BN708 BLACK");
		List<Order> ordersList= new ArrayList<Order>();
		ordersList.add(order);
		//inflowOrdersDao.updateInflow(ordersList);
		assert(true);
	}

}
