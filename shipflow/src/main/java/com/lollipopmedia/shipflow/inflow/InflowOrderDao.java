package com.lollipopmedia.shipflow.inflow;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.lollipopmedia.shipflow.Order;
import com.lollipopmedia.shipflow.ShipflowIntegrator;

/**
 * Writes to the inflow DB.
 * 
 * @author kduggan
 *
 */
@Repository
public class InflowOrderDao {

	@Autowired
	@Qualifier("inflowJdbcTemplate")
	private JdbcTemplate jdbcTemplate;

	private String maxSOIdQuery = "SELECT SalesOrderId, OrderNumber FROM SO_SalesOrder WHERE SalesOrderId=" +
									"( SELECT MAX(SalesOrderId) FROM SO_SalesOrder)";
	private String maxInvBatchLogIdQuery = "SELECT MAX(InventoryLogBatchId) FROM BASE_InventoryLogBatch";
	private String maxInvBatchLogCostIdQuery = "SELECT MAX(InventoryCostLogBatchId) FROM BASE_InventoryCostLogBatch";
	private String maxShipLineIdQuery = "SELECT MAX(SO_SalesOrderShip_Line) from SO_SalesOrderShip_Line";
	private String standardCostQuery = "SELECT standardCost FROM BASE_InventoryCost where prodId=%d";


	private final String ORDER_NUM_PREFIX = "SO-";
	
	NumberFormat orderIdFormatter = new DecimalFormat("000000");
	
	SimpleDateFormat sdf = 
		     new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	Logger logger = Logger.getLogger(InflowOrderDao.class);

	/**
	 * creates a sales order for that days shipments with a line item
	 * for the individual skus and qtys
	 * also updates the inventory for each product as it goes along.
	 * @param orders
	 */
	@Transactional (readOnly=false)
	public void updateInflow(List<Order> orders){
		String date = sdf.format(new Date());
		Map<String, Object> soResults = jdbcTemplate.queryForMap(maxSOIdQuery);
		Integer soId = ((Integer) soResults.get("SalesOrderId"))+1;
		int orderNum = (Integer.parseInt(((String)soResults.get("OrderNumber")).substring(ORDER_NUM_PREFIX.length())))+1;
		updateSOTable(orderNum,date);
		updateInventoryLogBatch(soId, orderNum, date);
		Integer invLogId = getInvBatchLogId();
		Integer invLogCostId = getInvBatchLogCostId();
		updateInventoryCostLogBatch(invLogId, soId, orderNum, date);
		int lineNumber = 0;
		for(Order order: orders){
			updateSingleOrder(date, soId, invLogId, invLogCostId, lineNumber, order);
			//update ids
			lineNumber++;
		}
		
		logger.info("Inflow updated successfully. "+lineNumber+" sales order lines added!");
	}

	/**
	 * @param soId
	 * @param orderNum
	 */
	private void updateInventoryLogBatch(Integer soId, int orderNum, String date) {
		String logBatchQuery = "INSERT INTO BASE_InventoryLogBatch (BatchDate, TransactionDate, BatchType, RecordId, Remarks, CreatedUserId, CreatedDttm)" + 
								"VALUES ('"+date+"', '"+date+"', %d,"+soId+", "+ORDER_NUM_PREFIX+orderIdFormatter.format(orderNum)+", 100, '"+date+"')";
		jdbcTemplate.execute(String.format(logBatchQuery,3));
		jdbcTemplate.execute(String.format(logBatchQuery,4));
		
	}
	
	/**
	 * @param invLogId
	 * @param soId
	 * @param orderNum
	 * @param date
	 */
	private void updateInventoryCostLogBatch(Integer invLogId, Integer soId,
			int orderNum, String date) {
		String costLogBatchQuery = "INSERT INTO BASE_InventoryCostLogBatch (TransactionDate, BatchType, RecordId, Remarks, InventoryLogBatchId, CreatedUserId, CreatedDttm)" + 
									"VALUES ('"+date+"', %d, "+soId+", "+ORDER_NUM_PREFIX+orderIdFormatter.format(orderNum)+", %d, 100, '"+date+"')";
		jdbcTemplate.execute(String.format(costLogBatchQuery,3,invLogId-1));
		jdbcTemplate.execute(String.format(costLogBatchQuery,4,invLogId));
	}

	
	
	

