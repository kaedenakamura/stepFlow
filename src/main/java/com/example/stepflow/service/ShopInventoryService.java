package com.example.stepflow.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.entity.ShopStock;
import com.example.stepflow.repository.ShopStockRepository;
import com.example.stepflow.entity.Category;
import com.example.stepflow.repository.CategoryRepository;

/**
 * 店舗スタッフ向け：自店舗の在庫一覧・数量更新。
 */
@Service
public class ShopInventoryService {

    @Autowired
    private ShopStockRepository shopStockRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    /** カテゴリ絞り込み用プルダウン（有効なカテゴリのみ） */
    @Transactional(readOnly = true)
    public List<Category> findActiveCategories() {
        return categoryRepository.findByDeleteFlagOrderByCategoryIdDesc(0);
    }

       /**
     * 自店舗の有効在庫一覧（delete_flag=0）
     * 画面で商品名を出すときは goods を読む（LAZY のため @Transactional 内で）
     */
    @Transactional(readOnly = true)
    public List<ShopStock> findActiveByShopId(Integer shopId){
        return findActiveByShopIdFiltered(shopId, null);
    }

    /** 自店舗の在庫一覧（カテゴリ任意絞り込み） */
    @Transactional(readOnly = true)
    public List<ShopStock> findActiveByShopIdFiltered(Integer shopId, Integer categoryId) {
        if (shopId == null) {
            throw new IllegalArgumentException("所属店舗が設定されていません。");
        }
        List<ShopStock> stocks = shopStockRepository.findByShopIdAndDeleteFlag(shopId, 0);
        stocks.forEach(s -> {
            if (s.getGoods() != null) {
                s.getGoods().getGoodsName();
                if (s.getGoods().getCategory() != null) {
                    s.getGoods().getCategory().getCategoryName();
                }
            }
        });
        if (categoryId == null) {
            return stocks;
        }
        return stocks.stream()
                .filter(s -> s.getGoods() != null && categoryId.equals(s.getGoods().getCategoryId()))
                .toList();
    }

    /**
     * 在庫数量のみ更新（他店舗の行は更新しない）
     * @return 成功 true / 失敗 false
     */
    @Transactional
    public boolean updateStockQuantity(Integer shopId, Integer shopStockId, Integer quantity){
        //パラメータチェック
        if(shopId == null || shopStockId == null || quantity == null || quantity < 0
         ){
            return false;
         }
         //対象行を取得
         ShopStock row = shopStockRepository.findById(shopStockId).orElse(null);
         if(row == null || !Integer.valueOf(0).equals(row.getDeleteFlag())){
            return false;
         }
         //所属店舗が一致しない場合は更新不可
         if(!shopId.equals(row.getShopId())){
            return false;//更新不可
         }
         //在庫数量を更新
         row.setShopStock(quantity);
         //更新した行を保存
         shopStockRepository.save(row);
         return true;
    }

    /** 編集画面用：自店舗の1行だけ取得（他店舗・削除済みは null） */
    @Transactional(readOnly = true)
    public ShopStock findEditableStock(Integer shopId, Integer shopStockId){
        //パラメータチェック
        if(shopId == null || shopStockId == null){
            return null;
        }
        //対象行を取得 goods名など
        ShopStock row = shopStockRepository.findById(shopStockId).orElse(null);
        if(row == null || !Integer.valueOf(0).equals(row.getDeleteFlag())){
            return null;
        }
        //所属店舗が一致しない場合は null を返す
        if(!shopId.equals(row.getShopId())){
            return null;
        }
        //商品名を出すときはgoodsを読む(LAZYのため@Transactional内で)。
        // ダミー読み込みのため、実際には読まれないが、バリデーションエラー回避のために入れておく
        if(row.getGoods() != null){
            row.getGoods().getGoodsName();
        }
        return row;
    }
}
