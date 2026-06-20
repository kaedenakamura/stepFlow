package com.example.stepflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.security.core.GrantedAuthority;
import java.util.List;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.repository.WarehouseRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.stepflow.service.AdminWarehouseService;


@Controller
@RequestMapping("/admin/warehouses") //管理者用/倉庫のURL共通部分
public class AdminWarehouseController {
    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private AdminWarehouseService adminWarehouseService;

      /**
     * 倉庫一覧表示（機能一覧：管理者＞倉庫管理＞倉庫一覧表示画面）
     * GET http://localhost:8080/admin/warehouses
     */
    @GetMapping("")
    public String list(
        @RequestParam(name = "address", required = false) String address,
        @AuthenticationPrincipal UserDetails userDetails,
        Model model
    ){
        //サイドバー共通処理(AdmininquiryControllerと同じ)IDを取得して画面に渡す
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .findFirst()
        .orElse("");
        model.addAttribute("authorityId",authorityId);
        model.addAttribute("addressFilter", address != null ? address : "");

        //DBから有効な倉庫だけ取得(delete_flag=0)
        List<Warehouse> warehouses;
        if (address != null && !address.isBlank()) {
            warehouses = warehouseRepository
                    .findByDeleteFlagAndWarehouseAddressContainingOrderByWarehouseIdDesc(
                            0, address.strip());
        } else {
            warehouses = warehouseRepository.findByDeleteFlagOrderByWarehouseIdDesc(0);
        }
        model.addAttribute("warehouses",warehouses);

        //warehouse-list.htmlを表示
        return "admin/warehouse-list";
     }
     /**
 * 倉庫新規登録画面（機能一覧：倉庫管理 → 新規登録画面）
 * GET http://localhost:8080/admin/warehouses/new
 */
     @GetMapping("/new")
     public String showNewForm(
        @AuthenticationPrincipal UserDetails userDetails,
        Model model){
            //サイドバー共通処理(AdmininquiryControllerと同じ)IDを取得して画面に渡す
            model.addAttribute("loginUsername", userDetails.getUsername());
            String authorityId = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("");

             model.addAttribute("authorityId", authorityId);
    // 空の Warehouse をフォームに渡す（user の new User() と同じ）
     model.addAttribute("warehouse", new Warehouse());
            return "admin/warehouse-form";//倉庫新規登録画面にリダイレクト
        }
/**
 * 倉庫編集画面（user の /users/edit/{id} と同じ）
 * GET http://localhost:8080/admin/warehouses/edit/1
 */
  @GetMapping("/edit/{id}")
  public String showEditForm(
    @PathVariable("id") Integer id,
    @AuthenticationPrincipal UserDetails userDetails,
    Model model){
        //サイドバー共通処理(AdmininquiryControllerと同じ)IDを取得して画面に渡す
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .findFirst()
        .orElse("");
        model.addAttribute("authorityId", authorityId);

        //DBから倉庫を取得(delete_flag=0)
        Warehouse warehouse = warehouseRepository.findById(id).orElse(null);
        //倉庫が存在しないか、削除されている場合は倉庫一覧画面にリダイレクト
        if(warehouse == null || !Integer.valueOf(0).equals(warehouse.getDeleteFlag())){//論理削除フラグが0でない場合はリダイレクト
            return "redirect:/admin/warehouses";
        }

        //倉庫をフォームに渡す
        model.addAttribute("warehouse",warehouse);
        return "admin/warehouse-form";//倉庫編集画面にリダイレクト
    }




        /**
 * 倉庫保存（新規 INSERT /  warehouseId ありで 編集UPDATE も同じ POST）
 * POST http://localhost:8080/admin/warehouses
 */
   @PostMapping
   public String save(
    @Validated @ModelAttribute("warehouse") Warehouse warehouse,
    BindingResult result,
    @AuthenticationPrincipal UserDetails userDetails,
    Model model,
    RedirectAttributes redirectAttributes){
        if(result.hasErrors()){
            //サイドバー共通処理(AdmininquiryControllerと同じ)IDを取得して画面に渡す
            model.addAttribute("loginUsername", userDetails.getUsername());
            String authorityId = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("");
            model.addAttribute("authorityId", authorityId);
            return "admin/warehouse-form";
        }
        
    //save()の直前に判定→メッセージの表示を分けるため
    boolean isNew =(warehouse.getWarehouseId() == null);    
    if (isNew) {
        adminWarehouseService.registerWarehouseWithInitialStock(warehouse);
    } else {
        adminWarehouseService.updateWarehouse(warehouse);
    }
    String successMessage = isNew ? "倉庫を登録しました" : "倉庫を更新しました";
   redirectAttributes.addFlashAttribute("successMessage",successMessage);
   return "redirect:/admin/warehouses";//倉庫一覧画面にリダイレクト
}
/**
 * 倉庫の論理削除（delete_flag = 1）
 * POST http://localhost:8080/admin/warehouses/delete/3
 */

  @PostMapping("/delete/{id}")
  public String delete(
    @PathVariable("id") Integer id,
    RedirectAttributes redirectAttributes){
        Warehouse warehouse = warehouseRepository.findById(id).orElse(null);
        if(warehouse != null && Integer.valueOf(0).equals(warehouse.getDeleteFlag())){
            adminWarehouseService.deleteWarehouseWithStocks(id);
            String successMessage = "倉庫を削除しました";
            redirectAttributes.addFlashAttribute("successMessage",successMessage);
        } 
        return "redirect:/admin/warehouses";
    }


}
    
    
