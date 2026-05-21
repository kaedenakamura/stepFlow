package com.example.stepflow.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class InquiryForm {
	// お問い合わせフォームのデータを保持するクラス
	@NotBlank(message = "送信先を選択してください")
	private String receiverRole;// 送信先の役割（ROLE_ADMIN / ROLE_SHOP / ROLE_WAREHOUSE）
	
	private Integer partnerId; //店舗・倉庫を選ぶ時の連絡先ID
	
	@NotBlank(message = "お問合せ内容を入力してください")
	@Size(max = 255 , message ="お問い合わせ内容は255文字以内で入力してください")
	private String content; // お問い合わせ内容DBの inquiry_detail に対応）
	
}


