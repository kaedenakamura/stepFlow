package com.example.stepflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.stepflow.entity.Category;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer>{
    
    //削除フラグでカテゴリーを検索

    List<Category> findByDeleteFlagOrderByCategoryIdDesc(Integer deleteFlag);

}
