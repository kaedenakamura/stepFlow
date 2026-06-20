package com.example.stepflow.service; // 問い合わせの管理を行うサービスクラス

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Spring標準の @Transactional

import com.example.stepflow.UserRepository.UserRepository;
import com.example.stepflow.dto.AdminInquiryListRow;
import com.example.stepflow.entity.Inquiry;
import com.example.stepflow.entity.User;
import com.example.stepflow.repository.InquiryRepository;


@Service
public class AdminInquiryService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private InquiryRepository inquiryRepository;

	private static final int PREVIEW_MAX = 30;

	/**
	 * DBから受け取ったInquiryのリストを、画面用のAdminInquiryListRowのリストに変換する
	 */
	public List<AdminInquiryListRow> toListRows(List<Inquiry> inquiries) {
		if (inquiries.isEmpty()) {
			return List.of(); // 空のリストを返す
		}

		Set<Integer> userIds = inquiries.stream()
				.map(Inquiry::getUserId) // 各Inquiryからuser_idを取る
				.filter(Objects::nonNull) // nullでないものだけ
				.collect(Collectors.toSet());

		Map<Integer, User> userMap = new HashMap<>();

		//user_idでユーザーを取得して、userMapに格納
		userRepository.findAllById(userIds).forEach(u -> userMap.put(u.getUserId(), u));

		List<AdminInquiryListRow> rows = new ArrayList<>();
		for (Inquiry i : inquiries) {
			User u = userMap.get(i.getUserId());
			String name = u != null ? u.getUserName() : "(不明)";
			// [修正] buildAffiliation(U) → buildAffiliation(u)（変数名のタイポ）
			String affiliation = buildAffiliation(u);
			String authorityLabel = buildAuthorityLabel(u);
			String preview = abbreviate(i.getInquiryDetail(), PREVIEW_MAX);
			rows.add(new AdminInquiryListRow(i, name, authorityLabel, affiliation, preview));
		}

		return rows;
	}

	/** 一覧用：本文を先頭だけ表示 */
	private static String abbreviate(String text, int max) {
		if (text == null) {
			return "";
		}
		String t = text.strip();
		return t.length() <= max ? t : t.substring(0, max) + "…";
	}

	/** 送信者の権限ラベル */
	private static String buildAuthorityLabel(User u) {
		if (u == null) {
			return "-";
		}
		Integer aid = u.getAuthorityId();
		if (Integer.valueOf(1).equals(aid)) {
			return "管理者";
		}
		if (Integer.valueOf(2).equals(aid)) {
			return "店舗";
		}
		if (Integer.valueOf(3).equals(aid)) {
			return "倉庫";
		}
		return "-";
	}

	/** userの権限に応じて所属表示文字列を作る */
	private static String buildAffiliation(User u) {
		if (u == null) {
			return "-";
		}
		Integer aid = u.getAuthorityId();
		if (Integer.valueOf(1).equals(aid)) {
			return "管理者（所属なし）";
		}
		if (Integer.valueOf(2).equals(aid)) {
			// [修正] ER図に合わせ shop_id も表示（あれば）
			return "店舗ID: " + (u.getShopId() != null ? u.getShopId() : "-");
		}
		if (Integer.valueOf(3).equals(aid)) {
			return "倉庫ID: " + (u.getWarehouseId() != null ? u.getWarehouseId() : "-");
		}
		return "-";
	}

	/** 詳細画面用（Controller から呼ぶ） */
	/** 送信者の所属を画面に渡す */
	public String formatAffiliation(User u) {
		return buildAffiliation(u);
	}

	/** 詳細画面用（Controller から呼ぶ） *
	 /
	/** 問い合わせを取得 */
	@Transactional(readOnly = true)
	public Inquiry findActiveForAdmin(Integer inquiryId) {
		return inquiryRepository.findById(inquiryId)
				.filter(i -> Integer.valueOf(0).equals(i.getDeleteFlag()))
				.orElse(null);
	}

	/** ステータス更新 */
	@Transactional
	public boolean updateStatus(Integer inquiryId, String newStatus) {
		Inquiry inv = findActiveForAdmin(inquiryId);
		if (inv == null) {
			return false;
		}
		/** ステータスが許可されているか */
		if (!isAllowedStatus(newStatus)) {
			throw new IllegalArgumentException("不正なステータス: " + newStatus);
		}
		inv.setInquiryStatus(newStatus);
		inquiryRepository.save(inv);
		return true;
	}

	/** 旧メソッド名の互換用（もし他で呼んでいたら） */
	public boolean updateInquiryStatus(Integer inquiryId, String status) {
		return updateStatus(inquiryId, status);
	}

	/** 論理削除 *
	 */
	@Transactional
	public boolean logicalDelete(Integer inquiryId) {
		Inquiry inv = inquiryRepository.findById(inquiryId).orElse(null);
		if (inv == null || Integer.valueOf(1).equals(inv.getDeleteFlag())) {
			return false;
		}
		inv.setDeleteFlag(1); // DB: delete_flag 論理削除
		inquiryRepository.save(inv);
		return true;
	}

	/** 仕様で許可されているステータスか */
	public static boolean isAllowedStatus(String s) {
		return "未対応".equals(s) || "対応中".equals(s) || "対応済".equals(s);
	}
}
