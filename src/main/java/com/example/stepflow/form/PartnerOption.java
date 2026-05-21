package com.example.stepflow.form;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data // Lombokの@Dataアノテーションは、クラスのフィールドに対して自動的にゲッター、セッター、toString、equals、hashCodeなどのメソッドを生成します。
@AllArgsConstructor // Lombokの@AllArgsConstructorアノテーションは、クラスの全てのフィールドを引数に取るコンストラクタを自動的に生成します。
public class PartnerOption { // 店舗・倉庫の選択肢を表すクラス
	private Integer id ; // 店舗・倉庫のID（shop_id または warehouse_id）
	private String name; // 店舗・倉庫の名前
}


