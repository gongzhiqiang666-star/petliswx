package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.ClientEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ClientEventController {
    private final ClientEventService events;
    public record EventBatch(List<Map<String,String>> events) { }
    @PostMapping("/client-events")
    public Result<Void> receive(@RequestBody EventBatch batch) {
        events.receive(batch.events()); return Result.success("事件已接收");
    }
    @GetMapping("/admin/client-events/summary")
    public Result<List<Map<String,Object>>> summary(@RequestParam(defaultValue="7") int days) {
        return Result.success(events.summary(days));
    }
}
