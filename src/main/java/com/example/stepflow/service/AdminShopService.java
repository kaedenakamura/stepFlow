package com.example.stepflow.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.entity.Goods;
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.ShopStock;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.ShopStockRepository;

@Service
public class AdminShopService {

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private ShopStockRepository shopStockRepository;

    /**
     * 店舗新規登録 + 有効な全商品に shop_stock（在庫0）を作成
     */
    @Transactional
    public void registerShopWithInitialStock(Shop shop) {
        shop.setDeleteFlag(0);
        shopRepository.save(shop);

        Integer shopId = shop.getShopId();

        List<Goods> goodsList = goodsRepository.findByDeleteFlagOrderByGoodsIdDesc(0);
        for (Goods goods : goodsList) {
            ShopStock row = new ShopStock();
            row.setShopId(shopId);
            row.setGoodsId(goods.getGoodsId());
            row.setShopStock(0);
            row.setDeleteFlag(0);
            shopStockRepository.save(row);
        }
    }

    /**
     * 店舗編集（在庫テーブルは触らない）
     */
    @Transactional
    public void updateShop(Shop shop) {
        shop.setDeleteFlag(0);
        shopRepository.save(shop);
    }

    /**
     * 店舗論理削除 + 関連する店舗在庫も論理削除
     */
    @Transactional
    public void deleteShopWithStocks(Integer shopId) {
        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop == null || !Integer.valueOf(0).equals(shop.getDeleteFlag())) {
            return;
        }

        shop.setDeleteFlag(1);
        shopRepository.save(shop);

        for (ShopStock shopStock : shopStockRepository.findByShopIdAndDeleteFlag(shopId, 0)) {
            shopStock.setDeleteFlag(1);
            shopStockRepository.save(shopStock);
        }
    }
}
