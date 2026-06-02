package com.stc.tickit.web;

import com.stc.tickit.dto.BookingForm;
import com.stc.tickit.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** US1 + entry point to US2 via Thymeleaf pages. */
@Controller
@RequiredArgsConstructor
public class EventWebController {

    private final EventService eventService;

    @GetMapping({"/", "/events"})
    public String list(Model model) {
        model.addAttribute("events", eventService.listBookableEvents());
        return "events/list";
    }

    @GetMapping("/events/{id}/book")
    public String bookForm(@PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.getEvent(id));
        if (!model.containsAttribute("bookingForm")) {
            model.addAttribute("bookingForm", new BookingForm(id));
        }
        return "events/book";
    }
}
