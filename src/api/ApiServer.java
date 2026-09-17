package api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.cloud.FirestoreClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import model.Appliance;
import service.EnergyCalculator;
import service.WasteAnalyzer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class ApiServer {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final EnergyCalculator CALCULATOR = new EnergyCalculator();
    private static final WasteAnalyzer WASTE = new WasteAnalyzer(CALCULATOR);
    private static final Firestore DB = initializeFirebase();

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", ApiServer::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Energy API listening on port " + port);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { send(exchange, 204, Map.of()); return; }
        try {
            if (exchange.getRequestURI().getPath().equals("/api/health")) { send(exchange, 200, Map.of("status", "ok")); return; }
            FirebaseToken token = authenticate(exchange);
            String uid = token.getUid();
            ensureProfile(token);
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();
            if (path.equals("/api/appliances")) { appliances(exchange, uid, method); return; }
            if (path.startsWith("/api/appliances/")) { appliance(exchange, uid, method, path.substring("/api/appliances/".length())); return; }
            if (path.equals("/api/simulations")) { simulations(exchange, uid, method); return; }
            if (path.equals("/api/settings")) { settings(exchange, uid, method); return; }
            if (path.equals("/api/dashboard")) { dashboard(exchange, uid); return; }
            sendError(exchange, 404, "Route not found");
        } catch (SecurityException e) { sendError(exchange, 401, "Authentication required"); }
        catch (IllegalArgumentException e) { sendError(exchange, 400, e.getMessage()); }
        catch (Exception e) { e.printStackTrace(); sendError(exchange, 500, "Request could not be completed"); }
    }

    private static void appliances(HttpExchange exchange, String uid, String method) throws Exception {
        DocumentReference root = DB.collection("users").document(uid);
        if (method.equals("GET")) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (QueryDocumentSnapshot doc : root.collection("appliances").get().get().getDocuments()) {
                Map<String, Object> item = new HashMap<>(doc.getData()); item.put("id", doc.getId()); result.add(item);
            }
            send(exchange, 200, result); return;
        }
        if (method.equals("POST")) { Map<String, Object> data = validateAppliance(readBody(exchange)); data.put("createdAt", Instant.now().toString()); String id = UUID.randomUUID().toString(); root.collection("appliances").document(id).set(data).get(); data.put("id", id); send(exchange, 201, data); return; }
        sendError(exchange, 405, "Method not allowed");
    }

    private static void appliance(HttpExchange exchange, String uid, String method, String id) throws Exception {
        if (id.isBlank() || id.contains("/")) { sendError(exchange, 400, "Invalid appliance id"); return; }
        DocumentReference ref = DB.collection("users").document(uid).collection("appliances").document(id);
        if (method.equals("PUT")) { Map<String, Object> data = validateAppliance(readBody(exchange)); data.put("updatedAt", Instant.now().toString()); ref.set(data).get(); data.put("id", id); send(exchange, 200, data); return; }
        if (method.equals("DELETE")) { ref.delete().get(); send(exchange, 204, Map.of()); return; }
        sendError(exchange, 405, "Method not allowed");
    }

    private static void simulations(HttpExchange exchange, String uid, String method) throws Exception {
        if (!method.equals("GET") && !method.equals("POST")) { sendError(exchange, 405, "Method not allowed"); return; }
        var collection = DB.collection("users").document(uid).collection("simulations");
        if (method.equals("GET")) { List<Map<String, Object>> result = new ArrayList<>(); for (QueryDocumentSnapshot doc : collection.get().get().getDocuments()) { Map<String, Object> item = new HashMap<>(doc.getData()); item.put("id", doc.getId()); result.add(item); } send(exchange, 200, result); return; }
        Map<String, Object> data = readBody(exchange); data.put("createdAt", Instant.now().toString()); String id = UUID.randomUUID().toString(); collection.document(id).set(data).get(); data.put("id", id); send(exchange, 201, data);
    }

    private static void settings(HttpExchange exchange, String uid, String method) throws Exception {
        DocumentReference ref = DB.collection("users").document(uid).collection("settings").document("general");
        if (method.equals("GET")) { var snapshot = ref.get().get(); send(exchange, 200, snapshot.exists() ? snapshot.getData() : Map.of("electricityRate", 8, "currency", "Rs.")); return; }
        if (method.equals("PUT")) { Map<String, Object> data = readBody(exchange); Object rate = data.get("electricityRate"); if (!(rate instanceof Number) || ((Number) rate).doubleValue() <= 0) throw new IllegalArgumentException("Electricity rate must be positive"); data.put("currency", String.valueOf(data.getOrDefault("currency", "Rs."))); ref.set(data).get(); send(exchange, 200, data); return; }
        sendError(exchange, 405, "Method not allowed");
    }

    private static void dashboard(HttpExchange exchange, String uid) throws Exception {
        List<Appliance> appliances = new ArrayList<>();
        for (QueryDocumentSnapshot doc : DB.collection("users").document(uid).collection("appliances").get().get().getDocuments()) appliances.add(toAppliance(doc.getData()));
        double rate = 8;
        var settings = DB.collection("users").document(uid).collection("settings").document("general").get().get();
        if (settings.exists() && settings.getData().get("electricityRate") instanceof Number n) rate = n.doubleValue();
        double monthly = CALCULATOR.totalMonthlyKwh(appliances), target = appliances.stream().mapToDouble(a -> a.getPowerWatts() * a.getUnits() * a.getNecessaryHoursPerDay() * a.getDaysPerMonth() / 1000).sum(), avoidable = Math.max(0, monthly - target);
        send(exchange, 200, Map.of("totalAppliances", appliances.size(), "monthlyConsumption", monthly, "targetConsumption", target, "potentiallyAvoidableEnergy", avoidable, "potentiallyAvoidablePercent", monthly == 0 ? 0 : avoidable / monthly * 100, "estimatedMonthlyCost", monthly * rate, "annualConsumption", monthly * 12, "potentialAnnualSaving", avoidable * rate * 12, "electricityRate", rate));
    }

    private static Map<String, Object> validateAppliance(Map<String, Object> data) {
        String name = String.valueOf(data.getOrDefault("name", "")).trim(); double watts = number(data, "powerWatts"); double actual = number(data, "actualHoursPerDay"); double necessary = number(data, "necessaryHoursPerDay"); int days = (int) number(data, "days"); int units = data.get("units") == null ? 1 : (int) number(data, "units");
        if (name.isEmpty() || watts <= 0 || actual < 0 || actual > 24 || necessary < 0 || necessary > actual || days < 1 || days > 31 || units < 1) throw new IllegalArgumentException("Invalid appliance values");
        return new HashMap<>(Map.of("name", name, "powerWatts", watts, "actualHoursPerDay", actual, "necessaryHoursPerDay", necessary, "days", days, "units", units));
    }

    private static double number(Map<String, Object> data, String key) { Object value = data.get(key); if (!(value instanceof Number)) throw new IllegalArgumentException("Missing numeric field: " + key); return ((Number) value).doubleValue(); }
    private static Appliance toAppliance(Map<String, Object> data) { return new Appliance(String.valueOf(data.get("name")), ((Number) data.get("powerWatts")).doubleValue(), ((Number) data.getOrDefault("units", 1)).intValue(), ((Number) data.get("actualHoursPerDay")).doubleValue(), ((Number) data.get("days")).intValue(), ((Number) data.get("necessaryHoursPerDay")).doubleValue()); }
    private static Map<String, Object> readBody(HttpExchange exchange) throws IOException { return JSON.readValue(exchange.getRequestBody(), new TypeReference<>() {}); }
    private static FirebaseToken authenticate(HttpExchange exchange) throws Exception { String header = exchange.getRequestHeaders().getFirst("Authorization"); if (header == null || !header.startsWith("Bearer ")) throw new SecurityException(); return FirebaseAuth.getInstance().verifyIdToken(header.substring(7)); }
    private static void ensureProfile(FirebaseToken token) throws Exception { DocumentReference ref = DB.collection("users").document(token.getUid()); if (!ref.get().get().exists()) ref.set(Map.of("name", token.getName() == null ? "" : token.getName(), "email", token.getEmail(), "profileImage", token.getPicture() == null ? "" : token.getPicture(), "createdAt", Instant.now().toString())).get(); }
    private static Firestore initializeFirebase() { try { String key = System.getenv("FIREBASE_PRIVATE_KEY").replace("\\n", "\n"); String json = "{\"type\":\"service_account\",\"project_id\":\"" + System.getenv("FIREBASE_PROJECT_ID") + "\",\"private_key\":\"" + key.replace("\n", "\\n") + "\",\"client_email\":\"" + System.getenv("FIREBASE_CLIENT_EMAIL") + "\"}"; FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))).setProjectId(System.getenv("FIREBASE_PROJECT_ID")).build(); if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(options); return FirestoreClient.getFirestore(); } catch (Exception e) { throw new IllegalStateException("Firebase configuration is invalid", e); } }
    private static void addCors(HttpExchange exchange) { String origin = System.getenv("FRONTEND_URL"); if (origin != null && !origin.isBlank()) exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin); exchange.getResponseHeaders().set("Vary", "Origin"); exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type"); exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"); }
    private static void sendError(HttpExchange exchange, int status, String message) throws IOException { send(exchange, status, Map.of("error", message)); }
    private static void send(HttpExchange exchange, int status, Object body) throws IOException { byte[] bytes = JSON.writeValueAsBytes(body); exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length); if (status != 204) try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); } }
}