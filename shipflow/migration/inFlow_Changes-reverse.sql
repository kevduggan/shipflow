--
--INVENTORY UPDATES
--

--1 Table Changes for BASE_InventoryLogBatch
INSERT INTO BASE_InventoryLogBatch (InventoryLogBatchId, BatchDate, TransactionDate, BatchType, RecordId, Remarks, CreatedUserId, CreatedDttm) 
										VALUES (298, '2013-09-02 21:06:38.450', '2013-09-02 21:06:38.483', 3, 152, 'SO-000048', 100, '2013-09-02 21:06:38.483')
INSERT INTO BASE_InventoryLogBatch (InventoryLogBatchId, BatchDate, TransactionDate, BatchType, RecordId, Remarks, CreatedUserId, CreatedDttm) 
										VALUES (299, '2013-09-02 21:06:39.157', '2013-09-02 21:06:39.157', 4, 152, 'SO-000048', 100, '2013-09-02 21:06:39.157')

--2 Table Changes for BASE_InventoryCostLogBatch
INSERT INTO BASE_InventoryCostLogBatch (InventoryCostLogBatchId, TransactionDate, BatchType, RecordId, Remarks, InventoryLogBatchId, CreatedUserId, CreatedDttm) 
VALUES (3484, '2013-09-02 21:06:38.450', 3, 152, 'SO-000048', 298, 100, '2013-09-02 21:06:38.540')
INSERT INTO BASE_InventoryCostLogBatch (InventoryCostLogBatchId, TransactionDate, BatchType, RecordId, Remarks, InventoryLogBatchId, CreatedUserId, CreatedDttm) 
VALUES (3485, '2013-09-02 21:06:39.157', 4, 152, 'SO-000048', 299, 100, '2013-09-02 21:06:39.157')

--3 Table Changes for BASE_InventoryLogDetail
INSERT INTO BASE_InventoryLogDetail (InventoryLogDetailId, InventoryLogBatchId, FromLocationId, FromSublocation, ToLocationId, ToSublocation, Quantity, CreatedUserId, CreatedDttm, FromQuantityBefore, FromQuantityAfter, ToQuantityBefore, ToQuantityAfter, ProdId) 
VALUES (13920, 298, 100, '', 1, '', 10.0000, 100, '2013-09-02 21:06:38.897', 210.0000, 200.0000, 0.0000, 10.0000, 1058)
INSERT INTO BASE_InventoryLogDetail (InventoryLogDetailId, InventoryLogBatchId, FromLocationId, FromSublocation, ToLocationId, ToSublocation, Quantity, CreatedUserId, CreatedDttm, FromQuantityBefore, FromQuantityAfter, ToQuantityBefore, ToQuantityAfter, ProdId) 
VALUES (13921, 299, 1, '', NULL, '', 10.0000, 100, '2013-09-02 21:06:39.157', 10.0000, 0.0000, NULL, NULL, 1058)

--4 Table Changes for BASE_InventoryQuantityTotal
UPDATE BASE_InventoryQuantityTotal SET QuantityOnHand = 200.0000 WHERE ProdId = 1058

--5 Table Changes for BASE_InventoryCostLogDetail
INSERT INTO BASE_InventoryCostLogDetail (InventoryCostLogDetailId, InventoryCostLogBatchId, Quantity, QuantityBefore, QuantityAfter, AverageCostBefore, AverageCostAfter, CreatedUserId, CreatedDttm, ProdId, LastPurchaseCostBefore, LastPurchaseCostAfter, StandardCostBefore, StandardCostAfter, CostingMethodBefore, CostingMethodAfter, CurrencyIdBefore, CurrencyIdAfter) 
VALUES (9986, 3485, -10.0000, 210.0000, 200.0000, 0.00000000, 0.00000000, 100, '2013-09-02 21:06:39.167', 1058, 0.00000000, 0.00000000, 1.24000000, 1.24000000, 1, 1, 1, 1)

--6 Table Changes for BASE_Inventory
UPDATE BASE_Inventory SET Quantity = 200.0000 WHERE LocationId = 100 AND Sublocation = '' AND ProdId = 1058

--7 Table Changes for BASE_InventoryCost
UPDATE BASE_InventoryCost SET InventoryCount = 200.0000 WHERE ProdId = 1058

