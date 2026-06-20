// 倉庫の在庫画面（/warehouse/inventory）をテストするクラス（テスト仕様書 No.57-58）
package com.example.stepflow.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import com.example.stepflow.entity.User;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.entity.WarehouseStock;
import com.example.stepflow.repository.CategoryRepository;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.ShopStockRepository;
import com.example.stepflow.repository.WarehouseRepository;
import com.example.stepflow.repository.WarehouseStockRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WarehouseInventoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private GoodsRepository goodsRepository;

	@Autowired
	private ShopStockRepository shopStockRepository;

	@Autowired
	private WarehouseStockRepository warehouseStockRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Integer testWarehouseStockId;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
		warehouseStockRepository.deleteAll();
		shopStockRepository.deleteAll();
		goodsRepository.deleteAll();
		categoryRepository.deleteAll();
		warehouseRepository.deleteAll();

		Warehouse warehouse = new Warehouse();
		warehouse.setWarehouseName("在庫テスト倉庫");
		warehouse.setWarehouseAddress("東京都品川区2-2");
		warehouse.setDeleteFlag(0);
		Integer warehouseId = warehouseRepository.save(warehouse).getWarehouseId();

		Category category = new Category();
		category.setCategoryName("飲料");
		category.setDeleteFlag(0);
		Integer categoryId = categoryRepository.save(category).getCategoryId();

		Goods goods = new Goods();
		goods.setGoodsName("倉庫在庫テスト商品");
		goods.setCategoryId(categoryId);
		goods.setDeleteFlag(0);
		Integer goodsId = goodsRepository.save(goods).getGoodsId();

		WarehouseStock stock = new WarehouseStock();
		stock.setWarehouseId(warehouseId);
		stock.setGoodsId(goodsId);
		stock.setWarehouseStock(20);
		stock.setDeleteFlag(0);
		testWarehouseStockId = warehouseStockRepository.save(stock).getWarehouseStockId();

		User warehouseUser = new User();
		warehouseUser.setUserName("warehouse_inventory_test");
		warehouseUser.setUserPassword(passwordEncoder.encode("warehouse"));
		warehouseUser.setAuthorityId(3);
		warehouseUser.setWarehouseId(warehouseId);
		warehouseUser.setDeleteFlag(0);
		userRepository.save(warehouseUser);
	}

	@Test
	@WithMockUser(username = "warehouse_inventory_test", roles = "WAREHOUSE")
	@DisplayName("No.57: 倉庫ユーザーは自倉庫の在庫一覧を表示できる")
	void warehouseUserCanViewInventoryList() throws Exception {

		mockMvc.perform(get("/warehouse/inventory"))
				.andExpect(status().isOk())
				.andExpect(view().name("warehouse/inventory-list"))
				.andExpect(model().attributeExists("stocks"));
	}

	@Test
	@WithMockUser(username = "warehouse_inventory_test", roles = "WAREHOUSE")
	@DisplayName("No.58: 倉庫ユーザーは在庫数量を更新できる")
	void warehouseUserCanUpdateQuantity() throws Exception {

		mockMvc.perform(post("/warehouse/inventory/edit/" + testWarehouseStockId)
						.with(csrf())
						.param("quantity", "30"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/warehouse/inventory"));
	}

	@Test
	@WithMockUser(username = "warehouse_inventory_test", roles = "WAREHOUSE")
	@DisplayName("在庫数未入力は編集画面にエラーを表示する")
	void blankQuantityShowsValidationError() throws Exception {

		mockMvc.perform(post("/warehouse/inventory/edit/" + testWarehouseStockId)
						.with(csrf())
						.param("quantity", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("warehouse/inventory-edit"))
				.andExpect(model().attribute("quantityError", "在庫数を入力してください"));
	}

	@Test
	@WithMockUser(roles = "SHOP")
	@DisplayName("店舗ユーザーは倉庫在庫画面にアクセスできない")
	void shopUserCannotViewWarehouseInventory() throws Exception {

		mockMvc.perform(get("/warehouse/inventory"))
				.andExpect(status().isForbidden());
	}
}
