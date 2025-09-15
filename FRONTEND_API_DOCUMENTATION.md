# Frontend API Documentation

This document provides a comprehensive guide for the frontend team to interact with the Quant Trading System's backend API. The API is built on REST principles and uses JSON for all data exchange.

**Base URL:** The base URL for all API endpoints is assumed to be the address of the running backend server. All endpoints are prefixed with `/api`.

## Authentication

(Note: The current implementation does not seem to have explicit authentication on all endpoints, but this section can be updated once SecurityConfig is fully implemented. For now, some endpoints might be protected by JWT, which would require an `Authorization: Bearer <token>` header.)

---

## Market Data API

**Controller:** `MarketDataController`

Provides access to market data, both real-time and historical.

### 1. Get Current Market Data

*   **Endpoint:** `GET /api/market-data/{symbol}`
*   **Description:** Retrieves the latest market data for a single symbol.
*   **Path Parameters:**
    *   `symbol` (string, required): The stock symbol (e.g., `AAPL`).
*   **Successful Response (200 OK):**

```json
{
  "symbol": "AAPL",
  "price": 151.25,
  "volume": 1000000,
  "high": 152.50,
  "low": 149.75,
  "open": 150.00,
  "change": 1.25,
  "changePercent": 0.8333,
  "timestamp": "2025-09-15T10:30:00",
  "isMarketOpen": true
}
```

### 2. Get Batch Market Data

*   **Endpoint:** `GET /api/market-data/batch`
*   **Description:** Retrieves the latest market data for multiple symbols in a single request.
*   **Query Parameters:**
    *   `symbols` (string, required): A comma-separated list of stock symbols (e.g., `AAPL,GOOG,MSFT`).
*   **Successful Response (200 OK):** An array of market data objects.

```json
[
  {
    "symbol": "AAPL",
    "price": 151.25,
    ...
  },
  {
    "symbol": "GOOG",
    "price": 2805.50,
    ...
  }
]
```

### 3. Get Historical Market Data (Date Range)

*   **Endpoint:** `GET /api/market-data/{symbol}/history`
*   **Description:** Retrieves historical OHLCV data for a symbol within a specified date range.
*   **Path Parameters:**
    *   `symbol` (string, required): The stock symbol.
*   **Query Parameters:**
    *   `from` (string, required): The start date in `YYYY-MM-DD` format.
    *   `to` (string, required): The end date in `YYYY-MM-DD` format.
*   **Successful Response (200 OK):** An array of historical data points.

```json
[
  {
    "symbol": "AAPL",
    "date": "2025-09-12",
    "open": 148.50,
    "high": 150.00,
    "low": 148.00,
    "close": 149.75,
    "adjustedClose": 149.75,
    "volume": 85000000
  }
]
```

### 4. Get Market Status

*   **Endpoint:** `GET /api/market-data/status`
*   **Description:** Returns the current market status (open/closed) and a list of supported symbols.
*   **Successful Response (200 OK):**

```json
{
  "isOpen": true,
  "status": "OPEN",
  "timestamp": "2025-09-15T10:30:00",
  "timezone": "America/New_York",
  "supportedSymbols": ["AAPL", "GOOG", "MSFT", ...]
}
```

---

## Trading API

**Controller:** `TradingController`

Handles all trading operations, including placing and managing orders.

### 1. Execute a Buy Order

*   **Endpoint:** `POST /api/trading/buy`
*   **Description:** Submits a buy order.
*   **Request Body:**

```json
{
  "symbol": "AAPL",
  "quantity": 10,
  "orderType": "market", // or "limit"
  "limitPrice": 150.00 // Required if orderType is "limit"
}
```

*   **Successful Response (200 OK):**

```json
{
  "orderId": "12345-abcde-67890",
  "symbol": "AAPL",
  "quantity": 10,
  "type": "BUY",
  "orderType": "market",
  "status": "SUBMITTED",
  "submittedAt": "2025-09-15T10:35:00"
}
```

### 2. Execute a Sell Order

