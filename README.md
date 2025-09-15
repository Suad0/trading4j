# Quant Trading System

This is a sophisticated quantitative trading system designed for algorithmic trading strategies, portfolio management, and market data analysis. The backend is built with Java and Spring Boot, providing a robust and scalable platform for trading operations.

## Key Features

The backend provides a comprehensive suite of features accessible through a RESTful API:

*   **Market Data:**
    *   Real-time and historical market data retrieval for various financial instruments.
    *   Batch retrieval for multiple symbols.
    *   Market status information (e.g., open/closed, supported symbols).
    *   Caching mechanism for efficient data access.

*   **Trading:**
    *   Execution of buy and sell orders (market, limit).
    *   Order status tracking and cancellation.
    *   Access to trade history with filtering capabilities.
    *   Order validation without execution.

*   **Portfolio Management:**
    *   Real-time portfolio summary, including total value, cash balance, and unrealized P&L.
    *   Detailed view of all current positions.
    *   Performance metrics calculation.
    *   Synchronization with external brokerages.

*   **Machine Learning:**
    *   Integration of machine learning models for trading predictions.
    *   Endpoints for model training, prediction, and performance monitoring.
    *   Support for a custom Stochastic XLSTM model.
    *   Comprehensive analysis endpoints that combine predictions, metrics, and status.

*   **System Monitoring:**
    *   Endpoints for monitoring system health, readiness, and performance.
    *   Detailed statistics on trading strategies and portfolio performance.

## Getting Started

To run the backend system, you will need to have Java and Maven installed. The application can be started using the provided `start-trading-system.sh` script or by running the `QuantTradingSystemApplication` class.

Configuration for different environments (dev, prod, test) can be found in the `src/main/resources` directory.
