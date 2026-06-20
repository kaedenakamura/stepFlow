package com.example.stepflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.stepflow.entity.Goods;
import java.util.List;
import org.springframework.data.jpa.repository.Query;//JPQLを使用するためのインポート
//Queryとは、JPQL（Java Persistence Query Language）を使用するためのインターフェースであり、
//JPQLは、SQLのようなクエリ言語であり、データベースのテーブルを操作するためのクエリを記述するための言語です。
@Repository
public interface GoodsRepository extends JpaRepository<Goods, Integer>{
    @Query("SELECT g FROM Goods as g LEFT JOIN FETCH g.category WHERE g.deleteFlag = :deleteFlag ORDER BY g.goodsId DESC")
    List<Goods> findByDeleteFlagWithCategory(Integer deleteFlag);//delete_flag=0 を goods_id 降順で取得
    //LEFT JOIN FETCH g.category→カテゴリーを取得
    //WHERE g.deleteFlag = :deleteFlag→delete_flag=0 を goods_id 降順で取得
    //ORDER BY g.goodsId DESC→goods_id 降順で取得
    //findByDeleteFlagWithCategory(Integer deleteFlag)→delete_flag=0 を goods_id 降順で取得
    //Goodsクラスのインスタンスを取得
    //Categoryクラスのインスタンスを取得
    //goods_id 降順で取得
    //delete_flag=0 を goods_id 降順で取得
    //Goodsクラスのインスタンスを取得
    //Categoryクラスのインスタンスを取得
    //goods_id 降順で取得
    //削除フラグで商品を検索
    List<Goods> findByDeleteFlagOrderByGoodsIdDesc(Integer deleteFlag);//delete_flag=0 を goods_id 降順で取得
    //findByDeleteFlagOrderByGoodsIdDesc(Integer deleteFlag)→delete_flag=0 を goods_id 降順で取得
    //Goodsクラスのインスタンスを取得
    //goods_id 降順で取得
    //delete_flag=0 を goods_id 降順で取得
    //Goodsクラスのインスタンスを取得
    //goods_id 降順で取得
    //delete_flag=0 を goods_id 降順で取得
    //Goodsクラスのインスタンスを取得

    /** カテゴリーIDで商品を検索 */
    @Query("SELECT g FROM Goods as g LEFT JOIN FETCH g.category WHERE g.deleteFlag = :deleteFlag AND g.categoryId = :categoryId ORDER BY g.goodsId DESC")
    List<Goods> findByDeleteFlagAndCategoryIdWithCategory(Integer deleteFlag, Integer categoryId);
    //findByDeleteFlagAndCategoryIdWithCategory(Integer deleteFlag, Integer categoryId)→delete_flag=0 を goods_id 降順で取得
    //Goodsクラスのインスタンスを取得
    //Categoryクラスのインスタンスを取得
    //goods_id 降順で取得
    //delete_flag=0 を goods_id 降順で取得
    //Goodsクラスのインスタンスを取得
    //Categoryクラスのインスタンスを取得
    //goods_id 降順で取得
    //delete_flag=0 を goods_id 降順で取得
    //Goodsクラスのインスタンスを取得
    //Categoryクラスのインスタンスを取得
    //goods_id 降順で取得
    //delete_flag=0 を goods_id 降順で取得
    //Goodsクラスのインスタンスを取得
    //Categoryクラスのインスタンスを取得
    //goods_id 降順で取得
    //delete_flag=0 を goods_id 降順で取得
    //Goodsクラスのインスタンスを取得
    //Categoryクラスのインスタンスを取得
    //goods_id 降順で取得
    /** 有効な商品に同じ名前があるか（新規登録用） */
    boolean existsByGoodsNameAndDeleteFlag(String goodsName, Integer deleteFlag);

    /** 有効な商品に同じ名前があるか（自分以外・編集用） */
    boolean existsByGoodsNameAndDeleteFlagAndGoodsIdNot(String goodsName, Integer deleteFlag, Integer goodsId);
}
