package com.onip.facm01.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Pages Thymeleaf du tableau de bord : un paramètre invalide dans l'adresse (filtre, identifiant
// de ménage ou de zone qui n'existe plus...) ramène à la liste des ménages avec un message,
// au lieu d'afficher la page d'erreur brute de Spring.
@ControllerAdvice(assignableTypes = {DashboardController.class, ZoneAdminController.class})
public class DashboardExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DashboardExceptionHandler.class);

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public String handle(Exception e, RedirectAttributes redirectAttributes) {
        log.warn("Requête du tableau de bord refusée : {}", e.getMessage());
        redirectAttributes.addFlashAttribute("error",
                e instanceof IllegalArgumentException ? e.getMessage() : "Filtre ou lien invalide : liste réaffichée.");
        return "redirect:/dashboard";
    }
}
