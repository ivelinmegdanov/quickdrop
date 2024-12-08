package org.rostislav.quickdrop.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.rostislav.quickdrop.entity.ApplicationSettingsEntity;
import org.rostislav.quickdrop.model.AnalyticsDataView;
import org.rostislav.quickdrop.model.ApplicationSettingsViewModel;
import org.rostislav.quickdrop.model.FileEntityView;
import org.rostislav.quickdrop.service.AnalyticsService;
import org.rostislav.quickdrop.service.ApplicationSettingsService;
import org.rostislav.quickdrop.service.FileService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static org.rostislav.quickdrop.util.FileUtils.bytesToMegabytes;
import static org.rostislav.quickdrop.util.FileUtils.megabytesToBytes;

@Controller
@RequestMapping("/admin")
public class AdminViewController {
    private final ApplicationSettingsService applicationSettingsService;
    private final AnalyticsService analyticsService;
    private final FileService fileService;

    public AdminViewController(ApplicationSettingsService applicationSettingsService, AnalyticsService analyticsService, FileService fileService) {
        this.applicationSettingsService = applicationSettingsService;
        this.analyticsService = analyticsService;
        this.fileService = fileService;
    }

    @GetMapping("/dashboard")
    public String getDashboardPage(Model model, HttpServletRequest request) {
        if (!applicationSettingsService.checkForAdminPassword(request)) {
            return "redirect:/admin/password";
        }

        List<FileEntityView> files = fileService.getAllFilesWithDownloadCounts();
        model.addAttribute("files", files);

        AnalyticsDataView analytics = analyticsService.getAnalytics();
        model.addAttribute("analytics", analytics);

        return "admin/dashboard";
    }

    @GetMapping("/setup")
    public String showSetupPage() {
        if (applicationSettingsService.isAdminPasswordSet()) {
            return "redirect:/admin/dashboard";
        }
        return "welcome";
    }

    @PostMapping("/setup")
    public String setAdminPassword(String adminPassword) {
        applicationSettingsService.setAdminPassword(adminPassword);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/settings")
    public String getSettingsPage(Model model, HttpServletRequest request) {
        if (!applicationSettingsService.checkForAdminPassword(request)) {
            return "redirect:/admin/password";
        }

        ApplicationSettingsEntity settings = applicationSettingsService.getApplicationSettings();

        ApplicationSettingsViewModel applicationSettingsViewModel = new ApplicationSettingsViewModel(settings);
        applicationSettingsViewModel.setMaxFileSize(bytesToMegabytes(settings.getMaxFileSize()));

        model.addAttribute("settings", applicationSettingsViewModel);
        return "admin/settings";
    }

    @PostMapping("/save")
    public String saveSettings(ApplicationSettingsViewModel settings, HttpServletRequest request) {
        if (!applicationSettingsService.checkForAdminPassword(request)) {
            return "redirect:/admin/password";
        }
        settings.setMaxFileSize(megabytesToBytes(settings.getMaxFileSize()));


        applicationSettingsService.updateApplicationSettings(settings, settings.getAppPassword());
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/password")
    public String checkAdminPassword(@RequestParam String password, HttpServletRequest request) {
        String adminPasswordHash = applicationSettingsService.getAdminPasswordHash();
        if (BCrypt.checkpw(password, adminPasswordHash)) {
            request.getSession().setAttribute("adminPassword", adminPasswordHash);
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/admin/password";
        }
    }

    @GetMapping("/password")
    public String showAdminPasswordPage() {
        return "/admin/admin-password";
    }
}
