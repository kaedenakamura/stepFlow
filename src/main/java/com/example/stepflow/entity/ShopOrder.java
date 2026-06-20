package com.example.stepflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.time.LocalDateTime;


@Entity
@Table(name = "shop_order")
@Data// 「Getter/Setterを自動で作ってください」という命令.
public class ShopOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shop_order_id")
    private Integer shopOrderId;

    @Column(name = "shop_id" , nullable = false)
    private Integer shopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id" , insertable = false, updatable = false)
    private Shop shop; // 店舗を入れる箱　DBの列名と同じなら@Columnは省略できる為name=shopIdは省略

    @Column(name = "warehouse_id" , nullable = false)
    private Integer warehouseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id" , insertable = false , updatable = false)
    private Warehouse warehouse; // 倉庫を入れる箱　DBの列名と同じなら@Columnは省略できる為name=warehouseIdは省略

    @Column(name = "goods_id" , nullable = false)
    private Integer goodsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id" , insertable = false , updatable = false)
    private Goods goods; // 商品を入れる箱　DBの列名と同じなら@Columnは省略できる為name=goodsIdは省略

    @Column(name = "order_quantity" , nullable = false)
    private Integer orderQuantity;

    @Column(name = "order_date" , insertable = false , updatable = false)
    private LocalDateTime orderDate;//発注日時

    @Column(name = "update_date")
    private LocalDateTime updateDate;//更新日時

    @Column(name = "order_status" , nullable = false)
    private String orderStatus;

    @Column(name = "delete_flag" , nullable = false)
    private Integer deleteFlag = 0; // 0:有効, 1:削除

    
}
