package com.example.stepflow.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.stepflow.entity.Warehouse;
import java.util.List;


@Repository // これで、Springがこのクラスを「リポジトリ」として認識します。
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer>{
    //delete_flag=0 を warehouse_id 降順で取得
    List<Warehouse> findByDeleteFlagOrderByWarehouseIdDesc(Integer deleteFlag);

    /** 有効・住所部分一致で絞り込み（管理者一覧） */
    List<Warehouse> findByDeleteFlagAndWarehouseAddressContainingOrderByWarehouseIdDesc(
            Integer deleteFlag, String warehouseAddress);

    //delete_flag=0 を warehouse_id で取得
    Warehouse findByWarehouseIdAndDeleteFlag(Integer warehouseId, Integer deleteFlag);


}

