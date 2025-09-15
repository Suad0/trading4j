package com.quanttrading.ml.stoxlstm;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified Stochastic Extended LSTM Cell with proper DL4J integration.
 * 
 * Features:
 * - Exponential gating mechanism for enhanced gradient flow
 * - Stochastic regularization with VAE-style latent variables
 * - Memory mixing for improved long-term dependencies
 * - Layer normalization for training stability
 * - Proper gradient computation support
 */
public class StochasticXLSTMCell {
    
    private static final Logger logger = LoggerFactory.getLogger(StochasticXLSTMCell.class);
    
    private final int inputSize;
    private final int hiddenSize;
    private final int latentSize;
    private final StoxLSTMConfig config;
    
    // Weight matrices (will be managed by DL4J layer)
    private INDArray Wi, Wf, Wo, Wg; // Input weights for gates
    private INDArray Ui, Uf, Uo, Ug; // Recurrent weights for gates
    private INDArray bi, bf, bo, bg; // Biases for gates
    
    // Stochastic components
    private INDArray WMu, WLogVar; // Latent variable projection weights
    private INDArray bMu, bLogVar; // Latent variable projection biases
    
    // Memory mixing
    private INDArray WMix, bMix; // Memory mixing weights and biases
    
    // Layer normalization parameters
    private INDArray gamma, beta; // Layer norm scale and shift
    
    public StochasticXLSTMCell(int inputSize, int hiddenSize, int latentSize, StoxLSTMConfig config) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.latentSize = latentSize;
        this.config = config != null ? config : new StoxLSTMConfig();
        
        initializeWeights();
        
