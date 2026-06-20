// 管理者の商品画面（/admin/goods）をテストするクラス（テスト仕様書 No.45-46）
package com.example.stepflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.entity.Category;
import com.example.stepflow.entity.Goods;
import com.example.stepflow.repository.CategoryRepository;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.ShopOrderRepository;
import com.example.stepflow.repository.ShopStockRepository;
import com.example.stepflow.repository.WarehouseStockRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminGoodsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private GoodsRepository goodsRepository;

	@Autowired
	private ShopStockRepository shopStockRepository;

	@Autowired
	private WarehouseStockRepository warehouseStockRepository;

	@Autowired
	private ShopOrderRepository shopOrderRepository;

	@BeforeEach
	void setUp() {
		shopOrderRepository.deleteAll();
		shopStockRepository.deleteAll();
		warehouseStockRepository.deleteAll();
		goodsRepository.deleteAll();
		categoryRepository.deleteAll();

		Category category = new Category();
		category.setCategoryName("商品テストカテゴリ");
		category.setDeleteFlag(0);
		Integer categoryId = categoryRepository.save(category).getCategoryId();

		Goods goods = new Goods();
		goods.setGoodsName("テスト商品A");
		goods.setCategoryId(categoryId);
		goods.setDeleteFlag(0);
		goodsRepository.save(goods);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("No.45: 管理者は商品一覧画面を表示できる")
	void adminCanViewGoodsList() throws Exception {

		mockMvc.perform(get("/admin/goods"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/goods-list"))
				.andExpect(model().attributeExists("goods"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("管理者は商品新規登録画面を表示できる")
	void adminCanViewNewGoodsForm() throws Exception {

		mockMvc.perform(get("/admin/goods/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/goods-form"))
				.andExpect(model().attributeExists("goods"));
	}

	@Test
	@WithMockUser(roles = "SHOP")
	@DisplayName("店舗ユーザーは商品管理画面にアクセスできない")
	void shopUserCannotViewGoodsList() throws Exception {

		mockMvc.perform(get("/admin/goods"))
				.andExpect(status().isForbidden());
	}
}
