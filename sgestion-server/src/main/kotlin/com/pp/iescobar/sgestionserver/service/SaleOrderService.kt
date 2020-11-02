package com.pp.iescobar.sgestionserver.service

import com.pp.iescobar.sgestionserver.entity.SaleOrder
import com.pp.iescobar.sgestionserver.repository.ItemRepository
import com.pp.iescobar.sgestionserver.repository.SaleOrderLineRepository
import com.pp.iescobar.sgestionserver.repository.SaleOrderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.util.*
import javax.transaction.Transactional

@Service
class SaleOrderService @Autowired constructor(
        val saleOrderRepository: SaleOrderRepository,
        val saleOrderLineRepository: SaleOrderLineRepository,
        val itemRepository: ItemRepository
) {
    @Transactional
    fun getAllSaleOrders() : List<SaleOrder> = saleOrderRepository.findAllByActive()

    @Transactional
    fun getSaleOrderById(id: Long) : SaleOrder? = saleOrderRepository.findByIdOrNull(id)

    @Transactional
    fun createOrUpdateSaleOrder(saleOrder: SaleOrder) : SaleOrder {
        val saleOrderSaved = saleOrderRepository.save(saleOrder)
        var orderLines = saleOrder.orderLines
        orderLines.forEach { saleOrderLine ->
            itemRepository.findByIdOrNull(saleOrderLine.itemId)?.let {
                if (it.stock >= saleOrderLine.quantity) {
                    it.stock -= saleOrderLine.quantity
                    itemRepository.save(it)
                    saleOrderLine.saleOrder = saleOrderSaved
                    saleOrderLineRepository.save(saleOrderLine)
                } else {
                    throw RuntimeException("Insufficient stock for item with ID: ${it.id}")
                }
            } ?: throw RuntimeException("Item with ID: ${saleOrderLine.itemId} not found.")
        }
        orderLines = saleOrderLineRepository.findAllBySaleOrder(saleOrderSaved)
        saleOrderSaved.orderLines = orderLines
        return saleOrderSaved
    }

    @Transactional
    fun deleteSaleOrder(saleOrder: SaleOrder) {
        saleOrder.active = false
        saleOrder.disabledDate = Date()
        saleOrder.disabledBy = "Test"
        createOrUpdateSaleOrder(saleOrder)
    }
}