*   **Endpoint:** `POST /api/trading/sell`
*   **Description:** Submits a sell order.
*   **Request Body:** Same as the buy order.
*   **Successful Response (200 OK):** Same as the buy order response, but with `type` as `SELL`.

### 3. Get Order Status

*   **Endpoint:** `GET /api/trading/orders/{orderId}/status`
*   **Description:** Retrieves the status of a specific order.
*   **Path Parameters:**
    *   `orderId` (string, required): The ID of the order.
*   **Successful Response (200 OK):**

```json
{
  "orderId": "12345-abcde-67890",
  "status": "FILLED",
  "statusMessage": "The order has been filled.",
  "lastUpdated": "2025-09-15T10:36:00"
}
```

### 4. Cancel an Order

*   **Endpoint:** `DELETE /api/trading/orders/{orderId}`
*   **Description:** Cancels a pending order.
*   **Path Parameters:**
    *   `orderId` (string, required): The ID of the order to cancel.
*   **Successful Response:** `200 OK` (No content)

### 5. Get Trade History

*   **Endpoint:** `GET /api/trading/history`
*   **Description:** Retrieves the history of all trades.
*   **Query Parameters:**
    *   `limit` (integer, optional): The maximum number of trades to return.
    *   `symbol` (string, optional): Filter trades by a specific symbol.
*   **Successful Response (200 OK):** An array of trade history objects.

```json
[
  {
    "orderId": "12345-abcde-67890",
    "symbol": "AAPL",
    "type": "BUY",
    "quantity": 10,
    "price": 151.20,
    "totalValue": 1512.00,
    "status": "FILLED",
    "executedAt": "2025-09-15T10:35:05",
    "strategyName": "SimpleMovingAverageStrategy",
    "accountId": "default-account"
  }
]
```

---

## Portfolio API

**Controller:** `PortfolioController`

Provides access to portfolio information, including positions and performance.

### 1. Get Portfolio Summary

*   **Endpoint:** `GET /api/portfolio`
*   **Description:** Retrieves a summary of the current portfolio.
*   **Successful Response (200 OK):**

```json
{
  "accountId": "default-account",
  "totalValue": 150000.00,
  "cashBalance": 50000.00,
  "positionsValue": 100000.00,
  "totalUnrealizedPnL": 5000.00,
  "positionCount": 5,
  "lastUpdated": "2025-09-15T10:40:00"
}
```

### 2. Get All Positions

*   **Endpoint:** `GET /api/portfolio/positions`
*   **Description:** Retrieves a list of all positions in the portfolio.
*   **Successful Response (200 OK):** An array of position objects.

```json
[
  {
    "symbol": "AAPL",
    "quantity": 100,
    "averagePrice": 145.00,
    "currentPrice": 151.25,
    "marketValue": 15125.00,
    "unrealizedPnL": 625.00,
    "unrealizedPnLPercent": 4.3103,
    "costBasis": 14500.00,
    "lastUpdated": "2025-09-15T10:40:00"
  }
]
```

### 3. Get Position for a Symbol

*   **Endpoint:** `GET /api/portfolio/positions/{symbol}`
*   **Description:** Retrieves the position for a specific symbol.
*   **Path Parameters:**
    *   `symbol` (string, required): The stock symbol.
*   **Successful Response (200 OK):** A single position object.

### 4. Get Portfolio Performance

*   **Endpoint:** `GET /api/portfolio/performance`
*   **Description:** Retrieves detailed performance metrics for the portfolio.
*   **Successful Response (200 OK):**

```json
{
  "totalValue": 150000.00,
  "cashBalance": 50000.00,
  "positionsValue": 100000.00,
  "totalUnrealizedPnL": 5000.00,
  "dailyPnL": 1500.00,
  "dailyReturn": 1.01,
  "totalReturn": 3.45,
  "positionCount": 5,
  "calculatedAt": "2025-09-15T10:45:00"
}
```

### 5. Synchronize Portfolio

*   **Endpoint:** `POST /api/portfolio/sync`
*   **Description:** Triggers a synchronization of the portfolio with the external brokerage.
*   **Successful Response (200 OK):** The updated portfolio summary.

