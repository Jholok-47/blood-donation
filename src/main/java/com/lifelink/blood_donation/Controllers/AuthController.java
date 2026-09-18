package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.DTO.LocalRegisterRequest;
import com.lifelink.blood_donation.DTO.OtpVerifyRequest;
import com.lifelink.blood_donation.Exceptions.EmailAlreadyExistsException;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ---------- Path B: local email/password registration ----------

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new LocalRegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") LocalRegisterRequest req,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            authService.registerLocal(req);
        } catch (EmailAlreadyExistsException | InvalidOperationException ex) {
            model.addAttribute("registerError", ex.getMessage());
            return "register";
        }
        redirectAttributes.addAttribute("email", req.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "We've sent a verification code to your email.");
        return "redirect:/verify-otp";
    }

    // ---------- Shared OTP verification step (Path B only — Google users never see this) ----------

    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(@RequestParam String email, Model model) {
        OtpVerifyRequest otpRequest = new OtpVerifyRequest();
        otpRequest.setEmail(email);
        model.addAttribute("otpVerifyRequest", otpRequest);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@Valid @ModelAttribute("otpVerifyRequest") OtpVerifyRequest req,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "verify-otp";
        }
        try {
            authService.verifyRegistrationOtp(req);
        } catch (InvalidOperationException ex) {
            model.addAttribute("otpError", ex.getMessage());
            return "verify-otp";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Email verified — you can now log in.");
        return "redirect:/login";
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            authService.resendOtp(email);
            redirectAttributes.addFlashAttribute("successMessage", "A new code has been sent.");
        } catch (InvalidOperationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        redirectAttributes.addAttribute("email", email);
        return "redirect:/verify-otp";
    }

    // ---------- Login (Google callback is handled entirely by Spring Security's oauth2Login()) ----------

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }
}