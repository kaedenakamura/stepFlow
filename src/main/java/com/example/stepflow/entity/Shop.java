package com.example.stepflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import lombok.Data;


@Entity// 「このクラスはデータベースのテーブルと対応します」という印
@Table(name="shop") // 対応テーブル名
@Data // 「Getter/Setterを自動で作ってください」という命令.

public class Shop {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)//AUTO_INCREMENTと同じ構造
   @Column(name="shop_id")
   private Integer shopId;
   @Column(name="shop_name" , nullable = false , length = 255)
   private String shopName;
   @Column(name="delete_flag", nullable = false)
   private Integer deleteFlag = 0;


}
