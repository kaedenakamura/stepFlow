package com.example.stepflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.stepflow.repository.WarehouseRepository;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.entity.User;
import com.example.stepflow.service.UserService;
import java.util.List;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.entity.Shop;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

@Controller
public class UserController {
	// ここは「ユーザーに関する画面の処理をするクラス」です。
    @Autowired
    private UserService userService;
	@Autowired
	private WarehouseRepository warehouseRepository;
	@Autowired
	private ShopRepository shopRepository;

    // ここに「ユーザーに関する画面の処理」を書いていきます。
    @GetMapping("/users")
    public String listUsers(
		@RequestParam(name = "authorityId",required = false) Integer authorityId,
		@AuthenticationPrincipal UserDetails userDetails,
		Model model) {
			addSidebarAttributes(userDetails, model);//サイドバー用（homeControllor.java）に追加したコードをここにも追加する
            model.addAttribute("selectedAuthorityId", authorityId);

		List<User> users;
		if(authorityId != null){
			users = userService.findUsersByAuthority(authorityId);
		}else{
			 users =userService.getAllUsers();
		}

		model.addAttribute("users", users);

        return "user-list";
    }
	
    @PostMapping("/users")
    public String saveUser(
    		@Valid @ModelAttribute("user") User user,
    		BindingResult result,
    		@AuthenticationPrincipal UserDetails userDetails,
    		Model model,
    		RedirectAttributes redirectAttributes) {

    	validateUserBusinessRules(user, result);

    	if (result.hasErrors()) {
			return renderUserForm(userDetails, model);
		}
    	try {
		userService.saveUser(user);
		  redirectAttributes.addFlashAttribute("successMessage", "ユーザーが保存されました: " + user.getUserName());
		  return "redirect:/users/new";
		} catch (Exception e) {
			return renderUserFormWithError(userDetails, model, result, "userName",
					"登録に失敗しました。入力内容を確認してください");
		}
    }
    
    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable Integer id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
		addSidebarAttributes(userDetails, model);
		User user = userService.getUserById(id);
		if(user == null) {
			return "redirect:/users";
		}
		user.setUserPassword(null);
		model.addAttribute("user", user);
		addWarehouseOptions(model);
		addShopOptions(model);
		return "user-form";
	}

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Integer userId) {
		userService.deleteUser(userId);
		return "redirect:/users";
    }
    
    @GetMapping("/users/new")
    public String showCreateUserForm(@AuthenticationPrincipal UserDetails userDetails,Model model) {
    	addSidebarAttributes(userDetails, model);
		addWarehouseOptions(model);
		addShopOptions(model);
    	model.addAttribute("user", new User());
    return "user-form";
    }

	private void validateUserBusinessRules(User user, BindingResult result) {
		boolean isNew = user.getUserId() == null;
		String password = user.getUserPassword();

		if (user.getUserGender() == null) {
			result.rejectValue("userGender", "error.userGender", "性別を選択してください");
		}

		if (isNew) {
			if (password == null || password.isBlank()) {
				result.rejectValue("userPassword", "error.userPassword", "パスワードを入力してください");
			} else {
				validatePasswordFormat(password, result);
			}
		} else if (password != null && !password.isBlank()) {
			validatePasswordFormat(password, result);
		}

		if (Integer.valueOf(2).equals(user.getAuthorityId()) && user.getShopId() == null) {
			result.rejectValue("shopId", "error.shopId", "店舗スタッフの場合は店舗を選択してください");
		}

		if (Integer.valueOf(3).equals(user.getAuthorityId()) && user.getWarehouseId() == null) {
			result.rejectValue("warehouseId", "error.warehouseId", "倉庫スタッフの場合は倉庫を選択してください");
		}
	}

	private void validatePasswordFormat(String password, BindingResult result) {
		if (password.length() < 8 || password.length() > 255) {
			result.rejectValue("userPassword", "error.userPassword",
					"パスワードは8文字以上255文字以内で入力してください");
			return;
		}
		if (!password.matches("[a-zA-Z0-9]+")) {
			result.rejectValue("userPassword", "error.userPassword", "パスワードは英数字で入力してください");
		}
	}

	private String renderUserForm(UserDetails userDetails, Model model) {
		addSidebarAttributes(userDetails, model);
		addWarehouseOptions(model);
		addShopOptions(model);
		return "user-form";
	}

	private String renderUserFormWithError(
			UserDetails userDetails,
			Model model,
			BindingResult result,
			String field,
			String message) {
		result.rejectValue(field, "error." + field, message);
		return renderUserForm(userDetails, model);
	}

	private void addSidebarAttributes(UserDetails userDetails, Model model){
		model.addAttribute("loginUsername", userDetails.getUsername());
		String authorityId = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.findFirst()
				.orElse("");
		model.addAttribute("authorityId", authorityId);
	}

	private void addWarehouseOptions(Model model) {
		List<Warehouse> warehouses = warehouseRepository.findByDeleteFlagOrderByWarehouseIdDesc(0);
		model.addAttribute("warehouses", warehouses);
	}

	private void addShopOptions(Model model) {
		List<Shop> shops = shopRepository.findByDeleteFlagOrderByShopIdDesc(0);
		model.addAttribute("shops", shops);
	}
}
