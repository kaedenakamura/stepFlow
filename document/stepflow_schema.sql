-- ============================================================
-- stepFlow  DB 初期構築（A5:SQL 用）
-- データベース名: stepflow_db（application.properties と合わせる）
-- 実行順: 上から順に。FK はテーブル作成後に ALTER。
-- ============================================================

CREATE DATABASE IF NOT EXISTS stepflow_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE stepflow_db;

-- ---------- 親マスタ（先に作る） ----------

CREATE TABLE IF NOT EXISTS category (
  category_id   INT AUTO_INCREMENT PRIMARY KEY,
  category_name VARCHAR(255) NOT NULL,
  delete_flag   INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS shop (
  shop_id       INT AUTO_INCREMENT PRIMARY KEY,
  shop_name     VARCHAR(255) NOT NULL,
  shop_address  VARCHAR(255) NOT NULL,
  delete_flag   INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS warehouse (
  warehouse_id       INT AUTO_INCREMENT PRIMARY KEY,
  warehouse_name     VARCHAR(255) NOT NULL,
  warehouse_address  VARCHAR(255) NULL,
  delete_flag        INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS goods (
  goods_id     INT AUTO_INCREMENT PRIMARY KEY,
  goods_name   VARCHAR(255) NOT NULL,
  category_id  INT NOT NULL,
  delete_flag  INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user (
  user_id       INT AUTO_INCREMENT PRIMARY KEY,
  user_name     VARCHAR(255) NOT NULL UNIQUE,
  user_password VARCHAR(255) NOT NULL,
  authority_id  INT NOT NULL COMMENT '1=管理者 2=店舗 3=倉庫',
  user_gender   INT NULL,
  shop_id       INT NULL COMMENT '店舗スタッフの所属',
  warehouse_id  INT NULL COMMENT '倉庫スタッフの所属',
  delete_flag   INT NOT NULL DEFAULT 0
);

-- ---------- 子テーブル（在庫・連携・問い合わせ） ----------

-- ★ 店舗在庫画面が読む・更新するテーブル
CREATE TABLE IF NOT EXISTS shop_stock (
  shop_stock_id  INT AUTO_INCREMENT PRIMARY KEY,
  shop_id        INT NOT NULL,
  goods_id       INT NOT NULL,
  stock_quantity INT NOT NULL DEFAULT 0,
  delete_flag    INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS warehouse_stock (
  warehouse_stock_id INT AUTO_INCREMENT PRIMARY KEY,
  warehouse_id       INT NOT NULL,
  goods_id           INT NOT NULL,
  stock_quantity     INT NOT NULL DEFAULT 0,
  delete_flag        INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS relation (
  relation_id   INT AUTO_INCREMENT PRIMARY KEY,
  shop_id       INT NOT NULL,
  warehouse_id  INT NOT NULL,
  delete_flag   INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS inquiry (
  inquiry_id           INT AUTO_INCREMENT PRIMARY KEY,
  user_id              INT NOT NULL,
  inquiry_category_id  INT NOT NULL,
  inquiry_detail       VARCHAR(255) NOT NULL,
  inquiry_date         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  inquiry_status       VARCHAR(255) DEFAULT '未対応',
  authority_id         INT NOT NULL,
  shop_id              INT NULL,
  warehouse_id         INT NULL,
  delete_flag          INT NOT NULL DEFAULT 0
);

-- 店舗発注（発注申請）
CREATE TABLE IF NOT EXISTS shop_order (
  shop_order_id  INT AUTO_INCREMENT PRIMARY KEY,
  shop_id        INT NOT NULL,
  warehouse_id   INT NOT NULL,
  goods_id       INT NOT NULL,
  order_quantity INT NOT NULL,
  order_status   VARCHAR(255) NOT NULL,
  order_date     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_date    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delete_flag    INT NOT NULL DEFAULT 0
);

-- 既存テーブルに update_date 列が無い場合のため（再実行しやすいよう IF NOT EXISTS）
ALTER TABLE shop_order
  ADD COLUMN IF NOT EXISTS update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 日次発注サマリ（店舗ダッシュボード用）
CREATE TABLE IF NOT EXISTS daily_order_summary (
  summary_id   INT AUTO_INCREMENT PRIMARY KEY,
  count_date   DATE NOT NULL,
  shop_id      INT NOT NULL,
  goods_id     INT NOT NULL,
  goods_amount INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_daily_summary (count_date, shop_id, goods_id)
);

-- ---------- 外部キー（お手本どおり手動追加） ----------

ALTER TABLE goods
  ADD CONSTRAINT fk_goods_category
  FOREIGN KEY (category_id) REFERENCES category(category_id);

ALTER TABLE user
  ADD CONSTRAINT fk_user_shop
  FOREIGN KEY (shop_id) REFERENCES shop(shop_id);

ALTER TABLE user
  ADD CONSTRAINT fk_user_warehouse
  FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id);

ALTER TABLE shop_stock
  ADD CONSTRAINT fk_shop_stock_shop
  FOREIGN KEY (shop_id) REFERENCES shop(shop_id);

ALTER TABLE shop_stock
  ADD CONSTRAINT fk_shop_stock_goods
  FOREIGN KEY (goods_id) REFERENCES goods(goods_id);

ALTER TABLE warehouse_stock
  ADD CONSTRAINT fk_warehouse_stock_warehouse
  FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id);

ALTER TABLE warehouse_stock
  ADD CONSTRAINT fk_warehouse_stock_goods
  FOREIGN KEY (goods_id) REFERENCES goods(goods_id);

ALTER TABLE relation
  ADD CONSTRAINT fk_relation_shop
  FOREIGN KEY (shop_id) REFERENCES shop(shop_id);

ALTER TABLE relation
  ADD CONSTRAINT fk_relation_warehouse
  FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id);

ALTER TABLE inquiry
  ADD CONSTRAINT fk_inquiry_user
  FOREIGN KEY (user_id) REFERENCES user(user_id);

ALTER TABLE shop_order
  ADD CONSTRAINT fk_shop_order_shop
  FOREIGN KEY (shop_id) REFERENCES shop(shop_id);

ALTER TABLE shop_order
  ADD CONSTRAINT fk_shop_order_warehouse
  FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id);

ALTER TABLE shop_order
  ADD CONSTRAINT fk_shop_order_goods
  FOREIGN KEY (goods_id) REFERENCES goods(goods_id);

ALTER TABLE daily_order_summary
  ADD CONSTRAINT fk_daily_summary_shop
  FOREIGN KEY (shop_id) REFERENCES shop(shop_id);

ALTER TABLE daily_order_summary
  ADD CONSTRAINT fk_daily_summary_goods
  FOREIGN KEY (goods_id) REFERENCES goods(goods_id);

-- 既存 shop_order からサマリを作り直すとき（任意・再実行注意）
-- INSERT INTO daily_order_summary (count_date, shop_id, goods_id, goods_amount)
-- SELECT DATE(order_date), shop_id, goods_id, SUM(order_quantity)
-- FROM shop_order WHERE delete_flag = 0
-- GROUP BY DATE(order_date), shop_id, goods_id
-- ON DUPLICATE KEY UPDATE goods_amount = VALUES(goods_amount);

-- ---------- 動作確認用サンプル（任意） ----------
-- 店舗在庫画面を試す最低条件:
--   1) shop 1件
--   2) category + goods 1件以上
--   3) shop_stock（その店舗×商品の行）
--   4) user（authority_id=2, shop_id を設定）… パスワードは画面から登録推奨

INSERT INTO category (category_name, delete_flag) VALUES ('飲料', 0);

INSERT INTO shop (shop_name, shop_address, delete_flag)
VALUES ('テスト店舗', '東京都千代田区1-1', 0);

INSERT INTO goods (goods_name, category_id, delete_flag)
VALUES ('テスト商品A', 1, 0);

INSERT INTO shop_stock (shop_id, goods_id, stock_quantity, delete_flag)
VALUES (1, 1, 10, 0);

-- 店舗ユーザーは /users/new または管理者画面から作成し、
-- authority_id=2, shop_id=1 を設定してください。
