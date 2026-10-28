package com.petlis.controller.api;

import com.petlis.service.impl.PrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("admin/print")
@RequiredArgsConstructor
public class PrintController {
    private final PrintService service;
    private ResponseEntity<String> html(String body) { return ResponseEntity.ok().contentType(new MediaType("text", "html", StandardCharsets.UTF_8)).body(body); }
    @GetMapping("/orders/{orderId}") public ResponseEntity<String> order(@PathVariable int orderId) { return html(service.order(orderId)); }
    @GetMapping("/specimens/{orderId}") public ResponseEntity<String> specimen(@PathVariable int orderId) { return html(service.specimenLabel(orderId)); }
    @GetMapping("/reports/{detailId}") public ResponseEntity<String> report(@PathVariable int detailId) { return html(service.report(detailId)); }
}