	/**
	 * Updates the database here for each line in the batch order
	 * 
	 * @param date
	 * @param soId
	 * @param invLogId
	 * @param lineNumber
	 * @param order
	 */
	private void updateSingleOrder(String date, Integer soId, Integer invLogId, Integer invLogCostId,
			int lineNumber, Order order){
		String prodIdQuery = "Select ProdId from base_product where name='"+getEscapedSKU(order)+"'";
		int prodId;
		try{
			logger.info("Processing "+order.getSku()+" ("+order.getQuantity()+"pcs)");
			 prodId = jdbcTemplate.queryForInt(prodIdQuery);
		}
		catch(EmptyResultDataAccessException e){
			logger.error("Looks like their was no matching product for the SKU '"+getEscapedSKU(order)+"'",e);
			return;
		}
		int existingQty = getExistingQuantity(prodId);
		int updatedQty = existingQty - order.getQuantity();
		Double standardCost = (Double) jdbcTemplate.queryForObject(String.format(standardCostQuery,prodId), Double.class); 

		updateInventoryLogDetails(order.getQuantity(), existingQty, updatedQty, date, invLogId, order, prodId);
		updateInventoryQtyTotal(updatedQty, prodId);
		updateCostLogDetail(invLogCostId, order.getQuantity(), existingQty, updatedQty, prodId, date, standardCost);
		addSalesOrderLine(soId, lineNumber, order, prodId, standardCost, date);

	}


	/**
	 * @param qty
	 * @param date
	 * @param invLogId
	 * @param order
	 * @param prodId
	 */
	private void updateInventoryLogDetails(int quantity, int existingQty, int updatedQty, String date,
			Integer invLogId, Order order, int prodId) {
		
		String logDetailsQueryLocA = "INSERT INTO BASE_InventoryLogDetail (InventoryLogBatchId, FromLocationId, FromSublocation, ToLocationId, ToSublocation, Quantity, CreatedUserId, CreatedDttm, FromQuantityBefore, FromQuantityAfter, ToQuantityBefore, ToQuantityAfter, ProdId)"+ 
									"VALUES (%d, 100, '', 1, '', "+quantity+", 100, '"+date+"', "+existingQty+", "+updatedQty+", 0.0000, "+quantity+", "+prodId+")";
		jdbcTemplate.execute(String.format(logDetailsQueryLocA,invLogId-1));
		String logDetailsQueryLocB = "INSERT INTO BASE_InventoryLogDetail (InventoryLogBatchId, FromLocationId, FromSublocation, ToLocationId, ToSublocation, Quantity, CreatedUserId, CreatedDttm, FromQuantityBefore, FromQuantityAfter, ToQuantityBefore, ToQuantityAfter, ProdId)"+ 
				"VALUES (%d, 1, '', NULL, '', "+quantity+", 100, '"+date+"', "+quantity+", "+0.000+", NULL, NULL, "+prodId+")";
		jdbcTemplate.execute(String.format(logDetailsQueryLocB,invLogId));

	}
	
	/**
	 * @param updatedQty
	 * @param prodId
	 */
	private void updateInventoryQtyTotal(int updatedQty, int prodId) {
		String invQtyQuery = "UPDATE BASE_InventoryQuantityTotal SET QuantityOnHand = " + updatedQty + " WHERE ProdId = " +prodId;
		jdbcTemplate.execute(invQtyQuery);
		
		String invQuery = "UPDATE BASE_Inventory SET Quantity = " + updatedQty + " WHERE LocationId = 100 AND Sublocation = '' AND ProdId = "+prodId;
		jdbcTemplate.execute(invQuery);
		String invCostQuery = "UPDATE BASE_InventoryCost SET InventoryCount = " + updatedQty + " WHERE ProdId = "+prodId;
		jdbcTemplate.execute(invCostQuery);
	}
	
	/**
	 * @param invLogCostId
	 * @param quantity
	 * @param existingQty
	 * @param updatedQty
	 * @param prodId
	 */
	private void updateCostLogDetail(Integer invLogCostId, int quantity,
			int existingQty, int updatedQty, int prodId, String date, Double standardCost) {
				
		String updateCostLogDetailQuery = "INSERT INTO BASE_InventoryCostLogDetail (InventoryCostLogDetailId, InventoryCostLogBatchId, Quantity, QuantityBefore, QuantityAfter, AverageCostBefore, AverageCostAfter, CreatedUserId, CreatedDttm, ProdId, LastPurchaseCostBefore, LastPurchaseCostAfter, StandardCostBefore, StandardCostAfter, CostingMethodBefore, CostingMethodAfter, CurrencyIdBefore, CurrencyIdAfter)"+ 
										  "VALUES (9986, "+invLogCostId+", -"+quantity+", "+existingQty+", "+updatedQty+", 0.00000000, 0.00000000, 100, '"+date+"', "+prodId+", 0.00000000, 0.00000000, "+standardCost+", "+standardCost+", 1, 1, 1, 1)";
		
		jdbcTemplate.execute(updateCostLogDetailQuery);
	}

