package com.example.stepflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.dto.ShopOrderGoodsRow;
import com.example.stepflow.entity.Category;
import com.example.stepflow.entity.Goods;
import com.example.stepflow.entity.Relation;
import com.example.stepflow.repository.CategoryRepository;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.RelationRepository;
import com.example.stepflow.repository.WarehouseStockRepository;

@Service
public class ShopOrderSelectionService {

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WarehouseStockRepository warehouseStockRepository;

    /** カテゴリ絞り込み用のプルダウン（有効なカテゴリのみ） */
    @Transactional(readOnly = true)
    public List<Category> findActiveCategories() {
        return categoryRepository.findByDeleteFlagOrderByCategoryIdDesc(0);
    }

    /**
     * 発注商品選択画面用の一覧。
     *
     * @param shopId     ログイン店舗
     * @param categoryId null なら全カテゴリ
     */
    @Transactional(readOnly = true)
    public List<ShopOrderGoodsRow> findGoodsRowsForShop(Integer shopId, Integer categoryId) {
        if (shopId == null) {
            throw new IllegalArgumentException("店舗IDが指定されていません");
        }

        List<Integer> linkedWarehouseIds = relationRepository
                .findByDeleteFlagAndShopIdWithShopAndWarehouse(0, shopId)
                .stream()
                .map(Relation::getWarehouseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Goods> goodsList = (categoryId != null)
                ? goodsRepository.findByDeleteFlagAndCategoryIdWithCategory(0, categoryId)
                : goodsRepository.findByDeleteFlagWithCategory(0);

        List<ShopOrderGoodsRow> rows = new ArrayList<>();
        for (Goods goods : goodsList) {
            String categoryName = "";
            if (goods.getCategory() != null) {
                categoryName = goods.getCategory().getCategoryName();
            }

            int totalStock = 0;
            if (!linkedWarehouseIds.isEmpty()) {
                Integer sum = warehouseStockRepository.sumWarehouseStockByGoodsIdAndWarehouseIds(
                        goods.getGoodsId(),
                        linkedWarehouseIds,
                        0);
                totalStock = sum != null ? sum : 0;
            }

            rows.add(new ShopOrderGoodsRow(
                    goods.getGoodsId(),
                    goods.getGoodsName(),
                    goods.getCategoryId(),
                    categoryName,
                    totalStock));
        }
        return rows;
    }
}
