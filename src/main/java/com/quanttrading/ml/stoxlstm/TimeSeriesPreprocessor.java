package com.quanttrading.ml.stoxlstm;

import java.util.Arrays;

// Data preprocessing utilities
public class TimeSeriesPreprocessor {
    
    public TimeSeriesPreprocessor() {
        // Default constructor
    }

    // Patch creation as described in the paper
    public static double[][][] createPatches(double[] series, int patchSize, int stride) {
        // Zero-padding at front and back
        int paddedLength = series.length + stride + (patchSize - stride);
        double[] paddedSeries = new double[paddedLength];

        // Front padding
        Arrays.fill(paddedSeries, 0, stride, 0.0);

        // Copy original series
        System.arraycopy(series, 0, paddedSeries, stride, series.length);

        // Back padding
        Arrays.fill(paddedSeries, stride + series.length, paddedLength, 0.0);

        // Create patches
        int numPatches = (paddedLength - patchSize) / stride + 1;
        double[][][] patches = new double[numPatches][1][patchSize];

        for (int i = 0; i < numPatches; i++) {
            int start = i * stride;
            for (int j = 0; j < patchSize; j++) {
                patches[i][0][j] = paddedSeries[start + j];
            }
        }

        return patches;
    }

    // Instance normalization
    public static double[] instanceNormalize(double[] series) {
        double mean = Arrays.stream(series).average().orElse(0.0);
        double variance = Arrays.stream(series).map(x -> Math.pow(x - mean, 2)).average().orElse(1.0);
        double std = Math.sqrt(variance);

        return Arrays.stream(series).map(x -> (x - mean) / (std + 1e-8)).toArray();
    }

    // Denormalization
    public static double[] denormalize(double[] normalizedSeries, double originalMean, double originalStd) {
        return Arrays.stream(normalizedSeries).map(x -> x * originalStd + originalMean).toArray();
    }
}
