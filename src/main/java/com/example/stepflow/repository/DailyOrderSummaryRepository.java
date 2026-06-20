package com.example.stepflow.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.stepflow.dto.OrderRankingRow;
import com.example.stepflow.entity.DailyOrderSummary;

@Repository
public interface DailyOrderSummaryRepository extends JpaRepository<DailyOrderSummary, Integer> {

    Optional<DailyOrderSummary> findByCountDateAndShopIdAndGoodsId(
            LocalDate countDate, Integer shopId, Integer goodsId);

    /**
     * 期間内の発注数量を商品ごとに合計（shopId が null のときは全店舗）
     */
    @Query("""
            SELECT new com.example.stepflow.dto.OrderRankingRow(
                g.goodsId, g.goodsName, SUM(d.goodsAmount))
            FROM DailyOrderSummary d
            JOIN Goods g ON g.goodsId = d.goodsId AND g.deleteFlag = 0
            WHERE d.countDate BETWEEN :fromDate AND :toDate
              AND (:shopId IS NULL OR d.shopId = :shopId)
            GROUP BY g.goodsId, g.goodsName
            ORDER BY SUM(d.goodsAmount) DESC
            """)
    List<OrderRankingRow> findRanking(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("shopId") Integer shopId);
}
