package com.example.stepflow.form;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
public class ShopOrderForm {
    @NotNull(message = "発注倉庫を選択してください")
    private Integer warehouseId;

    @NotNull(message = "商品を選択してください")
    private Integer goodsId;

    @NotNull(message = "発注数量を入力してください")
    @Min(value = 1 , message = "発注数量は1以上で入力してください")
    private Integer orderQuantity;
}
