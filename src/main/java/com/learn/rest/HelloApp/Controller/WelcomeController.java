package com.learn.rest.HelloApp.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thymeleaf-based controller for the Welcome Dashboard UI.
 * Serves the /welcome-dashboard page using Thymeleaf template engine.
 * The existing /welcome REST API in HelloController remains unchanged.
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

    @GetMapping("/welcome-dashboard")
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

