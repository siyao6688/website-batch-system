#!/bin/bash

echo "=== API 测试脚本 ==="
echo ""

# 检查MySQL连接
echo "1. 检查MySQL数据库连接..."
mysql -uroot -proot -e "SELECT 1;" 2>/dev/null && echo "✓ MySQL连接成功" || echo "✗ MySQL连接失败"
echo ""

# 检查后端服务
echo "2. 检查后端API (上传Excel)..."
curl -s -X POST http://localhost:8080/api/excel/upload -F "file=@test.xlsx" -o /dev/null -w "%{http_code}\n"
echo ""

# 检查获取公司列表API
echo "3. 检查获取公司列表API..."
curl -s http://localhost:8080/api/excel/companies -o /dev/null -w "%{http_code}\n"
echo ""

echo "=== 测试完成 ==="
