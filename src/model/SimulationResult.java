package model;

public class SimulationResult {
    private final double currentKwh;
    private final double optimizedKwh;
    private final double energySaved;
    private final double percentageReduction;
    private final double monthlyMoneySaved;
    private final double yearlyMoneySaved;

    public SimulationResult(double currentKwh, double optimizedKwh, double tariff) {
        this.currentKwh = currentKwh;
        this.optimizedKwh = optimizedKwh;
        this.energySaved = Math.max(0, currentKwh - optimizedKwh);
        this.percentageReduction = currentKwh == 0 ? 0 : energySaved / currentKwh * 100;
        this.monthlyMoneySaved = energySaved * tariff;
        this.yearlyMoneySaved = monthlyMoneySaved * 12;
    }

    public double getCurrentKwh() { return currentKwh; }
    public double getOptimizedKwh() { return optimizedKwh; }
    public double getEnergySaved() { return energySaved; }
    public double getPercentageReduction() { return percentageReduction; }
    public double getMonthlyMoneySaved() { return monthlyMoneySaved; }
    public double getYearlyMoneySaved() { return yearlyMoneySaved; }
}
