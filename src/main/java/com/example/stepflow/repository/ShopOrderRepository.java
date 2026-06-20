package com.example.stepflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.stepflow.entity.ShopOrder;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Integer>{
    /** delete_flag=0 を shop_order_id 降順で取得（管理者一覧） *
     * */
    List<ShopOrder> findByDeleteFlagOrderByShopOrderIdDesc(Integer deleteFlag);

    /** 自店舗・有効のみ・新しい発注が上 */
    List<ShopOrder> findByShopIdAndDeleteFlagOrderByShopOrderIdDesc(
            Integer shopId, Integer deleteFlag);

  // 自店舗・有効・ステータス絞り込み
      /**
     * Spring Data がメソッド名から SQL を自動生成する。
     * 意味: shop_id=? AND delete_flag=? AND order_status=? ORDER BY shop_order_id DESC
     */
  List<ShopOrder> findByShopIdAndDeleteFlagAndOrderStatusOrderByShopOrderIdDesc(
        Integer shopId, Integer deleteFlag, String orderStatus);

  /** 自店舗・有効・期間絞り込み（ステータスなし） */
    /**
     * 期間だけ絞る（ステータスは見ない）
     * Between = from 以上 かつ to 以下
     */
  List<ShopOrder> findByShopIdAndDeleteFlagAndOrderDateBetweenOrderByShopOrderIdDesc(
        Integer shopId, Integer deleteFlag, LocalDateTime from, LocalDateTime to
  );

  /** 自店舗・有効・期間＋ステータス絞り込み */
    /**
     * 期間 + ステータス両方で絞る
     */
  List<ShopOrder> findByShopIdAndDeleteFlagAndOrderStatusAndOrderDateBetweenOrderByShopOrderIdDesc(
        Integer shopId, Integer deleteFlag, String orderStatus, LocalDateTime from, LocalDateTime to 
);


    /** 自倉庫・有効のみ・新しい発注が上 */
    List<ShopOrder> findByWarehouseIdAndDeleteFlagOrderByShopOrderIdDesc(
            Integer warehouseId, Integer deleteFlag);


       /** 自倉庫・有効・ステータス絞り込み */
    /**
     * Spring Data がメソッド名から SQL を自動生成する。
     * 意味: warehouse_id=? AND delete_flag=? AND order_status=? ORDER BY shop_order_id DESC
     */
    List<ShopOrder> findByWarehouseIdAndDeleteFlagAndOrderStatusOrderByShopOrderIdDesc(
      Integer warehouseId, Integer deleteFlag, String orderStatus
    );

     /** 自倉庫・有効・期間絞り込み（ステータスなし） */
    /**
     * 期間だけ絞る（ステータスは見ない）
     * Between = from 以上 かつ to 以下
     */
    List<ShopOrder> findByWarehouseIdAndDeleteFlagAndOrderDateBetweenOrderByShopOrderIdDesc(
      Integer warehouseId, Integer deleteFlag, LocalDateTime from, LocalDateTime to
    );

    /** 自倉庫・有効・期間＋ステータス絞り込み */
    /**
     * 期間 + ステータス両方で絞る
     */
    List<ShopOrder> findByWarehouseIdAndDeleteFlagAndOrderStatusAndOrderDateBetweenOrderByShopOrderIdDesc(
Integer warehouseId, Integer deleteFlag, String orderStatus, LocalDateTime from, LocalDateTime to
    );

}

