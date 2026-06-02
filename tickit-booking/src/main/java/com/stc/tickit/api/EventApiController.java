package com.stc.tickit.api;

import com.stc.tickit.dto.EventResponse;
import com.stc.tickit.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** US1: browse available events. */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventApiController {

    private final EventService eventService;

    @GetMapping
    public List<EventResponse> list() {
        return eventService.listBookableEvents().stream()
                .map(EventResponse::from)
                .toList();
    }
}
