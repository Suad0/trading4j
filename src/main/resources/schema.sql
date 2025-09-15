-- Portfolio table
CREATE TABLE IF NOT EXISTS portfolios (
    account_id VARCHAR(255) PRIMARY KEY,
    cash_balance DECIMAL(19,4) NOT NULL DEFAULT 0.0,
    total_value DECIMAL(19,4) NOT NULL DEFAULT 0.0,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Position table
CREATE TABLE IF NOT EXISTS positions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol VARCHAR(10) NOT NULL,
    quantity DECIMAL(19,8) NOT NULL,
    average_price DECIMAL(19,4) NOT NULL,
    current_price DECIMAL(19,4),
    unrealized_pnl DECIMAL(19,4),
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    portfolio_account_id VARCHAR(255),
    FOREIGN KEY (portfolio_account_id) REFERENCES portfolios(account_id) ON DELETE CASCADE
);

-- Trade table
CREATE TABLE IF NOT EXISTS trades (
    order_id VARCHAR(255) PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('BUY', 'SELL')),
    quantity DECIMAL(19,8) NOT NULL,
    price DECIMAL(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUBMITTED', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')),
    executed_at TIMESTAMP,
    strategy_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Market Data table
CREATE TABLE IF NOT EXISTS market_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol VARCHAR(10) NOT NULL,
    price DECIMAL(19,4) NOT NULL,
    volume DECIMAL(19,0) NOT NULL,
    high DECIMAL(19,4) NOT NULL,
    low DECIMAL(19,4) NOT NULL,
    open DECIMAL(19,4) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_positions_symbol ON positions(symbol);
CREATE INDEX IF NOT EXISTS idx_positions_portfolio ON positions(portfolio_account_id);
CREATE INDEX IF NOT EXISTS idx_trades_symbol ON trades(symbol);
CREATE INDEX IF NOT EXISTS idx_trades_status ON trades(status);
CREATE INDEX IF NOT EXISTS idx_trades_created_at ON trades(created_at);
CREATE INDEX IF NOT EXISTS idx_market_data_symbol_timestamp ON market_data(symbol, timestamp);
CREATE INDEX IF NOT EXISTS idx_market_data_timestamp ON market_data(timestamp);