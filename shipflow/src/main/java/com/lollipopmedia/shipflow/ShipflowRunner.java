package com.lollipopmedia.shipflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Simple class to run the application
 * @author kevinduggan
 *
 */
@Component("shipflowRunner")
public class ShipflowRunner {

	public static void main(String[] args) {
		ApplicationContext context = 
	            new ClassPathXmlApplicationContext("/shipflow-context.xml");
		ShipflowRunner runner = (ShipflowRunner)context.getBean("shipflowRunner");
        runner.start(args);
    }

    @Autowired
    private ShipflowIntegrator integrator;
    
    private void start(String[] args) {
        
        integrator.transferOrders();
        System.exit(1);
    }
}
