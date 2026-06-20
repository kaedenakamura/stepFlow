package com.example.stepflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 店舗ダッシュボード：発注商品ランキング1行 */
@Data//Getter/Setterを自動生成
@NoArgsConstructor//引数のないコンストラクタを自動生成
@AllArgsConstructor//引数のあるコンストラクタを自動生成
public class OrderRankingRow{
    private int rank; //ランキング
    private Integer goodsId; //商品ID
    private String goodsName; //商品名
    private long totalAmount; //合計金額

   /**
     * JPQL の SELECT new ... OrderRankingRow(goodsId, goodsName, SUM(...))
     * が呼ぶコンストラクタ。rank はこの時点ではまだ付けない。
     */
   public OrderRankingRow(Integer goodsId, String goodsName, Long totalAmount) {
    this.goodsId = goodsId;         // 引数をフィールドにセット
    this.goodsName = goodsName;
    // SUM の結果が null のときは 0 として扱う（安全のため）
    this.totalAmount = totalAmount != null ? totalAmount : 0L;
}
}