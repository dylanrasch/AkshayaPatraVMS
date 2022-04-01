package com.akshayapatravms.c4g.service;

import com.akshayapatravms.c4g.domain.*;
import com.akshayapatravms.c4g.repository.CauseRepository;
import com.akshayapatravms.c4g.repository.CorporateSubgroupRepository;
import com.akshayapatravms.c4g.repository.EventRepository;
import com.akshayapatravms.c4g.security.AuthoritiesConstants;
import com.akshayapatravms.c4g.security.SecurityUtils;
import com.akshayapatravms.c4g.service.dto.AdminUserDTO;
import com.akshayapatravms.c4g.service.dto.CauseDTO;
import com.akshayapatravms.c4g.service.dto.CsvDTO;
import com.akshayapatravms.c4g.service.dto.EventDTO;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final EventRepository eventRepository;

    private final CauseRepository causeRepository;

    private final CorporateSubgroupRepository corporateSubgroupRepository;

    private final UserService userService;

    private final CacheManager cacheManager;

    public EventService(
        EventRepository eventRepository,
        CacheManager cacheManager,
        UserService userService,
        CauseRepository causeRepository,
        CorporateSubgroupRepository corporateSubgroupRepository
    ) {
        this.eventRepository = eventRepository;
        this.cacheManager = cacheManager;
        this.userService = userService;
        this.causeRepository = causeRepository;
        this.corporateSubgroupRepository = corporateSubgroupRepository;
    }

    //saves new causes to db
    private Set<Cause> existingAndNewCauses(EventDTO eventDTO) throws RuntimeException {
        return eventDTO
            .getCauses()
            .stream()
            .map(causeDTO -> {
                if (causeDTO.getId() != null) {
                    return causeRepository
                        .findOneById(causeDTO.getId())
                        .orElseThrow(() -> new RuntimeException("inexistant " + "cause by id"));
                } else {
                    //doesnt have an ID but has a name that matches an existing. Should the behavior be to use the cause with the matching name?
                    if (causeRepository.findOneByCauseName(causeDTO.getCauseName().toUpperCase()).isPresent()) {
                        throw new RuntimeException(
                            "One of the cause names you requested already exists. Please send its ID or choose a " + "new cause name"
                        );
                    } else {
                        Cause cause = new Cause();
                        cause.setCauseName(causeDTO.getCauseName().toUpperCase());
                        return causeRepository.save(cause);
                    }
                }
            })
            .collect(Collectors.toSet());
    }

    private Set<CorporateSubgroup> getCorpSubGroupsFromEvent(EventDTO eventDTO) {
        return eventDTO
            .getCorporateSubgroupIds()
            .stream()
            .map(corporateSubgroupId -> {
                return corporateSubgroupRepository
                    .findOneById(corporateSubgroupId)
                    .orElseThrow(() -> new RuntimeException("inexistant subgroup by id"));
            })
            .collect(Collectors.toSet());
    }

    public Event createEvent(EventDTO eventDTO) {
        Event event = new Event();

        Set<Cause> causes = existingAndNewCauses(eventDTO);
        event.setCauses(causes);

        Set<CorporateSubgroup> corporateSubgroups = getCorpSubGroupsFromEvent(eventDTO);
        event.setCorporateSubgroups(corporateSubgroups);

        event.setEventName(eventDTO.getEventName());

        if (eventDTO.getPhysicalLocation() != null) {
            event.setLocation(new PhysicalLocation(eventDTO.getPhysicalLocation()));
        } else if (eventDTO.getVirtualLocation() != null) {
            event.setVirtualLocation(new VirtualLocation(eventDTO.getVirtualLocation()));
        }

        event.setDescription(eventDTO.getDescription());
        event.setVolunteersNeededAmount(eventDTO.getVolunteersNeededAmount());
        event.setStartDate(eventDTO.getStartDate());
        event.setEndDate(eventDTO.getEndDate());
        event.setStartTime(new Time(eventDTO.getStartTime()));
        event.setEndTime(new Time(eventDTO.getEndTime()));
        event.setContactName(eventDTO.getContactName());
        event.setContactPhoneNumber(eventDTO.getContactPhoneNumber());
        event.setContactEmail(eventDTO.getContactEmail());
        event.setEmailBody(eventDTO.getEmailBody());

        User user = userService.getUserWithAuthorities().orElseThrow(() -> new RuntimeException("couldn't find currently logged in user"));
        event.setEventCreator(user);

        return eventRepository.save(event);
    }

    private HashSet<String> getEmailPatternsForEvent(Event event) {
        HashSet<String> emailPatterns = new HashSet<>();
        for (CorporateSubgroup corp : event.getCorporateSubgroups()){
            emailPatterns.addAll(corp.getSubgroupEmailPatterns());
        }

        return emailPatterns;
    }

    private Boolean isEmailMatch(Set<String> emailPatterns, String email){
        for (String emailPattern: emailPatterns){
            if (email.endsWith(emailPattern)){
                return true;
            }
        }
        return false;
    }

    public void checkEligibility(User user, Event event) throws RuntimeException {
//          Turned off until we have TOS set up
//        if (user.getAcceptedTOS() != true){
//            throw new RuntimeException("User has not accepted TOS");
//        }

        //have volunteers spots left
        if (event.getVolunteers().size() >= event.getVolunteersNeededAmount()){
            throw new RuntimeException("volunteering event is full");
        }

        //does user have an email that is eligible for event
        if (event.getCorporateSubgroups().size() >0) {
            HashSet<String> emailPatterns = getEmailPatternsForEvent(event);
            if (!isEmailMatch(emailPatterns,user.getEmail())){
                throw  new RuntimeException("user does not have an allowed corporate email");
            }
        }

        log.info("user is eligible for event");
    }


    public void volunteerForEvent(Long eventID) throws RuntimeException{
        final Optional<User> isUser = userService.getUserWithAuthorities();
        if(!isUser.isPresent()) {
            throw new RuntimeException("unable to find user");
        }

        final User user = isUser.get();

        Optional<Event>  event = eventRepository.findOneById(eventID);

        if (event.isPresent()){
            try{
                checkEligibility(user,event.get());
                event.get().getVolunteers().add(user);
                eventRepository.save(event.get());
            } catch (Exception e){
                throw e;
            }

        } else{
            throw new RuntimeException("unable to find event");
        }

    }

    public void unRegisterForEvent (Long eventID)  throws RuntimeException {

        final Optional<User> isUser = userService.getUserWithAuthorities();
        if(!isUser.isPresent()) {
            throw new RuntimeException("unable to find user");
        }
        final User user = isUser.get();

        Optional<Event>  event = eventRepository.findOneById(eventID);

        if (event.isPresent()){
            try {
//                log.info("event" + event.get());
//                log.info("volunteer count before " + event.get().getVolunteers().size());
                event.get().getVolunteers().remove(isUser.get());
//                log.info("volunteer count after " + event.get().getVolunteers().size());
//                log.info("user id " + user.getId() + " event id " + event.get().getId());
                eventRepository.save(event.get());

//                log.info("num of volunteers " + event.get().getVolunteers().size());
//                log.info("num of events vol for " + user.getEvents().size());
//                log.info("volunteers for event " + event.get().getVolunteers());
//                log.info("events volunteering for " + user.getEvents());
                //join table is emptied, but user is still showing events.
            } catch (Exception e) {
                throw new RuntimeException("unable to save event");
            }
        } else{
            throw new RuntimeException("unable to find event");
        }
    }