---

## Machine Learning API

**Controller:** `MLController`

Manages machine learning models and provides predictions.

### 1. Initialize ML Model

*   **Endpoint:** `POST /api/ml/models/{symbol}/initialize`
*   **Description:** Initializes an ML model for a given symbol.
*   **Path Parameters:**
    *   `symbol` (string, required): The stock symbol.
*   **Successful Response (200 OK):**

```json
{
  "success": true,
  "message": "ML model initialized successfully for AAPL",
  "symbol": "AAPL",
  "timestamp": "2025-09-15T10:50:00"
}
```

### 2. Train ML Model

*   **Endpoint:** `POST /api/ml/models/{symbol}/train`
*   **Description:** Starts the training process for an ML model asynchronously.
*   **Path Parameters:**
    *   `symbol` (string, required): The stock symbol.
*   **Successful Response (202 Accepted):**

```json
{
  "success": true,
  "message": "ML model training started for AAPL",
  "symbol": "AAPL",
  "training_status": "in_progress",
  "timestamp": "2025-09-15T10:51:00"
}
```

### 3. Get ML Prediction

*   **Endpoint:** `GET /api/ml/predictions/{symbol}`
*   **Description:** Retrieves an ML-based prediction for a symbol.
*   **Path Parameters:**
    *   `symbol` (string, required): The stock symbol.
*   **Successful Response (200 OK):**

```json
{
  "success": true,
  "symbol": "AAPL",
  "prediction": {
    "direction": "UP",
    "confidence": 75.5,
    "model_name": "StochasticXLSTM",
    ...
  },
  "timestamp": "2025-09-15T10:55:00",
  "interpretation": {
    "direction": "The ML model predicts upward price movement",
    "confidence_level": "High Confidence",
    ...
  }
}
```

### 4. Get Model Metrics

*   **Endpoint:** `GET /api/ml/models/{symbol}/metrics`
*   **Description:** Retrieves performance metrics for a trained model.
*   **Path Parameters:**
    *   `symbol` (string, required): The stock symbol.
*   **Successful Response (200 OK):**

```json
{
  "success": true,
  "symbol": "AAPL",
  "metrics": {
    "model_name": "StochasticXLSTM",
    "accuracy": 85.2,
    ...
  },
  "timestamp": "2025-09-15T11:00:00"
}
```

### 5. Get Comprehensive ML Analysis

*   **Endpoint:** `GET /api/ml/analysis/{symbol}`
*   **Description:** Provides a full analysis for a symbol, including prediction, metrics, and status.
*   **Path Parameters:**
    *   `symbol` (string, required): The stock symbol.
*   **Successful Response (200 OK):** A combined object with prediction, metrics, and model status.

---

## System Monitoring API

**Controller:** `MonitoringController`

Provides endpoints for monitoring the health and status of the trading system.

### 1. Get System Health Summary

*   **Endpoint:** `GET /api/monitoring/health/summary`
*   **Description:** Retrieves a summary of the system's health, including portfolio and P&L info.
*   **Successful Response (200 OK):**

```json
{
  "portfolio_value": 150000.00,
  "cash_balance": 50000.00,
  "active_positions": 5,
  "daily_pnl": 1500.00,
  "total_pnl": 5000.00,
  "status": "healthy"
}
```

### 2. Get System Status

*   **Endpoint:** `GET /api/monitoring/status`
*   **Description:** Returns the overall status of the application.
*   **Successful Response (200 OK):**

```json
{
  "application": "Quantitative Trading System",
  "version": "1.0.0",
  "status": "READY",
  "ready": true,
  "shutdownInProgress": false,
  "timestamp": 1663245600000
}
```

### 3. Get System Statistics

*   **Endpoint:** `GET /api/monitoring/stats`
*   **Description:** Provides various statistics about the trading system's operation.
*   **Successful Response (200 OK):**

```json
{
  "total_positions": 5,
  "total_strategies": 2,
  "active_strategies": 1,
  "total_trades": 150,
  "average_win_rate": 0.65
}
```
