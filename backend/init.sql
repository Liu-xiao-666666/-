-- ============================================
-- 天空外卖 数据库初始化脚本
-- 先创建数据库: CREATE DATABASE takeaway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- 然后执行: mysql -u root -p takeaway < init.sql
-- ============================================

CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone`       VARCHAR(20)  NOT NULL                COMMENT '手机号',
    `password`    VARCHAR(255) NOT NULL                COMMENT '密码(BCrypt加密)',
    `nickname`    VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    `avatar`      VARCHAR(500) DEFAULT NULL            COMMENT '头像URL',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态 0=禁用 1=正常',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `dish` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
    `name`        VARCHAR(100)   NOT NULL                COMMENT '菜品名称',
    `category`    VARCHAR(50)    DEFAULT NULL            COMMENT '分类(热菜/面食/饮品/小吃等)',
    `price`       DECIMAL(10,2)  NOT NULL                COMMENT '价格',
    `price_small` DECIMAL(10,2)  DEFAULT NULL            COMMENT '小份价格',
    `price_large` DECIMAL(10,2)  DEFAULT NULL            COMMENT '大份价格',
    `image`       VARCHAR(500)   DEFAULT NULL            COMMENT '图片URL',
    `description` VARCHAR(500)   DEFAULT NULL            COMMENT '描述',
    `sales`       INT            DEFAULT 0               COMMENT '销量',
    `status`      TINYINT        DEFAULT 1               COMMENT '状态 0=停售 1=在售',
    `create_time` DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

CREATE TABLE IF NOT EXISTS `cart` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
    `dish_id`     BIGINT   NOT NULL                COMMENT '菜品ID',
    `quantity`    INT      DEFAULT 1               COMMENT '数量',
    `size`        VARCHAR(10) DEFAULT 'large'       COMMENT '分量: large=大份 small=小份',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

CREATE TABLE IF NOT EXISTS `orders` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `user_id`       BIGINT         NOT NULL                COMMENT '用户ID',
    `order_no`      VARCHAR(50)    NOT NULL                COMMENT '订单编号',
    `total`         DECIMAL(10,2)  NOT NULL                COMMENT '订单总金额',
    `status`        TINYINT        DEFAULT 1               COMMENT '状态 1=待付款 2=已支付 3=配送中 4=待评价 5=已完成',
    `address`       VARCHAR(255)   DEFAULT NULL            COMMENT '配送地址',
    `phone`         VARCHAR(20)    DEFAULT NULL            COMMENT '联系电话',
    `remark`        VARCHAR(255)   DEFAULT NULL            COMMENT '备注',
    `delivery_time` DATETIME       DEFAULT NULL            COMMENT '配送时间',
    `eta`           INT            DEFAULT NULL            COMMENT '预计送达(秒)',
    `rating`        TINYINT        DEFAULT NULL            COMMENT '评分(1-5)',
    `review`            VARCHAR(500)   DEFAULT NULL            COMMENT '评价内容',
    `after_sale_status` TINYINT        DEFAULT 0               COMMENT '售后状态 0=无 1=处理中 2=已处理',
    `after_sale_reason` VARCHAR(500)   DEFAULT NULL            COMMENT '售后原因',
    `create_time`       DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_detail` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单详情ID',
    `order_id`    BIGINT         NOT NULL                COMMENT '订单ID',
    `dish_id`     BIGINT         NOT NULL                COMMENT '菜品ID',
    `dish_name`   VARCHAR(100)   NOT NULL                COMMENT '菜品名称(快照)',
    `price`       DECIMAL(10,2)  NOT NULL                COMMENT '下单时单价',
    `quantity`    INT            NOT NULL                COMMENT '数量',
    `size`        VARCHAR(10)    DEFAULT NULL            COMMENT '分量快照: large/small',
    `create_time` DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单详情表';

-- ============================================
-- 如果已有数据库，执行以下 ALTER TABLE 升级
-- ============================================
-- ALTER TABLE dish ADD COLUMN price_small DECIMAL(10,2) DEFAULT NULL COMMENT '小份价格';
-- ALTER TABLE dish ADD COLUMN price_large DECIMAL(10,2) DEFAULT NULL COMMENT '大份价格';
-- ALTER TABLE cart ADD COLUMN size VARCHAR(10) DEFAULT 'large' COMMENT '分量';
-- ALTER TABLE order_detail ADD COLUMN size VARCHAR(10) DEFAULT NULL COMMENT '分量快照';
-- ALTER TABLE orders ADD COLUMN after_sale_status TINYINT DEFAULT 0 COMMENT '售后状态';
-- ALTER TABLE orders ADD COLUMN after_sale_reason VARCHAR(500) DEFAULT NULL COMMENT '售后原因';
