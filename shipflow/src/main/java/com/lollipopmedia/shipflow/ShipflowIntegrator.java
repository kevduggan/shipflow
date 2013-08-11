package com.lollipopmedia.shipflow;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.lollipopmedia.shipflow.inflow.InflowOrderDao;
import com.lollipopmedia.shipflow.shipworks.ShipworksOrderDao;

/**
 * Simple enough. gets all skus and ids from shipworks and updates the necessary
 * in inflow (updates inventory, creates sales orders and sales order line items)
 * 
 * @author kduggan
 */
@Component
public class ShipflowIntegrator 
{
    
	@Autowired
	ShipworksOrderDao ordersDao;
	@Autowired
	InflowOrderDao inflowOrdersDao;
	
	Logger logger = Logger.getLogger(ShipflowIntegrator.class);
	
	public void transferOrders(){
		try{
			logger.info("Starting Shipflow");
			List<Order> orders = ordersDao.findAllSKUAndQtyPairs();
			logger.info(orders.size()+" orders about to be imported." );
			inflowOrdersDao.updateInflow(orders);
			logger.info("Shipflow transfer complete");
		}
		catch(Exception e){
			logger.error("SHIPFLOW ran but there was an error! Ring Kevin!", e);
		}
	}	
	
	public static void main(String[]args){
		ShipflowIntegrator integrator = new ShipflowIntegrator();
		integrator.transferOrders();
	}
}
