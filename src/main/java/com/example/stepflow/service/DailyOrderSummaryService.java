package com.example.stepflow.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.dto.OrderRankingRow;
import com.example.stepflow.entity.DailyOrderSummary;
import com.example.stepflow.entity.ShopOrder;
import com.example.stepflow.repository.DailyOrderSummaryRepository;

@Service
public class DailyOrderSummaryService {

    @Autowired
    private DailyOrderSummaryRepository dailyOrderSummaryRepository;

    /** 発注登録時に日次サマリへ加算 */
    @Transactional
    public void addFromShopOrder(ShopOrder order) {
        if (order == null || order.getShopId() == null || order.getGoodsId() == null) {
            return;
        }
        int quantity = order.getOrderQuantity() != null ? order.getOrderQuantity() : 0;
        if (quantity < 1) {
            return;
        }

        LocalDate countDate = order.getOrderDate() != null
                ? order.getOrderDate().toLocalDate()
                : LocalDate.now();
        addQuantity(order.getShopId(), order.getGoodsId(), quantity, countDate);
    }

    @Transactional
    public void addQuantity(Integer shopId, Integer goodsId, int quantity, LocalDate countDate) {
        DailyOrderSummary summary = dailyOrderSummaryRepository
                .findByCountDateAndShopIdAndGoodsId(countDate, shopId, goodsId)
                .orElseGet(() -> {
                    DailyOrderSummary created = new DailyOrderSummary();
                    created.setCountDate(countDate);
                    created.setShopId(shopId);
                    created.setGoodsId(goodsId);
                    created.setGoodsAmount(0);
                    return created;
                });

        summary.setGoodsAmount(summary.getGoodsAmount() + quantity);
        dailyOrderSummaryRepository.save(summary);
    }

    /** ダッシュボード用：期間内ランキングを取得し、順位番号を付ける */
    @Transactional(readOnly = true)
    public List<OrderRankingRow> findRanking(LocalDate fromDate, LocalDate toDate, Integer shopId) {
        List<OrderRankingRow> rows = dailyOrderSummaryRepository.findRanking(fromDate, toDate, shopId);
        int rank = 1;
        for (OrderRankingRow row : rows) {
            row.setRank(rank++);
        }
        return rows;
    }
}
