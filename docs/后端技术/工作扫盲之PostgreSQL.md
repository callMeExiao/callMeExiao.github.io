---
title: 工作扫盲之PostgreSQL
createTime: 2025/09/23 21:33:24
permalink: /article/k5v78e0h/
---

# 工作扫盲之PostgreSQL

## 1. 引言

作为一名后端开发者，在日常工作中我们经常接触MySQL数据库，但随着业务复杂度的提升，我开始接触到PostgreSQL这个"世界上最先进的开源关系型数据库"。初次使用PostgreSQL，我被它强大的功能特性深深震撼。

### 为什么选择PostgreSQL？

相比MySQL，PostgreSQL在以下方面表现出色：
- 🎯 **更丰富的数据类型**：原生支持JSON、数组、地理数据等
- 🚀 **更强大的查询能力**：窗口函数、CTE、全文搜索等
- 🌍 **地理信息处理**：PostGIS扩展提供专业级GIS功能
- ⚡ **高度可扩展**：丰富的扩展生态系统
- 🔒 **企业级特性**：MVCC、高级索引、复制等

### 本文概述

本文将深入介绍PostgreSQL相比MySQL的独特优势，重点讲解PostGIS地理功能，并分享实际工作中的应用场景。无论你是数据库新手还是有经验的开发者，都能从中获得有价值的知识。

## 2. PostgreSQL的独特数据类型支持 🎯

PostgreSQL最令人印象深刻的特性之一就是其丰富的数据类型支持，这为开发者提供了更大的灵活性。

### JSON/JSONB类型

PostgreSQL原生支持JSON数据类型，这在处理半结构化数据时非常有用。

#### 基本使用

```sql
-- 创建包含JSON字段的表
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    attributes JSON,
    metadata JSONB
);

-- 插入JSON数据
INSERT INTO products (name, attributes, metadata) VALUES 
('iPhone 15', 
 '{"color": "blue", "storage": "128GB", "price": 999}',
 '{"brand": "Apple", "category": "smartphone", "tags": ["5G", "premium"]}'
);
```

#### JSONB的优势

JSONB是二进制格式的JSON，支持索引和高效查询：

```sql
-- 创建JSONB索引
CREATE INDEX idx_metadata_gin ON products USING GIN (metadata);

-- 高效的JSON查询
SELECT * FROM products 
WHERE metadata @> '{"brand": "Apple"}';

-- JSON路径查询
SELECT name, metadata->'brand' as brand 
FROM products 
WHERE metadata->>'category' = 'smartphone';

-- JSON数组操作
SELECT name FROM products 
WHERE metadata->'tags' ? 'premium';
```

#### 实际应用场景

```sql
-- 电商商品属性存储
CREATE TABLE ecommerce_products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200),
    base_price DECIMAL(10,2),
    specifications JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 不同类型商品有不同属性
INSERT INTO ecommerce_products (name, base_price, specifications) VALUES 
('MacBook Pro', 1999.00, '{"screen_size": "14inch", "processor": "M3", "ram": "16GB", "storage": "512GB"}'),
('Nike Air Max', 129.99, '{"size": [7, 8, 9, 10, 11], "color": ["black", "white", "red"], "material": "mesh"}'),
('Coffee Maker', 89.99, '{"capacity": "12cups", "features": ["programmable", "auto-shutoff"], "warranty": "2years"}');

-- 灵活查询不同属性
SELECT name, specifications->'processor' as cpu 
FROM ecommerce_products 
WHERE specifications ? 'processor';

SELECT name, jsonb_array_elements_text(specifications->'color') as available_colors
FROM ecommerce_products 
WHERE specifications ? 'color';
```

### 数组类型

PostgreSQL支持任何数据类型的数组，这在很多场景下非常实用。

```sql
-- 数组字段定义
CREATE TABLE blog_posts (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200),
    tags TEXT[],
    view_counts INTEGER[],
    publish_dates DATE[]
);

-- 插入数组数据
INSERT INTO blog_posts (title, tags, view_counts) VALUES 
('PostgreSQL入门', ARRAY['数据库', 'PostgreSQL', '教程'], ARRAY[100, 150, 200]),
('React开发指南', ARRAY['前端', 'React', 'JavaScript'], ARRAY[300, 280, 320]);

-- 数组查询操作
-- 查找包含特定标签的文章
SELECT title FROM blog_posts WHERE 'PostgreSQL' = ANY(tags);

-- 查找标签数组长度
SELECT title, array_length(tags, 1) as tag_count FROM blog_posts;

-- 数组聚合
SELECT unnest(tags) as tag, COUNT(*) 
FROM blog_posts 
GROUP BY tag 
ORDER BY count DESC;
```

### 自定义数据类型

PostgreSQL允许创建自定义数据类型，提供更强的类型安全性。

#### 枚举类型

```sql
-- 创建枚举类型
CREATE TYPE order_status AS ENUM ('pending', 'processing', 'shipped', 'delivered', 'cancelled');

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    customer_name VARCHAR(100),
    status order_status DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW()
);

-- 类型安全的插入
INSERT INTO orders (customer_name, status) VALUES 
('张三', 'pending'),
('李四', 'processing');

-- 枚举值排序
SELECT * FROM orders ORDER BY status;
```

#### 复合类型

```sql
-- 创建复合类型
CREATE TYPE address_type AS (
    street VARCHAR(100),
    city VARCHAR(50),
    province VARCHAR(50),
    postal_code VARCHAR(10)
);

CREATE TABLE customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    billing_address address_type,
    shipping_address address_type
);

-- 使用复合类型
INSERT INTO customers (name, billing_address, shipping_address) VALUES 
('王五', 
 ROW('中关村大街1号', '北京', '北京市', '100080'),
 ROW('朝阳路88号', '北京', '北京市', '100020')
);

-- 查询复合类型字段
SELECT name, (billing_address).city, (shipping_address).city 
FROM customers;
```

## 3. 高级查询功能 🚀

PostgreSQL提供了许多MySQL不具备的高级查询功能，让复杂的数据分析变得简单。

### 窗口函数（Window Functions）

窗口函数是PostgreSQL的杀手级功能，可以在不使用GROUP BY的情况下进行聚合计算。

#### 排名函数

```sql
-- 创建销售数据表
CREATE TABLE sales (
    id SERIAL PRIMARY KEY,
    salesperson VARCHAR(50),
    region VARCHAR(50),
    amount DECIMAL(10,2),
    sale_date DATE
);

INSERT INTO sales (salesperson, region, amount, sale_date) VALUES 
('张三', '华北', 15000, '2024-01-15'),
('李四', '华北', 12000, '2024-01-16'),
('王五', '华南', 18000, '2024-01-17'),
('赵六', '华南', 16000, '2024-01-18'),
('钱七', '华北', 14000, '2024-01-19'),
('孙八', '华南', 20000, '2024-01-20');

-- 使用窗口函数进行排名
SELECT 
    salesperson,
    region,
    amount,
    ROW_NUMBER() OVER (ORDER BY amount DESC) as overall_rank,
    RANK() OVER (PARTITION BY region ORDER BY amount DESC) as region_rank,
    DENSE_RANK() OVER (PARTITION BY region ORDER BY amount DESC) as dense_region_rank
FROM sales;
```

