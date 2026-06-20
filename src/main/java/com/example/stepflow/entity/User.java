package com.example.stepflow.entity;

// ↓これらは「Spring Bootの標準機能（JPA）」を借りるための宣言です
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ↓これは「コードを楽に書くためのツール（Lombok）」を借りる宣言です
import lombok.Data;

@Entity // 「このクラスはデータベースのテーブルと対応します」という印
@Table(name = "user") // 「MySQLの中の "user" というテーブルを使います」という指定
@Data // 「Getter/Setterを自動で作ってください」という命令.
public class User {

    @Id //主キーを決める
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 番号は自動でプラス1
    @Column(name = "user_id") // 「MySQL側では user_id という列名ですよ」という指定
    private Integer userId; // Java側での変数名

    
    @Column(name = "user_name" ,nullable = false, unique = true , length = 255 ) //名前は一意（unique）かつ255文字以内で、空（null）を許さない（nullable = false）という指定
    private String userName;// 名前を入れる箱　DBの列名と同じなら@Columnは省略できる為name=nameは省略")
    
    @Column(name="user_password", nullable = false)
    @Size(min = 8 , max = 255 , message = "パスワードは8文字以上２５５文字いないで入力してください　") // パスワードは8文字以上255文字以下であるべきという指定
    //@Pattern(regexp = "^[a-zA-Z0-9]+$", message = "英数字で入力してください") // パスワードは英数字の必要があるという指定
    //↑ここでかけるservice層でとパスワードハッシュ化しているため、
    @NotBlank(message = "パスワードは必須項目です")
    private String userPassword; // パスワードを入れる箱　DBの列名と同じなら@Columnは省略できる為name=passwordは省略
    
    @Column(name = "authority_id" , nullable = false) // 「権限ID」の列　ユーザーの権限を表すための列になります。例えば、管理者（ADMIN）1や店舗スタッフ（SHOP）2、倉庫スタッフ（WAREHOUSE）3などの区別をするために使用されます。
    private Integer authorityId; // 権限IDを入れる箱　DBの列名と同じなら@Columnは省略できる為name=authorityIdは省略
    
    @Column(name = "user_gender")// 「性別」の列　1:男性, 2:女性, 0:その他など
    private Integer userGender; // 性別を入れる箱　DBの列名と同じなら@Column は省略できる為name=genderは省略　1:男性, 2:女性, 0:その他など
    
    /** 店舗スタッフの所属（FK → shop.shop_id）。管理者は null */
    @Column(name = "shop_id")
    private Integer shopId;

    /** 倉庫スタッフの所属（FK → warehouse.warehouse_id）。管理者は null */
    @Column(name = "warehouse_id")
    private Integer warehouseId;
    
    //isDeletedは論理削除のためのフラグで、trueなら削除されたとみなす。
    @Column(name = "delete_flag", nullable = false)// 「削除フラグ」の列　論理削除のためのフラグで、trueなら削除されたとみなす。
    private  Integer deleteFlag = 0  ; // 論理削除のフラグを入れる箱
}



