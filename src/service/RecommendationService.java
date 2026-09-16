package service;

import model.Appliance;
import java.util.ArrayList;
import java.util.List;

public class RecommendationService {
    private final EnergyCalculator calculator;
    private final WasteAnalyzer wasteAnalyzer;

    public RecommendationService(EnergyCalculator calculator, WasteAnalyzer wasteAnalyzer) {
        this.calculator = calculator;
        this.wasteAnalyzer = wasteAnalyzer;
    }

    public List<String> generate(List<Appliance> appliances, double tariff) {
        List<String> recommendations = new ArrayList<>();
        double total = calculator.totalMonthlyKwh(appliances);
        for (Appliance appliance : appliances) {
            double monthly = calculator.monthlyKwh(appliance);
            double waste = wasteAnalyzer.wastedMonthlyKwh(appliance);
            if (monthly > 100) {
                recommendations.add(appliance.getName() + " has high consumption (" + format(monthly)
                        + " kWh/month). Consider reducing its operating hours.");
            }
            if (waste > 0) {
                recommendations.add(appliance.getName() + " has potential avoidable usage. Reducing it to necessary hours could save "
                        + format(waste * tariff) + " per month.");
            }
            if (total > 0 && monthly / total >= 0.30) {
                recommendations.add(appliance.getName() + " contributes " + format(monthly / total * 100)
                        + "% of household consumption and deserves priority attention.");
            }
        }
        if (recommendations.isEmpty()) recommendations.add("No major rule-based opportunities were identified.");
        return recommendations;
    }

    private String format(double value) { return String.format("%.2f", value); }
}