#### LAG和LEAD函数

```sql
-- 计算销售额环比增长
SELECT 
    salesperson,
    sale_date,
    amount,
    LAG(amount) OVER (PARTITION BY salesperson ORDER BY sale_date) as prev_amount,
    amount - LAG(amount) OVER (PARTITION BY salesperson ORDER BY sale_date) as growth,
    LEAD(amount) OVER (PARTITION BY salesperson ORDER BY sale_date) as next_amount
FROM sales
ORDER BY salesperson, sale_date;
```

#### 移动平均和累计统计

```sql
-- 计算移动平均和累计销售额
SELECT 
    sale_date,
    amount,
    AVG(amount) OVER (ORDER BY sale_date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) as moving_avg_3days,
    SUM(amount) OVER (ORDER BY sale_date) as cumulative_sales,
    COUNT(*) OVER (ORDER BY sale_date) as cumulative_count
FROM sales
ORDER BY sale_date;
```

### 公用表表达式（CTE）

CTE让复杂查询更加清晰和可维护。

#### 基本CTE

```sql
-- 使用CTE简化复杂查询
WITH regional_stats AS (
    SELECT 
        region,
        COUNT(*) as sale_count,
        SUM(amount) as total_amount,
        AVG(amount) as avg_amount
    FROM sales
    GROUP BY region
),
top_performers AS (
    SELECT 
        salesperson,
        SUM(amount) as total_sales
    FROM sales
    GROUP BY salesperson
    HAVING SUM(amount) > 15000
)
SELECT 
    rs.region,
    rs.total_amount,
    rs.avg_amount,
    COUNT(tp.salesperson) as top_performer_count
FROM regional_stats rs
LEFT JOIN sales s ON rs.region = s.region
LEFT JOIN top_performers tp ON s.salesperson = tp.salesperson
GROUP BY rs.region, rs.total_amount, rs.avg_amount;
```

#### 递归CTE

```sql
-- 组织架构递归查询
CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50),
    manager_id INTEGER REFERENCES employees(id),
    department VARCHAR(50)
);

INSERT INTO employees (name, manager_id, department) VALUES 
('CEO张总', NULL, '管理层'),
('CTO李总', 1, '技术部'),
('CFO王总', 1, '财务部'),
('开发经理赵经理', 2, '技术部'),
('高级开发钱工', 4, '技术部'),
('初级开发孙工', 4, '技术部');

-- 递归查询组织层级
WITH RECURSIVE org_hierarchy AS (
    -- 基础查询：找到顶级管理者
    SELECT id, name, manager_id, department, 0 as level, name as path
    FROM employees 
    WHERE manager_id IS NULL
    
    UNION ALL
    
    -- 递归查询：找到下级员工
    SELECT e.id, e.name, e.manager_id, e.department, oh.level + 1,
           oh.path || ' -> ' || e.name
    FROM employees e
    INNER JOIN org_hierarchy oh ON e.manager_id = oh.id
)
SELECT 
    REPEAT('  ', level) || name as hierarchy,
    department,
    level,
    path
FROM org_hierarchy
ORDER BY path;
```

### 全文搜索

PostgreSQL内置强大的全文搜索功能。

```sql
-- 创建文章表
CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200),
    content TEXT,
    author VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO articles (title, content, author) VALUES 
('PostgreSQL高级特性', 'PostgreSQL是一个功能强大的开源关系型数据库，支持JSON、数组、地理数据等多种数据类型...', '张三'),
('MySQL vs PostgreSQL', '在选择数据库时，MySQL和PostgreSQL都是优秀的选择，但它们各有特点...', '李四'),
('PostGIS地理数据处理', 'PostGIS是PostgreSQL的地理信息扩展，提供了丰富的空间数据处理功能...', '王五');

-- 添加全文搜索索引
ALTER TABLE articles ADD COLUMN search_vector tsvector;
UPDATE articles SET search_vector = to_tsvector('chinese', title || ' ' || content);
CREATE INDEX idx_articles_search ON articles USING GIN(search_vector);

-- 全文搜索查询
SELECT title, author,
       ts_rank(search_vector, query) as rank
FROM articles, 
     to_tsquery('chinese', 'PostgreSQL & 数据库') query
WHERE search_vector @@ query
ORDER BY rank DESC;

-- 搜索结果高亮
SELECT title,
       ts_headline('chinese', content, to_tsquery('chinese', 'PostgreSQL'), 
                  'MaxWords=20, MinWords=5') as snippet
FROM articles
WHERE search_vector @@ to_tsquery('chinese', 'PostgreSQL');
```

## 4. PostGIS：地理信息系统的王者 🌍

PostGIS是PostgreSQL最引人注目的扩展之一，它将PostgreSQL转变为一个功能强大的空间数据库。这是PostgreSQL相比MySQL最大的优势之一。

### PostGIS简介

PostGIS是一个开源的地理信息系统扩展，为PostgreSQL提供了空间数据类型、空间索引和空间函数。它符合OGC（开放地理空间联盟）标准，被广泛应用于GIS应用、位置服务、地图应用等领域。

#### 安装和配置

```sql
-- 启用PostGIS扩展
CREATE EXTENSION postgis;

-- 查看PostGIS版本
SELECT PostGIS_Version();

-- 查看支持的空间参考系统
SELECT * FROM spatial_ref_sys LIMIT 5;
```

### 空间数据类型

PostGIS支持多种空间数据类型，每种类型都有其特定的用途。

#### 基本几何类型

```sql
-- 创建包含空间数据的表
CREATE TABLE locations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    location GEOMETRY(POINT, 4326),  -- WGS84坐标系的点
    area GEOMETRY(POLYGON, 4326),    -- 多边形区域
    route GEOMETRY(LINESTRING, 4326) -- 线路
);

-- 插入空间数据
INSERT INTO locations (name, location, area) VALUES 
('北京天安门', ST_GeomFromText('POINT(116.3974 39.9093)', 4326), NULL),
('上海外滩', ST_GeomFromText('POINT(121.4944 31.2407)', 4326), NULL),
('深圳市中心区域', ST_GeomFromText('POINT(114.0579 22.5431)', 4326), 
 ST_GeomFromText('POLYGON((114.05 22.54, 114.06 22.54, 114.06 22.53, 114.05 22.53, 114.05 22.54))', 4326));
```

#### 复杂几何类型

```sql
-- 多点、多线、多面
CREATE TABLE complex_geometries (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    multi_points GEOMETRY(MULTIPOINT, 4326),
    multi_lines GEOMETRY(MULTILINESTRING, 4326),
    multi_polygons GEOMETRY(MULTIPOLYGON, 4326),
    collection GEOMETRY(GEOMETRYCOLLECTION, 4326)
);

-- 插入复杂几何数据
INSERT INTO complex_geometries (name, multi_points, multi_lines) VALUES 
('连锁店位置', 
 ST_GeomFromText('MULTIPOINT((116.3974 39.9093), (121.4944 31.2407), (114.0579 22.5431))', 4326),
 ST_GeomFromText('MULTILINESTRING((116.39 39.90, 116.40 39.91), (121.49 31.24, 121.50 31.25))', 4326)
);
```

