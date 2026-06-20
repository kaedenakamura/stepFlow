// 倉庫の受注画面（/warehouse/stock）をテストするクラス
package com.example.stepflow.controller;

// static import：GET 送信用
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// static import：POST 送信用（ステータス更新テストで使う）
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// static import：HTTP ヘッダー検証用（CSV/PDF の Content-Type 確認）
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
// static import：Model 検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
// static import：リダイレクト先 URL 検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
// static import：HTTP ステータス検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// static import：画面名検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
// Hamcrest：文字列に部分一致するか（Content-Type 確認用）
import static org.hamcrest.Matchers.containsString;
// Spring Security Test：POST 時の CSRF トークン（本番と同じく CSRF 有効のため必須）
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.example.stepflow.UserRepository.UserRepository;
import com.example.stepflow.entity.Category;
import com.example.stepflow.entity.Goods;
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.ShopOrder;
import com.example.stepflow.entity.User;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.repository.CategoryRepository;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.ShopOrderRepository;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.ShopStockRepository;
import com.example.stepflow.repository.WarehouseRepository;
import com.example.stepflow.repository.WarehouseStockRepository;

// ─────────────────────────────────────────
// ShopOrderControllerTest の「倉庫版」
// 倉庫マスタ + ユーザー warehouse_id + 受注データ がセットで必要
// ─────────────────────────────────────────
@SpringBootTest // アプリ全体起動
@AutoConfigureMockMvc // MockMvc 有効化
@ActiveProfiles("test") // テスト DB
@Transactional // テスト後ロールバック
class WarehouseOrderControllerTest {

	@Autowired // 擬似 HTTP クライアント
	private MockMvc mockMvc;

	@Autowired // 倉庫スタッフユーザー insert 用
	private UserRepository userRepository;

	@Autowired // 店舗マスタ insert 用
	private ShopRepository shopRepository;

	@Autowired // 倉庫マスタ insert 用
	private WarehouseRepository warehouseRepository;

	@Autowired // カテゴリ insert 用
	private CategoryRepository categoryRepository;

	@Autowired // 商品 insert 用
	private GoodsRepository goodsRepository;

	@Autowired // 受注データ insert 用
	private ShopOrderRepository shopOrderRepository;

	@Autowired
	private ShopStockRepository shopStockRepository;

	@Autowired
	private WarehouseStockRepository warehouseStockRepository;

	@Autowired // パスワード暗号化用
	private PasswordEncoder passwordEncoder;

	// テストデータの ID を @BeforeEach で覚えておく（後のテストで使う）
	private Integer testWarehouseId;
	private Integer testShopId;
	private Integer testGoodsId;
	private Integer testOrderId;

	@BeforeEach // 各テストの直前に実行（仕様書の前提条件）
	void setUp() {
		// 子 → 親の順で削除（外部キー制約がある test DB 用）
		userRepository.deleteAll();
		shopOrderRepository.deleteAll();
		shopStockRepository.deleteAll();
		warehouseStockRepository.deleteAll();
		goodsRepository.deleteAll();
		categoryRepository.deleteAll();
		shopRepository.deleteAll();
		warehouseRepository.deleteAll();

		// ① 倉庫マスタ
		Warehouse warehouse = new Warehouse();
		warehouse.setWarehouseName("テスト倉庫");
		warehouse.setWarehouseAddress("東京都品川区1-1");
		warehouse.setDeleteFlag(0);
		testWarehouseId = warehouseRepository.save(warehouse).getWarehouseId();

		// ② 店舗マスタ（受注の shop_id 用）
		Shop shop = new Shop();
		shop.setShopName("受注テスト店舗");
		shop.setShopAddress("東京都新宿区1-1");
		shop.setDeleteFlag(0);
		testShopId = shopRepository.save(shop).getShopId();

		// ③ カテゴリ・商品（受注の goods_id 用）
		Category category = new Category();
		category.setCategoryName("飲料");
		category.setDeleteFlag(0);
		Integer categoryId = categoryRepository.save(category).getCategoryId();

		Goods goods = new Goods();
		goods.setGoodsName("受注テスト商品");
		goods.setCategoryId(categoryId);
		goods.setDeleteFlag(0);
		testGoodsId = goodsRepository.save(goods).getGoodsId();

		// ④ 倉庫スタッフユーザー（warehouse_id を倉庫と一致させる）
		User warehouseUser = new User();
		warehouseUser.setUserName("warehouse_order_test");
		warehouseUser.setUserPassword(passwordEncoder.encode("warehouse"));
		warehouseUser.setAuthorityId(3); // 倉庫スタッフ
		warehouseUser.setWarehouseId(testWarehouseId);
		warehouseUser.setDeleteFlag(0);
		userRepository.save(warehouseUser);

		// ⑤ 受注データ（ステータス「準備中」）
		ShopOrder order = new ShopOrder();
		order.setShopId(testShopId);
		order.setWarehouseId(testWarehouseId);
		order.setGoodsId(testGoodsId);
		order.setOrderQuantity(10);
		order.setOrderStatus("準備中");
		order.setDeleteFlag(0);
		order.setUpdateDate(LocalDateTime.now()); // test DB は update_date NOT NULL（本番 Service と同様）
		testOrderId = shopOrderRepository.save(order).getShopOrderId();

		// ⑥ 別ステータスの受注（絞り込みテスト用）
		ShopOrder orderDone = new ShopOrder();
		orderDone.setShopId(testShopId);
		orderDone.setWarehouseId(testWarehouseId);
		orderDone.setGoodsId(testGoodsId);
		orderDone.setOrderQuantity(3);
		orderDone.setOrderStatus("発注済");
		orderDone.setDeleteFlag(0);
		orderDone.setUpdateDate(LocalDateTime.now()); // 同上
		shopOrderRepository.save(orderDone);
	}