//    public Optional<EventDTO> updateEvent(EventDTO eventDTO) {
//        return Optional
//            .of(eventRepository.findById(eventDTO.getId()))
//            .filter(Optional::isPresent)
//            .map(Optional::get)
//            .map(event -> {
//                Set<Cause> causes = event.getCauses();
//                causes.clear();
//                eventDTO
//                    .getCauses()
//                    .stream()
//                    .map(Cause::getId)
//                    .map(causeRepository::findById)
//                    .filter(Optional::isPresent)
//                    .map(Optional::get)
//                    .forEach(causes::add);
//
//                Set<CorporateSubgroup> corporateSubgroups = event.getCorporateSubgroups();
//                corporateSubgroups.clear();
//                eventDTO
//                    .getCorporateSubgroupIds()
//                    .stream()
//                    .map(corporateSubgroupRepository::findById)
//                    .filter(Optional::isPresent)
//                    .map(Optional::get)
//                    .forEach(corporateSubgroups::add);
//                event.setEventName(eventDTO.getEventName());
//                event.setDescription(eventDTO.getDescription());
//                event.setContactEmail(eventDTO.getContactEmail());
//                event.setContactName(eventDTO.getContactName());
//                event.setContactPhoneNumber(eventDTO.getContactPhoneNumber());
//                event.setEmailBody(eventDTO.getEmailBody());
//                event.setEndDate(eventDTO.getEndDate());
//                event.setStartDate(eventDTO.getStartDate());
//                event.setStartTime(new Time(eventDTO.getStartTime()));
//                event.setLocation(new PhysicalLocation(eventDTO.getPhysicalLocation()));
//                event.setVirtualLocation(new VirtualLocation(eventDTO.getVirtualLocation()));
//                event.setVolunteersNeededAmount(eventDTO.getVolunteersNeededAmount());
//                return event;
//            })
//            .map(EventDTO::new);
//    }

    public void deleteEvent (Long eventID)  throws RuntimeException {
        final Optional<User> isUser = userService.getUserWithAuthorities();
        if(!isUser.isPresent()) {
            throw new RuntimeException("unable to find user");
        }
        if (SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)){
            eventRepository.deleteEventByCreator(eventID, isUser.get().getId());
            log.debug("Deleted Event: {}", eventID);
        }
    }

    public List<Event> getAll() throws  RuntimeException{
        final Optional<User> isUser = userService.getUserWithAuthorities();
        if(!isUser.isPresent()) {
            throw new RuntimeException("unable to find user");
        }
        if (SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)){
            return eventRepository.findAllEventsAndVolunteers();
        } else {
            return eventRepository.findAll();
        }
    }

    public List<Event> getAllPast() throws  RuntimeException{
        final Optional<User> isUser = userService.getUserWithAuthorities();
        if(!isUser.isPresent()) {
            throw new RuntimeException("unable to find user");
        }
        if (SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)){
            return eventRepository.findAllPastEvents();
        } else {
            return eventRepository.findAllCompletedEventsForUser(isUser.get().getId());
        }
    }

    public List<Event> getAllFuture() throws  RuntimeException{
        final Optional<User> isUser = userService.getUserWithAuthorities();
        if(!isUser.isPresent()) {
            throw new RuntimeException("unable to find user");
        }
        if (SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)){
            return eventRepository.findAllFutureEvents();
        } else {
            return eventRepository.findAllFutureEventsForUser(isUser.get().getId());
        }
    }

    public List<Event> getAllFutureEventsForUser() throws  RuntimeException{
        final Optional<User> isUser = userService.getUserWithAuthorities();
        if(!isUser.isPresent()) {
            throw new RuntimeException("unable to find user");
        }
        return eventRepository.findAllFutureEventsForUser(isUser.get().getId());
    }

    public List<Event> getAllCompletedEventsForUser() throws  RuntimeException{
        final Optional<User> isUser = userService.getUserWithAuthorities();
        if(!isUser.isPresent()) {
            throw new RuntimeException("unable to find user");
        }
        return eventRepository.findAllCompletedEventsForUser(isUser.get().getId());
    }


    //code from https://codeburst.io/returning-csv-content-from-an-api-in-spring-boot-63ea82bbcf0f
    public CsvDTO createCSVFileOfEventVolunteers(Long eventID) throws RuntimeException {
        String[] csvHeader = {
            "name", "email"
        };

        Event event = eventRepository.getById(eventID);
        Set<User> volunteers = event.getVolunteers();
        List<List<String>> csvBody = new ArrayList<>(volunteers.size());
        for (User volunteer: volunteers) {
            csvBody.add(Arrays.asList(volunteer.getFullName(),volunteer.getEmail()));
        }

        ByteArrayInputStream byteArrayOutputStream;

        try (
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CSVPrinter csvPrinter = new CSVPrinter(
                new PrintWriter(out),
                CSVFormat.DEFAULT.withHeader(csvHeader)
            );
        ) {
            for (List<String> record : csvBody) {
                csvPrinter.printRecord(record);
            }
            csvPrinter.flush();
            byteArrayOutputStream = new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        CsvDTO csvDTO = new CsvDTO();
        //add date as well to name?
        //may need to validate event name in case there's some character in the name that doesn't play well with filenames
        csvDTO.setFileName(event.getEventName() + "_volunteers.csv");
        csvDTO.setDataStream(new InputStreamResource(byteArrayOutputStream));
        return csvDTO;
    }
}
