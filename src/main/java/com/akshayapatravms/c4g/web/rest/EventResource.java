package com.akshayapatravms.c4g.web.rest;

import com.akshayapatravms.c4g.domain.Event;
import com.akshayapatravms.c4g.repository.EventRepository;
import com.akshayapatravms.c4g.security.AuthoritiesConstants;
import com.akshayapatravms.c4g.service.EventService;
import com.akshayapatravms.c4g.service.dto.EventDTO;
import com.akshayapatravms.c4g.service.dto.ProfileEventDTO;

import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

//todo: have controllers return response entities
@RestController
@RequestMapping("/api/events")
public class EventResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    private final EventService eventService;

    private final EventRepository eventRepository;

    public EventResource(EventService eventService,
                         EventRepository eventRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    @PostMapping("/createEvent")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public Event createEvent(@RequestBody EventDTO eventDTO) throws URISyntaxException {
        return eventService.createEvent(eventDTO);
    }

    @GetMapping("/event/{id}")
    public Event eventById(@PathVariable Long id) throws URISyntaxException {
        return eventRepository.findOneById(id).orElseThrow(() -> new RuntimeException("could not find an event by this id"));
    }

//    need one for admins which contains all registered users and one for normal user which doesn't
    @GetMapping("/all")
    public List<Event> allEvents() throws URISyntaxException {
        return this.eventRepository.findAll();
    }

    @PostMapping("/volunteer")
    public void volunteerForEvent(@RequestBody Long eventID) throws URISyntaxException {
        log.info("volunteerForEvent called");
        eventService.volunteerForEvent(eventID);
        return;
    }

    @DeleteMapping("unRegister")
    public void unRegisterForEvent(@RequestBody Long eventID) {
        log.info("unregister for event called");
        eventService.unRegisterForEvent(eventID);
    }

    @GetMapping("getAll")
    public List<Event> getAllEvents() {
        return eventService.getAll();
    }
}