	// ─────────────────────────────────────────
	// TC：受注一覧画面
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "warehouse_order_test", roles = "WAREHOUSE")
	@DisplayName("倉庫ユーザーは受注一覧画面を表示できる")
	void warehouseUserCanViewStockList() throws Exception {

		mockMvc.perform(get("/warehouse/stock")) // ① GET 一覧 URL
				.andExpect(status().isOk()) // ② 200 OK
				.andExpect(view().name("warehouse/stock-list")) // ③ テンプレート名
				.andExpect(model().attributeExists("orders")); // ④ 受注一覧が Model にある
	}

	// ─────────────────────────────────────────
	// TC：ステータス絞り込み
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "warehouse_order_test", roles = "WAREHOUSE")
	@DisplayName("倉庫ユーザーはステータスで受注を絞り込める")
	void warehouseUserCanFilterByStatus() throws Exception {

		mockMvc.perform(get("/warehouse/stock").param("status", "準備中")) // ① ?status=準備中
				.andExpect(status().isOk()) // ② 画面は開ける
				.andExpect(view().name("warehouse/stock-list")) // ③ 同じ一覧画面
				.andExpect(model().attribute("statusFilter", "準備中")); // ④ 絞り込み条件が Model に残る
	}

	// ─────────────────────────────────────────
	// TC：CSV ダウンロード
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "warehouse_order_test", roles = "WAREHOUSE")
	@DisplayName("倉庫ユーザーは受注一覧をCSVでダウンロードできる")
	void warehouseUserCanDownloadCsv() throws Exception {

		mockMvc.perform(get("/warehouse/stock/csv")) // ① CSV URL
				.andExpect(status().isOk()) // ② 200 OK
				.andExpect(header().string("Content-Type", containsString("text/csv"))); // ③ CSV 形式
	}

	// ─────────────────────────────────────────
	// TC：PDF ダウンロード
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "warehouse_order_test", roles = "WAREHOUSE")
	@DisplayName("倉庫ユーザーは受注一覧をPDFでダウンロードできる")
	void warehouseUserCanDownloadPdf() throws Exception {

		mockMvc.perform(get("/warehouse/stock/pdf")) // ① PDF URL
				.andExpect(status().isOk()) // ② 200 OK
				.andExpect(header().string("Content-Type", containsString("application/pdf"))); // ③ PDF 形式
	}

	// ─────────────────────────────────────────
	// TC：ステータス更新（POST）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "warehouse_order_test", roles = "WAREHOUSE")
	@DisplayName("倉庫ユーザーは受注ステータスを更新できる")
	void warehouseUserCanUpdateOrderStatus() throws Exception {

		mockMvc.perform(post("/warehouse/stock/{id}/status", testOrderId) // ① POST（パス変数 id）
				.with(csrf()) // ② CSRF トークン（Security 有効時は必須）
				.param("orderStatus", "発注済") // ③ 新しいステータス
				.param("filterStatus", "準備中") // ④ 更新後リダイレクト用（画面の hidden と同じ）
				.param("filterFrom", "2026-01-01") // ⑤ 日付パラメータ（LocalDate 変換用）
				.param("filterTo", "2026-12-31")) // ⑥ 日付パラメータ
				.andExpect(status().is3xxRedirection()) // ⑦ リダイレクト
				.andExpect(redirectedUrl("/warehouse/stock?status=%E6%BA%96%E5%82%99%E4%B8%AD&from=2026-01-01&to=2026-12-31"));
	}

	@Test
	@WithMockUser(username = "warehouse_order_test", roles = "WAREHOUSE")
	@DisplayName("絞り込み未使用時でも受注ステータスを更新できる")
	void warehouseUserCanUpdateOrderStatusWithoutFilters() throws Exception {

		mockMvc.perform(post("/warehouse/stock/{id}/status", testOrderId)
				.with(csrf())
				.param("orderStatus", "発注済")
				.param("filterStatus", "")
				.param("filterFrom", "")
				.param("filterTo", ""))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/warehouse/stock"));
	}

	// ─────────────────────────────────────────
	// TC：権限（店舗ユーザーは倉庫画面不可）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(roles = "SHOP")
	@DisplayName("店舗ユーザーは倉庫受注画面にアクセスできない")
	void shopUserCannotViewWarehouseStock() throws Exception {

		mockMvc.perform(get("/warehouse/stock"))
				.andExpect(status().isForbidden()); // 403
	}

	// ─────────────────────────────────────────
	// TC：権限（管理者は倉庫画面不可）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("管理者は倉庫受注画面にアクセスできない")
	void adminCannotViewWarehouseStock() throws Exception {

		mockMvc.perform(get("/warehouse/stock"))
				.andExpect(status().isForbidden()); // 403
	}
}
