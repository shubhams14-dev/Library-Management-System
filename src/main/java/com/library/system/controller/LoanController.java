package com.library.system.controller;

import com.library.system.domain.Loan;
import com.library.system.domain.User;
import com.library.system.service.LoanService;
import com.library.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/loans")
public class LoanController {
    
    @Autowired
    private LoanService loanService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/my-loans")
    public String myLoans(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userService.getUserByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Loan> activeLoans = loanService.getActiveLoansByUser(user);
        List<Loan> allLoans = loanService.getLoansByUser(user);
        
        model.addAttribute("activeLoans", activeLoans);
        model.addAttribute("allLoans", allLoans);
        model.addAttribute("user", user);
        
        return "my-loans";
    }
    
    @PostMapping("/borrow")
    public String borrowBook(@RequestParam Long bookId, RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Loan loan = loanService.borrowBook(user.getId(), bookId);
            redirectAttributes.addFlashAttribute("success", "Book borrowed successfully!");
            return "redirect:/books/" + bookId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/books/" + bookId;
        }
    }
    
    @PostMapping("/return/{loanId}")
    public String returnBook(@PathVariable Long loanId, RedirectAttributes redirectAttributes) {
        try {
            Loan loan = loanService.returnBook(loanId);
            redirectAttributes.addFlashAttribute("success", "Book returned successfully!");
            return "redirect:/loans/my-loans";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/loans/my-loans";
        }
    }
    
    @PostMapping("/extend/{loanId}")
    public String extendLoan(@PathVariable Long loanId, RedirectAttributes redirectAttributes) {
        try {
            Loan loan = loanService.extendLoan(loanId);
            redirectAttributes.addFlashAttribute("success", "Loan extended successfully!");
            return "redirect:/loans/my-loans";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/loans/my-loans";
        }
    }
}
