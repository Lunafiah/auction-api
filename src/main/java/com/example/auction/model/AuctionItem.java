package com.example.auction.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal startingPrice;

    private BigDecimal currentHighestBid;

    private Long highestBidderId;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // This is the magic for Optimistic Locking!
    @Version
    private Integer version;
}
