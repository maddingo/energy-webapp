package no.maddin.strom;

import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBException;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PreDestroy;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;

@Slf4j
@Controller
@DeclareRoles("USER")
@RolesAllowed("*")
public class FileUploadController {

    private ExecutorService saveService = java.util.concurrent.Executors.newSingleThreadExecutor();

    @Value("${strom.db_url:http://localhost:8083}")
    private String dbUrl;

    @Value("${strom.db_user:root}")
    private String dbUser;

    @Value("${strom.db_password:5up3rS3cr3t}")
    private String dbPassword;

    @Value("${file.upload-dir}")
    private String fileUploadDir;

    @GetMapping("/")
    public String index(HttpServletRequest req, Model model) {
        java.util.Optional.ofNullable(req.getSession(false)).ifPresent(httpSession -> model.addAttribute("csrf_token", httpSession.getAttribute(HttpSessionCsrfTokenRepository.class.getName().concat(".CSRF_TOKEN"))));
            return "upload";
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {

        storeFile(file);
        redirectAttributes.addFlashAttribute("message",
            "You successfully uploaded " + file.getOriginalFilename() + "!");
        redirectAttributes.addFlashAttribute("message-type", "text-success");

        return "redirect:/";
    }

    @GetMapping("/deleteAll")
    public String deleteAll(RedirectAttributes redirectAttributes) {
        try (InfluxDB influxDB = InfluxDBFactory.connect(dbUrl, dbUser, dbPassword)) {
            QueryResult queryResult = influxDB.query(new Query("DROP SERIES FROM /.*/", "strom"));
            if (queryResult.hasError()) {
                redirectAttributes.addFlashAttribute("message", queryResult.getError());
                redirectAttributes.addFlashAttribute("message-type", "text-danger");
            } else {
                redirectAttributes.addFlashAttribute("message", "All data deleted");
                redirectAttributes.addFlashAttribute("message-type", "text-success");
            }
            influxDB.flush();
        } catch (InfluxDBException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("message-type", "text-danger");
        }


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
            Path tempFile = Files.createTempFile("download", ".tmp");
            try (
                InputStream in = file.getInputStream();
                OutputStream out = Files.newOutputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
            ) {
                FileCopyUtils.copy(in, out);
                App.builder()
                    .dataFile(tempFile.toFile())
                    .dbUrl(dbUrl)
                    .dbUser(dbUser)
                    .dbPassword(dbPassword)
                    .build()
                    .save();
            }
        } catch (IOException ex) {
            log.warn("Processing file " + file.getOriginalFilename(), ex);
        }
    }

}