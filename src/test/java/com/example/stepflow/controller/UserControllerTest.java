// 管理者のユーザー管理画面（/users）をテストするクラス
package com.example.stepflow.controller;

// static import：GET 送信用
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
// static import：Model 検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
// static import：リダイレクト先 URL のパターン一致検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.User;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.WarehouseRepository;

// ─────────────────────────────────────────
// SecurityConfig：/users/** は ROLE_ADMIN 必須（/users/new と POST /users のみ例外）
// ─────────────────────────────────────────
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Integer shopUserId;
	private Integer warehouseUserId;
	private Integer shopId;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();

		Shop shop = new Shop();
		shop.setShopName("ユーザー管理テスト店舗");
		shop.setShopAddress("東京都新宿区1-1");
		shop.setDeleteFlag(0);
		shopId = shopRepository.save(shop).getShopId();

		Warehouse warehouse = new Warehouse();
		warehouse.setWarehouseName("ユーザー管理テスト倉庫");
		warehouse.setWarehouseAddress("東京都品川区1-1");
		warehouse.setDeleteFlag(0);
		Integer warehouseId = warehouseRepository.save(warehouse).getWarehouseId();

		User admin = new User();
		admin.setUserName("admin_user_test");
		admin.setUserPassword(passwordEncoder.encode("admin"));
		admin.setAuthorityId(1);
		admin.setDeleteFlag(0);
		userRepository.save(admin);

		User shopUser = new User();
		shopUser.setUserName("shop_user_test");
		shopUser.setUserPassword(passwordEncoder.encode("shop"));
		shopUser.setAuthorityId(2);
		shopUser.setShopId(shopId);
		shopUser.setDeleteFlag(0);
		shopUserId = userRepository.save(shopUser).getUserId();

		User warehouseUser = new User();
		warehouseUser.setUserName("warehouse_user_test");
		warehouseUser.setUserPassword(passwordEncoder.encode("warehouse"));
		warehouseUser.setAuthorityId(3);
		warehouseUser.setWarehouseId(warehouseId);
		warehouseUser.setDeleteFlag(0);
		warehouseUserId = userRepository.save(warehouseUser).getUserId();
	}

	// ─────────────────────────────────────────
	// TC：ユーザー一覧画面
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "admin_user_test", roles = "ADMIN")
	@DisplayName("管理者はユーザー一覧画面を表示できる")
	void adminCanViewUserList() throws Exception {

		mockMvc.perform(get("/users"))
				.andExpect(status().isOk())
				.andExpect(view().name("user-list"))
				.andExpect(model().attributeExists("users"));
	}

	// ─────────────────────────────────────────
	// TC：権限で絞り込み
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "admin_user_test", roles = "ADMIN")
	@DisplayName("管理者は権限IDでユーザーを絞り込める")
	void adminCanFilterUsersByAuthority() throws Exception {

		mockMvc.perform(get("/users").param("authorityId", "2"))
				.andExpect(status().isOk())
				.andExpect(view().name("user-list"))
				.andExpect(model().attribute("authorityId", 2))
				.andExpect(model().attributeExists("users"));
	}

	// ─────────────────────────────────────────
	// TC：新規登録画面
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "admin_user_test", roles = "ADMIN")
	@DisplayName("管理者はユーザー新規登録画面を表示できる")
	void adminCanViewNewUserForm() throws Exception {

		mockMvc.perform(get("/users/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("user-form"))
				.andExpect(model().attributeExists("user"))
				.andExpect(model().attributeExists("shops"))
				.andExpect(model().attributeExists("warehouses"));
	}

	// ─────────────────────────────────────────
	// TC：編集画面
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "admin_user_test", roles = "ADMIN")
	@DisplayName("管理者はユーザー編集画面を表示できる")
	void adminCanViewEditUserForm() throws Exception {

		mockMvc.perform(get("/users/edit/" + shopUserId))
				.andExpect(status().isOk())
				.andExpect(view().name("user-form"))
				.andExpect(model().attributeExists("user"));
	}

	// ─────────────────────────────────────────
	// TC：権限エラー
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "shop_user_test", roles = "SHOP")
	@DisplayName("店舗ユーザーはユーザー一覧画面にアクセスできない")
	void shopUserCannotViewUserList() throws Exception {

		mockMvc.perform(get("/users"))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("未ログインではユーザー一覧画面に入れずログインへ")
	void unauthenticatedCannotViewUserList() throws Exception {

		mockMvc.perform(get("/users"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	@WithMockUser(username = "admin_user_test", roles = "ADMIN")
	@DisplayName("No.33: 管理者はユーザーを論理削除できる")
	void adminCanDeleteUser() throws Exception {

		mockMvc.perform(post("/users/delete/" + warehouseUserId).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/users"));
	}
}
