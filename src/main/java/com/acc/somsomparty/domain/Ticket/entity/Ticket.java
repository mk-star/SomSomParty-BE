package com.acc.somsomparty.domain.Ticket.entity;

import com.acc.somsomparty.domain.Festival.entity.Festival;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "festival_id")
    private Festival festival;

    @Column(name = "festival_date", nullable = false)
    private LocalDate festivalDate;

    @Column(name = "total_tickets", nullable = false)
    private Integer totalTickets;

    @Setter
    @Column(name = "left_tickets", nullable = false)
    private Integer leftTickets;
}
