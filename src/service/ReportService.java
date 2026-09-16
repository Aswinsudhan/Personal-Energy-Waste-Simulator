package service;

import model.Appliance;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private final EnergyCalculator calculator;
    private final WasteAnalyzer wasteAnalyzer;
    private final RecommendationService recommendationService;

    public ReportService(EnergyCalculator calculator, WasteAnalyzer wasteAnalyzer,
                         RecommendationService recommendationService) {
        this.calculator = calculator;
        this.wasteAnalyzer = wasteAnalyzer;
        this.recommendationService = recommendationService;
    }

    public String build(List<Appliance> appliances, double tariff) {
        double daily = calculator.totalDailyKwh(appliances);
        double monthly = calculator.totalMonthlyKwh(appliances);
        double yearly = calculator.totalYearlyKwh(appliances);
        double wastedMonthly = wasteAnalyzer.totalWastedMonthlyKwh(appliances);
        double wastePercent = wasteAnalyzer.wastePercentage(appliances);
        Appliance highest = appliances.stream().max(Comparator.comparingDouble(calculator::monthlyKwh)).orElse(null);
        Appliance highestWaste = wasteAnalyzer.highestWasteAppliance(appliances);
        StringBuilder report = new StringBuilder("\n========== ENERGY REPORT ==========\n");
        report.append(String.format("Daily consumption: %.2f kWh | Cost: Rs. %.2f%n", daily, daily * tariff));
        report.append(String.format("Monthly consumption: %.2f kWh | Cost: Rs. %.2f%n", monthly, monthly * tariff));
        report.append(String.format("Yearly consumption: %.2f kWh | Cost: Rs. %.2f%n", yearly, yearly * tariff));
        report.append(String.format("Potential wasted energy: %.2f kWh/month (%.2f%%)%n", wastedMonthly, wastePercent));
        report.append(String.format("Potential wasted cost: Rs. %.2f/month | Rs. %.2f/year%n", wastedMonthly * tariff, wastedMonthly * tariff * 12));
        report.append("Highest consuming appliance: ").append(highest == null ? "None" : highest.getName()).append('\n');
        report.append("Highest potential waste appliance: ").append(highestWaste == null ? "None" : highestWaste.getName()).append('\n');
        report.append(String.format("Possible monthly savings: Rs. %.2f%nPossible yearly savings: Rs. %.2f%n", wastedMonthly * tariff, wastedMonthly * tariff * 12));
        report.append("\nRecommendations:\n");
        for (String recommendation : recommendationService.generate(appliances, tariff)) report.append("- ").append(recommendation).append('\n');
        report.append("\nWaste score: ").append(wasteAnalyzer.score(wastePercent)).append(" (project-defined indicator)\n");
        return report.toString();
    }
}
