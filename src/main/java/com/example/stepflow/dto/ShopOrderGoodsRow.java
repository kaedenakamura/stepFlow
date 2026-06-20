package com.example.stepflow.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 発注商品選択画面の1行分（商品＋カテゴリ＋連携倉庫の在庫合計）。
 * Entity をそのまま渡さず、画面用に必要な項目だけまとめる DTO。
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopOrderGoodsRow {
    private Integer goodsId;
    private String goodsName;
    private Integer categoryId;
    private String categoryName;

    /** 自店舗が連携している倉庫の、当該商品の在庫数合計 */
    private Integer linkedWarehouseTotalStock;
}

