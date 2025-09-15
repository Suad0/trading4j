#!/bin/bash

# ================================
# Alpaca API Test Script
# ================================

# Set your credentials here (or export them in your shell environment)
API_KEY="PK2FV4QQB5CFN177WXN4"
API_SECRET="RMVjdudR03eQV026EXvrZzAAzFJuXUh0oubO6JOW"

# Use paper trading by default
BASE_URL="https://paper-api.alpaca.markets"
DATA_URL="https://data.alpaca.markets"

# --------------------------------
# Functions
# --------------------------------

# Check account details
get_account() {
  echo "🔎 Fetching account info..."
  curl -s -X GET "$BASE_URL/v2/account" \
    -H "APCA-API-KEY-ID: $API_KEY" \
    -H "APCA-API-SECRET-KEY: $API_SECRET" | jq .
}

# Check market clock
get_clock() {
  echo "🕒 Checking market clock..."
  curl -s -X GET "$BASE_URL/v2/clock" \
    -H "APCA-API-KEY-ID: $API_KEY" \
    -H "APCA-API-SECRET-KEY: $API_SECRET" | jq .
}

# Get latest quote for a stock (default AAPL)
get_quote() {
  SYMBOL=${1:-AAPL}
  echo "💹 Getting latest quote for $SYMBOL..."
  curl -s -X GET "$DATA_URL/v2/stocks/$SYMBOL/quotes/latest" \
    -H "APCA-API-KEY-ID: $API_KEY" \
    -H "APCA-API-SECRET-KEY: $API_SECRET" | jq .
}

# Place a paper order (default: buy 1 share AAPL)
place_order() {
  SYMBOL=${1:-AAPL}
  QTY=${2:-1}
  echo "📝 Placing order: BUY $QTY of $SYMBOL..."
  curl -s -X POST "$BASE_URL/v2/orders" \
    -H "APCA-API-KEY-ID: $API_KEY" \
    -H "APCA-API-SECRET-KEY: $API_SECRET" \
    -H "Content-Type: application/json" \
    -d "{
          \"symbol\": \"$SYMBOL\",
          \"qty\": $QTY,
          \"side\": \"buy\",
          \"type\": \"market\",
          \"time_in_force\": \"day\"
        }" | jq .
}

# List all orders
list_orders() {
  echo "📜 Listing all orders..."
  curl -s -X GET "$BASE_URL/v2/orders" \
    -H "APCA-API-KEY-ID: $API_KEY" \
    -H "APCA-API-SECRET-KEY: $API_SECRET" | jq .
}

# --------------------------------
# Script Menu
# --------------------------------
echo "Alpaca API Test Script"
echo "======================"
echo "1) Get Account Info"
echo "2) Get Market Clock"
echo "3) Get Latest Quote (default AAPL)"
echo "4) Place Order (default 1 share AAPL)"
echo "5) List Orders"
echo "q) Quit"
echo

read -p "Choose an option: " choice

case $choice in
  1) get_account ;;
  2) get_clock ;;
  3) read -p "Enter symbol (default AAPL): " sym; get_quote $sym ;;
  4) read -p "Enter symbol (default AAPL): " sym; read -p "Enter qty (default 1): " qty; place_order $sym $qty ;;
  5) list_orders ;;
  q) echo "Exiting..."; exit 0 ;;
  *) echo "Invalid option" ;;
esac

