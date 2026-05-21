package com.example.stepflow.dto;

import com.example.stepflow.entity.Inquiry;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 管理者「問い合わせ一覧」1行分（仕様: 内容・所属・氏名・日時・ステータス・詳細）
 */
@Data
@AllArgsConstructor
public class AdminInquiryListRow {
	
	private final Inquiry inquiry;//お問合せ本体
	private final String userName;//送信者氏名
	private final String affiliationDisplay;//所属の表示用文字列
	private final String contentPreview;//一覧用に短くした本文
}
