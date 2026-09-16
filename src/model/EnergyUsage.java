package model;

public class EnergyUsage {
    private final double dailyKwh;
    private final double monthlyKwh;
    private final double yearlyKwh;
    private final double dailyCost;
    private final double monthlyCost;
    private final double yearlyCost;

    public EnergyUsage(double dailyKwh, double monthlyKwh, double yearlyKwh, double tariff) {
        this.dailyKwh = dailyKwh;
        this.monthlyKwh = monthlyKwh;
        this.yearlyKwh = yearlyKwh;
        this.dailyCost = dailyKwh * tariff;
        this.monthlyCost = monthlyKwh * tariff;
        this.yearlyCost = yearlyKwh * tariff;
    }

    public double getDailyKwh() { return dailyKwh; }
    public double getMonthlyKwh() { return monthlyKwh; }
    public double getYearlyKwh() { return yearlyKwh; }
    public double getDailyCost() { return dailyCost; }
    public double getMonthlyCost() { return monthlyCost; }
    public double getYearlyCost() { return yearlyCost; }
}
