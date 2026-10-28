package com.petlis.controller.api;

import com.petlis.service.impl.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CouponEntryReviewTest {
    @Test void grantWithoutRequestBodyCannotBypassStockAndIdempotency() throws Exception {
        CouponService service = mock(CouponService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CouponController(service)).build();
        mvc.perform(post("/admin/coupons/9/users/5").contentType("application/json")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
