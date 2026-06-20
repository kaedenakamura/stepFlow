// 店舗の発注画面（/shop/order）をテストするクラス
package com.example.stepflow.controller;

// static import：GET 送信用
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
// static import：Model 検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
// static import：HTTP ステータス検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// static import：画面名検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

import com.example.stepflow.UserRepository.UserRepository;
import com.example.stepflow.entity.Category;
import com.example.stepflow.entity.Goods;
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.User;
import com.example.stepflow.repository.CategoryRepository;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.ShopOrderRepository;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.ShopStockRepository;

// ─────────────────────────────────────────
// 店舗系画面テストのお手本（ログインユーザー + 店舗マスタが前提）
// AdminShopControllerTest より DB 準備が1段多い点に注意
// ─────────────────────────────────────────
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShopOrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private GoodsRepository goodsRepository;

	@Autowired
	private ShopStockRepository shopStockRepository;

	@Autowired
	private ShopOrderRepository shopOrderRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	// テスト内で「どの店舗 ID を使うか」を覚えておく変数
	private Integer testShopId;

	@BeforeEach
	void setUp() {
		// 削除順：子テーブル → 親テーブル（FK 制約がある test DB では順番が重要）
		userRepository.deleteAll();
		shopOrderRepository.deleteAll();
		shopStockRepository.deleteAll(); // goods / shop を参照しているので goods より先に削除
		goodsRepository.deleteAll();
		categoryRepository.deleteAll();
		shopRepository.deleteAll();

		// ① 店舗マスタ（ログインユーザーの shop_id と紐づける）
		Shop shop = new Shop();
		shop.setShopName("発注テスト店舗");
		shop.setShopAddress("東京都渋谷区1-1");
		shop.setDeleteFlag(0);
		Shop savedShop = shopRepository.save(shop); // save の戻り値で ID を取得
		testShopId = savedShop.getShopId(); // 自動採番された shop_id を保持

		// ② カテゴリ・商品（発注選択画面のプルダウン用。なくても画面は開けるが、データがある方が現場に近い）
		Category category = new Category();
		category.setCategoryName("飲料");
		category.setDeleteFlag(0);
		Category savedCategory = categoryRepository.save(category);

		Goods goods = new Goods();
		goods.setGoodsName("テスト商品");
		goods.setCategoryId(savedCategory.getCategoryId());
		goods.setDeleteFlag(0);
		goodsRepository.save(goods);

		// ③ 店舗スタッフユーザー（shop_id を上の店舗と一致させるのがポイント）
		User shopUser = new User();
		shopUser.setUserName("shop_order_test");
		shopUser.setUserPassword(passwordEncoder.encode("shop"));
		shopUser.setAuthorityId(2); // 店舗スタッフ
		shopUser.setShopId(testShopId); // ← ここが null だと Controller がエラー画面になる
		shopUser.setDeleteFlag(0);
		userRepository.save(shopUser);
	}

	// ─────────────────────────────────────────
	// TC：発注商品選択画面
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "shop_order_test", roles = "SHOP") // username も合わせるとより本番に近い
	@DisplayName("店舗ユーザーは発注商品選択画面を表示できる")
	void shopUserCanViewOrderSelection() throws Exception {

		mockMvc.perform(get("/shop/order")) // ① GET /shop/order
				.andExpect(status().isOk()) // ② 200 OK
				.andExpect(view().name("shop/order-select")) // ③ 選択画面
				.andExpect(model().attributeExists("goodsRows")) // ④ 商品一覧
				.andExpect(model().attributeExists("categories")); // ⑤ カテゴリプルダウン
	}

	// ─────────────────────────────────────────
	// TC：発注一覧画面
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "shop_order_test", roles = "SHOP")
	@DisplayName("店舗ユーザーは発注一覧画面を表示できる")
	void shopUserCanViewOrderList() throws Exception {

		mockMvc.perform(get("/shop/order/list")) // ① 一覧 URL（クラス /shop/order + /list）
				.andExpect(status().isOk())
				.andExpect(view().name("shop/order-list")) // ② 一覧テンプレート
				.andExpect(model().attributeExists("orders")); // ③ 発注データ
	}

	// ─────────────────────────────────────────
	// TC：権限（管理者は店舗画面に入れない）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(roles = "ADMIN") // 管理者
	@DisplayName("管理者は店舗発注画面にアクセスできない")
	void adminCannotViewShopOrder() throws Exception {

		mockMvc.perform(get("/shop/order"))
				.andExpect(status().isForbidden()); // 403（SecurityConfig の ROLE_SHOP ガード）
	}

	@Test
	@WithMockUser(username = "shop_order_test", roles = "SHOP")
	@DisplayName("店舗ユーザーはExcel一括発注画面を表示できる")
	void shopUserCanViewImportForm() throws Exception {

		mockMvc.perform(get("/shop/order/import"))
				.andExpect(status().isOk())
				.andExpect(view().name("shop/order-import"));
	}

	@Test
	@WithMockUser(username = "shop_order_test", roles = "SHOP")
	@DisplayName("店舗ユーザーは発注Excelテンプレートをダウンロードできる")
	void shopUserCanDownloadOrderTemplate() throws Exception {

		mockMvc.perform(get("/shop/order/template/download"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("spreadsheetml")));
	}
}
