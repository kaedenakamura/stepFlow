package com.example.stepflow.repository; // DB 検索・保存専用の置き場

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.stepflow.entity.Shop;
import java.util.List;

/**
 * shop テーブルへのアクセス窓口。
 * メソッド名だけ書けば、Spring Data JPA が裏で SQL を組み立ててくれる。
 */
@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer>{
	/** delete_flag=0 を shop_id 降順で取得（管理者一覧） */
	List<Shop> findByDeleteFlagOrderByShopIdDesc(Integer deleteFlag);
}

