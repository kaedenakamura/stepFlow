// ホーム画面（/home）をテストするクラス（テスト仕様書 No.8, No.48）
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
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.User;
import com.example.stepflow.repository.ShopRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HomeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Integer testShopId;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();

		Shop shop = new Shop();
		shop.setShopName("ホームテスト店舗");
		shop.setShopAddress("東京都渋谷区1-1");
		shop.setDeleteFlag(0);
		testShopId = shopRepository.save(shop).getShopId();

		User admin = new User();
		admin.setUserName("admin_home_test");
		admin.setUserPassword(passwordEncoder.encode("admin"));
		admin.setAuthorityId(1);
		admin.setDeleteFlag(0);
		userRepository.save(admin);

		User shopUser = new User();
		shopUser.setUserName("shop_home_test");
		shopUser.setUserPassword(passwordEncoder.encode("shop"));
		shopUser.setAuthorityId(2);
		shopUser.setShopId(testShopId);
		shopUser.setDeleteFlag(0);
		userRepository.save(shopUser);
	}

	@Test
	@WithMockUser(username = "admin_home_test", roles = "ADMIN")
	@DisplayName("No.8: 管理者はホーム画面を表示できる")
	void adminCanViewHome() throws Exception {

		mockMvc.perform(get("/home"))
				.andExpect(status().isOk())
				.andExpect(view().name("home"))
				.andExpect(model().attributeExists("loginUsername"));
	}

	@Test
	@WithMockUser(username = "shop_home_test", roles = "SHOP")
	@DisplayName("No.48: 店舗ユーザーはホームにダッシュボード情報が表示される")
	void shopUserCanViewDashboard() throws Exception {

		mockMvc.perform(get("/home"))
				.andExpect(status().isOk())
				.andExpect(view().name("home"))
				.andExpect(model().attribute("showShopDashboard", true))
				.andExpect(model().attributeExists("rankingRows"));
	}
}
