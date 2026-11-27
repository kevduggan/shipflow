package com.lollipopmedia.shipflow;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
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

	private static final String CONTEXT_LOCATION = "/shipflow-context.xml";

	private static final Logger logger = Logger.getLogger(ShipflowRunner.class);

	public static void main(String[] args) {
		beginRun();
		ApplicationContext context = startContext(CONTEXT_LOCATION);
		ShipflowRunner runner = (ShipflowRunner)context.getBean("shipflowRunner");
        runner.start(args);
    }

	/**
	 * Starts a run and puts its identifier on every record the run writes, so the
	 * records of one scheduled run can be told apart from the next one.
	 *
	 * @return the identifier of the run
	 */
	static String beginRun() {
		String runId = UUID.randomUUID().toString();
		MDC.put("run_id", runId);
		return runId;
	}

	/**
	 * Starts the spring context. A context that cannot start is logged with the
	 * run id before the failure is rethrown, so it belongs to one run.
	 */
	static ApplicationContext startContext(String configLocation) {
		try {
			return new ClassPathXmlApplicationContext(configLocation);
		} catch (RuntimeException e) {
			logger.error("Shipflow run could not start", e);
			throw e;
		}
	}

    @Autowired
    private ShipflowIntegrator integrator;
    
    private void start(String[] args) {
        
        integrator.transferOrders();
        System.exit(1);
    }
}
