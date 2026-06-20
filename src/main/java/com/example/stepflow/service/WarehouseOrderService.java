package com.example.stepflow.service;

import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.entity.ShopOrder;
import com.example.stepflow.repository.ShopOrderRepository;

import java.time.LocalDate;

@Service
public class WarehouseOrderService {

    @Autowired
    private ShopOrderRepository shopOrderRepository;

    /** 倉庫が設定してよいステータス（店舗登録・画面の option と揃える） */
    private static final List<String> ALLOWED_STATUSES = List.of("準備中", "発注済");

    /** 自倉庫への受注一覧（店舗名・商品名表示用に LAZY を読む） */
    @Transactional(readOnly = true)
    public List<ShopOrder> findActiveByWarehouseId(Integer warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("倉庫IDが指定されていません");
        }
        List<ShopOrder> orders = shopOrderRepository
                .findByWarehouseIdAndDeleteFlagOrderByShopOrderIdDesc(warehouseId, 0);
        orders.forEach(o -> {
            if (o.getShop() != null) {
                o.getShop().getShopName();
            }
            if (o.getGoods() != null) {
                o.getGoods().getGoodsName();
            }
        });
        return orders;
    }

    /** 受注一覧（/warehouse/stock 用）：自倉庫 + 期間/ステータスの任意絞り込み */
    @Transactional(readOnly = true)
    public List<ShopOrder> findActiveByWarehouseIdFiltered(
        Integer warehouseId,
        String status,
        LocalDate from,
        LocalDate to
    ){
    boolean hasStatus = status != null && !status.isBlank();//ステータスがnullや空白（ブランクではないブランクでない場合true)
    boolean hasFrom = from != null;//開始日がnullでない場合true
    boolean hasTo = to != null; //終了日がnullでない場合true

    List<ShopOrder> orders;//受注一覧を格納するリスト
      /** 倉庫IDが指定されていない場合はエラー */
        if(warehouseId == null){
            throw new IllegalArgumentException("倉庫IDが指定されていません");
        }
        // ===== パターンA: 期間指定なし（from/to が両方 null） =====
        //期間指定なしではステータス絞り込みのみ可能 hasFormがfalseかつhasToがfalseの場合
        if(!hasFrom && !hasTo){
            if(hasStatus){
            /** ステータスだけで絞る */
            orders = shopOrderRepository.findByWarehouseIdAndDeleteFlagAndOrderStatusOrderByShopOrderIdDesc
            (warehouseId,0, status.strip());//ステータスをstrip()で前後の空白を削除
            }else{
            //条件なし絞り込み
            orders = shopOrderRepository.findByWarehouseIdAndDeleteFlagOrderByShopOrderIdDesc(
                warehouseId,0);
        }

        // ===== パターンB: 期間指定あり（片側でも入力があれば範囲絞り込み） =====
        
    }else{
        // 欠けている側を極端な値で埋めて、片側だけでも動くようにする
        LocalDate fromDate = hasFrom ? from : LocalDate.of(1970,1,1);
        LocalDate toDate = hasTo ? to : LocalDate.of(2999,12,31);

        // DBの order_date は TIMESTAMP のため、日付→日付時刻に変換する
        LocalDateTime fromDateTime = fromDate.atStartOfDay();//00:00:00
        LocalDateTime toDateTime = toDate.plusDays(1)//翌日 00:00:00
        .atStartOfDay()
        .minusNanos(1);//その直前（終日を含める）

        //期間 + ステータス両方で絞る
        if(hasStatus){
            orders = shopOrderRepository.findByWarehouseIdAndDeleteFlagAndOrderStatusAndOrderDateBetweenOrderByShopOrderIdDesc(
                warehouseId,0,status.strip(),fromDateTime,toDateTime);
        }else{
            orders = shopOrderRepository.findByWarehouseIdAndDeleteFlagAndOrderDateBetweenOrderByShopOrderIdDesc(
                warehouseId,0,fromDateTime,toDateTime);
        }
    }
    // shop / goods は LAZY なので、画面表示前に名前を読み込む
    orders.forEach(order -> {
        if (order.getShop() != null) {
            order.getShop().getShopName();
        }
        if (order.getGoods() != null) {
            order.getGoods().getGoodsName();
        }
    });
    return orders;
    }


    /**
     * 受注ステータス更新（自倉庫の行だけ）
     * @return true=成功 / false=対象なし・権限外・不正なステータス
     */
    @Transactional
    public boolean updateOrderStatus(Integer warehouseId, Integer shopOrderId, String newStatus) {
        if (warehouseId == null || shopOrderId == null || newStatus == null || newStatus.isBlank()) {
            return false;
        }
        if (!ALLOWED_STATUSES.contains(newStatus)) {
            return false;
        }

        ShopOrder row = shopOrderRepository.findById(shopOrderId).orElse(null);
        if (row == null || !Integer.valueOf(0).equals(row.getDeleteFlag())) {
            return false;
        }
        if (!Objects.equals(warehouseId, row.getWarehouseId())) {
            return false;
        }

        row.setOrderStatus(newStatus);
        // ステータス更新時に更新日時も反映（/shop/sales の更新日時表示用）
        row.setUpdateDate(LocalDateTime.now());
        shopOrderRepository.save(row);
        return true;
    }
}
