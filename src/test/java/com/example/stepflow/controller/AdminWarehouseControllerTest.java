// 管理者の倉庫画面（/admin/warehouses）をテストするクラス（テスト仕様書 No.39-40）
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

import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.repository.WarehouseRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminWarehouseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@BeforeEach
	void setUp() {
		warehouseRepository.deleteAll();

		Warehouse warehouseA = new Warehouse();
		warehouseA.setWarehouseName("テスト倉庫A");
		warehouseA.setWarehouseAddress("東京都品川区1-1");
		warehouseA.setDeleteFlag(0);
		warehouseRepository.save(warehouseA);

		Warehouse warehouseB = new Warehouse();
		warehouseB.setWarehouseName("テスト倉庫B");
		warehouseB.setWarehouseAddress("大阪府大阪市1-1");
		warehouseB.setDeleteFlag(0);
		warehouseRepository.save(warehouseB);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("No.39: 管理者は倉庫一覧画面を表示できる")
	void adminCanViewWarehouseList() throws Exception {

		mockMvc.perform(get("/admin/warehouses"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/warehouse-list"))
				.andExpect(model().attributeExists("warehouses"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("No.40: 管理者は住所で倉庫を絞り込める")
	void adminCanFilterWarehousesByAddress() throws Exception {

		mockMvc.perform(get("/admin/warehouses").param("address", "品川"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/warehouse-list"))
				.andExpect(model().attributeExists("warehouses"));
	}

	@Test
	@WithMockUser(roles = "SHOP")
	@DisplayName("店舗ユーザーは倉庫一覧画面にアクセスできない")
	void shopUserCannotViewWarehouseList() throws Exception {

		mockMvc.perform(get("/admin/warehouses"))
				.andExpect(status().isForbidden());
	}
}
