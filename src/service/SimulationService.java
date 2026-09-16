package service;

import model.Appliance;
import model.SimulationResult;
import java.util.ArrayList;
import java.util.List;

public class SimulationService {
    private final EnergyCalculator calculator;
    public SimulationService(EnergyCalculator calculator) { this.calculator = calculator; }

    public SimulationResult simulate(List<Appliance> appliances, Appliance selected,
                                     double optimizedHours, double tariff) {
        List<Appliance> optimized = new ArrayList<>();
        for (Appliance appliance : appliances) {
            optimized.add(appliance == selected ? appliance.copyWithHours(optimizedHours) : appliance);
        }
        return new SimulationResult(calculator.totalMonthlyKwh(appliances),
                calculator.totalMonthlyKwh(optimized), tariff);
    }
}
