package com.example.stepflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.stepflow.entity.User;
import com.example.stepflow.service.UserService;

@Controller
public class UserController {
	// ここは「ユーザーに関する画面の処理をするクラス」です。
    @Autowired
    private UserService userService;
    // ここに「ユーザーに関する画面の処理」を書いていきます。
    @GetMapping("/users")
    public String listUsers(
		@AuthenticationPrincipal UserDetails userDetails,
		Model model) {
			addSidebarAttributes(userDetails, model);//サイドバー用（homeControllor.java）に追加したコードをここにも追加する
    	// ここで「ユーザー全員の情報をデータベースから取ってきて、画面に渡す」という処理をします。
        var users = userService.getAllUsers();
        // ここで「ユーザー全員の情報をデータベースから取ってきて、画面に渡す」という処理をします。
        model.addAttribute("users", users);

        return "user-list";// ここで「ユーザー全員の情報をデータベースから取ってきて、画面に渡す」という処理をします。
    }
	    
	   
    // @PostMapping は、HTMLの formタグの method="post" と対応します
    @PostMapping("/users")
    public String saveUser(@Validated @ModelAttribute("user") User user ,BindingResult result ,@AuthenticationPrincipal UserDetails userDetails,Model model, RedirectAttributes redirectAttributes) {
    	// @Validated は「このUserオブジェクトの中身を、Userクラスで定義したバリデーションルールに従ってチェックしてください」という命令です。
    	// BindingResultは、バリデーションの結果を受け取るため
    	
    	// 今ログインしている人の情報を「認証情報の箱」から取り出す
    	//Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    	
    	// CustomUserDetailServiceで作った「カード」をUserクラスとして受け取る
    	//UserDetails carrentUser =(UserDetails)authentication.getPrincipal();
    	// ここで「今ログインしている人の情報を取り出す」という処理をします。
    	//userService.findUserByName(carrentUser.getUsername());
    	//↑上の3つの処理により、ログインしている人のみ↓の実行をするような処理となる。
    			
    	//主導のバリデーションチェック（パスワード英数字チェック）
    	if(user.getUserPassword() != null && !user.getUserPassword().matches("[a-zA-Z0-9]+$")) {
    		// ここで「パスワードが英数字でない場合のエラー処理」をします。
    		result.rejectValue("userpassword","error.userpassword","英数字で入力してください");
    	}
    	
    	//ルール：店舗スタッフはSHOP、倉庫スタッフはWAREHOUSE、の場合は、入力必須だが、空の場合のエラー処理
    	// 店舗スタッフ（２）店舗ID、倉庫スタッフ（３）は倉庫IDが必要
    	//ID番号はマスターデータの登録状況によって変わるため、ユーザーの権限IDが「SHOP」または「WAREHOUSE」の場合に、所属（店舗IDや倉庫ID）が空でないかをチェックする処理を追加します。
    	if(Integer.valueOf(2).equals(user.getAuthorityId()) && user.getShopId() == null ){
    		result.rejectValue("shopId","error.shopId","店舗スタッフの場合は店舗を選択してください");
    	}
    	
    	if(Integer.valueOf(3).equals(user.getAuthorityId()) && user.getWarehouseId() == null) {
				// ここで「店舗スタッフまたは倉庫スタッフで、所属が空の場合のエラー処理」をします。
				// 例えば、result.rejectValue() を使って、特定のフィールドにエラーメッセージを追加することができます。
				result.rejectValue("warehouseId", "error.warehouseId", "倉庫スタッフの場合は所属を入力してください");
			}
    	if (result.hasErrors()) {
    		// バリデーションエラーがある場合は、ユーザー新規登録の画面に戻す
			addSidebarAttributes(userDetails, model);
			return "user-form";
		}
    	try {
    	// ここで「ユーザーの情報をデータベースに保存する」という処理をします。
    	// ここでIDがあればUPDATE、なければINSERTになります
		userService.saveUser(user);
		  redirectAttributes.addFlashAttribute("successMessage", "ユーザーが保存されました: " + user.getUserName());
		//でバック
			System.out.println("ユーザーが保存されました: " + user);
		  return "redirect:/users/new";//ユーザー新規登録の画面にリダイレクトして、成功メッセージを表示するための処理
		} catch (Exception e) {
			// ここで「ユーザーの情報をデータベースに保存する際のエラー処理」をします。
			// 例えば、ユーザー名が重複している場合などのエラーを
			//キャッチして、エラーメッセージを追加することができます。
			addSidebarAttributes(userDetails,model);//サイドバー用（homeControllor.java）に追加したコードをここにも追加する
			result.rejectValue("userName", "error.userName","登録に失敗しました。入力内容を確認してください");
			return "user-form"; // ユーザー新規登録の画面に戻す
		}
    }
    
    
    
    
    @GetMapping("/users/edit/{id}")
    //pathVariableはURLの一部を変数として受け取るためのアノテーションで、{id}の部分が変数になります。
    public String showEditUserForm(@PathVariable Integer id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
		addSidebarAttributes(userDetails, model);//サイドバー用（homeControllor.java）に追加したコードをここにも追加する
		// ここで「ユーザーの情報をデータベースから取ってきて、ユーザー編集の画面に渡す」という処理をします。
    	// IDを元にDBから既存のユーザー情報を取ってくる
			//ログイン中のユーザー名を取得して画面に渡す
		User user = userService.getUserById(id);
		if(user == null) {
			// ユーザーが存在しない、または削除されている場合は、ユーザー一覧画面にリダイレクトするなどの処理をします。
			return "redirect:/users";//ユーザー一覧画面にリダイレクト
		}
		
		model.addAttribute("user", user);
		return "user-form";
		//新規登録と同じフォームを使うため、user-formを返す
	}
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Integer UserId) {
    	// ここで「ユーザーの情報をデータベースから削除する」という処理をします。
		userService.deleteUser(UserId);
		return "redirect:/users"; // ユーザー一覧画面にリダイレクト
    }
    
    
    
    @GetMapping("/users/new")
    public String showCreateUserForm(@AuthenticationPrincipal UserDetails userDetails,Model model) {
		addSidebarAttributes(userDetails, model);//サイドバー用（homeControllor.java）に追加したコードをここにも追加する
		// ここで「ユーザー新規登録の画面を表示する」という処理をします。
		// 例えば、空のUserオブジェクトを作って、それをモデルに入れて、フォームで使えるようにするなどの処理です。
		// model.addAttribute("user", new User());
		// return "user-form";

	    //今ログインしている人の情報を「認証情報の箱」から取り出す
	    //Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	    
	    // CustomUserDetailServiceで作った「カード」をUserクラスとして受け取る
	    //UserDetails currentUser =(UserDetails)authentication.getPrincipal();
	    
	    // ここで「今ログインしている人の情報を取り出す」という処理をします。
	    //User loggedInUser = userService.findUserByName(currentUser.getUsername());
	    
	    // ここで「今ログインしている人の権限IDを取り出す」という処理をします。
    	//model.addAttribute("authorityId",loggedInUser.getAuthorityId());
    	//↑上の４つの処理により、ログインしている人のみ↓の実行をするような処理となる。
    	
    	//新規登録用のオブジェクト
    	model.addAttribute("user", new User());
    	
    return "user-form";
    }
	/**サイドバー用（homeControllor.java）に追加したコードをここにも追加する */
	private void addSidebarAttributes(UserDetails userDetails, Model model){
		model.addAttribute("loginUsername", userDetails.getUsername());
		String authorityId = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.findFirst()
				.orElse("");
		model.addAttribute("authorityId", authorityId);
	}
   
}
    
    