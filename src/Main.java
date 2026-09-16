import model.Appliance;
import model.EnergyUsage;
import model.SimulationResult;
import service.*;
import util.FileManager;
import util.InputValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Main {
    private final Scanner scanner = new Scanner(System.in);
    private final List<Appliance> appliances = new ArrayList<>();
    private final EnergyCalculator calculator = new EnergyCalculator();
    private final WasteAnalyzer wasteAnalyzer = new WasteAnalyzer(calculator);
    private final SimulationService simulationService = new SimulationService(calculator);
    private final RecommendationService recommendationService = new RecommendationService(calculator, wasteAnalyzer);
    private final ReportService reportService = new ReportService(calculator, wasteAnalyzer, recommendationService);
    private final FileManager fileManager = new FileManager();
    private double tariff = 8.0;

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        System.out.println("========================================");
        System.out.println(" PERSONAL ENERGY WASTE SIMULATOR");
        System.out.println("========================================");
        System.out.println("Default tariff: Rs. " + money(tariff) + "/kWh");
        boolean running = true;
        while (running) {
            showMenu();
            int choice = readInt("Choose an option: ");
            try {
                switch (choice) {
                    case 1 -> addAppliance();
                    case 2 -> viewAppliances();
                    case 3 -> calculateConsumption();
                    case 4 -> viewWaste();
                    case 5 -> runSimulation();
                    case 6 -> compareAppliances();
                    case 7 -> viewRecommendations();
                    case 8 -> System.out.println(reportService.build(appliances, tariff));
                    case 9 -> editAppliance();
                    case 10 -> saveData();
                    case 11 -> loadData();
                    case 12 -> { running = false; System.out.println("Goodbye."); }
                    default -> System.out.println("Please choose a number from 1 to 12.");
                }
            } catch (IllegalArgumentException | IOException exception) {
                System.out.println("Input or file error: " + exception.getMessage());
            }
        }
    }

    private void showMenu() {
        System.out.println("\n1. Add Appliance");
        System.out.println("2. View Appliances");
        System.out.println("3. Calculate Consumption");
        System.out.println("4. View Energy Waste");
        System.out.println("5. Run What-If Simulation");
        System.out.println("6. Compare Appliances");
        System.out.println("7. View Recommendations");
        System.out.println("8. Generate Report");
        System.out.println("9. Edit Appliance Usage");
        System.out.println("10. Save Data");
        System.out.println("11. Load Data");
        System.out.println("12. Exit");
    }

    private void addAppliance() {
        String name = readLine("Appliance name (for example Fan or Custom): ");
        InputValidator.requireText(name);
        double watts = readDouble("Power rating (watts): ");
        InputValidator.requirePositive(watts, "Power rating");
        int units = readInt("Number of units: ");
        InputValidator.requirePositive(units, "Units");
        double hours = readDouble("Hours used per day (0-24): ");
        InputValidator.requireRange(hours, 0, 24, "Hours per day");
        int days = readInt("Days used per month (1-31): ");
        InputValidator.requireRange(days, 1, 31, "Days per month");
        double necessary = readDouble("Necessary hours per day (0-actual hours): ");
        InputValidator.requireRange(necessary, 0, hours, "Necessary hours");
        appliances.add(new Appliance(name, watts, units, hours, days, necessary));
        System.out.println("Appliance added.");
    }

    private void viewAppliances() {
        if (appliances.isEmpty()) { System.out.println("No appliances added yet."); return; }
        System.out.println("\nAppliances:");
        for (int i = 0; i < appliances.size(); i++) {
            Appliance a = appliances.get(i);
            System.out.printf("%d. %s | %.1f W | %d unit(s) | %.1f h/day | necessary %.1f h/day | %d days/month%n",
                    i + 1, a.getName(), a.getPowerWatts(), a.getUnits(), a.getHoursPerDay(),
                    a.getNecessaryHoursPerDay(), a.getDaysPerMonth());
        }
    }

    private void calculateConsumption() {
        if (appliances.isEmpty()) { System.out.println("Add an appliance first."); return; }
        tariff = readTariff();
        System.out.printf("\n%-22s %12s %12s %12s %12s%n", "Appliance", "Daily kWh", "Monthly kWh", "Yearly kWh", "Monthly cost");
        for (Appliance a : appliances) {
            EnergyUsage usage = calculator.usage(a, tariff);
            System.out.printf("%-22s %12.2f %12.2f %12.2f Rs. %8.2f%n", a.getName(), usage.getDailyKwh(), usage.getMonthlyKwh(), usage.getYearlyKwh(), usage.getMonthlyCost());
        }
        double daily = calculator.totalDailyKwh(appliances);
        double monthly = calculator.totalMonthlyKwh(appliances);
        System.out.printf("Totals: %.2f kWh/day, %.2f kWh/month, %.2f kWh/year%n", daily, monthly, monthly * 12);
        System.out.printf("Costs: Rs. %.2f/day, Rs. %.2f/month, Rs. %.2f/year%n", daily * tariff, monthly * tariff, monthly * 12 * tariff);
    }

    private void viewWaste() {
        if (appliances.isEmpty()) { System.out.println("Add an appliance first."); return; }
        tariff = readTariff();
        System.out.printf("\n%-22s %14s %14s %14s %14s%n", "Appliance", "Waste/day", "Waste/month", "Waste/year", "Cost/month");
        for (Appliance a : appliances) System.out.printf("%-22s %11.2f kWh %11.2f kWh %11.2f kWh Rs. %8.2f%n", a.getName(), wasteAnalyzer.wastedDailyKwh(a), wasteAnalyzer.wastedMonthlyKwh(a), wasteAnalyzer.wastedYearlyKwh(a), wasteAnalyzer.wastedMonthlyKwh(a) * tariff);
        double percentage = wasteAnalyzer.wastePercentage(appliances);
        System.out.printf("Total potential waste: %.2f kWh/month, Rs. %.2f/month (%.2f%%)%n", wasteAnalyzer.totalWastedMonthlyKwh(appliances), wasteAnalyzer.totalWastedMonthlyKwh(appliances) * tariff, percentage);
        System.out.println("Waste score: " + wasteAnalyzer.score(percentage) + " (project-defined indicator, not an official standard)");
    }

    private void runSimulation() {
        if (appliances.isEmpty()) { System.out.println("Add an appliance first."); return; }
        viewAppliances();
        int index = readInt("Select appliance number: ");
        if (index < 1 || index > appliances.size()) { System.out.println("Invalid appliance number."); return; }
        Appliance selected = appliances.get(index - 1);
        double optimizedHours = readDouble("New hours per day (0-" + selected.getHoursPerDay() + "): ");
        InputValidator.requireRange(optimizedHours, 0, selected.getHoursPerDay(), "New hours");
        tariff = readTariff();
        SimulationResult result = simulationService.simulate(appliances, selected, optimizedHours, tariff);
        System.out.printf("Current household energy: %.2f kWh/month%nOptimized energy: %.2f kWh/month%nEnergy saved: %.2f kWh/month%nReduction: %.2f%%%nMoney saved: Rs. %.2f/month, Rs. %.2f/year%n", result.getCurrentKwh(), result.getOptimizedKwh(), result.getEnergySaved(), result.getPercentageReduction(), result.getMonthlyMoneySaved(), result.getYearlyMoneySaved());
    }

    private void compareAppliances() {
        if (appliances.isEmpty()) { System.out.println("Add an appliance first."); return; }
        List<Appliance> sorted = new ArrayList<>(appliances);
        sorted.sort(Comparator.comparingDouble(calculator::monthlyKwh).reversed());
        System.out.println("\nAppliances ranked by monthly consumption:");
        for (int i = 0; i < sorted.size(); i++) System.out.printf("%d. %s - %.2f kWh/month%n", i + 1, sorted.get(i).getName(), calculator.monthlyKwh(sorted.get(i)));
    }

    private void viewRecommendations() {
        if (appliances.isEmpty()) { System.out.println("Add an appliance first."); return; }
        tariff = readTariff();
        System.out.println("\nRecommendations:");
        for (String recommendation : recommendationService.generate(appliances, tariff)) System.out.println("- " + recommendation);
    }

    private void editAppliance() {
        if (appliances.isEmpty()) { System.out.println("Add an appliance first."); return; }
        viewAppliances();
        int index = readInt("Select appliance number: ");
        if (index < 1 || index > appliances.size()) { System.out.println("Invalid appliance number."); return; }
        Appliance appliance = appliances.get(index - 1);
        double hours = readDouble("New actual hours per day (0-24): ");
        InputValidator.requireRange(hours, 0, 24, "Hours per day");
        double necessary = readDouble("New necessary hours per day (0-" + hours + "): ");
        InputValidator.requireRange(necessary, 0, hours, "Necessary hours");
        appliance.setHoursPerDay(hours);
        appliance.setNecessaryHoursPerDay(necessary);
        System.out.println("Usage updated.");
    }

    private void saveData() throws IOException { fileManager.save(appliances); System.out.println("Saved " + appliances.size() + " appliance(s) to appliances.csv."); }
    private void loadData() throws IOException { appliances.clear(); appliances.addAll(fileManager.load()); System.out.println("Loaded " + appliances.size() + " appliance(s) from appliances.csv."); }
    private double readTariff() { double value = readDouble("Tariff in Rs./kWh (current " + money(tariff) + "): "); InputValidator.requirePositive(value, "Tariff"); return value; }
    private String readLine(String prompt) { System.out.print(prompt); return scanner.nextLine().trim(); }
    private int readInt(String prompt) { while (true) { try { return Integer.parseInt(readLine(prompt)); } catch (NumberFormatException e) { System.out.println("Enter a whole number."); } } }
    private double readDouble(String prompt) { while (true) { try { return Double.parseDouble(readLine(prompt)); } catch (NumberFormatException e) { System.out.println("Enter a number."); } } }
    private String money(double value) { return String.format("%.2f", value); }
}
