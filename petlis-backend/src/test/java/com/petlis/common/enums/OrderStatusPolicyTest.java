package com.petlis.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.petlis.common.enums.OrderStatusEnum.*;

class OrderStatusPolicyTest {
    @Test void reviewPrecedesPayment() {
        assertTrue(isValidTransition(DRAFT,PENDING_REVIEW));
        assertFalse(isValidTransition(DRAFT,PENDING_PAYMENT));
        assertTrue(isValidTransition(PENDING_REVIEW,PENDING_PAYMENT));
        assertTrue(isValidTransition(PENDING_PAYMENT,WAITING_REPORT));
        assertFalse(isValidTransition(PENDING_PAYMENT,PENDING_REVIEW));
    }
    @Test void rejectedApplicationsCanBeResubmitted() {
        assertTrue(isValidTransition(REVIEW_FAILED,PENDING_REVIEW));
        assertFalse(isValidTransition(REVIEW_FAILED,WAITING_REPORT));
    }
    @Test void paymentIsNotADraftAndTerminalStatesStayTerminal() {
        assertFalse(isDraftStatus(-3));
        assertTrue(isDraftStatus(-2));
        assertFalse(isValidTransition(COMPLETED,PENDING_REVIEW));
        assertFalse(isValidTransition(CANCELLED,DRAFT));
        assertFalse(isValidTransition(PENDING_REVIEW,ALL));
    }
}