### 核心地理函数详解

PostGIS提供了数百个空间函数，这里介绍最常用和最重要的函数。

#### 几何创建函数

```sql
-- ST_MakePoint：创建点几何
SELECT ST_MakePoint(116.3974, 39.9093) as point_geom;

-- ST_GeomFromText：从WKT文本创建几何
SELECT ST_GeomFromText('POINT(116.3974 39.9093)', 4326) as point_from_text;

-- ST_Buffer：创建缓冲区
SELECT ST_Buffer(ST_GeomFromText('POINT(116.3974 39.9093)', 4326), 0.01) as buffer_area;

-- ST_MakeLine：创建线几何
SELECT ST_MakeLine(
    ST_MakePoint(116.3974, 39.9093),
    ST_MakePoint(121.4944, 31.2407)
) as line_geom;

-- ST_MakePolygon：创建多边形
SELECT ST_MakePolygon(
    ST_GeomFromText('LINESTRING(116.39 39.90, 116.40 39.90, 116.40 39.91, 116.39 39.91, 116.39 39.90)')
) as polygon_geom;
```

#### 空间关系函数

这些函数用于判断几何对象之间的空间关系：

```sql
-- 创建测试数据
CREATE TABLE spatial_test AS
SELECT 
    'point1' as name, ST_GeomFromText('POINT(116.40 39.90)', 4326) as geom
UNION ALL
SELECT 
    'point2', ST_GeomFromText('POINT(116.41 39.91)', 4326)
UNION ALL
SELECT 
    'polygon1', ST_GeomFromText('POLYGON((116.39 39.89, 116.42 39.89, 116.42 39.92, 116.39 39.92, 116.39 39.89))', 4326)
UNION ALL
SELECT 
    'line1', ST_GeomFromText('LINESTRING(116.38 39.88, 116.43 39.93)', 4326);

-- ST_Contains：判断是否包含
SELECT 
    a.name as container,
    b.name as contained,
    ST_Contains(a.geom, b.geom) as contains
FROM spatial_test a, spatial_test b
WHERE a.name = 'polygon1' AND b.name LIKE 'point%';

-- ST_Intersects：判断是否相交
SELECT 
    a.name,
    b.name,
    ST_Intersects(a.geom, b.geom) as intersects
FROM spatial_test a, spatial_test b
WHERE a.name != b.name;

-- ST_Within：判断是否在内部
SELECT 
    a.name as inner_geom,
    b.name as outer_geom,
    ST_Within(a.geom, b.geom) as within
FROM spatial_test a, spatial_test b
WHERE a.name LIKE 'point%' AND b.name = 'polygon1';

-- ST_Distance：计算距离（单位：度）
SELECT 
    a.name,
    b.name,
    ST_Distance(a.geom, b.geom) as distance_degrees,
    ST_Distance(ST_Transform(a.geom, 3857), ST_Transform(b.geom, 3857)) as distance_meters
FROM spatial_test a, spatial_test b
WHERE a.name = 'point1' AND b.name = 'point2';
```

#### 几何分析函数

```sql
-- ST_Area：计算面积
SELECT 
    name,
    ST_Area(geom) as area_degrees,
    ST_Area(ST_Transform(geom, 3857)) as area_square_meters
FROM spatial_test 
WHERE name = 'polygon1';

-- ST_Length：计算长度
SELECT 
    name,
    ST_Length(geom) as length_degrees,
    ST_Length(ST_Transform(geom, 3857)) as length_meters
FROM spatial_test 
WHERE name = 'line1';

-- ST_Centroid：计算中心点
SELECT 
    name,
    ST_AsText(ST_Centroid(geom)) as centroid
FROM spatial_test 
WHERE name = 'polygon1';

-- ST_Envelope：计算边界框
SELECT 
    name,
    ST_AsText(ST_Envelope(geom)) as bounding_box
FROM spatial_test;

-- ST_ConvexHull：计算凸包
SELECT ST_AsText(ST_ConvexHull(ST_Collect(geom))) as convex_hull
FROM spatial_test;
```

#### 坐标系统函数

```sql
-- ST_Transform：坐标系转换
-- 从WGS84 (4326) 转换到Web Mercator (3857)
SELECT 
    ST_AsText(geom) as original_wgs84,
    ST_AsText(ST_Transform(geom, 3857)) as transformed_web_mercator
FROM spatial_test 
WHERE name = 'point1';

-- ST_SetSRID：设置空间参考系统ID
SELECT ST_SetSRID(ST_MakePoint(116.3974, 39.9093), 4326) as point_with_srid;

-- ST_SRID：获取空间参考系统ID
SELECT name, ST_SRID(geom) as srid
FROM spatial_test;
```

### 实际应用场景

#### 1. 附近商家查询

```sql
-- 创建商家表
CREATE TABLE businesses (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    category VARCHAR(50),
    location GEOMETRY(POINT, 4326),
    rating DECIMAL(2,1)
);

-- 创建空间索引
CREATE INDEX idx_businesses_location ON businesses USING GIST (location);

-- 插入测试数据
INSERT INTO businesses (name, category, location, rating) VALUES 
('星巴克(国贸店)', '咖啡', ST_GeomFromText('POINT(116.4074 39.9093)', 4326), 4.5),
('麦当劳(建国门店)', '快餐', ST_GeomFromText('POINT(116.4174 39.9093)', 4326), 4.2),
('海底捞(王府井店)', '火锅', ST_GeomFromText('POINT(116.4074 39.9193)', 4326), 4.8),
('肯德基(东单店)', '快餐', ST_GeomFromText('POINT(116.4174 39.9193)', 4326), 4.1);

-- 查找用户位置1公里内的商家
WITH user_location AS (
    SELECT ST_GeomFromText('POINT(116.4074 39.9143)', 4326) as location
)
SELECT 
    b.name,
    b.category,
    b.rating,
    ST_Distance(ST_Transform(b.location, 3857), ST_Transform(ul.location, 3857)) as distance_meters
FROM businesses b, user_location ul
WHERE ST_DWithin(ST_Transform(b.location, 3857), ST_Transform(ul.location, 3857), 1000)
ORDER BY distance_meters;
```

#### 2. 地理围栏

```sql
-- 创建地理围栏表
CREATE TABLE geofences (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    boundary GEOMETRY(POLYGON, 4326),
    fence_type VARCHAR(50)
);

-- 插入围栏数据
INSERT INTO geofences (name, boundary, fence_type) VALUES 
('北京二环内', 
 ST_GeomFromText('POLYGON((116.368 39.915, 116.427 39.915, 116.427 39.875, 116.368 39.875, 116.368 39.915))', 4326),
 'restricted_zone'),
('配送范围', 
 ST_GeomFromText('POLYGON((116.390 39.900, 116.420 39.900, 116.420 39.920, 116.390 39.920, 116.390 39.900))', 4326),
 'delivery_zone');

-- 检查用户是否在围栏内
CREATE OR REPLACE FUNCTION check_geofence(user_lat DECIMAL, user_lng DECIMAL)
RETURNS TABLE(fence_name VARCHAR, is_inside BOOLEAN, fence_type VARCHAR) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        g.name,
        ST_Contains(g.boundary, ST_GeomFromText('POINT(' || user_lng || ' ' || user_lat || ')', 4326)),
        g.fence_type
    FROM geofences g;
END;
$$ LANGUAGE plpgsql;

-- 使用函数检查位置
SELECT * FROM check_geofence(39.9093, 116.4074);
```

