USE travel_assistant;

INSERT INTO `user` (username, password, role) VALUES
('demo_user', '123456', 'USER'),
('travel_show', '123456', 'USER'),
('admin_user', '123456', 'ADMIN');

INSERT INTO attraction (city, name, description, image_url, reference_count, play_time, longitude, latitude) VALUES
('北京', '故宫博物院', '故宫博物院是明清皇宫旧址，建筑群恢弘，馆藏丰厚，周边可顺路体验老北京小吃与胡同风味。', 'https://images.unsplash.com/photo-1547981609-4b6bf67db7cc?auto=format&fit=crop&w=1200&q=80', 12, 4.0, 116.397477, 39.908692),
('北京', '天坛公园', '天坛以庄重古建和松柏林景闻名，适合慢步游览，附近有铜锅涮肉与京味点心可安排。', 'https://images.unsplash.com/photo-1577709811779-c5d80dc0a0d9?auto=format&fit=crop&w=1200&q=80', 8, 2.5, 116.417312, 39.887977),
('北京', '环球影城', '北京环球影城适合全天沉浸体验，游乐设施密集，园区和周边餐饮丰富，适合家庭与年轻游客。', 'https://images.unsplash.com/photo-1513883049090-d0b7439799bf?auto=format&fit=crop&w=1200&q=80', 15, 8.0, 116.673303, 39.902809),
('成都', '武侯祠', '武侯祠兼具三国文化与川西园林气质，逛完可顺路去锦里感受成都小吃与夜色。', 'https://images.unsplash.com/photo-1528164344705-47542687000d?auto=format&fit=crop&w=1200&q=80', 10, 2.5, 104.048606, 30.641994),
('成都', '宽窄巷子', '宽窄巷子适合慢节奏漫步拍照，能集中体验盖碗茶、糖油果子和川味街头美食。', 'https://images.unsplash.com/photo-1517309230475-6736d926b979?auto=format&fit=crop&w=1200&q=80', 9, 2.0, 104.055888, 30.667911),
('杭州', '西湖', '西湖串联断桥、苏堤与雷峰塔，适合轻松漫游，附近茶点、杭帮菜和湖景餐厅选择丰富。', 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1200&q=80', 13, 4.0, 120.153576, 30.243169),
('上海', '外滩', '外滩兼具城市天际线与老建筑风貌，傍晚和夜景尤其出彩，周边本帮菜与甜品店密集。', 'https://images.unsplash.com/photo-1508057198894-247b23fe5ade?auto=format&fit=crop&w=1200&q=80', 11, 2.0, 121.490317, 31.241701),
('西安', '大唐不夜城', '大唐不夜城夜间氛围感极强，适合边逛边看演出，附近可安排肉夹馍、甑糕等陕西风味。', 'https://images.unsplash.com/photo-1549893075-7ce4c78fdb6f?auto=format&fit=crop&w=1200&q=80', 7, 3.0, 108.967055, 34.219302);

INSERT INTO itinerary (user_id, title, city) VALUES
(1, '北京3日经典人文慢游', '北京'),
(1, '成都2日美食文化行', '成都');

INSERT INTO itinerary_detail (itinerary_id, attraction_id, day_number, sort_order) VALUES
(1, 1, 1, 1),
(1, 2, 1, 2),
(1, 3, 2, 1),
(2, 4, 1, 1),
(2, 5, 1, 2);

INSERT INTO itinerary_day_plan (itinerary_id, day_number, route_summary, route_distance, route_duration) VALUES
(1, 1, '第 1 天围绕故宫与天坛展开，适合历史文化主题慢游，路线紧凑，适合上午集中参观核心景点。', '12 公里', '43 分钟'),
(1, 2, '第 2 天主打主题乐园体验，可预留更完整的全天时间。', '18 公里', '55 分钟'),
(2, 1, '第 1 天从武侯祠到宽窄巷子，适合文化与美食串联，步调轻松。', '6 公里', '28 分钟');
