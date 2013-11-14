package com.lollipopmedia.shipflow;

/**
 * bean that holds the info related to a single sales order
 * 
 * @author kduggan
 *
 */
public class Order {

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	protected int quantity;
	
	protected String sku;
	
	
}