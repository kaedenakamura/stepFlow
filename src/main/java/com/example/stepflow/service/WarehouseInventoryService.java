package com.example.stepflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.stepflow.repository.WarehouseStockRepository;
import com.example.stepflow.entity.WarehouseStock;
import com.example.stepflow.entity.Category;
import com.example.stepflow.repository.CategoryRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 倉庫スタッフ向け：自倉庫の在庫一覧・数量更新。
 */

@Service
public class WarehouseInventoryService {
    @Autowired
    private WarehouseStockRepository warehouseStockRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    /** カテゴリ絞り込み用プルダウン（有効なカテゴリのみ） */
    @Transactional(readOnly = true)
    public List<Category> findActiveCategories() {
        return categoryRepository.findByDeleteFlagOrderByCategoryIdDesc(0);
    }

       /**
     * 自倉庫の有効在庫一覧（delete_flag=0）
     * 画面で商品名を出すときは goods を読む（LAZY のため @Transactional 内で）
     */

    @Transactional(readOnly = true)
    public List<WarehouseStock> findActiveByWarehouseId(Integer warehouseId){
        return findActiveByWarehouseIdFiltered(warehouseId, null);
    }

    /** 自倉庫の在庫一覧（カテゴリ任意絞り込み） */
    @Transactional(readOnly = true)
    public List<WarehouseStock> findActiveByWarehouseIdFiltered(
            Integer warehouseId, Integer categoryId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("所属倉庫が設定されていません。");
        }
        List<WarehouseStock> stocks = warehouseStockRepository.findByWarehouseIdAndDeleteFlag(warehouseId, 0);
        stocks.forEach(s -> {
            if (s.getGoods() != null) {
                s.getGoods().getGoodsName();
                if (s.getGoods().getCategory() != null) {
                    s.getGoods().getCategory().getCategoryName();
                }
            }
        });
        if (categoryId == null) {
            return stocks;
        }
        return stocks.stream()
                .filter(s -> s.getGoods() != null && categoryId.equals(s.getGoods().getCategoryId()))
                .toList();
    }

    /**
     * 在庫数量のみ更新（他倉庫の行は更新しない）
     * @return 成功 true / 失敗 false
     */
    @Transactional
    public boolean updateStockQuantity(Integer warehouseId, Integer warehouseStockId, Integer quantity){
        //パラメータチェック
        if(warehouseId == null || warehouseStockId == null || quantity == null || quantity < 0){
            return false;
        }
        //対象行を取得
        //orElse(null)は、対象行が存在しない場合はnullを返すためのメソッドです。

        WarehouseStock row = warehouseStockRepository.findById(warehouseStockId).orElse(null);
        if (row == null || !Integer.valueOf(0).equals(row.getDeleteFlag())) {
            return false;
        }
        if (!warehouseId.equals(row.getWarehouseId())) {
            return false;
        }
        //在庫数量を更新
        row.setWarehouseStock(quantity);
        //更新した行を保存
        warehouseStockRepository.save(row);
        return true;
    }

    /** 編集画面用：自倉庫の1行だけ取得（他倉庫・削除済みは null） */
    @Transactional(readOnly = true)
    public WarehouseStock findEditableStock(Integer warehouseId, Integer warehouseStockId) {
        if (warehouseId == null || warehouseStockId == null) {
            return null;
        }
        WarehouseStock row = warehouseStockRepository.findById(warehouseStockId).orElse(null);
        if (row == null || !Integer.valueOf(0).equals(row.getDeleteFlag())) {
            return null;
        }
        if (!warehouseId.equals(row.getWarehouseId())) {
            return null;
        }
        if (row.getGoods() != null) {
            row.getGoods().getGoodsName();
        }
        return row;
    }
}
