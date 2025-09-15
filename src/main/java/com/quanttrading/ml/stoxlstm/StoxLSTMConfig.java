package com.quanttrading.ml.stoxlstm;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

// Configuration class for StoxLSTM
class StoxLSTMConfig {
    // Extended LSTM configuration
    public boolean useExponentialGating = true;
    public double exponentialGatingScale = 0.5;
    public boolean useMemoryMixing = true;
    public boolean useLayerNormalization = true;
    public double stochasticRegularization = 0.01;
    public int lookbackLength = 336;      // L in paper
    public int predictionHorizon = 96;    // T in paper
    public int patchSize = 56;            // P in paper
    public int stride = 24;               // S in paper
    public int modelDim = 64;             // d_model in paper
    public int latentDim = 16;            // d_latent in paper
    public int hiddenSize = 64;
    public double beta = 500.0;           // KL divergence weight
    public double learningRate = 0.001;
    public int numChannels = 1;           // For multivariate series
    public boolean useSeriesDecomposition = true;
    public String cellType = "sLSTM";     // "sLSTM" or "mLSTM"
}
