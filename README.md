# Personal Energy Waste Simulator

## Hosted architecture

The web dashboard remains a static frontend suitable for Vercel. Google Identity Services are handled by Firebase Authentication in the browser; the browser sends the resulting Firebase ID token to the Java API on Render. The API verifies that token with Firebase Admin SDK and reads/writes only `users/{verifiedUid}/...` in Firestore.

```text
Google Login -> Vercel frontend -> HTTPS -> Render Java API -> Firebase Authentication + Firestore
```

The old local-storage values are no longer loaded into an authenticated session. This prevents anonymous browser data from silently being assigned to a Google account. The authenticated session starts empty unless its Firestore account already has data.

## Configure Firebase and deployment

1. Create a Firebase project and enable **Authentication > Sign-in method > Google**.
2. Create a Firestore database and deploy [firestore.rules](firestore.rules). The rules permit access only when `request.auth.uid` equals the document path user ID. The Render API still performs its own token verification and ownership scoping.
3. Register a Firebase web app. Copy its public configuration into [web/config.js](web/config.js). Public web API keys are not admin credentials.
4. Create a Firebase service account. Add `FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`, and `FIREBASE_PRIVATE_KEY` to Render. Keep the private key only in Render environment variables; never put it in `web/`.
5. Set `FRONTEND_URL` on Render to the exact Vercel origin, including `https://` and excluding a trailing path. CORS is intentionally not `*`.
6. Deploy the Java service using [render.yaml](render.yaml), or configure Render with `mvn -q package` as the build command and `java -cp "target/classes;target/dependency/*" api.ApiServer` as the start command. Render uses the included [Dockerfile](Dockerfile) by default.
7. Set the Render API URL in [web/config.js](web/config.js) as `apiUrl`. For a Vite-based Vercel wrapper, the equivalent deployment variable is `VITE_API_URL`; this repository intentionally retains its existing dependency-free static frontend.

For Vercel, the included [vercel.json](vercel.json) generates `web/config.js` during the build. Add the public Firebase variables and `VITE_API_URL` in Vercel Project Settings for the Production, Preview, and Development environments as needed. Do not commit the generated deployment values.

Required server variables are listed in [.env.example](.env.example). Never commit `.env`, service-account JSON, or real OAuth credentials.

## API

All routes except `/api/health` require `Authorization: Bearer <Firebase ID token>`:

```text
GET/POST /api/appliances
PUT/DELETE /api/appliances/{id}
GET/POST /api/simulations
GET       /api/dashboard
GET/PUT   /api/settings
```

The API validates appliance input and calculates energy using `watts * units * hours * days / 1000`. Dashboard values aggregate every appliance in the authenticated user's collection, including total consumption, target consumption, potentially avoidable energy, monthly cost, annual consumption, and potential annual saving.

## Acceptance test

Use two real Google accounts. Add appliances and a simulation under account A, log out, sign in as account B, and confirm account A's data is absent. Add account B's data, log out, and sign in as A again. Each account must see only its own Firestore collection. Browser sign-out clears the in-memory state before another account can load.

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
