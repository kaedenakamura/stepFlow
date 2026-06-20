// 管理者の問い合わせ画面（/admin/inquiries）をテストするクラス（テスト仕様書 No.10-12）
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.UserRepository.UserRepository;
import com.example.stepflow.entity.Inquiry;
import com.example.stepflow.entity.User;
import com.example.stepflow.repository.InquiryRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminInquiryControllerTest {

	private static final int GUEST_USER_ID = 99;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private InquiryRepository inquiryRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer adminInquiryId;

	@BeforeEach
	void setUp() {
		inquiryRepository.deleteAll();
		userRepository.deleteAll();

		jdbcTemplate.update(
				"INSERT INTO user (user_id, user_name, user_password, authority_id, delete_flag) VALUES (?, ?, ?, ?, ?)",
				GUEST_USER_ID,
				"guest_admin_inquiry",
				passwordEncoder.encode("guestpass1"),
				1,
				0);

		User admin = new User();
		admin.setUserName("admin_inquiry_ctrl_test");
		admin.setUserPassword(passwordEncoder.encode("admin"));
		admin.setAuthorityId(1);
		admin.setDeleteFlag(0);
		userRepository.save(admin);

		Inquiry inquiry = new Inquiry();
		inquiry.setUserId(GUEST_USER_ID);
		inquiry.setInquiryCategoryId(1);
		inquiry.setInquiryDetail("管理者問い合わせテスト");
		inquiry.setInquiryStatus("未対応");
		inquiry.setAuthorityId(1);
		inquiry.setDeleteFlag(0);
		adminInquiryId = inquiryRepository.save(inquiry).getInquiryId();
	}

	@Test
	@WithMockUser(username = "admin_inquiry_ctrl_test", roles = "ADMIN")
	@DisplayName("No.10: 管理者は問い合わせ一覧画面を表示できる")
	void adminCanViewInquiryList() throws Exception {

		mockMvc.perform(get("/admin/inquiries"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/inquiry-list"))
				.andExpect(model().attributeExists("rows"));
	}

	@Test
	@WithMockUser(username = "admin_inquiry_ctrl_test", roles = "ADMIN")
	@DisplayName("No.11: 管理者はステータスで問い合わせを絞り込める")
	void adminCanFilterInquiriesByStatus() throws Exception {

		mockMvc.perform(get("/admin/inquiries").param("status", "未対応"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/inquiry-list"))
				.andExpect(model().attribute("statusFilter", "未対応"));
	}

	@Test
	@WithMockUser(username = "admin_inquiry_ctrl_test", roles = "ADMIN")
	@DisplayName("No.12: 管理者は問い合わせ詳細を表示しステータスを更新できる")
	void adminCanViewDetailAndUpdateStatus() throws Exception {

		mockMvc.perform(get("/admin/inquiries/" + adminInquiryId))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/inquiry-detail"))
				.andExpect(model().attributeExists("inquiry"));

		mockMvc.perform(post("/admin/inquiries/" + adminInquiryId + "/status")
						.with(csrf())
						.param("inquiryStatus", "対応中"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/inquiries/" + adminInquiryId));
	}

	@Test
	@WithMockUser(roles = "SHOP")
	@DisplayName("店舗ユーザーは管理者問い合わせ画面にアクセスできない")
	void shopUserCannotViewAdminInquiries() throws Exception {

		mockMvc.perform(get("/admin/inquiries"))
				.andExpect(status().isForbidden());
	}
}
