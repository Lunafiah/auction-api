package com.example.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BidResponse {
    private String status;       // "ACCEPTED" or "REJECTED"
    private String message;
    private Long auctionItemId;
    private BigDecimal currentHighestBid;
    private String highestBidder;
}
