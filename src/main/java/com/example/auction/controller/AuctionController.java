package com.example.auction.controller;

import com.example.auction.dto.BidRequest;
import com.example.auction.dto.BidResponse;
import com.example.auction.model.AuctionItem;
import com.example.auction.service.AuctionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing auctions.
 * Exposes endpoints for retrieving auction items and placing bids.
 */
@RestController
@RequestMapping("/api/auctions")
@Slf4j
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Retrieves a list of all available auction items.
     *
     * @return A ResponseEntity containing a list of AuctionItems.
     */
    @GetMapping
    public ResponseEntity<List<AuctionItem>> getAllAuctions() {
        log.info("Fetching all auctions");
        return ResponseEntity.ok(auctionService.getAllAuctions());
    }

    /**
     * Retrieves details of a specific auction item by ID.
     *
     * @param id The ID of the auction item.
     * @return A ResponseEntity containing the AuctionItem.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuctionItem> getAuction(@PathVariable Long id) {
        log.info("Fetching auction with ID: {}", id);
        return ResponseEntity.ok(auctionService.getAuctionById(id));
    }

    /**
     * Places a bid on a specific auction item.
     *
     * @param id             The ID of the auction item to bid on.
     * @param bidRequest     The bid payload containing the bid amount.
     * @param authentication The current authenticated user.
     * @return A ResponseEntity containing the BidResponse with success/failure status.
     */
    @PostMapping("/{id}/bid")
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable Long id,
            @Valid @RequestBody BidRequest bidRequest,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("User '{}' is placing a bid of {} on auction ID: {}", username, bidRequest.getAmount(), id);
        
        BidResponse response = auctionService.placeBid(id, bidRequest, username);

        if ("REJECTED".equals(response.getStatus())) {
            log.warn("Bid from user '{}' on auction ID: {} was REJECTED. Reason: {}", username, id, response.getMessage());
            return ResponseEntity.status(409).body(response); // 409 Conflict
        }
        
        log.info("Bid from user '{}' on auction ID: {} was ACCEPTED", username, id);
        return ResponseEntity.ok(response);
    }
}
