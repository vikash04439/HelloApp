package com.learn.rest.HelloApp.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified Welcome Controller (MVC + REST).
 * - @Controller at class level enables Thymeleaf view resolution.
 * - @ResponseBody on individual methods returns JSON/plain text directly.
 *
 * Endpoints:
 *   GET /                  → Redirects to /welcome-dashboard
 *   GET /welcome           → JSON system info (REST API)
 *   GET /welcome-dashboard → Thymeleaf HTML dashboard (MVC view)
 */
@Controller
public class WelcomeController {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${spring.application.name:HelloApp}")
    private String appName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${logging.file.name:logs/helloapp.log}")
    private String logPath;

    private LocalDateTime serverStartTime;

    @PostConstruct
    public void init() {
        serverStartTime = LocalDateTime.now();
    }

    private String computeUptime() {
        Duration duration = Duration.between(serverStartTime, LocalDateTime.now());
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
    }

    // ======================== REST Endpoints ========================

    /**
     * Root endpoint — redirects to the Thymeleaf dashboard
     */
    @RequestMapping(method = RequestMethod.GET, path = "/")
    public String helloWorld() {
        return "redirect:/dashboard";
    }

    /**
     * REST API — returns system info as JSON
     */
    @ResponseBody
    @GetMapping(path = "/welcome")
    public Map<String, Object> welcome() {
        Map<String, Object> info = new LinkedHashMap<>();

        // Application Info
        info.put("application", appName);
        info.put("message", "Welcome from the App");
        info.put("activeProfile", activeProfile);
        info.put("serverPort", serverPort);
        info.put("logPath", logPath);

        // Java Runtime Info
        Map<String, String> javaInfo = new LinkedHashMap<>();
        javaInfo.put("version", System.getProperty("java.version"));
        javaInfo.put("vendor", System.getProperty("java.vendor"));
        javaInfo.put("jvmName", System.getProperty("java.vm.name"));
        info.put("java", javaInfo);

        // OS Info
        Map<String, String> osInfo = new LinkedHashMap<>();
        osInfo.put("name", System.getProperty("os.name"));
        osInfo.put("version", System.getProperty("os.version"));
        osInfo.put("arch", System.getProperty("os.arch"));
        info.put("os", osInfo);

        // Runtime Info
        Runtime runtime = Runtime.getRuntime();
        Map<String, String> runtimeInfo = new LinkedHashMap<>();
        runtimeInfo.put("availableProcessors", String.valueOf(runtime.availableProcessors()));
        runtimeInfo.put("totalMemory", runtime.totalMemory() / (1024 * 1024) + " MB");
        runtimeInfo.put("freeMemory", runtime.freeMemory() / (1024 * 1024) + " MB");
        runtimeInfo.put("maxMemory", runtime.maxMemory() / (1024 * 1024) + " MB");
        info.put("runtime", runtimeInfo);

        // Server Uptime
        info.put("serverStartTime", serverStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("uptime", computeUptime());

        // Timestamp
        info.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return info;
    }

    // ======================== MVC (Thymeleaf) Endpoint ========================

    /**
     * Thymeleaf dashboard — renders welcome.html template
     */
    @GetMapping("/dashboard")
    public String welcomeDashboard(Model model) {

        // Header-level fields
        model.addAttribute("appName", appName);
        model.addAttribute("message", "Welcome from the App");
        model.addAttribute("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Scalar entries (top-level cards) — using LinkedHashMap to preserve order
        Map<String, String> scalarEntries = new LinkedHashMap<>();
        scalarEntries.put("Active Profile", activeProfile);
        scalarEntries.put("Server Port", serverPort);
        scalarEntries.put("Log Path", logPath);
        scalarEntries.put("Server Started At", serverStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        scalarEntries.put("Uptime", computeUptime());
        model.addAttribute("scalarEntries", scalarEntries);

        // Section entries (nested card groups) — each entry is a section
        Map<String, Map<String, String>> sectionEntries = new LinkedHashMap<>();

        // Java info
        Map<String, String> javaInfo = new LinkedHashMap<>();
        javaInfo.put("Version", System.getProperty("java.version"));
        javaInfo.put("Vendor", System.getProperty("java.vendor"));
        javaInfo.put("JVM Name", System.getProperty("java.vm.name"));
        sectionEntries.put("Java", javaInfo);

        // OS info
        Map<String, String> osInfo = new LinkedHashMap<>();
        osInfo.put("Name", System.getProperty("os.name"));
        osInfo.put("Version", System.getProperty("os.version"));
        osInfo.put("Architecture", System.getProperty("os.arch"));
        sectionEntries.put("OS", osInfo);

        // Runtime info
        Runtime runtime = Runtime.getRuntime();
        Map<String, String> runtimeInfo = new LinkedHashMap<>();
        runtimeInfo.put("Available Processors", String.valueOf(runtime.availableProcessors()));
        runtimeInfo.put("Total Memory", runtime.totalMemory() / (1024 * 1024) + " MB");
        runtimeInfo.put("Free Memory", runtime.freeMemory() / (1024 * 1024) + " MB");
        runtimeInfo.put("Max Memory", runtime.maxMemory() / (1024 * 1024) + " MB");
        sectionEntries.put("Runtime", runtimeInfo);

        model.addAttribute("sectionEntries", sectionEntries);

        // Section icon map for the template
        Map<String, String> sectionIcons = new LinkedHashMap<>();
        sectionIcons.put("Java", "☕");
        sectionIcons.put("OS", "🖥️");
        sectionIcons.put("Runtime", "⚙️");
        model.addAttribute("sectionIcons", sectionIcons);

        return "welcome"; // resolves to templates/welcome.html
    }
}

