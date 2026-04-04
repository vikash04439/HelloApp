package com.learn.rest.HelloApp.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HelloController {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${spring.application.name:HelloApp}")
    private String appName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${logging.file.name}")
    private String logPath;

    @RequestMapping(method = RequestMethod.GET, path="/")
    public String helloWorld(){
        return "Hello World from the App";
    }

    @GetMapping(path="/welcome")
    public Map<String, Object> welcome(){
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

        // Timestamp
        info.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return info;
    }
}
