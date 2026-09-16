package service;

import model.Appliance;
import java.util.Comparator;
import java.util.List;

public class WasteAnalyzer {
    private final EnergyCalculator calculator;

    public WasteAnalyzer(EnergyCalculator calculator) { this.calculator = calculator; }

    public double wastedDailyKwh(Appliance appliance) {
        double hours = Math.max(0, appliance.getHoursPerDay() - appliance.getNecessaryHoursPerDay());
        return hours * appliance.getPowerWatts() * appliance.getUnits() / 1000;
    }

    public double wastedMonthlyKwh(Appliance appliance) {
        return wastedDailyKwh(appliance) * appliance.getDaysPerMonth();
    }

    public double wastedYearlyKwh(Appliance appliance) { return wastedMonthlyKwh(appliance) * 12; }

    public double totalWastedMonthlyKwh(List<Appliance> appliances) {
        return appliances.stream().mapToDouble(this::wastedMonthlyKwh).sum();
    }

    public double wastePercentage(List<Appliance> appliances) {
        double total = calculator.totalMonthlyKwh(appliances);
        return total == 0 ? 0 : totalWastedMonthlyKwh(appliances) / total * 100;
    }

    public String score(double percentage) {
        if (percentage < 5) return "Very Low";
        if (percentage < 15) return "Low";
        if (percentage < 30) return "Moderate";
        if (percentage < 50) return "High";
        return "Very High";
    }

    public Appliance highestWasteAppliance(List<Appliance> appliances) {
        return appliances.stream().max(Comparator.comparingDouble(this::wastedMonthlyKwh)).orElse(null);
    }
}
