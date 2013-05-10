package com.lollipopmedia.shipflow.inflow;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.lollipopmedia.shipflow.Order;
import com.lollipopmedia.shipflow.ShipflowIntegrator;

/**
 * writes to the inflow DB.
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
	public void updateInflow(List<Order> orders) {
		String date = sdf.format(new Date());
		Map<String, Object> soResults = jdbcTemplate.queryForMap(maxSOIdQuery);
		Integer soId = ((Integer) soResults.get("SalesOrderId"))+1;
		int orderNum = (Integer.parseInt(((String)soResults.get("OrderNumber")).substring(ORDER_NUM_PREFIX.length())))+1;
		updateSOTable(orderNum,date);
		Integer invLogId = jdbcTemplate.queryForInt(maxInvBatchLogIdQuery)+1;
		updateInventoryLogBatch(date);
		int lineNumber = 1;
		for(Order order: orders){
			String prodIdQuery = "Select ProdId from base_product where name='"+order.getSku()+"'";
			int prodId = jdbcTemplate.queryForInt(prodIdQuery);
			String invQuery="Update base_inventory SET quantity=quantity-"+ order.getQuantity()+
					"where ProdId="+prodId;
			//add new sales order
					
			String soLineQuery="INSERT INTO SO_SalesOrder_Line( SalesOrderId"+
								",Version ,LineNum,Description,Quantity,UnitPrice" +
								",Discount,SubTotal,ItemTaxCodeId,QuantityUom," +
								"QuantityDisplay,DiscountIsPercent,ProdId)" +
								"VALUES ("+soId+",1,"+lineNumber+",''"+
								","+ order.getQuantity()+",0.0,0.0,0.0,100,'pcs'"+
								","+ order.getQuantity()+",1,"+prodId+")";
			
			String invLogDetailQuery = "INSERT INTO BASE_InventoryLogDetail"+
			           "(InventoryLogBatchId,FromLocationId,FromSublocation,ToLocationId,"+
			           "ToSublocation,Quantity,CreatedUserId,CreatedDttm,FromQuantityBefore,"+
			           "FromQuantityAfter,ToQuantityBefore,ToQuantityAfter,ProdId)"+
			     "VALUES" +
			           "("+invLogId+",NULL,'',100,''," +order.getQuantity()+",100,'"+date+","+
			           ""
			           		"<FromSublocation, nvarchar(100),>
			           ,<ToLocationId, int,>
			           ,<ToSublocation, nvarchar(100),>
			           ,<Quantity, decimal(18,4),>
			           ,<CreatedUserId, int,>
			           ,<CreatedDttm, datetime,>
			           ,<FromQuantityBefore, decimal(18,4),>
			           ,<FromQuantityAfter, decimal(18,4),>
			           ,<ToQuantityBefore, decimal(18,4),>
			           ,<ToQuantityAfter, decimal(18,4),>
			           ,<ProdId, int,>)
           		
			jdbcTemplate.execute(invQuery);
			jdbcTemplate.execute(soLineQuery);
			
			//update ids
			soLineId++;
			lineNumber++;
		}
		
		logger.info("Inflow updated successfully");
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
	
	private void updateInventoryLogBatch(String date){

		String logQuery = "INSERT INTO BASE_InventoryLogBatch ("+
		           			"BatchDate, TransactionDate,BatchType,RecordId "+
		           			",Remarks,CreatedUserId,CreatedDttm) "+
		           			"VALUES "+
		           			"('"+date+"','"+date+"',1,NULL,'',100,'"+date+")";
		jdbcTemplate.update(logQuery);

	}
}
