// 店舗の発注履歴画面（/shop/sales）をテストするクラス（テスト仕様書 発注履歴関連）
package com.example.stepflow.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;

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
import com.example.stepflow.entity.ShopOrder;
import com.example.stepflow.entity.User;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.repository.CategoryRepository;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.ShopOrderRepository;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.WarehouseRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShopSalesControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private GoodsRepository goodsRepository;

	@Autowired
	private ShopOrderRepository shopOrderRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
		shopOrderRepository.deleteAll();
		goodsRepository.deleteAll();
		categoryRepository.deleteAll();
		shopRepository.deleteAll();
		warehouseRepository.deleteAll();

		Shop shop = new Shop();
		shop.setShopName("売上テスト店舗");
		shop.setShopAddress("東京都渋谷区1-1");
		shop.setDeleteFlag(0);
		Integer shopId = shopRepository.save(shop).getShopId();

		Warehouse warehouse = new Warehouse();
		warehouse.setWarehouseName("売上テスト倉庫");
		warehouse.setWarehouseAddress("東京都品川区1-1");
		warehouse.setDeleteFlag(0);
		Integer warehouseId = warehouseRepository.save(warehouse).getWarehouseId();

		Category category = new Category();
		category.setCategoryName("飲料");
		category.setDeleteFlag(0);
		Integer categoryId = categoryRepository.save(category).getCategoryId();

		Goods goods = new Goods();
		goods.setGoodsName("売上テスト商品");
		goods.setCategoryId(categoryId);
		goods.setDeleteFlag(0);
		Integer goodsId = goodsRepository.save(goods).getGoodsId();

		ShopOrder order = new ShopOrder();
		order.setShopId(shopId);
		order.setWarehouseId(warehouseId);
		order.setGoodsId(goodsId);
		order.setOrderQuantity(5);
		order.setOrderStatus("準備中");
		order.setDeleteFlag(0);
		order.setUpdateDate(LocalDateTime.now());
		shopOrderRepository.save(order);

		User shopUser = new User();
		shopUser.setUserName("shop_sales_test");
		shopUser.setUserPassword(passwordEncoder.encode("shop"));
		shopUser.setAuthorityId(2);
		shopUser.setShopId(shopId);
		shopUser.setDeleteFlag(0);
		userRepository.save(shopUser);
	}

	@Test
	@WithMockUser(username = "shop_sales_test", roles = "SHOP")
	@DisplayName("店舗ユーザーは発注履歴画面を表示できる")
	void shopUserCanViewSalesHistory() throws Exception {

		mockMvc.perform(get("/shop/sales"))
				.andExpect(status().isOk())
				.andExpect(view().name("shop/sales-list"))
				.andExpect(model().attributeExists("orders"));
	}

	@Test
	@WithMockUser(username = "shop_sales_test", roles = "SHOP")
	@DisplayName("店舗ユーザーはステータスで発注履歴を絞り込める")
	void shopUserCanFilterSalesByStatus() throws Exception {

		mockMvc.perform(get("/shop/sales").param("status", "準備中"))
				.andExpect(status().isOk())
				.andExpect(view().name("shop/sales-list"))
				.andExpect(model().attribute("statusFilter", "準備中"));
	}

	@Test
	@WithMockUser(username = "shop_sales_test", roles = "SHOP")
	@DisplayName("店舗ユーザーは発注履歴をCSVでダウンロードできる")
	void shopUserCanDownloadSalesCsv() throws Exception {

		mockMvc.perform(get("/shop/sales/csv"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("text/csv")));
	}

	@Test
	@WithMockUser(username = "shop_sales_test", roles = "SHOP")
	@DisplayName("店舗ユーザーは発注履歴をPDFでダウンロードできる")
	void shopUserCanDownloadSalesPdf() throws Exception {

		mockMvc.perform(get("/shop/sales/pdf"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/pdf")));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("管理者は店舗発注履歴画面にアクセスできない")
	void adminCannotViewShopSales() throws Exception {

		mockMvc.perform(get("/shop/sales"))
				.andExpect(status().isForbidden());
	}
}
