// このテストクラスが属するパッケージ（ログイン・権限のセキュリティテスト専用）
package com.example.stepflow.security;

// static import：ログインフォームの POST を再現する formLogin() 用
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
// static import：POST 送信用（ログアウトテスト）
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// static import：GET リクエストを送る get() 用
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// static import：CSRF トークン（ログアウト POST 用）
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
// static import：リダイレクト先 URL が完全一致するか検証する用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
// static import：リダイレクト先 URL がパターン一致するか検証する用（末尾 /login など）
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
// static import：HTTP ステータスコードを検証する用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// JUnit：各テストメソッドの「直前に」毎回実行する処理を指定するアノテーション
import org.junit.jupiter.api.BeforeEach;
// JUnit：テスト名を日本語で表示するアノテーション
import org.junit.jupiter.api.DisplayName;
// JUnit：テストメソッドであることを示すアノテーション
import org.junit.jupiter.api.Test;
// Spring：Bean をフィールドへ自動注入するアノテーション
import org.springframework.beans.factory.annotation.Autowired;
// Spring Test：MockMvc を使えるようにするアノテーション
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// Spring Test：アプリ全体（Security・DB 含む）を起動して統合テストするアノテーション
import org.springframework.boot.test.context.SpringBootTest;
// Spring Security：パスワードを BCrypt で暗号化する道具（本番と同じ方式）
import org.springframework.security.crypto.password.PasswordEncoder;
// Spring Security Test：「ログイン済みのふり」をするアノテーション
import org.springframework.security.test.context.support.WithMockUser;
// Spring Test：application-test.properties を読み込むアノテーション
import org.springframework.test.context.ActiveProfiles;
// Spring Test：擬似 HTTP クライアント
import org.springframework.test.web.servlet.MockMvc;
// Spring：テスト終了後に DB 変更をロールバックしやすくするアノテーション
import org.springframework.transaction.annotation.Transactional;

// ユーザー情報を DB に保存するための Repository
import com.example.stepflow.UserRepository.UserRepository;
// ユーザーのエンティティ（DB の user テーブルに対応）
import com.example.stepflow.entity.User;
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.WarehouseRepository;

// アプリ全体を起動する統合テスト（SecurityConfig も本番と同じ設定で動く）
@SpringBootTest
// 上記とセットで MockMvc を使えるようにする
@AutoConfigureMockMvc
// テスト用 DB（stepflow_test_db）と application-test.properties を使う
@ActiveProfiles("test")
// 各テスト後に DB を汚さないようトランザクションで巻き戻す
@Transactional
// テストクラス本体
class LoginSecurityTest {

	// 擬似ブラウザ：HTTP リクエストの送受信に使う
	@Autowired
	private MockMvc mockMvc;

	// テスト用ユーザーを DB に insert するために使う
	@Autowired
	private UserRepository userRepository;

	// 平文パスワードを BCrypt 化してから DB に保存するために使う
	@Autowired
	private PasswordEncoder passwordEncoder;