#### 3. 路径规划和距离计算

```sql
-- 创建路径点表
CREATE TABLE route_points (
    id SERIAL PRIMARY KEY,
    route_id INTEGER,
    sequence_order INTEGER,
    location GEOMETRY(POINT, 4326),
    address VARCHAR(200)
);

-- 插入路径数据
INSERT INTO route_points (route_id, sequence_order, location, address) VALUES 
(1, 1, ST_GeomFromText('POINT(116.3974 39.9093)', 4326), '天安门'),
(1, 2, ST_GeomFromText('POINT(116.4074 39.9193)', 4326), '王府井'),
(1, 3, ST_GeomFromText('POINT(116.4174 39.9093)', 4326), '建国门'),
(1, 4, ST_GeomFromText('POINT(116.4074 39.8993)', 4326), '前门');

-- 计算路径总长度
WITH route_segments AS (
    SELECT 
        route_id,
        sequence_order,
        location,
        LAG(location) OVER (PARTITION BY route_id ORDER BY sequence_order) as prev_location
    FROM route_points
)
SELECT 
    route_id,
    SUM(ST_Distance(ST_Transform(location, 3857), ST_Transform(prev_location, 3857))) as total_distance_meters
FROM route_segments
WHERE prev_location IS NOT NULL
GROUP BY route_id;

-- 创建路径线几何
SELECT 
    route_id,
    ST_AsText(ST_MakeLine(location ORDER BY sequence_order)) as route_line
FROM route_points
GROUP BY route_id;
```

#### 4. 热力图分析

```sql
-- 创建事件表（如外卖订单）
CREATE TABLE delivery_orders (
    id SERIAL PRIMARY KEY,
    order_time TIMESTAMP,
    delivery_location GEOMETRY(POINT, 4326),
    order_value DECIMAL(10,2)
);

-- 插入模拟数据
INSERT INTO delivery_orders (order_time, delivery_location, order_value) VALUES 
('2024-01-15 12:30:00', ST_GeomFromText('POINT(116.4074 39.9093)', 4326), 85.50),
('2024-01-15 12:35:00', ST_GeomFromText('POINT(116.4084 39.9103)', 4326), 92.30),
('2024-01-15 12:40:00', ST_GeomFromText('POINT(116.4064 39.9083)', 4326), 76.80),
('2024-01-15 18:30:00', ST_GeomFromText('POINT(116.4174 39.9193)', 4326), 156.20),
('2024-01-15 18:35:00', ST_GeomFromText('POINT(116.4184 39.9183)', 4326), 134.70);

-- 创建网格进行热力图分析
WITH grid AS (
    SELECT 
        i, j,
        ST_MakeEnvelope(
            116.40 + (i * 0.01), 
            39.90 + (j * 0.01),
            116.40 + ((i + 1) * 0.01), 
            39.90 + ((j + 1) * 0.01),
            4326
        ) as grid_cell
    FROM generate_series(0, 2) i,
         generate_series(0, 2) j
)
SELECT 
    g.i, g.j,
    COUNT(o.id) as order_count,
    COALESCE(SUM(o.order_value), 0) as total_value,
    ST_AsText(ST_Centroid(g.grid_cell)) as grid_center
FROM grid g
LEFT JOIN delivery_orders o ON ST_Contains(g.grid_cell, o.delivery_location)
GROUP BY g.i, g.j, g.grid_cell
ORDER BY order_count DESC;
```

## 5. 扩展生态系统

PostgreSQL的扩展机制是其最强大的特性之一，允许用户轻松添加新功能而无需修改核心代码。

### pg_stat_statements（性能监控）

这个扩展提供了查询性能统计信息，是数据库性能调优的利器。

```sql
-- 启用扩展
CREATE EXTENSION pg_stat_statements;

-- 查看最耗时的查询
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements 
ORDER BY total_time DESC 
LIMIT 10;

-- 查看最频繁执行的查询
SELECT 
    query,
    calls,
    total_time,
    mean_time
FROM pg_stat_statements 
ORDER BY calls DESC 
LIMIT 10;

-- 重置统计信息
SELECT pg_stat_statements_reset();
```

### pg_trgm（模糊匹配）

提供三元组（trigram）相似度匹配，支持模糊搜索和相似度查询。

```sql
-- 启用扩展
CREATE EXTENSION pg_trgm;

-- 创建测试表
CREATE TABLE products_search (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200),
    description TEXT
);

INSERT INTO products_search (name, description) VALUES 
('iPhone 15 Pro Max', 'Apple最新旗舰手机，配备A17 Pro芯片'),
('Samsung Galaxy S24', '三星最新Android旗舰，拍照功能强大'),
('MacBook Pro M3', 'Apple专业级笔记本电脑'),
('ThinkPad X1 Carbon', '联想商务笔记本电脑');

-- 创建GIN索引支持模糊搜索
CREATE INDEX idx_products_name_gin ON products_search USING GIN (name gin_trgm_ops);
CREATE INDEX idx_products_desc_gin ON products_search USING GIN (description gin_trgm_ops);

-- 相似度搜索
SELECT 
    name,
    similarity(name, 'iphone') as name_similarity,
    similarity(description, '手机') as desc_similarity
FROM products_search
WHERE name % 'iphone' OR description % '手机'
ORDER BY greatest(similarity(name, 'iphone'), similarity(description, '手机')) DESC;

-- 模糊匹配查询
SELECT name, description
FROM products_search
WHERE name ILIKE '%phone%' OR description ILIKE '%手机%';
```

### uuid-ossp（UUID生成）

提供多种UUID生成函数，在分布式系统中非常有用。

```sql
-- 启用扩展
CREATE EXTENSION "uuid-ossp";

-- 生成不同类型的UUID
SELECT 
    uuid_generate_v1() as uuid_v1,    -- 基于时间戳和MAC地址
    uuid_generate_v4() as uuid_v4;    -- 随机UUID

-- 在表中使用UUID作为主键
CREATE TABLE distributed_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO distributed_users (username, email) VALUES 
('user1', 'user1@example.com'),
('user2', 'user2@example.com');

SELECT * FROM distributed_users;
```

## 6. 高级索引类型

PostgreSQL支持多种高级索引类型，每种都针对特定的查询模式进行了优化。

### GIN索引（倒排索引）

GIN（Generalized Inverted Index）索引特别适合包含多个值的数据类型，如数组、JSONB、全文搜索等。