	/**
	 * @param prodId
	 * @return
	 */
	private int getExistingQuantity(int prodId) {
		String existingQuantityQuery = "Select quantity from base_inventory where ProdId="+prodId;
		int qty = jdbcTemplate.queryForInt(existingQuantityQuery);
		return qty;
	}

	/**
	 * @param prodId
	 */
	private void updateInventoryQtyTotal(int prodId) {
		String updateInvQtyTotalQuery = "Update base_inventoryquantitytotal set quantitysold = quantitysold+1 where ProdId="+prodId;
		jdbcTemplate.execute(updateInvQtyTotalQuery);
	}

	/**
	 * @param date
	 * @param invLogId
	 * @param order
	 * @param prodId
	 */
	private void updateInventory(int qty, String date, Integer invLogId, Order order,
			int prodId) {
		
		
		String invQuery="Update base_inventory SET quantity=quantity-"+ order.getQuantity()+
				"where ProdId="+prodId;
		
		String invLogDetailQuery = "INSERT INTO BASE_InventoryLogDetail"+
		           "(InventoryLogBatchId,FromLocationId,FromSublocation,ToLocationId,"+
		           "ToSublocation,Quantity,CreatedUserId,CreatedDttm,FromQuantityBefore,"+
		           "FromQuantityAfter,ToQuantityBefore,ToQuantityAfter,ProdId)"+
		     "VALUES" +
		           "("+invLogId+",NULL,'',100,''," +order.getQuantity()+",100,'"+date+","+
		           "NULL,NULL,"+qty+","+(qty-order.getQuantity())+","+prodId+")";
			
		jdbcTemplate.execute(invQuery);
		
		jdbcTemplate.execute(invLogDetailQuery);
	}

	/**
	 * @param soId
	 * @param lineNumber
	 * @param order
	 * @param prodId
	 */
	private void addSalesOrderLine(Integer soId, int lineNumber, Order order,
			int prodId, Double standardCost, String date) {
		String soLineQuery="INSERT INTO SO_SalesOrder_Line( SalesOrderId"+
							",Version ,LineNum,Description,Quantity,UnitPrice" +
							",Discount,SubTotal,ItemTaxCodeId,QuantityUom," +
							"QuantityDisplay,DiscountIsPercent,ProdId)" +
							"VALUES ("+soId+",1,"+lineNumber+",''"+
							","+ order.getQuantity()+",0.0,0.0,0.0,100,'pcs'"+
							","+ order.getQuantity()+",1,"+prodId+")";
		jdbcTemplate.execute(soLineQuery);
		
		String soInvoiceLineQuery="INSERT INTO SO_SalesOrderInvoice_Line (SalesOrderInvoiceLineId, SalesOrderId, Version, LineNum, Description, Quantity, UnitPrice, Discount, SubTotal, ItemTaxCodeId, QuantityUom, QuantityDisplay, DiscountIsPercent, Cost, ProdId, CostCurrencyId)"+ 
								  "VALUES ("+soId+",1,"+lineNumber+", '', "+ order.getQuantity()+", 0.00000, 0.00000, 0.00000, 100, 'pcs.', "+ order.getQuantity()+", 1, NULL, "+prodId+", 1)";
		jdbcTemplate.execute(soInvoiceLineQuery);
		
		Double cost = standardCost*order.getQuantity();
		String packLineQuery = "INSERT INTO SO_SalesOrderPack_Line (SalesOrderPackLineId, SalesOrderId, Version, LineNum, Description, Quantity, ContainerNumber, IsShipped, QuantityUom, QuantityDisplay, Cost, ProdId, TotalAverageCost, CostCurrencyId)"+ 
							   "VALUES ("+soId+",1,"+lineNumber+", '', "+ order.getQuantity()+", '1', 1, 'pcs.', "+ order.getQuantity()+", "+cost+", "+prodId+", 0.00000000, 1)";
		jdbcTemplate.execute(packLineQuery);
		
		String pickLIneQuery = "INSERT INTO SO_SalesOrderPick_Line (SalesOrderPickLineId, SalesOrderId, Version, LineNum, Description, Quantity, LocationId, Sublocation, IsShipped, QuantityUom, QuantityDisplay, ProdId)"+ 
							   "VALUES ("+soId+",1,"+lineNumber+", '', "+ order.getQuantity()+", 100, '', 1, 'pcs.', "+ order.getQuantity()+", "+prodId+")";
		jdbcTemplate.execute(pickLIneQuery);

		String shipLineQuery = "INSERT INTO SO_SalesOrderShip_Line (SalesOrderShipLineId, SalesOrderId, Version, LineNum, ShippedDate, Carrier, TrackingNumber, Description, IsShipped, InventoryBatchLogId)" +
							   "VALUES ("+soId+",1,"+lineNumber+", '+date+', '', '', '', 1, NULL)";
		jdbcTemplate.execute(shipLineQuery);
		
		int shipLineId = jdbcTemplate.queryForInt(maxShipLineIdQuery);
		String shipContainerQuery = "INSERT INTO SO_SalesOrderShipContainer (SalesOrderShipContainerId, Version, SalesOrderShipLineId, ContainerNumber) VALUES (1, "+shipLineId+", '1')";
		jdbcTemplate.execute(shipContainerQuery);

	}

