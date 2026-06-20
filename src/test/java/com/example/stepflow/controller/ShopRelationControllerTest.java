// 店舗の連携倉庫画面（/shop/relations）をテストするクラス（テスト仕様書 No.52）
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.UserRepository.UserRepository;
import com.example.stepflow.entity.Relation;
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.User;
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
class ShopRelationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private RelationRepository relationRepository;

	@Autowired
	private ShopStockRepository shopStockRepository;

	@Autowired
	private WarehouseStockRepository warehouseStockRepository;

	@Autowired
	private ShopOrderRepository shopOrderRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
		relationRepository.deleteAll();
		shopOrderRepository.deleteAll();
		shopStockRepository.deleteAll();
		warehouseStockRepository.deleteAll();
		shopRepository.deleteAll();
		warehouseRepository.deleteAll();

		Shop shop = new Shop();
		shop.setShopName("連携テスト店舗");
		shop.setShopAddress("東京都渋谷区1-1");
		shop.setDeleteFlag(0);
		Integer shopId = shopRepository.save(shop).getShopId();

		Warehouse warehouse = new Warehouse();
		warehouse.setWarehouseName("連携テスト倉庫");
		warehouse.setWarehouseAddress("東京都品川区1-1");
		warehouse.setDeleteFlag(0);
		Integer warehouseId = warehouseRepository.save(warehouse).getWarehouseId();

		Relation relation = new Relation();
		relation.setShopId(shopId);
		relation.setWarehouseId(warehouseId);
		relation.setDeleteFlag(0);
		relationRepository.save(relation);

		User shopUser = new User();
		shopUser.setUserName("shop_relation_test");
		shopUser.setUserPassword(passwordEncoder.encode("shop"));
		shopUser.setAuthorityId(2);
		shopUser.setShopId(shopId);
		shopUser.setDeleteFlag(0);
		userRepository.save(shopUser);
	}

	@Test
	@WithMockUser(username = "shop_relation_test", roles = "SHOP")
	@DisplayName("No.52: 店舗ユーザーは連携倉庫一覧を表示できる")
	void shopUserCanViewRelationList() throws Exception {

		mockMvc.perform(get("/shop/relations"))
				.andExpect(status().isOk())
				.andExpect(view().name("shop/relation-list"))
				.andExpect(model().attributeExists("relations"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("管理者は店舗連携倉庫画面にアクセスできない")
	void adminCannotViewShopRelations() throws Exception {

		mockMvc.perform(get("/shop/relations"))
				.andExpect(status().isForbidden());
	}
}
