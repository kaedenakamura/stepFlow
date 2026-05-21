package com.example.stepflow.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table; // 対応するテーブル名を指定する印

import lombok.Data;

@Entity
@Table(name = "inquiry")
@Data// 「Getter/Setterを自動で作ってください」という命令.
public class Inquiry {
// ここでは、問い合わせ内容を保存するためのエンティティクラスを定義します。
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "inquiry_id")
	private Integer inquiryId; // 問い合わせIDを入れる箱　DBの列
	
	@Column(name = "user_id", nullable = false)
	private Integer userId;// 問い合わせをしたユーザーのIDを入れる箱　DBの列
	
	@Column(name = "inquiry_category_id", nullable = false)
	private Integer inquiryCategoryId;// 問い合わせのカテゴリIDを入れる箱　DBの列
	
	@Column(name= "inquiry_detail", nullable = false , length = 255)
	private String inquiryDetail;// 問い合わせ内容を入れる箱　DBの列
	
	@Column(name = "inquiry_date", insertable = false, updatable = false)
	private LocalDateTime inquiryDate; // DBの TIMESTAMP（DEFAULT CURRENT_TIMESTAMP）。INSERTはDB任せ
	
	@Column(name = "inquiry_status", length = 255) // 対応状況（未対応など）
    private String inquiryStatus = "未対応"; // DBの DEFAULT と同じ初期値
	
    @Column(name = "authority_id", nullable = false) // 送信先ロール（管理者=1, 店舗=2, 倉庫=3）
    private Integer authorityId; // receiverRole から Controller で変換してセット
    
    @Column(name = "shop_id") // 店舗宛のときだけ値が入る
    private Integer shopId; // ROLE_SHOP 選択時に partnerId を入れる
    
    @Column(name = "warehouse_id") // 倉庫宛のときだけ値が入る
    private Integer warehouseId; // ROLE_WAREHOUSE 選択時に partnerId を入れる
    
    @Column(name = "delete_flag", nullable = false) // 論理削除フラグ
    private Integer deleteFlag = 0; // 0=有効。新規は常に0
}
