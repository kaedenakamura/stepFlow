package com.example.stepflow.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.example.stepflow.entity.Relation;
import org.springframework.data.jpa.repository.Query;//JPQLを使用するためのインポート


@Repository
public interface RelationRepository extends JpaRepository<Relation, Integer>{
    @Query("SELECT r FROM Relation r LEFT JOIN FETCH r.shop LEFT JOIN FETCH r.warehouse "
            + "WHERE r.deleteFlag = :deleteFlag ORDER BY r.relationId DESC")
    List<Relation> findByDeleteFlagWithShopAndWarehouse(Integer deleteFlag);

    @Query("SELECT r FROM Relation r LEFT JOIN FETCH r.shop LEFT JOIN FETCH r.warehouse "
            + "WHERE r.deleteFlag = :deleteFlag AND r.shopId = :shopId "
            + "ORDER BY r.relationId DESC")
    List<Relation> findByDeleteFlagAndShopIdWithShopAndWarehouse(Integer deleteFlag, Integer shopId);
    //LEFT JOIN FETCH r.shop LEFT JOIN FETCH r.warehouse→店舗と倉庫を取得
    //WHERE r.deleteFlag = :deleteFlag→delete_flag=0 を relation_id 降順で取得
    //ORDER BY r.relationId DESC→relation_id 降順で取得
    //findByDeleteFlagWithShopAndWarehouse(Integer deleteFlag)→delete_flag=0 を relation_id 降順で取得
    //Relationクラスのインスタンスを取得
    //shopクラスのインスタンスを取得
    //warehouseクラスのインスタンスを取得
    //relation_id 降順で取得
    //delete_flag=0 を relation_id 降順で取得
    //Relationクラスのインスタンスを取得
    //shopクラスのインスタンスを取得
    //warehouseクラスのインスタンスを取得
    //relation_id 降順で取得
           


    //delete_flag=0 を relation_id 降順で取得
    List<Relation> findByDeleteFlagOrderByRelationIdDesc(Integer deleteFlag);

}
