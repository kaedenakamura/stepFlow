package com.example.stepflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

@Entity//「このクラスはデータベースのテーブルと対応します」という印
@Table(name = "relation")//「MySQLの中の "relation" というテーブルを使います」という指定
@Data//「Getter/Setterを自動で作ってください」という命令.
public class Relation {
    @Id//「主キーを決める」という印
    @GeneratedValue(strategy = GenerationType.IDENTITY)//「番号は自動でプラス1」という指定
    @Column(name = "relation_id")//「MySQL側では relation_id という列名ですよ」という指定
    private Integer relationId;//関係ID

    @NotNull(message = "店舗を選択してください")//「店舗を選択してください」というエラーメッセージを表示
    @Column(name = "shop_id")
    private Integer shopId;//店舗ID

    @ManyToOne(fetch = FetchType.LAZY)//「多対一の関係」という印 遅延(LAZY)ロード Fetch→取得する　
    // FatchType.LAZY→遅延ロード(relation.getshop()で取得できる)(最初はshopを取得しないと宣言（FetchType.LAZY）)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)//「shop_id」という列名でJoinする
    private Shop shop;//店舗オブジェクト(Shopクラスのインスタンス)→th:text="${relation.shop.shopName}"で表示

    @NotNull(message = "倉庫を選択してください")//「倉庫を選択してください」というエラーメッセージを表示
    @Column(name = "warehouse_id")
    private Integer warehouseId; //倉庫ID

    @ManyToOne(fetch = FetchType.LAZY)//「多対一の関係」という印 遅延(LAZY)ロード Fetch→取得する　
    // FatchType.LAZY→遅延ロード(relation.getshop()で取得できる)(最初はshopを取得しないと宣言（FetchType.LAZY）)
    @JoinColumn(name = "warehouse_id", insertable = false, updatable = false)//「warehouse_id」という列名でJoinする
    private Warehouse warehouse;//倉庫オブジェクト(Warehouseクラスのインスタンス)→th:text="${relation.warehouse.warehouseName}"で表示

    @Column(name = "delete_flag", nullable = false)//「MySQL側では delete_flag という列名ですよ」という指定
    private Integer deleteFlag = 0; //論理削除フラグ　0=有効。新規は常に0 1=削除済




}
