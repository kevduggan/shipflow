USE [inFlow]
GO
--Table Changes for [dbo].[BASE_InventoryLogBatch]
DELETE FROM [dbo].[BASE_InventoryLogBatch] WHERE [InventoryLogBatchId] = 298
DELETE FROM [dbo].[BASE_InventoryLogBatch] WHERE [InventoryLogBatchId] = 299
GO
--Table Changes for [dbo].[BASE_InventoryCostLogBatch]
DELETE FROM [dbo].[BASE_InventoryCostLogBatch] WHERE [InventoryCostLogBatchId] = 3484
DELETE FROM [dbo].[BASE_InventoryCostLogBatch] WHERE [InventoryCostLogBatchId] = 3485
GO
--Table Changes for [dbo].[BASE_InventoryLogDetail]
DELETE FROM [dbo].[BASE_InventoryLogDetail] WHERE [InventoryLogDetailId] = 13920
DELETE FROM [dbo].[BASE_InventoryLogDetail] WHERE [InventoryLogDetailId] = 13921
GO
--Table Changes for [dbo].[BASE_InventoryQuantityTotal]
UPDATE [dbo].[BASE_InventoryQuantityTotal] SET [QuantityOnHand] = 210.0000 WHERE [ProdId] = 1058
GO
--Table Changes for [dbo].[BASE_InventoryCostLogDetail]
DELETE FROM [dbo].[BASE_InventoryCostLogDetail] WHERE [InventoryCostLogDetailId] = 9986
GO
--Table Changes for [dbo].[BASE_Inventory]
UPDATE [dbo].[BASE_Inventory] SET [Quantity] = 210.0000 WHERE [LocationId] = 100 AND [Sublocation] = '' AND [ProdId] = 1058
GO
--Table Changes for [dbo].[BASE_InventoryCost]
UPDATE [dbo].[BASE_InventoryCost] SET [InventoryCount] = 210.0000 WHERE [ProdId] = 1058
GO
--Table Changes for [dbo].[SO_SalesOrder]
DELETE FROM [dbo].[SO_SalesOrder] WHERE [SalesOrderId] = 152
GO
--Table Changes for [dbo].[SO_SalesOrder_Line]
DELETE FROM [dbo].[SO_SalesOrder_Line] WHERE [SalesOrderLineId] = 7068
GO
--Table Changes for [dbo].[SO_SalesOrderInvoice_Line]
DELETE FROM [dbo].[SO_SalesOrderInvoice_Line] WHERE [SalesOrderInvoiceLineId] = 5256
GO
--Table Changes for [dbo].[SO_SalesOrderPack_Line]
DELETE FROM [dbo].[SO_SalesOrderPack_Line] WHERE [SalesOrderPackLineId] = 5256
GO
--Table Changes for [dbo].[SO_SalesOrderPick_Line]
DELETE FROM [dbo].[SO_SalesOrderPick_Line] WHERE [SalesOrderPickLineId] = 5263
GO
--Table Changes for [dbo].[SO_SalesOrderShip_Line]
DELETE FROM [dbo].[SO_SalesOrderShip_Line] WHERE [SalesOrderShipLineId] = 112
GO
--Table Changes for [dbo].[SO_SalesOrderShipContainer]
DELETE FROM [dbo].[SO_SalesOrderShipContainer] WHERE [SalesOrderShipContainerId] = 112
GO
