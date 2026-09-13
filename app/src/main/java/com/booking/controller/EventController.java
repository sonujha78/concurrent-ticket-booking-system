package com.booking.controller;

import com.booking.entity.Event;
import com.booking.entity.Seat;
import com.booking.repository.EventRepository;
import com.booking.repository.SeatRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class EventController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatRepository seatRepository;

    @GetMapping("/events")
    public String listEvents(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        List<Event> events = eventRepository.findAll();
        model.addAttribute("events", events);
        model.addAttribute("userId", userId);
        return "events";
    }

    @GetMapping("/events/{eventId}/seats")
    public String seatMap(@PathVariable Integer eventId, HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        List<Seat> seats = seatRepository.findByEventId(eventId);
        model.addAttribute("seats", seats);
        model.addAttribute("eventId", eventId);
        model.addAttribute("userId", userId);
        return "seatmap";
    }
}
