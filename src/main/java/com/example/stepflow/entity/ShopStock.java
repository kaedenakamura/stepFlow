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
@Table(name = "shop_stock")//「MySQLの中の "shop_stock" というテーブルを使います」という指定
@Data//「Getter/Setterを自動で作ってください」という命令.
public class ShopStock {
    @Id//「主キーを決める」という印
    @GeneratedValue(strategy = GenerationType.IDENTITY)//「番号は自動でプラス1」という指定
    @Column(name = "shop_stock_id")//「MySQL側では shop_stock_id という列名ですよ」という指定
    private Integer shopStockId;//店舗在庫ID

    @NotNull(message = "店舗を選択してください")//「店舗を選択してください」というエラーメッセージを表示
    @Column(name = "shop_id")
    private Integer shopId;//店舗ID

    @ManyToOne(fetch = FetchType.LAZY)//「多対一の関係」という印 遅延(LAZY)ロード Fetch→取得する　
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)//「shop_id」という列名でJoinする
    private Shop shop;//店舗オブジェクト(Shopクラスのインスタンス)→th:text="${relation.shop.shopName}"で表示    
    
    @NotNull(message = "商品を選択してください")//「商品を選択してください」というエラーメッセージを表示
    @Column(name = "goods_id")
    private Integer goodsId;//商品ID

    @ManyToOne(fetch = FetchType.LAZY)//「多対一の関係」という印 遅延(LAZY)ロード Fetch→取得する　
    @JoinColumn(name = "goods_id", insertable = false, updatable = false)//「goods_id」という列名でJoinする
    private Goods goods;//商品オブジェクト(Goodsクラスのインスタンス)→th:text="${relation.goods.goodsName}"で表示


    @NotNull(message = "在庫数を入力してください")//「在庫数を入力してください」というエラーメッセージを表示
    @Column(name = "stock_quantity")
    private Integer shopStock = 0;//在庫数 0=在庫なし 1=在庫あり

    @Column(name = "delete_flag", nullable = false)//「MySQL側では delete_flag という列名ですよ」という指定
    private Integer deleteFlag = 0; //論理削除フラグ　0=有効。新規は常に0 1=削除済




}