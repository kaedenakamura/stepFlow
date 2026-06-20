package com.example.stepflow.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.stepflow.entity.ShopStock;
import java.util.List;

@Repository
public interface ShopStockRepository extends JpaRepository<ShopStock, Integer> {
    
    /** delete_flag=0 を shop_stock_id 降順で取得 */
    List<ShopStock> findByDeleteFlagOrderByShopStockIdDesc(Integer deleteFlag);

    /** goods_id に紐づく有効な在庫 */
    List<ShopStock> findByGoodsIdAndDeleteFlag(Integer goodsId, Integer deleteFlag);

    /** shop_id に紐づく有効な在庫 */
    List<ShopStock> findByShopIdAndDeleteFlag(Integer shopId, Integer deleteFlag);
}

