// 店舗の在庫画面（/shop/inventory）をテストするクラス
package com.example.stepflow.controller;

// static import：GET 送信用
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// static import：POST 送信用
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// static import：Model 検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
// static import：リダイレクト先 URL 検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
// static import：HTTP ステータス検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// static import：画面名検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
// Spring Security Test：POST 時の CSRF トークン
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

import com.example.stepflow.UserRepository.UserRepository;
import com.example.stepflow.entity.Category;
import com.example.stepflow.entity.Goods;
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.ShopStock;
import com.example.stepflow.entity.User;
import com.example.stepflow.repository.CategoryRepository;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.ShopStockRepository;

// ─────────────────────────────────────────
// ShopOrderControllerTest と同様：店舗マスタ + 商品 + shop_stock + 店舗ユーザーが前提
// ─────────────────────────────────────────
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShopInventoryControllerTest {

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
	private PasswordEncoder passwordEncoder;

	private Integer testShopId;
	private Integer testCategoryId;
	private Integer testShopStockId;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
		shopStockRepository.deleteAll();
		goodsRepository.deleteAll();
		categoryRepository.deleteAll();
		shopRepository.deleteAll();

		Shop shop = new Shop();
		shop.setShopName("在庫テスト店舗");
		shop.setShopAddress("東京都渋谷区1-1");
		shop.setDeleteFlag(0);
		testShopId = shopRepository.save(shop).getShopId();

		Category category = new Category();
		category.setCategoryName("飲料");
		category.setDeleteFlag(0);
		testCategoryId = categoryRepository.save(category).getCategoryId();

		Goods goods = new Goods();
		goods.setGoodsName("在庫テスト商品");
		goods.setCategoryId(testCategoryId);
		goods.setDeleteFlag(0);
		Integer goodsId = goodsRepository.save(goods).getGoodsId();

		ShopStock stock = new ShopStock();
		stock.setShopId(testShopId);
		stock.setGoodsId(goodsId);
		stock.setShopStock(5);
		stock.setDeleteFlag(0);
		testShopStockId = shopStockRepository.save(stock).getShopStockId();

		User shopUser = new User();
		shopUser.setUserName("shop_inventory_test");
		shopUser.setUserPassword(passwordEncoder.encode("shop"));
		shopUser.setAuthorityId(2);
		shopUser.setShopId(testShopId);
		shopUser.setDeleteFlag(0);
		userRepository.save(shopUser);
	}

	// ─────────────────────────────────────────
	// TC：在庫一覧画面
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "shop_inventory_test", roles = "SHOP")
	@DisplayName("店舗ユーザーは在庫一覧画面を表示できる")
	void shopUserCanViewInventoryList() throws Exception {

		mockMvc.perform(get("/shop/inventory"))
				.andExpect(status().isOk())
				.andExpect(view().name("shop/inventory-list"))
				.andExpect(model().attributeExists("stocks"))
				.andExpect(model().attributeExists("categories"));
	}

	// ─────────────────────────────────────────
	// TC：カテゴリ絞り込み
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "shop_inventory_test", roles = "SHOP")
	@DisplayName("店舗ユーザーはカテゴリで在庫を絞り込める")
	void shopUserCanFilterInventoryByCategory() throws Exception {

		mockMvc.perform(get("/shop/inventory").param("categoryId", testCategoryId.toString()))
				.andExpect(status().isOk())
				.andExpect(view().name("shop/inventory-list"))
				.andExpect(model().attribute("selectedCategoryId", testCategoryId))
				.andExpect(model().attributeExists("stocks"));
	}

	// ─────────────────────────────────────────
	// TC：在庫編集画面
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "shop_inventory_test", roles = "SHOP")
	@DisplayName("店舗ユーザーは自店舗の在庫編集画面を表示できる")
	void shopUserCanViewEditForm() throws Exception {

		mockMvc.perform(get("/shop/inventory/edit/" + testShopStockId))
				.andExpect(status().isOk())
				.andExpect(view().name("shop/inventory-edit"))
				.andExpect(model().attributeExists("stock"));
	}

	// ─────────────────────────────────────────
	// TC：在庫数量更新
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "shop_inventory_test", roles = "SHOP")
	@DisplayName("店舗ユーザーは在庫数量を更新できる")
	void shopUserCanUpdateQuantity() throws Exception {

		mockMvc.perform(post("/shop/inventory/edit/" + testShopStockId)
						.with(csrf())
						.param("quantity", "10"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/shop/inventory"));
	}

	// ─────────────────────────────────────────
	// TC：権限（管理者は店舗画面に入れない）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("管理者は店舗在庫画面にアクセスできない")
	void adminCannotViewShopInventory() throws Exception {

		mockMvc.perform(get("/shop/inventory"))
				.andExpect(status().isForbidden());
	}
}
