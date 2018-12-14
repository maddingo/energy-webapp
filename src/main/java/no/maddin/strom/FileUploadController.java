package no.maddin.strom;

import lombok.extern.slf4j.Slf4j;
import org.apache.el.stream.Optional;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
@DeclareRoles("USER")
@RolesAllowed("*")
public class FileUploadController {

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

    private void storeFile(MultipartFile file) {
        log.info("Storing file: " + file.getOriginalFilename());
    }

}