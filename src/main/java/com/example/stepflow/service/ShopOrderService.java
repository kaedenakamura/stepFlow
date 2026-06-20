package com.example.stepflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.stepflow.repository.ShopOrderRepository;
import com.example.stepflow.repository.RelationRepository;
import com.example.stepflow.repository.GoodsRepository; 
import com.example.stepflow.entity.ShopOrder;
import com.example.stepflow.entity.Relation;
import com.example.stepflow.entity.Goods;
import com.example.stepflow.form.ShopOrderForm;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ShopOrderService {
    
    @Autowired
    private ShopOrderRepository shopOrderRepository;

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private DailyOrderSummaryService dailyOrderSummaryService;

    
    /** 自店舗の発注一覧（倉庫名・商品名表示用に LAZY を読む） */
    @Transactional(readOnly = true)
    public List<ShopOrder> findActiveByShopId(Integer shopId){
        if(shopId == null){
            throw new IllegalArgumentException("店舗IDが指定されていません");
        }
        List<ShopOrder> orders = shopOrderRepository.findByShopIdAndDeleteFlagOrderByShopOrderIdDesc(shopId,0);
        warmupLazyRelations(orders);
        return orders;
    }

    /** 発注履歴（/shop/sales 用）：自店舗 + 期間/ステータスの任意絞り込み */
    @Transactional(readOnly = true)
    public List<ShopOrder> findActiveByShopIdFiltered(
            Integer shopId,
            String status,
            LocalDate from,
            LocalDate to) {

        if (shopId == null) {
            throw new IllegalArgumentException("店舗IDが指定されていません");
        }

        boolean hasStatus = status != null && !status.isBlank(); // ステータス指定あり？
        boolean hasFrom = from != null; // 開始日指定あり？
        boolean hasTo = to != null;     // 終了日指定あり？

        List<ShopOrder> orders;

        // ===== パターンA: 期間指定なし（from/to が両方 null） =====
        if (!hasFrom && !hasTo) {
            if (hasStatus) {
                // ステータスだけで絞る
                orders = shopOrderRepository.findByShopIdAndDeleteFlagAndOrderStatusOrderByShopOrderIdDesc(
                        shopId, 0, status.strip());
            } else {
                // 条件なし（既存一覧と同じ）
                orders = shopOrderRepository.findByShopIdAndDeleteFlagOrderByShopOrderIdDesc(shopId, 0);
            }

        // ===== パターンB: 期間指定あり（片側でも入力があれば範囲絞り込み） =====
        } else {
            // 欠けている側を極端な値で埋めて、片側だけでも動くようにする
            LocalDate fromDate = hasFrom ? from : LocalDate.of(1970, 1, 1);
            LocalDate toDate = hasTo ? to : LocalDate.of(2999, 12, 31);

            // DBの order_date は TIMESTAMP のため、日付→日付時刻に変換する
            LocalDateTime fromDateTime = fromDate.atStartOfDay(); // 00:00:00
            LocalDateTime toDateTime = toDate.plusDays(1)          // 翌日 00:00:00
                    .atStartOfDay()
                    .minusNanos(1); // その直前（終日を含める）

            if (hasStatus) {
                orders = shopOrderRepository.findByShopIdAndDeleteFlagAndOrderStatusAndOrderDateBetweenOrderByShopOrderIdDesc(
                        shopId, 0, status.strip(), fromDateTime, toDateTime);
            } else {
                orders = shopOrderRepository.findByShopIdAndDeleteFlagAndOrderDateBetweenOrderByShopOrderIdDesc(
                        shopId, 0, fromDateTime, toDateTime);
            }
        }

        // goods / warehouse は LAZY なので、表示前に名前を読み込む
        warmupLazyRelations(orders);
        return orders;
    }

  /**
     * ShopOrder の goods / warehouse は LAZY（使うまでDB読まない）。
     * 画面表示前に getName() しておかないと、テンプレートでエラーになりやすい。
     */
    private void warmupLazyRelations(List<ShopOrder> orders){
        orders.forEach(o -> {//ShopOrderを一つずつ取り出す
            if(o.getWarehouse() != null){
                o.getWarehouse().getWarehouseName();//倉庫名を取得
            }
            if(o.getGoods() != null){
                o.getGoods().getGoodsName();//商品名を取得
            }
        });
    }




    /** 申請画面用：自店舗に連携されている倉庫だけ */
    @Transactional(readOnly = true)
    public List<Relation> findRelationsForShop(Integer shopId){
        if(shopId == null){
            return List.of();
        }

        
        //ここでrelationが取れるのは、@ManyToOne(fetch = FetchType.LAZY)でLazyロードされているため、
        return relationRepository.findByDeleteFlagAndShopIdWithShopAndWarehouse(0,shopId);
    }

    /** 連携倉庫一覧 有効範囲表示*/
    @Transactional(readOnly = true)
    public List<Goods> findActiveGoods(){
        return goodsRepository.findByDeleteFlagOrderByGoodsIdDesc(0);
    }

      /**
     * 発注登録
     * @return true=成功 / false=連携倉庫でない・商品無効など
     */

    @Transactional
    public boolean registerShopOrder(Integer shopId, ShopOrderForm form) {
        if (shopId == null || form == null) {
            return false;
        }

        Integer warehouseId = form.getWarehouseId();
        Integer goodsId = form.getGoodsId();
        Integer orderQuantity = form.getOrderQuantity();
        if (warehouseId == null || goodsId == null
                || orderQuantity == null || orderQuantity < 1) {
            return false;
        }

        boolean linked = relationRepository
                .findByDeleteFlagAndShopIdWithShopAndWarehouse(0, shopId)
                .stream()
                .anyMatch(r -> Objects.equals(r.getWarehouseId(), warehouseId));
        if (!linked) {
            return false;
        }

        if (goodsRepository.findById(goodsId)
                .filter(g -> Integer.valueOf(0).equals(g.getDeleteFlag()))
                .isEmpty()) {
            return false;
        }

        ShopOrder order = new ShopOrder();
        order.setShopId(shopId);
        order.setWarehouseId(warehouseId);
        order.setGoodsId(goodsId);
        order.setOrderQuantity(orderQuantity);
        order.setOrderStatus("準備中");
        // 新規作成時も更新日時を入れておく（/shop/sales で表示するため）
        order.setUpdateDate(LocalDateTime.now());
        order.setDeleteFlag(0);
        // saveAndFlush: 同一トランザクション内でサマリ更新まで確実に反映
        ShopOrder saved = shopOrderRepository.saveAndFlush(order);
        dailyOrderSummaryService.addFromShopOrder(saved);
        return true;
    }
}
