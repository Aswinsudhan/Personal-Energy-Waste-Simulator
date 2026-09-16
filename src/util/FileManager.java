package util;

import model.Appliance;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private final Path file = Paths.get("appliances.csv");

    public void save(List<Appliance> appliances) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Appliance a : appliances) lines.add(String.join("|", a.getName().replace("|", " "),
                String.valueOf(a.getPowerWatts()), String.valueOf(a.getUnits()), String.valueOf(a.getHoursPerDay()),
                String.valueOf(a.getDaysPerMonth()), String.valueOf(a.getNecessaryHoursPerDay())));
        Files.write(file, lines);
    }

    public List<Appliance> load() throws IOException {
        List<Appliance> appliances = new ArrayList<>();
        if (!Files.exists(file)) return appliances;
        for (String line : Files.readAllLines(file)) {
            String[] parts = line.split("\\|", -1);
            if (parts.length == 6) appliances.add(new Appliance(parts[0], Double.parseDouble(parts[1]), Integer.parseInt(parts[2]),
                    Double.parseDouble(parts[3]), Integer.parseInt(parts[4]), Double.parseDouble(parts[5])));
        }
        return appliances;
    }
}
