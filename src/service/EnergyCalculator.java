package service;

import model.Appliance;
import model.EnergyUsage;
import java.util.List;

public class EnergyCalculator {
    public double dailyKwh(Appliance appliance) {
        return appliance.getPowerWatts() * appliance.getUnits() * appliance.getHoursPerDay() / 1000;
    }

    public double monthlyKwh(Appliance appliance) {
        return dailyKwh(appliance) * appliance.getDaysPerMonth();
    }

    public double yearlyKwh(Appliance appliance) {
        return monthlyKwh(appliance) * 12;
    }

    public EnergyUsage usage(Appliance appliance, double tariff) {
        return new EnergyUsage(dailyKwh(appliance), monthlyKwh(appliance), yearlyKwh(appliance), tariff);
    }

    public double totalMonthlyKwh(List<Appliance> appliances) {
        return appliances.stream().mapToDouble(this::monthlyKwh).sum();
    }

    public double totalDailyKwh(List<Appliance> appliances) {
        return appliances.stream().mapToDouble(this::dailyKwh).sum();
    }

    public double totalYearlyKwh(List<Appliance> appliances) {
        return totalMonthlyKwh(appliances) * 12;
    }
}
