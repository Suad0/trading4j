#!/bin/bash

# Stochastic XLSTM API Testing Script with Authentication
# This script tests all endpoints with proper basic auth

echo "🧪 Testing Stochastic XLSTM API Endpoints (with Authentication)"
echo "============================================================="

BASE_URL="http://localhost:8080/api/ml/stochastic-xlstm"
AUTH="-u admin:password"  # Basic auth from application.yml

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo -e "\n${BLUE}Testing: $description${NC}"
    echo "Endpoint: $method $endpoint"
    
    if [ -n "$data" ]; then
        echo "Data: $data"
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X $method \
            $AUTH \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$endpoint")
    else
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X $method $AUTH "$endpoint")
    fi
    
    http_code=$(echo $response | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
    body=$(echo $response | sed -e 's/HTTPSTATUS\:.*//g')
    
    if [ $http_code -ge 200 ] && [ $http_code -lt 300 ]; then
        echo -e "${GREEN}✅ SUCCESS (HTTP $http_code)${NC}"
        echo "Response: $body" | jq . 2>/dev/null || echo "Response: $body"
    else
        echo -e "${RED}❌ FAILED (HTTP $http_code)${NC}"
        echo "Response: $body"
    fi
}

echo -e "\n${YELLOW}🔍 1. Health Check${NC}"
test_endpoint "GET" "$BASE_URL/health" "" "Health check endpoint"

echo -e "\n${YELLOW}📊 2. Model Status${NC}"
test_endpoint "GET" "$BASE_URL/status" "" "Get model status and configuration"

echo -e "\n${YELLOW}⚙️ 3. Model Configuration${NC}"
test_endpoint "GET" "$BASE_URL/configuration" "" "Get model configuration details"

echo -e "\n${YELLOW}📈 4. Model Metrics${NC}"
test_endpoint "GET" "$BASE_URL/metrics" "" "Get model performance metrics"

echo -e "\n${YELLOW}🎯 5. Training Data (Sample)${NC}"
training_data='[
  {
    "symbol": "AAPL",
    "price": 150.50,
    "volume": 1000000,
    "high": 151.20,
    "low": 149.80,
    "open": 150.00,
    "timestamp": "2024-01-15T10:00:00"
  },
  {
    "symbol": "AAPL",
    "price": 151.00,
    "volume": 1100000,
    "high": 152.00,
    "low": 150.30,
    "open": 150.50,
    "timestamp": "2024-01-15T11:00:00"
  },
  {
    "symbol": "AAPL",
    "price": 150.75,
    "volume": 950000,
    "high": 151.50,
    "low": 150.20,
    "open": 151.00,
    "timestamp": "2024-01-15T12:00:00"
  }
]'

test_endpoint "POST" "$BASE_URL/train" "$training_data" "Train model with sample data"

echo -e "\n${YELLOW}🔮 6. Single Prediction${NC}"
prediction_data='{
  "symbol": "AAPL",
  "price": 150.25,
  "volume": 1200000,
  "high": 150.80,
  "low": 149.90,
  "open": 150.10,
  "timestamp": "2024-01-15T13:00:00"
}'

test_endpoint "POST" "$BASE_URL/predict" "$prediction_data" "Get single prediction"

echo -e "\n${YELLOW}🎲 7. Batch Predictions${NC}"
batch_data='[
  {
    "symbol": "AAPL",
    "price": 150.25,
    "volume": 1200000,
    "high": 150.80,
    "low": 149.90,
    "open": 150.10,
    "timestamp": "2024-01-15T13:00:00"
  },
  {
    "symbol": "GOOGL",
    "price": 2750.50,
    "volume": 800000,
    "high": 2760.00,
    "low": 2740.00,
    "open": 2745.00,
    "timestamp": "2024-01-15T13:00:00"
  },
  {
    "symbol": "MSFT",
    "price": 375.25,
    "volume": 1500000,
    "high": 376.50,
    "low": 374.00,
    "open": 375.00,
    "timestamp": "2024-01-15T13:00:00"
  }
]'

test_endpoint "POST" "$BASE_URL/predict/batch" "$batch_data" "Get batch predictions"

echo -e "\n${YELLOW}📝 8. Model Update${NC}"
update_data='{
  "marketData": {
    "symbol": "AAPL",
    "price": 151.50,
    "volume": 1300000,
    "high": 152.00,
    "low": 150.50,
    "open": 150.75,
    "timestamp": "2024-01-15T14:00:00"
  },
  "actualOutcome": 0.015
}'

test_endpoint "POST" "$BASE_URL/update" "$update_data" "Update model with actual outcome"

echo -e "\n${YELLOW}🔄 9. Retrain Model${NC}"
retrain_data='[
  {
    "symbol": "AAPL",
    "price": 150.50,
    "volume": 1000000,
    "high": 151.20,
    "low": 149.80,
    "open": 150.00,
    "timestamp": "2024-01-15T10:00:00"
  },
  {
    "symbol": "AAPL",
    "price": 151.00,
    "volume": 1100000,
    "high": 152.00,
    "low": 150.30,
    "open": 150.50,
    "timestamp": "2024-01-15T11:00:00"
  },
  {
    "symbol": "AAPL",
    "price": 150.75,
    "volume": 950000,
    "high": 151.50,
    "low": 150.20,
    "open": 151.00,
    "timestamp": "2024-01-15T12:00:00"
  },
  {
    "symbol": "AAPL",
    "price": 151.50,
    "volume": 1300000,
    "high": 152.00,
    "low": 150.50,
    "open": 150.75,
    "timestamp": "2024-01-15T14:00:00"
  }
]'

test_endpoint "POST" "$BASE_URL/retrain" "$retrain_data" "Retrain model with extended data"

echo -e "\n${YELLOW}✅ 10. Final Status Check${NC}"
test_endpoint "GET" "$BASE_URL/status" "" "Final model status after training"

echo -e "\n${GREEN}🎉 API Testing Complete!${NC}"
echo "============================================================="
echo ""
echo "📋 Individual curl commands you can run manually:"
echo ""
echo "# 1. Health Check"
echo "curl -u admin:password http://localhost:8080/api/ml/stochastic-xlstm/health"
echo ""
echo "# 2. Model Status"
echo "curl -u admin:password http://localhost:8080/api/ml/stochastic-xlstm/status"
echo ""
echo "# 3. Single Prediction"
echo 'curl -u admin:password -H "Content-Type: application/json" \'
echo '  -d '\''{"symbol":"AAPL","price":150.25,"volume":1200000,"high":150.80,"low":149.90,"open":150.10,"timestamp":"2024-01-15T13:00:00"}'\'' \'
echo '  http://localhost:8080/api/ml/stochastic-xlstm/predict'
echo ""