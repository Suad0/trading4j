# Comprehensive Integration Tests

This directory contains comprehensive integration tests for the Quantitative Trading System, covering end-to-end workflows, portfolio management, and strategy execution scenarios.

## Test Structure

### 1. TradingWorkflowIntegrationTest
**Purpose**: End-to-end integration tests covering complete trading workflows

**Key Test Scenarios**:
- Complete trade workflow from order placement to portfolio updates
- Buy and sell order execution with portfolio verification
- Multiple trades workflow maintaining portfolio consistency
- Order status tracking throughout the trade lifecycle
- Trade history maintenance and retrieval
- Portfolio summary updates reflecting executed trades
- Error handling for invalid orders and insufficient funds
- Concurrent trades maintaining data integrity

**Coverage**: Tests the entire flow from REST API endpoints through services to database persistence.

### 2. PortfolioManagementIntegrationTest
**Purpose**: Integration tests for portfolio management and performance calculation scenarios

**Key Test Scenarios**:
- Portfolio summary calculations with multiple positions
- Position management and retrieval
- Performance metrics calculation (P&L, returns, etc.)
- Profitable and losing position scenarios
- Position updates after trades with average price calculations
- Portfolio synchronization with external sources
- Active position filtering (excluding zero quantities)
- Portfolio value updates with market data changes
- Performance calculation over time with historical data
- Multiple account portfolio separation

**Coverage**: Tests portfolio operations, position management, and performance metrics calculation.

### 3. StrategyExecutionIntegrationTest
**Purpose**: Integration tests for strategy execution with mock market data

**Key Test Scenarios**:
- Strategy registration and management
- Strategy execution with bullish, bearish, and neutral market conditions
- Multiple strategy execution
- Strategy performance tracking and metrics
- Strategy execution with insufficient data handling
- Disabled strategy behavior
- Strategy signal execution triggering trades
- Strategy backtesting with historical data
- Strategy configuration effects on signal generation
- Strategy risk management and position limits
- Strategy performance reset functionality

**Coverage**: Tests strategy registration, execution, and performance tracking with various market conditions.

### 4. EndToEndIntegrationTest
**Purpose**: Comprehensive end-to-end integration test covering the complete trading system workflow

**Key Test Scenarios**:
- Complete automated trading workflow (market data → strategy → signals → trades → portfolio)
- Multi-symbol trading workflow
- Risk management workflow preventing overexposure
- Market data driven trading responding to different market conditions
- Concurrent trading workflow maintaining data integrity
- Error recovery workflow handling failures gracefully
- Performance optimization workflow handling large volumes
- Data consistency workflow maintaining accuracy across operations

**Coverage**: Tests the integration of all system components from market data to portfolio management.

## Supporting Infrastructure

### Test Data Fixtures (`fixtures/TestDataFixtures.java`)
**Purpose**: Provides reusable methods for creating test data

**Key Features**:
- Portfolio setup with configurable cash balances
- Position creation with various scenarios
- Trade record creation
- Order request builders
- Market data creation
- Historical data generation
- Multi-symbol portfolio setup
- Large portfolio setup for performance testing
- Error scenario setup
- Test data cleanup utilities

### Mock Market Data Provider (`mocks/MockMarketDataProvider.java`)
**Purpose**: Provides various market data scenarios for testing strategies

**Key Features**:
- Bullish market data (upward trends)
- Bearish market data (downward trends)
- Neutral market data (sideways movement)
- Volatile market data (high price swings)
- Random market data generation
- Historical market data for backtesting
- Moving average test data setups
- Gap up/down scenarios
- Realistic price movement simulation

### Integration Test Utils (`utils/IntegrationTestUtils.java`)
**Purpose**: Utility methods for common integration test operations

**Key Features**:
- HTTP request/response helpers
- JSON serialization/deserialization
- BigDecimal comparison with tolerance
- Random data generation
- Condition waiting with timeout
- Date/time formatting
- UUID validation
- Percentage and price validation
- Retry operations with exponential backoff
- Assertion helpers for approximate equality

## Test Configuration

### Application Test Configuration (`application-test.yml`)
- In-memory SQLite database for isolation
- Disabled Flyway migrations
- Test-specific configuration values
- Reduced logging levels for cleaner test output

### Test Profiles
- Uses `@ActiveProfiles("test")` for test-specific configuration
- Transactional tests with automatic rollback
- Spring Boot test context with random ports for web tests

## Running the Tests

### Individual Test Classes
```bash
mvn test -Dtest=TradingWorkflowIntegrationTest
mvn test -Dtest=PortfolioManagementIntegrationTest
mvn test -Dtest=StrategyExecutionIntegrationTest
mvn test -Dtest=EndToEndIntegrationTest
```

### All Integration Tests
```bash
mvn test -Dtest="com.quanttrading.integration.*"
```

## Test Coverage

The integration tests cover:

1. **Complete Trading Workflows**: From order placement to portfolio updates
2. **Portfolio Management**: Position tracking, performance calculation, and synchronization
3. **Strategy Execution**: Strategy registration, execution, and performance tracking
4. **Error Handling**: Invalid orders, insufficient funds, system failures
5. **Concurrency**: Multiple simultaneous operations
6. **Data Consistency**: Ensuring accurate state across all operations
7. **Performance**: Large volume handling and optimization
8. **Risk Management**: Position limits and validation

## Key Requirements Covered

- **Requirement 6.5**: Comprehensive unit and integration tests
- **End-to-end workflows**: Complete trading system functionality
- **Portfolio management**: Position tracking and performance calculation
- **Strategy execution**: Automated trading with various market conditions
- **Error handling**: Graceful failure handling and recovery
- **Data integrity**: Consistent state across concurrent operations

## Notes

- Tests use mock external APIs to avoid dependencies on real market data
- Database operations are transactional and rolled back after each test
- Test data is isolated and cleaned up automatically
- Performance tests validate reasonable execution times
- All monetary calculations use BigDecimal for precision
- Tests cover both success and failure scenarios
- Comprehensive assertion coverage ensures system reliability