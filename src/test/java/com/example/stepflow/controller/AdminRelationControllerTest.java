// 管理者の連携画面（/admin/relations）をテストするクラス（テスト仕様書 No.47）
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

import com.example.stepflow.entity.Relation;
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.repository.RelationRepository;
import com.example.stepflow.repository.ShopOrderRepository;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.ShopStockRepository;
import com.example.stepflow.repository.WarehouseRepository;
import com.example.stepflow.repository.WarehouseStockRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminRelationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RelationRepository relationRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private ShopStockRepository shopStockRepository;

	@Autowired
	private WarehouseStockRepository warehouseStockRepository;

	@Autowired
	private ShopOrderRepository shopOrderRepository;

	@BeforeEach
	void setUp() {
		relationRepository.deleteAll();
		shopOrderRepository.deleteAll();
		shopStockRepository.deleteAll();
		warehouseStockRepository.deleteAll();
		shopRepository.deleteAll();
		warehouseRepository.deleteAll();

		Shop shop = new Shop();
		shop.setShopName("連携管理テスト店舗");
		shop.setShopAddress("東京都新宿区1-1");
		shop.setDeleteFlag(0);
		Integer shopId = shopRepository.save(shop).getShopId();

		Warehouse warehouse = new Warehouse();
		warehouse.setWarehouseName("連携管理テスト倉庫");
		warehouse.setWarehouseAddress("東京都品川区1-1");
		warehouse.setDeleteFlag(0);
		Integer warehouseId = warehouseRepository.save(warehouse).getWarehouseId();

		Relation relation = new Relation();
		relation.setShopId(shopId);
		relation.setWarehouseId(warehouseId);
		relation.setDeleteFlag(0);
		relationRepository.save(relation);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("No.47: 管理者は連携一覧画面を表示できる")
	void adminCanViewRelationList() throws Exception {

		mockMvc.perform(get("/admin/relations"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/relation-list"))
				.andExpect(model().attributeExists("relations"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("管理者は連携新規登録画面を表示できる")
	void adminCanViewNewRelationForm() throws Exception {

		mockMvc.perform(get("/admin/relations/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/relation-form"))
				.andExpect(model().attributeExists("relation"));
	}
}