```sql
-- 为JSONB字段创建GIN索引
CREATE TABLE user_profiles (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50),
    profile JSONB,
    tags TEXT[]
);

INSERT INTO user_profiles (username, profile, tags) VALUES 
('alice', '{"age": 25, "city": "北京", "interests": ["编程", "音乐", "旅行"]}', ARRAY['developer', 'music-lover']),
('bob', '{"age": 30, "city": "上海", "interests": ["摄影", "运动"]}', ARRAY['photographer', 'athlete']),
('charlie', '{"age": 28, "city": "深圳", "interests": ["编程", "游戏"]}', ARRAY['developer', 'gamer']);

-- 创建GIN索引
CREATE INDEX idx_profile_gin ON user_profiles USING GIN (profile);
CREATE INDEX idx_tags_gin ON user_profiles USING GIN (tags);

-- 高效的JSONB查询
EXPLAIN (ANALYZE, BUFFERS) 
SELECT username FROM user_profiles 
WHERE profile @> '{"interests": ["编程"]}';

-- 数组查询
SELECT username FROM user_profiles 
WHERE tags && ARRAY['developer'];

-- JSON路径查询
SELECT username, profile->'city' as city
FROM user_profiles 
WHERE profile->>'city' = '北京';
```

### GiST索引（通用搜索树）

GiST（Generalized Search Tree）索引支持多种数据类型，特别适合几何数据和范围查询。

```sql
-- 为几何数据创建GiST索引（PostGIS）
CREATE INDEX idx_businesses_gist ON businesses USING GIST (location);

-- 为范围类型创建GiST索引
CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    room_id INTEGER,
    time_range TSRANGE,
    guest_name VARCHAR(100)
);

INSERT INTO reservations (room_id, time_range, guest_name) VALUES 
(101, '[2024-01-15 14:00, 2024-01-15 16:00)', '张三'),
(102, '[2024-01-15 15:00, 2024-01-15 17:00)', '李四'),
(101, '[2024-01-15 18:00, 2024-01-15 20:00)', '王五');

CREATE INDEX idx_reservations_time ON reservations USING GIST (time_range);

-- 查找时间冲突的预订
SELECT * FROM reservations r1
WHERE EXISTS (
    SELECT 1 FROM reservations r2 
    WHERE r1.room_id = r2.room_id 
    AND r1.id != r2.id 
    AND r1.time_range && r2.time_range
);

-- 查找特定时间段的可用房间
SELECT DISTINCT room_id 
FROM reservations 
WHERE NOT time_range && '[2024-01-15 15:30, 2024-01-15 16:30)'::tsrange;
```

### BRIN索引（块范围索引）

BRIN（Block Range Index）索引适合大表中有序或半有序的数据，占用空间小，维护成本低。

```sql
-- 创建时序数据表
CREATE TABLE sensor_data (
    id BIGSERIAL,
    sensor_id INTEGER,
    timestamp TIMESTAMP,
    temperature DECIMAL(5,2),
    humidity DECIMAL(5,2)
);

-- 插入大量时序数据
INSERT INTO sensor_data (sensor_id, timestamp, temperature, humidity)
SELECT 
    (random() * 100)::INTEGER,
    '2024-01-01'::timestamp + (i || ' seconds')::interval,
    20 + (random() * 15),
    40 + (random() * 30)
FROM generate_series(1, 1000000) i;

-- 创建BRIN索引
CREATE INDEX idx_sensor_timestamp_brin ON sensor_data USING BRIN (timestamp);
CREATE INDEX idx_sensor_temp_brin ON sensor_data USING BRIN (temperature);

-- 范围查询性能测试
EXPLAIN (ANALYZE, BUFFERS)
SELECT AVG(temperature), AVG(humidity)
FROM sensor_data 
WHERE timestamp BETWEEN '2024-01-01 10:00:00' AND '2024-01-01 12:00:00';

-- 比较索引大小
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexname::regclass)) as index_size
FROM pg_indexes 
WHERE tablename = 'sensor_data';
```

## 7. 并发控制MVCC

PostgreSQL使用多版本并发控制（MVCC），这是其相比MySQL的重要优势之一。

### MVCC原理

MVCC允许读操作不阻塞写操作，写操作也不阻塞读操作，大大提高了并发性能。

```sql
-- 创建测试表
CREATE TABLE account_balance (
    id SERIAL PRIMARY KEY,
    account_id VARCHAR(20),
    balance DECIMAL(15,2),
    last_updated TIMESTAMP DEFAULT NOW()
);

INSERT INTO account_balance (account_id, balance) VALUES 
('ACC001', 1000.00),
('ACC002', 2000.00);

-- 演示事务隔离
-- 会话1：开始事务但不提交
BEGIN;
UPDATE account_balance SET balance = balance - 100 WHERE account_id = 'ACC001';
-- 此时不要COMMIT

-- 会话2：在另一个连接中查询
SELECT * FROM account_balance WHERE account_id = 'ACC001';
-- 仍然看到原始值1000.00，因为会话1的事务未提交

-- 会话1：提交事务
COMMIT;

-- 会话2：再次查询
SELECT * FROM account_balance WHERE account_id = 'ACC001';
-- 现在看到更新后的值900.00
```

### 事务隔离级别

```sql
-- 查看当前隔离级别
SHOW transaction_isolation;

-- 设置不同的隔离级别
BEGIN ISOLATION LEVEL READ COMMITTED;
-- 或者
BEGIN ISOLATION LEVEL REPEATABLE READ;
-- 或者
BEGIN ISOLATION LEVEL SERIALIZABLE;

-- 演示可重复读
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT balance FROM account_balance WHERE account_id = 'ACC001';
-- 即使其他事务修改了数据，在当前事务中多次读取会得到相同结果
SELECT balance FROM account_balance WHERE account_id = 'ACC001';
COMMIT;
```

### 死锁检测和处理

```sql
-- PostgreSQL自动检测死锁并回滚其中一个事务
-- 创建死锁场景的示例

-- 会话1
BEGIN;
UPDATE account_balance SET balance = balance - 50 WHERE account_id = 'ACC001';
-- 等待一会儿，然后执行：
UPDATE account_balance SET balance = balance + 50 WHERE account_id = 'ACC002';

-- 会话2（同时执行）
BEGIN;
UPDATE account_balance SET balance = balance - 30 WHERE account_id = 'ACC002';
-- 然后执行：
UPDATE account_balance SET balance = balance + 30 WHERE account_id = 'ACC001';
-- PostgreSQL会检测到死锁并自动回滚其中一个事务
```

## 8. 分区表支持

PostgreSQL提供了强大的表分区功能，可以显著提高大表的查询性能。

### 声明式分区

#### 范围分区

```sql
-- 创建按日期范围分区的表
CREATE TABLE sales_data (
    id BIGSERIAL,
    sale_date DATE NOT NULL,
    product_id INTEGER,
    amount DECIMAL(10,2),
    customer_id INTEGER
) PARTITION BY RANGE (sale_date);

-- 创建分区
CREATE TABLE sales_2024_q1 PARTITION OF sales_data
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE sales_2024_q2 PARTITION OF sales_data
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

CREATE TABLE sales_2024_q3 PARTITION OF sales_data
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');

CREATE TABLE sales_2024_q4 PARTITION OF sales_data
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

-- 插入数据会自动路由到正确的分区
INSERT INTO sales_data (sale_date, product_id, amount, customer_id) VALUES 
('2024-02-15', 101, 299.99, 1001),
('2024-05-20', 102, 199.99, 1002),
('2024-08-10', 103, 399.99, 1003),
('2024-11-25', 104, 499.99, 1004);

-- 查询会自动使用分区裁剪
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM sales_data 
WHERE sale_date BETWEEN '2024-02-01' AND '2024-02-28';
```

