package com.stc.tickit.service;

import com.stc.tickit.domain.Event;
import com.stc.tickit.exception.ResourceNotFoundException;
import com.stc.tickit.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    /** US1: events that are upcoming and not fully booked. */
    @Transactional(readOnly = true)
    public List<Event> listBookableEvents() {
        List<Event> events = eventRepository.findBookable(LocalDateTime.now());
        log.debug("Found {} bookable events", events.size());
        return events;
    }

    @Transactional(readOnly = true)
    public Event getEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }
}
