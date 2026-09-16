package model;

public class Appliance {
    private String name;
    private double powerWatts;
    private int units;
    private double hoursPerDay;
    private int daysPerMonth;
    private double necessaryHoursPerDay;

    public Appliance(String name, double powerWatts, int units, double hoursPerDay,
                     int daysPerMonth, double necessaryHoursPerDay) {
        this.name = name;
        this.powerWatts = powerWatts;
        this.units = units;
        this.hoursPerDay = hoursPerDay;
        this.daysPerMonth = daysPerMonth;
        this.necessaryHoursPerDay = necessaryHoursPerDay;
    }

    public String getName() { return name; }
    public double getPowerWatts() { return powerWatts; }
    public int getUnits() { return units; }
    public double getHoursPerDay() { return hoursPerDay; }
    public int getDaysPerMonth() { return daysPerMonth; }
    public double getNecessaryHoursPerDay() { return necessaryHoursPerDay; }

    public void setHoursPerDay(double hoursPerDay) { this.hoursPerDay = hoursPerDay; }
    public void setNecessaryHoursPerDay(double hours) { this.necessaryHoursPerDay = hours; }

    public Appliance copyWithHours(double hours) {
        return new Appliance(name, powerWatts, units, hours, daysPerMonth,
                Math.min(necessaryHoursPerDay, hours));
    }
}