#### 列表分区

```sql
-- 按地区分区
CREATE TABLE customer_data (
    id BIGSERIAL,
    customer_name VARCHAR(100),
    region VARCHAR(20) NOT NULL,
    registration_date DATE,
    status VARCHAR(20)
) PARTITION BY LIST (region);

-- 创建地区分区
CREATE TABLE customer_data_north PARTITION OF customer_data
    FOR VALUES IN ('华北', '东北', '西北');

CREATE TABLE customer_data_south PARTITION OF customer_data
    FOR VALUES IN ('华南', '华中', '西南');

CREATE TABLE customer_data_east PARTITION OF customer_data
    FOR VALUES IN ('华东');

-- 插入数据
INSERT INTO customer_data (customer_name, region, registration_date, status) VALUES 
('张三', '华北', '2024-01-15', 'active'),
('李四', '华南', '2024-01-20', 'active'),
('王五', '华东', '2024-01-25', 'inactive');
```

#### 哈希分区

```sql
-- 按用户ID哈希分区
CREATE TABLE user_activities (
    id BIGSERIAL,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50),
    activity_time TIMESTAMP,
    details JSONB
) PARTITION BY HASH (user_id);

-- 创建哈希分区
CREATE TABLE user_activities_0 PARTITION OF user_activities
    FOR VALUES WITH (modulus 4, remainder 0);

CREATE TABLE user_activities_1 PARTITION OF user_activities
    FOR VALUES WITH (modulus 4, remainder 1);

CREATE TABLE user_activities_2 PARTITION OF user_activities
    FOR VALUES WITH (modulus 4, remainder 2);

CREATE TABLE user_activities_3 PARTITION OF user_activities
    FOR VALUES WITH (modulus 4, remainder 3);
```

## 9. 外部数据包装器（FDW）

FDW允许PostgreSQL访问外部数据源，就像访问本地表一样。

### 连接其他数据库

```sql
-- 安装并启用postgres_fdw扩展
CREATE EXTENSION postgres_fdw;

-- 创建外部服务器
CREATE SERVER remote_pg_server
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (host 'remote-host.example.com', port '5432', dbname 'remote_db');

-- 创建用户映射
CREATE USER MAPPING FOR current_user
    SERVER remote_pg_server
    OPTIONS (user 'remote_user', password 'remote_password');

-- 创建外部表
CREATE FOREIGN TABLE remote_users (
    id INTEGER,
    username VARCHAR(50),
    email VARCHAR(100),
    created_at TIMESTAMP
) SERVER remote_pg_server
OPTIONS (schema_name 'public', table_name 'users');

-- 查询外部表
SELECT * FROM remote_users WHERE created_at > '2024-01-01';

-- 本地表和外部表的联合查询
SELECT 
    l.order_id,
    l.amount,
    r.username,
    r.email
FROM local_orders l
JOIN remote_users r ON l.user_id = r.id
WHERE l.order_date > '2024-01-01';
```

### 文件系统访问

```sql
-- 启用file_fdw扩展
CREATE EXTENSION file_fdw;

-- 创建文件服务器
CREATE SERVER file_server FOREIGN DATA WRAPPER file_fdw;

-- 创建外部表读取CSV文件
CREATE FOREIGN TABLE csv_import (
    id INTEGER,
    name VARCHAR(100),
    category VARCHAR(50),
    price DECIMAL(10,2)
) SERVER file_server
OPTIONS (filename '/path/to/products.csv', format 'csv', header 'true');

-- 查询CSV数据
SELECT category, AVG(price) as avg_price
FROM csv_import
GROUP BY category;

-- 将CSV数据导入到本地表
INSERT INTO products (name, category, price)
SELECT name, category, price FROM csv_import;
```

## 10. 存储过程和函数

PostgreSQL支持多种编程语言编写存储过程和函数。

### PL/pgSQL

```sql
-- 创建复杂的存储过程
CREATE OR REPLACE FUNCTION calculate_customer_stats(customer_id_param INTEGER)
RETURNS TABLE(
    total_orders INTEGER,
    total_amount DECIMAL(15,2),
    avg_order_amount DECIMAL(15,2),
    last_order_date DATE,
    customer_tier VARCHAR(20)
) AS $$
DECLARE
    order_count INTEGER;
    total_spent DECIMAL(15,2);
BEGIN
    -- 计算订单统计
    SELECT COUNT(*), COALESCE(SUM(amount), 0)
    INTO order_count, total_spent
    FROM orders 
    WHERE customer_id = customer_id_param;
    
    -- 返回结果
    RETURN QUERY
    SELECT 
        order_count,
        total_spent,
        CASE WHEN order_count > 0 THEN total_spent / order_count ELSE 0 END,
        (SELECT MAX(order_date) FROM orders WHERE customer_id = customer_id_param),
        CASE 
            WHEN total_spent > 10000 THEN 'VIP'
            WHEN total_spent > 5000 THEN 'Gold'
            WHEN total_spent > 1000 THEN 'Silver'
            ELSE 'Bronze'
        END;
END;
$$ LANGUAGE plpgsql;

-- 使用函数
SELECT * FROM calculate_customer_stats(1001);
```

### 触发器高级应用

```sql
-- 创建审计表
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(50),
    operation VARCHAR(10),
    old_data JSONB,
    new_data JSONB,
    changed_by VARCHAR(50),
    changed_at TIMESTAMP DEFAULT NOW()
);

-- 创建通用审计触发器函数
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO audit_log (table_name, operation, old_data, changed_by)
        VALUES (TG_TABLE_NAME, TG_OP, row_to_json(OLD), current_user);
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_log (table_name, operation, old_data, new_data, changed_by)
        VALUES (TG_TABLE_NAME, TG_OP, row_to_json(OLD), row_to_json(NEW), current_user);
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO audit_log (table_name, operation, new_data, changed_by)
        VALUES (TG_TABLE_NAME, TG_OP, row_to_json(NEW), current_user);
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 为表添加审计触发器
CREATE TRIGGER audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON account_balance
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- 测试审计功能
UPDATE account_balance SET balance = balance + 100 WHERE account_id = 'ACC001';
DELETE FROM account_balance WHERE account_id = 'ACC002';

-- 查看审计日志
SELECT * FROM audit_log ORDER BY changed_at DESC;
```

## 11. 性能优化

PostgreSQL提供了丰富的性能优化工具和技术。

### 查询计划分析

```sql
-- 使用EXPLAIN分析查询计划
EXPLAIN (ANALYZE, BUFFERS, VERBOSE) 
SELECT 
    c.customer_name,
    COUNT(o.id) as order_count,
    SUM(o.amount) as total_amount
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id
WHERE c.registration_date > '2024-01-01'
GROUP BY c.id, c.customer_name
HAVING COUNT(o.id) > 5
ORDER BY total_amount DESC;

-- 查看查询执行统计
SELECT 
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch,
    n_tup_ins,
    n_tup_upd,
    n_tup_del
FROM pg_stat_user_tables
WHERE tablename IN ('customers', 'orders');
```

