-- Create portfolios table
CREATE TABLE portfolios (
    account_id VARCHAR(255) PRIMARY KEY,
    cash_balance DECIMAL(19,4) NOT NULL CHECK (cash_balance >= 0),
    total_value DECIMAL(19,4) NOT NULL CHECK (total_value >= 0),
    last_updated TIMESTAMP NOT NULL
);

-- Create positions table
CREATE TABLE positions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol VARCHAR(255) NOT NULL,
    quantity DECIMAL(19,8) NOT NULL,
    average_price DECIMAL(19,4) NOT NULL CHECK (average_price >= 0),
    current_price DECIMAL(19,4) CHECK (current_price >= 0),
    unrealized_pnl DECIMAL(19,4),
    last_updated TIMESTAMP NOT NULL,
    portfolio_account_id VARCHAR(255) NOT NULL,
    FOREIGN KEY (portfolio_account_id) REFERENCES portfolios(account_id) ON DELETE CASCADE,
    UNIQUE(portfolio_account_id, symbol)
);

-- Create trades table
CREATE TABLE trades (
    order_id VARCHAR(255) PRIMARY KEY,
    symbol VARCHAR(255) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('BUY', 'SELL')),
    quantity DECIMAL(19,8) NOT NULL CHECK (quantity > 0),
    price DECIMAL(19,4) NOT NULL CHECK (price > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'FILLED', 'CANCELLED', 'REJECTED', 'PARTIALLY_FILLED')),
    executed_at TIMESTAMP,
    strategy_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create indexes for better query performance
CREATE INDEX idx_positions_portfolio_id ON positions(portfolio_account_id);
CREATE INDEX idx_positions_symbol ON positions(symbol);
CREATE INDEX idx_positions_last_updated ON positions(last_updated);

CREATE INDEX idx_trades_symbol ON trades(symbol);
CREATE INDEX idx_trades_status ON trades(status);
CREATE INDEX idx_trades_type ON trades(type);
CREATE INDEX idx_trades_strategy_name ON trades(strategy_name);
CREATE INDEX idx_trades_executed_at ON trades(executed_at);
CREATE INDEX idx_trades_created_at ON trades(created_at);

CREATE INDEX idx_portfolios_last_updated ON portfolios(last_updated);