        logger.info("Initialized StochasticXLSTMCell: input={}, hidden={}, latent={}", 
                   inputSize, hiddenSize, latentSize);
    }
    
    /**
     * Initialize weight matrices with proper scaling.
     */
    private void initializeWeights() {
        // Xavier initialization for better gradient flow
        double inputScale = Math.sqrt(2.0 / (inputSize + hiddenSize));
        double hiddenScale = Math.sqrt(2.0 / (hiddenSize + hiddenSize));
        
        // Input gate weights
        Wi = Nd4j.randn(inputSize, hiddenSize).muli(inputScale);
        Ui = Nd4j.randn(hiddenSize, hiddenSize).muli(hiddenScale);
        bi = Nd4j.zeros(1, hiddenSize);
        
        // Forget gate weights (initialize bias to 1 for better learning)
        Wf = Nd4j.randn(inputSize, hiddenSize).muli(inputScale);
        Uf = Nd4j.randn(hiddenSize, hiddenSize).muli(hiddenScale);
        bf = Nd4j.ones(1, hiddenSize);
        
        // Output gate weights
        Wo = Nd4j.randn(inputSize, hiddenSize).muli(inputScale);
        Uo = Nd4j.randn(hiddenSize, hiddenSize).muli(hiddenScale);
        bo = Nd4j.zeros(1, hiddenSize);
        
        // Candidate weights
        Wg = Nd4j.randn(inputSize, hiddenSize).muli(inputScale);
        Ug = Nd4j.randn(hiddenSize, hiddenSize).muli(hiddenScale);
        bg = Nd4j.zeros(1, hiddenSize);
        
        // Stochastic components
        double latentScale = Math.sqrt(2.0 / (hiddenSize + latentSize));
        WMu = Nd4j.randn(hiddenSize, latentSize).muli(latentScale);
        WLogVar = Nd4j.randn(hiddenSize, latentSize).muli(latentScale);
        bMu = Nd4j.zeros(1, latentSize);
        bLogVar = Nd4j.zeros(1, latentSize);
        
        // Memory mixing
        WMix = Nd4j.randn(hiddenSize, hiddenSize).muli(hiddenScale);
        bMix = Nd4j.zeros(1, hiddenSize);
        
        // Layer normalization
        gamma = Nd4j.ones(1, hiddenSize);
        beta = Nd4j.zeros(1, hiddenSize);
    }
    
    /**
     * Forward pass through the stochastic xLSTM cell.
     */
    public StoxLSTMState forward(INDArray input, StoxLSTMState prevState, boolean training) {
        long batchSize = input.size(0);
        
        // Create new state
        StoxLSTMState newState = new StoxLSTMState((int) batchSize, hiddenSize, latentSize);
        
        // Get previous states
        INDArray prevHidden = prevState.getHiddenState();
        INDArray prevCell = prevState.getCellState();
        INDArray prevLatent = prevState.getStochasticLatent();
        
        // Compute gates with exponential gating mechanism
        INDArray inputGate = computeExponentialGate(input, prevHidden, Wi, Ui, bi, "sigmoid");
        INDArray forgetGate = computeExponentialGate(input, prevHidden, Wf, Uf, bf, "sigmoid");
        INDArray outputGate = computeExponentialGate(input, prevHidden, Wo, Uo, bo, "sigmoid");
        INDArray candidateValues = computeExponentialGate(input, prevHidden, Wg, Ug, bg, "tanh");
        
        // Update cell state with memory mixing
        INDArray newCellState = updateCellStateWithMixing(
            prevCell, inputGate, forgetGate, candidateValues, prevHidden);
        
        // Apply layer normalization to cell state
        INDArray normalizedCellState = applyLayerNormalization(newCellState);
        
        // Compute stochastic latent variables
        INDArray latentInput = prevHidden; // Could concatenate with other features
        INDArray mu = latentInput.mmul(WMu).addRowVector(bMu);
        INDArray logVar = latentInput.mmul(WLogVar).addRowVector(bLogVar);
        
        // Update stochastic components
        newState.updateStochasticLatent(mu, logVar);
        
        // Compute new hidden state with stochastic enhancement
        INDArray baseHidden = outputGate.mul(Nd4j.nn.tanh(normalizedCellState));
        INDArray stochasticEnhancement = computeStochasticEnhancement(
            newState.getStochasticLatent(), baseHidden);
        INDArray newHiddenState = baseHidden.add(stochasticEnhancement);
        
        // Apply stochastic regularization if training
        if (training) {
            newState.applyStochasticRegularization(config.beta, true);
        }
        
        // Set states
        newState.setHiddenState(newHiddenState);
        newState.setCellState(newCellState);
        newState.setNormalizedCellState(normalizedCellState);
        
        return newState;
    }
    
    /**
     * Compute exponential gating mechanism (key xLSTM feature).
     */
    private INDArray computeExponentialGate(INDArray input, INDArray hidden, 
                                          INDArray W, INDArray U, INDArray b, String activation) {
        
        // Linear transformation: W*x + U*h + b
        INDArray linear = input.mmul(W).add(hidden.mmul(U)).addRowVector(b);
        
        // Exponential gating for enhanced gradient flow
        if (config.useExponentialGating) {
            INDArray exponentialComponent = Nd4j.nn.sigmoid(linear.mul(config.exponentialGatingScale));
            linear = linear.mul(exponentialComponent);
        }
        
        // Apply final activation
        return switch (activation) {
            case "sigmoid" -> Nd4j.nn.sigmoid(linear);
            case "tanh" -> Nd4j.nn.tanh(linear);
            default -> linear;
        };
    }
    
    /**
     * Update cell state with memory mixing mechanism.
     */
    private INDArray updateCellStateWithMixing(INDArray prevCellState, INDArray inputGate, 
                                             INDArray forgetGate, INDArray candidateValues,
                                             INDArray hiddenState) {
        
        // Standard LSTM update
        INDArray standardUpdate = forgetGate.mul(prevCellState).add(inputGate.mul(candidateValues));
        
        if (!config.useMemoryMixing) {
            return standardUpdate;
        }
        
        // Memory mixing component (xLSTM enhancement)
        INDArray mixingInput = hiddenState.mmul(WMix).addRowVector(bMix);
        INDArray mixingGate = Nd4j.nn.sigmoid(mixingInput);
        
        // Mix previous cell state with new update
        INDArray mixedUpdate = mixingGate.mul(prevCellState)
                .add(mixingGate.rsub(1.0).mul(standardUpdate));
        
        return mixedUpdate;
    }
    
    /**
     * Apply layer normalization for training stability.
     */
    private INDArray applyLayerNormalization(INDArray input) {
        if (!config.useLayerNormalization) {
            return input;
        }
        
        // Compute mean and variance across hidden dimension
        INDArray mean = input.mean(1);
        INDArray variance = input.var(1).add(1e-8); // Add epsilon for stability
        INDArray std = Nd4j.math().sqrt(variance);
        
        // Normalize and apply learnable parameters
        INDArray normalized = input.sub(mean).div(std);
        return normalized.mul(gamma).add(beta);
    }
    
    /**
     * Compute stochastic enhancement to hidden state.
     */
    private INDArray computeStochasticEnhancement(INDArray stochasticLatent, INDArray baseHidden) {
        // Simple linear projection of latent variables
        // In practice, this could be more sophisticated
        double enhancementScale = config.stochasticRegularization;
        return stochasticLatent.mmul(Nd4j.randn(latentSize, hiddenSize).muli(enhancementScale));
    }
    
    /**
     * Set weight matrices (called by DL4J layer).
     */
    public void setWeights(INDArray Wi, INDArray Wf, INDArray Wo, INDArray Wg,
                          INDArray Ui, INDArray Uf, INDArray Uo, INDArray Ug,
                          INDArray bi, INDArray bf, INDArray bo, INDArray bg,
                          INDArray WMu, INDArray WLogVar, INDArray bMu, INDArray bLogVar,
                          INDArray WMix, INDArray bMix, INDArray gamma, INDArray beta) {
        this.Wi = Wi; this.Wf = Wf; this.Wo = Wo; this.Wg = Wg;
        this.Ui = Ui; this.Uf = Uf; this.Uo = Uo; this.Ug = Ug;
        this.bi = bi; this.bf = bf; this.bo = bo; this.bg = bg;
        this.WMu = WMu; this.WLogVar = WLogVar; this.bMu = bMu; this.bLogVar = bLogVar;
        this.WMix = WMix; this.bMix = bMix;
        this.gamma = gamma; this.beta = beta;
    }
    
    /**
     * Get all weight matrices (for gradient computation).
     */
    public java.util.Map<String, INDArray> getWeights() {
        java.util.Map<String, INDArray> weights = new java.util.HashMap<>();
        weights.put("Wi", Wi); weights.put("Wf", Wf); weights.put("Wo", Wo); weights.put("Wg", Wg);
        weights.put("Ui", Ui); weights.put("Uf", Uf); weights.put("Uo", Uo); weights.put("Ug", Ug);
        weights.put("bi", bi); weights.put("bf", bf); weights.put("bo", bo); weights.put("bg", bg);
        weights.put("WMu", WMu); weights.put("WLogVar", WLogVar); weights.put("bMu", bMu); weights.put("bLogVar", bLogVar);
        weights.put("WMix", WMix); weights.put("bMix", bMix);
        weights.put("gamma", gamma); weights.put("beta", beta);
        return weights;
    }
    
    // Getters
    public int getInputSize() { return inputSize; }
    public int getHiddenSize() { return hiddenSize; }
    public int getLatentSize() { return latentSize; }
    public StoxLSTMConfig getConfig() { return config; }
}