package com.akshayapatravms.c4g.service.dto;

import com.akshayapatravms.c4g.domain.*;

import javax.validation.constraints.Email;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class EventDTO {

    private Long id;

    private Set<CauseDTO> causes;

    private Set<String> emailFilters;

    private String eventName;

    private PhysicalLocationDTO physicalLocation;

    private VirtualLocationDTO virtualLocation;

    private String description;

    private Integer volunteersNeededAmount;

    private Instant startDate;

    private Instant endDate;

    private TimeDTO startTime;

    private TimeDTO endTime;

    private String contactName;

    private String contactPhoneNumber;

    @Email
    private String contactEmail;

    private String emailBody;

    private Set<CorporateSubgroup> corporateSubgroups;

    public EventDTO() {}

    public EventDTO(Event event) {

        Set<CauseDTO> causeDTOSet = new HashSet<>();
        event.getCauses().forEach(cause ->
            causeDTOSet.add(new CauseDTO(cause))
            );

        this.causes = causeDTOSet;
        if (event.getCorporateSubgroups() != null) {
            this.corporateSubgroups = event.getCorporateSubgroups();
        }
        this.setEventName(event.getEventName());
        this.setDescription(event.getDescription());
        this.setContactEmail(event.getContactEmail());
        this.setContactName(event.getContactName());
        this.setContactPhoneNumber(event.getContactPhoneNumber());
        this.setEmailBody(event.getEmailBody());
        this.setEndDate(event.getEndDate());
        this.setStartDate(event.getStartDate());
        if (event.getStartTime() != null) {
            this.setStartTime(new TimeDTO(event.getStartTime()));
        }
        if (event.getPhysicalLocation() != null) {
            this.setPhysicalLocation(new PhysicalLocationDTO(event.getPhysicalLocation()));
        }
        if (event.getVirtualLocation() != null) {
            this.setVirtualLocation(new VirtualLocationDTO(event.getVirtualLocation()));
        }
        this.setVolunteersNeededAmount(event.getVolunteersNeededAmount());
    }

    public VirtualLocationDTO getVirtualLocation() {
        return virtualLocation;
    }

    public void setVirtualLocation(VirtualLocationDTO virtualLocation) {
        this.virtualLocation = virtualLocation;
    }

    public TimeDTO getStartTime() {
        return startTime;
    }

    public void setStartTime(TimeDTO startTime) {
        this.startTime = startTime;
    }

    public TimeDTO getEndTime() {
        return endTime;
    }

    public void setEndTime(TimeDTO endTime) {
        this.endTime = endTime;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Set<String> getEmailFilters() {
        return emailFilters;
    }

    public void setEmailFilters(Set<String> emailFilters) {
        this.emailFilters = emailFilters;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<CauseDTO> getCauses() {
        return causes;
    }

    public void setCauses(Set<CauseDTO> causes) {
        this.causes = causes;
    }

    public PhysicalLocationDTO getPhysicalLocation() {
        return physicalLocation;
    }

    public void setPhysicalLocation(PhysicalLocationDTO physicalLocation) {
        this.physicalLocation = physicalLocation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getVolunteersNeededAmount() {
        return volunteersNeededAmount;
    }

    public void setVolunteersNeededAmount(Integer volunteersNeededAmount) {
        this.volunteersNeededAmount = volunteersNeededAmount;
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhoneNumber() {
        return contactPhoneNumber;
    }

    public void setContactPhoneNumber(String contactPhoneNumber) {
        this.contactPhoneNumber = contactPhoneNumber;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Set<CorporateSubgroup> getCorporateSubgroups() { return corporateSubgroups; }

    public void setCorporateSubgroups(Set<CorporateSubgroup> corporateSubgroups) { this.corporateSubgroups = corporateSubgroups;  }

}
