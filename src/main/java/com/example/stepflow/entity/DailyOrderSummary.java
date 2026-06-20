package com.example.stepflow.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "daily_order_summary")
@Data
public class DailyOrderSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Integer summaryId;

    /** 日付 */
    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    /** 店舗ID */
    @Column(name = "shop_id", nullable = false)
    private Integer shopId;

    /** 商品ID */
    @Column(name = "goods_id", nullable = false)
    private Integer goodsId;

    /** その日・店舗・商品の発注数量合計 */
    @Column(name = "goods_amount", nullable = false)
    private Integer goodsAmount = 0;
}
