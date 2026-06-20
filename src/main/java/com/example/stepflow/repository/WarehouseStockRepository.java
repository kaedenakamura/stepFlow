package com.example.stepflow.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import com.example.stepflow.entity.WarehouseStock;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Integer> {

    /** delete_flag=0 を warehouse_stock_id 降順で取得 */
    List<WarehouseStock> findByDeleteFlagOrderByWarehouseStockIdDesc(Integer deleteFlag);

    /** goods_id に紐づく有効な在庫 */
    List<WarehouseStock> findByGoodsIdAndDeleteFlag(Integer goodsId, Integer deleteFlag);

    /** warehouse_id に紐づく有効な在庫 */
    List<WarehouseStock> findByWarehouseIdAndDeleteFlag(Integer warehouseId, Integer deleteFlag);

    /**
     * 指定倉庫群の、1商品あたりの在庫合計（発注商品選択画面の「連携先合計在庫」用）。
     */
    @Query("SELECT COALESCE(SUM(ws.warehouseStock), 0) FROM WarehouseStock ws "
            + "WHERE ws.deleteFlag = :deleteFlag AND ws.goodsId = :goodsId "
            + "AND ws.warehouseId IN :warehouseIds")
    Integer sumWarehouseStockByGoodsIdAndWarehouseIds(
            @Param("goodsId") Integer goodsId,
            @Param("warehouseIds") List<Integer> warehouseIds,
            @Param("deleteFlag") Integer deleteFlag);
}
