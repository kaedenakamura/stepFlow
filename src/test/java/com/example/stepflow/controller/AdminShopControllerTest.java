// 管理者の店舗画面（/admin/shops）をテストするクラス
package com.example.stepflow.controller;

// static import：GET リクエスト送信用
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// static import：Model に値が入っているか検証する用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
// static import：HTTP ステータス検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// static import：返却画面名（Thymeleaf）検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// JUnit：各テストの直前に毎回実行
import org.junit.jupiter.api.BeforeEach;
// JUnit：日本語のテスト名
import org.junit.jupiter.api.DisplayName;
// JUnit：テストメソッド印
import org.junit.jupiter.api.Test;
// Spring：Bean 自動注入
import org.springframework.beans.factory.annotation.Autowired;
// Spring Test：MockMvc 有効化
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// Spring Test：アプリ全体起動
import org.springframework.boot.test.context.SpringBootTest;
// Spring Security Test：ログイン済みのふり
import org.springframework.security.test.context.support.WithMockUser;
// Spring Test：テスト用 DB
import org.springframework.test.context.ActiveProfiles;
// Spring Test：擬似 HTTP クライアント
import org.springframework.test.web.servlet.MockMvc;
// Spring：テスト後ロールバック
import org.springframework.transaction.annotation.Transactional;

// 店舗を DB に保存する Repository
import com.example.stepflow.entity.Shop;
import com.example.stepflow.repository.ShopRepository;

// ─────────────────────────────────────────
// ログインテスト（LoginSecurityTest）の「次」に書く画面テストのお手本
// コピーするときはクラス名・URL・画面名・Repository だけ差し替える
// ─────────────────────────────────────────
@SpringBootTest // Security + DB 込みで本番に近い状態でテスト
@AutoConfigureMockMvc // MockMvc を使えるようにする
@ActiveProfiles("test") // stepflow_test_db を使う
@Transactional // テストごとに DB を巻き戻す
class AdminShopControllerTest {

	@Autowired // 擬似ブラウザ
	private MockMvc mockMvc;

	@Autowired // テスト用の店舗データを insert するため
	private ShopRepository shopRepository;

	// ─────────────────────────────────────────
	// 前提条件：仕様書の「テストデータ」欄に相当
	// ─────────────────────────────────────────
	@BeforeEach // 各 @Test の直前に実行
	void setUp() {
		shopRepository.deleteAll(); // 店舗を一旦空にする

		// 店舗A：住所に「千代田」を含む（絞り込みテスト用）
		Shop shopA = new Shop();
		shopA.setShopName("テスト店舗A");
		shopA.setShopAddress("東京都千代田区1-1");
		shopA.setDeleteFlag(0); // 有効データ
		shopRepository.save(shopA); // INSERT

		// 店舗B：住所が異なる（絞り込みで除外される想定）
		Shop shopB = new Shop();
		shopB.setShopName("テスト店舗B");
		shopB.setShopAddress("大阪府大阪市1-1");
		shopB.setDeleteFlag(0);
		shopRepository.save(shopB);
	}

	// ─────────────────────────────────────────
	// TC：店舗一覧画面
	// ─────────────────────────────────────────
	@Test // テストメソッド
	@WithMockUser(roles = "ADMIN") // 管理者としてアクセス（未指定だと 403 / login へ）
	@DisplayName("管理者は店舗一覧画面を表示できる")
	void adminCanViewShopList() throws Exception {

		mockMvc.perform(get("/admin/shops")) // ① GET で一覧 URL にアクセス
				.andExpect(status().isOk()) // ② 200 OK
				.andExpect(view().name("admin/shop-list")) // ③ テンプレート名が Controller の return と一致
				.andExpect(model().attributeExists("shops")); // ④ 画面に渡す shops が Model にある
	}

	// ─────────────────────────────────────────
	// TC：住所絞り込み（仕様書に「フィルタ」項目があればこれ）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("管理者は住所で店舗を絞り込める")
	void adminCanFilterShopsByAddress() throws Exception {

		mockMvc.perform(get("/admin/shops").param("address", "千代田")) // ① クエリ ?address=千代田
				.andExpect(status().isOk()) // ② 画面は開ける
				.andExpect(view().name("admin/shop-list")) // ③ 同じ一覧画面
				.andExpect(model().attributeExists("shops")); // ④ 絞り込み結果が Model に入る
		// ※ 件数まで厳密に見る場合は Hamcrest 等で別途 assert（初めは画面が開ければ OK）
	}

	// ─────────────────────────────────────────
	// TC：新規登録画面
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("管理者は店舗新規登録画面を表示できる")
	void adminCanViewNewShopForm() throws Exception {

		mockMvc.perform(get("/admin/shops/new")) // ① 新規登録 URL
				.andExpect(status().isOk()) // ② 200 OK
				.andExpect(view().name("admin/shop-form")) // ③ フォーム画面
				.andExpect(model().attributeExists("shop")); // ④ 空の Shop がフォーム用に渡る
	}

	// ─────────────────────────────────────────
	// TC：権限エラー（ログインテストと同型）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(roles = "SHOP") // 店舗ユーザー（管理者ではない）
	@DisplayName("店舗ユーザーは店舗一覧画面にアクセスできない")
	void shopUserCannotViewShopList() throws Exception {

		mockMvc.perform(get("/admin/shops")) // ① 管理者 URL にアクセス
				.andExpect(status().isForbidden()); // ② 403 = 権限なし
	}
}
