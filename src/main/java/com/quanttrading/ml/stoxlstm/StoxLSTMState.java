package com.quanttrading.ml.stoxlstm;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * State container for Stochastic xLSTM cells.
 * Manages hidden states, cell states, and stochastic components.
 */
public class StoxLSTMState {
    
    private INDArray hiddenState;
    private INDArray cellState;
    private INDArray normalizedCellState;
    private INDArray memoryState;
    private INDArray stochasticLatent;
    private INDArray stochasticMask;
    
    // Stochastic components
    private INDArray mu; // Mean of latent distribution
    private INDArray logVar; // Log variance of latent distribution
    private INDArray epsilon; // Random noise for reparameterization
    
    // Memory mixing components
    private INDArray memoryMixingWeights;
    private INDArray attentionWeights;
    
    private final int batchSize;
    private final int hiddenSize;
    private final int latentSize;
    
    public StoxLSTMState(int batchSize, int hiddenSize, int latentSize) {
        this.batchSize = batchSize;
        this.hiddenSize = hiddenSize;
        this.latentSize = latentSize;
        
        initializeStates();
    }
    
    private void initializeStates() {
        // Initialize main states
        this.hiddenState = Nd4j.zeros(batchSize, hiddenSize);
        this.cellState = Nd4j.zeros(batchSize, hiddenSize);
        this.normalizedCellState = Nd4j.zeros(batchSize, hiddenSize);
        this.memoryState = Nd4j.zeros(batchSize, hiddenSize);
        
        // Initialize stochastic components
        this.stochasticLatent = Nd4j.zeros(batchSize, latentSize);
        this.mu = Nd4j.zeros(batchSize, latentSize);
        this.logVar = Nd4j.zeros(batchSize, latentSize);
        this.epsilon = Nd4j.randn(batchSize, latentSize);
        
        // Initialize memory components
        this.memoryMixingWeights = Nd4j.zeros(batchSize, hiddenSize);
        this.attentionWeights = Nd4j.zeros(batchSize, hiddenSize);
        this.stochasticMask = Nd4j.ones(batchSize, hiddenSize);
    }
    
    /**
     * Update stochastic latent variables using reparameterization trick.
     */
    public void updateStochasticLatent(INDArray newMu, INDArray newLogVar) {
        this.mu = newMu;
        this.logVar = newLogVar;
        
        // Reparameterization trick: z = mu + sigma * epsilon
        INDArray sigma = Nd4j.math().exp(logVar.mul(0.5));
        this.stochasticLatent = mu.add(sigma.mul(epsilon));
    }
    
    /**
     * Calculate KL divergence for regularization.
     */
    public double calculateKLDivergence() {
        // KL(q(z|x) || p(z)) where p(z) = N(0,I)
        // KL = 0.5 * sum(1 + log(sigma^2) - mu^2 - sigma^2)
        INDArray klTerm = Nd4j.ones(mu.shape())
                .add(logVar)
                .sub(mu.mul(mu))
                .sub(Nd4j.math().exp(logVar));
        
        return -0.5 * klTerm.sumNumber().doubleValue();
    }
    
    /**
     * Apply stochastic regularization to hidden state.
     */
    public void applyStochasticRegularization(double dropoutRate, boolean training) {
        if (training && dropoutRate > 0) {
            // Generate new stochastic mask
            stochasticMask = Nd4j.rand(hiddenState.shape()).gt(dropoutRate)
                    .castTo(hiddenState.dataType());
            
            // Apply mask with proper scaling
            hiddenState = hiddenState.mul(stochasticMask).div(1.0 - dropoutRate);
        }
    }
    
    /**
     * Update memory mixing weights based on attention mechanism.
     */
    public void updateMemoryMixing(INDArray attentionScores) {
        this.attentionWeights = Nd4j.nn.softmax(attentionScores, 1);
        this.memoryMixingWeights = attentionWeights;
    }
    
    /**
     * Reset all states to zero.
     */
    public void reset() {
        hiddenState.assign(0);
        cellState.assign(0);
        normalizedCellState.assign(0);
        memoryState.assign(0);
        stochasticLatent.assign(0);
        mu.assign(0);
        logVar.assign(0);
        epsilon.assign(Nd4j.randn(batchSize, latentSize));
        memoryMixingWeights.assign(0);
        attentionWeights.assign(0);
        stochasticMask.assign(1);
    }
    
    /**
     * Create a deep copy of the state.
     */
    public StoxLSTMState copy() {
        StoxLSTMState copy = new StoxLSTMState(batchSize, hiddenSize, latentSize);
        copy.hiddenState = this.hiddenState.dup();
        copy.cellState = this.cellState.dup();
        copy.normalizedCellState = this.normalizedCellState.dup();
        copy.memoryState = this.memoryState.dup();
        copy.stochasticLatent = this.stochasticLatent.dup();
        copy.mu = this.mu.dup();
        copy.logVar = this.logVar.dup();
        copy.epsilon = this.epsilon.dup();
        copy.memoryMixingWeights = this.memoryMixingWeights.dup();
        copy.attentionWeights = this.attentionWeights.dup();
        copy.stochasticMask = this.stochasticMask.dup();
        return copy;
    }
    
    // Getters and Setters
    public INDArray getHiddenState() { return hiddenState; }
    public void setHiddenState(INDArray hiddenState) { this.hiddenState = hiddenState; }
    
    public INDArray getCellState() { return cellState; }
    public void setCellState(INDArray cellState) { this.cellState = cellState; }
    
    public INDArray getNormalizedCellState() { return normalizedCellState; }
    public void setNormalizedCellState(INDArray normalizedCellState) { this.normalizedCellState = normalizedCellState; }
    
    public INDArray getMemoryState() { return memoryState; }
    public void setMemoryState(INDArray memoryState) { this.memoryState = memoryState; }
    
    public INDArray getStochasticLatent() { return stochasticLatent; }
    public void setStochasticLatent(INDArray stochasticLatent) { this.stochasticLatent = stochasticLatent; }
    
    public INDArray getMu() { return mu; }
    public void setMu(INDArray mu) { this.mu = mu; }
    
    public INDArray getLogVar() { return logVar; }
    public void setLogVar(INDArray logVar) { this.logVar = logVar; }
    
    public INDArray getEpsilon() { return epsilon; }
    public void setEpsilon(INDArray epsilon) { this.epsilon = epsilon; }
    
    public INDArray getMemoryMixingWeights() { return memoryMixingWeights; }
    public void setMemoryMixingWeights(INDArray memoryMixingWeights) { this.memoryMixingWeights = memoryMixingWeights; }
    
    public INDArray getAttentionWeights() { return attentionWeights; }
    public void setAttentionWeights(INDArray attentionWeights) { this.attentionWeights = attentionWeights; }
    
    public INDArray getStochasticMask() { return stochasticMask; }
    public void setStochasticMask(INDArray stochasticMask) { this.stochasticMask = stochasticMask; }
    
    public int getBatchSize() { return batchSize; }
    public int getHiddenSize() { return hiddenSize; }
    public int getLatentSize() { return latentSize; }
}