### 索引优化策略

```sql
-- 创建复合索引
CREATE INDEX idx_orders_customer_date ON orders (customer_id, order_date);
CREATE INDEX idx_orders_amount_date ON orders (amount, order_date) WHERE amount > 100;

-- 部分索引（条件索引）
CREATE INDEX idx_active_customers ON customers (registration_date) 
WHERE status = 'active';

-- 表达式索引
CREATE INDEX idx_customers_lower_email ON customers (LOWER(email));
CREATE INDEX idx_orders_year_month ON orders (EXTRACT(YEAR FROM order_date), EXTRACT(MONTH FROM order_date));

-- 查看索引使用情况
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- 查找未使用的索引
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexname::regclass)) as index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
AND schemaname = 'public';
```

### 配置优化

```sql
-- 查看重要的配置参数
SELECT name, setting, unit, context 
FROM pg_settings 
WHERE name IN (
    'shared_buffers',
    'effective_cache_size',
    'work_mem',
    'maintenance_work_mem',
    'checkpoint_completion_target',
    'wal_buffers',
    'default_statistics_target'
);

-- 查看数据库统计信息
SELECT 
    datname,
    numbackends,
    xact_commit,
    xact_rollback,
    blks_read,
    blks_hit,
    temp_files,
    temp_bytes,
    deadlocks
FROM pg_stat_database 
WHERE datname = current_database();

-- 缓存命中率分析
SELECT 
    'Buffer Cache Hit Rate' as metric,
    ROUND(
        100.0 * sum(blks_hit) / (sum(blks_hit) + sum(blks_read)), 2
    ) as percentage
FROM pg_stat_database
UNION ALL
SELECT 
    'Index Cache Hit Rate' as metric,
    ROUND(
        100.0 * sum(idx_blks_hit) / nullif(sum(idx_blks_hit) + sum(idx_blks_read), 0), 2
    ) as percentage
FROM pg_statio_user_indexes;
```

### VACUUM和ANALYZE优化

```sql
-- 手动VACUUM和ANALYZE
VACUUM ANALYZE customers;
VACUUM ANALYZE orders;

-- 查看表的膨胀情况
SELECT 
    schemaname,
    tablename,
    n_dead_tup,
    n_live_tup,
    ROUND(100 * n_dead_tup / (n_live_tup + n_dead_tup), 2) as dead_tuple_percent,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE n_dead_tup > 0
ORDER BY dead_tuple_percent DESC;

-- 配置自动VACUUM
-- 在postgresql.conf中设置：
-- autovacuum = on
-- autovacuum_vacuum_threshold = 50
-- autovacuum_vacuum_scale_factor = 0.2
-- autovacuum_analyze_threshold = 50
-- autovacuum_analyze_scale_factor = 0.1
```

## 12. 企业级特性

### 流复制和高可用

```sql
-- 主服务器配置
-- 在postgresql.conf中：
-- wal_level = replica
-- max_wal_senders = 3
-- wal_keep_segments = 64
-- archive_mode = on
-- archive_command = 'cp %p /path/to/archive/%f'

-- 创建复制用户
CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'replica_password';

-- 在pg_hba.conf中添加：
-- host replication replicator slave_ip/32 md5

-- 查看复制状态
SELECT 
    client_addr,
    state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn,
    write_lag,
    flush_lag,
    replay_lag
FROM pg_stat_replication;

-- 查看WAL状态
SELECT 
    pg_current_wal_lsn(),
    pg_wal_lsn_diff(pg_current_wal_lsn(), '0/0') as wal_bytes;
```

### 备份和恢复

```sql
-- 逻辑备份
-- pg_dump -h localhost -U postgres -d mydb > backup.sql
-- pg_dump -h localhost -U postgres -d mydb -t customers > customers_backup.sql

-- 物理备份（基础备份）
-- pg_basebackup -h localhost -D /backup/base -U replicator -v -P -W

-- 时间点恢复（PITR）
-- 在recovery.conf中：
-- restore_command = 'cp /path/to/archive/%f %p'
-- recovery_target_time = '2024-01-15 14:30:00'

-- 查看备份信息
SELECT 
    pg_start_backup('manual_backup', false, false);
-- 执行文件系统备份
SELECT pg_stop_backup(false, true);
```

### 连接池和负载均衡

```sql
-- 查看当前连接
SELECT 
    datname,
    usename,
    client_addr,
    state,
    query_start,
    state_change,
    query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY query_start;

-- 连接统计
SELECT 
    datname,
    COUNT(*) as connection_count,
    COUNT(*) FILTER (WHERE state = 'active') as active_connections,
    COUNT(*) FILTER (WHERE state = 'idle') as idle_connections
FROM pg_stat_activity
GROUP BY datname;

-- 长时间运行的查询
SELECT 
    pid,
    usename,
    datname,
    query_start,
    now() - query_start as duration,
    state,
    query
FROM pg_stat_activity
WHERE state != 'idle'
AND now() - query_start > interval '5 minutes'
ORDER BY duration DESC;
```

## 13. 实际案例研究

### 案例1：电商订单系统

```sql
-- 创建电商订单系统的核心表结构
CREATE TABLE ecommerce_products (
    id SERIAL PRIMARY KEY,
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    category_path LTREE,  -- 使用ltree扩展支持层级分类
    price DECIMAL(10,2),
    inventory_count INTEGER,
    attributes JSONB,
    search_vector TSVECTOR,  -- 全文搜索
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE ecommerce_orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id INTEGER,
    status VARCHAR(20) DEFAULT 'pending',
    total_amount DECIMAL(15,2),
    shipping_address JSONB,
    order_date TIMESTAMP DEFAULT NOW(),
    shipped_date TIMESTAMP,
    delivered_date TIMESTAMP
) PARTITION BY RANGE (order_date);

-- 创建按月分区
CREATE TABLE ecommerce_orders_2024_01 PARTITION OF ecommerce_orders
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE ecommerce_orders_2024_02 PARTITION OF ecommerce_orders
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE ecommerce_order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES ecommerce_orders(id),
    product_id INTEGER REFERENCES ecommerce_products(id),
    quantity INTEGER,
    unit_price DECIMAL(10,2),
    total_price DECIMAL(10,2)
);

-- 启用必要的扩展
CREATE EXTENSION ltree;
CREATE EXTENSION pg_trgm;

-- 创建优化索引
CREATE INDEX idx_products_category ON ecommerce_products USING GIST (category_path);
CREATE INDEX idx_products_search ON ecommerce_products USING GIN (search_vector);
CREATE INDEX idx_products_attributes ON ecommerce_products USING GIN (attributes);
CREATE INDEX idx_orders_customer_date ON ecommerce_orders (customer_id, order_date);
CREATE INDEX idx_orders_status ON ecommerce_orders (status) WHERE status != 'delivered';

-- 插入示例数据
INSERT INTO ecommerce_products (sku, name, category_path, price, inventory_count, attributes) VALUES 
('PHONE001', 'iPhone 15 Pro', 'electronics.mobile.smartphones', 999.99, 50, 
 '{"brand": "Apple", "color": "Natural Titanium", "storage": "128GB", "features": ["Face ID", "5G", "ProRAW"]}'),
('LAPTOP001', 'MacBook Pro M3', 'electronics.computers.laptops', 1999.99, 25,
 '{"brand": "Apple", "screen": "14-inch", "processor": "M3", "memory": "16GB", "storage": "512GB SSD"}'),
('BOOK001', 'PostgreSQL权威指南', 'books.technology.databases', 89.99, 100,
 '{"author": "PostgreSQL专家", "pages": 800, "language": "中文", "format": "精装"}');

-- 更新搜索向量
UPDATE ecommerce_products SET search_vector = 
    to_tsvector('chinese', name || ' ' || COALESCE(attributes->>'brand', ''));

-- 复杂查询示例：商品推荐系统
WITH customer_preferences AS (
    SELECT 
        o.customer_id,
        p.category_path,
        COUNT(*) as purchase_count,
        AVG(oi.unit_price) as avg_price
    FROM ecommerce_orders o
    JOIN ecommerce_order_items oi ON o.id = oi.order_id
    JOIN ecommerce_products p ON oi.product_id = p.id
    WHERE o.order_date > NOW() - INTERVAL '6 months'
    GROUP BY o.customer_id, p.category_path
),
similar_products AS (
    SELECT 
        p.*,
        similarity(p.name, '手机') as name_similarity
    FROM ecommerce_products p
    WHERE p.search_vector @@ to_tsquery('chinese', '手机 | 电话')
    OR p.name % '手机'
)
SELECT 
    sp.name,
    sp.price,
    sp.category_path,
    sp.attributes,
    sp.name_similarity
FROM similar_products sp
WHERE sp.inventory_count > 0
ORDER BY sp.name_similarity DESC, sp.price ASC
LIMIT 10;
```

