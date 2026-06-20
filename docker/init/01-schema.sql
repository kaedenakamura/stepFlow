-- stepFlow 本番 Docker 初期化（docker-entrypoint-initdb.d）
-- MYSQL_DATABASE=stepflow_db に対して実行される（初回ボリューム作成時のみ）

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

-- ---------- 子テーブル ----------

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

ALTER TABLE shop_order
  ADD COLUMN IF NOT EXISTS update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS daily_order_summary (
  summary_id   INT AUTO_INCREMENT PRIMARY KEY,
  count_date   DATE NOT NULL,
  shop_id      INT NOT NULL,
  goods_id     INT NOT NULL,
  goods_amount INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_daily_summary (count_date, shop_id, goods_id)
);

-- ---------- 外部キー ----------

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

-- ---------- 本番用初期データ ----------
-- ログイン（パスワードはすべて password）
--   管理者: admin
--   店舗:   店長
--   倉庫:   倉庫長

INSERT INTO category (category_id, category_name, delete_flag) VALUES
  (1, '飲料', 0),
  (2, '食品', 0);

INSERT INTO shop (shop_id, shop_name, shop_address, delete_flag) VALUES
  (1, '東京本店', '東京都千代田区丸の内1-1', 0);

INSERT INTO warehouse (warehouse_id, warehouse_name, warehouse_address, delete_flag) VALUES
  (1, '中央倉庫', '神奈川県川崎市幸区2-2', 0);

INSERT INTO goods (goods_id, goods_name, category_id, delete_flag) VALUES
  (1, 'ミネラルウォーター 500ml', 1, 0),
  (2, '緑茶 500ml', 1, 0),
  (3, 'カップラーメン', 2, 0);

INSERT INTO relation (relation_id, shop_id, warehouse_id, delete_flag) VALUES
  (1, 1, 1, 0);

INSERT INTO shop_stock (shop_stock_id, shop_id, goods_id, stock_quantity, delete_flag) VALUES
  (1, 1, 1, 50, 0),
  (2, 1, 2, 30, 0),
  (3, 1, 3, 20, 0);

INSERT INTO warehouse_stock (warehouse_stock_id, warehouse_id, goods_id, stock_quantity, delete_flag) VALUES
  (1, 1, 1, 500, 0),
  (2, 1, 2, 300, 0),
  (3, 1, 3, 200, 0);

INSERT INTO `user` (user_id, user_name, user_password, authority_id, shop_id, warehouse_id, delete_flag) VALUES
  (1, 'admin',  '$2a$10$ILv4g75e26OWo3Qc7MBrUuNSROVbu5lIp6Ngsme41iRvtsISaO8Ra', 1, NULL, NULL, 0),
  (2, '店長',   '$2a$10$ILv4g75e26OWo3Qc7MBrUuNSROVbu5lIp6Ngsme41iRvtsISaO8Ra', 2, 1,    NULL, 0),
  (3, '倉庫長', '$2a$10$ILv4g75e26OWo3Qc7MBrUuNSROVbu5lIp6Ngsme41iRvtsISaO8Ra', 3, NULL, 1,    0);

INSERT INTO shop_order (shop_order_id, shop_id, warehouse_id, goods_id, order_quantity, order_status, order_date, update_date, delete_flag) VALUES
  (1, 1, 1, 1, 20, '準備中', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 0),
  (2, 1, 1, 2, 10, '発注済', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0);

INSERT INTO daily_order_summary (summary_id, count_date, shop_id, goods_id, goods_amount) VALUES
  (1, DATE(DATE_SUB(NOW(), INTERVAL 2 DAY)), 1, 1, 20),
  (2, DATE(DATE_SUB(NOW(), INTERVAL 1 DAY)), 1, 2, 10);

INSERT INTO inquiry (inquiry_id, user_id, inquiry_category_id, inquiry_detail, inquiry_status, authority_id, shop_id, warehouse_id, delete_flag) VALUES
  (1, 2, 1, '在庫の補充タイミングについて教えてください。', '未対応', 1, NULL, NULL, 0);