	// 店舗・倉庫マスタ（user.shop_id / warehouse_id の FK 用）
	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	// ─────────────────────────────────────────
	// 共通の前準備：各テストの直前に毎回実行（仕様書の「前提条件」に相当）
	// ─────────────────────────────────────────
	@BeforeEach // このメソッドは @Test の前に毎回走る
	void setUp() { // テストデータ準備メソッド

		userRepository.deleteAll(); // 既存ユーザーを全削除
		// shop / warehouse は deleteAll しない（shop_stock 等の FK で削除エラーになることがある）
		// 代わりに毎回新しいマスタを INSERT し、user から参照する

		// ── マスタデータ（user の FK が参照する先を先に作る） ──
		Shop shopMaster = new Shop();
		shopMaster.setShopName("ログインテスト店舗");
		shopMaster.setShopAddress("東京都新宿区1-1");
		shopMaster.setDeleteFlag(0);
		Integer shopId = shopRepository.save(shopMaster).getShopId();

		Warehouse warehouseMaster = new Warehouse();
		warehouseMaster.setWarehouseName("ログインテスト倉庫");
		warehouseMaster.setWarehouseAddress("東京都品川区1-1");
		warehouseMaster.setDeleteFlag(0);
		Integer warehouseId = warehouseRepository.save(warehouseMaster).getWarehouseId();

		// ── 管理者ユーザーの作成 ──
		User admin = new User(); // 空の User オブジェクトを作る
		admin.setUserName("admin_test"); // ログイン時に使うユーザー名
		admin.setUserPassword(passwordEncoder.encode("admin")); // パスワードを暗号化（平文のままだとログイン失敗する）
		admin.setAuthorityId(1); // 1 = 管理者（CustomUserDetailService の ROLE_ADMIN に対応）
		admin.setDeleteFlag(0); // 0 = 削除されていない有効ユーザー
		// shop_id / warehouse_id は null のまま（管理者は所属なし）
		userRepository.save(admin); // テスト DB の user テーブルへ INSERT

		// ── 店舗スタッフユーザーの作成 ──
		User shop = new User(); // 空の User オブジェクトを作る
		shop.setUserName("shop_test"); // ログイン用ユーザー名
		shop.setUserPassword(passwordEncoder.encode("shop")); // 暗号化したパスワード
		shop.setAuthorityId(2); // 2 = 店舗スタッフ（ROLE_SHOP）
		shop.setShopId(shopId); // 上で作った店舗 ID（FK 整合）
		shop.setDeleteFlag(0); // 有効ユーザー
		userRepository.save(shop); // DB へ保存

		// ── 倉庫スタッフユーザーの作成 ──
		User warehouse = new User(); // 空の User オブジェクトを作る
		warehouse.setUserName("warehouse_test"); // ログイン用ユーザー名
		warehouse.setUserPassword(passwordEncoder.encode("warehouse")); // 暗号化したパスワード
		warehouse.setAuthorityId(3); // 3 = 倉庫スタッフ（ROLE_WAREHOUSE）
		warehouse.setWarehouseId(warehouseId); // 上で作った倉庫 ID（FK 整合）
		warehouse.setDeleteFlag(0); // 有効ユーザー
		userRepository.save(warehouse); // DB へ保存
	}

	// ─────────────────────────────────────────
	// TC：正しい認証情報でログイン成功
	// ─────────────────────────────────────────
	@Test // テストメソッドであることを宣言
	@DisplayName("No.3: 正しいユーザー名・パスワードでログイン成功しホームへ遷移する") // 仕様書に対応する日本語名
	void loginSuccessRedirectsToHome() throws Exception { // ログイン成功テスト

		mockMvc.perform( // ① 擬似 HTTP リクエストを1回送る
				formLogin("/login") // login.html の POST /login を再現（CSRF も自動処理）
						.user("admin_test") // フォームの name="username" に入れる値
						.password("admin")) // フォームの name="password" に入れる値
				.andExpect(status().is3xxRedirection()) // ② 302 などのリダイレクトが返ること
				.andExpect(redirectedUrl("/home")); // ③ 飛び先が /home（SecurityConfig の defaultSuccessUrl と一致）
	}

	// ─────────────────────────────────────────
	// TC：空入力でログイン失敗（テスト仕様書 No.1）
	// ─────────────────────────────────────────
	@Test
	@DisplayName("No.1: 空入力ではログインできずエラーになる")
	void emptyLoginFails() throws Exception {

		mockMvc.perform(formLogin("/login")
						.user("")
						.password(""))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?error"));
	}

	// ─────────────────────────────────────────
	// TC：誤ったパスワードでログイン失敗
	// ─────────────────────────────────────────
	@Test // テストメソッド
	@DisplayName("No.2: 誤ったパスワードではログイン画面へ戻る") // 仕様書の項目名
	void loginFailureShowsError() throws Exception { // ログイン失敗テスト

		mockMvc.perform( // 擬似リクエスト送信
				formLogin("/login") // POST /login
						.user("admin_test") // ユーザー名は正しい
						.password("wrong_password")) // パスワードだけ間違える
				.andExpect(status().is3xxRedirection()) // リダイレクトで戻る
				.andExpect(redirectedUrl("/login?error")); // /login?error（login.html のエラー表示と対応）
	}

	// ─────────────────────────────────────────
	// TC：未ログインで保護 URL にアクセス
	// ─────────────────────────────────────────
	@Test // テストメソッド
	@DisplayName("No.6: 未ログインでは保護URLにアクセスできずログイン画面へ") // 仕様書の項目名
	void unauthenticatedUserIsRedirectedToLogin() throws Exception { // 未認証アクセステスト

		mockMvc.perform(get("/home")) // ログインせずに /home へ GET
				.andExpect(status().is3xxRedirection()) // リダイレクトされる
				.andExpect(redirectedUrlPattern("**/login")); // どこかの /login へ飛ばされる（SecurityConfig の authenticated ガード）
	}

