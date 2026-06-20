package com.example.stepflow.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.entity.Goods;
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.ShopStock;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.entity.WarehouseStock;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.ShopStockRepository;
import com.example.stepflow.repository.WarehouseRepository;
import com.example.stepflow.repository.WarehouseStockRepository;

@Service
public class AdminGoodsService {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ShopStockRepository shopStockRepository;

    @Autowired
    private WarehouseStockRepository warehouseStockRepository;

    /**
     * 商品新規登録 + 全店舗・全倉庫に在庫0のレコードを作成
     */
    @Transactional//トランザクション管理を行う→トランザクションとは、
    // データベースの一連の操作を一つの単位として扱う機能であり、複数の操作をまとめて実行することができる。
    public void registerGoodsWithInitialStock(Goods goods) {
        goods.setDeleteFlag(0);//削除フラグを0→生存しているものあるもの(or新規登録)
        goodsRepository.save(goods);//商品を登録 Insert語goodsIdが入る
        //ここで、商品を登録 Insert語goodsIdが入る→goodsIdが入ったら、その商品の在庫を全店舗に登録する（初期値は在庫０で登録）

        Integer goodsId = goods.getGoodsId();

        List<Shop> shops = shopRepository.findByDeleteFlagOrderByShopIdDesc(0);//店舗を取得
        for (Shop shop : shops) {//店舗を一つずつ取得
            ShopStock row = new ShopStock();//店舗在庫オブジェクトを作成
            row.setShopId(shop.getShopId());
            row.setGoodsId(goodsId);//商品IDを設定
            row.setShopStock(0);//在庫数を0に設定
            row.setDeleteFlag(0);//削除フラグを0→生存しているものあるもの(or新規登録)
            shopStockRepository.save(row);//店舗在庫を登録 Insert語shopStockIdが入る
        }

        List<Warehouse> warehouses = warehouseRepository.findByDeleteFlagOrderByWarehouseIdDesc(0);
        for (Warehouse warehouse : warehouses) {
            WarehouseStock row = new WarehouseStock();//倉庫在庫オブジェクトを作成
            row.setWarehouseId(warehouse.getWarehouseId());
            row.setGoodsId(goodsId);//商品IDを設定
            row.setWarehouseStock(0);//在庫数を0に設定
            row.setDeleteFlag(0);//削除フラグを0→生存しているものあるもの(or新規登録)
            warehouseStockRepository.save(row);//倉庫在庫を登録 Insert語warehouseStockIdが入る
        }
    }

    /**
     * 商品編集（在庫テーブルは触らない）
     */
    @Transactional//トランザクション管理を行う
    public void updateGoods(Goods goods) {
        goods.setDeleteFlag(0);//削除フラグを0→生存しているものあるもの
        goodsRepository.save(goods);//商品を登録 Insert語goodsIdが入る
    }

    /**
     * 商品論理削除 + 関連する店舗在庫・倉庫在庫も論理削除
     */
    @Transactional//トランザクション管理を行う
    public void deleteGoodsWithStocks(Integer goodsId) {
        Goods goods = goodsRepository.findById(goodsId).orElse(null);//商品を取得
        if (goods == null || !Integer.valueOf(0).equals(goods.getDeleteFlag())) {   
            return;//商品が存在しない、または削除されている場合は何もしない
        }

        goods.setDeleteFlag(1);//削除フラグを1→削除されたとみなす
        goodsRepository.save(goods);//商品を登録 Insert語goodsIdが入る

        for (ShopStock shopStock : shopStockRepository.findByGoodsIdAndDeleteFlag(goodsId, 0)) {
            shopStock.setDeleteFlag(1);
            shopStockRepository.save(shopStock);
        }

        for (WarehouseStock warehouseStock : warehouseStockRepository.findByGoodsIdAndDeleteFlag(goodsId, 0)) {
            warehouseStock.setDeleteFlag(1);
            warehouseStockRepository.save(warehouseStock);
        }
    }
}