### 案例2：地理位置服务

```sql
-- 基于PostGIS的位置服务系统
CREATE TABLE location_businesses (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200),
    category VARCHAR(100),
    address TEXT,
    location GEOMETRY(POINT, 4326),  -- WGS84坐标系
    rating DECIMAL(3,2),
    price_level INTEGER,  -- 1-4价格等级
    opening_hours JSONB,
    contact_info JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE location_users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50),
    current_location GEOMETRY(POINT, 4326),
    home_location GEOMETRY(POINT, 4326),
    preferences JSONB,
    last_active TIMESTAMP DEFAULT NOW()
);

-- 创建空间索引
CREATE INDEX idx_businesses_location ON location_businesses USING GIST (location);
CREATE INDEX idx_users_current_location ON location_users USING GIST (current_location);

-- 插入示例数据（北京地区）
INSERT INTO location_businesses (name, category, address, location, rating, price_level, opening_hours) VALUES 
('星巴克(三里屯店)', '咖啡厅', '北京市朝阳区三里屯路19号', ST_SetSRID(ST_MakePoint(116.4551, 39.9365), 4326), 4.2, 3,
 '{"monday": "06:30-22:00", "tuesday": "06:30-22:00", "sunday": "07:00-21:00"}'),
('全聚德(前门店)', '餐厅', '北京市东城区前门大街30号', ST_SetSRID(ST_MakePoint(116.3967, 39.9015), 4326), 4.0, 4,
 '{"monday": "11:00-21:00", "tuesday": "11:00-21:00", "sunday": "11:00-21:00"}'),
('北京大学', '教育', '北京市海淀区颐和园路5号', ST_SetSRID(ST_MakePoint(116.3105, 39.9926), 4326), 4.8, 1,
 '{"monday": "全天开放", "sunday": "全天开放"}');

INSERT INTO location_users (username, current_location, home_location, preferences) VALUES 
('user1', ST_SetSRID(ST_MakePoint(116.4074, 39.9042), 4326), ST_SetSRID(ST_MakePoint(116.4074, 39.9042), 4326),
 '{"favorite_categories": ["咖啡厅", "书店"], "max_distance": 2000, "price_preference": [1,2,3]}');

-- 附近商家查询（2公里范围内）
SELECT 
    b.name,
    b.category,
    b.rating,
    b.price_level,
    ST_Distance(
        ST_Transform(b.location, 3857),  -- 转换为米制坐标系
        ST_Transform(u.current_location, 3857)
    ) as distance_meters
FROM location_businesses b
CROSS JOIN location_users u
WHERE u.username = 'user1'
AND ST_DWithin(
    ST_Transform(b.location, 3857),
    ST_Transform(u.current_location, 3857),
    2000  -- 2公里
)
ORDER BY distance_meters ASC;

-- 路径规划查询
WITH route_points AS (
    SELECT 
        ST_SetSRID(ST_MakePoint(116.4074, 39.9042), 4326) as start_point,  -- 天安门
        ST_SetSRID(ST_MakePoint(116.3105, 39.9926), 4326) as end_point     -- 北大
)
SELECT 
    b.name,
    b.category,
    ST_Distance(
        ST_Transform(b.location, 3857),
        ST_Transform(ST_MakeLine(rp.start_point, rp.end_point), 3857)
    ) as distance_to_route
FROM location_businesses b
CROSS JOIN route_points rp
WHERE ST_DWithin(
    ST_Transform(b.location, 3857),
    ST_Transform(ST_MakeLine(rp.start_point, rp.end_point), 3857),
    500  -- 距离路线500米内
)
ORDER BY distance_to_route ASC;

-- 热力图数据生成
SELECT 
    ST_X(location) as longitude,
    ST_Y(location) as latitude,
    COUNT(*) as business_count,
    AVG(rating) as avg_rating
FROM location_businesses
WHERE ST_Within(
    location,
    ST_MakeEnvelope(116.3, 39.8, 116.5, 40.0, 4326)  -- 北京市区范围
)
GROUP BY ST_SnapToGrid(location, 0.01)  -- 按0.01度网格聚合
HAVING COUNT(*) > 0
ORDER BY business_count DESC;
```

## 总结

通过这次PostgreSQL的深度探索，我们可以看到它相比MySQL的诸多优势：

1. **数据类型丰富**：原生支持JSON/JSONB、数组、自定义类型等
2. **查询功能强大**：窗口函数、CTE、全文搜索等高级特性
3. **PostGIS地理信息**：业界最强的地理信息系统支持
4. **扩展生态丰富**：pg_stat_statements、pg_trgm、uuid-ossp等实用扩展
5. **索引类型多样**：GIN、GiST、BRIN等针对不同场景的索引
6. **并发控制先进**：MVCC机制提供更好的并发性能
7. **分区表支持**：声明式分区简化大表管理
8. **外部数据访问**：FDW实现数据联邦查询
9. **存储过程灵活**：支持多种编程语言
10. **企业级特性**：流复制、PITR、高可用等

PostgreSQL不仅仅是一个关系型数据库，更是一个强大的数据平台。它的这些"大杀招"功能让它在处理复杂业务场景时游刃有余，特别是在需要地理信息处理、复杂数据分析、高并发读写的现代应用中表现出色。

对于开发者来说，掌握PostgreSQL的这些特性，不仅能提高开发效率，还能为系统架构提供更多可能性。在选择数据库技术栈时，PostgreSQL绝对值得认真考虑。

