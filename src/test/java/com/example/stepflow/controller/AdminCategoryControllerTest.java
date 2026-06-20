// 管理者のカテゴリ画面（/admin/categories）をテストするクラス（テスト仕様書 商品マスタ関連）
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
import com.example.stepflow.repository.CategoryRepository;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.repository.ShopOrderRepository;
import com.example.stepflow.repository.ShopStockRepository;
import com.example.stepflow.repository.WarehouseStockRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminCategoryControllerTest {

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
		category.setCategoryName("テストカテゴリ");
		category.setDeleteFlag(0);
		categoryRepository.save(category);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("管理者はカテゴリ一覧画面を表示できる")
	void adminCanViewCategoryList() throws Exception {

		mockMvc.perform(get("/admin/categories"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/category-list"))
				.andExpect(model().attributeExists("categories"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("管理者はカテゴリ新規登録画面を表示できる")
	void adminCanViewNewCategoryForm() throws Exception {

		mockMvc.perform(get("/admin/categories/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/category-form"))
				.andExpect(model().attributeExists("category"));
	}
}
