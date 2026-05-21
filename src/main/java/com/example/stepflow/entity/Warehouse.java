package com.example.stepflow.entity; // このファイルの所属パッケージ
import jakarta.persistence.Column;   // DBの列名をJavaに結びつける
import jakarta.persistence.Entity;   // 「DBの1テーブル＝このクラス」という印
import jakarta.persistence.GeneratedValue; // 主キーを自動採番
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;       // 主キー列
import jakarta.persistence.Table;    // 対応テーブル名
import lombok.Data; // getter/setter を自動生成

@Entity// 「このクラスはデータベースのテーブルと対応します」という印
@Table(name = "warehouse")// 「MySQLの中の "warehouse" というテーブルを使います」という指定
@Data// 「Getter/Setterを自動で作ってください」という命令.
public class Warehouse {
    @Id// 主キーを決める
    @GeneratedValue(strategy = GenerationType.IDENTITY)//AUTO_INCREMENT
    @Column(name = "warehouse_id")
    private Integer warehouseId; // 倉庫IDを入れる箱　DBの列名と同じなら@Columnは省略できる為name=warehouseIdは省略

    @Column(name = "warehouse_name", nullable = false , length = 255)
    private String warehouseName;//倉庫名を入れる箱　DBの列名と同じなら@Columnは省略できる為name=warehouseNameは省略
    @Column(name = "warehouse_address")
    private String warehouseAddress;//倉庫住所を入れる箱　DBの列名と同じなら@Columnは省略できる為name=warehouseAddressは省略

    @Column(name = "delete_flag", nullable = false)
    private Integer deleteFlag = 0; // 0=有効。新規は常に0


}
