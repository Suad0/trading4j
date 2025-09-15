package com.quanttrading.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrading.config.AlpacaConfig;
import com.quanttrading.dto.AccountInfo;
import com.quanttrading.dto.OrderRequest;
import com.quanttrading.dto.OrderResponse;
import com.quanttrading.exception.ApiConnectionException;
import com.quanttrading.exception.InvalidOrderException;
import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Position;
import com.quanttrading.model.TradeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Alpaca API client with authentication, error handling, and retry logic
 */
@Component
public class AlpacaApiClientImpl implements AlpacaApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AlpacaApiClientImpl.class);
    
    private final AlpacaConfig alpacaConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public AlpacaApiClientImpl(AlpacaConfig alpacaConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.alpacaConfig = alpacaConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Retryable(value = {ApiConnectionException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public OrderResponse placeOrder(OrderRequest request) {
        logger.info("Placing order: {} {} shares of {}", request.getType(), request.getQuantity(), request.getSymbol());
        
        validateOrderRequest(request);
        
        try {
            String url = alpacaConfig.baseUrl() + "/v2/orders";
            HttpHeaders headers = createAuthHeaders();
            
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("symbol", request.getSymbol());
            orderData.put("qty", request.getQuantity().toString());
            orderData.put("side", request.getType().name().toLowerCase());
            orderData.put("type", request.getOrderType());
            orderData.put("time_in_force", request.getTimeInForce());
            
            if ("limit".equals(request.getOrderType()) && request.getLimitPrice() != null) {
                orderData.put("limit_price", request.getLimitPrice().toString());
            }
            
            if ("stop".equals(request.getOrderType()) && request.getStopPrice() != null) {
                orderData.put("stop_price", request.getStopPrice().toString());
            }
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderData, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            return parseOrderResponse(response.getBody());
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error placing order: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new InvalidOrderException("Order placement failed: " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            logger.error("Connection error placing order", e);
            throw new ApiConnectionException("Failed to connect to Alpaca API", e);
        } catch (Exception e) {
            logger.error("Unexpected error placing order", e);
            throw new ApiConnectionException("Unexpected error during order placement", e);
        }
    }
    
    @Override
    @Retryable(value = {ApiConnectionException.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public OrderStatus getOrderStatus(String orderId) {
        logger.debug("Getting order status for order: {}", orderId);
        
        try {
            String url = alpacaConfig.baseUrl() + "/v2/orders/" + orderId;
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            return parseOrderStatus(response.getBody());
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error getting order status: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiConnectionException("Failed to get order status: " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            logger.error("Connection error getting order status", e);
            throw new ApiConnectionException("Failed to connect to Alpaca API", e);
        } catch (Exception e) {
            logger.error("Unexpected error getting order status", e);
            throw new ApiConnectionException("Unexpected error getting order status", e);
        }
    }
    
    @Override
    @Retryable(value = {ApiConnectionException.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public AccountInfo getAccountInfo() {
        logger.debug("Getting account information");
        
        try {
            String url = alpacaConfig.baseUrl() + "/v2/account";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            return parseAccountInfo(response.getBody());
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error getting account info: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiConnectionException("Failed to get account info: " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            logger.error("Connection error getting account info", e);
            throw new ApiConnectionException("Failed to connect to Alpaca API", e);
        } catch (Exception e) {
            logger.error("Unexpected error getting account info", e);
            throw new ApiConnectionException("Unexpected error getting account info", e);
        }
    }
    
    @Override
    @Retryable(value = {ApiConnectionException.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public List<Position> getPositions() {
        logger.debug("Getting current positions");
        
        try {
            String url = alpacaConfig.baseUrl() + "/v2/positions";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            return parsePositions(response.getBody());
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error getting positions: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiConnectionException("Failed to get positions: " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            logger.error("Connection error getting positions", e);
            throw new ApiConnectionException("Failed to connect to Alpaca API", e);
        } catch (Exception e) {
            logger.error("Unexpected error getting positions", e);
            throw new ApiConnectionException("Unexpected error getting positions", e);
        }
    }
    
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("APCA-API-KEY-ID", alpacaConfig.apiKey());
        headers.set("APCA-API-SECRET-KEY", alpacaConfig.secretKey());
        return headers;
    }
    
    private void validateOrderRequest(OrderRequest request) {
        if (request.getSymbol() == null || request.getSymbol().trim().isEmpty()) {
            throw new InvalidOrderException("Symbol is required");
        }
        
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Quantity must be positive");
        }
        
        if (request.getType() == null) {
            throw new InvalidOrderException("Trade type is required");
        }
        
        if (request.getOrderType() == null || request.getOrderType().trim().isEmpty()) {
            throw new InvalidOrderException("Order type is required");
        }
        
        if ("limit".equals(request.getOrderType()) && request.getLimitPrice() == null) {
            throw new InvalidOrderException("Limit price is required for limit orders");
        }
        
        if ("stop".equals(request.getOrderType()) && request.getStopPrice() == null) {
            throw new InvalidOrderException("Stop price is required for stop orders");
        }
    }
    
    private OrderResponse parseOrderResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            
            OrderResponse response = new OrderResponse();
            response.setOrderId(json.get("id").asText());
            response.setSymbol(json.get("symbol").asText());
            response.setQuantity(new BigDecimal(json.get("qty").asText()));
            response.setType(TradeType.valueOf(json.get("side").asText().toUpperCase()));
            response.setOrderType(json.get("type").asText());
            response.setStatus(OrderStatus.valueOf(json.get("status").asText().toUpperCase()));
            
            if (json.has("submitted_at")) {
                response.setSubmittedAt(LocalDateTime.parse(json.get("submitted_at").asText(), 
                    DateTimeFormatter.ISO_DATE_TIME));
            }
            
            if (json.has("filled_at") && !json.get("filled_at").isNull()) {
                response.setFilledAt(LocalDateTime.parse(json.get("filled_at").asText(), 
                    DateTimeFormatter.ISO_DATE_TIME));
            }
            
            if (json.has("filled_avg_price") && !json.get("filled_avg_price").isNull()) {
                response.setFilledPrice(new BigDecimal(json.get("filled_avg_price").asText()));
            }
            
            if (json.has("filled_qty")) {
                response.setFilledQuantity(new BigDecimal(json.get("filled_qty").asText()));
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error parsing order response", e);
            throw new ApiConnectionException("Failed to parse order response", e);
        }
    }
    
    private OrderStatus parseOrderStatus(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            return OrderStatus.valueOf(json.get("status").asText().toUpperCase());
        } catch (Exception e) {
            logger.error("Error parsing order status", e);
            throw new ApiConnectionException("Failed to parse order status", e);
        }
    }
    
    private AccountInfo parseAccountInfo(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            
            AccountInfo accountInfo = new AccountInfo();
            accountInfo.setAccountId(json.get("id").asText());
            accountInfo.setCash(new BigDecimal(json.get("cash").asText()));
            accountInfo.setPortfolioValue(new BigDecimal(json.get("portfolio_value").asText()));
            accountInfo.setBuyingPower(new BigDecimal(json.get("buying_power").asText()));
            
            if (json.has("daytrading_buying_power")) {
                accountInfo.setDayTradingBuyingPower(new BigDecimal(json.get("daytrading_buying_power").asText()));
            }
            
            accountInfo.setStatus(json.get("status").asText());
            accountInfo.setPatternDayTrader(json.get("pattern_day_trader").asBoolean());
            accountInfo.setLastUpdated(LocalDateTime.now());
            
            return accountInfo;
            
        } catch (Exception e) {
            logger.error("Error parsing account info", e);
            throw new ApiConnectionException("Failed to parse account info", e);
        }
    }
    
    private List<Position> parsePositions(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            List<Position> positions = new ArrayList<>();
            
            if (json.isArray()) {
                for (JsonNode positionNode : json) {
                    Position position = new Position();
                    position.setSymbol(positionNode.get("symbol").asText());
                    position.setQuantity(new BigDecimal(positionNode.get("qty").asText()));
                    position.setAveragePrice(new BigDecimal(positionNode.get("avg_entry_price").asText()));
                    
                    if (positionNode.has("market_value")) {
                        position.setCurrentPrice(new BigDecimal(positionNode.get("market_value").asText())
                            .divide(position.getQuantity(), 2, BigDecimal.ROUND_HALF_UP));
                    }
                    
                    if (positionNode.has("unrealized_pl")) {
                        position.setUnrealizedPnL(new BigDecimal(positionNode.get("unrealized_pl").asText()));
                    }
                    
                    position.setLastUpdated(LocalDateTime.now());
                    positions.add(position);
                }
            }
            
            return positions;
            
        } catch (Exception e) {
            logger.error("Error parsing positions", e);
            throw new ApiConnectionException("Failed to parse positions", e);
        }
    }
}