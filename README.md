# Personal Energy Waste Simulator

A beginner-friendly Java 21 console application that estimates household electricity use, identifies potential avoidable usage, and compares current and optimized scenarios.

## Problem statement
People often know that an appliance uses electricity but cannot easily see the monthly cost or which usage could potentially be avoided. This project turns simple appliance and usage details into understandable energy, cost, comparison, and recommendation reports.

## Features
- Add, list, edit, and remove appliances.
- Daily, monthly, and yearly energy and cost calculations.
- Configurable electricity tariff in Rs./kWh.
- Potential waste analysis using user-entered necessary hours.
- What-if simulation for reducing one appliance's hours.
- Transparent waste score and appliance comparison.
- Rule-based recommendations, with no AI or external service.
- Simple CSV file persistence in `appliances.csv`.

## Formulae
- `daily kWh = watts x units x hours per day / 1000`
- `monthly kWh = daily kWh x days used per month`
- `yearly kWh = monthly kWh x 12`
- `potential wasted kWh = (actual hours - necessary hours) x watts x units x days / 1000`
- `cost = kWh x tariff`
- `percentage reduction = energy saved / current energy x 100`

Potential waste is an estimate, not proof that extra usage is unnecessary. The waste score thresholds are a project-defined indicator, not an official energy-efficiency standard.

## OOP concepts used
`Appliance` and result classes encapsulate state. Services separate calculation, waste analysis, simulation, recommendations, and reporting responsibilities. Constructors, getters, setters, composition, `ArrayList`, an enum-like menu, validation, exception handling, and file I/O are demonstrated.

## Project structure
- `src/model`: domain data classes
- `src/service`: calculations and business rules
- `src/util`: validation and CSV persistence
- `src/Main.java`: console user interface

## How to run
From the project root using Java 21:

```text
javac -d out src\\Main.java src\\model\\*.java src\\service\\*.java src\\util\\*.java
java -cp out Main
```

## Run the website locally
The easiest option on Windows is to double-click **Start Personal Energy Simulator.bat** in the project folder. It starts the local website and opens it in your browser automatically.

The browser version is a dependency-free local website. If you prefer to start it from a terminal, make sure Node.js is installed and run:

```text
node server.js
```

You can also use the package script with `npm.cmd start` in PowerShell.

Then open `http://localhost:3000`. Appliance data and tariff settings are saved in the browser's local storage. The website includes the same calculations, potential waste analysis, what-if simulation, recommendations, comparison, and report views as the console version.

Keep the small server window open while using the website. Close it when you are finished.

The application creates or updates `appliances.csv` when appliances are saved. Use menu option 10 to save and option 11 to load.

## Example output
```text
Monthly consumption: 252.00 kWh | Cost: Rs. 2016.00
Potential wasted energy: 72.00 kWh/month (28.57%)
Potential wasted cost: Rs. 576.00/month
Waste score: Moderate
```

## Limitations and future enhancements
The estimates use user-entered values, a monthly day count per appliance, and a single tariff. Future versions could support tiered tariffs, seasonal usage, charts, exportable PDF reports, and multiple household profiles while keeping the same calculation core.
