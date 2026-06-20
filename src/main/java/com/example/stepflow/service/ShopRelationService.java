package com.example.stepflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.stepflow.repository.RelationRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.example.stepflow.entity.Relation;


@Service
public class ShopRelationService {
    @Autowired
    private RelationRepository relationRepository;

        /** 自店舗の有効な連携倉庫一覧（倉庫名表示用に JOIN FETCH 済み） */
    @Transactional(readOnly = true)
    public List<Relation> findActiveByShopId(Integer shopId){
        if(shopId == null){
            throw new IllegalArgumentException("店舗IDが指定されていません");
        }
        List<Relation> relations = relationRepository.findByDeleteFlagAndShopIdWithShopAndWarehouse(Integer.valueOf(0), shopId);
          // 念のため倉庫名を読む（LAZY 対策・一覧と同パターン）

          //倉庫名を読む（LAZY 対策・一覧と同パターン）
          relations.forEach(r -> {
            if(r.getWarehouse() != null){
                r.getWarehouse().getWarehouseName();
                r.getWarehouse().getWarehouseAddress();
            }
          });
          return relations;
    }

}
