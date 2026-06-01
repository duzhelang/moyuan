-- 添加text测试用户
INSERT IGNORE INTO `user` (`username`, `password`, `email`, `nickname`, `role`, `status`) VALUES
('text', '$2b$10$7WF/nU54ij0NgOYQzcbvWOIUsfCBQ28BeEq9rc.buaUcl0zaR.9PO', 'text@moyuan.com', '文本用户', 'user', 1);