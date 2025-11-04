package com.library.system.service;

import com.library.system.domain.Loan;
import com.library.system.domain.LoanStatus;
import com.library.system.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(LoanReminderScheduler.class);

    private static final List<LoanStatus> REMINDER_ELIGIBLE_STATUSES = List.of(
        LoanStatus.ACTIVE,
        LoanStatus.EXTENDED
    );

    private final LoanRepository loanRepository;

    public LoanReminderScheduler(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    /**
     * Trigger the reminder check once when the application starts to aid local testing.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        sendDueSoonReminders();
    }

    /**
     * Daily job that scans for loans due soon (between 1 and 2 days away),
     * logs a reminder email in dev, and marks each loan as notified.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void sendDueSoonReminders() {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.plusDays(1);
        LocalDate windowEnd = today.plusDays(2);

        List<Loan> dueSoonLoans = loanRepository.findDueSoonLoansWithoutReminder(
            windowStart,
            windowEnd,
            REMINDER_ELIGIBLE_STATUSES
        );

        if (dueSoonLoans.isEmpty()) {
            log.debug("Loan reminder scheduler: no loans due between {} and {}", windowStart, windowEnd);
            return;
        }

        LocalDateTime sentAt = LocalDateTime.now();
        for (Loan loan : dueSoonLoans) {
            log.info(
                "DEV EMAIL -> To: {} <{}> | Book: '{}' | Due Date: {} | Message: Your loan is due soon.",
                loan.getUser().getFullName(),
                loan.getUser().getEmail(),
                loan.getBook().getTitle(),
                loan.getDueDate()
            );
            loan.setReminderSentAt(sentAt);
            loan.setUpdatedAt(sentAt);
        }

        loanRepository.saveAll(dueSoonLoans);
    }
}
