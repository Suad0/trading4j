package com.quanttrading.ml.stoxlstm;

// Result container for forecasts
class ForecastResult {
    public final double[] forecast;
    public final double klDivergence;
    public final double reconstructionLoss;

    public ForecastResult(double[] forecast, double klDivergence, double reconstructionLoss) {
        this.forecast = forecast;
        this.klDivergence = klDivergence;
        this.reconstructionLoss = reconstructionLoss;
    }
}
