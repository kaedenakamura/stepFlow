package com.example.stepflow.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.entity.Goods;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.entity.WarehouseStock;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.WarehouseRepository;
import com.example.stepflow.repository.WarehouseStockRepository;

@Service
public class AdminWarehouseService {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private WarehouseStockRepository warehouseStockRepository;

    /**
     * 倉庫新規登録 + 有効な全商品に warehouse_stock（在庫0）を作成
     */
    @Transactional
    public void registerWarehouseWithInitialStock(Warehouse warehouse) {
        warehouse.setDeleteFlag(0);
        warehouseRepository.save(warehouse);

        Integer warehouseId = warehouse.getWarehouseId();

        List<Goods> goodsList = goodsRepository.findByDeleteFlagOrderByGoodsIdDesc(0);
        for (Goods goods : goodsList) {
            WarehouseStock row = new WarehouseStock();
            row.setWarehouseId(warehouseId);
            row.setGoodsId(goods.getGoodsId());
            row.setWarehouseStock(0);
            row.setDeleteFlag(0);
            warehouseStockRepository.save(row);
        }
    }

    /**
     * 倉庫編集（在庫テーブルは触らない）
     */
    @Transactional
    public void updateWarehouse(Warehouse warehouse) {
        warehouse.setDeleteFlag(0);
        warehouseRepository.save(warehouse);
    }

    /**
     * 倉庫論理削除 + 関連する倉庫在庫も論理削除
     */
    @Transactional
    public void deleteWarehouseWithStocks(Integer warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
        if (warehouse == null || !Integer.valueOf(0).equals(warehouse.getDeleteFlag())) {
            return;
        }

        warehouse.setDeleteFlag(1);
        warehouseRepository.save(warehouse);

        for (WarehouseStock stock : warehouseStockRepository.findByWarehouseIdAndDeleteFlag(warehouseId, 0)) {
            stock.setDeleteFlag(1);
            warehouseStockRepository.save(stock);
        }
    }
}