--
--SALES ORDER UPDATES
--
--8 Table Changes for SO_SalesOrder
INSERT INTO SO_SalesOrder (SalesOrderId, Version, OrderStatus, OrderNumber, OrderDate, CustomerId, SalesRep, PONumber, RequestShipDate, PaymentTermsId, DueDate, CalculatedDueDate, PricingSchemeId, OrderRemarks, OrderSubTotal, OrderTax1, OrderTax2, OrderExtra, OrderTotal, OrderTaxCodeId, Tax1Rate, Tax2Rate, CalculateTax2OnTax1, Tax1Name, Tax2Name, Email, PickedDate, PickingRemarks, PackedDate, PackingRemarks, ShippingRemarks, PaymentMethod, IsStandAloneInvoice, InvoicedDate, DatePaid, InvoiceRemarks, InvoiceSubTotal, InvoiceTax1, InvoiceTax2, InvoiceExtra, InvoiceTotal, AmountPaid, InvoiceBalance, ReturnDate, ReturnRemarks, ReturnSubTotal, ReturnTax1, ReturnTax2, ReturnExtra, ReturnTotal, ReturnFee, ReturnRefunded, ReturnCredit, ReturnInventoryBatchLogId, RestockRemarks, ContactName, Phone, BillingAddress1, BillingAddress2, BillingCity, BillingState, BillingCountry, BillingPostalCode, BillingAddressRemarks, BillingAddressType, ShippingAddress1, ShippingAddress2, ShippingCity, ShippingState, ShippingCountry, ShippingPostalCode, ShippingAddressRemarks, ShippingAddressType, Custom1, Custom2, Custom3, Custom4, Custom5, LastModUserId, LastModDttm, AutoInvoice, ParentSalesOrderId, SplitPartNumber, TaxOnShipping, LocationId, IsFullWorkflow, ShowShipping, ShipToCompanyName, CurrencyId, ExchangeRate) 
VALUES (152, 1, 6, 'SO-000048', '2013-09-02 21:06:36.770', 100, '', '', NULL, NULL, NULL, 1, 100, '', 0.00000, 0.00000, 0.00000, NULL, 0.00000, 100, 0.00000, 0.00000, 0, '', '', '', NULL, '', NULL, '', '', '', 0, '2013-09-02 21:06:37.527', NULL, '', 0.00000, 0.00000, 0.00000, NULL, 0.00000, 0.00000, 0.00000, NULL, '', 0.00000, 0.00000, 0.00000, NULL, 0.00000, 0.00000, 0.00000, 0.00000, NULL, '', '', '', '', '', '', '', '', '', '', NULL, '', '', '', '', '', '', '', NULL, '', '', '', '', '', 100, '2013-09-02 21:06:37.830', 1, NULL, NULL, 0, NULL, 1, 1, '', 1, 1.0000000000)

--9 Table Changes for SO_SalesOrder_Line
INSERT INTO SO_SalesOrder_Line (SalesOrderLineId, SalesOrderId, Version, LineNum, Description, Quantity, UnitPrice, Discount, SubTotal, ItemTaxCodeId, QuantityUom, QuantityDisplay, DiscountIsPercent, ProdId) 
VALUES (7068, 152, 1, 1, '', 10.0000, 0.00000, 0.00000, 0.00000, 100, 'pcs.', 10.0000, 1, 1058)

--10 Table Changes for SO_SalesOrderInvoice_Line
INSERT INTO SO_SalesOrderInvoice_Line (SalesOrderInvoiceLineId, SalesOrderId, Version, LineNum, Description, Quantity, UnitPrice, Discount, SubTotal, ItemTaxCodeId, QuantityUom, QuantityDisplay, DiscountIsPercent, Cost, ProdId, CostCurrencyId) 
VALUES (5256, 152, 1, 1, '', 10.0000, 0.00000, 0.00000, 0.00000, 100, 'pcs.', 10.0000, 1, NULL, 1058, 1)

--11 Table Changes for SO_SalesOrderPack_Line
INSERT INTO SO_SalesOrderPack_Line (SalesOrderPackLineId, SalesOrderId, Version, LineNum, Description, Quantity, ContainerNumber, IsShipped, QuantityUom, QuantityDisplay, Cost, ProdId, TotalAverageCost, CostCurrencyId) 
VALUES (5256, 152, 1, 1, '', 10.0000, '1', 1, 'pcs.', 10.0000, 12.40000000, 1058, 0.00000000, 1)

--12 Table Changes for SO_SalesOrderPick_Line
INSERT INTO SO_SalesOrderPick_Line (SalesOrderPickLineId, SalesOrderId, Version, LineNum, Description, Quantity, LocationId, Sublocation, IsShipped, QuantityUom, QuantityDisplay, ProdId) 
VALUES (5263, 152, 1, 1, '', 10.0000, 100, '', 1, 'pcs.', 10.0000, 1058)

--13 Table Changes for SO_SalesOrderShip_Line
INSERT INTO SO_SalesOrderShip_Line (SalesOrderShipLineId, SalesOrderId, Version, LineNum, ShippedDate, Carrier, TrackingNumber, Description, IsShipped, InventoryBatchLogId) 
VALUES (112, 152, 1, 1, '2013-09-02 00:00:00.000', '', '', '', 1, NULL)

--14 Table Changes for SO_SalesOrderShipContainer
INSERT INTO SO_SalesOrderShipContainer (SalesOrderShipContainerId, Version, SalesOrderShipLineId, ContainerNumber) VALUES (112, 1, 112, '1')