	/**
	 * @return
	 */
	private int getInvBatchLogId() {
		return jdbcTemplate.queryForInt(maxInvBatchLogIdQuery);
	}
	
	/**
	 * @return
	 */
	private int getInvBatchLogCostId() {
		return jdbcTemplate.queryForInt(maxInvBatchLogCostIdQuery);
	}
	
	private void updateSOTable(int orderNum,String date){
		String soQuery= "INSERT INTO SO_SalesOrder( Version "+
				",OrderStatus,OrderNumber,OrderDate,CustomerId"+
				",SalesRep,PONumber,RequestShipDate,PaymentTermsId,DueDate"+
				",CalculatedDueDate,PricingSchemeId,OrderRemarks,OrderSubTotal,OrderTax1,OrderTax2"+
				",OrderExtra,OrderTotal,OrderTaxCodeId,Tax1Rate"+
				",Tax2Rate,CalculateTax2OnTax1"+
				",Tax1Name,Tax2Name"+
				",Email,PickedDate,PickingRemarks"+
				",PackedDate,PackingRemarks"+
				",ShippingRemarks"+
				",PaymentMethod,IsStandAloneInvoice,InvoicedDate,DatePaid"+
				",InvoiceRemarks"+
				",InvoiceSubTotal,InvoiceTax1"+
				",InvoiceTax2,InvoiceExtra"+
				",InvoiceTotal,AmountPaid"+
				",InvoiceBalance,ReturnDate"+
				",ReturnRemarks,ReturnSubTotal"+
				",ReturnTax1,ReturnTax2"+
				",ReturnExtra,ReturnTotal"+
				",ReturnFee,ReturnRefunded"+
				",ReturnCredit,ReturnInventoryBatchLogId"+
				",RestockRemarks,ContactName"+
				",Phone,BillingAddress1"+
				",BillingAddress2,BillingCity"+
				",BillingState,BillingCountry"+
				",BillingPostalCode,BillingAddressRemarks"+
				",BillingAddressType,ShippingAddress1"+
				",ShippingAddress2,ShippingCity"+
				",ShippingState,ShippingCountry"+
				",ShippingPostalCode,ShippingAddressRemarks"+
				",ShippingAddressType,Custom1"+
				",Custom2,Custom3"+
				",Custom4,Custom5"+
				",LastModUserId"+
				",LastModDttm"+
				",AutoInvoice"+
				",ParentSalesOrderId"+
				",SplitPartNumber"+
				",TaxOnShipping"+
				",LocationId"+
				",IsFullWorkflow"+
				",ShowShipping"+
				",ShipToCompanyName"+
				",CurrencyId"+
				",ExchangeRate)"+
				"VALUES ( 1, 6, '"+ORDER_NUM_PREFIX+orderIdFormatter.format(orderNum)+"','"+date+
				            "',100,'','',NULL,NULL,NULL,1,100,'',0.0,0.0,0.0,NULL,0.0,100,0.0,0.0,0"+
				            ",'','','',NULL,'',NULL,'','','',0,'"+date+"',NULL,'',0.0,0.0"+
				            ",0.0,NULL,0.0,0.0,0.0,NULL,'',0.0,0.0,0.0,NULL,0.0,0.0,0.0,0.0,NULL,''"+
				            ",'','','','','','','','','',NULL,'','','','','','','',NULL,'','','','',''"+
				            ",100,'"+date+"',1,NULL,NULL,0,NULL,1,1,'',1,1.0)";
		
		jdbcTemplate.update(soQuery);
	}
	
	private String getEscapedSKU(Order order){
		return StringEscapeUtils.escapeSql(order.getSku());
	}
}
