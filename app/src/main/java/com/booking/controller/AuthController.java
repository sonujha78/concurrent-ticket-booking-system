package com.booking.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Simple demo login: no password check, just sets the session.
    // Session is stored in Redis (Spring Session), so it survives
    // even if HAProxy routes the next request to a different Tomcat node.
    @PostMapping("/login")
    public String login(@RequestParam String userId, HttpSession session) {
        session.setAttribute("userId", userId);
        return "redirect:/events";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/whoami")
    public String whoAmI(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        String userId = (session != null) ? (String) session.getAttribute("userId") : null;
        model.addAttribute("userId", userId);
        model.addAttribute("sessionId", (session != null) ? session.getId() : "none");
        return "whoami";
    }
}
