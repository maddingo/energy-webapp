package no.maddin.strom;

import lombok.extern.slf4j.Slf4j;
import org.apache.el.stream.Optional;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PreDestroy;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

@Slf4j
@Controller
@DeclareRoles("USER")
@RolesAllowed("*")
public class FileUploadController {

    private ExecutorService saveService = java.util.concurrent.Executors.newSingleThreadExecutor();
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    @GetMapping("/")
    public String index(HttpServletRequest req, Model model) {
        java.util.Optional.ofNullable(req.getSession(false)).ifPresent(httpSession -> model.addAttribute("csrf_token", httpSession.getAttribute(HttpSessionCsrfTokenRepository.class.getName().concat(".CSRF_TOKEN"))));
        return "upload";
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {

        storeFile(file);
        redirectAttributes.addFlashAttribute("message",
            "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/";
    }

    @PreDestroy
    private void close() {
        saveService.shutdown();
    }

    private void storeFile(MultipartFile file) {
        log.info("Storinging file: " + file.getOriginalFilename());
        saveService.submit(() -> this.processFile(file));
    }

    private void processFile(MultipartFile file) {
        try {
            App.builder()
                .dataFile(file.getResource().getFile())
                .dbUrl(dbUrl)
                .dbUser(dbUser)
                .dbPassword(dbPassword)
                .build()
                .save();
        } catch(IOException ex) {
            log.warn("Processing file " + file.getOriginalFilename(), ex);
        }
    }

}