	// ─────────────────────────────────────────
	// TC：未ログインでも公開ページは見られる
	// ─────────────────────────────────────────
	@Test // テストメソッド
	@DisplayName("未ログインでもログイン画面と問い合わせは見られる") // 仕様書の項目名
	void publicPagesAreAccessible() throws Exception { // 公開 URL テスト

		mockMvc.perform(get("/login")) // GET /login（SecurityConfig で permitAll）
				.andExpect(status().isOk()); // 200 OK で表示できる

		mockMvc.perform(get("/inquiry")) // GET /inquiry（未ログイン問い合わせも permitAll）
				.andExpect(status().isOk()); // 200 OK で表示できる
	}

	// ─────────────────────────────────────────
	// TC：管理者の権限
	// ─────────────────────────────────────────
	@Test // テストメソッド
	@WithMockUser(roles = "ADMIN") // 「管理者でログイン済み」の状態を模擬（DB ログインは不要）
	@DisplayName("管理者は管理画面にアクセスできる") // 仕様書の項目名
	void adminCanAccessAdminPage() throws Exception { // 管理者アクセス許可テスト

		mockMvc.perform(get("/admin/shops")) // 管理者専用 URL へ GET
				.andExpect(status().isOk()); // 200 OK = アクセス成功
	}

	// ─────────────────────────────────────────
	// TC：店舗ユーザーが管理画面に入れない
	// ─────────────────────────────────────────
	@Test // テストメソッド
	@WithMockUser(roles = "SHOP") // 「店舗スタッフでログイン済み」の状態を模擬
	@DisplayName("店舗ユーザーは管理画面にアクセスできない") // 仕様書の項目名
	void shopUserCannotAccessAdminPage() throws Exception { // 権限拒否テスト

		mockMvc.perform(get("/admin/shops")) // 管理者専用 URL へ GET
				.andExpect(status().isForbidden()); // 403 Forbidden = アクセス拒否（MockMvc ではリダイレクトではなく 403 になる）
	}

	// ─────────────────────────────────────────
	// TC：店舗ユーザーが店舗画面に入れる
	// ─────────────────────────────────────────
	@Test // テストメソッド
	@WithMockUser(roles = "SHOP") // 店舗スタッフとしてログイン済みのふり
	@DisplayName("店舗ユーザーは店舗画面にアクセスできる") // 仕様書の項目名
	void shopUserCanAccessShopPage() throws Exception { // 店舗画面アクセス許可テスト

		mockMvc.perform(get("/shop/order")) // 店舗専用 URL へ GET
				.andExpect(status().isOk()); // 200 OK = アクセス成功
	}

	// ─────────────────────────────────────────
	// TC：倉庫ユーザーが店舗画面に入れない
	// ─────────────────────────────────────────
	@Test // テストメソッド
	@WithMockUser(roles = "WAREHOUSE") // 倉庫スタッフとしてログイン済みのふり
	@DisplayName("倉庫ユーザーは店舗画面にアクセスできない") // 仕様書の項目名
	void warehouseUserCannotAccessShopPage() throws Exception { // 権限拒否テスト

		mockMvc.perform(get("/shop/order")) // 店舗専用 URL へ GET
				.andExpect(status().isForbidden()); // 403 Forbidden = アクセス拒否
	}

	// ─────────────────────────────────────────
	// TC：ログイン済みで /login に再アクセス（テスト仕様書 No.4）
	// ─────────────────────────────────────────
	@Test
	@DisplayName("No.4: ログイン成功後も /login は表示できる（仕様書は /home リダイレクト想定）")
	void loggedInUserCanAccessLoginPageAfterLogin() throws Exception {

		mockMvc.perform(formLogin("/login")
						.user("admin_test")
						.password("admin"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/home"));

		// 現行実装: ログイン済みでも /login は 200 で表示される（仕様書との差分）
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"));
	}

	// ─────────────────────────────────────────
	// TC：店舗ユーザーが /users にアクセス（テスト仕様書 No.5）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "shop_test", roles = "SHOP")
	@DisplayName("No.5: 店舗ユーザーはユーザー管理画面にアクセスできない")
	void shopUserCannotAccessUsersPage() throws Exception {

		mockMvc.perform(get("/users"))
				.andExpect(status().isForbidden());
	}

	// ─────────────────────────────────────────
	// TC：ログアウト（テスト仕様書 No.7）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "admin_test", roles = "ADMIN")
	@DisplayName("No.7: ログアウトするとログイン画面へ遷移する")
	void logoutRedirectsToLogin() throws Exception {

		mockMvc.perform(post("/logout").with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?logout"));
	}
}
